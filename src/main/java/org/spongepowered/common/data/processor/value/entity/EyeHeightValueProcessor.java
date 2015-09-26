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
package org.spongepowered.common.data.processor.value.entity;

import com.google.common.base.Optional;
import net.minecraft.entity.Entity;
import org.spongepowered.api.data.DataTransactionBuilder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import org.spongepowered.common.interfaces.entity.IMixinEntity;

@SuppressWarnings("ConstantConditions")
public class EyeHeightValueProcessor extends AbstractSpongeValueProcessor<Double, Value<Double>> {

    public EyeHeightValueProcessor() {
        super(Keys.EYE_HEIGHT);
    }

    @Override
    public boolean supports(ValueContainer<?> container) {
        return container instanceof Entity;
    }

    @Override
    public Value<Double> constructValue(Double defaultValue) {
        return new SpongeValue<Double>(Keys.EYE_HEIGHT, defaultValue);
    }

    @Override
    public Optional<Double> getValueFromContainer(ValueContainer<?> container) {
        if (supports(container)) {
            final Entity entity = (Entity) container;
            return Optional.of((double) entity.getEyeHeight());
        }
        return Optional.absent();
    }

    @Override
    public DataTransactionResult offerToStore(ValueContainer<?> container, Double value) {
        if (supports(container)) {
            final Entity entity = (Entity) container;
            final Double oldValue = (double) entity.getEyeHeight();
            ((IMixinEntity) entity).setEyeHeight(value);
            return DataTransactionBuilder.successReplaceResult(new ImmutableSpongeValue<Double>(Keys.EYE_HEIGHT, value),
                new ImmutableSpongeValue<Double>(Keys.EYE_HEIGHT, oldValue));
        }
        return DataTransactionBuilder.failResult(new ImmutableSpongeValue<Double>(Keys.EYE_HEIGHT, value));
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionBuilder.failNoData();
    }

}
