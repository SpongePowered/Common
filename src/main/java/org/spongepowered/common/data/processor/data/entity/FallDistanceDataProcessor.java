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
package org.spongepowered.common.data.processor.data.entity;

import com.google.common.base.Preconditions;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataTransactionBuilder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableFallDistanceData;
import org.spongepowered.api.data.manipulator.mutable.entity.FallDistanceData;
import org.spongepowered.api.data.value.immutable.ImmutableBoundedValue;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongeFallDistanceData;
import org.spongepowered.common.data.processor.common.AbstractEntitySingleDataProcessor;
import org.spongepowered.common.data.value.SpongeValueBuilder;

import java.util.Optional;

public class FallDistanceDataProcessor extends AbstractEntitySingleDataProcessor<EntityLivingBase, Float, MutableBoundedValue<Float>,
        FallDistanceData, ImmutableFallDistanceData> {

    public FallDistanceDataProcessor() {
        super(EntityLivingBase.class, Keys.FALL_DISTANCE);
    }

    @Override
    protected boolean set(EntityLivingBase entity, Float value) {
        entity.fallDistance = Preconditions.checkNotNull(value);
        return true;
    }

    @Override
    protected Optional<Float> getVal(EntityLivingBase entity) {
        return Optional.of(entity.fallDistance);
    }

    @Override
    protected ImmutableBoundedValue<Float> constructImmutableValue(Float value) {
        return SpongeValueBuilder.boundedBuilder(Keys.FALL_DISTANCE)
                .actualValue(value)
                .defaultValue(0F)
                .minimum(0F)
                .maximum(Float.MAX_VALUE)
                .build()
                .asImmutable();
    }

    @Override
    protected FallDistanceData createManipulator() {
        return new SpongeFallDistanceData();
    }

    @Override
    public DataTransactionResult remove(DataHolder dataHolder) {
        return DataTransactionBuilder.failNoData();
    }
}
