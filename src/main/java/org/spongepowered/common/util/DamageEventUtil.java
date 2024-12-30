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
package org.spongepowered.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.function.Predicate;

public final class DamageEventUtil {

    public static ServerLocation findFirstMatchingBlock(final Entity entity, final AABB bb, final Predicate<BlockState> predicate) {
        final int i = Mth.floor(bb.minX);
        final int j = Mth.floor(bb.maxX + 1.0D);
        final int k = Mth.floor(bb.minY);
        final int l = Mth.floor(bb.maxY + 1.0D);
        final int i1 = Mth.floor(bb.minZ);
        final int j1 = Mth.floor(bb.maxZ + 1.0D);
        final ChunkSource chunkSource = entity.level().getChunkSource();
        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    final BlockPos blockPos = new BlockPos(k1, l1, i2);
                    final LevelChunk chunk = chunkSource.getChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4, false);
                    if (chunk == null || chunk.isEmpty()) {
                        continue;
                    }
                    if (predicate.test(chunk.getBlockState(blockPos))) {
                        return ServerLocation.of((ServerWorld) entity.level(), k1, l1, i2);
                    }
                }
            }
        }

        // Entity is source of fire
        return ((org.spongepowered.api.entity.Entity) entity).serverLocation();
    }
}
