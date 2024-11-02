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
package org.spongepowered.common.event.tracking.context.transaction.block;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import io.leangen.geantyref.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.ScheduledTick;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.transaction.ScheduleUpdateTicket;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.ScheduleBlockUpdateEvent;
import org.spongepowered.api.scheduler.TaskPriority;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.common.block.SpongeScheduleUpdateTicket;
import org.spongepowered.common.bridge.world.ticks.TickNextTickDataBridge;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.GameTransaction;
import org.spongepowered.common.event.tracking.context.transaction.type.TransactionTypes;
import org.spongepowered.common.event.tracking.context.transaction.world.WorldBasedTransaction;
import org.spongepowered.common.util.PrettyPrinter;
import org.spongepowered.common.world.server.SpongeLocatableBlockBuilder;
import org.spongepowered.math.vector.Vector3i;

import java.time.Duration;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ScheduleUpdateTransaction<T> extends WorldBasedTransaction<ScheduleBlockUpdateEvent<T>> {

    private final BlockPos affectedPosition;
    private final BlockState originalState;
    private final TypeToken<T> typeToken;

    private Supplier<ScheduledTick<T>> dataSupplier;
    private Supplier<ScheduleUpdateTicket<T>> ticketSupplier;

    @SuppressWarnings("unchecked")
    public ScheduleUpdateTransaction(
        final Supplier<ServerLevel> serverWorldSupplier, final Supplier<ScheduledTick<T>> dataSupplier,
        final BlockPos affectedPosition, final T target, final Duration delay, final TaskPriority priority
    ) {
        super(TransactionTypes.SCHEDULE_BLOCK_UPDATE.get(), ((org.spongepowered.api.world.server.ServerWorld) serverWorldSupplier.get()).key());
        this.affectedPosition = affectedPosition;
        this.originalState = serverWorldSupplier.get().getBlockState(affectedPosition);
        this.typeToken = TypeToken.get((Class<T>) target.getClass());

        this.dataSupplier = dataSupplier;
        this.ticketSupplier = Suppliers.memoize(() -> {
            final LocatableBlock locatableBlock = new SpongeLocatableBlockBuilder()
                .world(serverWorldSupplier)
                .position(this.affectedPosition.getX(), this.affectedPosition.getY(), this.affectedPosition.getZ())
                .state((org.spongepowered.api.block.BlockState) this.originalState)
                .build();
            return new SpongeScheduleUpdateTicket<>(locatableBlock, target, delay, priority);
        });
    }

    @Override
    public Optional<BiConsumer<PhaseContext<@NonNull ?>, CauseStackManager.StackFrame>> getFrameMutator(final @Nullable GameTransaction<@NonNull ?> parent) {
        return Optional.empty();
    }

    @Override
    public Optional<ScheduleBlockUpdateEvent<T>> generateEvent(
        final PhaseContext<@NonNull ?> context,
        final @Nullable GameTransaction<@NonNull ?> parent,
        final ImmutableList<GameTransaction<ScheduleBlockUpdateEvent<T>>> gameTransactions,
        final Cause currentCause
    ) {
        final ImmutableList<ScheduleUpdateTicket<T>> tickets = gameTransactions.stream()
            .map(transaction -> ((ScheduleUpdateTransaction<T>) transaction).ticketSupplier.get())
            .collect(ImmutableList.toImmutableList());

        return Optional.of(SpongeEventFactory.createScheduleBlockUpdateEvent(currentCause, this.typeToken, tickets));
    }

    @Override
    public void restore(final PhaseContext<?> context, final ScheduleBlockUpdateEvent<T> event) {
        ((TickNextTickDataBridge<?>) (Object) this.dataSupplier.get()).bridge$cancelForcibly();
    }

    @Override
    public boolean markCancelledTransactions(
        final ScheduleBlockUpdateEvent<T> event,
        final ImmutableList<? extends GameTransaction<ScheduleBlockUpdateEvent<T>>> gameTransactions
    ) {
        boolean cancelledAny = false;
        for (final ScheduleUpdateTicket<T> transaction : event.tickets()) {
            if (!transaction.valid()) {
                cancelledAny = true;
                for (final GameTransaction<ScheduleBlockUpdateEvent<T>> gameTransaction : gameTransactions) {
                    final ScheduleUpdateTransaction<T> scheduleUpdateTransaction = (ScheduleUpdateTransaction<T>) gameTransaction;
                    final Vector3i position = transaction.block().blockPosition();
                    final BlockPos affectedPosition = scheduleUpdateTransaction.affectedPosition;
                    if (position.x() == affectedPosition.getX()
                        && position.y() == affectedPosition.getY()
                        && position.z() == affectedPosition.getZ()
                    ) {
                        gameTransaction.markCancelled();
                    }
                }
            }
        }

        return cancelledAny;
    }

    @Override
    public void markEventAsCancelledIfNecessary(final ScheduleBlockUpdateEvent<T> event) {
        super.markEventAsCancelledIfNecessary(event);
        event.tickets().forEach(ScheduleUpdateTicket::invalidate);
    }

    @Override
    public void addToPrinter(final PrettyPrinter printer) {
        printer.add("ScheduleUpdate")
            .add(" %s : %s", "Affected Position", this.affectedPosition)
            .add(" %s : %s", "Original State", this.originalState);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ScheduleUpdateTransaction.class.getSimpleName() + "[", "]")
            .add("affectedPosition=" + this.affectedPosition)
            .add("worldKey=" + this.worldKey)
            .add("originalState=" + this.originalState)
            .add("cancelled=" + this.cancelled)
            .toString();
    }

    @Override
    protected boolean shouldBuildEventAndRestartBatch(
        final GameTransaction<@NonNull ?> pointer, final PhaseContext<@NonNull ?> context
    ) {
        return super.shouldBuildEventAndRestartBatch(pointer, context) || !this.typeToken.equals(((ScheduleUpdateTransaction<?>) pointer).typeToken);
    }
}
