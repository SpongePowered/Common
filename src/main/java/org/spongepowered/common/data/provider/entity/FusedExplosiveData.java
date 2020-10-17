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
import org.spongepowered.api.entity.explosive.fused.FusedExplosive;
import org.spongepowered.common.bridge.explosives.FusedExplosiveBridge;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.util.SpongeTicks;

public final class FusedExplosiveData {

    private FusedExplosiveData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(FusedExplosive.class)
                    .create(Keys.FUSE_DURATION)
                        .get(h -> new SpongeTicks(((FusedExplosiveBridge) h).bridge$getFuseDuration()))
                        .setAnd((h, v) -> {
                            final int ticks = (int) v.getTicks();
                            if (ticks < 0) {
                                return false;
                            }
                            ((FusedExplosiveBridge) h).bridge$setFuseDuration(ticks);
                            return true;
                        })
                    .create(Keys.TICKS_REMAINING)
                        .get(h -> new SpongeTicks(((FusedExplosiveBridge) h).bridge$getFuseTicksRemaining()))
                        .setAnd((h, v) -> {
                            final int ticks = (int) v.getTicks();
                            if (ticks < 0) {
                                return false;
                            }
                            // TODO isPrimed on bridge?
                            if (h.primed().get()) {
                                ((FusedExplosiveBridge) h).bridge$setFuseTicksRemaining(ticks);
                            }
                            return true;
                        });
    }
    // @formatter:on
}
