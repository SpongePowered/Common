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

import net.minecraft.core.Holder;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.CatType;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.common.accessor.world.entity.animal.CatAccessor;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.util.MissingImplementationException;

public final class CatData {

    private CatData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(Cat.class)
                    .create(Keys.CAT_TYPE)
                        .get(h -> (CatType) (Object) h.getVariant().value())
                        .set((h, v) -> ((CatAccessor) h).invoker$setVariant(Holder.direct((CatVariant) (Object) v)))
                    .create(Keys.DYE_COLOR)
                        .get(h -> (DyeColor) (Object) h.getCollarColor())
                        .set((h, v) -> ((CatAccessor)h).invoker$setCollarColor((net.minecraft.world.item.DyeColor) (Object) v))
                    .create(Keys.IS_BEGGING_FOR_FOOD)
                        .get(h -> {
                            throw new MissingImplementationException("CatData", "IS_BEGGING_FOR_FOOD::getter");
                        })
                        .set((h, v) -> {
                            throw new MissingImplementationException("CatData", "IS_BEGGING_FOR_FOOD::setter");
                        })
                    .create(Keys.IS_HISSING)
                        .get(h -> {
                            throw new MissingImplementationException("CatData", "IS_HISSING::getter");
                        })
                        .set((h, v) -> {
                            throw new MissingImplementationException("CatData", "IS_HISSING::setter");
                        })
                    .create(Keys.IS_LYING_DOWN)
                        .get(Cat::isLying)
                        .set(Cat::setLying)
                    .create(Keys.IS_PURRING)
                        .get(h -> {
                            throw new MissingImplementationException("CatData", "IS_PURRING::getter");
                        })
                        .set((h, v) -> {
                            throw new MissingImplementationException("CatData", "IS_PURRING::setter");
                        })
                .asMutable(CatAccessor.class)
                    .create(Keys.IS_RELAXED)
                        .get(CatAccessor::invoker$isRelaxStateOne)
                        .set(CatAccessor::invoker$setRelaxStateOne);
    }
    // @formatter:on
}
