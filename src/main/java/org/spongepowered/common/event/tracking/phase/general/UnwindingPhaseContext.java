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
package org.spongepowered.common.event.tracking.phase.general;

import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;

import java.util.Optional;

import javax.annotation.Nullable;

public final class UnwindingPhaseContext extends GeneralPhaseContext<UnwindingPhaseContext> {

    @Nullable
    public static UnwindingPhaseContext unwind(IPhaseState<?> state, PhaseContext<?> context) {
        if (!state.requiresPost() || !context.hasCaptures()) {
            return null;
        }
        return new UnwindingPhaseContext(state, context)
                .addCaptures()
                .addEntityDropCaptures()
                .buildAndSwitch();
    }

    private IPhaseState<?> unwindingState;

    private PhaseContext<?> unwindingContext;

    private UnwindingPhaseContext(IPhaseState<?> unwindingState, PhaseContext<?> unwindingContext) {
        super(GeneralPhase.Post.UNWINDING);
        this.unwindingState = unwindingState;
        this.unwindingContext = unwindingContext;
    }

    @Override
    public Optional<User> getOwner() {
        return this.unwindingContext.getOwner();
    }

    @Override
    public Optional<User> getNotifier() {
        return this.unwindingContext.getNotifier();
    }

    @SuppressWarnings("unchecked")
    public <T extends PhaseContext<T>> T getUnwindingContext() {
        return (T) this.unwindingContext;
    }

    public IPhaseState<?> getUnwindingState() {
        return this.unwindingState;
    }

    @Override
    public PrettyPrinter printCustom(PrettyPrinter printer, int indent) {
        String s = String.format("%1$" + indent + "s", "");
        super.printCustom(printer, indent)
            .add(s + "- %s: %s", "UnwindingState", this.unwindingState)
            .add(s + "- %s: %s", "UnwindingContext", this.unwindingContext);
        this.unwindingContext.printCustom(printer, indent * 2);
        return printer;
    }
}
