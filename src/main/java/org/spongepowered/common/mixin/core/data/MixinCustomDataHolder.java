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
package org.spongepowered.common.mixin.core.data;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.api.data.DataTransactionBuilder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.interfaces.data.IMixinCustomDataHolder;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

// TODO don't enable this until we want to enable custom data! Also, needs a lot of testing
@Mixin({TileEntity.class, Entity.class})
public abstract class MixinCustomDataHolder implements IMixinCustomDataHolder {

    private List<DataManipulator<?, ?>> manipulators = Lists.newArrayList();

    @Override
    public DataTransactionResult offerCustom(DataManipulator<?, ?> manipulator, MergeFunction function) {
        @Nullable DataManipulator<?, ?> existingManipulator = null;
        for (DataManipulator<?, ?> existing : this.manipulators) {
            if (manipulator.getClass().isInstance(existing)) {
                existingManipulator = existing;
                break;
            }
        }
        final DataTransactionBuilder builder = DataTransactionBuilder.builder();
        final DataManipulator<?, ?> newManipulator = checkNotNull(function.merge(existingManipulator, (DataManipulator) manipulator.copy()));
        if (existingManipulator != null) {
            builder.replace(existingManipulator.getValues());
            this.manipulators.remove(existingManipulator);
        }
        this.manipulators.add(newManipulator);
        return builder.success(newManipulator.getValues())
            .result(DataTransactionResult.Type.SUCCESS)
            .build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DataManipulator<?, ?>> Optional<T> getCustom(Class<T> customClass) {
        for (DataManipulator<?, ?> existing : this.manipulators) {
            if (customClass.isInstance(existing)) {
                return Optional.of((T) existing);
            }
        }
        return Optional.absent();
    }

    @Override
    public DataTransactionResult removeCustom(Class<? extends DataManipulator<?, ?>> customClass) {
        @Nullable DataManipulator<?, ?> manipulator = null;
        for (DataManipulator<?, ?> existing : this.manipulators) {
            if (customClass.isInstance(existing)) {
                manipulator = existing;
            }
        }
        if (manipulator != null) {
            this.manipulators.remove(manipulator);
            return DataTransactionBuilder.builder().replace(manipulator.getValues()).result(DataTransactionResult.Type.SUCCESS).build();
        } else {
            return DataTransactionBuilder.failNoData();
        }
    }

    @Override
    public boolean hasManipulators() {
        return !this.manipulators.isEmpty();
    }

    @Override
    public List<DataManipulator<?, ?>> getCustomManipulators() {
        final List<DataManipulator<?, ?>> list = Lists.newArrayList();
        for (DataManipulator<?, ?> manipulator : this.manipulators) {
            list.add(manipulator.copy());
        }
        return list;
    }

    @Override
    public <E> DataTransactionResult offerCustom(Key<? extends BaseValue<E>> key, E value) {
        for (DataManipulator<?, ?> manipulator : this.manipulators) {
            if (manipulator.supports(key)) {
                final DataTransactionBuilder builder = DataTransactionBuilder.builder();
                builder.replace(((Value) manipulator.getValue((Key) key).get()).asImmutable());
                manipulator.set(key, value);
                builder.success(((Value) manipulator.getValue((Key) key).get()).asImmutable());
                return builder.result(DataTransactionResult.Type.SUCCESS).build();
            }
        }
        return DataTransactionBuilder.failNoData();
    }

    @Override
    public DataTransactionResult removeCustom(Key<?> key) {
        final Iterator<DataManipulator<?, ?>> iterator = this.manipulators.iterator();
        while (iterator.hasNext()) {
            final DataManipulator<?, ?> manipulator = iterator.next();
            if (manipulator.getKeys().size() == 1 && manipulator.supports(key)) {
                iterator.remove();
                return DataTransactionBuilder.builder()
                    .replace(manipulator.getValues())
                    .result(DataTransactionResult.Type.SUCCESS)
                    .build();
            }
        }
        return DataTransactionBuilder.failNoData();
    }

}
