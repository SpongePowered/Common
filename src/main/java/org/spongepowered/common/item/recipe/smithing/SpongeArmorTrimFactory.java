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
package org.spongepowered.common.item.recipe.smithing;

import net.minecraft.core.Registry;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.recipe.smithing.ArmorTrim;
import org.spongepowered.api.item.recipe.smithing.TrimMaterial;
import org.spongepowered.api.item.recipe.smithing.TrimPattern;
import org.spongepowered.api.registry.RegistryTypes;

public class SpongeArmorTrimFactory implements ArmorTrim.Factory {

    @Override
    public ArmorTrim create(TrimMaterial material, TrimPattern pattern) {

        final var trimRegistry = Sponge.server().registry(RegistryTypes.TRIM_MATERIAL);
        final var patternRegistry = Sponge.server().registry(RegistryTypes.TRIM_PATTERN);

        final var materialHolder = ((Registry<net.minecraft.world.item.armortrim.TrimMaterial>) trimRegistry).wrapAsHolder((net.minecraft.world.item.armortrim.TrimMaterial) (Object) material);
        final var patternHolder = ((Registry<net.minecraft.world.item.armortrim.TrimPattern>) patternRegistry).wrapAsHolder((net.minecraft.world.item.armortrim.TrimPattern) (Object) pattern);
        return (ArmorTrim) new net.minecraft.world.item.armortrim.ArmorTrim(materialHolder, patternHolder);
    }

}
