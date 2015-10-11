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
package org.spongepowered.common.data.processor.common;

import static com.google.common.base.Preconditions.checkNotNull;

import net.minecraft.item.ItemStack;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataTransactionBuilder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.common.Sponge;
import org.spongepowered.common.data.util.DataUtil;

import java.util.Optional;
import java.util.function.Predicate;


public abstract class AbstractItemSingleDataProcessor<T, V extends BaseValue<T>, M extends DataManipulator<M, I>, I extends ImmutableDataManipulator<I, M>> extends AbstractSingleDataProcessor<T, V, M, I> {

    private final Predicate<ItemStack> predicate;

    protected AbstractItemSingleDataProcessor(Predicate<ItemStack> predicate, Key<V> key) {
        super(key);
        this.predicate = predicate;
    }


    protected abstract boolean set(ItemStack itemStack, T value);

    protected abstract Optional<T> getVal(ItemStack itemStack);

    protected abstract ImmutableValue<T> constructImmutableValue(T value);

    @SuppressWarnings("unchecked")
    @Override
    public boolean supports(DataHolder dataHolder) {
        return dataHolder instanceof ItemStack && this.predicate.test((ItemStack) dataHolder);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<M> from(DataHolder dataHolder) {
        if (!supports(dataHolder)) {
            return Optional.empty();
        } else {
            final Optional<T> optional = getVal((ItemStack) dataHolder);
            if (optional.isPresent()) {
                return Optional.of(createManipulator().set(this.key, optional.get()));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<M> fill(DataContainer container, M m) {
        m.set(this.key, DataUtil.getData(container, this.key));
        return Optional.of(m);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public DataTransactionResult set(DataHolder dataHolder, M manipulator, MergeFunction function) {
        if (supports(dataHolder)) {
            final DataTransactionBuilder builder = DataTransactionBuilder.builder();
            final Optional<M> old = from(dataHolder);
            final M merged = checkNotNull(function).merge(old.orElse(null), manipulator);
            final T newValue = merged.get(this.key).get();
            final V immutableValue = (V) ((Value) merged.getValue(this.key).get()).asImmutable();
            try {
                if (set((ItemStack) dataHolder, newValue)) {
                    if (old.isPresent()) {
                        builder.replace(old.get().getValues());
                    }
                    return builder.result(DataTransactionResult.Type.SUCCESS).success((ImmutableValue<?>) immutableValue).build();
                } else {
                    return builder.result(DataTransactionResult.Type.FAILURE).reject((ImmutableValue<?>) immutableValue).build();
                }
            } catch (Exception e) {
                Sponge.getLogger().debug("An exception occurred when setting data: ", e);
                return builder.result(DataTransactionResult.Type.ERROR).reject((ImmutableValue<?>) newValue).build();
            }
        }
        return DataTransactionBuilder.failResult(manipulator.getValues());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<I> with(Key<? extends BaseValue<?>> key, Object value, I immutable) {
        if (immutable.supports(key)) {
            return Optional.of(immutable.asMutable().set(this.key, (T) value).asImmutable());
        }
        return Optional.empty();
    }
}
