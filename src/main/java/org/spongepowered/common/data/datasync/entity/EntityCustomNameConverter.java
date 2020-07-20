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
package org.spongepowered.common.data.datasync.entity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.entity.Entity;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.common.data.datasync.DataParameterConverter;
import org.spongepowered.common.accessor.entity.EntityAccessor;

import java.util.List;
import java.util.Optional;

public class EntityCustomNameConverter extends DataParameterConverter<String> {

    public EntityCustomNameConverter() {
        super(EntityAccessor.accessor$getCustomName());
    }

    @Override
    public Optional<DataTransactionResult> createTransaction(final Entity entity, final String currentValue, final String value) {
        final Component currentText = LegacyComponentSerializer.legacy().deserialize(currentValue);
        final Component newValue = LegacyComponentSerializer.legacy().deserialize(value);

        return Optional.of(DataTransactionResult.builder()
                .replace(Value.immutableOf(Keys.DISPLAY_NAME, currentText))
                .success(Value.immutableOf(Keys.DISPLAY_NAME, newValue))
                .result(DataTransactionResult.Type.SUCCESS)
                .build());
    }

    @Override
    public String getValueFromEvent(final String originalValue, final List<Immutable<?>> immutableValues) {
        for (final Immutable<?> value : immutableValues) {
            if (value.getKey() == Keys.DISPLAY_NAME.get()) {
                try {
                    return LegacyComponentSerializer.legacy().serialize((Component) value.get());
                } catch (Exception e) {
                    return originalValue;
                }
            }
        }
        return originalValue;
    }
}
