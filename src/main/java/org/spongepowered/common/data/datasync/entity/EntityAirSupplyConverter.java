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

import net.minecraft.world.entity.Entity;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.common.accessor.world.entity.EntityAccessor;
import org.spongepowered.common.data.datasync.DataParameterConverter;

import java.util.Optional;

public class EntityAirSupplyConverter extends DataParameterConverter<Integer> {

    public EntityAirSupplyConverter() {
        super(EntityAccessor.accessor$DATA_AIR_SUPPLY_ID());
    }

    @Override
    public Optional<DataTransactionResult> createTransaction(Entity entity, Integer currentValue, Integer value) {
        return Optional.of(DataTransactionResult.builder()
                .replace(Value.immutableOf(Keys.REMAINING_AIR, currentValue))
                .success(Value.immutableOf(Keys.REMAINING_AIR, value))
                .result(DataTransactionResult.Type.SUCCESS)
                .build());
    }

    @Override
    public Integer getValueFromEvent(Integer originalValue, DataTransactionResult result) {
        return result.successfulValue(Keys.REMAINING_AIR)
                .map(Value::get)
                .orElse(originalValue);
    }
}
