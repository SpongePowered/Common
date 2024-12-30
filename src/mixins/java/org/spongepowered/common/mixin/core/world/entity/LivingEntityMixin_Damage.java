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

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.event.cause.entity.damage.DamageStepTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.bridge.world.entity.LivingEntityBridge;
import org.spongepowered.common.bridge.world.entity.TrackedDamageBridge;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageStep;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageTracker;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;

import java.util.Deque;
import java.util.LinkedList;

@Mixin(value = LivingEntity.class, priority = 900)
public abstract class LivingEntityMixin_Damage extends EntityMixin implements LivingEntityBridge, TrackedDamageBridge {

    //@formatter:off
    @Shadow protected abstract void shadow$playHurtSound(final DamageSource source);
    @Shadow protected abstract float shadow$getKnockback(final Entity entity, final DamageSource source);
    @Shadow public abstract @NonNull ItemStack shadow$getWeaponItem();
    @Shadow public abstract ItemStack shadow$getItemBySlot(final EquipmentSlot slot);
    @Shadow protected abstract void shadow$hurtHelmet(final DamageSource source, final float damage);
    @Shadow protected abstract void shadow$hurtArmor(final DamageSource source, final float damage);
    @Shadow public abstract @Nullable MobEffectInstance shadow$getEffect(final Holder<MobEffect> effect);
    @Shadow public abstract double shadow$getAttributeValue(final Holder<Attribute> attribute);
    // @formatter:on

    private final Deque<SpongeDamageTracker> damage$trackers = new LinkedList<>();
    private boolean damage$inventoryChanged = false;

    @Override
    public final @Nullable SpongeDamageTracker damage$tracker() {
        return this.damage$trackers.peekLast();
    }

    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"), cancellable = true)
    private void damage$firePreEvent(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfoReturnable<Boolean> cir) {
        final SpongeDamageTracker tracker = SpongeDamageTracker.callDamagePreEvent((org.spongepowered.api.entity.Entity) this, source, this.damage$getContainerDamage(damage));
        if (tracker == null) {
            cir.setReturnValue(false);
        } else {
            this.damage$trackers.addLast(tracker);
            this.damage$setContainerDamage((float) tracker.preEvent().baseDamage());
        }
    }

    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtCurrentlyUsedShield(F)V"))
    private void damage$onHurtShield(final CallbackInfoReturnable<Boolean> cir) {
        this.damage$inventoryChanged = true;
    }

