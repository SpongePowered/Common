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
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.CompositeEvent;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.impl.AbstractCompositeEvent;
import org.spongepowered.common.event.tracking.context.transaction.GameTransaction;
import org.spongepowered.common.event.tracking.context.transaction.type.TransactionType;

import java.util.stream.Stream;

public abstract class CompositeTransaction<E extends CompositeEvent> extends GameTransaction<E> {
    protected CompositeTransaction(TransactionType<? extends E> transactionType) {
        super(transactionType);
    }

    @Override
    public void associateSideEffectEvents(E event, Stream<Event> elements) {
        elements.forEach(event.children()::add);
    }

    @Override
    public void finalizeSideEffects(E post) {
        // This finalizes the list to be immutable
        ((AbstractCompositeEvent) post).postInit();
    }

    public void pushCause(CauseStackManager.StackFrame frame, E e) {
        frame.pushCause(e.baseEvent());
    }

    @Override
    public boolean markCancelledTransactions(
        final E event,
        final ImmutableList<? extends GameTransaction<E>> gameTransactions) {
        event.children().stream().filter(e -> e instanceof Cancellable)
            .map(e -> (Cancellable) e)
            .forEach(e -> e.setCancelled(event.isCancelled()));
        return false;
    }

}
