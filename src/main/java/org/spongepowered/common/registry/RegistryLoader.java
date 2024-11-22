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
package org.spongepowered.common.registry;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.registry.RegistryKey;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RegistryLoader<T> extends InitialRegistryData<T> {

    private final Map<ResourceKey, T> values = new LinkedHashMap<>();
    private @MonotonicNonNull Map<ResourceKey, Integer> ids;

    private RegistryLoader() {
    }

    public static <T> RegistryLoader<T> of(final Consumer<RegistryLoader<T>> consumer) {
        final RegistryLoader<T> loader = new RegistryLoader<>();
        consumer.accept(loader);
        return loader;
    }

    public RegistryLoader<T> add(final RegistryKey<? extends T> key, final Supplier<? extends T> function) {
        this.values.put(key.location(), function.get());
        return this;
    }

    public RegistryLoader<T> add(final RegistryKey<? extends T> key, final Function<ResourceKey, ? extends T> function) {
        this.values.put(key.location(), function.apply(key.location()));
        return this;
    }


    public RegistryLoader<T> add(final ResourceKey key, final T value) {
        this.values.put(key, value);
        return this;
    }


    public RegistryLoader<T> addWithId(final int id, final RegistryKey<? extends T> key, final Supplier<? extends T> supplier) {
        return this.addWithId0(id, key, supplier.get());
    }

    public RegistryLoader<T> addWithId(final int id, final RegistryKey<? extends T> key, final Function<ResourceKey, ? extends T> function) {
        return this.addWithId0(id, key, function.apply(key.location()));
    }

    public <MC> RegistryLoader<T> addWithId(final MC mcType, final RegistryKey<? extends T> key, final Function<MC, ? extends T> function, final Function<MC, Integer> idFunction) {
        return this.addWithId0(idFunction.apply(mcType), key, function.apply(mcType));
    }

    private RegistryLoader<T> addWithId0(final int id, final RegistryKey<? extends T> key, final T value) {
        this.values.put(key.location(), value);
        if (this.ids == null) {
            this.ids = new HashMap<>();
        }
        this.ids.put(key.location(), id);
        return this;
    }

    public RegistryLoader<T> mapping(final Supplier<? extends T> supplier, final Consumer<Mapping<T>> consumer) {
        consumer.accept(new Mapping<T>() {
            @Override
            public Mapping<T> add(final RegistryKey<? extends T>... keys) {
                for (final RegistryKey<? extends T> key : keys) {
                    RegistryLoader.this.add(key, supplier);
                }
                return this;
            }
        });

        return this;
    }

    public RegistryLoader<T> mapping(final Function<ResourceKey, ? extends T> function, final Consumer<Mapping<T>> consumer) {
        consumer.accept(new Mapping<T>() {
            @Override
            public Mapping<T> add(final RegistryKey<? extends T>... keys) {
                for (final RegistryKey<? extends T> key : keys) {
                    RegistryLoader.this.add(key, function);
                }
                return this;
            }
        });

        return this;
    }

    public Map<ResourceKey, T> values() {
        return this.values;
    }

    @Override
    Map<ResourceKey, T> keyToValue() {
        return this.values;
    }

    @Override
    @Nullable Map<ResourceKey, Integer> keyToId() {
        return this.ids;
    }

    public interface Mapping<T> {
        Mapping<T> add(final RegistryKey<? extends T>... keys);
    }

}
