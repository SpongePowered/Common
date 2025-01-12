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
package org.spongepowered.common.mixin.core.world.item.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.TransmuteResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.world.item.crafting.SmithingRecipeBridge;

import java.util.Optional;

@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingTransformRecipeMixin implements SmithingRecipeBridge {

    // @formatter=off
    @Shadow @Final Optional<Ingredient> template;
    @Shadow @Final Optional<Ingredient> base;
    @Shadow @Final Optional<Ingredient> addition;
    @Shadow @Final TransmuteResult result;
    // @formatter=on

    @Override
    public Optional<Ingredient> bridge$template() {
        return this.template;
    }

    @Override
    public Optional<Ingredient> bridge$base() {
        return this.base;
    }

    @Override
    public Optional<Ingredient> bridge$addition() {
        return this.addition;
    }

    @Override
    public ItemStack bridge$result() {
        return new ItemStack(this.result.item(), this.result.count(), this.result.components());
    }

}
