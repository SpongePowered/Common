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
package org.spongepowered.common.event.tracking.context.transaction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.Event;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.ICaptureSupplier;
import org.spongepowered.common.event.tracking.context.transaction.effect.PrepareBlockDrops;
import org.spongepowered.common.event.tracking.context.transaction.type.TransactionType;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

@SuppressWarnings("rawtypes")
public final class TransactionalCaptureSupplier implements ICaptureSupplier, TransactionSink, Iterable<GameTransaction<@NonNull ?>> {

    // We made BlockTransaction a Node and this is a pseudo LinkedList due to the nature of needing
    // to be able to track what block states exist at the time of the transaction while other transactions
    // are processing (because future transactions performing logic based on what exists at that state,
    // will potentially get contaminated information based on the last transaction prior to transaction
    // processing). Example: When starting to perform neighbor notifications during piston movement, one
    // can feasibly see that the block state is changed already without being able to get the appropriate
    // block state.
    private @Nullable GameTransaction<@NonNull ?> tail;
    private @Nullable GameTransaction<@NonNull ?> head;
    private @Nullable ResultingTransactionBySideEffect effect;
    private final PhaseContext<@NonNull ?> context;

    public TransactionalCaptureSupplier(final PhaseContext<@NonNull ?> context) {
        this.context = context;
    }


    /**
     * Returns {@code true} if there are no captured objects.
     *
     * @return {@code true} if empty
     */
    @Override
    public boolean isEmpty() {
        return this.head == null;
    }

    /*
    Begin the more enhanced block tracking. This is only used by states that absolutely need to be able to track certain changes
    that involve more "physics" related transactions, such as neighbor notification tracking, tile entity tracking, and
    normally, intermediary transaction tracking. Because of these states, we need to envelop the information relating to:
    - The most recent block change, if it has been a change that was applied
    - The most recent tile entity being captured
    - The most recent neighbor notification in the order in which it is being applied to in comparison with the most recent block change

    In some rare cases, some block changes may take place after a neighbor notification is submitted, or a tile entity is being replaced.
    To acommodate this, when such cases arise, we attempt to snapshot any potential transactions that may take place prior to their
    blocks being changed, allowing us to take full snapshots of tile entities in the event a complete restoration is required.
    This is achieved through captureNeighborNotification and logTileChange.
     */

    @Override
    public EffectTransactor pushEffect(final ResultingTransactionBySideEffect effect) {
        final GameTransaction<@NonNull ?> parentTransaction = Optional.ofNullable(this.effect)
            .map(child -> (GameTransaction) child.tail)
            .orElse(Objects.requireNonNull(this.tail, "Somehow pushing a new effect without an owning Transaction"));
        final EffectTransactor effectTransactor = new EffectTransactor(effect, parentTransaction, this.effect, this);
        this.effect = effect;
        parentTransaction.addLast(effect);
        return effectTransactor;
    }

    void popEffect(final EffectTransactor transactor) {
        this.effect = transactor.previousEffect;
    }

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Override
    public void logTransaction(final StatefulTransaction transaction) {
        // todo - abstract the rest of this out into StatefulTransaction
        if (this.head == null) {
            final GameTransaction<@NonNull ?> gameTransaction = transaction.recordState();
            this.head = gameTransaction;
            this.tail = gameTransaction;
            return;
        }
        final Optional<TransactionFlow.AbsorbingFlowStep> absorbingFlowStep = transaction.parentAbsorber();
        if (absorbingFlowStep.isPresent()) {
            final TransactionFlow.AbsorbingFlowStep absorber = absorbingFlowStep.get();
            final Iterator<GameTransaction<@NonNull ?>> iterator = this.descendingIterator();
            while (iterator.hasNext()) {
                final GameTransaction<@NonNull ?> next = iterator.next();
                if (absorber.absorb(this.context, next)) {
                    return;
                }
            }
        }
        if (transaction.shouldHaveBeenAbsorbed()) {
            SpongeCommon.logger().warn("Logged transaction without event transaction!", new Exception(transaction.getClass().getName()));
        }
        // Finally, mark the transaction as recorded, does any setup handling with regards to capturing details
        // that otherwise would've been expensive to perform possibly later.
        final GameTransaction<@NonNull ?> gameTransaction = transaction.recordState();
        if (this.effect != null) {
            this.effect.addChild(this.context, gameTransaction);
        } else {
            gameTransaction.previous = this.tail;
            if (this.tail != null) {
                this.tail.next = gameTransaction;
            }
            this.tail = gameTransaction;
        }
    }

