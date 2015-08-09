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
package org.spongepowered.common.mixin.core.data.holders;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockButton;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutablePoweredData;
import org.spongepowered.api.data.manipulator.mutable.block.PoweredData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.interfaces.block.IMixinPoweredHolder;
import org.spongepowered.common.mixin.core.block.MixinBlock;

@Mixin(BlockButton.class)
public abstract class MixinBlockButton extends MixinBlock implements IMixinPoweredHolder {

    @Override
    public ImmutablePoweredData getPoweredData(IBlockState blockState) {
        final boolean powered = (Boolean) blockState.getValue(BlockButton.POWERED);
        return null;
    }

    @Override
    public DataTransactionResult setPoweredData(PoweredData poweredData, World world, BlockPos blockPos) {
        final ImmutablePoweredData data = getPoweredData(world.getBlockState(blockPos));
        return null;
    }

    @Override
    public BlockState resetPoweredData(BlockState blockState) {
        return (BlockState) ((IBlockState) blockState).withProperty(BlockButton.POWERED, false);
    }

    @Override
    public ImmutableList<ImmutableDataManipulator<?, ?>> getManipulators(World world, BlockPos blockPos) {
        return super.getManipulators(world, blockPos);
    }
}
