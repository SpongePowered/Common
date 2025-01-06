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
package org.spongepowered.common.command.brigadier.tree;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParameterModifier;
import org.spongepowered.api.command.parameter.managed.ValueUsage;
import org.spongepowered.common.command.brigadier.argument.ArgumentParser;
import org.spongepowered.common.command.parameter.SpongeParameterKey;

// We use the ArgumentBuilder primarily for setting redirects properly.
public final class SpongeArgumentCommandNodeBuilder<T> extends ArgumentBuilder<CommandSourceStack, SpongeArgumentCommandNodeBuilder<T>> {

    private final SpongeParameterKey<? super T> key;
    private final ArgumentParser<T> type;
    private final @Nullable ValueCompleter completer;
    private final @Nullable String suffix;
    private final @Nullable ValueUsage usage;
    private final @Nullable ValueParameterModifier<T> modifier;
    private final boolean isOptional;

    private static @Nullable ValueCompleter filterNativeCompleters(final ArgumentParser<?> parser, final ValueCompleter completer) {
        if (parser == completer && parser.hasClientNativeCompletions()) {
            return null;
        }
        return completer;
    }

    public SpongeArgumentCommandNodeBuilder(
            final SpongeParameterKey<? super T> key,
            final ArgumentParser<T> type,
            final ValueCompleter completer,
            final @Nullable ValueParameterModifier<T> modifier,
            final @Nullable ValueUsage usage,
            final @Nullable String suffix,
            final boolean isOptional) {
        this.key = key;
        this.type = type;
        this.completer = SpongeArgumentCommandNodeBuilder.filterNativeCompleters(type, completer);
        this.modifier = modifier;
        this.usage = usage;
        this.suffix = suffix;
        this.isOptional = isOptional;
    }

    @Override
    protected SpongeArgumentCommandNodeBuilder<T> getThis() {
        return this;
    }

    @Override
    public SpongeArgumentCommandNode<T> build() {
        final SpongeArgumentCommandNode<T> node = new SpongeArgumentCommandNode<>(
                this.key,
                this.usage,
                this.type,
                this.completer,
                this.getCommand(),
                this.getRequirement(),
                this.getRedirect(),
                this.getRedirectModifier(),
                this.isFork(),
                this.suffix == null ? this.key.key() : this.key.key() + "_" + this.suffix,
                this.modifier,
                this.isOptional);
        for (final CommandNode<CommandSourceStack> child : this.getArguments()) {
            node.addChild(child);
        }
        return node;
    }
}
