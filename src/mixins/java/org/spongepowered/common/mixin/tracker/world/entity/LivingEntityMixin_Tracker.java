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
package org.spongepowered.common.mixin.tracker.world.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.accessor.world.damagesource.CombatTrackerAccessor;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.phase.entity.EntityPhase;
import org.spongepowered.common.event.tracking.phase.tick.EntityTickContext;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_Tracker extends EntityMixin_Tracker {

    // @formatter:off

    @Shadow protected abstract void shadow$tickDeath();
    @Shadow public abstract CombatTracker shadow$getCombatTracker();
    @Shadow public void shadow$die(final DamageSource cause) {}
    @Shadow protected abstract void shadow$pushEntities();
    @Shadow public abstract float shadow$getHealth();

    @Shadow public int deathTime;
    @Shadow protected boolean dead;

    // @formatter:on

    @Override
    protected void tracker$populateDeathContextIfNeeded(
        final CauseStackManager.StackFrame frame, final EntityTickContext context
    ) {
        if (!(this.shadow$getHealth() <= 0) && this.deathTime<=0 && !this.dead) {
            return;
        }
        final List<CombatEntry> entries = ((CombatTrackerAccessor) this.shadow$getCombatTracker()).accessor$entries();
        if (!entries.isEmpty()) {
            final CombatEntry entry = entries.get(entries.size() - 1);
            final DamageSource source = entry.source();
            if (source != null) {
                frame.addContext(
                    EventContextKeys.LAST_DAMAGE_SOURCE,
                    (org.spongepowered.api.event.cause.entity.damage.source.DamageSource) source
                );
            }
        }
    }

    /**
     * @author i509VCB
     * @author gabizou
     *
     * @reason We can enter in to the entity drops transaction here which will
     * successfully batch the side effects (this is effectively a singular side
     * effect/transaction instead of a pipeline) to perform some things around
     * the entity's death. This successfully records the transactions associated
     * with this entity entering into the death state.
     */
    @Redirect(method = "baseTick()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;tickDeath()V"))
    private void tracker$enterDeathPhase(final LivingEntity livingEntity) {
        if (((LevelBridge) this.shadow$level()).bridge$isFake()) {
            this.shadow$tickDeath();
            return;
        }
        final PhaseTracker instance = PhaseTracker.getWorldInstance((ServerLevel) livingEntity.level());
        if (!instance.onSidedThread()) {
            this.shadow$tickDeath();
            return;
        }
        final PhaseContext<@NonNull ?> context = instance.getPhaseContext();
        if (!context.doesBlockEventTracking()) {
            this.shadow$tickDeath();
            return;
        }
        try (final EffectTransactor ignored = context.getTransactor().ensureEntityDropTransactionEffect((LivingEntity) (Object) this)) {
            this.shadow$tickDeath();
        }
    }

    /**
     * @author gabizou
     * @reason Instead of inlining the onDeath method with the main mixin, we can "toggle"
     * the usage of the death state control in the tracker mixin.
     */
    @Redirect(method = "hurtServer",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;die(Lnet/minecraft/world/damagesource/DamageSource;)V"
        )
    )
    private void tracker$wrapOnDeathWithState(final LivingEntity thisEntity, final DamageSource cause) {
        // Sponge Start - notify the cause tracker
        if (((LevelBridge) this.shadow$level()).bridge$isFake()) {
            return;
        }
        final PhaseTracker instance = PhaseTracker.getWorldInstance((ServerLevel) thisEntity.level());
        if (!instance.onSidedThread()) {
            return;
        }
        final PhaseContext<@NonNull ?> context = instance.getPhaseContext();
        if (!context.doesBlockEventTracking()) {
            return;
        }
        try (final EffectTransactor ignored = context.getTransactor().ensureEntityDropTransactionEffect((LivingEntity) (Object) this)) {
            // Create new EntityDeathTransaction
            // Add new EntityDeathEffect
            this.shadow$die(cause);
        }
        // Sponge End
    }

    @Redirect(
        method = "aiStep",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;pushEntities()V")
    )
    private void tracker$switchIntoCollisions(final LivingEntity livingEntity) {
        if (this.shadow$level().isClientSide) {
            this.shadow$pushEntities();
            return;
        }
        try (final PhaseContext<@NonNull ?> context = EntityPhase.State.COLLISION
            .createPhaseContext(PhaseTracker.getWorldInstance((ServerLevel) livingEntity.level()))
            .source(livingEntity)
        ) {
            context.buildAndSwitch();
            this.shadow$pushEntities();
        }
    }

    @Redirect(
        method = "tickEffects",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/effect/MobEffectInstance;tickServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/Runnable;)Z"
        )
    )
    private boolean impl$wrapEffectWithFrame(
        final MobEffectInstance instance, final ServerLevel level, final LivingEntity thisEntity, final Runnable runnable) {
        try {
            PhaseTracker.getInstance().pushCause(instance); // push the PotionEffect
            return instance.tickServer(level, thisEntity, runnable);
        } finally {
            PhaseTracker.getInstance().popCause();
        }
    }

}
