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
package org.spongepowered.common.data.provider.item.stack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.api.data.Keys;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.util.FireworkUtil;
import org.spongepowered.common.util.SpongeTicks;

import java.util.OptionalInt;

public final class FireworkItemStackData {

    private FireworkItemStackData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(ItemStack.class)
                    .create(Keys.FIREWORK_EFFECTS)
                        .get(h -> FireworkUtil.getFireworkEffects(h).orElse(null))
                        .setAnd(FireworkUtil::setFireworkEffects)
                        .deleteAnd(FireworkUtil::removeFireworkEffects)
                        .supports(h -> h.getItem() == Items.FIREWORK_ROCKET || h.getItem() == Items.FIREWORK_STAR)
                    .create(Keys.FIREWORK_FLIGHT_MODIFIER)
                        .get(h -> {
                            final OptionalInt modifier = FireworkUtil.getFlightModifier(h);
                            if (modifier.isEmpty()) {
                                return null;
                            }
                            return new SpongeTicks(modifier.getAsInt());
                        })
                        .setAnd((h, v) -> {
                            final int ticks = SpongeTicks.toSaturatedIntOrInfinite(v);
                            if (v.isInfinite() || ticks < 0 || ticks > Byte.MAX_VALUE) {
                                return false;
                            }
                            return FireworkUtil.setFlightModifier(h, ticks);
                        })
                        .supports(h -> h.getItem() == Items.FIREWORK_ROCKET);
    }
    // @formatter:on
}
