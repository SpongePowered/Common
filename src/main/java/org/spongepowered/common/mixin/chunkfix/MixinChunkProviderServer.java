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
package org.spongepowered.common.mixin.chunkfix;

import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.interfaces.world.IMixinWorld;

@Mixin(value = ChunkProviderServer.class, priority = 1111)
public abstract class MixinChunkProviderServer {

    @Shadow public WorldServer worldObj;
    @Shadow private LongHashMap<Chunk> id2ChunkMap;
    @Shadow private Chunk dummyChunk;
    @Shadow public abstract Chunk loadChunk(int chunkX, int chunkZ);

    @Overwrite
    public Chunk provideChunk(int x, int z) {
        Chunk chunk = this.id2ChunkMap.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(x, z));
        final boolean isDummyChunkEnabled = ((IMixinWorld) this.worldObj).getWorldConfig().getConfig().getGameFixes().isDummyChunkLoadingEnabled();
        return chunk == null ? (!this.worldObj.isFindingSpawnPoint() && isDummyChunkEnabled ? this.dummyChunk : this.loadChunk(x, z)) : chunk;
    }

}
