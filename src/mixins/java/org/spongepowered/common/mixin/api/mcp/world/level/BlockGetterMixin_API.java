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
package org.spongepowered.common.mixin.api.mcp.world.level;

import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.fluid.FluidState;
import org.spongepowered.api.world.volume.game.PrimitiveGameVolume;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.math.vector.Vector3i;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Mixin(BlockGetter.class)
@Implements(@Interface(iface = PrimitiveGameVolume.class, prefix = "primitive$"))
public interface BlockGetterMixin_API extends PrimitiveGameVolume {

    //@formatter:off
    @Shadow @Nullable net.minecraft.world.level.block.entity.BlockEntity shadow$getBlockEntity(BlockPos p_175625_1_);
    @Shadow BlockState shadow$getBlockState(BlockPos p_180495_1_);
    @Shadow net.minecraft.world.level.material.FluidState shadow$getFluidState(BlockPos p_204610_1_);
    @Shadow int shadow$getLightEmission(BlockPos p_217298_1_);
    @Shadow int shadow$getMaxLightLevel();
    @Shadow int shadow$getMaxBuildHeight();
    //@formatter:on

    @Override
    default int getMaximumLight() {
        return this.shadow$getMaxLightLevel();
    }

    @Override
    default int getEmittedLight(final Vector3i position) {
        return this.shadow$getLightEmission(new BlockPos(position.getX(), position.getY(), position.getZ()));
    }

    @Override
    default int getEmittedLight(final int x, final int y, final int z) {
        return this.shadow$getLightEmission(new BlockPos(x, y, z));
    }

    @Intrinsic
    default int primitive$getHeight() {
        return this.shadow$getMaxBuildHeight();
    }

    @Override
    default Collection<? extends BlockEntity> getBlockEntities() {
        return Collections.emptyList();
    }

    @Override
    default Optional<? extends BlockEntity> getBlockEntity(final int x, final int y, final int z) {
        return Optional.ofNullable((BlockEntity) this.shadow$getBlockEntity(new BlockPos(x, y, z)));
    }

    @Override
    default org.spongepowered.api.block.BlockState getBlock(final int x, final int y, final int z) {
        return (org.spongepowered.api.block.BlockState) this.shadow$getBlockState(new BlockPos(x, y, z));
    }

    @Override
    default FluidState getFluid(final int x, final int y, final int z) {
        return (FluidState) (Object) this.shadow$getFluidState(new BlockPos(x, y, z));
    }

    @Override
    default int getHighestYAt(final int x, final int z) {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of IBlockReader that isn't part of Sponge API");
    }

    @Override
    default Vector3i getBlockMin() {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of IBlockReader that isn't part of Sponge API");
    }

    @Override
    default Vector3i getBlockMax() {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of IBlockReader that isn't part of Sponge API");
    }

    @Override
    default Vector3i getBlockSize() {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of IBlockReader that isn't part of Sponge API");
    }

    @Override
    default boolean containsBlock(final int x, final int y, final int z) {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of IBlockReader that isn't part of Sponge API");
    }

    @Override
    default boolean isAreaAvailable(final int x, final int y, final int z) {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of IBlockReader that isn't part of Sponge API");
    }
}
