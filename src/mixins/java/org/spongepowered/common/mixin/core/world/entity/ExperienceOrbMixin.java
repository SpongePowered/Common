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
package org.spongepowered.common.mixin.core.world.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageTracker;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin extends EntityMixin {

    @Inject(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;discard()V")
    )
    private void impl$throwExpireEvent(final CallbackInfo ci) {
        this.impl$callExpireEntityEvent();
    }

    @Inject(method = "hurtServer", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ExperienceOrb;markHurt()V"))
    private void attack$onHurt(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfoReturnable<Boolean> cir) {
        if (SpongeDamageTracker.callDamageEvents((Entity) this, source, damage) == null) {
            cir.setReturnValue(true);
        }
    }
}
