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
package org.spongepowered.common.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.util.CatalogBuilder;
import org.spongepowered.api.util.ResettableBuilder;

import java.util.Objects;

@SuppressWarnings("unchecked")
@DefaultQualifier(NonNull.class)
public abstract class SpongeCatalogBuilder<C extends CatalogType, B extends ResettableBuilder<C, B>>
        implements CatalogBuilder<C, B> {

    @Nullable protected ResourceKey key;

    @Override
    public B key(final ResourceKey key) {
        Objects.requireNonNull(key);
        if (key.getNamespace().isEmpty()) {
            throw new IllegalArgumentException("The key namespace cannot be empty!");
        }
        if (key.getValue().isEmpty()) {
            throw new IllegalArgumentException("The key value cannot be empty!");
        }
        this.key = key;
        return (B) this;
    }

    @Override
    public C build() {
        Objects.requireNonNull(this.key);
        return this.build(this.key);
    }

    protected abstract C build(ResourceKey key);

    @Override
    public B reset() {
        this.key = null;
        return (B) this;
    }
}
