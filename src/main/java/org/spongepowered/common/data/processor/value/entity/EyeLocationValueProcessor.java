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
public class EyeLocationValueProcessor extends AbstractSpongeValueProcessor<Vector3d, Value<Vector3d>> {

    public EyeLocationValueProcessor() {
        super(Keys.EYE_LOCATION);
    }

    @Override
    public boolean supports(ValueContainer<?> container) {
        return container instanceof Entity;
    }

    @Override
    public Value<Vector3d> constructValue(Vector3d defaultValue) {
        return new SpongeValue<Vector3d>(Keys.EYE_LOCATION, defaultValue);
    }

    @Override
    public Optional<Vector3d> getValueFromContainer(ValueContainer<?> container) {
        if (supports(container)) {
            final Entity entity = (Entity) container;
            return Optional.of(new Vector3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ));
        }
        return Optional.absent();
    }

    @Override
    public DataTransactionResult offerToStore(ValueContainer<?> container, Vector3d value) {
        if (supports(container)) {
            final Entity entity = (Entity) container;
            final Vector3d oldValue = new Vector3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
            ((IMixinEntity) entity).setEyeHeight(value.getY() - oldValue.getY());
            return DataTransactionBuilder.successReplaceResult(new ImmutableSpongeValue<Vector3d>(Keys.EYE_LOCATION, value),
                new ImmutableSpongeValue<Vector3d>(Keys.EYE_LOCATION, oldValue));
        }
        return DataTransactionBuilder.failResult(new ImmutableSpongeValue<Vector3d>(Keys.EYE_LOCATION, value));
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionBuilder.failNoData();
    }

}
