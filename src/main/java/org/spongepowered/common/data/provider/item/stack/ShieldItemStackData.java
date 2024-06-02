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

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.meta.BannerPatternLayer;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.common.data.provider.DataProviderRegistrator;

public final class ShieldItemStackData {

    private ShieldItemStackData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(ItemStack.class)
                    .create(Keys.DYE_COLOR)
                        .get(h -> (DyeColor) (Object) h.getOrDefault(DataComponents.BASE_COLOR, net.minecraft.world.item.DyeColor.WHITE))
                        .set((h, v) -> h.set(DataComponents.BASE_COLOR, (net.minecraft.world.item.DyeColor) (Object) v))
                        .supports(h -> h.getItem() instanceof ShieldItem)
                    .create(Keys.BANNER_PATTERN_LAYERS)
                        .get(h -> h.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers()
                                .stream().map(BannerPatternLayer.class::cast).toList())
                        .set((h, v) -> {
                            h.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(v.stream().map(BannerPatternLayers.Layer.class::cast).toList()));
                            // TODO check setting banner base? Constants.TileEntity.Banner.BANNER_BASE / BannerPatternShapes.BASE
                        })
                        .supports(h -> h.getItem() instanceof ShieldItem || h.getItem() instanceof BannerItem);
    }
    // @formatter:on

}
