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
package org.spongepowered.common.data.provider;

import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.common.bridge.data.CustomDataHolderBridge;

import java.util.Optional;

public class CustomDataProvider<V extends Value<E>, E> extends MutableDataProvider<V, E> {

    public CustomDataProvider(Key<V> key) {
        super(key);
    }

    @Override
    public Optional<E> get(DataHolder dataHolder) {
        if (this.isSupported(dataHolder)) {
            return ((CustomDataHolderBridge) dataHolder).bridge$getCustom(this.getKey());
        }
        return Optional.empty();
    }

    @Override
    public boolean isSupported(DataHolder dataHolder) {
        if (!(dataHolder instanceof CustomDataHolderBridge)) {
            return false;
        }
        // TODO supported dataholders in registration?
        return true;
    }

    @Override
    public DataTransactionResult offer(DataHolder.Mutable dataHolder, E element) {
        if (this.isSupported(dataHolder)) {
            return ((CustomDataHolderBridge) dataHolder).bridge$offerCustom(this.getKey(),  element);
        }
        return DataTransactionResult.failNoData();
    }

    @Override
    public DataTransactionResult remove(DataHolder.Mutable dataHolder) {
        if (this.isSupported(dataHolder)) {
            return ((CustomDataHolderBridge) dataHolder).bridge$removeCustom(this.getKey());
        }
        return DataTransactionResult.failNoData();
    }
}
