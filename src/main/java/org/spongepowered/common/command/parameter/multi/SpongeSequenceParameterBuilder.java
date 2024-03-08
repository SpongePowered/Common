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
package org.spongepowered.common.command.parameter.multi;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.common.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

public final class SpongeSequenceParameterBuilder implements Parameter.SequenceBuilder {

    private boolean terminal;
    private boolean optional;
    private final List<Parameter> parameterList = new ArrayList<>();

    @Override
    public Parameter.@NonNull SequenceBuilder terminal() {
        this.terminal = true;
        return this;
    }

    @Override
    public Parameter.@NonNull SequenceBuilder optional() {
        this.optional = true;
        return this;
    }

    @Override
    public Parameter.@NonNull SequenceBuilder then(final @NonNull Parameter parameter) {
        this.parameterList.add(parameter);
        return this;
    }

    @Override
    public Parameter.@NonNull Multi build() {
        Preconditions.checkState(!this.parameterList.isEmpty(), "There must be at least one parameter!");
        return new SpongeSequenceParameter(this.parameterList, this.optional, this.terminal);
    }

    @Override
    public Parameter.@NonNull SequenceBuilder reset() {
        this.terminal = false;
        this.optional = false;
        this.parameterList.clear();
        return this;
    }
}
