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
package org.spongepowered.common.command;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.minecraft.commands.CommandSourceStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.common.command.brigadier.SpongeParameterTranslator;
import org.spongepowered.common.command.brigadier.SpongeStringReader;
import org.spongepowered.common.command.brigadier.dispatcher.SpongeCommandDispatcher;
import org.spongepowered.common.command.manager.SpongeCommandManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class SpongeParameterizedCommand implements Command.Parameterized {

    private final List<Parameter.Subcommand> subcommands;
    private final List<Parameter> parameters;
    private final List<Flag> flags;
    private final Function<CommandCause, Optional<Component>> shortDescription;
    private final Function<CommandCause, Optional<Component>> extendedDescription;
    private final Predicate<CommandCause> executionRequirements;
    private final @Nullable CommandExecutor executor;
    private final boolean isTerminal;
    private @Nullable SpongeCommandManager commandManager;
    private @Nullable SpongeCommandDispatcher cachedDispatcher;

    SpongeParameterizedCommand(
            final List<Parameter.Subcommand> subcommands,
            final List<Parameter> parameters,
            final Function<CommandCause, Optional<Component>> shortDescription,
            final Function<CommandCause, Optional<Component>> extendedDescription,
            final Predicate<CommandCause> executionRequirements,
            final @Nullable CommandExecutor executor,
            final List<Flag> flags,
            final boolean isTerminal) {
        this.subcommands = subcommands;
        this.parameters = parameters;
        this.shortDescription = shortDescription;
        this.extendedDescription = extendedDescription;
        this.executionRequirements = executionRequirements;
        this.executor = executor;
        this.flags = flags;
        this.isTerminal = isTerminal;
    }

    public void setCommandManager(final SpongeCommandManager commandManager) {
        this.cachedDispatcher = null;
        this.commandManager = commandManager;
    }

    @Override
    public List<CommandCompletion> complete(final @NonNull CommandCause cause, final ArgumentReader.@NonNull Mutable arguments) {
        final SpongeCommandDispatcher dispatcher = this.getCachedDispatcher();
        final String input = arguments.remaining();
        final ParseResults<CommandSourceStack> parseResults = dispatcher.parse((StringReader) arguments, (CommandSourceStack) cause);
        final Suggestions suggestions = dispatcher.getCompletionSuggestions(parseResults).join();
        return suggestions.getList().stream().map(SpongeCommandCompletion::from).collect(Collectors.toList());
    }

    @Override
    public boolean canExecute(final @NonNull CommandCause cause) {
        return this.executionRequirements().test(cause);
    }

    @Override
    public @NonNull Optional<Component> shortDescription(final @NonNull CommandCause cause) {
        return this.shortDescription.apply(cause);
    }

    @Override
    public @NonNull Optional<Component> extendedDescription(final @NonNull CommandCause cause) {
        return this.extendedDescription.apply(cause);
    }

    @Override
    public @NonNull Component usage(final @NonNull CommandCause cause) {
        final Collection<Component> usage =
                Arrays.stream(this.getCachedDispatcher().getAllUsage(this.getCachedDispatcher().getRoot(), (CommandSourceStack) cause, true))
                    .map(Component::text).collect(Collectors.toList());
        return Component.join(JoinConfiguration.separator(Component.newline()), usage);
    }

    @Override
    public List<Flag> flags() {
        return new ArrayList<>(this.flags);
    }

    @Override
    public @NonNull List<Parameter> parameters() {
        return new ArrayList<>(this.parameters);
    }

    @Override
    public List<Parameter.Subcommand> subcommands() {
        return new ArrayList<>(this.subcommands);
    }

    @Override
    public boolean isTerminal() {
        return this.isTerminal;
    }

    @Override
    public @NonNull Predicate<CommandCause> executionRequirements() {
        return this.executionRequirements;
    }

    @Override
    public @NonNull CommandContext parseArguments(final @NonNull CommandCause cause, final ArgumentReader.@NonNull Mutable arguments) {
        final ParseResults<CommandSourceStack> results = this.getCachedDispatcher().parse((SpongeStringReader) arguments, (CommandSourceStack) cause);
        return (CommandContext) results.getContext().build(arguments.input());
    }

    @Override
    public Optional<CommandExecutor> executor() {
        return Optional.ofNullable(this.executor);
    }

    private SpongeCommandDispatcher getCachedDispatcher() {
        if (this.cachedDispatcher == null) {
            if (this.commandManager == null) {
                throw new IllegalStateException("Completions cannot be requested for an unregistered parameterized command");
            }
            this.cachedDispatcher = new SpongeCommandDispatcher(this.commandManager);
            this.cachedDispatcher.register(this.buildWithAlias("command"));
        }
        return this.cachedDispatcher;
    }

    public LiteralCommandNode<CommandSourceStack> buildWithAlias(final String primaryAlias) {
        return this.buildWithAliases(Collections.singleton(primaryAlias)).iterator().next();
    }

    public Collection<LiteralCommandNode<CommandSourceStack>> buildWithAliases(final Collection<String> aliases) {
        return SpongeParameterTranslator.INSTANCE.createCommandTree(this, aliases);
    }
}
