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
package org.spongepowered.common.mixin.core.world.item.enchantment;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.spongepowered.api.event.cause.entity.damage.DamageStepTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.event.cause.entity.damage.SpongeAttackTracker;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageStep;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.List;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin_Attack {

    @Shadow protected abstract void shadow$modifyDamageFilteredValue(
        final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> component,
        final ServerLevel level, final int enchantmentLevel, final ItemStack weapon, final Entity target, final DamageSource source, final MutableFloat damage);

    @Redirect(method = "modifyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;modifyDamageFilteredValue(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/server/level/ServerLevel;ILnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lorg/apache/commons/lang3/mutable/MutableFloat;)V"))
    private void attack$modifyWeaponEnchantment(
        final Enchantment self, final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> component,
        final ServerLevel level, final int enchantmentLevel, final ItemStack weapon, final Entity target, final DamageSource source, final MutableFloat damage) {

        final SpongeAttackTracker tracker = SpongeAttackTracker.of(source);
        if (tracker == null) {
            this.shadow$modifyDamageFilteredValue(component, level, enchantmentLevel, weapon, target, source, damage);
            return;
        }

        final SpongeDamageStep step = tracker.newStep(DamageStepTypes.WEAPON_ENCHANTMENT, damage.floatValue(), ItemStackUtil.snapshotOf(weapon), self);
        damage.setValue((float) step.applyModifiersBefore());
        if (!step.isSkipped()) {
            this.shadow$modifyDamageFilteredValue(component, level, enchantmentLevel, weapon, target, source, damage);
        }
        damage.setValue((float) step.applyModifiersAfter(damage.floatValue()));
    }
}