    public void completeBlockDrops(final @Nullable EffectTransactor context) {
        if (this.effect != null) {
            if (this.effect.effect == PrepareBlockDrops.getInstance()) {
                if (context != null) {
                    context.close();
                }
            }
        }
    }

    public void clear() {
        this.head = null;
        this.tail = null;
        this.effect = null;
    }

    @SuppressWarnings("unchecked")
    public boolean processTransactions(final PhaseContext<@NonNull ?> context) {
        if (this.head == null) {
            return false;
        }
        final ImmutableMultimap.Builder<TransactionType, ? extends Event> builder = ImmutableMultimap.builder();
        final ImmutableList<EventByTransaction<@NonNull ?>> batched = TransactionalCaptureSupplier.batchTransactions(
            this.head, null, context, builder
        );
        boolean cancelledAny = false;
        for (final EventByTransaction<@NonNull ?> eventWithTransactions : batched) {
            final Event event = eventWithTransactions.event();
            if (eventWithTransactions.isParentOrDeciderCancelled()) {
                cancelledAny = true;
                eventWithTransactions.markCancelled();
                continue;
            }
            Sponge.eventManager().post(event);
            if (event instanceof Cancellable && ((Cancellable) event).isCancelled()) {
                eventWithTransactions.markCancelled();
                cancelledAny = true;
            }
            if (((GameTransaction) eventWithTransactions.decider()).markCancelledTransactions(event, eventWithTransactions.transactions())) {
                cancelledAny = true;
            }
            for (final GameTransaction<@NonNull ?> transaction : eventWithTransactions.transactions()) {
                if (transaction.cancelled) {
                    ((GameTransaction) transaction).markEventAsCancelledIfNecessary(eventWithTransactions.event());
                }
                if (!transaction.cancelled) {
                    ((GameTransaction) transaction).postProcessEvent(context, event);
                }
            }
        }
        if (cancelledAny) {
            for (final EventByTransaction<@NonNull ?> eventByTransaction : batched.reverse()) {
                if (eventByTransaction.decider().cancelled) {
                    ((GameTransaction) eventByTransaction.decider()).markEventAsCancelledIfNecessary(eventByTransaction.event());
                }
                for (final GameTransaction<@NonNull ?> gameTransaction : eventByTransaction.transactions().reverse()) {
                    if (gameTransaction.cancelled) {
                        ((GameTransaction) gameTransaction).restore(context, eventByTransaction.event());
                    }
                }
            }
        }
        builder.build().asMap()
            .forEach((transactionType, events) -> transactionType.createAndProcessPostEvents(context, events));
        return !cancelledAny;
    }

