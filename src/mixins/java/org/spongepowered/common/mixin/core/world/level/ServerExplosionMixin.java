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
package org.spongepowered.common.mixin.core.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.world.level.ExplosionBridge;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.util.VecHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Mixin(net.minecraft.world.level.ServerExplosion.class)
public abstract class ServerExplosionMixin implements ExplosionBridge {

    // @formatter:off
    @Shadow @Final private Map<Player, Vec3> hitPlayers;
    @Shadow @Final private boolean fire;
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private Entity source;
    @Shadow @Final private float radius;
    @Shadow @Final private Vec3 center;
    @Shadow @Final private net.minecraft.world.level.Explosion.BlockInteraction blockInteraction;

    @Shadow protected abstract void shadow$interactWithBlocks(final List<BlockPos> $$0);
    @Shadow protected abstract List<BlockPos> shadow$calculateExplodedPositions();
    @Shadow protected abstract boolean shadow$interactsWithBlocks();

    @Shadow protected abstract void shadow$createFire(final List<BlockPos> $$0);

    // @formatter:on


    private boolean impl$shouldDamageEntities;
    private boolean impl$shouldPlaySmoke;
    private int impl$resolution;
    private float impl$randomness;
    private double impl$knockbackMultiplier;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void impl$onConstructed(final ServerLevel $$0, final Entity $$1, final DamageSource $$2,
        final ExplosionDamageCalculator $$3, final Vec3 $$4, final float $$5, final boolean $$6,
        final net.minecraft.world.level.Explosion.BlockInteraction $$7, final CallbackInfo ci) {
        this.impl$shouldDamageEntities = true;
        this.impl$resolution = 16;
        this.impl$randomness = 1.0F;
        this.impl$knockbackMultiplier = 1.0;
    }

    private List<BlockPos> impl$affectedBlocks;

    @Redirect(method = "explode", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/ServerExplosion;calculateExplodedPositions()Ljava/util/List;"))
    private List<BlockPos> impl$onCalculateExplodedPositions(final ServerExplosion instance) {
        // If we don't fire the event and do not affect any blocks we can ignore the calculation
        if (!this.fire && !this.shadow$interactsWithBlocks() && !ShouldFire.EXPLOSION_EVENT_DETONATE) {
            return List.of();
        }

        this.impl$affectedBlocks = this.shadow$calculateExplodedPositions();
        return this.impl$affectedBlocks;
    }


    @Redirect(method = "hurtEntities", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> impl$onGetHurtEntities(final ServerLevel instance, final Entity sourceEntity, final AABB aabb) {
        final List<Entity> entities;
        if (this.impl$shouldDamageEntities) {
            // filter out invulnerable entities before event
            entities = instance.getEntities(sourceEntity, aabb).stream()
                .filter(e -> !e.ignoreExplosion((net.minecraft.world.level.Explosion) this))
                .toList();
        } else {
            entities = Collections.emptyList();
        }

        if (ShouldFire.EXPLOSION_EVENT_DETONATE) {
            final var apiWorld = (ServerWorld) this.level;
            final var apiEntities = entities.stream().map(org.spongepowered.api.entity.Entity.class::cast).toList();
            final var apiBlockPositions = this.impl$affectedBlocks.stream().map(bp -> ServerLocation.of(apiWorld, VecHelper.toVector3i(bp))).toList();
            final Cause cause = PhaseTracker.getCauseStackManager().currentCause();
            final ExplosionEvent.Detonate event = SpongeEventFactory.createExplosionEventDetonate(cause, apiBlockPositions, apiEntities, (Explosion) this, apiWorld);
            if (SpongeCommon.post(event)) {
                this.impl$affectedBlocks.clear(); // no blocks affected
                return Collections.emptyList(); // no entities affected
            }
            if (this.shadow$interactsWithBlocks()) {
                this.impl$affectedBlocks = event.affectedLocations().stream().map(VecHelper::toBlockPos).collect(Collectors.toList());
            }
            if (this.impl$shouldDamageEntities) {
                return event.entities().stream().map(Entity.class::cast).toList();
            }
        }
        return entities;
    }

    @Redirect(method = "hurtEntities", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 impl$onAddKnockback(final double $$0, final double $$1, final double $$2) {
        // Honor our knockback value from event
        return new Vec3($$0 * this.impl$knockbackMultiplier,
                        $$1 * this.impl$knockbackMultiplier,
                        $$2 * this.impl$knockbackMultiplier);
    }

    @Redirect(method = "explode", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/ServerExplosion;interactWithBlocks(Ljava/util/List;)V"))
    private void impl$onInteractWithBlocks(final ServerExplosion instance, final List<BlockPos> $$0) {
        this.shadow$interactWithBlocks(this.impl$affectedBlocks);
    }

    @Redirect(method = "explode", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/ServerExplosion;createFire(Ljava/util/List;)V"))
    private void impl$onCreateFire(final ServerExplosion instance, final List<BlockPos> $$0) {
        this.shadow$createFire(this.impl$affectedBlocks);
    }

    @Override
    public boolean bridge$getShouldDamageEntities() {
        return this.impl$shouldDamageEntities;
    }

    @Override
    public void bridge$setShouldDamageEntities(final boolean shouldDamageEntities) {
        this.impl$shouldDamageEntities = shouldDamageEntities;
    }

    @Override
    public void bridge$setResolution(final int resolution) {
        this.impl$resolution = resolution;
    }

    @Override
    public int bridge$getResolution() {
        return this.impl$resolution;
    }

    @Override
    public void bridge$setShouldPlaySmoke(final boolean shouldPlaySmoke) {
        this.impl$shouldPlaySmoke = shouldPlaySmoke;
    }

    @Override
    public boolean bridge$getShouldPlaySmoke() {
        return this.impl$shouldPlaySmoke;
    }

    @Override
    public void bridge$setRandomness(final float randomness) {
        this.impl$randomness = randomness;
    }

    @Override
    public float bridge$getRandomness() {
        return this.impl$randomness;
    }

    @Override
    public void bridge$setKnockback(final double knockback) {
        this.impl$knockbackMultiplier = knockback;
    }

    @Override
    public double bridge$getKnockback() {
        return this.impl$knockbackMultiplier;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ServerExplosionMixin.class.getSimpleName() + "[", "]")
            .add("causesFire=" + this.fire)
            .add("blockInteraction=" + this.blockInteraction)
            .add("world=" + this.level)
            .add("x=" + this.center.x)
            .add("y=" + this.center.y)
            .add("z=" + this.center.z)
            .add("exploder=" + this.source)
            .add("size=" + this.radius)
            .add("playerKnockbackMap=" + this.hitPlayers)
            .add("shouldDamageEntities=" + this.impl$shouldDamageEntities)
            .add("resolution=" + this.impl$resolution)
            .add("randomness=" + this.impl$randomness)
            .add("knockback=" + this.impl$knockbackMultiplier)
            .toString();
    }
}
