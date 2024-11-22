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
package org.spongepowered.common.mixin.core.server.level;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.world.chunk.ChunkEvent;
import org.spongepowered.api.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3i;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkHolder.class)
abstract class ChunkHolderMixin extends GenerationChunkHolderMixin {

    // @formatter:off
    @Shadow public abstract CompletableFuture<ChunkResult<LevelChunk>> shadow$getEntityTickingChunkFuture();
    // @formatter:on

    private LevelChunk impl$loadedChunk;

    /**
     * After onFullChunkStatusChange
     */
    @Inject(method = "lambda$scheduleFullChunkPromotion$4", at = @At("TAIL"))
    private void impl$onScheduleFullChunkPromotion(final ChunkMap $$0x, final FullChunkStatus $$1x, final CallbackInfo ci) {
        if ($$1x == FullChunkStatus.ENTITY_TICKING) {
            this.shadow$getEntityTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).ifSuccess(chunk -> {
                this.impl$loadedChunk = chunk;
                if (ShouldFire.CHUNK_EVENT_LOAD) {
                    final Vector3i chunkPos = VecHelper.toVector3i(chunk.getPos());
                    final ChunkEvent.Load event = SpongeEventFactory.createChunkEventLoad(PhaseTracker.getInstance().currentCause(),
                        (WorldChunk) chunk, chunkPos, (ResourceKey) (Object) chunk.getLevel().dimension().location());
                    SpongeCommon.post(event);
                }
            });
        }
    }

    @Inject(method = "demoteFullChunk", at = @At("HEAD"))
    private void impl$onDemoteFullChunkPre(final ChunkMap $$0x, final FullChunkStatus $$1x, final CallbackInfo ci) {
        if (this.impl$loadedChunk != null && ShouldFire.CHUNK_EVENT_UNLOAD_PRE) {
            final Vector3i chunkPos = VecHelper.toVector3i(this.impl$loadedChunk.getPos());
            final ChunkEvent.Unload event = SpongeEventFactory.createChunkEventUnloadPre(PhaseTracker.getInstance().currentCause(),
                (WorldChunk) this.impl$loadedChunk, chunkPos, (ResourceKey) (Object) this.impl$loadedChunk.getLevel().dimension().location());
            SpongeCommon.post(event);
        }
    }

    @Inject(method = "demoteFullChunk", at = @At("TAIL"))
    private void impl$onDemoteFullChunkPost(final ChunkMap $$0x, final FullChunkStatus $$1x, final CallbackInfo ci) {
        if (this.impl$loadedChunk != null && ShouldFire.CHUNK_EVENT_UNLOAD_POST) {
            final Vector3i chunkPos = VecHelper.toVector3i(this.impl$loadedChunk.getPos());
            final ChunkEvent.Unload event = SpongeEventFactory.createChunkEventUnloadPost(PhaseTracker.getInstance().currentCause(), chunkPos,
                (ResourceKey) (Object) this.impl$loadedChunk.getLevel().dimension().location());
            SpongeCommon.post(event);
        }
        this.impl$loadedChunk = null;
    }
}
