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

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.command.parameter.Parameter;

import java.util.ArrayList;
import java.util.List;

public final class SpongeFirstOfParameterBuilder implements Parameter.FirstOfBuilder {

    private boolean isTerminal;
    private boolean isOptional;
    private final List<Parameter> parameterList = new ArrayList<>();

    @Override
    public Parameter.@NonNull FirstOfBuilder terminal() {
        this.isTerminal = true;
        return this;
    }

    @Override
    public Parameter.@NonNull FirstOfBuilder optional() {
        this.isOptional = true;
        return this;
    }

    @Override
    public Parameter.@NonNull FirstOfBuilder or(@NonNull final Parameter parameter) {
        this.parameterList.add(parameter);
        return this;
    }

    @Override
    public Parameter.@NonNull Multi build() {
        Preconditions.checkState(!this.parameterList.isEmpty(), "There must be at least one parameter!");
        return new SpongeFirstOfParameter(this.parameterList, this.isOptional, this.isTerminal);
    }

    @Override
    public Parameter.@NonNull FirstOfBuilder reset() {
        this.isTerminal = false;
        this.isOptional = false;
        this.parameterList.clear();
        return this;
    }
}
