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

import static org.spongepowered.common.data.util.DataUtil.getData;

import java.util.Map;

import net.minecraft.entity.Entity;

import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataTransactionBuilder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableIgniteableData;
import org.spongepowered.api.data.manipulator.mutable.entity.IgniteableData;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongeIgniteableData;
import org.spongepowered.common.data.processor.common.AbstractEntityDataProcessor;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public class IgniteableDataProcessor extends AbstractEntityDataProcessor<Entity, IgniteableData, ImmutableIgniteableData> {

	public IgniteableDataProcessor() {
        super(Entity.class);
    }

	@Override
	public Optional<IgniteableData> fill(DataContainer container, IgniteableData igniteableData) {
		igniteableData.set(Keys.FIRE_TICKS, getData(container, Keys.FIRE_TICKS));
		igniteableData.set(Keys.FIRE_DAMAGE_DELAY, getData(container, Keys.FIRE_DAMAGE_DELAY));
		return Optional.of(igniteableData);
	}

	@Override
	public DataTransactionResult remove(DataHolder dataHolder) {
		return DataTransactionBuilder.failNoData();
	}

    @Override
    protected IgniteableData createManipulator() {
        return new SpongeIgniteableData(0, 20);
    }

    @Override
    protected boolean doesDataExist(Entity entity) {
        return true;
    }

    @Override
    protected boolean set(Entity entity, Map<Key<?>, Object> keyValues) {
        entity.fire = (Integer) keyValues.get(Keys.FIRE_TICKS);
        entity.fire = (Integer) keyValues.get(Keys.FIRE_DAMAGE_DELAY);
        return true;
    }

    @Override
    protected Map<Key<?>, ?> getValues(Entity entity) {
        final int fireTicks = entity.fire;
        final int fireDamageDelay = entity.fireResistance;
        return ImmutableMap.<Key<?>, Object>of(Keys.FIRE_TICKS, fireTicks,
                                               Keys.FIRE_DAMAGE_DELAY, fireDamageDelay);
    }

}
