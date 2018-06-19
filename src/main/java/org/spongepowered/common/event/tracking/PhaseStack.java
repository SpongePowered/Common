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
package org.spongepowered.common.event.tracking;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.spongepowered.common.event.tracking.phase.general.GeneralPhase;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A simple stack that couples a {@link IPhaseState} and
 * {@link PhaseContext}. As states are pushed, they can likewise
 * be popped and include any contextual data with the {@link PhaseContext}.
 * Note that the {@link PhaseContext} must be marked as {@link PhaseContext#isSwitched()},
 * otherwise an {@link IllegalArgumentException} is thrown.
 */
final class PhaseStack {

    static final PhaseData EMPTY_DATA = new PhaseData(PhaseContext.empty(), GeneralPhase.State.COMPLETE);
    private static final int DEFAULT_QUEUE_SIZE = 16;

    private final Deque<PhaseData> states;

    PhaseStack() {
        this(DEFAULT_QUEUE_SIZE);
    }

    private PhaseStack(int size) {
        this.states = new ArrayDeque<>(size);
    }

    PhaseData peek() {
        final PhaseData phase = this.states.peek();
        return phase == null ? PhaseStack.EMPTY_DATA : phase;
    }

    IPhaseState<?> peekState() {
        final PhaseData peek = this.states.peek();
        return peek == null ? GeneralPhase.State.COMPLETE : peek.state;
    }

    PhaseContext<?> peekContext() {
        final PhaseData peek = this.states.peek();
        return peek == null ? PhaseContext.empty() : peek.context;
    }

    PhaseData pop() {
        return this.states.pop();
    }

    private PhaseStack push(PhaseData tuple) {
        checkNotNull(tuple, "Tuple cannot be null!");
        checkArgument(tuple.context.isSwitched(), "Phase context must be complete: %s", tuple);
        this.states.push(tuple);
        return this;
    }

    PhaseStack push(IPhaseState<?> state, PhaseContext<?> context) {
        return push(new PhaseData(context, state));
    }

    public void forEach(Consumer<PhaseData> consumer) {
        this.states.forEach(consumer);
    }

    public boolean isEmpty() {
        return this.states.isEmpty();
    }

    public int size() {
        return this.states.size();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.states);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PhaseStack other = (PhaseStack) obj;
        return Objects.equals(this.states, other.states);
    }

    @Override
    public String toString() {
        return com.google.common.base.MoreObjects.toStringHelper(this)
                .add("states", this.states)
                .toString();
    }

}
