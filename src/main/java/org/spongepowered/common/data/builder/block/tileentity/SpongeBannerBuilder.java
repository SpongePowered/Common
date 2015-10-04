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
package org.spongepowered.common.data.builder.block.tileentity;

import static org.spongepowered.api.data.DataQuery.of;

import net.minecraft.tileentity.TileEntityBanner;
import org.spongepowered.api.Game;
import org.spongepowered.api.block.tileentity.Banner;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.mutable.tileentity.BannerData;
import org.spongepowered.api.data.meta.PatternLayer;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.service.persistence.InvalidDataException;
import org.spongepowered.api.service.persistence.SerializationService;

import java.util.List;
import java.util.Optional;

public class SpongeBannerBuilder extends AbstractTileBuilder<Banner> {

    public static final DataQuery BASE = of("Base");
    public static final DataQuery PATTERNS = of("Patterns");

    public SpongeBannerBuilder(Game game) {
        super(game);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Banner> build(DataView container) throws InvalidDataException {
        final Optional<Banner> bannerOptional = super.build(container);
        if (!bannerOptional.isPresent()) {
            throw new InvalidDataException("The container had insufficient data to create a Banner tile entity!");
        }

        if (!container.contains(BASE) || !container.contains(PATTERNS)) {
            throw new InvalidDataException("The provided container does not contain the data to make a Banner!");
        }
        final SerializationService service = this.game.getServiceManager().provide(SerializationService.class).get();

        final BannerData bannerData = null;

        String dyeColorId = container.getString(BASE).get();
        Optional<DyeColor> colorOptional = this.game.getRegistry().getType(DyeColor.class, dyeColorId);
        if (!colorOptional.isPresent()) {
            throw new InvalidDataException("The provided container has an invalid dye color entry!");
        }
        bannerData.baseColor().set(colorOptional.get());

        // Now we have to get the patterns list
        final List<PatternLayer> patternsList = container.getSerializableList(PATTERNS, PatternLayer.class, service).get();
        final ListValue<PatternLayer> patternLayers = bannerData.patternsList();
        patternsList.forEach(patternLayers::add);
        bannerData.set(patternLayers);
        final Banner banner = bannerOptional.get();
        banner.offer(bannerData);
        ((TileEntityBanner) banner).validate();
        return Optional.of(banner);
    }
}
