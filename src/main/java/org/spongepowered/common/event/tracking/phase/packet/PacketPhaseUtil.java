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

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.common.accessor.entity.passive.AbstractChestedHorseEntityAccessor;
import org.spongepowered.common.accessor.network.protocol.game.ServerboundMovePlayerPacketAccessor;
import org.spongepowered.common.accessor.world.entity.EntityAccessor;
import org.spongepowered.common.accessor.world.entity.animal.PigAccessor;
import org.spongepowered.common.accessor.world.entity.animal.SheepAccessor;
import org.spongepowered.common.accessor.world.entity.animal.WolfAccessor;
import org.spongepowered.common.accessor.world.inventory.SlotAccessor;
import org.spongepowered.common.bridge.network.protocol.PacketBridge;
import org.spongepowered.common.bridge.world.level.block.TrackableBlockBridge;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.block.BlockPhase;
import org.spongepowered.common.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.inventory.adapter.impl.slots.SlotAdapter;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.List;

public final class PacketPhaseUtil {

    @SuppressWarnings("removal")
    public static void handleSlotRestore(@Nullable final Player player, final @Nullable AbstractContainerMenu containerMenu, final List<SlotTransaction> slotTransactions, final boolean eventCancelled) {
        final PhaseTracker phaseTracker = player != null
            ? PhaseTracker.getWorldInstance((ServerLevel) player.level())
            : PhaseTracker.getWorldInstance();
        try (PhaseContext<@NonNull ?> ignored = BlockPhase.State.RESTORING_BLOCKS.createPhaseContext(phaseTracker).buildAndSwitch()) {
            boolean restoredAny = false;
            for (final SlotTransaction slotTransaction : slotTransactions) {

                if ((!slotTransaction.custom().isPresent() && slotTransaction.isValid()) && !eventCancelled) {
                    continue;
                }
                restoredAny = true;
                final org.spongepowered.api.item.inventory.Slot slot = slotTransaction.slot();
                final ItemStackSnapshot snapshot = eventCancelled || !slotTransaction.isValid() ? slotTransaction.original() : slotTransaction.custom().get();
                if (containerMenu == null || slot.viewedSlot() instanceof Slot) {
                    slot.set(snapshot);
                } else if (player instanceof ServerPlayer serverPlayer
                        && containerMenu != player.inventoryMenu && serverPlayer.inventory().containsInventory(slot)) {
                    final org.spongepowered.api.item.inventory.ItemStack stack = snapshot.asMutable();
                    slot.set(stack);
                    ((net.minecraft.server.level.ServerPlayer) player).connection.send(
                            new ClientboundSetPlayerInventoryPacket(((SlotAdapter) slot).getOrdinal(), ItemStackUtil.toNative(stack)));
                } else {
                    final int slotNumber = ((SlotAdapter) slot).getOrdinal();
                    final Slot nmsSlot = containerMenu.getSlot(slotNumber);
                    if (nmsSlot != null) {
                        nmsSlot.set(ItemStackUtil.fromSnapshotToNative(snapshot));
                    }
                }
            }
            if (restoredAny && player instanceof net.minecraft.server.level.ServerPlayer) {
                if (containerMenu != null) {
                    containerMenu.broadcastChanges();
                    if (player.containerMenu == containerMenu) {
                        containerMenu.sendAllDataToRemote();
                    }
                } else {
                    player.inventoryMenu.broadcastChanges();
                }
            }
        }
    }

    public static void handleCursorRestore(final Player player, final Transaction<ItemStackSnapshot> cursorTransaction, final boolean eventCancelled) {
        final ItemStackSnapshot cursorSnap;
        if (eventCancelled || !cursorTransaction.isValid()) {
            cursorSnap = cursorTransaction.original();
        } else if (cursorTransaction.custom().isPresent()) {
            cursorSnap = cursorTransaction.finalReplacement();
        } else {
            return;
        }
        final ItemStack cursor = ItemStackUtil.fromSnapshotToNative(cursorSnap);
        player.containerMenu.setCarried(cursor);
        player.containerMenu.setRemoteCarried(cursor);
        if (player instanceof net.minecraft.server.level.ServerPlayer) {
            ((net.minecraft.server.level.ServerPlayer) player).connection.send(new ClientboundSetCursorItemPacket(cursor));
        }
    }

