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
package org.spongepowered.common.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.spongepowered.api.data.DataPerspectiveResolver;
import org.spongepowered.api.data.DataProvider;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.DuplicateDataStoreException;
import org.spongepowered.api.data.DuplicateProviderException;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class SpongeDataRegistrationBuilder implements DataRegistration.Builder {

    Multimap<Key, DataProvider> dataProviderMap = HashMultimap.create();
    Map<Type, DataStore> dataStoreMap = new HashMap<>();
    Map<Key, DataPerspectiveResolver> dataPerspectiveResolverMap = new HashMap<>();
    List<Key<?>> keys = new ArrayList<>();

    @Override
    public DataRegistration.Builder store(final DataStore store) throws DuplicateDataStoreException {
        for (final Type holderType : store.supportedTypes()) {
            this.dataStoreMap.put(holderType, store);
        }
        return this;
    }

    @Override
    public DataRegistration.Builder provider(final DataProvider<?, ?> provider) throws DuplicateProviderException {
        this.dataProviderMap.put(provider.key(), provider);
        return this;
    }

    @Override
    public DataRegistration.Builder perspectiveResolver(DataPerspectiveResolver<?, ?> resolver) {
        this.dataPerspectiveResolverMap.put(resolver.key(), resolver);
        return this;
    }

    @Override
    public DataRegistration.Builder dataKey(final Key<?> key) {
        this.keys.add(key);
        return this;
    }

    @Override
    public DataRegistration.Builder dataKey(final Key<?> key, final Key<?>... others) {
        this.keys.add(key);
        Collections.addAll(this.keys, others);
        return this;
    }

    @Override
    public DataRegistration.Builder dataKey(final Iterable<Key<?>> keys) {
        keys.forEach(this.keys::add);
        return this;
    }

    @Override
    public DataRegistration build() {
        return new SpongeDataRegistration(this);
    }

    @Override
    public SpongeDataRegistrationBuilder reset() {
        this.dataProviderMap = HashMultimap.create();
        this.dataStoreMap = new IdentityHashMap<>();
        this.dataPerspectiveResolverMap = new HashMap<>();
        this.keys = new ArrayList<>();
        return this;
    }
}
