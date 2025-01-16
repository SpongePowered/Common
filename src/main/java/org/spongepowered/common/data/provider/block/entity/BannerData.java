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
package org.spongepowered.common.data.provider.block.entity;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.meta.BannerPatternLayer;
import org.spongepowered.common.bridge.world.level.block.entity.BannerBlockEntityBridge;
import org.spongepowered.common.data.provider.DataProviderRegistrator;

import java.util.List;

public final class BannerData {

    private BannerData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(BannerBlockEntity.class)
                    .create(Keys.BANNER_PATTERN_LAYERS)
                        .get(h -> h.getPatterns().layers().stream().map(BannerPatternLayer.class::cast).toList())
                        .setAnd((h, v) -> {
                            final Level world = h.getLevel();
                            if (world != null && !world.isClientSide) { // This avoids a client crash because clientside.
                                applyBannerPatternLayers(h, v);
                                return true;
                            }
                            return false;
                        })
            .asImmutable(BannerBlockEntity.class)
                    .create(Keys.DYE_COLOR)
                        .get(h -> ((BannerBlockEntityBridge) h).bridge$getBaseColor())
        ;
    }
    // @formatter:on
    private static void applyBannerPatternLayers(final BannerBlockEntity h, final List<BannerPatternLayer> v) {
        final DataComponentMap.Builder builder = DataComponentMap.builder();
        builder.addAll(h.collectComponents());
        builder.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(v.stream().map(BannerPatternLayers.Layer.class::cast).toList()));
        final DataComponentMap components = builder.build();
        h.applyComponents(components, DataComponentPatch.EMPTY);
    }
}