    public static void handleCustomCursor(final Player player, final ItemStackSnapshot customCursor) {
        final ItemStack cursor = ItemStackUtil.fromSnapshotToNative(customCursor);
        player.containerMenu.setCarried(cursor);
        if (player instanceof net.minecraft.server.level.ServerPlayer) {
            ((net.minecraft.server.level.ServerPlayer) player).connection.send(new ClientboundSetCursorItemPacket(cursor));
        }
    }

    public static void validateCapturedTransactions(final int slotId, final AbstractContainerMenu containerMenu, final List<SlotTransaction> capturedTransactions) {
        if (capturedTransactions.size() == 0 && slotId >= 0 && slotId < containerMenu.slots.size()) {
            final Slot slot = containerMenu.getSlot(slotId);
            if (slot != null) {
                final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(slot.getItem());
                final SlotTransaction slotTransaction = new SlotTransaction(
                        ((InventoryAdapter) containerMenu).inventoryAdapter$getSlot(slotId).get(), snapshot, snapshot);
                capturedTransactions.add(slotTransaction);
            }
        }
    }

    public static void handlePlayerSlotRestore(final net.minecraft.server.level.ServerPlayer player, final ItemStack itemStack, final InteractionHand hand) {
        if (itemStack.isEmpty()) { // No need to check if it's NONE, NONE is checked by isEmpty.
            return;
        }

        player.containerMenu.suppressRemoteUpdates();
        int slotId = 0;
        if (hand == InteractionHand.OFF_HAND) {
            player.getInventory().offhand.set(0, itemStack);
            slotId = (player.getInventory().items.size() + Inventory.getSelectionSize());
        } else {
            player.getInventory().items.set(player.getInventory().selected, itemStack);
            // TODO check if window id -2 and slotid = player.getInventory().currentItem works instead of this:
            for (Slot containerSlot : player.containerMenu.slots) {
                if (containerSlot.container == player.getInventory() && ((SlotAccessor) containerSlot).accessor$slot() == player.getInventory().selected) {
                    slotId = containerSlot.index;
                    break;
                }
            }
        }

        player.containerMenu.broadcastChanges();
        player.containerMenu.resumeRemoteUpdates();
        player.connection.send(new ClientboundContainerSetSlotPacket(player.containerMenu.containerId, player.containerMenu.getStateId(), slotId, itemStack));
    }

