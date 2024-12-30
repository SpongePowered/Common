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
package org.spongepowered.vanilla.mixin.core.world.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.api.event.cause.entity.damage.DamageStepTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.common.bridge.world.entity.TrackedDamageBridge;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageStep;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageTracker;
import org.spongepowered.common.item.util.ItemStackUtil;

@Mixin(value = LivingEntity.class, priority = 900)
public abstract class LivingEntityMixin_Vanilla_Damage implements TrackedDamageBridge {

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isDamageSourceBlocked(Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    private boolean damage$modifyBeforeAndAfterShield(final LivingEntity self, final DamageSource source) {
        if (!self.isDamageSourceBlocked(source)) {
            return false;
        }

        final SpongeDamageTracker tracker = this.damage$tracker();
        if (tracker == null) {
            return true;
        }

        final SpongeDamageStep step = tracker.newStep(DamageStepTypes.SHIELD, (float) tracker.preEvent().baseDamage(), ItemStackUtil.snapshotOf(self.getUseItem()));
        step.applyModifiersBefore();
        step.applyModifiersAfter(0);
        return !step.isSkipped();
    }

    @ModifyVariable(method = "hurtServer", ordinal = 2, at = @At("STORE"), slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtCurrentlyUsedShield(F)V"),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;blockUsingShield(Lnet/minecraft/world/entity/LivingEntity;)V")
    ))
    private float damage$setBlockedDamage(final float damage) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        if (tracker == null) {
            return damage;
        }
        final SpongeDamageStep step = tracker.currentStep(DamageStepTypes.SHIELD);
        return step == null ? damage : (float) Math.max(step.damageBeforeStep(), 0);
    }

    @ModifyVariable(method = "hurtServer", at = @At("STORE"), slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;blockUsingShield(Lnet/minecraft/world/entity/LivingEntity;)V"),
        to = @At(value = "FIELD", target = "Lnet/minecraft/tags/DamageTypeTags;IS_FREEZING:Lnet/minecraft/tags/TagKey;")
    ))
    private boolean damage$setBlockedFlag(final boolean blocked) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        if (tracker == null) {
            return blocked;
        }
        final SpongeDamageStep step = tracker.currentStep(DamageStepTypes.SHIELD);
        return step == null ? blocked : step.damageAfterModifiers() <= 0;
    }

    @ModifyVariable(method = "actuallyHurt", at = @At("LOAD"), argsOnly = true, slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;awardStat(Lnet/minecraft/resources/ResourceLocation;I)V"),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCombatTracker()Lnet/minecraft/world/damagesource/CombatTracker;")))
    private float damage$firePostEvent_Living(final float damage) {
        return this.damage$firePostEvent(damage);
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyConstant(method = "resolvePlayerResponsibleForDamage", constant = @Constant(classValue = Wolf.class, ordinal = 0))
    private Class damage$onWolfCast(final Object entity, final Class wolf) {
        return TamableAnimal.class;
    }

    @Redirect(method = "resolvePlayerResponsibleForDamage", at = @At(value = "INVOKE" , target = "Lnet/minecraft/world/entity/animal/Wolf;isTame()Z"))
    private boolean damage$onWolfIsTame(@Coerce final Object instance) {
        return ((TamableAnimal) instance).isTame();
    }

    @Redirect(method = "resolvePlayerResponsibleForDamage", at = @At(value = "INVOKE" , target = "Lnet/minecraft/world/entity/animal/Wolf;getOwner()Lnet/minecraft/world/entity/LivingEntity;"))
    private LivingEntity damage$onWolfGetOwner(@Coerce final Object instance) {
        return ((TamableAnimal) instance).getOwner();
    }
}
