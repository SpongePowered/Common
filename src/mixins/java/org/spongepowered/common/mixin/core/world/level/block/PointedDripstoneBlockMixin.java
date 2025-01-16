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
package org.spongepowered.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.mixin.core.block.BlockMixin;

@Mixin(PointedDripstoneBlock.class)
public abstract class PointedDripstoneBlockMixin extends BlockMixin {

    @Redirect(method = "fallOn",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;stalagmite()Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource impl$spongeRedirectForFireDamage(
        final DamageSources instance, final Level level, final BlockState state,
        final BlockPos pos,final  Entity ignored, final double distance
    ) {
        final DamageSource source = instance.stalagmite();
        if (level.isClientSide) { // Short Circuit
            return source;
        }
        final ServerLocation location = ServerLocation.of((ServerWorld) level, pos.getX(), pos.getY(), pos.getZ());
        var blockSource = org.spongepowered.api.event.cause.entity.damage.source.DamageSource.builder()
            .from((org.spongepowered.api.event.cause.entity.damage.source.DamageSource) source).block(location)
            .block(location.createSnapshot()).build();
        return (DamageSource) blockSource;
    }

}
