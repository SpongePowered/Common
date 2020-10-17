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

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.CommandSource;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.common.command.brigadier.tree.SpongeCommandExecutorWrapper;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class SpongeMultiParameter implements Parameter {

    private final List<Parameter> parameterCandidates;
    private final boolean isOptional;
    private final boolean isTerminal;

    protected SpongeMultiParameter(final List<Parameter> parameterCandidates, final boolean isOptional, final boolean isTerminal) {
        this.parameterCandidates = ImmutableList.copyOf(parameterCandidates);
        this.isOptional = isOptional;
        this.isTerminal = isTerminal;
    }

    public List<Parameter> getParameterCandidates() {
        // put subcommands first.
        return this.parameterCandidates.stream().sorted((x1, x2) -> {
            final boolean firstIs = x1 instanceof Parameter.Subcommand;
            final boolean secondIs = x2 instanceof Parameter.Subcommand;
            if (firstIs != secondIs) {
                if (firstIs) {
                    return -1;
                }
                return 1;
            }
            return 0;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean isOptional() {
        return this.isOptional;
    }

    @Override
    public boolean isTerminal() {
        return this.isTerminal;
    }

    public abstract boolean createNode(SpongeCommandExecutorWrapper executorWrapper,
            Consumer<CommandNode<CommandSource>> parentNode,
            Consumer<ArgumentBuilder<CommandSource, ?>> nodeCallback,
            List<CommandNode<CommandSource>> potentialOptionalRedirects,
            boolean isTermination,
            boolean previousWasOptional,
            String suffix);
}
