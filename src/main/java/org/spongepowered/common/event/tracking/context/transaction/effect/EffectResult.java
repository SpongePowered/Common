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
package org.spongepowered.common.event.tracking.context.transaction.effect;

import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.List;

public final class EffectResult<@Nullable R> {

    public static <@Nullable T> EffectResult<T> nullReturn() {
        return new EffectResult<>(null, true);
    }

    public static <@Nullable T> EffectResult<T> nullPass() {
        return new EffectResult<>(null, false);
    }

    public final @Nullable R resultingState;
    public final List<ItemStack> drops;
    public final boolean hasResult;

    public EffectResult(final @Nullable R resultingState, final boolean hasResult) {
        this.resultingState = resultingState;
        this.hasResult = hasResult;
        this.drops = Collections.emptyList();
    }

    public EffectResult(final @Nullable R resultingState, final List<ItemStack> drops, final boolean hasResult) {
        this.resultingState = resultingState;
        this.drops = drops;
        this.hasResult = hasResult;
    }

}
