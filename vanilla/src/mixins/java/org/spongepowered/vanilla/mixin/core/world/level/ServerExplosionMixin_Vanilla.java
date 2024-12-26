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
package org.spongepowered.vanilla.mixin.core.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.util.VecHelper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin_Vanilla {


    // @formatter:off
    @Shadow @Final private ServerLevel level;

    @Shadow protected abstract boolean shadow$interactsWithBlocks();
    // @formatter:on

    /**
     * These two fields are used in the mixin below, but require a redirect specific for SpongeVanilla
     * and SpongeForge as NeoForge makes a change to the method being used here.
     *
     * @see org.spongepowered.common.mixin.core.world.level.ServerExplosionMixin
     */
    private boolean impl$shouldDamageEntities;
    private double impl$knockbackMultiplier;

    private List<BlockPos> impl$affectedBlocks;

    @Redirect(method = "hurtEntities", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> vanilla$throwEventToFilterEntitiesOnHurt(final ServerLevel instance, final Entity sourceEntity, final AABB aabb) {
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
    private Vec3 vanilla$useKnockbackMultiplier(final double $$0, final double $$1, final double $$2) {
        // Honor our knockback value from event
        return new Vec3($$0 * this.impl$knockbackMultiplier,
            $$1 * this.impl$knockbackMultiplier,
            $$2 * this.impl$knockbackMultiplier);
    }

}
