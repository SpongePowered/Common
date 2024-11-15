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
package org.spongepowered.common.mixin.api.minecraft.world.level.levelgen.placement;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.RegistryOps;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.world.generation.feature.PlacementModifier;
import org.spongepowered.api.world.generation.feature.PlacementModifierType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.SpongeCommon;

@Mixin(net.minecraft.world.level.levelgen.placement.PlacementModifierType.class)
public interface PlacementModifierTypeMixin_API<P extends net.minecraft.world.level.levelgen.placement.PlacementModifier> extends PlacementModifierType {

    //@formatter:off
    @Shadow MapCodec<P> shadow$codec();
    //@formatter:on

    @Override
    default PlacementModifier configure(final DataView config) throws IllegalArgumentException {
        try {
            final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(config));
            final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, SpongeCommon.vanillaRegistryAccess());
            return (PlacementModifier) this.shadow$codec().codec().parse(ops, json).getOrThrow();
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not configure PlacementModifier." , e);
        }
    }
}
