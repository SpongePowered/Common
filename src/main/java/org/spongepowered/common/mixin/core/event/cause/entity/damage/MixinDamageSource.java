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
package org.spongepowered.common.mixin.core.event.cause.entity.damage;

import org.spongepowered.api.event.cause.entity.damage.DamageType;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.registry.SpongeGameRegistry;

@Mixin(net.minecraft.util.DamageSource.class)
@Implements(@Interface(iface = DamageSource.class, prefix = "damage$"))
public abstract class MixinDamageSource {

    @Shadow public String damageType;

    @Shadow public abstract boolean isProjectile();
    @Shadow public abstract boolean isUnblockable();
    @Shadow public abstract boolean canHarmInCreative();
    @Shadow public abstract boolean isDamageAbsolute();
    @Shadow public abstract boolean isMagicDamage();
    @Shadow public abstract float getHungerDamage();
    @Shadow public abstract net.minecraft.util.DamageSource setExplosion();
    @Shadow public abstract net.minecraft.util.DamageSource setProjectile();
    @Shadow public abstract boolean isDifficultyScaled();
    @Shadow public abstract boolean isExplosion();

    private DamageType apiDamageType;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onConstructed(String damageTypeIn, CallbackInfo ci) {
        if (!SpongeGameRegistry.damageSourceToTypeMappings.containsKey(damageTypeIn)) {
            SpongeGameRegistry.damageSourceToTypeMappings.put(damageTypeIn, DamageTypes.CUSTOM);
        }

        this.apiDamageType = SpongeGameRegistry.damageSourceToTypeMappings.get(damageTypeIn);
    }

    @Intrinsic
    public boolean damage$isExplosion() {
        return isExplosion();
    }

    public boolean isMagic() {
        return isMagicDamage();
    }

    public boolean isAbsolute() {
        return isDamageAbsolute();
    }

    public boolean isBypassingArmor() {
        return isUnblockable();
    }

    @Intrinsic
    public boolean damage$isDifficultyScaled() {
        return isDifficultyScaled();
    }

    public DamageType damage$getDamageType() {
        return this.apiDamageType;
    }

}
