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
package org.spongepowered.common.mixin.core.item;

import com.google.common.base.Objects;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.api.item.Enchantment;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextRepresentable;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.text.SpongeTexts;
import org.spongepowered.common.text.translation.SpongeTranslation;

@NonnullByDefault
@Mixin(net.minecraft.enchantment.Enchantment.class)
@Implements(value = @Interface(iface = Enchantment.class, prefix = "enchantment$") )
public abstract class MixinEnchantment implements Enchantment, TextRepresentable {

    @Shadow protected String name;
    @Shadow private int weight;

    @Shadow public abstract String getName();
    @Shadow public abstract int getMinLevel();
    @Shadow public abstract int getMaxLevel();
    @Shadow public abstract int getMinEnchantability(int level);
    @Shadow public abstract int getMaxEnchantability(int level);
    @Shadow public abstract boolean canApplyTogether(net.minecraft.enchantment.Enchantment ench);
    @Shadow(prefix = "minecraft$") public abstract String minecraft$getName();

    private String id = "";
    private Translation translation;

    @Inject(method = "<init>", at = @At("RETURN") )
    public void onConstructed(int id, ResourceLocation resLoc, int weight, EnumEnchantmentType type, CallbackInfo ci) {
        this.id = resLoc.toString();
        this.translation = new SpongeTranslation(getName());
    }

    @Inject(method = "setName", at = @At("RETURN") )
    public void setName(String name, CallbackInfoReturnable<Enchantment> cir) {
        this.translation = new SpongeTranslation(getName());
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String enchantment$getName() {
        return getName();
    }

    @Override
    public int getWeight() {
        return this.weight;
    }

    @Override
    public int getMinimumLevel() {
        return getMinLevel();
    }

    @Override
    public int getMaximumLevel() {
        return getMaxLevel();
    }

    @Override
    public int getMinimumEnchantabilityForLevel(int level) {
        return getMinEnchantability(level);
    }

    @Override
    public int getMaximumEnchantabilityForLevel(int level) {
        return getMaxEnchantability(level);
    }

    @Override
    public boolean canBeAppliedByTable(ItemStack stack) {
        return canBeAppliedToStack(stack);
    }

    @Override
    public boolean isCompatibleWith(Enchantment ench) {
        return canApplyTogether((net.minecraft.enchantment.Enchantment) ench);
    }

    @Intrinsic
    public String shadow$getName() {
        return minecraft$getName();
    }

    @Override
    public Translation getTranslation() {
        return translation;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper("Enchantment")
                .add("id", getId())
                .add("name", shadow$getName())
                .toString();
    }

    @Override
    public Text toText() {
        return SpongeTexts.toText(this);
    }

}
