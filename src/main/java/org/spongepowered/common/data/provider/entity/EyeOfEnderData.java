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
package org.spongepowered.common.data.provider.entity;

import org.spongepowered.api.data.Keys;
import org.spongepowered.common.accessor.entity.item.EyeOfEnderEntityAccessor;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.SpongeTicks;

public final class EyeOfEnderData {

    private EyeOfEnderData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(EyeOfEnderEntityAccessor.class)
                    .create(Keys.WILL_SHATTER)
                        .get(h -> !h.accessor$getShatterOrDrop())
                        .set((h, v) -> h.accessor$setShatterOrDrop(!v))
                    .create(Keys.DESPAWN_DELAY)
                        .get(h -> new SpongeTicks(Constants.Sponge.Entity.EyeOfEnder.DESPAWN_TIMER_MAX - h.accessor$getDespawnTimer()))
                        .set((h, v) -> h.accessor$setDespawnTimer(Constants.Sponge.Entity.EyeOfEnder.DESPAWN_TIMER_MAX - (int) v.getTicks()));
    }
    // @formatter:on
}
