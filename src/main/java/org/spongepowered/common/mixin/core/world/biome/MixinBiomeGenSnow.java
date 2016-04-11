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
package org.spongepowered.common.mixin.core.world.biome;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenSnow;
import org.spongepowered.api.util.weighted.VariableAmount;
import org.spongepowered.api.world.gen.populator.Forest;
import org.spongepowered.api.world.gen.populator.IcePath;
import org.spongepowered.api.world.gen.populator.IceSpike;
import org.spongepowered.api.world.gen.type.BiomeTreeTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.world.biome.SpongeBiomeGenerationSettings;

@Mixin(BiomeGenSnow.class)
public abstract class MixinBiomeGenSnow extends MixinBiomeGenBase {

    @Shadow private boolean field_150615_aC;

    @Override
    public void buildPopulators(World world, SpongeBiomeGenerationSettings gensettings) {
        if (this.field_150615_aC) {
            IceSpike spike = IceSpike.builder()
                    .spikesPerChunk(3)
                    .build();
            gensettings.getPopulators().add(spike);
            IcePath path = IcePath.builder()
                    .perChunk(2)
                    .radius(VariableAmount.baseWithRandomAddition(2, 2))
                    .build();
            gensettings.getPopulators().add(path);
        }
        super.buildPopulators(world, gensettings);
        BiomeDecorator theBiomeDecorator = this.theBiomeDecorator;
        gensettings.getPopulators().removeAll(gensettings.getPopulators(Forest.class));
        Forest.Builder forest = Forest.builder();
        forest.perChunk(VariableAmount.baseWithOptionalAddition(theBiomeDecorator.treesPerChunk, 2, 0.1));
        forest.type(BiomeTreeTypes.TALL_TAIGA.getPopulatorObject(), 1);
        gensettings.getPopulators().add(0, forest.build());
    }
}
