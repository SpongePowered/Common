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
package org.spongepowered.common.mixin.api.mcp.world;

import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.ICollisionReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.HeightType;
import org.spongepowered.api.world.ProtoWorld;
import org.spongepowered.api.world.WorldBorder;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.chunk.ProtoChunk;
import org.spongepowered.api.world.dimension.DimensionType;
import org.spongepowered.api.world.volume.game.Region;
import org.spongepowered.api.world.volume.stream.StreamOptions;
import org.spongepowered.api.world.volume.stream.VolumeStream;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.accessor.tileentity.TileEntityAccessor;
import org.spongepowered.common.world.volume.VolumeStreamUtils;
import org.spongepowered.common.world.volume.buffer.biome.ObjectArrayMutableBiomeBuffer;
import org.spongepowered.common.world.volume.buffer.block.ArrayMutableBlockBuffer;
import org.spongepowered.common.world.volume.buffer.blockentity.ObjectArrayMutableBlockEntityBuffer;
import org.spongepowered.common.world.volume.buffer.entity.ObjectArrayMutableEntityBuffer;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings({"RedundantTypeArguments", "unchecked", "RedundantCast"})
@Mixin(IWorldReader.class)
@Implements(@Interface(iface = Region.class, prefix = "readable$"))
public interface IWorldReaderMixin_API<R extends Region<R>> extends Region<R> {

    // @formatter:off
    @Nullable @Shadow IChunk shadow$getChunk(int p_217353_1_, int p_217353_2_, ChunkStatus p_217353_3_, boolean p_217353_4_);
    @Deprecated @Shadow boolean shadow$hasChunk(int p_217354_1_, int p_217354_2_);
    @Shadow int shadow$getHeight(Heightmap.Type p_201676_1_, int p_201676_2_, int p_201676_3_);
    @Shadow int shadow$getSkyDarken();
    @Shadow int shadow$getSeaLevel();
    @Shadow boolean shadow$isWaterAt(BlockPos p_201671_1_);
    @Deprecated @Shadow boolean shadow$hasChunksAt(int p_217344_1_, int p_217344_2_, int p_217344_3_, int p_217344_4_, int p_217344_5_, int p_217344_6_);
    @Shadow net.minecraft.world.DimensionType shadow$dimensionType();
    @Shadow boolean shadow$containsAnyLiquid(AxisAlignedBB bb);
    @Shadow Biome shadow$getBiome(BlockPos p_226691_1_);
    // @formatter:on

    // Region

    @Override
    default DimensionType getDimensionType() {
        return (DimensionType) this.shadow$dimensionType();
    }

    @Override
    default WorldBorder getBorder() {
        return (WorldBorder) ((ICollisionReader) this).getWorldBorder();
    }

    @Override
    default boolean isInBorder(final Entity entity) {
        return ((ICollisionReader) this).getWorldBorder().isWithinBounds(((net.minecraft.entity.Entity) entity).getBoundingBox());
    }

    @Override
    default boolean canSeeSky(final int x, final int y, final int z) {
        return ((IBlockDisplayReader) this).canSeeSky(new BlockPos(x, y, z));
    }

    @Override
    default boolean hasLiquid(final int x, final int y, final int z) {
        return this.shadow$isWaterAt(new BlockPos(x, y, z));
    }

    @Override
    default boolean containsAnyLiquids(final AABB aabb) {
        final Vector3d max = aabb.getMax();
        final Vector3d min = aabb.getMin();
        return this.shadow$containsAnyLiquid(new AxisAlignedBB(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ()));
    }

    @Override
    default int getSkylightSubtracted() {
        return this.shadow$getSkyDarken();
    }

    @Intrinsic
    default int readable$getSeaLevel() {
        return this.shadow$getSeaLevel();
    }

    @Override
    default boolean isAreaLoaded(final int xStart, final int yStart, final int zStart, final int xEnd, final int yEnd,
        final int zEnd, final boolean allowEmpty) {
        return this.shadow$hasChunksAt(xStart, yStart, zStart, xEnd, yEnd, zEnd);
    }

    // RandomProvider

    /**
     * Generates a random for usage, specific cases where randoms are being stored,
     * will override this appropriately.
     *
     * @return A generated Random
     */
    @Override
    default Random getRandom() {
        return new Random();
    }

    // EntityVolume

    @Override
    default Optional<Entity> getEntity(final UUID uuid) {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of IWorldReaderBase that isn't part of Sponge API");
    }

    @Override
    default Collection<? extends Player> getPlayers() {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of IWorldReaderBase that isn't part of Sponge API");
    }

