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
package org.spongepowered.common.mixin.core.block;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableInWallData;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableOpenData;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutablePoweredData;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.data.ImmutableDataCachingUtil;
import org.spongepowered.common.data.manipulator.immutable.block.ImmutableSpongeInWallData;
import org.spongepowered.common.data.manipulator.immutable.block.ImmutableSpongeOpenData;
import org.spongepowered.common.data.manipulator.immutable.block.ImmutableSpongePoweredData;

import java.util.Optional;

@Mixin(BlockFenceGate.class)
public abstract class MixinBlockFenceGate extends MixinBlock {

    @Override
    public ImmutableList<ImmutableDataManipulator<?, ?>> getManipulators(IBlockState blockState) {
        return ImmutableList.<ImmutableDataManipulator<?, ?>>builder()
                .addAll(super.getManipulators(blockState))
                .add(getIsOpenFor(blockState))
                .add(getIsPoweredFor(blockState))
                .add(getInWallFor(blockState))
                .build();
    }

    @Override
    public boolean supports(Class<? extends ImmutableDataManipulator<?, ?>> immutable) {
        return super.supports(immutable) || ImmutableOpenData.class.isAssignableFrom(immutable)
                || ImmutablePoweredData.class.isAssignableFrom(immutable) || ImmutableInWallData.class.isAssignableFrom(immutable);
    }

    @Override
    public Optional<BlockState> getStateWithData(IBlockState blockState, ImmutableDataManipulator<?, ?> manipulator) {
        if (manipulator instanceof ImmutableOpenData) {
            final boolean isOpen = ((ImmutableOpenData) manipulator).open().get();
            return Optional.of((BlockState) blockState.withProperty(BlockFenceGate.OPEN, isOpen));
        }
        if (manipulator instanceof ImmutablePoweredData) {
            return Optional.of((BlockState) blockState.withProperty(BlockFenceGate.POWERED, ((ImmutablePoweredData) manipulator).powered().get()));
        }
        if (manipulator instanceof ImmutableInWallData) {
            return Optional.of((BlockState) blockState);
        }
        return super.getStateWithData(blockState, manipulator);
    }

    @Override
    public <E> Optional<BlockState> getStateWithValue(IBlockState blockState, Key<? extends BaseValue<E>> key, E value) {
        if (key.equals(Keys.OPEN)) {
            final boolean isOpen = (Boolean) value;
            return Optional.of((BlockState) blockState.withProperty(BlockFenceGate.OPEN, isOpen));
        }
        if (key.equals(Keys.POWERED)) {
            return Optional.of((BlockState) blockState.withProperty(BlockFenceGate.POWERED, (Boolean) value));
        }
        if (key.equals(Keys.IN_WALL)) {
            return Optional.of((BlockState) blockState);
        }
        return super.getStateWithValue(blockState, key, value);
    }

    private ImmutableOpenData getIsOpenFor(IBlockState blockState) {
        return ImmutableDataCachingUtil.getManipulator(ImmutableSpongeOpenData.class, blockState.getValue(BlockFenceGate.OPEN));
    }

    private ImmutablePoweredData getIsPoweredFor(IBlockState blockState) {
        return ImmutableDataCachingUtil.getManipulator(ImmutableSpongePoweredData.class, blockState.getValue(BlockFenceGate.POWERED));
    }

    private ImmutableInWallData getInWallFor(IBlockState blockState) {
        return ImmutableDataCachingUtil.getManipulator(ImmutableSpongeInWallData.class, blockState.getValue(BlockFenceGate.IN_WALL));
    }
}
