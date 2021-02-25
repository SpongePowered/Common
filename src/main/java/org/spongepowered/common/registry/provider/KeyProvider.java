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
package org.spongepowered.common.registry.provider;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;

import java.util.Map;
import java.util.Optional;

public final class KeyProvider {

    public static final KeyProvider INSTANCE = new KeyProvider();

    private final Map<ResourceKey, Key<Value<?>>> mappings;

    KeyProvider() {
        this.mappings = new Object2ObjectOpenHashMap<>();
    }

    public void register(final ResourceKey resourceKey, final Key<Value<?>> key) {
        this.mappings.put(resourceKey, key);
    }

    public <T extends Value<?>> Optional<Key<@NonNull T>> get(final ResourceKey resourceKey) {
        return (Optional<Key<T>>) (Object) Optional.ofNullable(this.mappings.get(resourceKey));
    }
}
