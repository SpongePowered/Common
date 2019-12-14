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
package org.spongepowered.common.mixin.core.village;

import net.minecraft.item.MerchantOffer;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackComparators;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin {

    @Shadow public abstract net.minecraft.item.ItemStack getBuyingStackFirst();
    @Shadow @Nullable public abstract net.minecraft.item.ItemStack getBuyingStackSecond();
    @Shadow public abstract net.minecraft.item.ItemStack getSellingStack();
    @Shadow public abstract int getUses();
    @Shadow public abstract boolean getDoesRewardExp();
    @Shadow public abstract int func_222214_i(); // getMaxUses

    @Shadow public abstract int getGivenExp();

    // This is a little questionable, since we're mixing into a Minecraft class.
    // However, Vanilla doesn't override equals(), so no one except plugins
    // should be calling it,
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        MerchantOffer other = (MerchantOffer) o;
        return ItemStackComparators.ALL.compare((ItemStack) this.getBuyingStackFirst(), (ItemStack) other.getBuyingStackFirst()) == 0
                && ItemStackComparators.ALL.compare((ItemStack) this.getBuyingStackSecond(), (ItemStack) other.getBuyingStackSecond()) == 0
                && ItemStackComparators.ALL.compare((ItemStack) this.getSellingStack(), (ItemStack) other.getSellingStack()) == 0
                && this.getUses() == other.getUses()
                && this.func_222214_i() == other.func_222214_i()
                && this.getGivenExp() == other.getGivenExp();
    }
}
