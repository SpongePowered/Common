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
package org.spongepowered.common.data.value;

import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.common.data.key.SpongeKey;

import java.util.StringJoiner;

public abstract class SpongeValue<E> implements Value<E> {

    protected final SpongeKey<? extends Value<E>, E> key;
    protected E element;

    @SuppressWarnings("unchecked")
    public SpongeValue(Key<? extends Value<E>> key, E element) {
        this.key = (SpongeKey<? extends Value<E>, E>) key;
        this.element = element;
    }

    @Override
    public E get() {
        return this.element;
    }

    @Override
    public SpongeKey<? extends Value<E>, E> key() {
        return this.key;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SpongeValue.class.getSimpleName() + "[", "]")
                .add("key=" + this.key)
                .add("element=" + this.element)
                .toString();
    }
}