    @Override
    default <T extends Entity> Collection<? extends T> getEntities(final Class<? extends T> entityClass, final AABB box,
        @Nullable final Predicate<? super T> predicate) {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of IWorldReaderBase that isn't part of Sponge API");
    }

    // ChunkVolume


    @SuppressWarnings("ConstantConditions")
    @Override
    default ProtoChunk<@NonNull ?> getChunk(final int x, final int y, final int z) {
        return (ProtoChunk<@NonNull ?>) this.shadow$getChunk(x >> 4, z >> 4, ChunkStatus.EMPTY, true);
    }

    @Override
    default boolean isChunkLoaded(final int x, final int y, final int z, final boolean allowEmpty) {
        return this.shadow$hasChunk(x >> 4, z >> 4);
    }

    @Override
    default boolean hasChunk(final int x, final int y, final int z) {
        return this.shadow$hasChunk(x >> 4, z >> 4);
    }

    @Override
    default boolean hasChunk(final Vector3i position) {
        return this.shadow$hasChunk(position.getX() >> 4, position.getZ() >> 4);
    }

    // HeightAwareVolume

    @Override
    default int getHeight(final HeightType type, final int x, final int z) {
        return this.shadow$getHeight((Heightmap.Type) (Object) type, x, z);
    }

    @Override
    default BiomeType getBiome(final int x, final int y, final int z) {
        return (BiomeType) (Object) this.shadow$getBiome(new BlockPos(x, y, z));
    }

    @Override
    default VolumeStream<R, BiomeType> getBiomeStream(final Vector3i min, final Vector3i max, final StreamOptions options) {
        VolumeStreamUtils.validateStreamArgs(min, max, options);

        final boolean shouldCarbonCopy = options.carbonCopy();
        final Vector3i size = max.sub(min).add(1, 1 ,1);
        final @MonotonicNonNull ObjectArrayMutableBiomeBuffer backingVolume;
        if (shouldCarbonCopy) {
            backingVolume = new ObjectArrayMutableBiomeBuffer(min, size);
        } else {
            backingVolume = null;
        }
        return VolumeStreamUtils.<R, BiomeType, Biome, IChunk, BlockPos>generateStream(
            min,
            max,
            options,
            // Ref
            (R) this,
            // IdentityFunction
            (pos, biome) -> {
                if (shouldCarbonCopy) {
                    backingVolume.setBiome(pos, biome);
                }
            },
            // ChunkAccessor
            VolumeStreamUtils.getChunkAccessorByStatus((IWorldReader) (Object) this, options.loadingStyle().generateArea()),
            // Biome by key
            (key, biome) -> key,
            // Entity Accessor
            VolumeStreamUtils.getBiomesForChunkByPos((IWorldReader) (Object) this, min, max)
            ,
            // Filtered Position Entity Accessor
            (blockPos, world) -> {
                final Biome biome = shouldCarbonCopy
                    ? backingVolume.getNativeBiome(blockPos.getX(), blockPos.getY(), blockPos.getZ())
                    : ((IWorldReader) world).getBiome(blockPos);
                return new Tuple<>(blockPos, biome);
            }
        );
    }

    @Override
    default VolumeStream<R, BlockState> getBlockStateStream(final Vector3i min, final Vector3i max, final StreamOptions options) {
        VolumeStreamUtils.validateStreamArgs(min, max, options);

        final boolean shouldCarbonCopy = options.carbonCopy();
        final Vector3i size = max.sub(min).add(1, 1 ,1);
        final @MonotonicNonNull ArrayMutableBlockBuffer backingVolume;
        if (shouldCarbonCopy) {
            backingVolume = new ArrayMutableBlockBuffer(min, size);
        } else {
            backingVolume = null;
        }
        return VolumeStreamUtils.<R, BlockState, net.minecraft.block.BlockState, IChunk, BlockPos>generateStream(
            min,
            max,
            options,
            // Ref
            (R) this,
            // IdentityFunction
            (pos, blockState) -> {
                if (shouldCarbonCopy) {
                    backingVolume.setBlock(pos, blockState);
                }
            },
            // ChunkAccessor
            VolumeStreamUtils.getChunkAccessorByStatus((IWorldReader) (Object) this, options.loadingStyle().generateArea()),
            // Biome by block position
            (key, biome) -> key,
            // Entity Accessor
            VolumeStreamUtils.getBlockStatesForSections(min, max),
            // Filtered Position Entity Accessor
            (blockPos, world) -> {
                final net.minecraft.block.BlockState tileEntity = shouldCarbonCopy
                    ? backingVolume.getBlock(blockPos)
                    : ((IWorldReader) world).getBlockState(blockPos);
                return new Tuple<>(blockPos, tileEntity);
            }
        );
    }

