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
package org.spongepowered.common.mixin.core.entity.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.FireworkRocketEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.projectile.explosive.FireworkRocket;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.projectile.source.ProjectileSource;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.explosives.ExplosiveBridge;
import org.spongepowered.common.bridge.explosives.FusedExplosiveBridge;
import org.spongepowered.common.bridge.util.DamageSourceBridge;
import org.spongepowered.common.bridge.world.WorldBridge;
import org.spongepowered.common.entity.projectile.ProjectileSourceSerializer;
import org.spongepowered.common.entity.projectile.UnknownProjectileSource;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.mixin.core.entity.EntityMixin;
import org.spongepowered.common.util.Constants;

import java.util.Optional;

import javax.annotation.Nullable;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin extends EntityMixin implements FusedExplosiveBridge, ExplosiveBridge {

    @Shadow private int fireworkAge;
    @Shadow private int lifetime;

    @Shadow protected abstract void func_213893_k();

    private ProjectileSource impl$projectileSource = UnknownProjectileSource.UNKNOWN;
    private int impl$explosionRadius = Constants.Entity.Firework.DEFAULT_EXPLOSION_RADIUS;


    @Override
    public void impl$readFromSpongeCompound(final CompoundNBT compound) {
        super.impl$readFromSpongeCompound(compound);
        ProjectileSourceSerializer.readSourceFromNbt(compound, (FireworkRocket) this);
    }

    @Override
    public void impl$writeToSpongeCompound(final CompoundNBT compound) {
        super.impl$writeToSpongeCompound(compound);
        ProjectileSourceSerializer.writeSourceToNbt(compound, this.impl$projectileSource, (Entity) null);
    }

    @Override
    public int bridge$getFuseDuration() {
        return this.lifetime;
    }

    @Override
    public void bridge$setFuseDuration(final int fuseTicks) {
        this.lifetime = fuseTicks;
    }

    @Override
    public int bridge$getFuseTicksRemaining() {
        return this.lifetime - this.fireworkAge;
    }

    @Override
    public void bridge$setFuseTicksRemaining(final int fuseTicks) {
        this.fireworkAge = 0;
        this.lifetime = fuseTicks;
    }

    @Override
    public Optional<Integer> bridge$getExplosionRadius() {
        return Optional.of(this.impl$explosionRadius);
    }

    @Override
    public void bridge$setExplosionRadius(final @Nullable Integer radius) {
        this.impl$explosionRadius = radius == null ? Constants.Entity.Firework.DEFAULT_EXPLOSION_RADIUS : radius;
    }

    @Redirect(method = "func_213893_k()V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setEntityState(Lnet/minecraft/entity/Entity;B)V")
    )
    private void impl$useSpongeExplosion(final World world, final Entity self, final byte state) {
        if (this.world.isRemote) {
            world.setEntityState(self, state);
            return;
        }
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            // Fireworks don't typically explode like other explosives, but we'll
            // post an event regardless and if the radius is zero the explosion
            // won't be triggered (the default behavior).
            frame.pushCause(this);
            frame.addContext(EventContextKeys.PROJECTILE_SOURCE, this.impl$projectileSource);
            SpongeCommonEventFactory.detonateExplosive(this, Explosion.builder()
                .sourceExplosive(((FireworkRocket) this))
                .location(((FireworkRocket) this).getServerLocation())
                .radius(this.impl$explosionRadius))
                .ifPresent(explosion -> world.setEntityState(self, state));
        }
    }

    @Inject(method = "tick()V", at = @At("RETURN"))
    private void impl$postPrimeEvent(final CallbackInfo ci) {
        if (this.fireworkAge == 1 && !this.world.isRemote) {
            try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
                frame.pushCause(this);
                frame.addContext(EventContextKeys.PROJECTILE_SOURCE, this.impl$projectileSource);
                this.bridge$postPrime();
            }
        }
    }

    @Redirect(method = "dealExplosionDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;attackEntityFrom"
            + "(Lnet/minecraft/util/DamageSource;F)Z"))
    private boolean impl$useEntitySource(final LivingEntity entityLivingBase, final DamageSource source, final float amount) {
        try {
            final DamageSource fireworks = new EntityDamageSource(DamageSource.FIREWORKS.damageType, (Entity) (Object) this).setExplosion();
            ((DamageSourceBridge) fireworks).bridge$setFireworksSource();
            return entityLivingBase.attackEntityFrom(DamageSource.FIREWORKS, amount);
        } finally {
            ((DamageSourceBridge) source).bridge$setFireworksSource();
        }
    }

    @Inject(method = "func_213892_a", at = @At("HEAD"), cancellable = true)
    private void impl$onImpact(final RayTraceResult rayTraceResult, final CallbackInfo ci) {
        if (((WorldBridge) this.world).bridge$isFake() || rayTraceResult.getType() == RayTraceResult.Type.MISS) {
            return;
        }

        if (SpongeCommonEventFactory.handleCollideImpactEvent((Entity) (Object)this,
                ((FireworkRocket) this).get(Keys.SHOOTER).orElse(null), rayTraceResult)) {
            this.shadow$remove();
            ci.cancel();
        }
    }
}
