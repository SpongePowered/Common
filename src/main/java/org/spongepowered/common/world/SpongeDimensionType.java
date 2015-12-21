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
package org.spongepowered.common.world;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import net.minecraft.world.WorldProvider;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextRepresentable;
import org.spongepowered.api.world.Dimension;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.common.text.SpongeTexts;

public class SpongeDimensionType implements DimensionType, TextRepresentable {

    private String name;
    private int dimensionTypeId;
    private boolean keepLoaded;
    private Class<? extends WorldProvider> dimensionClass;

    public SpongeDimensionType(String name, boolean keepLoaded, Class<? extends WorldProvider> dimensionClass, int id) {
        this.name = checkNotNull(name);
        this.keepLoaded = keepLoaded;
        this.dimensionClass = checkNotNull(dimensionClass, "The dimension class was null! The name was: " + name);
        this.dimensionTypeId = id;
    }

    @Override
    public boolean doesKeepSpawnLoaded() {
        return this.keepLoaded;
    }

    @Override
    public String getId() {
        return this.name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends Dimension> getDimensionClass() {
        return (Class<? extends Dimension>) this.dimensionClass;
    }

    public int getDimensionTypeId() {
        return this.dimensionTypeId;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", this.name)
                .add("keepLoaded", this.keepLoaded)
                .add("class", this.dimensionClass.getName())
                .toString();
    }

    @Override
    public Text toText() {
        return SpongeTexts.toText(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DimensionType)) {
            return false;
        }

        DimensionType other = (DimensionType) obj;
        return this.name.equalsIgnoreCase(other.getName()) && this.dimensionClass.equals(other.getDimensionClass());

    }

    @Override
    public int hashCode() {
        return super.hashCode(); // todo this is a warning
    }
}
