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
package org.spongepowered.common.mixin.core.world.entity.vehicle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.bridge.explosives.ExplosiveBridge;
import org.spongepowered.common.bridge.explosives.FusedExplosiveBridge;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageTracker;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.util.Constants;

import java.util.Optional;

@Mixin(MinecartTNT.class)
public abstract class MinecartTNTMixin extends AbstractMinecartMixin implements FusedExplosiveBridge, ExplosiveBridge {

    // @formatter:off
    @Shadow private int fuse;
    @Shadow public abstract boolean shadow$isPrimed();
    @Shadow public abstract void shadow$primeFuse();
    // @formatter:on

    @Nullable private Float impl$explosionRadius = null; // override for fixed radius
    private int impl$fuseDuration = Constants.Entity.Minecart.DEFAULT_FUSE_DURATION;
    private boolean impl$detonationCancelled;
    @Nullable private Object impl$primeCause;

    @Override
    public Optional<Float> bridge$getExplosionRadius() {
        return Optional.ofNullable(this.impl$explosionRadius);
    }

    @Override
    public void bridge$setExplosionRadius(final @Nullable Float radius) {
        this.impl$explosionRadius = radius;
    }

    @Override
    public boolean bridge$isPrimed() {
        return this.shadow$isPrimed();
    }

    @Override
    public int bridge$getFuseDuration() {
        return this.impl$fuseDuration;
    }

    @Override
    public void bridge$setFuseDuration(final int fuseTicks) {
        if (this.shadow$isPrimed()) {
            this.fuse = Math.max(this.fuse + fuseTicks - this.impl$fuseDuration, 0);
        }
        this.impl$fuseDuration = fuseTicks;
    }

    @Override
    public int bridge$getFuseTicksRemaining() {
        return this.fuse;
    }

    @Override
    public void bridge$setFuseTicksRemaining(final int fuseTicks) {
        this.fuse = fuseTicks;
    }


    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void impl$onAttackSetPrimeCause(final ServerLevel level, final DamageSource damageSource,
                                            final float amount, final CallbackInfoReturnable<Boolean> ci) {
        this.impl$primeCause = damageSource;
    }

    @Inject(method = "activateMinecart(IIIZ)V", at = @At("HEAD"))
    private void impl$onActivateSetPrimeCauseNotifier(final int x, final int y, final int z, final boolean receivingPower, final CallbackInfo ci) {
        if (((LevelBridge) this.shadow$level()).bridge$isFake()) {
            return;
        }
        if (receivingPower) {
            ((ServerWorld) this.shadow$level()).get(x, y, z, Keys.NOTIFIER).ifPresent(notifier -> this.impl$primeCause = notifier);
        }
    }

    @Inject(method = "primeFuse", at = @At("HEAD"), cancellable = true)
    private void impl$preIgnite(final CallbackInfo ci) {
        if (!this.bridge$shouldPrime()) {
            this.bridge$setFuseTicksRemaining(-1);
            ci.cancel();
        }
    }

    @Inject(method = "primeFuse", at = @At("RETURN"))
    private void impl$postIgnite(final CallbackInfo ci) {
        this.bridge$setFuseTicksRemaining(this.impl$fuseDuration);
        if (this.impl$primeCause != null) {
            PhaseTracker.getCauseStackManager().pushCause(this.impl$primeCause);
        }
        this.bridge$postPrime();
        if (this.impl$primeCause != null) {
            PhaseTracker.getCauseStackManager().popCause();
        }
    }

    @Override
    public void bridge$cancelExplosion() {
        this.impl$detonationCancelled = true;
    }

    @Inject(method = "explode(Lnet/minecraft/world/damagesource/DamageSource;D)V", at = @At("RETURN"))
    private void impL$postExplode(final CallbackInfo ci) {
        if (this.impl$detonationCancelled) {
            this.impl$detonationCancelled = false;
            this.shadow$unsetRemoved();
        }
    }

    @Inject(method = "hurtServer", cancellable = true, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/vehicle/MinecartTNT;explode(Lnet/minecraft/world/damagesource/DamageSource;D)V"))
    private void attackImpl$postOnAttackEntityFrom(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfoReturnable<Boolean> cir) {
        if (SpongeDamageTracker.callDamageEvents((org.spongepowered.api.entity.Entity) this, source, damage) == null) {
            cir.setReturnValue(true);
        }
    }

}
