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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.GameTransaction;
import org.spongepowered.common.event.tracking.context.transaction.type.TransactionTypes;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhaseUtil;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.PrettyPrinter;

import java.util.Optional;
import java.util.function.BiConsumer;

public class OpenMenuTransaction extends GameTransaction<InteractContainerEvent> {

    private final ServerPlayer player;
    private final ItemStackSnapshot cursor;
    private AbstractContainerMenu menu;

    public OpenMenuTransaction(final Player player) {
        super(TransactionTypes.INTERACT_CONTAINER_EVENT.get());
        this.player = (ServerPlayer) player;
        this.menu = player.containerMenu;
        this.cursor = ItemStackUtil.snapshotOf(player.containerMenu.getCarried());
    }

    @Override
    public Optional<BiConsumer<PhaseContext<@NonNull ?>, CauseStackManager.StackFrame>> getFrameMutator(
            @Nullable final GameTransaction<@NonNull ?> parent
    ) {
        return Optional.of((context, frame) -> {
            frame.pushCause(this.menu);
        });
    }

    @Override
    public Optional<InteractContainerEvent> generateEvent(
        final PhaseContext<@NonNull ?> context, @Nullable final GameTransaction<@NonNull ?> parent,
        final ImmutableList<GameTransaction<InteractContainerEvent>> gameTransactions, final Cause currentCause
    ) {
        // TODO: API-14 post events?
        if (true) {
            return Optional.empty();
        }
        final ItemStackSnapshot resultingCursor = ItemStackUtil.snapshotOf(this.player.containerMenu.getCarried());
        final Transaction<ItemStackSnapshot> cursorTransaction = new Transaction<>(this.cursor, resultingCursor);

        final InteractContainerEvent.Open event = SpongeEventFactory.createInteractContainerEventOpen(currentCause, (Container) this.menu, cursorTransaction);
        return Optional.of(event);
    }

    @Override
    public void restore(final PhaseContext<@NonNull ?> context, final InteractContainerEvent event) {
        PacketPhaseUtil.handleCursorRestore(this.player, event.cursorTransaction(), event.isCancelled());
        this.player.closeContainer();
    }

    @Override
    public void postProcessEvent(final PhaseContext<@NonNull ?> context, final InteractContainerEvent event) {
        PacketPhaseUtil.handleCursorRestore(this.player, event.cursorTransaction(), event.isCancelled());
    }

    @Override
    public boolean markCancelledTransactions(final InteractContainerEvent event,
            final ImmutableList<? extends GameTransaction<InteractContainerEvent>> gameTransactions) {
        if (event.isCancelled()) {
            event.cursorTransaction().invalidate();
            return true;
        }
        return !event.cursorTransaction().isValid();
    }

    @Override
    public void addToPrinter(final PrettyPrinter printer) {

    }

    @Override
    public boolean absorbContainerSet(final PhaseContext<@NonNull ?> ctx, final SetPlayerContainerTransaction transaction) {
        // Just to sanity check that we're not talking about the same player?
        if (this.player != transaction.player) {
            return false;
        }
        this.menu = transaction.menu;
        return true;
    }

    @Override
    public boolean absorbSlotTransaction(
        final ContainerSlotTransaction slotTransaction
    ) {
        return true;
    }
}
