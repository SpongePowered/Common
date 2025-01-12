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
package org.spongepowered.common.mixin.tracker.world.level.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.apache.logging.log4j.Level;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.bridge.CreatorTrackedBridge;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.bridge.world.level.block.state.BlockStateBridge;
import org.spongepowered.common.bridge.world.level.chunk.ActiveChunkReferantBridge;
import org.spongepowered.common.bridge.world.level.chunk.LevelChunkBridge;
import org.spongepowered.common.bridge.world.level.chunk.TrackedLevelChunkBridge;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhasePrinter;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.context.transaction.block.ChangeBlock;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.ChunkPipeline;
import org.spongepowered.common.event.tracking.phase.generation.ChunkLoadContext;
import org.spongepowered.common.event.tracking.phase.generation.GenerationPhase;
import org.spongepowered.common.util.PrettyPrinter;
import org.spongepowered.common.world.BlockChange;
import org.spongepowered.common.world.SpongeBlockChangeFlag;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;


@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin_Tracker extends ChunkAccessMixin_Tracker implements TrackedLevelChunkBridge, LevelHeightAccessor {
    // @formatter:off
    @Shadow @Final net.minecraft.world.level.Level level;

    @Shadow public abstract @Nullable BlockEntity shadow$getBlockEntity(BlockPos pos, LevelChunk.EntityCreationType creationMode);
    @Shadow public abstract BlockState getBlockState(BlockPos pos);
    // @formatter:on
    private @MonotonicNonNull PhaseContext<@NonNull ?> tracker$postProcessContext = null;

    @Inject(method = "setBlockState", at = @At("HEAD"), cancellable = true)
    private void tracker$sanityCheckServerWorldSetBlockState(final BlockPos pos, final BlockState state, final int flag,
        final CallbackInfoReturnable<BlockState> cir
    ) {
        if (!((LevelBridge) this.level).bridge$isFake()) {
            new PrettyPrinter(80).add("Illegal Direct Chunk Access")
                .hr()
                .add(new IllegalAccessException("No one should be accessing Chunk.setBlock in a ServerWorld's environment"))
                .log(PhaseTracker.LOGGER, Level.WARN);
            cir.setReturnValue(null);
        }
    }

    /**
     * Technically a full overwrite for {@link LevelChunk#setBlockState(BlockPos, BlockState, boolean)}
     * and due to Sponge's hijacking of {@link ServerLevel#setBlock(BlockPos, BlockState, int)},
     * it needs to be able to record transactions when necessary. This implementation allows for us to
     * further specify the types of transactions and what proxies are needing to set up where.
     *
     * @param pos The position changing
     * @param newState The new state
     * @param currentState The current state - passed in from either chunk or world
     * @param flag The sponge change flag, converted from an int to a proper struct
     * @return The changed block state if not null
     * @author gabizou - January 13th, 2020 - Minecraft 1.14.3
     */
    @Override
    @NonNull
    public ChunkPipeline bridge$createChunkPipeline(final BlockPos pos, final BlockState newState, final BlockState currentState,
            final SpongeBlockChangeFlag flag, final int limit) {
        final boolean isFake = ((LevelBridge) this.level).bridge$isFake();
        if (isFake) {
            throw new IllegalStateException("Cannot call ChunkBridge.bridge$buildChunkPipeline in non-Server managed worlds");
        }
        // int var4 = var1.getY();
        // LevelChunkSection var6 = this.sections[var5];
        // int i = pos.getX() & 15;
        final int xPos = pos.getX() & 15;
        // int j = pos.getY();
        final int yPos = pos.getY();
        // int k = pos.getZ() & 15;
        final int zPos = pos.getZ() & 15;
        final int sectionIndex = ((LevelChunk) (Object) this).getSectionIndex(yPos);
        // Sponge - get the moving flag from our flag construct
        final LevelChunkSection chunksection = this.shadow$getSection(this.getSectionIndex(yPos));
        final var isEmpty = chunksection.hasOnlyAir();
        if (isEmpty && newState.isAir()) {
            return ChunkPipeline.nullReturn((LevelChunk) (Object) this, (ServerLevel) this.level);
        }

        // Sponge Start - Build out the BlockTransaction
        final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance((ServerLevel) this.level).getPhaseContext();
        final @Nullable BlockEntity existing = this.shadow$getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
        // Build a transaction maybe?
        final WeakReference<ServerLevel> ref = new WeakReference<>((ServerLevel) this.level);
        final SpongeBlockSnapshot snapshot = TrackingUtil.createPooledSnapshot(currentState, pos, flag, limit, existing,
            () -> Objects.requireNonNull(ref.get(), "ServerWorld dereferenced"),
            Optional::empty, Optional::empty
        );

        // Pulled up from below
        final ChangeBlock transaction = context.createTransaction(snapshot, newState, flag);

        snapshot.blockChange = context.associateBlockChangeWithSnapshot(
            newState,
            currentState
        );
        if (((BlockStateBridge) snapshot.state()).bridge$hasTileEntity()
            && (snapshot.blockChange == BlockChange.BREAK || snapshot.blockChange == BlockChange.MODIFY)) {
            transaction.queuedRemoval = existing;
        }

        final ChunkPipeline.Builder builder = ChunkPipeline.builder()
            .kickOff(transaction)
            .chunk((LevelChunk) (Object) this)
            .chunkSection(chunksection)
            .world((ServerLevel) this.level);

        // Populate the effects
        transaction.populateChunkEffects(builder);

        return builder.build();
    }

    @Inject(method = "postProcessGeneration", at = @At("HEAD"))
    private void tracker$startChunkPostProcess(final CallbackInfo ci) {
        final PhaseTracker phaseTracker = PhaseTracker.getWorldInstance((ServerLevel) this.level);
        if (this.tracker$postProcessContext != null) {
            PhasePrinter.printMessageWithCaughtException(phaseTracker, "Expected to not have a chunk post process", "Chunk Post Process has not completed!", GenerationPhase.State.CHUNK_LOADING, this.tracker$postProcessContext, new NullPointerException("spongecommon.ChunkMixin_Tracker:tracker$postProcessContext is Null"));
            this.tracker$postProcessContext.close();
        }
        this.tracker$postProcessContext = GenerationPhase.State.CHUNK_LOADING.createPhaseContext(phaseTracker)
            .chunk((LevelChunk) (Object) this)
            .world((ServerLevel) this.level)
            .buildAndSwitch();
    }

    @Inject(method = "postProcessGeneration", at = @At("RETURN"))
    private void tracker$endChunkPostProcess(final CallbackInfo ci) {
        if (this.tracker$postProcessContext == null) {
            PhasePrinter.printMessageWithCaughtException(PhaseTracker.getInstance(), "Expected to complete Chunk Post Process", "Chunk Post Process has a null PhaseContext", new NullPointerException("spongecommon.ChunkMixin_Tracker:tracker$postProcessContext is Null"));
        }
        this.tracker$postProcessContext.close();
        this.tracker$postProcessContext = null;
    }

    @Redirect(method = "removeBlockEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;setRemoved()V"))
    private void tracker$resetTileEntityActiveChunk(final BlockEntity tileEntityIn) {
        ((ActiveChunkReferantBridge) tileEntityIn).bridge$setActiveChunk(null);
        tileEntityIn.setRemoved();
    }

    @Inject(
        method = "setBlockEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;clearRemoved()V"))
    private void tracker$SetActiveChunkOnTileEntityAdd(final BlockEntity tileEntityIn, final CallbackInfo ci) {
        ((ActiveChunkReferantBridge) tileEntityIn).bridge$setActiveChunk(this);
        // Make sure to set creator/notifier for TE if any chunk data exists
        // Failure to do this during chunk load will cause TE's to not have proper user tracking
        ((CreatorTrackedBridge) tileEntityIn).tracker$setTrackedUUID(PlayerTracker.Type.CREATOR, ((LevelChunkBridge) this).bridge$getBlockCreatorUUID(tileEntityIn.getBlockPos()).orElse(null));
        ((CreatorTrackedBridge) tileEntityIn).tracker$setTrackedUUID(PlayerTracker.Type.NOTIFIER, null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "unpackTicks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ticks/LevelChunkTicks;unpack(J)V"))
    private void tracker$wrapRescheduledTicks(final LevelChunkTicks instance, final long $$0) {
        final PhaseTracker phaseTracker = PhaseTracker.getWorldInstance((ServerLevel) this.level);
        if (!phaseTracker.onSidedThread()) {
            return;
        }
        try (final ChunkLoadContext context = GenerationPhase.State.CHUNK_LOADING.createPhaseContext(phaseTracker)) {
            context.chunk((LevelChunk) (Object) this);
            context.buildAndSwitch();
            instance.unpack($$0);
        }
    }
}
