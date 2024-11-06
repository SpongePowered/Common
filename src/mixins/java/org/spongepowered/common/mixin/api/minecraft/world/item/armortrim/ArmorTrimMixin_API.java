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
package org.spongepowered.common.mixin.api.minecraft.world.item.armortrim;

import net.minecraft.core.Holder;
import net.minecraft.world.item.armortrim.ArmorTrim;
import org.spongepowered.api.item.recipe.smithing.TrimMaterial;
import org.spongepowered.api.item.recipe.smithing.TrimPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ArmorTrim.class)
public abstract class ArmorTrimMixin_API implements org.spongepowered.api.item.recipe.smithing.ArmorTrim {

    // @formatter:off
    @Shadow public abstract Holder<net.minecraft.world.item.armortrim.TrimMaterial> shadow$material();
    @Shadow public abstract Holder<net.minecraft.world.item.armortrim.TrimPattern> shadow$pattern();
    // @formatter:on

    @Override
    public TrimMaterial material() {
        return (TrimMaterial) this.shadow$material();
    }

    @Override
    public TrimPattern pattern() {
        return (TrimPattern) this.shadow$pattern();
    }
}
