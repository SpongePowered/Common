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

import com.flowpowered.math.vector.Vector3d;
import net.minecraft.entity.Entity;
import org.spongepowered.api.data.DataTransactionBuilder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;

public class VelocityValueProcessor extends AbstractSpongeValueProcessor<Vector3d, Value<Vector3d>> {

    public VelocityValueProcessor() {
        super(Keys.VELOCITY);
    }

    @Override
    public Value<Vector3d> constructValue(Vector3d defaultValue) {
        return new SpongeValue<>(Keys.VELOCITY, Vector3d.ZERO, defaultValue);
    }

    @Override
    public Optional<Vector3d> getValueFromContainer(ValueContainer<?> container) {
        if (container instanceof Entity) {
            return Optional.of(new Vector3d(((Entity) container).motionX, ((Entity) container).motionY, ((Entity) container).motionZ));
        }
        return Optional.empty();
    }

    @Override
    public boolean supports(ValueContainer<?> container) {
        return container instanceof Entity;
    }

    @Override
    public DataTransactionResult offerToStore(ValueContainer<?> container, Vector3d value) {
        final ImmutableValue<Vector3d> newValue = new ImmutableSpongeValue<>(Keys.VELOCITY, Vector3d.ZERO, value);
        if (container instanceof Entity) {
            final Vector3d old = getValueFromContainer(container).get();
            final ImmutableValue<Vector3d> oldValue = new ImmutableSpongeValue<>(Keys.VELOCITY, Vector3d.ZERO, old);
            try {
                ((Entity) container).motionX = value.getX();
                ((Entity) container).motionY = value.getY();
                ((Entity) container).motionZ = value.getZ();
                return DataTransactionBuilder.successReplaceResult(newValue, oldValue);
            } catch (Exception e) {
                DataTransactionBuilder.errorResult(newValue);
            }
        }
        return DataTransactionBuilder.failResult(newValue);
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionBuilder.failNoData();
    }
}
