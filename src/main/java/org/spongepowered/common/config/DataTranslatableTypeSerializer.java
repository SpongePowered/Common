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
package org.spongepowered.common.config;

import io.leangen.geantyref.GenericTypeReflector;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.data.persistence.DataTranslator;
import org.spongepowered.common.data.persistence.ConfigurateTranslator;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

import javax.inject.Inject;

public final class DataTranslatableTypeSerializer implements TypeSerializer<Object> {

    private final DataManager dataManager;

    @Inject
    DataTranslatableTypeSerializer(final DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public boolean accepts(final Type x) {
        return this.dataManager.translator(GenericTypeReflector.erase(x)).isPresent();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object deserialize(final Type type, final ConfigurationNode value) throws SerializationException {
        final Class<Object> clazz = (Class<Object>) GenericTypeReflector.erase(type);
        final DataTranslator<Object> translator = this.dataManager.translator(clazz)
            .orElseThrow(() -> new SerializationException("Could not translate of DataTranslatable type: " + clazz.getName()));
        return translator.translate(ConfigurateTranslator.instance().translate(value));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void serialize(final Type type, final Object obj, final ConfigurationNode value) throws SerializationException {
        if (obj == null) {
            value.raw(null);
        } else {
            final Class<Object> clazz = (Class<Object>) GenericTypeReflector.erase(type);
            final DataTranslator<Object> translator = this.dataManager.translator(clazz)
                .orElseThrow(() -> new SerializationException("Could not translate of DataTranslatable type: " + clazz.getName()));
            ConfigurateTranslator.instance().translateDataToNode(value, translator.translate(obj));
        }
    }
}
