/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.event.tracking.phase.packet;

import com.google.common.collect.Lists;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketClickWindow;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.event.InternalNamedCauses;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.interfaces.IMixinContainer;
import org.spongepowered.common.item.inventory.util.ContainerUtil;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public abstract class InventoryClickPacketState extends BasicInventoryPacketState {

    /**
     * Flags we care about
     */
    private final int stateId;

    /**
     * Mask for flags we care about, the caremask if you will
     */
    private final int stateMask;

    /**
     * We care a lot
     *
     * @param stateId state
     */
    public InventoryClickPacketState(int stateId) {
        this(stateId, PacketPhase.MASK_ALL);
    }

    /**
     * We care about some things
     *
     * @param stateId flags we care about
     * @param stateMask caring mask
     */
    public InventoryClickPacketState(int stateId, int stateMask) {
        this.stateId = stateId & stateMask;
        this.stateMask = stateMask;
    }

    @Nullable
    public abstract ClickInventoryEvent createInventoryEvent(EntityPlayerMP playerMP, Container openContainer,
            Transaction<ItemStackSnapshot> transaction, List<SlotTransaction> slotTransactions, List<Entity> capturedEntities, Cause cause,
            int usedButton);

    @Override
    public void unwind(Packet<?> packet, EntityPlayerMP player, PhaseContext context) {
        final IMixinContainer mixinContainer = ContainerUtil.toMixin(player.openContainer);

        final CPacketClickWindow packetIn = context.firstNamed(InternalNamedCauses.Packet.CAPTURED_PACKET, CPacketClickWindow.class)
                .orElseThrow(TrackingUtil.throwWithContext("Expected to be capturing the packet used, but no packet was captured!", context));
        final ItemStackSnapshot lastCursor = context.firstNamed(InternalNamedCauses.Packet.CURSOR, ItemStackSnapshot.class)
                .orElseThrow(TrackingUtil.throwWithContext("Expected to be capturing the cursor item in use, but found none.", context));
        final ItemStackSnapshot newCursor = ItemStackUtil.snapshotOf(player.inventory.getItemStack());
        final Transaction<ItemStackSnapshot> transaction = new Transaction<>(lastCursor, newCursor);

        final net.minecraft.inventory.Container openContainer = player.openContainer;
        final List<SlotTransaction> slotTransactions = mixinContainer.getCapturedTransactions();

        final int usedButton = packetIn.getUsedButton();
        final List<Entity> capturedItems = new ArrayList<>();
        for (EntityItem entityItem : context.getCapturedItems()) {
            capturedItems.add(EntityUtil.fromNative(entityItem));
        }
        final Cause cause = Cause.of(NamedCause.source(player), NamedCause.of("Container", openContainer));
        final ClickInventoryEvent inventoryEvent = this.createInventoryEvent(player, ContainerUtil.fromNative(openContainer), transaction,
                                Lists.newArrayList(slotTransactions), capturedItems,
                                cause, usedButton);

        // Some mods may override container detectAndSendChanges method and prevent captures
        // If this happens and we captured no entities, avoid firing events
        if (mixinContainer.getCapturedTransactions().isEmpty() && capturedItems.isEmpty()) {
            mixinContainer.setCaptureInventory(false);
            return;
        }


        if (inventoryEvent != null) {
            // The client sends several packets all at once for drag events - we only care about the last one.
            // Therefore, we never add any 'fake' transactions, as the final packet has everything we want.
            if (shouldValidateTransactions()) {
                TrackingUtil.validateCapturedTransactions(packetIn.getSlotId(), openContainer, inventoryEvent.getTransactions());
            }

            SpongeImpl.postEvent(inventoryEvent);
            if (inventoryEvent.isCancelled() || TrackingUtil.allTransactionsInvalid(inventoryEvent.getTransactions())) {
                onCancelled(inventoryEvent);

                // Restore cursor
                TrackingUtil.handleCustomCursor(player, inventoryEvent.getCursorTransaction().getOriginal());

                // Restore target slots
                TrackingUtil.handleSlotRestore(player, openContainer, inventoryEvent.getTransactions(), true);
            } else {
                TrackingUtil.handleSlotRestore(player, openContainer, inventoryEvent.getTransactions(), false);

                // Handle cursor
                if (!inventoryEvent.getCursorTransaction().isValid()) {
                    TrackingUtil.handleCustomCursor(player, inventoryEvent.getCursorTransaction().getOriginal());
                } else if (inventoryEvent.getCursorTransaction().getCustom().isPresent()) {
                    TrackingUtil.handleCustomCursor(player, inventoryEvent.getCursorTransaction().getFinal());
                } else if (inventoryEvent instanceof ClickInventoryEvent.Drag) {
                    int increment;

                    increment = slotTransactions.stream().filter((t) -> !t.isValid()).collect(Collectors.summingInt((t) -> t.getFinal()
                            .getQuantity()));

                    final ItemStack cursor = inventoryEvent.getCursorTransaction().getFinal().createStack();
                    cursor.setQuantity(cursor.getQuantity() + increment);
                    TrackingUtil.handleCustomCursor(player, cursor.createSnapshot());
                } else if (inventoryEvent instanceof ClickInventoryEvent.Double && !(inventoryEvent instanceof ClickInventoryEvent.Shift)) {
                    int decrement;

                    decrement = slotTransactions.stream().filter((t) -> !t.isValid()).collect(Collectors.summingInt((t) -> t.getOriginal()
                            .getQuantity()));

                    final ItemStack cursor = inventoryEvent.getCursorTransaction().getFinal().createStack();
                    cursor.setQuantity(cursor.getQuantity() - decrement);
                    TrackingUtil.handleCustomCursor(player, cursor.createSnapshot());
                }
                SpawnEntityEvent spawnEntityEvent = fireSpawnEntityEvent(inventoryEvent, cause, context.getCapturedEntities());
                if (spawnEntityEvent != null) {
                    TrackingUtil.processSpawnedEntities(player, spawnEntityEvent);
                }
            }
        }
        slotTransactions.clear();
        mixinContainer.setCaptureInventory(false);
    }

    protected boolean shouldValidateTransactions() {
        return true;
    }

    @Nullable
    protected SpawnEntityEvent fireSpawnEntityEvent(ClickInventoryEvent inventoryEvent, Cause cause, List<Entity> entities) {
        if (entities.isEmpty()) {
            return null;
        }
        SpawnEntityEvent spawnEntityEvent = SpongeEventFactory.createSpawnEntityEvent(cause, entities);
        SpongeImpl.postEvent(spawnEntityEvent);
        if (spawnEntityEvent.isCancelled()) {
            return null;
        }
        return spawnEntityEvent;
    }

    protected void onCancelled(ClickInventoryEvent event) {
    }

    public boolean matches(int packetState) {
        return (packetState & this.stateMask & this.stateId) == (packetState & this.stateMask);
    }

}
