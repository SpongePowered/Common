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
package org.spongepowered.common.inventory.custom;

import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.menu.ClickTypes;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.menu.handler.ClickHandler;
import org.spongepowered.api.item.inventory.menu.handler.CloseHandler;
import org.spongepowered.api.item.inventory.menu.handler.InventoryCallbackHandler;
import org.spongepowered.api.item.inventory.menu.handler.KeySwapHandler;
import org.spongepowered.api.item.inventory.menu.handler.SlotChangeHandler;
import org.spongepowered.api.item.inventory.menu.handler.SlotClickHandler;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.common.accessor.world.inventory.AbstractContainerMenuAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.world.inventory.container.MenuBridge;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.inventory.adapter.impl.slots.SlotAdapter;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class SpongeInventoryMenu implements InventoryMenu {

    private ViewableInventory inventory;
    private Map<Container, ServerPlayer> tracked = new HashMap<>();
    private Component title;

    private @Nullable SlotClickHandler slotClickHandler;
    private @Nullable ClickHandler clickHandler;
    private @Nullable KeySwapHandler keySwapHandler;
    private @Nullable SlotChangeHandler changeHandler;
    private @Nullable CloseHandler closeHandler;

    private boolean readonly; // TODO make with wrapper slot
    private final HashMap<Integer, Boolean> slotReadOnly = new HashMap<>();

    public SpongeInventoryMenu(final ViewableInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public ViewableInventory inventory() {
        return this.inventory;
    }

    @Override
    public void setCurrentInventory(final ViewableInventory inventory) {
        this.slotReadOnly.clear();
        if (inventory.getClass().equals(this.inventory.getClass()) && inventory instanceof ViewableCustomInventory && inventory.capacity() == this.inventory.capacity()) {
            this.inventory = inventory;
            for (Map.Entry<Container, ServerPlayer> entry : this.tracked.entrySet()) {
                final net.minecraft.world.inventory.AbstractContainerMenu container = (net.minecraft.world.inventory.AbstractContainerMenu) entry.getKey();
                final ServerPlayer player = entry.getValue();
                // create a new container for the viewable inventory
                final net.minecraft.world.inventory.AbstractContainerMenu newContainer = ((ViewableCustomInventory) inventory).createMenu(-1, ((Player)player).getInventory(), (Player) player);
                for (int i = 0; i < inventory.capacity(); i++) {
                    // And put its slots into the old container
                    final Slot slot = newContainer.slots.get(i);
                    container.slots.set(i, slot);
                    // Update Container items
                    ((AbstractContainerMenuAccessor)container).accessor$lastSlots().set(i, slot.getItem());
                    // send update to Client
                    for (final ContainerListener listener : ((AbstractContainerMenuAccessor) container).accessor$containerListeners()) {
                        listener.slotChanged(container, i, slot.getItem());
                    }
                }

            }
        } else {
            // Get all distinct players and reopen inventory for them
            this.inventory = inventory;
            this.reopen();
        }

    }

    private void reopen() {
        new ArrayList<>(this.tracked.values()).stream().distinct().forEach(this::open);
    }

    @Override
    public void setTitle(final Component title) {
        this.title = title;
        this.tracked.forEach((c, p) -> {
            var container = (AbstractContainerMenu) c;
            var connection = (ServerGamePacketListenerImpl) p.connection();
            var packet = new ClientboundOpenScreenPacket(container.containerId, container.getType(), SpongeAdventure.asVanilla(this.title));
            connection.send(packet);
            container.broadcastFullState();
        });
    }

    @Override
    public Optional<Component> title() {
        return Optional.ofNullable(this.title);
    }

    @Override
    public InventoryMenu setReadOnly(final boolean readOnly) {
        this.readonly = readOnly;
        return this;
    }

    @Override
    public InventoryMenu setReadOnly(final org.spongepowered.api.item.inventory.Slot slot, final boolean readOnly) {
        if (!this.inventory.containsInventory(slot)) {
            throw new IllegalArgumentException();
        }
        this.slotReadOnly.put(((SlotAdapter) slot).getOrdinal(), readOnly);
        return this;
    }

    @Override
    public void registerHandler(final InventoryCallbackHandler handler) {
        if (handler instanceof ClickHandler) {
            this.registerClick(((ClickHandler) handler));
        }
        if (handler instanceof SlotClickHandler) {
            this.registerSlotClick(((SlotClickHandler) handler));
        }
        if (handler instanceof KeySwapHandler) {
            this.registerKeySwap(((KeySwapHandler) handler));
        }
        if (handler instanceof CloseHandler) {
            this.registerClose(((CloseHandler) handler));
        }
        if (handler instanceof SlotChangeHandler) {
            this.registerChange(((SlotChangeHandler) handler));
        }
    }

    public boolean isReadOnly(final int slot) {
        return this.slotReadOnly.getOrDefault(slot, this.readonly);
    }

    @Override
    public void registerClose(final CloseHandler handler) {
        this.closeHandler = handler;
    }

    @Override
    public void registerClick(final ClickHandler handler) {
        this.clickHandler = handler;
    }

    @Override
    public void registerSlotClick(final SlotClickHandler handler) {
        this.slotClickHandler = handler;
    }

    @Override
    public void registerKeySwap(final KeySwapHandler handler) {
        this.keySwapHandler = handler;
    }

    @Override
    public void registerChange(final SlotChangeHandler handler) {
        this.changeHandler = handler;
    }

    @Override
    public void unregisterAll() {
        this.clickHandler = null;
        this.slotClickHandler = null;
        this.keySwapHandler = null;
        this.changeHandler = null;
        this.closeHandler = null;
    }

    @Override
    public Optional<Container> open(final ServerPlayer player) {
        final Optional<Container> container = player.openInventory(this.inventory, this.title);
        container.ifPresent(c -> {
            ((MenuBridge)c).bridge$setMenu(this);
            this.tracked.put(c, player);
        });
        return container;
    }

    public void onClose(final Player player, final Container container) {

        if (this.closeHandler != null) {
            try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
                frame.pushCause(player);
                final Cause cause = frame.currentCause();
                this.closeHandler.handle(cause, container);
            }
        }
        this.tracked.remove(container);
    }

    public boolean onClick(
        final int slotId, final int dragType, final ClickType clickTypeIn, final Player player, final Container container) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(player);
            final Cause cause = frame.currentCause();
            if (clickTypeIn == ClickType.QUICK_CRAFT) {
                return this.onClickDrag(cause, slotId, dragType, container);
            }
            final Optional<org.spongepowered.api.item.inventory.Slot> slot = container.slot(slotId);

            if (slot.isPresent()) {
                switch (clickTypeIn) {
                    case SWAP:
                        if (dragType >= 0 && dragType < 9 || dragType == 40) {
                            final Optional<org.spongepowered.api.item.inventory.Slot> slot2 =
                                    ((org.spongepowered.api.entity.living.player.Player) player).inventory().slot(dragType);
                            if (slot2.isPresent() && this.keySwapHandler != null) {
                                return this.keySwapHandler.handle(cause, container, slot.get(), slotId, ClickTypes.KEY_SWAP.get(), slot2.get());
                            }
                        }
                        break;
                    case CLONE:
                        if (this.slotClickHandler != null) {
                            return this.slotClickHandler.handle(cause, container, slot.get(), slotId, ClickTypes.CLICK_MIDDLE.get());
                        }
                        break;
                    case PICKUP_ALL:
                        if (this.slotClickHandler != null) {
                            return this.slotClickHandler.handle(cause, container, slot.get(), slotId, ClickTypes.DOUBLE_CLICK.get());
                        }
                        break;
                    default:
                        if (this.slotClickHandler != null) {
                            if (dragType == 0) {
                                return this.onClickLeft(cause, this.slotClickHandler, clickTypeIn, container, slotId, slot.get());
                            } else if (dragType == 1) {
                                return this.onClickRight(cause, this.slotClickHandler, clickTypeIn, container, slotId, slot.get());
                            }
                            // else unknown drag-type
                        }
                }
                return true;
            }
            // else no slot present
            switch (clickTypeIn) {
                case PICKUP:
                    if (slotId == -999 && this.clickHandler != null) {
                        if (dragType == 0) {return this.clickHandler.handle(cause, container, ClickTypes.CLICK_LEFT_OUTSIDE.get());
                        } else if (dragType == 1) {  return this.clickHandler.handle(cause, container, ClickTypes.CLICK_RIGHT_OUTSIDE.get());
                        } }  // else unknown slotId/drag-type
                    break;
                case THROW:
                    if (slotId == -999) {
                        // TODO check packets - does THROW with slotid -999 exist or is this actually PICKUP?
                        // its supposed to be l/r-click with nothing in hand
                        // TODO check if those exist
                        ///**
                        // * Throwing one item on the cursor by clicking outside the inventory window.
                        // */
                        //public static final org.spongepowered.api.item.inventory.menu.ClickType
                        //        CLICK_THROW_ONE = DummyObjectProvider.createFor(org.spongepowered.api.item.inventory.menu.ClickType.class, "click_throw_one");
                        ///**
                        // * Throwing all items on the cursor by clicking outside the inventory window.
                        // */
                        //public static final org.spongepowered.api.item.inventory.menu.ClickType
                        //        CLICK_THROW_ALL = DummyObjectProvider.createFor(org.spongepowered.api.item.inventory.menu.ClickType.class, "click_throw_all");
                    }        // else unknown slotId/drag-type
                    break;
            }
            return true;
        }
    }

    private boolean onClickDrag(final Cause cause, final int slotId, final int dragType, final Container container) {

        final int dragMode = dragType >> 2 & 3; // (0 : evenly split, 1 : one item by slot, 2 : creative)
        final int dragEvent = dragType & 3; // (0 : start drag, 1 : add slot, 2 : end drag)

        switch (dragEvent) {
            case 0: // start drag
                if (this.clickHandler != null) {
                    return this.clickHandler.handle(cause, container, ClickTypes.DRAG_START.get());
                }
                break;
            case 1: // add drag
                final Optional<org.spongepowered.api.item.inventory.Slot> slot = container.slot(slotId);
                if (slot.isPresent() && this.slotClickHandler != null) {
                    switch (dragMode) {
                        case 0:
                            return this.slotClickHandler.handle(cause, container, slot.get(), slotId, ClickTypes.DRAG_LEFT_ADD.get());
                        case 1:
                            return this.slotClickHandler.handle(cause, container, slot.get(), slotId, ClickTypes.DRAG_RIGHT_ADD.get());
                        case 2:
                            return this.slotClickHandler.handle(cause, container, slot.get(), slotId, ClickTypes.DRAG_MIDDLE_ADD.get());
                    }
                }
                break;
            case 2: // end drag
                if (this.clickHandler != null) {
                    return this.clickHandler.handle(cause, container, ClickTypes.DRAG_END.get());
                }
                break;
        }
        return true;
    }

    private boolean onClickRight(
        final Cause cause, final SlotClickHandler handler, final ClickType clickTypeIn, final Container container,
                                 final int idx, final org.spongepowered.api.item.inventory.Slot slot) {
        switch (clickTypeIn) {
            case PICKUP:
                return handler.handle(cause, container, slot, idx, ClickTypes.CLICK_RIGHT.get());
            case QUICK_MOVE:
                return handler.handle(cause, container, slot, idx, ClickTypes.SHIFT_CLICK_RIGHT.get());
            case THROW:
                // TODO empty cursor check?
                return handler.handle(cause, container, slot, idx, ClickTypes.KEY_THROW_ALL.get());

        }
        return true;
    }

    private Boolean onClickLeft(
        final Cause cause, final SlotClickHandler handler, final ClickType clickTypeIn, final Container container,
                                final int idx, final org.spongepowered.api.item.inventory.Slot slot) {
        switch (clickTypeIn) {
            case PICKUP:
                return handler.handle(cause, container, slot, idx, ClickTypes.CLICK_LEFT.get());
            case QUICK_MOVE:
                return handler.handle(cause, container, slot, idx, ClickTypes.SHIFT_CLICK_LEFT.get());
            case THROW:
                // TODO empty cursor check?
                return handler.handle(cause, container, slot, idx, ClickTypes.KEY_THROW_ONE.get());
        }
        return true;
    }

    public boolean onChange(final ItemStack newStack, final ItemStack oldStack, final Container container, final int slotIndex, final Slot slot) {

        if (this.changeHandler != null) {
            final Cause cause = PhaseTracker.getCauseStackManager().currentCause();
            return this.changeHandler.handle(cause, container, ((org.spongepowered.api.item.inventory.Slot) slot), slotIndex,
                    ItemStackUtil.snapshotOf(oldStack), ItemStackUtil.snapshotOf(newStack));
        }
        // readonly by default cancels top inventory changes . but can be overridden by change callbacks
        return !this.isReadOnly(slotIndex) || slot.container instanceof PlayerInventory;
    }

}
