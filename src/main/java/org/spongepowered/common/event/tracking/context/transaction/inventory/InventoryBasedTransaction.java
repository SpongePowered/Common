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
package org.spongepowered.common.event.tracking.context.transaction.inventory;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.player.Player;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.GameTransaction;
import org.spongepowered.common.event.tracking.context.transaction.type.TransactionTypes;
import org.spongepowered.common.event.tracking.context.transaction.world.SpawnEntityTransaction;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhaseUtil;
import org.spongepowered.common.util.PrettyPrinter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

abstract class InventoryBasedTransaction extends GameTransaction<ChangeInventoryEvent> {

    final Inventory inventory;
    @MonotonicNonNull List<net.minecraft.world.entity.Entity> entities;
    @MonotonicNonNull private List<SlotTransaction> acceptedTransactions;
    protected boolean used = false;

    protected InventoryBasedTransaction(final Inventory inventory) {
        super(TransactionTypes.CHANGE_INVENTORY_EVENT.get());
        this.inventory = inventory;
    }

    @Override
    protected boolean hasUnknownChainRequiringCancellation() {
        return true;
    }

    @Override
    public Optional<BiConsumer<PhaseContext<@NonNull ?>, CauseStackManager.StackFrame>> getFrameMutator(
        @Nullable final GameTransaction<@NonNull ?> parent
    ) {
        return Optional.of((context, frame) -> {
            frame.pushCause(this.inventory);
        });
    }

    @Override
    public Optional<ChangeInventoryEvent> generateEvent(
        final PhaseContext<@NonNull ?> context, @Nullable final GameTransaction<@NonNull ?> parent,
        final ImmutableList<GameTransaction<ChangeInventoryEvent>> gameTransactions, final Cause currentCause
    ) {
        final ImmutableList<InventoryBasedTransaction> containerBasedTransactions = gameTransactions.stream()
            .filter(tx -> tx instanceof InventoryBasedTransaction)
            .map(tx -> (InventoryBasedTransaction) tx)
            .filter(tx -> !tx.used)
            .collect(ImmutableList.toImmutableList());

        final List<SlotTransaction> slotTransactions = containerBasedTransactions
            .stream()
            .map(InventoryBasedTransaction::getSlotTransactions)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        for (InventoryBasedTransaction transaction : containerBasedTransactions) {
            transaction.used = true;
        }

        final List<Entity> entities = containerBasedTransactions.stream()
                .map(InventoryBasedTransaction::getEntitiesSpawned)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // TODO on pickup grouping does not work?
        final Map<Slot, List<SlotTransaction>> collected = slotTransactions.stream().collect(Collectors.groupingBy(SlotTransaction::slot));
        slotTransactions.clear();
        collected.values().forEach(list -> {
            final SlotTransaction first = list.get(0);
            if (list.size() > 1) {
                final ItemStackSnapshot last = list.get(list.size() - 1).defaultReplacement();
                slotTransactions.add(new SlotTransaction(first.slot(), first.original(), last));
            } else {
                slotTransactions.add(first);
            }
        });

        return containerBasedTransactions.stream()
            .map(t -> t.createInventoryEvent(slotTransactions, entities, context, currentCause))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    List<SlotTransaction> getSlotTransactions() {
        return this.acceptedTransactions == null ? Collections.emptyList() : this.acceptedTransactions;
    }

    Optional<ChangeInventoryEvent> createInventoryEvent(
            final List<SlotTransaction> slotTransactions,
            final List<Entity> entities, final PhaseContext<@NonNull ?> context,
            final Cause currentCause
    ) {
        return Optional.empty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    List<Entity> getEntitiesSpawned() {
        return this.entities == null ? Collections.emptyList() : (List<Entity>) (List) this.entities;
    }

    @Override
    public void restore(
        final PhaseContext<@NonNull ?> context,
        final ChangeInventoryEvent event
    ) {

    }

    protected void handleEventResults(final Player player, final ChangeInventoryEvent event) {
        PacketPhaseUtil.handleSlotRestore(player, null, event.transactions(), event.isCancelled());
        if (this.entities != null && event instanceof SpawnEntityEvent) {
            EntityUtil.despawnFilteredEntities(this.entities, (SpawnEntityEvent) event);
        }
    }

    @Override
    public boolean markCancelledTransactions(
        final ChangeInventoryEvent event,
        final ImmutableList<? extends GameTransaction<ChangeInventoryEvent>> gameTransactions
    ) {
        if (event.isCancelled()) {
            event.transactions().forEach(SlotTransaction::invalidate);
            return true;
        }
        boolean cancelledAny = false;
        for (final SlotTransaction transaction : event.transactions()) {
            if (!transaction.isValid()) {
                cancelledAny = true;
                for (final GameTransaction<ChangeInventoryEvent> gameTransaction : gameTransactions) {
                    ((InventoryBasedTransaction) gameTransaction).getSlotTransactions()
                        .forEach(tx -> {
                            if (tx == transaction) {
                                gameTransaction.markCancelled();
                            }
                        });
                }
            }
        }
        return cancelledAny;
    }

    @Override
    public boolean absorbSlotTransaction(
        final ContainerSlotTransaction slotTransaction
    ) {
        if (this.inventory == slotTransaction.menu) {
            if (this.acceptedTransactions == null) {
                this.acceptedTransactions = new ArrayList<>();
            }
            this.acceptedTransactions.add(slotTransaction.transaction);
            return true;
        }
        // or accept menu transactions as inventory transactions
        final SlotTransaction newTransaction = slotTransaction.transaction;
        if (newTransaction.slot().viewedSlot() != newTransaction.slot() && this.inventory == newTransaction.slot().viewedSlot().parent()) {
            if (this.acceptedTransactions == null) {
                this.acceptedTransactions = new ArrayList<>();
            }
            this.acceptedTransactions.add(new SlotTransaction(newTransaction.slot().viewedSlot(), newTransaction.original(), newTransaction.defaultReplacement()));
            return true;
        }
        return false;
    }

    @Override
    public boolean absorbSpawnEntity(final PhaseContext<@NonNull ?> context, final SpawnEntityTransaction spawn) {
        if (context.doesContainerCaptureEntitySpawn(spawn.entityToSpawn)) {
            if (this.entities == null) {
                this.entities = new LinkedList<>();
            }
            this.entities.add(spawn.entityToSpawn);
            return true;
        }
        return super.absorbSpawnEntity(context, spawn);
    }

    @Override
    public void addToPrinter(final PrettyPrinter printer) {

    }

    @Override
    protected boolean shouldBuildEventAndRestartBatch(
            final GameTransaction<@NonNull ?> pointer, final PhaseContext<@NonNull ?> context
    ) {
        return super.shouldBuildEventAndRestartBatch(pointer, context) || ((InventoryBasedTransaction) pointer).inventory != this.inventory;
    }
}
