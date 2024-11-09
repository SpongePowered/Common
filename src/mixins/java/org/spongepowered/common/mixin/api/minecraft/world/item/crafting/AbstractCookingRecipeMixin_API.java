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
package org.spongepowered.common.mixin.api.minecraft.world.item.crafting;

import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import org.spongepowered.api.item.inventory.ItemStackLike;
import org.spongepowered.api.item.recipe.cooking.CookingRecipe;
import org.spongepowered.api.item.recipe.cooking.CookingResult;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.item.recipe.ingredient.IngredientUtil;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.SpongeTicks;

import java.util.Optional;

@Mixin(AbstractCookingRecipe.class)
public abstract class AbstractCookingRecipeMixin_API extends SingleItemRecipeMixin_API implements CookingRecipe {

    // @formatter:off
    @Shadow public abstract float shadow$experience();
    @Shadow public abstract int shadow$cookingTime();
    // @formatter:on

    @Override
    public org.spongepowered.api.item.recipe.crafting.Ingredient ingredient() {
        return IngredientUtil.fromNative(this.shadow$input());
    }

    @Override
    public boolean isValid(final ItemStackLike ingredient) {
        return this.shadow$input().test(ItemStackUtil.fromLikeToNative(ingredient));
    }

    @Override
    public Optional<CookingResult> result(final ItemStackLike ingredient) {
        if (this.isValid(ingredient)) {
            return Optional.of(new CookingResult((ItemStackLike) this.exemplaryResult(), this.shadow$experience()));
        }
        return Optional.empty();
    }

    @Override
    public Ticks cookingTime() {
        return SpongeTicks.ticksOrInfinite(this.shadow$cookingTime());
    }
}
