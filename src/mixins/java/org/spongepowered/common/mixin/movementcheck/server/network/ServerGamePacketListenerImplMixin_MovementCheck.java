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
package org.spongepowered.common.mixin.movementcheck.server.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.common.config.SpongeGameConfigs;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin_MovementCheck {

    @Shadow public ServerPlayer player;

    @ModifyConstant(method = "handleMovePlayer", constant = @Constant(floatValue = 100.0F, ordinal = 0), slice =
        @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isFallFlying()Z", ordinal = 0),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getBoundingBox()Lnet/minecraft/world/phys/AABB;", ordinal = 0)
        )
    )
    private float movementCheck$onMovedTooQuickly(final float value) {
        final double threshold = SpongeGameConfigs.getForWorld(this.player.level()).get().movementChecks.movedTooQuicklyThreshold;
        if (threshold > 0.0D && SpongeGameConfigs.getForWorld(this.player.level()).get().movementChecks.player.movedTooQuickly) {
            return (float) threshold;
        }
        return Float.NaN;
    }

    @ModifyConstant(method = "handleMovePlayer", constant = @Constant(doubleValue = 0.0625D, ordinal = 0), slice =
        @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isChangingDimension()Z", ordinal = 0),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSleeping()Z", ordinal = 1)
        )
    )
    private double movementCheck$onMovedWronglyCheck(final double value) {
        final double threshold = SpongeGameConfigs.getForWorld(this.player.level()).get().movementChecks.movedWronglyThreshold;
        if (threshold > 0.0D && SpongeGameConfigs.getForWorld(this.player.level()).get().movementChecks.movedWrongly) {
            return threshold;
        }
        return Double.NaN;
    }

    @ModifyConstant(method = "handleMoveVehicle", constant = @Constant(doubleValue = 0.0625D, ordinal = 0), slice =
        @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", ordinal = 0),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;absSnapTo(DDDFF)V")
        )
    )
    private double movementCheck$onVehicleMovedWronglyCheck(final double value) {
        final double threshold = SpongeGameConfigs.getForWorld(this.player.level()).get().movementChecks.vehicleMovedWronglyThreshold;
        if (threshold > 0.0D && SpongeGameConfigs.getForWorld(this.player.level()).get().movementChecks.movedWrongly) {
            return threshold;
        }
        return Double.NaN;
    }

    @ModifyConstant(method = "handleMoveVehicle", constant = @Constant(doubleValue = 100D, ordinal = 0), slice =
        @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;lengthSqr()D", ordinal = 0),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;isSingleplayerOwner()Z", ordinal = 0)
        )
    )
    private double movementCheck$onVehicleMovedTooQuicklyCheck(final double value) {
        final double threshold = SpongeGameConfigs.getForWorld(this.player.level()).get().movementChecks.vehicleMovedTooQuicklyThreshold;
        if (threshold > 0.0D && SpongeGameConfigs.getForWorld(this.player.level()).get().movementChecks.player.vehicleMovedTooQuickly) {
            return threshold;
        }
        return Double.NaN;
    }
}
