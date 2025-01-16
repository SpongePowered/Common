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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.effect.ProcessingSideEffect;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

public class ResultingTransactionBySideEffect<T, C, A extends ProcessingSideEffect.Args, @Nullable R> {
    public final ProcessingSideEffect<T, C, A, @Nullable R> effect;
    @Nullable GameTransaction<@NonNull ?> head;
    @Nullable GameTransaction<@NonNull ?> tail;

    public ResultingTransactionBySideEffect(final ProcessingSideEffect<T, C, A, @Nullable R> effect) {
        this.effect = effect;
    }

    public void addChild(
        final PhaseContext<@NonNull ?> context,
        final GameTransaction<@NonNull ?> child
    ) {
        // Basically attempt to climb up the chain to see if any of the existing
        // transactions will accept the child. This can get expensive at times
        // if the transaction tree reaches hundreds x hundreds of transactions.
        final Optional<TransactionFlow.AbsorbingFlowStep> absorbingFlowStep = child.parentAbsorber();
        if (absorbingFlowStep.isPresent()) {
            final TransactionFlow.AbsorbingFlowStep absorber = absorbingFlowStep.get();
            for (final Iterator<GameTransaction<@NonNull ?>> iterator = this.reverseDeepIterator();
                 iterator.hasNext(); ) {
                if (absorber.absorb(context, iterator.next())) {
                    return;
                }
            }
        }
        if (this.tail != null) {
            this.tail.append(child);
        } else {
            this.head = child;
        }
        this.tail = child;
    }

    public Iterator<GameTransaction<@NonNull ?>> deepIterator() {
        return this.head != null ? new DeepIterator(this.head) : Collections.emptyIterator();
    }

    public Iterator<GameTransaction<@NonNull ?>> reverseDeepIterator() {
        return this.tail != null ? new ReverseDeepIterator(this.tail) : Collections.emptyIterator();
    }

}