    // Check if all transactions are invalid
    public static boolean allTransactionsInvalid(final List<SlotTransaction> slotTransactions) {
        if (slotTransactions.size() == 0) {
            return false;
        }

        for (final SlotTransaction slotTransaction : slotTransactions) {
            if (slotTransaction.isValid()) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
    public static void onProcessPacket(final Packet packetIn, final PacketListener netHandler) {
        if (netHandler instanceof ServerGamePacketListenerImpl) {
            net.minecraft.server.level.ServerPlayer packetPlayer = ((ServerGamePacketListenerImpl) netHandler).player;
            // Only process the CustomPayload, Respawn & ChunkBatchReceived packets from players if they are dead.
            if (!packetPlayer.isAlive() && !((PacketBridge) packetIn).bridge$canProcessWhenDead()) {
                return;
            }
            try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
                frame.pushCause(packetPlayer);

                // Don't process movement capture logic if player hasn't moved
                boolean ignoreMovementCapture;
                if (packetIn instanceof ServerboundMovePlayerPacket) {
                    final ServerboundMovePlayerPacket movingPacket = ((ServerboundMovePlayerPacket) packetIn);
                    if (movingPacket instanceof ServerboundMovePlayerPacket.Rot) {
                        ignoreMovementCapture = true;
                    } else if (packetPlayer.getX() == ((ServerboundMovePlayerPacketAccessor) movingPacket).accessor$x() && packetPlayer.getY() == ((ServerboundMovePlayerPacketAccessor) movingPacket).accessor$y() && packetPlayer.getZ() == ((ServerboundMovePlayerPacketAccessor) movingPacket).accessor$z()) {
                        ignoreMovementCapture = true;
                    } else {
                        ignoreMovementCapture = false;
                    }
                    // just a sanity check, if the entity is potentially colliding with some block
                    // we cannot ignore movement capture
                    if (ignoreMovementCapture) {
                        // Basically, we need to sanity check the nearby blocks because if they have
                        // any positional logic, we need to run captures
                        final AABB boundingBox = packetPlayer.getBoundingBox();
                        final BlockPos min = new BlockPos((int) (boundingBox.minX + 0.001D), (int) (boundingBox.minY + 0.001D), (int) (boundingBox.minZ + 0.001D));
                        final BlockPos max = new BlockPos((int) (boundingBox.maxX - 0.001D), (int) (boundingBox.maxY - 0.001D), (int) (boundingBox.maxZ - 0.001D));
                        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                        if (packetPlayer.level().hasChunksAt(min, max)) {
                            for(int x = min.getX(); x <= max.getX(); ++x) {
                                for(int y = min.getY(); y <= max.getY(); ++y) {
                                    for(int z = min.getZ(); z <= max.getZ(); ++z) {
                                        pos.set(x, y, z);
                                        final Block block = packetPlayer.level().getBlockState(pos).getBlock();
                                        if (((TrackableBlockBridge) block).bridge$hasEntityInsideLogic()) {
                                            ignoreMovementCapture = false;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    ignoreMovementCapture = false;
                }
                if (ignoreMovementCapture || (packetIn instanceof ServerboundClientInformationPacket)) {
                    packetIn.handle(netHandler);
                } else {
                    final IPhaseState<? extends PacketContext<?>> packetState = PacketPhase.getInstance().getStateForPacket(packetIn);
                    // At the very least make an unknown packet state case.
                    final PacketContext<?> context = packetState.createPhaseContext(PhaseTracker.getWorldInstance(packetPlayer.serverLevel()));
                    context.source(packetPlayer)
                           .packetPlayer(packetPlayer)
                           .packet(packetIn);
                    if (!PacketPhase.getInstance().isPacketInvalid(packetIn, packetPlayer, packetState)) {

                        PacketPhase.getInstance().populateContext(packetIn, packetPlayer, packetState, context);
                        context.creator(((ServerPlayer) packetPlayer).uniqueId());
                        context.notifier(((ServerPlayer) packetPlayer).uniqueId());
                    }
                    try (final PhaseContext<?> packetContext = context) {
                        packetContext.buildAndSwitch();
                        packetIn.handle(netHandler);
                    }
                }
            }
        } else { // client
            packetIn.handle(netHandler);
        }
    }

    /**
     * Attempts to find the {@link EntityDataAccessor} that was potentially modified
     * when a player interacts with an entity.
     *
     * @param stack The item the player is holding
     * @param entity The entity
     * @return A possible data parameter or null if unknown
     */
    public static @Nullable EntityDataAccessor<?> findModifiedEntityInteractDataParameter(final ItemStack stack, final Entity entity) {
        final Item item = stack.getItem();

        if (item instanceof DyeItem) {
            // ItemDye.itemInteractionForEntity
            if (entity instanceof Sheep) {
                return SheepAccessor.accessor$DATA_WOOL_ID();
            }

            // EntityWolf.processInteract
            if (entity instanceof Wolf) {
                return WolfAccessor.accessor$DATA_COLLAR_COLOR();
            }

            return null;
        }

        if (item == Items.NAME_TAG) {
            // ItemNameTag.itemInteractionForEntity
            return entity instanceof LivingEntity && !(entity instanceof Player) && stack.has(DataComponents.CUSTOM_NAME) ? EntityAccessor.accessor$DATA_CUSTOM_NAME() : null;
        }

        if (item == Items.SADDLE) {
            // ItemSaddle.itemInteractionForEntity
            return entity instanceof Pig ? PigAccessor.accessor$DATA_SADDLE_ID() : null;
        }

        if (item instanceof BlockItem && ((BlockItem) item).getBlock() == Blocks.CHEST) {
            // AbstractChestHorse.processInteract
            return entity instanceof AbstractChestedHorse ? AbstractChestedHorseEntityAccessor.accessor$DATA_ID_CHEST() : null;
        }

        return null;
    }
}