    @SuppressWarnings("unchecked")
    static ImmutableList<EventByTransaction<@NonNull ?>> batchTransactions(
        final GameTransaction head,
        @Nullable final GameTransaction parent,
        final PhaseContext<@NonNull ?> context,
        final ImmutableMultimap.Builder<TransactionType, ? extends Event> transactionPostEventBuilder
    ) {
        final ImmutableList.Builder<EventByTransaction<@NonNull ?>> builder = ImmutableList.builder();
        @Nullable GameTransaction pointer = head;
        ImmutableList.Builder<GameTransaction> accumilator = ImmutableList.builder();
        @MonotonicNonNull GameTransaction batchDecider = null;
        while (pointer != null) {
            if (batchDecider == null) {
                batchDecider = pointer;
            }
            if (batchDecider.shouldBuildEventAndRestartBatch(pointer, context)) {
                final ImmutableList<GameTransaction> transactions = accumilator.build();
                accumilator = ImmutableList.builder();
                TransactionalCaptureSupplier.generateEventForTransaction(
                    batchDecider,
                    parent,
                    context,
                    builder,
                    (ImmutableList) transactions,
                    transactionPostEventBuilder
                );
                // accumilator.add(pointer);
                batchDecider = pointer;
                continue;
            } else if (pointer.hasAnyPrimaryChildrenTransactions() || pointer.isUnbatchable() || pointer.next == null) {
                accumilator.add(pointer);
                final ImmutableList<GameTransaction> transactions = accumilator.build();
                accumilator = ImmutableList.builder();
                batchDecider = pointer.next;
                TransactionalCaptureSupplier.generateEventForTransaction(
                    pointer,
                    parent,
                    context,
                    builder,
                    (ImmutableList) transactions,
                    transactionPostEventBuilder
                );
            } else {
                accumilator.add(pointer);
            }
            pointer = pointer.next;
        }
        final ImmutableList<GameTransaction> remaining = accumilator.build();
        if (!remaining.isEmpty()) {
            TransactionalCaptureSupplier.generateEventForTransaction(
                Objects.requireNonNull(batchDecider, "BatchDeciding Transaction was null"),
                parent,
                context,
                builder,
                (ImmutableList) remaining,
                transactionPostEventBuilder
            );
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static <E extends Event & Cancellable> void generateEventForTransaction(
        final @NonNull GameTransaction<E> pointer,
        final @Nullable GameTransaction<@NonNull ?> parent,
        final PhaseContext<@NonNull ?> context,
        final ImmutableList.Builder<EventByTransaction<@NonNull ?>> builder,
        final ImmutableList<GameTransaction<E>> transactions,
        final ImmutableMultimap.Builder<TransactionType, ? extends Event> transactionPostEventBuilder
    ) {
        final Optional<BiConsumer<PhaseContext<@NonNull ?>, CauseStackManager.StackFrame>> frameMutator = pointer.getFrameMutator(parent);
        final PhaseTracker instance = PhaseTracker.getInstance();
        try (
            final CauseStackManager.StackFrame frame = frameMutator
                .map(mutator -> {
                    final CauseStackManager.StackFrame transactionFrame = instance.pushCauseFrame();
                    mutator.accept(context, transactionFrame);
                    return transactionFrame;
                })
                .orElseGet(instance::pushCauseFrame)
        ) {
            final Optional<E> generatedEvent = pointer.generateEvent(context, parent, transactions, instance.currentCause());
            generatedEvent
                // It's not guaranteed that a transaction has a valid world or some other artifact,
                // and in those cases, we don't want to treat the transaction as being "cancellable"
                .ifPresent(e -> {
                    final EventByTransaction<E> element = new EventByTransaction<>(e, transactions, parent, pointer);
                    builder.add(element);
                    ((ImmutableMultimap.Builder) transactionPostEventBuilder).put(pointer.getTransactionType(), e);

                });

            for (final GameTransaction<E> transaction : transactions) {
                if (transaction.sideEffects == null || transaction.sideEffects.isEmpty()) {
                    continue;
                }
                generatedEvent.ifPresent(frame::pushCause);
                for (final ResultingTransactionBySideEffect sideEffect : transaction.sideEffects) {
                    if (sideEffect.head == null) {
                        continue;
                    }
                    builder.addAll(TransactionalCaptureSupplier.batchTransactions(sideEffect.head, pointer, context, transactionPostEventBuilder));
                }
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.head);
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final TransactionalCaptureSupplier other = (TransactionalCaptureSupplier) obj;
        return Objects.equals(this.head, other.head);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TransactionalCaptureSupplier.class.getSimpleName() + "[", "]")
            .add("tail=" + this.tail)
            .add("head=" + this.head)
            .add("effect=" + this.effect)
            .toString();
    }

    public void reset() {
        if (this.head != null) {
            this.head = null;
            this.tail = null;
        }
        if (this.effect != null) {
            this.effect = null;
        }
    }

    @Override
    public Iterator<GameTransaction<@NonNull ?>> iterator() {
        return this.head != null ? new DeepIterator(this.head) : Collections.emptyIterator();
    }

    @Override
    public Spliterator<GameTransaction<@NonNull ?>> spliterator() {
        return Spliterators.spliteratorUnknownSize(this.iterator(), Spliterator.ORDERED | Spliterator.NONNULL);
    }

    public Iterator<GameTransaction<@NonNull ?>> descendingIterator() {
        return this.tail != null ? new ReverseDeepIterator(this.tail) : Collections.emptyIterator();
    }

}
