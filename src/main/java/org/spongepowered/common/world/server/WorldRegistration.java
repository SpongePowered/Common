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
package org.spongepowered.common.world.server;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.dimension.DimensionType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;

import java.util.Objects;

public final class WorldRegistration {

    private final ResourceKey key;
    @Nullable private DimensionType dimensionType;
    @Nullable private WorldSettings defaultSettings;

    public WorldRegistration(final ResourceKey key, @Nullable final DimensionType dimensionType, @Nullable final WorldSettings defaultSettings) {
        this.key = Objects.requireNonNull(key);
        this.dimensionType = dimensionType;
        this.defaultSettings = defaultSettings;
    }

    public ResourceKey getKey() {
        return this.key;
    }

    @Nullable
    public DimensionType getDimensionType() {
        return this.dimensionType;
    }

    @Nullable
    public WorldSettings getDefaultSettings() {
        return this.defaultSettings;
    }

    public void setDimensionType(@Nullable final DimensionType dimensionType) {
        this.dimensionType = dimensionType;
    }

    public void setDimensionType(@Nullable final WorldSettings defaultSettings) {
        this.defaultSettings = defaultSettings;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }
        final WorldRegistration that = (WorldRegistration) o;
        return this.key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("key", this.key)
            .add("dimensionType", this.dimensionType)
            .toString();
    }
}
