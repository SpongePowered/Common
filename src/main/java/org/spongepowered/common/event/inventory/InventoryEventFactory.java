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
package org.spongepowered.common.event.inventory;

import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.EnchantItemEvent;
import org.spongepowered.api.event.item.inventory.TransferInventoryEvent;
import org.spongepowered.api.event.item.inventory.UpdateAnvilEvent;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.world.inventory.container.ContainerBridge;
import org.spongepowered.common.bridge.world.inventory.container.TrackedInventoryBridge;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.context.transaction.TransactionalCaptureSupplier;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhaseUtil;
import org.spongepowered.common.inventory.util.ContainerUtil;
import org.spongepowered.common.inventory.util.InventoryUtil;
import org.spongepowered.common.item.enchantment.SpongeRandomEnchantmentListBuilder;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

public class InventoryEventFactory {

    public static boolean callPlayerInventoryPickupEvent(final Player player, final ItemEntity itemToPickup) {
        final ItemStack stack = itemToPickup.getItem();
        final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(stack);
        final PhaseTracker phaseTracker = PhaseTracker.getWorldInstance((ServerLevel) player.level());
        final ChangeInventoryEvent.Pickup.Pre event =
                SpongeEventFactory.createChangeInventoryEventPickupPre(
                    phaseTracker.currentCause(),
                        Optional.empty(), Collections.singletonList(snapshot), ((Inventory) player.containerMenu), (Item) itemToPickup, snapshot);
        SpongeCommon.post(event);
        if (event.isCancelled()) {
            return false;
        }
        if (event.custom().isPresent()) {
            final List<ItemStackSnapshot> list = event.custom().get();
            if (list.isEmpty()) {
                stack.setCount(0);
                return true;
            }

            final PhaseContext<@NonNull ?> context = phaseTracker.getPhaseContext();
            final TransactionalCaptureSupplier transactor = context.getTransactor();
            try (final EffectTransactor ignored = transactor.logPlayerInventoryChangeWithEffect(player, PlayerInventoryTransaction.EventCreator.PICKUP)) {
                for (final ItemStackSnapshot item : list) {
                    final org.spongepowered.api.item.inventory.ItemStack itemStack = item.asMutable();
                    player.getInventory().add(ItemStackUtil.toNative(itemStack));
                    if (!itemStack.isEmpty()) {
                        // Modified pickup items do not fit inventory - pre-cancel ChangeInventoryEvent.Pickup
                        ignored.parent.markCancelled();
                        return false;
                    }
                }
            }

            if (!TrackingUtil.processBlockCaptures(context)) {
                return false;
            }
            stack.setCount(0);
            return true;
        } else {
            final PhaseContext<@NonNull ?> context = phaseTracker.getPhaseContext();
            final TransactionalCaptureSupplier transactor = context.getTransactor();
            final boolean added;
            try (final EffectTransactor ignored = transactor.logPlayerInventoryChangeWithEffect(player, PlayerInventoryTransaction.EventCreator.PICKUP)) {
                added = player.getInventory().add(stack);
            }

            if (!TrackingUtil.processBlockCaptures(context)) {
                return false;
            }
            return added;
        }
    }