    @ModifyVariable(method = "hurtServer", argsOnly = true, at = @At("STORE"), slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtCurrentlyUsedShield(F)V"),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;blockUsingShield(Lnet/minecraft/world/entity/LivingEntity;)V")
    ))
    private float damage$setDamageAfterShield(final float damage) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        if (tracker == null) {
            return damage;
        }
        final SpongeDamageStep step = tracker.currentStep(DamageStepTypes.SHIELD);
        return step == null ? damage : (float) step.damageAfterModifiers();
    }

    @ModifyVariable(method = "hurtServer", at = @At("LOAD"), argsOnly = true, slice = @Slice(
        from = @At(value = "FIELD", target = "Lnet/minecraft/tags/EntityTypeTags;FREEZE_HURTS_EXTRA_TYPES:Lnet/minecraft/tags/TagKey;"),
        to = @At(value = "FIELD", target = "Lnet/minecraft/tags/DamageTypeTags;DAMAGES_HELMET:Lnet/minecraft/tags/TagKey;")
    ))
    private float damage$modifyBeforeFreezingBonus(final float damage) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        return tracker == null ? damage : tracker.startStep(DamageStepTypes.FREEZING_BONUS, damage, tracker.preEvent().source(), this);
    }

    @ModifyVariable(method = "hurtServer", at = @At("STORE"), argsOnly = true, slice = @Slice(
        from = @At(value = "FIELD", target = "Lnet/minecraft/tags/EntityTypeTags;FREEZE_HURTS_EXTRA_TYPES:Lnet/minecraft/tags/TagKey;"),
        to = @At(value = "FIELD", target = "Lnet/minecraft/tags/DamageTypeTags;DAMAGES_HELMET:Lnet/minecraft/tags/TagKey;")
    ))
    private float damage$modifyAfterFreezingBonus(final float damage) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        return tracker == null ? damage : tracker.endStep(DamageStepTypes.FREEZING_BONUS, damage);
    }

    @ModifyVariable(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtHelmet(Lnet/minecraft/world/damagesource/DamageSource;F)V"), argsOnly = true)
    private float damage$modifyBeforeHardHat(final float damage) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        return tracker == null ? damage : tracker.startStep(DamageStepTypes.HARD_HAT, damage, this.shadow$getItemBySlot(EquipmentSlot.HEAD));
    }

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtHelmet(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void damage$skipHardHat(final LivingEntity self, final DamageSource source, final float damage) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        if (tracker == null || !tracker.isSkipped(DamageStepTypes.HARD_HAT)) {
            this.shadow$hurtHelmet(source, damage);
            this.damage$inventoryChanged = true;
        }
    }

    @ModifyVariable(method = "hurtServer", at = @At("STORE"), argsOnly = true, slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtHelmet(Lnet/minecraft/world/damagesource/DamageSource;F)V"),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;setSpeed(F)V")
    ))
    private float damage$modifyAfterHardHat(final float damage) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        return tracker == null ? damage : tracker.endStep(DamageStepTypes.HARD_HAT, damage);
    }

    @ModifyVariable(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V"), argsOnly = true)
    private float damage$modifyBeforeArmor(final float damage) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        return tracker == null ? damage : tracker.startStep(DamageStepTypes.ARMOR, damage, this, Attributes.ARMOR_TOUGHNESS);
    }

    @Redirect(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void damage$skipArmor(final LivingEntity self, final DamageSource source, final float damage) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        if (tracker == null || !tracker.isSkipped(DamageStepTypes.ARMOR)) {
            this.shadow$hurtArmor(source, damage);
            this.damage$inventoryChanged = true;
        }
    }

    @ModifyVariable(method = "getDamageAfterArmorAbsorb", at = @At("STORE"), argsOnly = true)
    private float damage$modifyAfterArmor(final float damage) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        return tracker == null ? damage : tracker.endStep(DamageStepTypes.ARMOR, damage);
    }

    @ModifyVariable(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getEffect(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/effect/MobEffectInstance;"), argsOnly = true)
    private float damage$modifyBeforeDefensivePotionEffect(final float damage) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        return tracker == null ? damage : tracker.startStep(DamageStepTypes.DEFENSIVE_POTION_EFFECT, damage, this.shadow$getEffect(MobEffects.DAMAGE_RESISTANCE));
    }

    @ModifyVariable(method = "getDamageAfterMagicAbsorb", at = @At("STORE"), argsOnly = true, slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getEffect(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/effect/MobEffectInstance;"),
        to = @At(value = "FIELD", target = "Lnet/minecraft/stats/Stats;DAMAGE_RESISTED:Lnet/minecraft/resources/ResourceLocation;")))
    private float damage$modifyAfterDefensivePotionEffect(final float damage) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        return tracker == null ? damage : tracker.endStep(DamageStepTypes.DEFENSIVE_POTION_EFFECT, damage);
    }

    @Redirect(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterMagicAbsorb(FF)F"))
    private float damage$modifyBeforeAndAfterArmorEnchantment(float damage, final float protection) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        if (tracker == null) {
            return CombatRules.getDamageAfterMagicAbsorb(damage, protection);
        }

        final SpongeDamageStep step = tracker.newStep(DamageStepTypes.ARMOR_ENCHANTMENT, damage, this);
        damage = (float) step.applyModifiersBefore();
        if (!step.isSkipped()) {
            damage = CombatRules.getDamageAfterMagicAbsorb(damage, protection);
        }
        return (float) step.applyModifiersAfter(damage);
    }

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE",  target = "Lnet/minecraft/world/entity/LivingEntity;playHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void damage$onHurtSound(final LivingEntity self, final DamageSource source) {
        if (this.bridge$vanishState().createsSounds()) {
            this.shadow$playHurtSound(source);
        }
    }

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE",  target = "Lnet/minecraft/world/entity/LivingEntity;makeSound(Lnet/minecraft/sounds/SoundEvent;)V"))
    private void damage$onMakeSound(final LivingEntity self, final SoundEvent sound) {
        if (this.bridge$vanishState().createsSounds()) {
            self.makeSound(sound);
        }
    }

    @Inject(method = "hurtServer", at = @At("RETURN"), slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z", shift = At.Shift.AFTER)
    ))
    private void damage$removeTrackerAndCaptureInventory(final CallbackInfoReturnable<Boolean> cir) {
        this.damage$trackers.removeLast();

        if (this.damage$inventoryChanged) {
            this.damage$inventoryChanged = false;

            if ((Object) this instanceof Player player) {
                PhaseTracker.getWorldInstance((ServerLevel) player.level()).getPhaseContext().getTransactor().logPlayerInventoryChange(player, PlayerInventoryTransaction.EventCreator.STANDARD);
                player.inventoryMenu.broadcastChanges();
            }
        }
    }
}
