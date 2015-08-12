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
package org.spongepowered.common.data.key;

import static org.spongepowered.api.data.DataQuery.of;

import com.google.common.collect.MapMaker;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.Career;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Axis;
import org.spongepowered.common.data.key.SpongeKey;
import org.spongepowered.common.registry.RegistryHelper;

import java.awt.Color;
import java.util.Map;

@SuppressWarnings({"unchecked"})
public class KeyRegistry {

    private static final Map<String, Key<?>> keyMap = new MapMaker().concurrencyLevel(4).makeMap();

    public static void registerKeys() {
        keyMap.put("axis", makeKey(Axis.class, Value.class, of("Axis")));
        keyMap.put("color", makeKey(Color.class, Value.class, of("Color")));
        keyMap.put("health", makeKey(Double.class, MutableBoundedValue.class, of("Health")));
        keyMap.put("max_health", makeKey(Double.class, MutableBoundedValue.class, of("MaxHealth")));
        keyMap.put("shows_display_name", makeKey(Boolean.class, Value.class, of("ShowDisplayName")));
        keyMap.put("display_name", makeKey(Text.class, Value.class, of("DisplayName")));
        keyMap.put("career", makeKey(Career.class, Value.class, of("Career")));
        RegistryHelper.mapFields(Keys.class, keyMap);
    }

    private static <E, V extends BaseValue<E>> Key<V> makeKey(final Class<E> elementClass, final Class<V> valueClass,
            final DataQuery query) {
        return new SpongeKey<E, V>(elementClass, valueClass, query);
    }

}
