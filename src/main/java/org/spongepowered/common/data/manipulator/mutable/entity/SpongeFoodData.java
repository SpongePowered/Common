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
package org.spongepowered.common.data.manipulator.mutable.entity;

import static org.spongepowered.common.data.util.ComparatorUtil.doubleComparator;
import static org.spongepowered.common.data.util.ComparatorUtil.intComparator;

import com.google.common.collect.ComparisonChain;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableFoodData;
import org.spongepowered.api.data.manipulator.mutable.entity.FoodData;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.common.data.manipulator.immutable.entity.ImmutableSpongeFoodData;
import org.spongepowered.common.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.common.data.value.mutable.SpongeBoundedValue;

public class SpongeFoodData extends AbstractData<FoodData, ImmutableFoodData> implements FoodData {

    private int foodLevel;
    private float foodSaturationLevel;
    private float foodExhaustionLevel;

    public SpongeFoodData(int foodLevel, float foodSaturationLevel, float foodExhaustionLevel) {
        super(FoodData.class);
        this.foodLevel = foodLevel;
        this.foodSaturationLevel = foodSaturationLevel;
        this.foodExhaustionLevel = foodExhaustionLevel;
        registerGettersAndSetters();
    }

    public SpongeFoodData() {
        this(20, 5.0F, 0);
    }

    @Override
    public FoodData copy() {
        return new SpongeFoodData(this.foodLevel, this.foodSaturationLevel, this.foodExhaustionLevel);
    }

    @Override
    public ImmutableFoodData asImmutable() {
        return new ImmutableSpongeFoodData(this.foodLevel, this.foodSaturationLevel, this.foodExhaustionLevel);
    }

    @Override
    public int compareTo(FoodData o) {
        return ComparisonChain.start()
                .compare(o.foodLevel().get().intValue(), this.foodLevel)
                .compare(o.saturation().get().floatValue(), this.foodSaturationLevel)
                .compare(o.exhaustion().get().floatValue(), this.foodExhaustionLevel)
                .result();
    }

    @Override
    public DataContainer toContainer() {
        return new MemoryDataContainer()
                .set(Keys.FOOD_LEVEL.getQuery(), this.foodLevel)
                .set(Keys.SATURATION.getQuery(), this.foodSaturationLevel)
                .set(Keys.EXHAUSTION.getQuery(), this.foodExhaustionLevel);
    }

    @Override
    public MutableBoundedValue<Integer> foodLevel() {
        return new SpongeBoundedValue<>(Keys.FOOD_LEVEL, 20, intComparator(), 0, Integer.MAX_VALUE, this.foodLevel);
    }

    @Override
    public MutableBoundedValue<Double> exhaustion() {
        return new SpongeBoundedValue<>(Keys.EXHAUSTION, 0D, doubleComparator(), 0D, Double.MAX_VALUE, (double) this.foodExhaustionLevel);
    }

    @Override
    public MutableBoundedValue<Double> saturation() {
        return new SpongeBoundedValue<>(Keys.SATURATION, 5.0D, doubleComparator(), 0D, Double.MAX_VALUE,
                                        (double) this.foodSaturationLevel);
    }

    public int getFoodLevel() {
        return this.foodLevel;
    }

    public void setFoodLevel(int foodLevel) {
        this.foodLevel = foodLevel;
    }

    public double getFoodSaturation() {
        return this.foodSaturationLevel;
    }

    public void setFoodSaturation(double foodSaturationLevel) {
        this.foodSaturationLevel = (float) foodSaturationLevel;
    }

    public double getFoodExhaustion() {
        return this.foodExhaustionLevel;
    }

    public void setFoodExhaustion(double foodExhaustionLevel) {
        this.foodExhaustionLevel = (float) foodExhaustionLevel;
    }

    @Override
    protected void registerGettersAndSetters() {
        registerFieldGetter(Keys.FOOD_LEVEL, SpongeFoodData.this::getFoodLevel);
        registerFieldSetter(Keys.FOOD_LEVEL, SpongeFoodData.this::setFoodLevel);
        registerKeyValue(Keys.FOOD_LEVEL, SpongeFoodData.this::foodLevel);

        registerFieldGetter(Keys.SATURATION, SpongeFoodData.this::getFoodSaturation);
        registerFieldSetter(Keys.SATURATION, SpongeFoodData.this::setFoodSaturation);
        registerKeyValue(Keys.SATURATION, SpongeFoodData.this::saturation);

        registerFieldGetter(Keys.EXHAUSTION, SpongeFoodData.this::getFoodExhaustion);
        registerFieldSetter(Keys.EXHAUSTION, SpongeFoodData.this::setFoodExhaustion);
        registerKeyValue(Keys.EXHAUSTION, SpongeFoodData.this::exhaustion);
    }
}
