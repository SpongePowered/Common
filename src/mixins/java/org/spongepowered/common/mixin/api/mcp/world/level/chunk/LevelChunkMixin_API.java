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
package org.spongepowered.common.mixin.api.mcp.world.level.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.biome.Biome;
import org.spongepowered.api.world.chunk.Chunk;
import org.spongepowered.api.world.volume.stream.StreamOptions;
import org.spongepowered.api.world.volume.stream.VolumeStream;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.accessor.world.level.chunk.ChunkBiomeContainerAccessor;
import org.spongepowered.common.world.volume.VolumeStreamUtils;
import org.spongepowered.common.world.volume.buffer.biome.ObjectArrayMutableBiomeBuffer;
import org.spongepowered.common.world.volume.buffer.block.ArrayMutableBlockBuffer;
import org.spongepowered.common.world.volume.buffer.blockentity.ObjectArrayMutableBlockEntityBuffer;
import org.spongepowered.common.world.volume.buffer.entity.ObjectArrayMutableEntityBuffer;
import org.spongepowered.math.vector.Vector3i;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Mixin(net.minecraft.world.level.chunk.LevelChunk.class)
public abstract class LevelChunkMixin_API implements Chunk {

    //@formatter:off
    @Shadow private ChunkBiomeContainer biomes;
    @Shadow private long inhabitedTime;
    @Shadow @Final private ChunkPos chunkPos;
    @Shadow @Final private Level level;

    @Shadow public abstract void shadow$setInhabitedTime(long p_177415_1_);
    //@formatter:on

    @Override
    public boolean setBiome(final int x, final int y, final int z, final Biome biome) {
        final net.minecraft.world.level.biome.Biome[] biomes = ((ChunkBiomeContainerAccessor) this.biomes).accessor$biomes();

        int maskedX = x & ChunkBiomeContainer.HORIZONTAL_MASK;
        int maskedY = Mth.clamp(y, 0, ChunkBiomeContainer.VERTICAL_MASK);
        int maskedZ = z & ChunkBiomeContainer.HORIZONTAL_MASK;

        final int WIDTH_BITS = ChunkBiomeContainerAccessor.accessor$WIDTH_BITS();
        final int posKey = maskedY << WIDTH_BITS + WIDTH_BITS | maskedZ << WIDTH_BITS | maskedX;
        biomes[posKey] = (net.minecraft.world.level.biome.Biome) (Object) biome;

        return true;
    }

    @Intrinsic
    public long impl$getInhabitedTime() {
        return this.inhabitedTime;
    }

    @Intrinsic
    public void impl$setInhabitedTime(long newInhabitedTime) {
        this.shadow$setInhabitedTime(newInhabitedTime);
    }

    @Override
    public Vector3i getChunkPosition() {
        return new Vector3i(this.chunkPos.x, 0, this.chunkPos.z);
    }

    @Override
    public double getRegionalDifficultyFactor() {
        return new DifficultyInstance(this.level.getDifficulty(), this.level.getDayTime(),
                this.getInhabitedTime(), this.level.getMoonBrightness()).getEffectiveDifficulty();
    }

    @Override
    public double getRegionalDifficultyPercentage() {
        return new DifficultyInstance(this.level.getDifficulty(), this.level.getDayTime(),
                this.getInhabitedTime(), this.level.getMoonBrightness()).getSpecialMultiplier();
    }

    @Override
    public org.spongepowered.api.world.World<?, ?> getWorld() {
        return ((org.spongepowered.api.world.World<?, ?>) this.level);
    }

    @Override
    public VolumeStream<Chunk, Entity> getEntityStream(
        Vector3i min, Vector3i max, StreamOptions options
    ) {
        VolumeStreamUtils.validateStreamArgs(
            Objects.requireNonNull(min, "min"), Objects.requireNonNull(max, "max"),
            Objects.requireNonNull(options, "options"));

        final boolean shouldCarbonCopy = options.carbonCopy();
        final Vector3i size = max.sub(min).add(1, 1 ,1);
        final @MonotonicNonNull ObjectArrayMutableEntityBuffer backingVolume;
        if (shouldCarbonCopy) {
            backingVolume = new ObjectArrayMutableEntityBuffer(min, size);
        } else {
            backingVolume = null;
        }

        return VolumeStreamUtils.<Chunk, Entity, net.minecraft.world.entity.Entity, LevelChunk, UUID>generateStream(
            options,
            this,
            (LevelChunk) (Object) this,
            // Entity Accessor
            (chunk) -> VolumeStreamUtils.getEntitiesFromChunk(min, max, chunk),
            // Entity Identity Function
            VolumeStreamUtils.getOrCloneEntityWithVolume(shouldCarbonCopy, backingVolume, this.level),
            (key, entity) -> entity.getUUID(),
            // Filtered Position Entity Accessor
            (entityUuid, chunk) -> {
                final net.minecraft.world.entity.Entity tileEntity = shouldCarbonCopy
                    ? (net.minecraft.world.entity.Entity) backingVolume.getEntity(entityUuid).orElse(null)
                    : (net.minecraft.world.entity.Entity) chunk.getWorld().getEntity(entityUuid).orElse(null);
                return new Tuple<>(tileEntity.blockPosition(), tileEntity);
            }
            );
    }