    @Override
    default VolumeStream<R, BlockEntity> getBlockEntityStream(final Vector3i min, final Vector3i max, final StreamOptions options) {
        VolumeStreamUtils.validateStreamArgs(min, max, options);

        final boolean shouldCarbonCopy = options.carbonCopy();
        final Vector3i size = max.sub(min).add(1, 1 ,1);
        final @MonotonicNonNull ObjectArrayMutableBlockEntityBuffer backingVolume;
        if (shouldCarbonCopy) {
            backingVolume = new ObjectArrayMutableBlockEntityBuffer(min, size);
        } else {
            backingVolume = null;
        }

        return VolumeStreamUtils.<R, BlockEntity, TileEntity, IChunk, BlockPos>generateStream(
            min,
            max,
            options,
            // Ref
            (R) this,
            // IdentityFunction
            shouldCarbonCopy ? (pos, tile) -> {
                final CompoundNBT nbt = tile.save(new CompoundNBT());
                final @Nullable TileEntity cloned = tile.getType().create();
                final net.minecraft.block.BlockState state = tile.getBlockState();
                Objects.requireNonNull(
                    cloned,
                    () -> String.format("TileEntityType[%s] creates a null TileEntity!", TileEntityType.getKey(tile.getType()))
                ).load(state, nbt);
                if (this instanceof World) {
                    ((TileEntityAccessor) cloned).accessor$level((World) this);
                }
                backingVolume.addBlockEntity(pos.getX(), pos.getY(), pos.getZ(), (BlockEntity) cloned);
            } : (pos, tile) -> {},
            // ChunkAccessor
            VolumeStreamUtils.getChunkAccessorByStatus((IWorldReader) (Object) this, options.loadingStyle().generateArea()),
            // TileEntity by block pos
            (key, tileEntity) -> key,
            // TileEntity Accessor
            (chunk) -> chunk instanceof Chunk ? ((Chunk) chunk).getBlockEntities().entrySet().stream() : Stream.empty(),
            // Filtered Position TileEntity Accessor
            (blockPos, world) -> {
                final @Nullable TileEntity tileEntity = shouldCarbonCopy
                    ? backingVolume.getTileEntity(blockPos)
                    : ((IWorldReader) world).getBlockEntity(blockPos);
                return new Tuple<>(blockPos, tileEntity);
            }
        );
    }

    @Override
    default VolumeStream<R, Entity> getEntityStream(final Vector3i min, final Vector3i max, final StreamOptions options) {
        VolumeStreamUtils.validateStreamArgs(min, max, options);

        final boolean shouldCarbonCopy = options.carbonCopy();
        final Vector3i size = max.sub(min).add(1, 1 ,1);
        final @MonotonicNonNull ObjectArrayMutableEntityBuffer backingVolume;
        if (shouldCarbonCopy) {
            backingVolume = new ObjectArrayMutableEntityBuffer(min, size);
        } else {
            backingVolume = null;
        }
        return VolumeStreamUtils.<R, Entity, net.minecraft.entity.Entity, IChunk, UUID>generateStream(
            min,
            max,
            options,
            // Ref
            (R) this,
            // IdentityFunction
            shouldCarbonCopy ? (pos, entity) -> {
                final CompoundNBT nbt = new CompoundNBT();
                entity.save(nbt);
                final net.minecraft.entity.Entity cloned = entity.getType().create((World) (IWorldReader) (Object) this);
                Objects.requireNonNull(
                    cloned,
                    () -> String.format("EntityType[%s] creates a null Entity!", EntityType.getKey(entity.getType()))
                ).load(nbt);
                backingVolume.spawnEntity((Entity) cloned);
            } : (pos, tile) -> {},
            // ChunkAccessor
            VolumeStreamUtils.getChunkAccessorByStatus((IWorldReader) (Object) this, options.loadingStyle().generateArea()),
            // Entity -> UniqueID
            (key, entity) -> entity.getUUID(),
            // Entity Accessor
            (chunk) -> chunk instanceof Chunk ? Stream.empty() : Arrays.stream(((Chunk) chunk).getEntitySections())
                    .flatMap(Collection::stream)
                    .map(entity -> new AbstractMap.SimpleEntry<>(entity.blockPosition(), entity))
            ,
            // Filtered Position Entity Accessor
            (entityUuid, world) -> {
                final net.minecraft.entity.Entity tileEntity = shouldCarbonCopy
                    ? (net.minecraft.entity.Entity) backingVolume.getEntity(entityUuid).orElse(null)
                    : (net.minecraft.entity.Entity) ((ProtoWorld) world).getEntity(entityUuid).orElse(null);
                return new Tuple<>(tileEntity.blockPosition(), tileEntity);
            }
        );
    }
}
