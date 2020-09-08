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
package org.spongepowered.common.data.manipulator;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import org.spongepowered.api.data.DataManipulator;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.MergeFunction;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.data.value.ValueContainer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ImmutableDataManipulatorFactory implements DataManipulator.Immutable.Factory {

    public static final DataManipulator.Immutable.Factory INSTANCE = new ImmutableDataManipulatorFactory();

    @Override
    public DataManipulator.Immutable of() {
        return new ImmutableDataManipulator(ImmutableMap.of());
    }

    @Override
    public DataManipulator.Immutable of(final Iterable<? extends Value<?>> values) {
        final Map<Key<?>, Object> mappedValues = MutableDataManipulatorFactory.mapValues(values);
        return new ImmutableDataManipulator(Collections.unmodifiableMap(mappedValues));
    }

    @Override
    public DataManipulator.Immutable of(final ValueContainer valueContainer) {
        checkNotNull(valueContainer, "valueContainer");
        if (valueContainer instanceof DataManipulator.Immutable) {
            return (DataManipulator.Immutable) valueContainer;
        }
        final Map<Key<?>, Object> values = new HashMap<>();
        MutableDataManipulator.copyFrom(values, valueContainer, MergeFunction.REPLACEMENT_PREFERRED);
        return new ImmutableDataManipulator(Collections.unmodifiableMap(values));
    }
}
