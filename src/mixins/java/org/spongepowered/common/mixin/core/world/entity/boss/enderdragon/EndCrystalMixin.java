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
package org.spongepowered.common.mixin.core.world.entity.boss.enderdragon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.bridge.explosives.ExplosiveBridge;
import org.spongepowered.common.bridge.world.entity.boss.enderdragon.EndCrystalBridge;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageTracker;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.mixin.core.world.entity.EntityMixin;
import org.spongepowered.common.util.Constants;

import java.util.Optional;

@Mixin(EndCrystal.class)
public abstract class EndCrystalMixin extends EntityMixin implements ExplosiveBridge, EndCrystalBridge {

    private float impl$explosionRadius = Constants.Entity.EnderCrystal.DEFAULT_EXPLOSION_STRENGTH;

    // Explosive Impl

    @Override
    public Optional<Float> bridge$getExplosionRadius() {
        return Optional.of(this.impl$explosionRadius);
    }

    @Override
    public void bridge$setExplosionRadius(final @Nullable Float radius) {
        this.impl$explosionRadius = radius == null ? Constants.Entity.EnderCrystal.DEFAULT_EXPLOSION_STRENGTH : radius;
    }

    @Redirect(method = "hurtServer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"
        )
    )
    private void impl$onHurtExplode(
        final ServerLevel instance, final Entity entity, final DamageSource damageSource,
        final ExplosionDamageCalculator explosionDamageCalculator, final double xIn, double yIn, double zIn,
        float v, boolean b, Level.ExplosionInteraction explosionInteraction, final ServerLevel level,
        final DamageSource source, final float amount) {
        this.bridge$wrappedExplode(xIn, yIn, zIn, damageSource, source);
    }

    @Override
    public void bridge$wrappedExplode(final double x, final double y, final double z,
        @Nullable final DamageSource source, final DamageSource causeSource) {
        final CauseStackManager causeStackManager = PhaseTracker.getCauseStackManager();
        try (final CauseStackManager.StackFrame frame = causeStackManager.pushCauseFrame()) {
            frame.pushCause(this);
            if (source != null) {
                frame.pushCause(causeSource);
            }
            this.level().explode( (Entity) (Object) this, source, null, x, y, z, this.impl$explosionRadius, false, Level.ExplosionInteraction.BLOCK);
        }
    }

    @Inject(method = "hurtServer", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void attack$onHurt(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfoReturnable<Boolean> cir) {
        if (SpongeDamageTracker.callDamageEvents((org.spongepowered.api.entity.Entity) this, source, damage) == null) {
            cir.setReturnValue(true);
        }
    }

}
