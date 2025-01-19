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
package org.spongepowered.common.mixin.core.server.level;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.mixin.core.world.entity.player.PlayerMixin_Attack_Impl;
import org.spongepowered.common.util.DamageEventUtil;

import java.util.ArrayList;

@Mixin(value = ServerPlayer.class, priority = 900)
public abstract class ServerPlayerMixin_Attack_impl extends PlayerMixin_Attack_Impl {

    @Override
    protected void attackImpl$enchanttDamageFunc(Entity $$0, CallbackInfo ci) {
        final var weapon = this.attackImpl$attack.weapon();
        // this.getEnchantedDamage(targetEntity, damage, damageSource) - damage;
        final var functions = DamageEventUtil.createAttackEnchantmentFunction(weapon, this.attackImpl$attack.target(), this.attackImpl$attack.dmgSource());
        final var separateFunc = DamageEventUtil.provideSeparateEnchantmentFromBaseDamageFunction(this.attackImpl$attack.baseDamage(), weapon);
        // enchantmentDamage *= attackStrength;
        final var strengthScaleFunc = DamageEventUtil.provideCooldownEnchantmentStrengthFunction(weapon, this.attackImpl$attack.strengthScale());

        this.attackImpl$attack.functions().addAll(functions);
        this.attackImpl$attack.functions().add(separateFunc);
        this.attackImpl$attack.functions().add(strengthScaleFunc);
    }

    @Override
    protected double attackImpl$beforeSweepHurt(
        final Player instance, final Entity sweepTarget, final Operation<Double> original
    ) {
        final var distanceToSqr = original.call(instance, sweepTarget);
        if (!(distanceToSqr < 9.0)) {
            return distanceToSqr; // Too far - no event
        }

        final var mainAttack = this.attackImpl$attack;
        final var mainAttackDamage = this.attackImpl$finalDamageAmounts.getOrDefault("minecraft:attack_damage", 0.0).floatValue();

        var sweepAttack = new DamageEventUtil.Attack<>(mainAttack.sourceEntity(), sweepTarget, mainAttack.weapon(), mainAttack.dmgSource(), mainAttack.strengthScale(), 1, new ArrayList<>());
        // float sweepBaseDamage = 1.0F + (float)this.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * attackDamage;
        sweepAttack.functions().add(DamageEventUtil.provideSweepingDamageRatioFunction(mainAttack.weapon(), mainAttack.sourceEntity(), mainAttackDamage));
        // float sweepFullDamage = this.getEnchantedDamage(sweepTarget, sweepBaseDamage, $$3) * strengthScale;
        sweepAttack.functions().addAll(DamageEventUtil.createAttackEnchantmentFunction(mainAttack.weapon(), sweepTarget, mainAttack.dmgSource()));
        sweepAttack.functions().add(DamageEventUtil.provideCooldownEnchantmentStrengthFunction(mainAttack.weapon(), mainAttack.strengthScale()));

        this.attackImpl$attackEvent = DamageEventUtil.callPlayerAttackEntityEvent(sweepAttack, 1.0F);
        if (attackImpl$attackEvent.isCancelled()) {
            return Double.MAX_VALUE;
        }

        return distanceToSqr;
    }
}
