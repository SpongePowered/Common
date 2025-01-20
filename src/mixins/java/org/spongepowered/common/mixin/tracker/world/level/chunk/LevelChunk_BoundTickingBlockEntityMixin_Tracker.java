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
package org.spongepowered.common.mixin.tracker.world.level.chunk;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.event.tracking.TrackingUtil;

@Mixin(targets = "net/minecraft/world/level/chunk/LevelChunk$BoundTickingBlockEntity")
public class LevelChunk_BoundTickingBlockEntityMixin_Tracker<T extends BlockEntity> {

    @WrapOperation(
        method = "tick()V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V")
    )
    private void tracker$wrapBlockEntityTickWithPhase(
        final BlockEntityTicker<T> instance, final Level level, final BlockPos blockPos, final BlockState blockState,
        final T blockEntity, final Operation<Void> original
    ) {
        if (level instanceof LevelBridge tb && tb.bridge$isFake()) {
            original.call(instance, level, blockPos, blockState, blockEntity);
            return;
        }
        if (!((org.spongepowered.api.block.entity.BlockEntity) blockEntity).isTicking()) {
            return;
        }

        TrackingUtil.tickTileEntity(blockEntity, () -> original.call(instance, level, blockPos, blockState, blockEntity));

    }
}
