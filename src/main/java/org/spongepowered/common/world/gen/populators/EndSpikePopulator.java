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
package org.spongepowered.common.world.gen.populators;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeEndDecorator;
import net.minecraft.world.gen.feature.WorldGenSpikes;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.gen.Populator;
import org.spongepowered.api.world.gen.PopulatorType;
import org.spongepowered.common.util.VecHelper;

import java.util.Random;

public class EndSpikePopulator implements Populator {

    private final WorldGenSpikes spikeGen = new WorldGenSpikes();

    @Override
    public PopulatorType getType() {
        return null;
    }

    @Override
    public void populate(Chunk chunk, Random random) {
        World worldIn = (World) chunk.getWorld();
        WorldGenSpikes.EndSpike[] aworldgenspikes$endspike = BiomeEndDecorator.func_185426_a(worldIn);
        BlockPos pos = VecHelper.toBlockPos(chunk.getBlockMin());
        for (WorldGenSpikes.EndSpike worldgenspikes$endspike : aworldgenspikes$endspike) {
            if (worldgenspikes$endspike.func_186154_a(pos)) {
                this.spikeGen.func_186143_a(worldgenspikes$endspike);
                this.spikeGen.generate(worldIn, random,
                        new BlockPos(worldgenspikes$endspike.func_186151_a(), 45, worldgenspikes$endspike.func_186152_b()));
            }
        }
    }

}
