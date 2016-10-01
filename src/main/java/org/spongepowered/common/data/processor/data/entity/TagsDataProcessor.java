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

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableTagsData;
import org.spongepowered.api.data.manipulator.mutable.entity.TagsData;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.SetValue;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongeTagsData;
import org.spongepowered.common.data.processor.common.AbstractEntitySingleDataProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeSetValue;
import org.spongepowered.common.data.value.mutable.SpongeSetValue;

import net.minecraft.entity.Entity;

public class TagsDataProcessor extends AbstractEntitySingleDataProcessor<Entity, Set<String>, SetValue<String>, TagsData, ImmutableTagsData> {

    public TagsDataProcessor() {
        super(Entity.class, Keys.TAGS);
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }

    @Override
    protected boolean set(Entity dataHolder, Set<String> value) {
        for (String oldTag : dataHolder.getTags()) {
            dataHolder.removeTag(oldTag);
        }

        for (String newTag : value) {
            if (!dataHolder.addTag(newTag)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Optional<Set<String>> getVal(Entity dataHolder) {
        return Optional.of(new HashSet<String>(dataHolder.getTags()));
    }

    @Override
    protected ImmutableValue<Set<String>> constructImmutableValue(Set<String> value) {
        return ImmutableSpongeSetValue.cachedOf(Keys.TAGS, Collections.emptySet(), value);
    }

    @Override
    protected SetValue<String> constructValue(Set<String> actualValue) {
        return new SpongeSetValue<>(Keys.TAGS, Collections.emptySet(), actualValue);
    }

    @Override
    protected TagsData createManipulator() {
        return new SpongeTagsData();
    }

}