    @Override
    public VolumeStream<Chunk, BlockState> getBlockStateStream(
        Vector3i min, Vector3i max, StreamOptions options
    ) {
        VolumeStreamUtils.validateStreamArgs(Objects.requireNonNull(min, "min"), Objects.requireNonNull(max, "max"),
            Objects.requireNonNull(options, "options"));

        final boolean shouldCarbonCopy = options.carbonCopy();
        final Vector3i size = max.sub(min).add(1, 1 ,1);
        final @MonotonicNonNull ArrayMutableBlockBuffer backingVolume;
        if (shouldCarbonCopy) {
            backingVolume = new ArrayMutableBlockBuffer(min, size);
        } else {
            backingVolume = null;
        }

        return VolumeStreamUtils.<Chunk, BlockState, net.minecraft.world.level.block.state.BlockState, ChunkAccess, BlockPos>generateStream(
            options,
            // Ref
            (Chunk) this,
            (LevelChunk) (Object) this,
            // Entity Accessor
            VolumeStreamUtils.getBlockStatesForSections(min, max),
            // IdentityFunction
            (pos, blockState) -> {
                if (shouldCarbonCopy) {
                    backingVolume.setBlock(pos, blockState);
                }
            },
            // Biome by block position
            (key, biome) -> key,
            // Filtered Position Entity Accessor
            (blockPos, world) -> {
                final net.minecraft.world.level.block.state.BlockState tileEntity = shouldCarbonCopy
                    ? backingVolume.getBlock(blockPos)
                    : ((LevelReader) world).getBlockState(blockPos);
                return new Tuple<>(blockPos, tileEntity);
            }
        );
    }

    @Override
    public VolumeStream<Chunk, BlockEntity> getBlockEntityStream(
        Vector3i min, Vector3i max, StreamOptions options
    ) {
        VolumeStreamUtils.validateStreamArgs(Objects.requireNonNull(min, "min"), Objects.requireNonNull(max, "max"),
            Objects.requireNonNull(options, "options"));

        final boolean shouldCarbonCopy = options.carbonCopy();
        final Vector3i size = max.sub(min).add(1, 1 ,1);
        final @MonotonicNonNull ObjectArrayMutableBlockEntityBuffer backingVolume;
        if (shouldCarbonCopy) {
            backingVolume = new ObjectArrayMutableBlockEntityBuffer(min, size);
        } else {
            backingVolume = null;
        }

        return VolumeStreamUtils.<Chunk, BlockEntity, net.minecraft.world.level.block.entity.BlockEntity, ChunkAccess, BlockPos>generateStream(
            options,
            // Ref
            (Chunk) this,
            (LevelChunk) (Object) this,
            // Entity Accessor
            this::impl$getBlockEntitiesStream,
            // IdentityFunction
            VolumeStreamUtils.getBlockEntityOrCloneToBackingVolume(shouldCarbonCopy, backingVolume, this.level),
            // Biome by block position
            (key, biome) -> key,
            // Filtered Position Entity Accessor
            (blockPos, world) -> {
                final net.minecraft.world.level.block.entity.@Nullable BlockEntity tileEntity = shouldCarbonCopy
                    ? (net.minecraft.world.level.block.entity.BlockEntity) backingVolume.getBlockEntity(blockPos.getX(), blockPos.getY(), blockPos.getZ())
                    .orElse(null)
                    : ((LevelReader) world).getBlockEntity(blockPos);
                return new Tuple<>(blockPos, tileEntity);
            }
        );
    }

    private Stream<Map.Entry<BlockPos, net.minecraft.world.level.block.entity.BlockEntity>> impl$getBlockEntitiesStream(ChunkAccess chunk) {
        return chunk instanceof LevelChunk ? ((LevelChunk) chunk).getBlockEntities().entrySet().stream() : Stream.empty();
    }

    @Override
    public VolumeStream<Chunk, Biome> getBiomeStream(
        Vector3i min, Vector3i max, StreamOptions options
    ) {
        VolumeStreamUtils.validateStreamArgs(Objects.requireNonNull(min, "min"), Objects.requireNonNull(max, "max"),
            Objects.requireNonNull(options, "options"));

        final boolean shouldCarbonCopy = options.carbonCopy();
        final Vector3i size = max.sub(min).add(1, 1 ,1);
        final @MonotonicNonNull ObjectArrayMutableBiomeBuffer backingVolume;
        if (shouldCarbonCopy) {
            backingVolume = new ObjectArrayMutableBiomeBuffer(min, size);
        } else {
            backingVolume = null;
        }
        return VolumeStreamUtils.<Chunk, Biome, net.minecraft.world.level.biome.Biome, ChunkAccess, BlockPos>generateStream(
            options,
            // Ref
            (Chunk) this,
            (LevelChunk) (Object) this,
            // Entity Accessor
            VolumeStreamUtils.getBiomesForChunkByPos((LevelReader) (Object) this, min, max),
            // IdentityFunction
            (pos, biome) -> {
                if (shouldCarbonCopy) {
                    backingVolume.setBiome(pos, biome);
                }
            },            // Biome by block position
            (key, biome) -> key,
            // Filtered Position Entity Accessor
            (blockPos, world) -> {
                final net.minecraft.world.level.biome.Biome biome = shouldCarbonCopy
                    ? backingVolume.getNativeBiome(blockPos.getX(), blockPos.getY(), blockPos.getZ())
                    : ((LevelReader) world.getWorld()).getBiome(blockPos);
                return new Tuple<>(blockPos, biome);
            }
        );
    }
// TODO implement the rest of it
}
