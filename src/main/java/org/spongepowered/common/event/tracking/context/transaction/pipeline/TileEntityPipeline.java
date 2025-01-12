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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.context.transaction.ResultingTransactionBySideEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.BlockChangeArgs;
import org.spongepowered.common.event.tracking.context.transaction.effect.EffectResult;
import org.spongepowered.common.event.tracking.context.transaction.effect.ProcessingSideEffect;
import org.spongepowered.common.world.SpongeBlockChangeFlag;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class TileEntityPipeline implements BlockPipeline {

    private final @Nullable Supplier<LevelChunk> chunkSupplier;
    private final @Nullable Supplier<ServerLevel> serverWorld;
    private final @Nullable Supplier<LevelChunkSection> sectionSupplier;
    private final List<ResultingTransactionBySideEffect<BlockPipeline, PipelineCursor, BlockChangeArgs, BlockState>> effects;

    private TileEntityPipeline(final Builder builder) {
        this.chunkSupplier = builder.chunkSupplier;
        this.serverWorld = builder.serverWorld;
        this.sectionSupplier = builder.sectionSupplier;
        this.effects = builder.effects;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder kickOff(final ServerLevel world, final BlockPos pos) {
        final WeakReference<ServerLevel> worldRef = new WeakReference<>(world);
        final LevelChunk chunk = world.getChunkAt(pos);
        final WeakReference<LevelChunk> chunkRef = new WeakReference<>(chunk);
        final WeakReference<LevelChunkSection> sectionRef = new WeakReference<>(chunk.getSection(chunk.getSectionIndex(pos.getY())));
        final Supplier<ServerLevel> worldSupplier = () -> Objects.requireNonNull(worldRef.get(), "ServerWorld de-referenced");
        final Supplier<LevelChunk> chunkSupplier = () -> Objects.requireNonNull(chunkRef.get(), "Chunk de-referenced");
        final Supplier<LevelChunkSection> chunkSectionSupplier = () -> Objects.requireNonNull(sectionRef.get(), "ChunkSection de-referenced");
        return TileEntityPipeline.builder()
            .chunk(chunkSupplier)
            .world(worldSupplier)
            .chunkSection(chunkSectionSupplier);
    }

    @Override
    public ServerLevel getServerWorld() {
        return Objects.requireNonNull(this.serverWorld, "ServerWorld Supplier is null in TileEntityPipeline").get();
    }

    @Override
    public LevelChunk getAffectedChunk() {
        return Objects.requireNonNull(this.chunkSupplier, "Chunk Supplier is null in TileEntityPipeline").get();
    }

    @Override
    public LevelChunkSection getAffectedSection() {
        return Objects.requireNonNull(this.sectionSupplier, "ChunkSection Supplier is null in TileEntityPipeline").get();
    }

    @Override
    public boolean wasEmpty() {
        return false;
    }

    public boolean processEffects(final PhaseContext<?> context, final PipelineCursor initialCursor) {
        PipelineCursor currentCursor = initialCursor;
        for (final ResultingTransactionBySideEffect<BlockPipeline, PipelineCursor, BlockChangeArgs, BlockState> effect : this.effects) {
            try (final EffectTransactor ignored = context.getTransactor().pushEffect(effect)) {
                final BlockChangeArgs args = new BlockChangeArgs(
                    currentCursor.state(),
                    (SpongeBlockChangeFlag) BlockChangeFlags.NONE,
                    currentCursor.limit()
                );
                final EffectResult<@Nullable BlockState> result = effect.effect.processSideEffect(
                    this,
                    currentCursor,
                    args
                );
                if (result.resultingState != currentCursor.state()) {
                    currentCursor = new PipelineCursor(
                        result.resultingState,
                        currentCursor.pos(),
                        currentCursor.tileEntity(),
                        currentCursor.destroyer(),
                        currentCursor.limit()
                    );
                }
                if (result.hasResult) {
                    return result.resultingState != null;
                }
            }
        }
        return false;
    }

    public static final class Builder {

        @Nullable Supplier<ServerLevel> serverWorld;
        @Nullable Supplier<LevelChunk> chunkSupplier;
        @Nullable Supplier<LevelChunkSection> sectionSupplier;
        List<ResultingTransactionBySideEffect<BlockPipeline, PipelineCursor, BlockChangeArgs, BlockState>> effects;

        public Builder addEffect(final ProcessingSideEffect<BlockPipeline, PipelineCursor, BlockChangeArgs, BlockState> effect) {
            if (this.effects == null) {
                this.effects = new LinkedList<>();
            }
            this.effects.add(new ResultingTransactionBySideEffect<>(Objects.requireNonNull(effect, "Effect is null")));
            return this;
        }

        public Builder chunk(final Supplier<LevelChunk> chunk) {
            this.chunkSupplier = chunk;
            return this;
        }

        public Builder chunkSection(final Supplier<LevelChunkSection> section) {
            this.sectionSupplier = section;
            return this;
        }

        public Builder world(final Supplier<ServerLevel> world) {
            this.serverWorld = world;
            return this;
        }

        public TileEntityPipeline build() {
            if (this.effects == null) {
                this.effects = Collections.emptyList();
            }
            Objects.requireNonNull(this.serverWorld, "ServerWorld must have been recorded!");
            Objects.requireNonNull(this.chunkSupplier, "Chunk must have been recorded!");
            return new TileEntityPipeline(this);
        }

    }
}
