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

import java.util.Deque;

public class EffectTransactor implements AutoCloseable {
    final @Nullable ResultingTransactionBySideEffect<?, ?, ?, ?> previousEffect;
    public final @Nullable GameTransaction<@NonNull ?> parent;
    private final TransactionalCaptureSupplier supplier;
    private final ResultingTransactionBySideEffect<?, ?, ?, ?> effect;

    EffectTransactor(final ResultingTransactionBySideEffect<?, ?, ?, ?> effect, final @Nullable GameTransaction<@NonNull ?> parent,
        final @Nullable ResultingTransactionBySideEffect<?, ?, ?, ?> previousEffect, final TransactionalCaptureSupplier transactor) {
        /*
        | ChangeBlock(1) <- head will be RemoveTileEntity(1), tail is still RemoveTileentity(1)
        |  |- RemoveTileEntity <- Head will be ChangeBlock(2) tail is still ChangeBlock(2)
        |  |   |- ChangeBlock <-- Head will be null, tail is still null
         */
        this.effect = effect;
        this.supplier = transactor;
        this.parent = parent;
        this.previousEffect = previousEffect;
    }

    @Override
    public void close() {
        if (this.effect.head == null
            && this.parent != null
            && this.parent.sideEffects != null
            && this.parent.getEffects().peekLast() == this.effect
        ) {
            final Deque<ResultingTransactionBySideEffect<?, ?, ?, ?>> effects = this.parent.getEffects();
            effects.removeLast();
            if (effects.isEmpty()) {
                this.parent.sideEffects = null;
            }
        }
        this.supplier.popEffect(this);
    }
}
