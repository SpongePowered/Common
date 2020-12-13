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
package org.spongepowered.common.world.schematic;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.spongepowered.api.world.schematic.Palette;
import org.spongepowered.api.world.schematic.PaletteType;

import java.util.BitSet;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

public class MutableBimapPalette<T> implements Palette.Mutable<T> {

    private static final int DEFAULT_ALLOCATION_SIZE = 64;

    private final BiMap<Integer, T> ids;
    private final BiMap<T, Integer> idsr;
    private final BitSet allocation = new BitSet(MutableBimapPalette.DEFAULT_ALLOCATION_SIZE);
    private final PaletteType<T> paletteType;
    private int maxId = 0;

    public MutableBimapPalette(final PaletteType<T> paletteType) {
        this.ids = HashBiMap.create();
        this.idsr = this.ids.inverse();
        this.paletteType = paletteType;
    }

    public MutableBimapPalette(final PaletteType<T> paletteType, final BiMap<T, Integer> reference) {
        this.ids = HashBiMap.create(reference.size());
        this.idsr = this.ids.inverse();
        this.paletteType = paletteType;
        reference.forEach((key, id) -> this.getOrAssign(key));
    }

    public MutableBimapPalette(final PaletteType<T> paletteType, final int expectedSize) {
        this.ids = HashBiMap.create(expectedSize);
        this.idsr = this.ids.inverse();
        this.paletteType = paletteType;
    }

    @Override
    public PaletteType<T> getType() {
        return this.paletteType;
    }

    @Override
    public int getHighestId() {
        return this.maxId;
    }

    @Override
    public OptionalInt get(final T state) {
        final Integer value = this.idsr.get(state);
        if (value == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(value);
    }

    @Override
    public int getOrAssign(final T state) {
        final Integer id = this.idsr.get(state);
        if (id == null) {
            final int next = this.allocation.nextClearBit(0);
            if (this.maxId < next) {
                this.maxId = next;
            }
            this.allocation.set(next);
            this.ids.put(next, state);
            return next;
        }
        return id;
    }

    @Override
    public Optional<T> get(final int id) {
        return Optional.ofNullable(this.ids.get(id));
    }

    public void assign(final T state, final int id) {
        if (this.maxId < id) {
            this.maxId = id;
        }
        this.allocation.set(id);
        this.ids.put(id, state);
    }

    @Override
    public boolean remove(final T state) {
        final Integer id = this.idsr.get(state);
        if (id == null) {
            return false;
        }
        this.allocation.clear(id);
        if (id == this.maxId) {
            this.maxId = this.allocation.previousSetBit(this.maxId);
        }
        this.ids.remove(id);
        return true;
    }

    @Override
    public Stream<T> stream() {
        return this.idsr.keySet().stream();
    }

    @Override
    public Immutable<T> asImmutable() {
        return new ImmutableBimapPalette<>(this.paletteType, this.ids);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final MutableBimapPalette<?> that = (MutableBimapPalette<?>) o;
        return this.maxId == that.maxId &&
               this.ids.equals(that.ids) &&
               this.allocation.equals(that.allocation) &&
               this.paletteType.equals(that.paletteType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.ids, this.allocation, this.paletteType, this.maxId);
    }
}