    public static ItemStack callHopperInventoryPickupEvent(final Container inventory, final ItemEntity item, final ItemStack stack) {
        final PhaseTracker phaseTracker = PhaseTracker.getWorldInstance((ServerLevel) item.level());
        try (final CauseStackManager.StackFrame frame = phaseTracker.pushCauseFrame()) {
            frame.pushCause(inventory);

            final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(stack);
            final ChangeInventoryEvent.Pickup.Pre event =
                    SpongeEventFactory.createChangeInventoryEventPickupPre(frame.currentCause(),
                            Optional.empty(), Collections.singletonList(snapshot), (Inventory) inventory, (Item) item, snapshot);
            SpongeCommon.post(event);
            if (event.isCancelled()) {
                return stack;
            }

            final int size = inventory.getContainerSize();
            final ItemStack[] prevInventory = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                prevInventory[i] = inventory.getItem(i);
            }

            if (event.custom().isPresent()) {
                if (event.custom().get().isEmpty()) {
                    return ItemStack.EMPTY;
                }

                boolean fullTransfer = true;
                for (final ItemStackSnapshot snap : event.custom().get()) {
                    final ItemStack stackToAdd = ItemStackUtil.fromSnapshotToNative(snap);
                    final ItemStack remaining = HopperBlockEntity.addItem(null, inventory, stackToAdd, null);
                    if (!remaining.isEmpty()) {
                        fullTransfer = false;
                        break;
                    }
                }
                if (!fullTransfer) {
                    for (int i = 0; i < prevInventory.length; i++) {
                        inventory.setItem(i, prevInventory[i]);
                    }
                    return stack;
                }

                if (InventoryEventFactory.callInventoryPickupEvent(inventory, prevInventory)) {
                    return ItemStack.EMPTY;
                }
                return stack;
            } else {
                final ItemStack remainder = HopperBlockEntity.addItem(null, inventory, stack, null);
                if (InventoryEventFactory.callInventoryPickupEvent(inventory, prevInventory)) {
                    return remainder;
                }
                return stack;
            }
        }
    }

    private static boolean callInventoryPickupEvent(final Container inventory, final ItemStack[] prevInventory) {
        final Inventory spongeInventory = InventoryUtil.toInventory(inventory, null);
        final List<SlotTransaction> trans = InventoryEventFactory.generateTransactions(spongeInventory, inventory, prevInventory);
        if (trans.isEmpty()) {
            return true;
        }
        final ChangeInventoryEvent.Pickup event = SpongeEventFactory.createChangeInventoryEventPickup(PhaseTracker.getCauseStackManager().currentCause(), spongeInventory, trans);
        SpongeCommon.post(event);
        PacketPhaseUtil.handleSlotRestore(null, null, event.transactions(), event.isCancelled());
        return !event.isCancelled();
    }

    private static List<SlotTransaction> generateTransactions(final @Nullable Inventory inv, final Container inventory, final ItemStack[] previous) {
        if (inv == null) {
            return Collections.emptyList();
        }
        final List<SlotTransaction> trans = new ArrayList<>();
        List<org.spongepowered.api.item.inventory.Slot> slots = inv.slots();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            final org.spongepowered.api.item.inventory.Slot slot = slots.get(i);
            final ItemStack newStack = inventory.getItem(i);
            final ItemStack prevStack = previous[i];
            if (!ItemStack.matches(newStack, prevStack)) {
                trans.add(new SlotTransaction(slot, ItemStackUtil.snapshotOf(prevStack), ItemStackUtil.snapshotOf(newStack)));
            }
        }
        return trans;
    }

    public static boolean callInteractContainerOpenEvent(final ServerPlayer player) {
        final ItemStackSnapshot newCursor = ItemStackUtil.snapshotOf(player.containerMenu.getCarried());
        final Transaction<ItemStackSnapshot> cursorTransaction = new Transaction<>(ItemStackSnapshot.empty(), newCursor);
        final InteractContainerEvent.Open event =
                SpongeEventFactory.createInteractContainerEventOpen(PhaseTracker.getWorldInstance(player.serverLevel()).currentCause(),
                        (org.spongepowered.api.item.inventory.Container) player.containerMenu, cursorTransaction);
        SpongeCommon.post(event);
        if (event.isCancelled()) {
            player.closeContainer();
            return false;
        }
        // Custom cursor
        PacketPhaseUtil.handleCursorRestore(player, event.cursorTransaction(), event.isCancelled());
        return true;
    }

    public static @org.checkerframework.checker.nullness.qual.Nullable AbstractContainerMenu displayContainer(final ServerPlayer player, final Inventory inventory, final Component displayName) {
        final net.minecraft.world.inventory.AbstractContainerMenu previousContainer = player.containerMenu;
        final net.minecraft.world.inventory.AbstractContainerMenu container;

        Optional<ViewableInventory> viewable = inventory.asViewable();
        if (viewable.isPresent()) {
            if (viewable.get() instanceof MenuProvider namedContainerProvider) {
                if (displayName != null) {
                    namedContainerProvider = new SimpleMenuProvider(namedContainerProvider, SpongeAdventure.asVanilla(displayName));
                }
                final OptionalInt containerId = player.openMenu(namedContainerProvider);
                if (containerId.isPresent() && player.containerMenu instanceof MerchantMenu merchantMenu) {
                    player.sendMerchantOffers(containerId.getAsInt(), merchantMenu.getOffers(), 0, 0, false, false);
                }
            } else if (viewable.get() instanceof Merchant merchant) {
                net.minecraft.network.chat.Component display = null;
                int level = 0;
                if (merchant instanceof Villager villager) {
                    display = villager.getDisplayName();
                    level = villager.getVillagerData().level();
                } else if (merchant instanceof WanderingTrader trader) {
                    display = trader.getDisplayName();
                    level = 1;
                }
                if (displayName != null) {
                    display = SpongeAdventure.asVanilla(displayName);
                }
                OptionalInt containerId = player.openMenu(new SimpleMenuProvider((id, playerInv, p) ->
                        new MerchantMenu(id, playerInv, merchant), display));
                if (containerId.isPresent() && !merchant.getOffers().isEmpty()) {
                    player.sendMerchantOffers(containerId.getAsInt(), merchant.getOffers(), level, merchant.getVillagerXp(), merchant.showProgressBar(), merchant.canRestock());
                }
            }
        } else if (inventory instanceof CarriedInventory<?> carriedInventory && carriedInventory.carrier().orElse(null) instanceof AbstractHorse horse) {
            horse.openCustomInventoryScreen(player);
        }

        container = player.containerMenu;

        if (previousContainer == container) {
            return null;
        }

        if (!InventoryEventFactory.callInteractContainerOpenEvent(player)) {
            return null;
        }

        if (container instanceof ContainerBridge) {
            // This overwrites the normal container behaviour and allows viewing
            // inventories that are more than 8 blocks away
            // This currently actually only works for the Containers mixed into
            // by InteractableContainerMixin ; but throws no errors for other
            // containers

            // Allow viewing inventory; except when dead
            ((ContainerBridge) container).bridge$setCanInteractWith(p -> !p.isRemoved());
        }

        return container;
    }

    public static TransferInventoryEvent.Pre callTransferPre(final Inventory source, final Inventory destination) {
        PhaseTracker.getCauseStackManager().pushCause(source);
        final TransferInventoryEvent.Pre event = SpongeEventFactory.createTransferInventoryEventPre(
                PhaseTracker.getCauseStackManager().currentCause(), source, destination);
        SpongeCommon.post(event);
        PhaseTracker.getCauseStackManager().popCause();
        return event;
    }

    public static void callTransferPost(final @Nullable TrackedInventoryBridge captureSource, final @Nullable Inventory source,
            final @Nullable Inventory destination, ItemStack sourceStack, SlotTransaction sourceSlotTransaction) {
        // TODO make sure we never got null
        if (captureSource == null || source == null || destination == null || sourceSlotTransaction == null) {
            return;
        }
        PhaseTracker.getCauseStackManager().pushCause(source);
        List<SlotTransaction> slotTransactions = captureSource.bridge$getCapturedSlotTransactions();

        sourceStack = sourceStack.copy();
        sourceStack.setCount(1);
        ItemStackSnapshot transferredStack = ItemStackUtil.snapshotOf(sourceStack);

        Slot sourceSlot = sourceSlotTransaction.slot();
        Slot targetSlot = null;
        // There should only be 2 transactions - the other is the transaction on the target slot
        for (SlotTransaction transaction : slotTransactions) {
            if (transaction != sourceSlotTransaction) {
                targetSlot = transaction.slot();
                break;
            }
        }

        final TransferInventoryEvent.Post event =
                SpongeEventFactory.createTransferInventoryEventPost(PhaseTracker.getCauseStackManager().currentCause(),
                        source, sourceSlot, destination, targetSlot, transferredStack);
        SpongeCommon.post(event);
        slotTransactions.clear();
        PhaseTracker.getCauseStackManager().popCause();
    }


    /**
     * Captures a transaction
     *
     * @param captureIn the {@link TrackedInventoryBridge} to capture the transaction in
     * @param inv the Inventory
     * @param index the affected SlotIndex
     * @param originalStack the original Stack
     */
    public static SlotTransaction captureTransaction(final @Nullable TrackedInventoryBridge captureIn, final @Nullable Inventory inv, final int index, final ItemStack originalStack) {
        // TODO make sure we never got null
        if (captureIn == null || inv == null) {
            return null;
        }

        Optional<org.spongepowered.api.item.inventory.Slot> slot = inv.slot(index);
        if (slot.isPresent()) {
            SlotTransaction trans = new SlotTransaction(slot.get(),
                    ItemStackUtil.snapshotOf(originalStack),
                    ItemStackUtil.snapshotOf(slot.get().peek()));
            captureIn.bridge$getCapturedSlotTransactions().add(trans);
            return trans;
        } else {
            SpongeCommon.logger().warn("Unable to capture transaction from " + inv.getClass() + " at index " + index);
            return null;
        }
    }



    /**
     * Captures a transaction
     *
     * @param captureIn the {@link TrackedInventoryBridge} to capture the transaction in
     * @param inv the Inventory
     * @param index the affected SlotIndex
     * @param transaction the transaction to execute
     * @return the result if the transaction
     */
    public static ItemStack captureTransaction(final @Nullable TrackedInventoryBridge captureIn, final @Nullable Inventory inv, final int index, final Supplier<ItemStack> transaction) {
        // TODO make sure we never got null
        if (captureIn == null || inv == null) {
            return transaction.get();
        }

        Optional<org.spongepowered.api.item.inventory.Slot> slot = inv.slot(index);
        if (!slot.isPresent()) {
            SpongeCommon.logger().warn("Unable to capture transaction from " + inv.getClass() + " at index " + index);
            return transaction.get();
        }
        ItemStackSnapshot original = ItemStackUtil.snapshotOf(slot.get().peek());
        ItemStack remaining = transaction.get();
        if (remaining.isEmpty()) {
            ItemStackSnapshot replacement = ItemStackUtil.snapshotOf(slot.get().peek());
            captureIn.bridge$getCapturedSlotTransactions().add(new SlotTransaction(slot.get(), original, replacement));
        }
        return remaining;
    }

    public static UpdateAnvilEvent callUpdateAnvilEvent(final AnvilMenu anvil, final ItemStack slot1, final ItemStack slot2, final ItemStack result, final String name, final int levelCost, final int materialCost) {
        final Transaction<ItemStackSnapshot> transaction = new Transaction<>(ItemStackSnapshot.empty(), ItemStackUtil.snapshotOf(result));
        final UpdateAnvilEventCost costs = new UpdateAnvilEventCost(levelCost, materialCost);
        final UpdateAnvilEvent event = SpongeEventFactory.createUpdateAnvilEvent(PhaseTracker.getCauseStackManager().currentCause(),
                new Transaction<>(costs, costs), (Inventory)anvil, name, ItemStackUtil.snapshotOf(slot1), transaction, ItemStackUtil.snapshotOf(slot2));
        SpongeCommon.post(event);
        return event;
    }

    public static ChangeEntityEquipmentEvent callChangeEntityEquipmentEvent(
            final LivingEntity entity, final ItemStackSnapshot before, final ItemStackSnapshot after, final Slot slot) {
        final ChangeEntityEquipmentEvent event;
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(entity);
            final Transaction<ItemStackSnapshot> transaction = new Transaction<>(before, after);
            if (after.isEmpty()) {
                event = SpongeEventFactory.createChangeEntityEquipmentEventBreak(frame.currentCause(), (Entity) entity, slot, transaction);
            } else {
                event = SpongeEventFactory.createChangeEntityEquipmentEvent(frame.currentCause(), (Entity)  entity, slot, transaction);
            }
            SpongeCommon.post(event);
            return event;
        }
    }


    public static int callEnchantEventLevelRequirement(EnchantmentMenu container, int seed, int option, int power, ItemStack itemStack, int levelRequirement) {
        org.spongepowered.api.item.inventory.Container enchantContainer = ContainerUtil.fromNative(container);

        Player viewer = (Player) enchantContainer.viewer();
        ItemStackSnapshot cursor = ItemStackUtil.snapshotOf(viewer.containerMenu.getCarried());
        Transaction<ItemStackSnapshot> cursorTrans = new Transaction<>(cursor, cursor);

        EnchantItemEvent.CalculateLevelRequirement event =
                SpongeEventFactory.createEnchantItemEventCalculateLevelRequirement(PhaseTracker.getCauseStackManager().currentCause(),
                        levelRequirement, levelRequirement, enchantContainer, cursorTrans, ItemStackUtil.snapshotOf(itemStack), option, power, seed);

        SpongeCommon.post(event);

        return event.levelRequirement();
    }

    public static List<EnchantmentInstance> callEnchantEventEnchantmentList(EnchantmentMenu container,
            int seed, ItemStack itemStack, int option, int level, List<EnchantmentInstance> list) {

        List<Enchantment> enchList = Collections.unmodifiableList(SpongeRandomEnchantmentListBuilder.fromNative(list));

        org.spongepowered.api.item.inventory.Container enchantContainer = ContainerUtil.fromNative(container);

        Player viewer = (Player) enchantContainer.viewer();
        ItemStackSnapshot cursor = ItemStackUtil.snapshotOf(viewer.containerMenu.getCarried());
        Transaction<ItemStackSnapshot> cursorTrans = new Transaction<>(cursor, cursor);

        EnchantItemEvent.CalculateEnchantment event =
                SpongeEventFactory.createEnchantItemEventCalculateEnchantment(PhaseTracker.getCauseStackManager().currentCause(),
                        enchList, enchList, enchantContainer, cursorTrans, ItemStackUtil.snapshotOf(itemStack), level, option, seed);

        SpongeCommon.post(event);

        if (event.enchantments() != event.originalEnchantments()) {
            return SpongeRandomEnchantmentListBuilder.toNative(event.enchantments());
        }
        return list;
    }


    public static EnchantItemEvent.Post callEnchantEventEnchantPost(Player playerIn, EnchantmentMenu container,
            SlotTransaction enchantedItem, SlotTransaction lapisItem, int option, int seed) {
        org.spongepowered.api.item.inventory.Container enchantContainer = ContainerUtil.fromNative(container);

        ItemStackSnapshot cursor = ItemStackUtil.snapshotOf(playerIn.containerMenu.getCarried());
        Transaction<ItemStackSnapshot> cursorTrans = new Transaction<>(cursor, cursor);

        List<SlotTransaction> slotTrans = new ArrayList<>();
        slotTrans.add(lapisItem);
        slotTrans.add(enchantedItem);

        EnchantItemEvent.Post event =
                SpongeEventFactory.createEnchantItemEventPost(PhaseTracker.getCauseStackManager().currentCause(), enchantContainer,
                        cursorTrans, enchantedItem.slot(), Optional.empty(),slotTrans, option, seed);

        SpongeCommon.post(event);

        PacketPhaseUtil.handleSlotRestore(playerIn, container, event.transactions(), event.isCancelled());
        PacketPhaseUtil.handleCursorRestore(playerIn, event.cursorTransaction(), event.isCancelled());
        return event;
    }


}
