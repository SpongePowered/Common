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
package org.spongepowered.common.map.color;

import net.minecraft.block.material.MaterialColor;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.map.color.MapColorType;
import org.spongepowered.api.util.Color;

import java.util.Objects;

public class SpongeMapColorType implements MapColorType {
    private final ResourceKey resourceKey;
    private final int colorIndex;


    public SpongeMapColorType(ResourceKey resourceKey, int colorIndex) {
        this.resourceKey = resourceKey;
        this.colorIndex = colorIndex;
    }

    public int getColorIndex() {
        return this.colorIndex;
    }

    @Override
    public Color getColor() {
        return Color.of(new java.awt.Color(MaterialColor.COLORS[colorIndex].colorValue));
    }

    @Override
    public ResourceKey getKey() {
        return this.resourceKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpongeMapColorType that = (SpongeMapColorType) o;
        return colorIndex == that.colorIndex &&
                resourceKey.equals(that.resourceKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceKey, colorIndex);
    }
}