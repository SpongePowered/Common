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
import org.spongepowered.common.event.tracking.context.transaction.block.ChangeBlock;
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

public final class ChunkPipeline implements BlockPipeline {

    private final @Nullable Supplier<LevelChunk> chunkSupplier;
    private final @Nullable Supplier<ServerLevel> serverWorld;
    private final @Nullable Supplier<LevelChunkSection> sectionSupplier;
    private final boolean wasEmpty;
    private final List<ResultingTransactionBySideEffect> chunkEffects;
    final ChangeBlock transaction;

    public static ChunkPipeline nullReturn(final LevelChunk chunk, final ServerLevel world) {
        return new ChunkPipeline(chunk, world);
    }

    private ChunkPipeline(final LevelChunk chunk, final ServerLevel world) {
        final WeakReference<LevelChunk> chunkWeakReference = new WeakReference<>(chunk);
        this.chunkSupplier = () -> chunkWeakReference.get();
        final WeakReference<ServerLevel> serverWorldWeakReference = new WeakReference<>(world);
        this.serverWorld = () -> serverWorldWeakReference.get();
        this.sectionSupplier = () -> null;
        this.wasEmpty = true;
        this.chunkEffects = Collections.emptyList();
        this.transaction = null;
    }

    ChunkPipeline(final Builder builder) {
        this.chunkSupplier = builder.chunkSupplier;
        this.chunkEffects = builder.effects;
        this.serverWorld = builder.serverWorld;
        this.sectionSupplier = builder.sectionSupplier;
        this.wasEmpty = Objects.requireNonNull(builder.sectionSupplier).get().hasOnlyAir();
        this.transaction = builder.transaction;
    }

    public Supplier<LevelChunk> getChunkSupplier() {
        return this.chunkSupplier;
    }

    public List<ResultingTransactionBySideEffect> getChunkEffects() {
        return this.chunkEffects;
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

    public @Nullable BlockState processChange(final PhaseContext<?> context, final BlockState currentState, final BlockState proposedState,
        final BlockPos pos,
        final int limit
    ) {
        if (this.chunkEffects.isEmpty()) {
            return null;
        }
        final ServerLevel serverWorld = this.serverWorld.get();
        final int oldOpacity = currentState.getLightBlock();
        final SpongeBlockChangeFlag flag = this.transaction.getBlockChangeFlag();
        final @Nullable BlockEntity existing = this.chunkSupplier.get().getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
        PipelineCursor formerState = new PipelineCursor(currentState, pos, existing, (Entity) null, limit);

        for (final ResultingTransactionBySideEffect effect : this.chunkEffects) {
            try (final EffectTransactor ignored = context.getTransactor().pushEffect(effect)) {
                final var args = new BlockChangeArgs(proposedState, flag, limit);
                final EffectResult<@Nullable BlockState> result = effect.effect.processSideEffect(
                    this,
                    formerState,
                    args
                );
                if (result.hasResult) {
                    return result.resultingState;
                }
                if (formerState.drops().isEmpty() && !result.drops.isEmpty()) {
                    formerState = new PipelineCursor(currentState, pos, existing, null, result.drops, limit);
                }
            }
        }
        // if we've gotten here, means something is wrong, we didn't build our effects right.
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean wasEmpty() {
        return this.wasEmpty;
    }

    public static final class Builder {

        @Nullable Supplier<ServerLevel> serverWorld;
        @Nullable Supplier<LevelChunk> chunkSupplier;
        @Nullable Supplier<LevelChunkSection> sectionSupplier;
        boolean wasSectionEmpty;
        @MonotonicNonNull ChangeBlock transaction;
        List<ResultingTransactionBySideEffect> effects;

        public Builder kickOff(final ChangeBlock transaction) {
            this.transaction = Objects.requireNonNull(transaction, "ChangeBlock transaction cannot be null!");
            return this;
        }
        public Builder addEffect(final ProcessingSideEffect effect) {
            if (this.effects == null) {
                this.effects = new LinkedList<>();
            }
            this.effects.add(new ResultingTransactionBySideEffect(Objects.requireNonNull(effect, "Effect is null")));
            return this;
        }

        public Builder chunk(final LevelChunk chunk) {
            final WeakReference<LevelChunk> worldRef = new WeakReference<>(chunk);
            this.chunkSupplier = () -> {
                final LevelChunk chunkRef = worldRef.get();
                if (chunkRef == null) {
                    throw new IllegalStateException("ServerWorld dereferenced");
                }
                return chunkRef;
            };
            return this;
        }

        public Builder chunkSection(final LevelChunkSection section) {
            final WeakReference<LevelChunkSection> worldRef = new WeakReference<>(section);
            this.sectionSupplier = () -> {
                final LevelChunkSection chunkRef = worldRef.get();
                if (chunkRef == null) {
                    throw new IllegalStateException("ServerWorld dereferenced");
                }
                return chunkRef;
            };
            this.wasSectionEmpty = section.hasOnlyAir();
            return this;
        }

        public Builder world(final ServerLevel world) {
            final WeakReference<ServerLevel> worldRef = new WeakReference<>(world);
            this.serverWorld = () -> {
                final ServerLevel serverWorld = worldRef.get();
                if (serverWorld == null) {
                    throw new IllegalStateException("ServerWorld dereferenced");
                }
                return serverWorld;
            };
            return this;
        }

        public ChunkPipeline build() {
            if (this.effects == null) {
                this.effects = Collections.emptyList();
            }
            Objects.requireNonNull(this.transaction, "ChangeBlock transaction must have been recorded!");
            return new ChunkPipeline(this);
        }

    }
}
