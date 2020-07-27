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

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.server.ServerWorld;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.BlockTransaction;
import org.spongepowered.common.event.tracking.context.transaction.ResultingTransactionBySideEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.EffectResult;
import org.spongepowered.common.event.tracking.context.transaction.effect.FormerWorldState;
import org.spongepowered.common.event.tracking.context.transaction.effect.ProcessingSideEffect;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ChunkPipeline implements BlockPipeline {

    private final @Nullable Supplier<Chunk> chunkSupplier;
    private final @Nullable Supplier<ServerWorld> serverWorld;
    private final @Nullable Supplier<ChunkSection> sectionSupplier;
    private final boolean wasEmpty;
    private final List<ResultingTransactionBySideEffect> chunkEffects;

    public ChunkPipeline(final Builder builder) {
        this.chunkSupplier = builder.chunkSupplier;
        this.chunkEffects = builder.effects;
        this.serverWorld = builder.serverWorld;
        this.sectionSupplier = builder.sectionSupplier;
        this.wasEmpty = Objects.requireNonNull(builder.sectionSupplier).get().isEmpty();
    }

    public Supplier<Chunk> getChunkSupplier() {
        return this.chunkSupplier;
    }

    public List<ResultingTransactionBySideEffect> getChunkEffects() {
        return this.chunkEffects;
    }

    public ServerWorld getServerWorld() {
        return Objects.requireNonNull(this.serverWorld, "ServerWorld Supplier is null in ChunkPipeline").get();
    }

    @Override
    public Chunk getAffectedChunk() {
        return Objects.requireNonNull(this.chunkSupplier, "Chunk Supplier is null in ChunkPipeline").get();
    }

    @Override
    public ChunkSection getAffectedSection() {
        return Objects.requireNonNull(this.sectionSupplier, "ChunkSection Supplier is null in ChunkPipeline").get();
    }

    public BlockState processChange(final PhaseContext<?> context, BlockTransaction.ChangeBlock changeBlock, final BlockState currentState, final BlockPos pos) {
        if (this.chunkEffects.isEmpty()) {
            return null;
        }
        final Chunk chunk = this.chunkSupplier.get();
        final ChunkSection chunkSection = this.sectionSupplier.get();
        final ServerWorld serverWorld = this.serverWorld.get();
        final FormerWorldState formerState = new FormerWorldState(currentState, oldOpacity, pos);
        for (final ResultingTransactionBySideEffect effect : this.chunkEffects) {
            try (final PhaseContext<?>.EffectTransactor ignored = context.pushTransactor(effect)) {
                final EffectResult result = effect.effect.processSideEffect(
                    serverWorld,
                    formerState,
                    newState,
                    flag
                );
                if (result.hasResult) {
                    return result.resultingState;
                }
            }
        }
        // if we've gotten here, means something is wrong, we didn't build our effects right.
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean wasEmpty() {
        return this.wasEmpty;
    }

    public static final class Builder {

        @Nullable Supplier<ServerWorld> serverWorld;
        @Nullable Supplier<Chunk> chunkSupplier;
        @Nullable Supplier<ChunkSection> sectionSupplier;
        List<ResultingTransactionBySideEffect> effects;

        public Builder addEffect(final ProcessingSideEffect effect) {
            if (this.effects == null) {
                this.effects = new LinkedList<>();
            }
            this.effects.add(new ResultingTransactionBySideEffect(Objects.requireNonNull(effect, "Effect is null")));
            return this;
        }

        public Builder chunk(final Chunk chunk) {
            final WeakReference<Chunk> worldRef = new WeakReference<>(chunk);
            this.chunkSupplier = () -> {
                final Chunk chunkRef = worldRef.get();
                if (chunkRef == null) {
                    throw new IllegalStateException("ServerWorld dereferenced");
                }
                return chunkRef;
            };
            return this;
        }

        public Builder chunkSection(final ChunkSection section) {
            final WeakReference<ChunkSection> worldRef = new WeakReference<>(section);
            this.sectionSupplier = () -> {
                final ChunkSection chunkRef = worldRef.get();
                if (chunkRef == null) {
                    throw new IllegalStateException("ServerWorld dereferenced");
                }
                return chunkRef;
            };
            return this;
        }

        public Builder world(final ServerWorld world) {
            final WeakReference<ServerWorld> worldRef = new WeakReference<>(world);
            this.serverWorld = () -> {
                final ServerWorld serverWorld = worldRef.get();
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
            return new ChunkPipeline(this);
        }

    }
}
