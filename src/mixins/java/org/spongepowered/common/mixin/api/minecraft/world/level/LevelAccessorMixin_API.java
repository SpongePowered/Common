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
package org.spongepowered.common.mixin.api.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.LevelData;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.WorldLike;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.schematic.Palette;
import org.spongepowered.api.world.schematic.PaletteTypes;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Interface.Remap;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.plugin.PluginPhase;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.world.SpongeBlockChangeFlag;
import org.spongepowered.common.world.schematic.PaletteWrapper;
import org.spongepowered.common.world.volume.VolumeStreamUtils;
import org.spongepowered.math.vector.Vector3i;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Mixin(LevelAccessor.class)
@Implements(@Interface(iface = WorldLike.class, prefix = "worldLike$", remap = Remap.NONE))
public interface LevelAccessorMixin_API<P extends WorldLike<P>> extends WorldLike<P> {

    //@formatter:off
    @Shadow boolean shadow$hasChunk(int p_217354_1_, int p_217354_2_);
    @Shadow net.minecraft.util.RandomSource shadow$getRandom();
    @Shadow LevelData shadow$getLevelData();
    //@formatter:on

    // BlockVolume

    @SuppressWarnings("unchecked")
    @Override
    default Palette<org.spongepowered.api.block.BlockState, BlockType> blockPalette() {
        return PaletteWrapper.of(
            PaletteTypes.BLOCK_STATE_PALETTE.get(),
            Block.BLOCK_STATE_REGISTRY,
            (org.spongepowered.api.registry.Registry<BlockType>) ((LevelAccessor) (Object) this).registryAccess().lookupOrThrow(Registries.BLOCK)
        );
    }

    // MutableBiomeVolume

    @Override
    @SuppressWarnings({"ConstantConditions"})
    default boolean setBiome(final int x, final int y, final int z, final org.spongepowered.api.world.biome.Biome biome) {
        Objects.requireNonNull(biome, "biome");

        final ChunkAccess iChunk = ((LevelReader) this).getChunk(new BlockPos(x, y, z));
        if (iChunk == null) {
            return false;
        }

        return VolumeStreamUtils.setBiomeOnNativeChunk(x, y, z, biome, () -> iChunk.getSection(iChunk.getSectionIndex(y)), iChunk::markUnsaved);
    }

    // Volume

    @Override
    default Vector3i min() {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of LevelAccessor that isn't part of Sponge API: " + this.getClass());
    }

    @Override
    default Vector3i max() {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of LevelAccessor that isn't part of Sponge API: " + this.getClass());
    }

    @Override
    default Vector3i size() {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of LevelAccessor that isn't part of Sponge API: " + this.getClass());
    }

    @Override
    default boolean contains(final int x, final int y, final int z) {
        return this.shadow$hasChunk(x >> 4, z >> 4);
    }

    @Override
    default boolean isAreaAvailable(final int x, final int y, final int z) {
        return this.shadow$hasChunk(x >> 4, z >> 4);
    }

    // EntityVolume

    @Override
    default Optional<Entity> entity(final UUID uuid) {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of LevelAccessor that isn't part of Sponge API: " + this.getClass());
    }

    // RandomProvider

    @Intrinsic
    default Source worldLike$random() {
        return (Source) this.shadow$getRandom();
    }

    // WorldLike

    @Override
    default Difficulty difficulty() {
        return (Difficulty) (Object) this.shadow$getLevelData().getDifficulty();
    }

    @Override
    default Collection<Entity> spawnEntities(final Iterable<? extends Entity> entities) {
        Objects.requireNonNull(entities, "entities");
        return EntityUtil.spawnEntities(entities, x -> true, e -> e.level().addFreshEntity(e));
    }

    @Override
    default boolean spawnEntity(final Entity entity) {
        return ((LevelAccessor) this).addFreshEntity((net.minecraft.world.entity.Entity) Objects.requireNonNull(entity, "entity"));
    }

    // MutableBlockVolume

    @Override
    default boolean setBlock(final int x, final int y, final int z, final org.spongepowered.api.block.BlockState blockState,
            final BlockChangeFlag flag) {
        Objects.requireNonNull(blockState, "blockState");
        Objects.requireNonNull(flag, "flag");

        if (!((Level) this).isInWorldBounds(new BlockPos(x, y, z))) {
            throw new PositionOutOfBoundsException(new Vector3i(x, y, z), Constants.World.BLOCK_MIN, Constants.World.BLOCK_MAX);
        }
        try (final @Nullable PhaseContext<@NonNull ?> context = PluginPhase.State.BLOCK_WORKER.switchIfNecessary(PhaseTracker.getWorldInstance((LevelAccessor) this))) {
            if (context != null) {
                context.buildAndSwitch();
            }
            return ((LevelAccessor) this).setBlock(new BlockPos(x, y, z), (BlockState) blockState, ((SpongeBlockChangeFlag) flag).rawFlag());
        }
    }

}
