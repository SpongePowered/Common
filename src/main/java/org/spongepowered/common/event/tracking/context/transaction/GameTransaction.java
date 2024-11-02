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
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.Event;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.type.TransactionType;
import org.spongepowered.common.util.PrettyPrinter;

import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

@DefaultQualifier(NonNull.class)
public abstract class GameTransaction<E extends Event & Cancellable> implements TransactionFlow, StatefulTransaction {

    private final TransactionType<? super E> transactionType;
    protected boolean cancelled = false;

    // Children Definitions
    @Nullable LinkedList<ResultingTransactionBySideEffect> sideEffects;

    // LinkedList node definitions
    @Nullable GameTransaction<@NonNull ?> previous;
    @Nullable GameTransaction<@NonNull ?> next;
    private boolean recorded = false;

    protected GameTransaction(final TransactionType<? super E> transactionType) {
        this.transactionType = transactionType;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GameTransaction.class.getSimpleName() + "[", "]")
            .toString();
    }

    public final TransactionType<? super E> getTransactionType() {
        return this.transactionType;
    }

    final Deque<ResultingTransactionBySideEffect> getEffects() {
        if (this.sideEffects == null) {
            this.sideEffects = new LinkedList<>();
        }
        return this.sideEffects;
    }

    public final void addLast(final ResultingTransactionBySideEffect effect) {
        if (this.sideEffects == null) {
            this.sideEffects = new LinkedList<>();
        }
        this.sideEffects.addLast(effect);
    }

    public final boolean hasChildTransactions() {
        return this.sideEffects != null && this.sideEffects.stream().anyMatch(effect -> effect.head != null);
    }

    public final boolean hasAnyPrimaryChildrenTransactions() {
        if (this.sideEffects == null) {
            return false;
        }
        for (final ResultingTransactionBySideEffect sideEffect : this.sideEffects) {
            @Nullable GameTransaction<@NonNull ?> transaction = sideEffect.head;
            while (transaction != null) {
                if (transaction.transactionType.isPrimary() || transaction.hasChildTransactions()) {
                    return true;
                }
                transaction = transaction.next;
            }
        }
        return false;
    }

    public abstract Optional<BiConsumer<PhaseContext<@NonNull ?>, CauseStackManager.StackFrame>> getFrameMutator(
        @Nullable GameTransaction<@NonNull ?> parent
    );

    public abstract void addToPrinter(PrettyPrinter printer);

    public boolean isUnbatchable() {
        return false;
    }

    public abstract Optional<E> generateEvent(
        PhaseContext<@NonNull ?> context,
        @Nullable GameTransaction<@NonNull ?> parent,
        ImmutableList<GameTransaction<E>> transactions,
        Cause currentCause
    );

    public abstract void restore(PhaseContext<@NonNull ?> context, E event);

    public final void markCancelled() {
        this.cancelled = true;
        this.childIterator().forEachRemaining(GameTransaction::markCancelled);
    }

    public abstract boolean markCancelledTransactions(E event, ImmutableList<? extends GameTransaction<E>> transactions);

    public void postProcessEvent(final PhaseContext<@NonNull ?> context, final E event) {

    }

    public void markEventAsCancelledIfNecessary(final E event) {
        if (this.cancelled) {
            event.setCancelled(true);
        }
    }

    public Optional<ResourceKey> worldKey() {
        return Optional.empty();
    }

    protected boolean shouldBuildEventAndRestartBatch(final GameTransaction<@NonNull ?> pointer, final PhaseContext<@NonNull ?> context) {
        return this.getTransactionType() != pointer.getTransactionType();
    }

    final void append(final GameTransaction<@NonNull ?> child) {
        this.next = child;
        child.previous = this;
    }

    final Iterator<GameTransaction<@NonNull ?>> childIterator() {
        return this.sideEffects != null ? new ChildIterator(this.sideEffects.iterator()) : Collections.emptyIterator();
    }

    final Iterator<GameTransaction<@NonNull ?>> reverseChildIterator() {
        return this.sideEffects != null ? new ReverseChildIterator(this.sideEffects.descendingIterator()) : Collections.emptyIterator();
    }

    @Override
    public final boolean recorded() {
        return this.recorded;
    }

    @Override
    public final GameTransaction<E> recordState() {
        this.captureState();
        this.recorded = true;
        return this;
    }

    protected void captureState() {

    }

    private static class ChildIterator implements Iterator<GameTransaction<@NonNull ?>> {
        private final Iterator<ResultingTransactionBySideEffect> effectIterator;
        private @Nullable GameTransaction<@NonNull ?> cachedNext;
        private @MonotonicNonNull GameTransaction<@NonNull ?> pointer;
        private boolean hasNoRemainingElements = false;

        ChildIterator(final Iterator<ResultingTransactionBySideEffect> iterator) {
            // We're going to search the iterator's effects until we find the first at least
            this.effectIterator = iterator;
            while (this.effectIterator.hasNext()) {
                final ResultingTransactionBySideEffect next = this.effectIterator.next();
                if (next.head != null) {
                    this.cachedNext = next.head;
                    this.pointer = next.head;
                    break;
                }
            }
            if (this.pointer == null) {
                this.hasNoRemainingElements = true;
            }
        }

        @Override
        public boolean hasNext() {
            if (this.cachedNext != null) {
                return true;
            }
            if (this.hasNoRemainingElements) {
                return false;
            }
            if (this.pointer.next != null) {
                this.cachedNext = this.pointer.next;
                return true;
            }
            // start search for the next, sadly because effects don't make a clean chain,
            // there can be many effects with no transactions recorded
            while (this.effectIterator.hasNext()) {
                final ResultingTransactionBySideEffect next = this.effectIterator.next();
                if (next.head != null) {
                    this.cachedNext = next.head;
                    return true;
                }
            }
            this.hasNoRemainingElements = true;
            return false;
        }

        @Override
        public GameTransaction<@NonNull ?> next() {
            if (this.cachedNext != null) {
                final GameTransaction<@NonNull ?> next = this.cachedNext;
                this.pointer = next;
                this.cachedNext = null;
                return next;
            }
            if (this.hasNoRemainingElements) {
                throw new NoSuchElementException("No next GameTransaction to iterate to");
            }
            // But, because someone can *not* call next, we have to call it ourselves
            if (this.hasNext()) {
                return this.next();
            }
            throw new NoSuchElementException("No next GameTransaction to iterate to");
        }
    }


    private static class ReverseChildIterator implements Iterator<GameTransaction<@NonNull ?>> {
        private final Iterator<ResultingTransactionBySideEffect> effectIterator;
        private @Nullable GameTransaction<@NonNull ?> cachedPrevious;
        private @MonotonicNonNull GameTransaction<@NonNull ?> pointer;
        private boolean hasNoRemainingElements = false;

        ReverseChildIterator(final Iterator<ResultingTransactionBySideEffect> iterator) {
            // We're going to search the iterator's effects until we find the first at least
            this.effectIterator = iterator;
            while (this.effectIterator.hasNext()) {
                final ResultingTransactionBySideEffect next = this.effectIterator.next();
                if (next.tail != null) {
                    this.pointer = next.tail;
                    this.cachedPrevious = next.tail;
                    break;
                }
            }
            if (this.pointer == null) {
                this.hasNoRemainingElements = true;
            }
        }

        @Override
        public boolean hasNext() {
            if (this.cachedPrevious != null) {
                return true;
            }
            if (this.hasNoRemainingElements) {
                return false;
            }
            if (this.pointer.previous != null) {
                this.cachedPrevious = this.pointer.previous;
                return true;
            }

            // start search for the next, sadly because effects don't make a clean chain,
            // there can be many effects with no transactions recorded
            while (this.effectIterator.hasNext()) {
                final ResultingTransactionBySideEffect next = this.effectIterator.next();
                if (next.tail != null) {
                    this.cachedPrevious = next.tail;
                    return true;
                }
            }
            this.hasNoRemainingElements = true;
            return false;
        }

        @Override
        public GameTransaction<@NonNull ?> next() {
            if (this.cachedPrevious != null) {
                final GameTransaction<@NonNull ?> next = this.cachedPrevious;
                this.cachedPrevious = null;
                this.pointer = next;
                return next;
            }
            if (this.hasNoRemainingElements) {
                throw new NoSuchElementException("No next GameTransaction to iterate to");
            }
            // But, because someone can *not* call next, we have to call it ourselves
            if (this.hasNext()) {
                return this.next();
            }
            throw new NoSuchElementException("No next GameTransaction to iterate to");
        }
    }

}
