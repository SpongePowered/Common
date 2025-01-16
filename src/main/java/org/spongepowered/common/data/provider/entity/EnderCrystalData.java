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

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.api.data.Keys;
import org.spongepowered.common.accessor.world.entity.EntityAccessor;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageSources;
import org.spongepowered.common.util.VecHelper;

public final class EnderCrystalData {

    private static final double ALIVE_HEALTH = 1.0;

    private EnderCrystalData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(EndCrystal.class)
                    .create(Keys.HEALTH)
                        .get(h -> h.isRemoved() ? 0.0 : EnderCrystalData.ALIVE_HEALTH)
                        .setAnd((h, v) -> {
                            if (v < 0 || v > EnderCrystalData.ALIVE_HEALTH) {
                                return false;
                            }
                            if (v == 0) {
                                final var damageSource = (DamageSource) SpongeDamageSources.IGNORED;
                                switch (h.level()) {
                                    case ServerLevel sl -> h.hurtServer(sl, damageSource, 1000F);
                                    default -> h.hurtClient(damageSource);
                                }
                            } else {
                                ((EntityAccessor) h).invoker$unsetRemoved();
                            }
                            return true;
                        })
                    .create(Keys.SHOW_BOTTOM)
                        .get(EndCrystal::showsBottom)
                        .set(EndCrystal::setShowBottom)
                    .create(Keys.TARGET_POSITION)
                        .get(h -> VecHelper.toVector3i(h.getBeamTarget()))
                        .set((h, v) -> h.setBeamTarget(VecHelper.toBlockPos(v)));
    }
    // @formatter:on
}
