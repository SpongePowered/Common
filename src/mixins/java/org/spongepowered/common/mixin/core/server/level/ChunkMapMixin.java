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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.world.chunk.ChunkEvent;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.chunk.BlockChunk;
import org.spongepowered.api.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.server.level.ServerChunkCacheAccessor;
import org.spongepowered.common.accessor.world.level.chunk.storage.ChunkStorageAccessor;
import org.spongepowered.common.bridge.server.level.ServerLevelBridge;
import org.spongepowered.common.bridge.world.DistanceManagerBridge;
import org.spongepowered.common.bridge.world.level.chunk.LevelChunkBridge;
import org.spongepowered.common.bridge.world.level.chunk.storage.IOWorkerBridge;
import org.spongepowered.common.bridge.world.level.storage.PrimaryLevelDataBridge;
import org.spongepowered.common.bridge.world.server.ChunkMapBridge;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.DirectionUtil;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.level.chunk.storage.SpongeIOWorkerType;
import org.spongepowered.math.vector.Vector3i;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements ChunkMapBridge {

    // @formatter:off
    @Shadow @Final ServerLevel level;
    // @formatter:on

    @Override
    public DistanceManagerBridge bridge$distanceManager() {
        // The ticket manager on this object is a package-private class and isn't accessible from here
        // - @Shadow doesn't work because it seems to need the exact type.
        return (DistanceManagerBridge) ((ServerChunkCacheAccessor) this.level.getChunkSource()).accessor$distanceManager();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void impl$setIOWorkerDimension(final CallbackInfo ci) {
        ((IOWorkerBridge) ((ChunkStorageAccessor) this).accessor$worker()).bridge$setDimension(SpongeIOWorkerType.CHUNK, this.level.dimension());
    }

    @Redirect(method = "save",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;flush(Lnet/minecraft/world/level/ChunkPos;)V"))
    private void impl$useSerializationBehaviorForPOI(final PoiManager pointOfInterestManager, final ChunkPos p_219112_1_) {
        final PrimaryLevelDataBridge infoBridge = (PrimaryLevelDataBridge) this.level.getLevelData();
        final SerializationBehavior serializationBehavior = infoBridge.bridge$serializationBehavior().orElse(SerializationBehavior.AUTOMATIC);
        if (serializationBehavior == SerializationBehavior.AUTOMATIC || serializationBehavior == SerializationBehavior.MANUAL) {
            pointOfInterestManager.flush(p_219112_1_);
        }
    }

    @Redirect(method = "save", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkMap;write(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Void> impl$doNotWriteIfWeHaveNoData(final ChunkMap chunkManager, final ChunkPos pos, final Supplier<CompoundTag> compound) {
        final PrimaryLevelDataBridge infoBridge = (PrimaryLevelDataBridge) this.level.getLevelData();
        final SerializationBehavior serializationBehavior = infoBridge.bridge$serializationBehavior().orElse(SerializationBehavior.AUTOMATIC);
        if (serializationBehavior == SerializationBehavior.AUTOMATIC || serializationBehavior == SerializationBehavior.MANUAL) {
            return chunkManager.write(pos, compound);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Redirect(method = "lambda$scheduleUnload$12",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;unload(Lnet/minecraft/world/level/chunk/LevelChunk;)V"),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z")
            )
    )
    private void impl$onSetUnloaded(final ServerLevel level, final LevelChunk chunk) {
        level.unload(chunk);

        for (final Direction dir : Constants.Chunk.CARDINAL_DIRECTIONS) {
            final int index = DirectionUtil.directionToIndex(dir);
            final LevelChunk neighbor = ((LevelChunkBridge) chunk).bridge$getNeighborChunk(index);
            if (neighbor != null) {
                final int oppositeIndex = DirectionUtil.directionToIndex(dir.opposite());
                ((LevelChunkBridge) chunk).bridge$setNeighborChunk(index, null);
                ((LevelChunkBridge) neighbor).bridge$setNeighborChunk(oppositeIndex, null);
            }
        }
    }

    @Inject(method = "save", at = @At(value = "HEAD"), cancellable = true)
    private void impl$onSave(final ChunkAccess var1, final CallbackInfoReturnable<Boolean> cir) {
        if (var1 instanceof WorldChunk) {
            if (ShouldFire.CHUNK_EVENT_BLOCKS_SAVE_PRE) {
                final Vector3i chunkPos = VecHelper.toVector3i(var1.getPos());
                final ChunkEvent.Blocks.Save.Pre postSave = SpongeEventFactory.createChunkEventBlocksSavePre(PhaseTracker.getInstance().currentCause(),
                    ((BlockChunk) var1), chunkPos, (ResourceKey) (Object) this.level.dimension().location());
                SpongeCommon.post(postSave);
                if (postSave.isCancelled()) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Inject(method = "applyStep", at = @At("HEAD"), cancellable = true)
    private void impl$onApplyStep(final CallbackInfoReturnable<CompletableFuture<ChunkResult<ChunkAccess>>> cir) {
        if (!((ServerLevelBridge) this.level).bridge$isLoaded()) {
            cir.setReturnValue(ChunkHolder.UNLOADED_CHUNK_FUTURE);
        }
    }
}
