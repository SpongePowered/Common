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
package org.spongepowered.common.mixin.core.world.entity.decoration;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.gameevent.GameEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageTracker;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.mixin.core.world.entity.LivingEntityMixin;

@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin extends LivingEntityMixin {

    // @formatter:off
    @Shadow protected abstract void shadow$causeDamage(ServerLevel level, DamageSource source, float damage); // damageArmorStand
    @Shadow protected abstract void shadow$brokenByPlayer(final ServerLevel level, final DamageSource source);
    // @formatter:on

    /**
     * The return value is set to false if the entity should not be completely destroyed.
     */
    private void impl$callDamageBeforeKill(final DamageSource source, final CallbackInfoReturnable<Boolean> cir) {
        final DamageEntityEvent.Post event = SpongeDamageTracker.callDamageEvents((Entity) this, source, this.shadow$getHealth());
        if (event == null) {
            cir.setReturnValue(false);
        } else if (event.finalDamage() < this.shadow$getHealth()) { // Deal reduced damage?
            this.shadow$causeDamage((ServerLevel) this.shadow$level(), source, (float) event.finalDamage());
            cir.setReturnValue(false);
        }
        // else kill?
    }

    /**
     * BYPASSES_INVULNERABILITY
     */
    @Inject(method = "hurtServer", cancellable = true,
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/tags/DamageTypeTags;BYPASSES_INVULNERABILITY:Lnet/minecraft/tags/TagKey;")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;kill(Lnet/minecraft/server/level/ServerLevel;)V", ordinal = 0))
    private void impl$fireDamageEventOutOfWorld(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfoReturnable<Boolean> cir) {
        this.impl$callDamageBeforeKill(source, cir);
    }

    /**
     * IS_EXPLOSION
     */
    @Inject(method = "hurtServer", cancellable = true,
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/tags/DamageTypeTags;IS_EXPLOSION:Lnet/minecraft/tags/TagKey;")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;brokenByAnything(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void impl$fireDamageEventExplosion(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfoReturnable<Boolean> cir) {
        this.impl$callDamageBeforeKill(source, cir);
    }

    /**
     * causeDamage with damage value
     * IGNITES_ARMOR_STANDS + isOnFire
     * BURNS_ARMOR_STANDS + health > 0.5
     */
    @Redirect(method = "hurtServer",
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/tags/DamageTypeTags;IGNITES_ARMOR_STANDS:Lnet/minecraft/tags/TagKey;")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;causeDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void impl$fireDamageEventDamage(final ArmorStand self, final ServerLevel level, final DamageSource source, final float damage) {
        final DamageEntityEvent.Post event = SpongeDamageTracker.callDamageEvents((Entity) this, source, damage);
        if (event != null) {
            this.shadow$causeDamage(level, source, (float) event.finalDamage());
        }
    }

    /**
     * {@link DamageSource#isCreativePlayer()}
     */
    @Inject(method = "hurtServer",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;isCreativePlayer()Z")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;playBrokenSound()V"), cancellable = true)
    private void impl$fireDamageEventCreativePunch(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfoReturnable<Boolean> cir) {
        this.impl$callDamageBeforeKill(source, cir);
    }

    /**
     * {@link ArmorStand#lastHit} was not recently
     */
    @Inject(method = "hurtServer", cancellable = true,
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;lastHit:J", opcode = Opcodes.GETFIELD)),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;broadcastEntityEvent(Lnet/minecraft/world/entity/Entity;B)V"))
    private void impl$fireDamageEventFirstPunch(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfoReturnable<Boolean> cir) {
        // While this doesn't technically "damage" the armor stand, it feels like damage in other respects, so fire an event.
        final DamageEntityEvent.Post event = SpongeDamageTracker.callDamageEvents((Entity) this, source, 0);
        if (event == null) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurtServer", cancellable = true, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/decoration/ArmorStand;brokenByPlayer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void impl$beforeBrokenByPlayer(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfoReturnable<Boolean> cir) {
        if (ShouldFire.DESTRUCT_ENTITY_EVENT && !((LevelBridge) this.shadow$level()).bridge$isFake()) {
            final var event = SpongeCommonEventFactory.callDestructEntityEventDeath((ArmorStand) (Object) this, null);
            if (event.isCancelled()) {
                cir.setReturnValue(false);
            }
            // TODO event.keepInventory() actually prevent inventory drops
        }
    }

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/decoration/ArmorStand;brokenByPlayer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    public void impl$onBrokenByPlayer(final ArmorStand self, final ServerLevel level, final DamageSource source) {
        try (final EffectTransactor ignored = PhaseTracker.getWorldInstance(level).getPhaseContext().getTransactor().ensureEntityDropTransactionEffect((LivingEntity) (Object) this)) {
            this.shadow$brokenByPlayer(level, source);
        }
    }

    /**
     * {@link ArmorStand#lastHit} was recently
     */
    @Inject(method = "hurtServer", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/decoration/ArmorStand;brokenByPlayer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void impl$fireDamageEventSecondPunch(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfoReturnable<Boolean> cir) {
        this.impl$callDamageBeforeKill(source, cir);
    }

    /**
     * To avoid a loop between {@link #kill} and {@link ArmorStand#hurtServer},
     * we make sure that any killing within the {@link ArmorStand#hurtServer}
     * method calls this instead of the {@link Overwrite}
     *
     * @param self the killed stand
     */
    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;kill(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void impl$actuallyKill(final ArmorStand self, final ServerLevel level) {
        self.remove(RemovalReason.KILLED);
        self.gameEvent(GameEvent.ENTITY_DIE);
    }

    /**
     * To avoid a loop between {@link #kill} and {@link #shadow$causeDamage},
     * we make sure that any killing within the {@link #shadow$causeDamage}
     * method calls this instead of the {@link Overwrite}
     *
     * @param self the killed stand
     */
    @Redirect(method = "causeDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;kill(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void impl$actuallyKill2(final ArmorStand self, final ServerLevel level) {
        self.remove(RemovalReason.KILLED);
        self.gameEvent(GameEvent.ENTITY_DIE);
    }

    /**
     * @author JBYoshi
     * @reason EntityArmorStand "simplifies" this method to simply call {@link
     * #shadow$remove(RemovalReason)}. However, this ignores our custom event.
     * Instead, delegate to the superclass causing it to use
     * {@link ArmorStand#hurtServer} with {@link DamageTypes#GENERIC_KILL}
     *
     * This needs to be reimplemented in {@link #impl$actuallyKill}!
     */
    @Overwrite
    public void kill(final ServerLevel level) {
        super.shadow$kill(level);
    }
}
