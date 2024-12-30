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
package org.spongepowered.forge.mixin.core.world.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import org.spongepowered.api.event.cause.entity.damage.DamageStepTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.common.bridge.world.entity.TrackedDamageBridge;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageStep;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageTracker;
import org.spongepowered.common.item.util.ItemStackUtil;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_Forge_Damage implements TrackedDamageBridge {

    @ModifyConstant(method = "actuallyHurt", constant = @Constant(floatValue = 0), slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/ForgeHooks;onLivingHurt(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;F)F"),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")))
    private float damage$doNotReturnEarly(final float constant) {
        return Float.NaN;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/ForgeEventFactory;onShieldBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;F)Lnet/minecraftforge/event/entity/living/ShieldBlockEvent;"))
    private ShieldBlockEvent damage$modifyBeforeAndAfterShield(final LivingEntity self, final DamageSource source, final float originalDamage) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        if (tracker == null) {
            return ForgeEventFactory.onShieldBlock(self, source, originalDamage);
        }

        final SpongeDamageStep step = tracker.newStep(DamageStepTypes.SHIELD, originalDamage, ItemStackUtil.snapshotOf(self.getUseItem()));
        float damage = (float) step.applyModifiersBefore();
        final ShieldBlockEvent event;
        if (step.isSkipped()) {
            event = new ShieldBlockEvent(self, source, damage);
            event.setCanceled(true);
        } else {
            event = ForgeEventFactory.onShieldBlock(self, source, damage);
            if (!event.isCanceled()) {
                damage -= event.getBlockedDamage();
            }
        }
        step.applyModifiersAfter(damage);
        return event;
    }

    @ModifyArg(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/ForgeHooks;onLivingDamage(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float damage$firePostEvent_Living(final float damage) {
        return this.damage$firePostEvent(damage);
    }
}
