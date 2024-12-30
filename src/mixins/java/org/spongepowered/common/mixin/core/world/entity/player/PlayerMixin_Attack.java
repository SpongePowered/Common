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
package org.spongepowered.common.mixin.core.world.entity.player;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.event.cause.entity.damage.DamageStepTypes;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.bridge.world.entity.TrackedAttackBridge;
import org.spongepowered.common.event.cause.entity.damage.SpongeAttackTracker;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageStep;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;
import org.spongepowered.common.mixin.core.world.entity.LivingEntityMixin_Damage;

import java.util.Deque;
import java.util.LinkedList;

@SuppressWarnings("ConstantConditions")
@Mixin(value = Player.class, priority = 900)
public abstract class PlayerMixin_Attack extends LivingEntityMixin_Damage implements TrackedAttackBridge {

    //@formatter:off
    @Shadow protected abstract float shadow$getEnchantedDamage(final Entity target, final float damage, final DamageSource source);
    @Shadow @Final public InventoryMenu inventoryMenu;
    //@formatter:on

    private final Deque<SpongeAttackTracker> attack$trackers = new LinkedList<>();

    @Override
    public final @Nullable SpongeAttackTracker attack$tracker() {
        return this.attack$trackers.peekLast();
    }

    @Inject(method = "attack", locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEnchantedDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F", ordinal = 0))
    private void attack$firePreEvent(final Entity target, final CallbackInfo ci, final float damage, final ItemStack weapon, final DamageSource source) {
        final SpongeAttackTracker tracker = SpongeAttackTracker.callAttackPreEvent((org.spongepowered.api.entity.Entity) target, source, damage, weapon);
        if (tracker == null) {
            ci.cancel();
        } else {
            this.attack$trackers.addLast(tracker);
        }
    }

    @ModifyVariable(method = "attack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEnchantedDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F", ordinal = 0))
    private float attack$setBaseDamage(final float damage) {
        final SpongeAttackTracker tracker = this.attack$tracker();
        return tracker == null ? damage : (float) tracker.preEvent().baseDamage();
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttackStrengthScale(F)F"))
    private float attack$captureAttackStrength(final Player self, final float param) {
        final float value = self.getAttackStrengthScale(param);
        final SpongeAttackTracker tracker = this.attack$tracker();
        if (tracker != null) {
            tracker.setAttackStrength(value);
        }
        return value;
    }

    @ModifyVariable(method = "attack", at = @At("LOAD"), ordinal = 0, slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttackStrengthScale(F)F"),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;resetAttackStrengthTicker()V")
    ))
    private float attack$modifyBeforeBaseCooldown(final float damage) {
        final SpongeAttackTracker tracker = this.attack$tracker();
        return tracker == null ? damage : tracker.startStep(DamageStepTypes.BASE_COOLDOWN, damage, this);
    }

    @ModifyVariable(method = "attack", at = @At("STORE"), ordinal = 0, slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttackStrengthScale(F)F"),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;resetAttackStrengthTicker()V")
    ))
    private float attack$modifyAfterBaseCooldown(final float damage) {
        final SpongeAttackTracker tracker = this.attack$tracker();
        return tracker == null ? damage : tracker.endStep(DamageStepTypes.BASE_COOLDOWN, damage);
    }

    @ModifyVariable(method = "attack", at = @At("LOAD"), ordinal = 1, slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttackStrengthScale(F)F"),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;resetAttackStrengthTicker()V")
    ))
    private float attack$modifyBeforeEnchantmentCooldown(final float damage) {
        final SpongeAttackTracker tracker = this.attack$tracker();
        return tracker == null ? damage : tracker.startStep(DamageStepTypes.ENCHANTMENT_COOLDOWN, damage, tracker.weaponSnapshot());
    }

    @ModifyVariable(method = "attack", at = @At("STORE"), ordinal = 1, slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttackStrengthScale(F)F"),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;resetAttackStrengthTicker()V")
    ))
    private float attack$modifyAfterEnchantmentCooldown(final float damage) {
        final SpongeAttackTracker tracker = this.attack$tracker();
        return tracker == null ? damage : tracker.endStep(DamageStepTypes.ENCHANTMENT_COOLDOWN, damage);
    }

    @Inject(method = "attack",  at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"), slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSprinting()Z", ordinal = 0),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getAttackDamageBonus(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F")))
    private void attack$captureStrongSprint(final Entity target, final CallbackInfo ci) {
        final SpongeAttackTracker tracker = this.attack$tracker();
        if (tracker != null) {
            tracker.setStrongSprint(true);
        }
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getAttackDamageBonus(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F"))
    private float attack$modifyBeforeAndAfterWeaponBonus(final Item item, final Entity target, final float originalDamage, final DamageSource source) {
        final SpongeAttackTracker tracker = this.attack$tracker();
        if (tracker == null) {
            return item.getAttackDamageBonus(target, originalDamage, source);
        }

        final SpongeDamageStep step = tracker.newStep(DamageStepTypes.WEAPON_BONUS, originalDamage, tracker.weaponSnapshot());
        float damage = (float) step.applyModifiersBefore();
        if (!step.isSkipped()) {
            damage += item.getAttackDamageBonus(target, damage, source);
        }
        return (float) step.applyModifiersAfter(damage) - originalDamage;
    }

    @ModifyVariable(method = "attack", at = @At(value = "LOAD", ordinal = 0), ordinal = 0, slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSprinting()Z", ordinal = 1),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;horizontalDistanceSqr()D")
    ))
    private float attack$modifyBeforeCriticalHit(final float damage) {
        final SpongeAttackTracker tracker = this.attack$tracker();
        return tracker == null ? damage : tracker.startStep(DamageStepTypes.CRITICAL_HIT, damage, this);
    }

    @ModifyVariable(method = "attack", at = @At("STORE"), ordinal = 0, slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSprinting()Z", ordinal = 1),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;horizontalDistanceSqr()D")
    ))
    private float attack$modifyAfterCriticalHit(final float damage) {
        final SpongeAttackTracker tracker = this.attack$tracker();
        return tracker == null ? damage : tracker.endStep(DamageStepTypes.CRITICAL_HIT, damage);
    }

    @SuppressWarnings("deprecation")
    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getKnockback(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)F")
    ))
    private boolean attack$firePostEvent(final Entity target, final DamageSource source, float damage) {
        final SpongeAttackTracker tracker = this.attack$tracker();
        if (tracker != null) {
            final float knockbackModifier = this.shadow$getKnockback(target, source) + (tracker.isStrongSprint() ? 1.0F : 0.0F);
            if (tracker.callAttackPostEvent((org.spongepowered.api.entity.Entity) target, source, damage, knockbackModifier)) {
                return false;
            }
            damage = (float) tracker.postEvent().finalDamage();
        }
        return target.hurtOrSimulate(source, damage);
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getKnockback(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)F"))
    private float attack$knockbackModifier(final Player self, final Entity target, final DamageSource source) {
        final SpongeAttackTracker tracker = this.attack$tracker();
        return tracker == null ? this.shadow$getKnockback(target, source) : ((float) tracker.postEvent().knockbackModifier() - (tracker.isStrongSprint() ? 1.0F : 0.0F));
    }

    @Redirect(method = "attack",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private void attack$preventSound(final Level level, final Player player, final double x, final double y, final double z,
            final SoundEvent sound, final SoundSource source, final float volume, final float pitch) {
        final SpongeAttackTracker tracker = this.attack$tracker();
        if (tracker == null || !tracker.postEvent().isCancelled()) {
            level.playSound(player, x, y, z, sound, source, volume, pitch);
        }
    }

    @Inject(method = "attack", at = @At("RETURN"), slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEnchantedDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F", ordinal = 0)
    ))
    private void attack$removeTracker(CallbackInfo ci) {
        this.attack$trackers.removeLast();
    }

    @Redirect(method = "attack",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"))
    private double sweepAttack$fireEvents(final Player self, final Entity sweepTarget) {
        final double distanceSquared = self.distanceToSqr(sweepTarget);
        if (!(distanceSquared < this.attack$interactionRangeSquared())) {
            return distanceSquared;
        }

        final SpongeAttackTracker mainTracker = this.attack$tracker();
        if (mainTracker == null) {
            return distanceSquared;
        }

        final AttackEntityEvent.Post mainEvent = mainTracker.postEvent();
        DamageSource source = (DamageSource) mainEvent.source();
        float damage = (float) (mainEvent.finalDamage() - mainTracker.damageAfter(DamageStepTypes.ENCHANTMENT_COOLDOWN));

        final SpongeAttackTracker sweepTracker = SpongeAttackTracker.callAttackPreEvent((org.spongepowered.api.entity.Entity) sweepTarget, source, damage, mainTracker.weapon());
        if (sweepTracker == null) {
            return Double.MAX_VALUE;
        }

        this.attack$trackers.addLast(sweepTracker);
        damage = (float) sweepTracker.preEvent().baseDamage();

        // In vanilla, this step is outside the loop, but we move it to here so it can be modified per target
        SpongeDamageStep step = sweepTracker.newStep(DamageStepTypes.SWEEPING, damage, sweepTracker.weaponSnapshot());
        damage = (float) step.applyModifiersBefore();
        if (!step.isSkipped()) {
            damage = 1.0F + (float) this.shadow$getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * damage;
        }
        damage = (float) step.applyModifiersAfter(damage);

        damage = this.shadow$getEnchantedDamage(sweepTarget, damage, source);

        step = sweepTracker.newStep(DamageStepTypes.ENCHANTMENT_COOLDOWN, damage, sweepTracker.weaponSnapshot());
        damage = (float) step.applyModifiersBefore();
        if (!step.isSkipped()) {
            damage *= mainTracker.attackStrength();
        }
        damage = (float) step.applyModifiersAfter(damage);

        if (sweepTracker.callAttackPostEvent((org.spongepowered.api.entity.Entity) sweepTarget, source, damage, 0.4F)) {
            this.attack$trackers.removeLast();
            return Double.MAX_VALUE;
        }

        return distanceSquared;
    }

    @Redirect(method = "attack",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEnchantedDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F"))
    private float sweepAttack$cancelEnchantedDamage(final Player self, final Entity sweepTarget, final float damage, final DamageSource source) {
        return damage; // We already did it above
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"), slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)V")))
    private void sweepAttack$knockbackModifier(final LivingEntity sweepTarget, double modifier, final double dirX, final double dirZ) {
        final SpongeAttackTracker sweepTracker = this.attack$tracker();
        if (sweepTracker != null) {
            modifier = sweepTracker.postEvent().knockbackModifier();
        }
        sweepTarget.knockback(modifier, dirX, dirZ);
    }

    @SuppressWarnings("deprecation")
    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")))
    private void sweepAttack$finalDamage(final LivingEntity sweepTarget, final DamageSource source, float damage) {
        final SpongeAttackTracker sweepTracker = this.attack$tracker();
        if (sweepTracker != null) {
            damage = (float) sweepTracker.postEvent().finalDamage();
        }
        sweepTarget.hurt(source, damage);
        this.attack$trackers.removeLast();
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V", shift = At.Shift.AFTER))
    private void attack$captureInventoryChange(final CallbackInfo ci) {
        PhaseTracker.getWorldInstance(this.shadow$level()).getPhaseContext().getTransactor().logPlayerInventoryChange((Player) (Object) this, PlayerInventoryTransaction.EventCreator.STANDARD);
        this.inventoryMenu.broadcastChanges();
    }
}
