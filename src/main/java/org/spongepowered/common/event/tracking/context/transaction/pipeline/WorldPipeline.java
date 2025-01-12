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
package org.spongepowered.common.event.tracking.context.transaction.pipeline;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.context.transaction.ResultingTransactionBySideEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.BlockChangeArgs;
import org.spongepowered.common.event.tracking.context.transaction.effect.EffectResult;
import org.spongepowered.common.event.tracking.context.transaction.effect.ProcessingSideEffect;
import org.spongepowered.common.world.SpongeBlockChangeFlag;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class WorldPipeline implements BlockPipeline {

    private final Supplier<LevelChunk> chunkSupplier;
    private final Supplier<ServerLevel> serverWorld;
    private final Supplier<LevelChunkSection> sectionSupplier;
    private final boolean wasEmpty;
    private final List<ResultingTransactionBySideEffect<BlockPipeline, PipelineCursor, BlockChangeArgs, BlockState>> worldEffects;
    private final ChunkPipeline chunkPipeline;

    WorldPipeline(final Builder builder) {
        this.chunkSupplier = builder.chunkSupplier;
        this.worldEffects = builder.effects;
        this.serverWorld = builder.serverWorld;
        this.sectionSupplier = builder.sectionSupplier;
        final @Nullable LevelChunkSection chunkSection = Objects.requireNonNull(builder.sectionSupplier).get();
        this.wasEmpty = chunkSection == null || chunkSection.hasOnlyAir();
        this.chunkPipeline = builder.chunkPipeline;
    }

    public ServerLevel getServerWorld() {
        return Objects.requireNonNull(this.serverWorld, "ServerWorld Supplier is null in ChunkPipeline").get();
    }

    @Override
    public LevelChunk getAffectedChunk() {
        return Objects.requireNonNull(this.chunkSupplier, "Chunk Supplier is null in ChunkPipeline").get();
    }

    @Override
    public LevelChunkSection getAffectedSection() {
        return Objects.requireNonNull(this.sectionSupplier, "ChunkSection Supplier is null in ChunkPipeline").get();
    }

    public boolean processEffects(final PhaseContext<?> context, final BlockState currentState,
        final BlockState newProposedState, final BlockPos pos,
        final @Nullable Entity destroyer, final SpongeBlockChangeFlag flag,
        final int limit
    ) {
        if (this.worldEffects.isEmpty()) {
            return false;
        }
        final ServerLevel serverWorld = Objects.requireNonNull(this.serverWorld).get();
        // Keep track of the existing block entity prior to processing the chunk pipeline
        // and the reasoning is that in several cases where the block entity that is being removed
        // will no longer be available. This could be avoided by having the "previous cursor" returned
        // from ChunkPipeline, but alas.... that's a refactor for another time.
        final @Nullable BlockEntity existing = this.chunkSupplier.get().getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
        // We have to get the "old state" from
        final @Nullable BlockState oldState = this.chunkPipeline.processChange(context, currentState, newProposedState, pos, limit);
        if (oldState == null) {
            return false;
        }
        final int oldOpacity = oldState.getLightBlock();
        PipelineCursor formerState = new PipelineCursor(oldState, pos, existing, destroyer, limit);

        for (final ResultingTransactionBySideEffect<BlockPipeline, PipelineCursor, BlockChangeArgs, BlockState> effect : this.worldEffects) {
            try (final EffectTransactor ignored = context.getTransactor().pushEffect(effect)) {
                final var args = new BlockChangeArgs(newProposedState, flag, limit);
                final EffectResult<@Nullable BlockState> result = effect.effect.processSideEffect(
                    this,
                    formerState,
                    args
                );
                if (result.hasResult) {
                    return result.resultingState != null;
                }
                if (formerState.drops().isEmpty() && !result.drops.isEmpty()) {
                    formerState = new PipelineCursor(oldState, pos, existing, formerState.destroyer(), result.drops, limit);
                }
            }
        }
        // if we've gotten here, means something is wrong, we didn't build our effects right.
        return false;
    }

    public static Builder builder(final ChunkPipeline pipeline) {
        return new Builder(Objects.requireNonNull(pipeline, "ChunkPipeline cannot be null!"));
    }

    public boolean wasEmpty() {
        return this.wasEmpty;
    }

    public static final class Builder {

        final Supplier<ServerLevel> serverWorld;
        final Supplier<LevelChunk> chunkSupplier;
        final Supplier<@Nullable LevelChunkSection> sectionSupplier;
        @MonotonicNonNull List<ResultingTransactionBySideEffect<BlockPipeline, PipelineCursor, BlockChangeArgs, BlockState>> effects;
        final ChunkPipeline chunkPipeline;

        Builder(final ChunkPipeline chunkPipeline) {
            this.serverWorld = chunkPipeline::getServerWorld;
            this.chunkSupplier = chunkPipeline::getAffectedChunk;
            this.sectionSupplier = chunkPipeline::getAffectedSection;
            this.chunkPipeline = chunkPipeline;
        }

        public Builder addEffect(final ProcessingSideEffect<BlockPipeline, PipelineCursor, BlockChangeArgs, BlockState> effect) {
            if (this.effects == null) {
                this.effects = new LinkedList<>();
            }
            this.effects.add(new ResultingTransactionBySideEffect<>(Objects.requireNonNull(effect, "Effect is null")));
            return this;
        }

        public WorldPipeline build() {
            if (this.effects == null) {
                this.effects = Collections.emptyList();
            }
            return new WorldPipeline(this);
        }

    }
}
