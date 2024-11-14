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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(value = LivingEntity.class, priority = 900)
public abstract class LivingEntityMixin_Attack_Impl {

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyConstant(method = "resolvePlayerResponsibleForDamage", constant = @Constant(classValue = Wolf.class, ordinal = 0))
    private Class attackImpl$onWolfCast(final Object entity, final Class wolf) {
        return TamableAnimal.class;
    }

    @Redirect(method = "resolvePlayerResponsibleForDamage",
        at = @At(value = "INVOKE" , target = "Lnet/minecraft/world/entity/animal/Wolf;isTame()Z"))
    private boolean attackImpl$onWolfIsTame(@Coerce final Object instance) {
        return ((TamableAnimal)instance).isTame();
    }

    @Redirect(method = "resolvePlayerResponsibleForDamage",
        at = @At(value = "INVOKE" , target = "Lnet/minecraft/world/entity/animal/Wolf;getOwner()Lnet/minecraft/world/entity/LivingEntity;"))
    private LivingEntity attackImpl$onWolfGetOwner(@Coerce final Object instance) {
        return ((TamableAnimal)instance).getOwner();
    }

    /**
     * Prevents {@link ServerPlayer#awardStat} from running before event
     */
    @Redirect(method = "getDamageAfterMagicAbsorb",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;awardStat(Lnet/minecraft/resources/ResourceLocation;I)V"))
    public void attackImpl$onAwardStatDamageResist(final ServerPlayer instance, final ResourceLocation resourceLocation, final int i) {
        // do nothing
    }


}
