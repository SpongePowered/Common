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
package org.spongepowered.common.data.processor.value.item;

import com.google.common.base.Optional;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.spongepowered.api.data.DataTransactionBuilder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.GoldenApple;
import org.spongepowered.api.data.type.GoldenApples;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.common.data.ImmutableDataCachingUtil;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.processor.common.GoldenAppleUtils;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

public class GoldenAppleValueProcessor extends AbstractSpongeValueProcessor<GoldenApple, Value<GoldenApple>> {

    public GoldenAppleValueProcessor() {
        super(Keys.GOLDEN_APPLE_TYPE);
    }

    @Override
    protected Value<GoldenApple> constructValue(GoldenApple defaultValue) {
        return new SpongeValue<GoldenApple>(Keys.GOLDEN_APPLE_TYPE, defaultValue);
    }

    @Override
    public Optional<GoldenApple> getValueFromContainer(ValueContainer<?> container) {
        if (this.supports(container)) {
            return Optional.of(GoldenAppleUtils.getType((ItemStack) container));
        }
        return Optional.absent();
    }

    @Override
    public boolean supports(ValueContainer<?> container) {
        return container instanceof ItemStack && ((ItemStack) container).getItem().equals(Items.golden_apple);
    }

    @Override
    public DataTransactionResult offerToStore(ValueContainer<?> container, GoldenApple value) {
        if (this.supports(container)) {
            GoldenApple old = this.getValueFromContainer(container).get();
            final ImmutableValue<GoldenApple> oldValue = ImmutableDataCachingUtil.getValue(ImmutableSpongeValue.class, Keys.GOLDEN_APPLE_TYPE, old,
                    GoldenApples.GOLDEN_APPLE);
            final ImmutableValue<GoldenApple> newValue = ImmutableDataCachingUtil.getValue(ImmutableSpongeValue.class, Keys.GOLDEN_APPLE_TYPE, value,
                    GoldenApples.GOLDEN_APPLE);

            GoldenAppleUtils.setType((ItemStack) container, value);
            return DataTransactionBuilder.successReplaceResult(newValue, oldValue);
        }
        return DataTransactionBuilder.failResult(ImmutableDataCachingUtil.getValue(ImmutableSpongeValue.class, Keys.GOLDEN_APPLE_TYPE, value, GoldenApples.GOLDEN_APPLE));
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionBuilder.failNoData();
    }
}
