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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParameterModifier;
import org.spongepowered.api.command.parameter.managed.ValueUsage;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.commands.arguments.CompletionsArgumentTypeBridge;
import org.spongepowered.common.command.SpongeCommandCompletion;
import org.spongepowered.common.command.brigadier.SpongeStringReader;
import org.spongepowered.common.command.brigadier.argument.ArgumentParser;
import org.spongepowered.common.command.brigadier.argument.ComplexSuggestionNodeProvider;
import org.spongepowered.common.command.brigadier.argument.ResourceKeyedArgumentValueParser;
import org.spongepowered.common.command.brigadier.context.SpongeCommandContextBuilder;
import org.spongepowered.common.util.CommandUtil;
import org.spongepowered.common.util.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// We have to extend ArgumentCommandNode for Brig to even use this...
public final class SpongeArgumentCommandNode<T> extends ArgumentCommandNode<CommandSourceStack, T> implements SpongeNode {

    private static @Nullable SuggestionProvider<CommandSourceStack> createSuggestionProvider(final @Nullable ValueCompleter completer) {
        if (completer == null) {
            return null;
        }

        // We don't need to go through everything if we're passing through to the native completer.
        if (completer instanceof ResourceKeyedArgumentValueParser.ClientNativeCompletions<?>) {
            return ((ResourceKeyedArgumentValueParser.ClientNativeCompletions<?>) completer)::listSuggestions;
        }

        return (context, builder) -> {
            final List<CommandCompletion> suggestions = completer.complete((org.spongepowered.api.command.parameter.CommandContext) context, builder.getRemaining());
            for (final CommandCompletion suggestion : suggestions) {
                builder.suggest(suggestion.completion(), suggestion.tooltip().map(SpongeAdventure::asVanilla).orElse(null));
            }
            return builder.buildFuture();
        };
    }

    private final Parameter.Key<? super T> key;
    private final ArgumentParser<T> parser;
    private final @Nullable ValueParameterModifier<T> modifier;
    private final ValueUsage usage;
    private final boolean isComplexSuggestions;
    private final boolean isOptional;

    // used so we can have insertion order.
    private final UnsortedNodeHolder nodeHolder = new UnsortedNodeHolder();

    private @Nullable Command<CommandSourceStack> executor;
    private @Nullable CommandNode<CommandSourceStack> forcedRedirect;

    @SuppressWarnings({"unchecked"})
    public SpongeArgumentCommandNode(
            final Parameter.Key<? super T> key,
            final ValueUsage usage,
            final ArgumentParser<T> parser,
            final @Nullable ValueCompleter valueCompleter,
            final @Nullable Command command,
            final Predicate<CommandSourceStack> predicate,
            final @Nullable CommandNode<CommandSourceStack> redirect,
            final RedirectModifier<CommandSourceStack> modifier,
            final boolean forks,
            final String keyName,
            final @Nullable ValueParameterModifier<T> parameterModifier,
            final boolean isOptional) {
        super(keyName,
                (ArgumentType<T>) Constants.Command.STANDARD_STRING_ARGUMENT_TYPE, // we can abuse generics, we're not actually going to use this.
                command,
                predicate,
                redirect,
                modifier,
                forks,
                SpongeArgumentCommandNode.createSuggestionProvider(valueCompleter));
        this.parser = parser;
        this.modifier = parameterModifier;
        this.isComplexSuggestions = this.parser instanceof ComplexSuggestionNodeProvider;
        this.key = key;
        this.usage = usage;
        this.isOptional = isOptional;
    }

    public Parameter.Key<? super T> key() {
        return this.key;
    }

    public final boolean isComplex() {
        return this.isComplexSuggestions;
    }

    public final CommandNode<SharedSuggestionProvider> getComplexSuggestions(
            final CommandNode<SharedSuggestionProvider> rootSuggestionNode,
            final Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> commandNodeToSuggestionNode,
            final Map<CommandNode<CommandSourceStack>, List<CommandNode<SharedSuggestionProvider>>> commandNodeListMap,
            final boolean allowCustomSuggestionsOnTheFirstElement) {
        if (!this.isComplexSuggestions) {
            throw new IllegalStateException("The parser is not a ComplexSuggestionNodeParser");
        }

        final ComplexSuggestionNodeProvider provider = (ComplexSuggestionNodeProvider) this.parser;
        return provider.createSuggestions(
                rootSuggestionNode,
                this.key.key(),
                this.getCommand() != null,
                nodeList -> commandNodeListMap.put(this, nodeList),
                firstNode -> commandNodeToSuggestionNode.put(this, firstNode),
                allowCustomSuggestionsOnTheFirstElement);
    }

    @Override
    public final Collection<CommandNode<CommandSourceStack>> getChildrenForSuggestions() {
        return this.nodeHolder.getChildrenForSuggestions();
    }

    private ArgumentType<?> switchTypeIfRequired(final ArgumentType<?> type) {
        if (type instanceof CompletionsArgumentTypeBridge) {
            return ((CompletionsArgumentTypeBridge<?>) type).bridge$clientSideCompletionType();
        }
        return type;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public final ArgumentBuilder<SharedSuggestionProvider, ?> createBuilderForSuggestions(
            final CommandNode<SharedSuggestionProvider> rootSuggestionNode,
            final Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> commandNodeToSuggestionNode
    ) {
        ArgumentType<?> type = this.switchTypeIfRequired(this.getType());
        CommandNode<SharedSuggestionProvider> previousNode = rootSuggestionNode;
        if (!this.parser.getClientCompletionArgumentType().isEmpty()) {
            // create multiple entries, return the last one
            final boolean forceCustomSuggestions;
            final Collection<ArgumentType<?>> types = this.parser.getClientCompletionArgumentType().stream()
                    .filter(Objects::nonNull).collect(Collectors.toList());
            if (types.size() > 1) {
                forceCustomSuggestions = false; // handled in here
                final Iterator<ArgumentType<?>> clientCompletionTypeIterator = this.parser.getClientCompletionArgumentType().iterator();
                boolean isFirst = true;
                while (clientCompletionTypeIterator.hasNext()) {
                    final ArgumentType<?> originalType = clientCompletionTypeIterator.next();
                    type = this.switchTypeIfRequired(originalType);
                    final boolean forceCustomSuggestionsInner = type != this.getType() && !CommandUtil.checkForCustomSuggestions(previousNode);
                    if (clientCompletionTypeIterator.hasNext()) {
                        // create node
                        final RequiredArgumentBuilder<SharedSuggestionProvider, ?> arg = RequiredArgumentBuilder.argument(this.getName(), type);
                        arg.requires(x -> true);
                        // if the first node is forced to be custom suggestions or is a string argument type, send the completions.
                        if (forceCustomSuggestionsInner || isFirst && type instanceof StringArgumentType) {
                            arg.suggests(SuggestionProviders.ASK_SERVER);
                        }
                        final CommandNode<SharedSuggestionProvider> built = arg.build();
                        previousNode.addChild(built);
                        previousNode = built;
                        if (isFirst) {
                            commandNodeToSuggestionNode.put(this, built);
                            isFirst = false;
                        }
                    }
                }
            } else {
                final ArgumentType<?> originalType = this.parser.getClientCompletionArgumentType().get(0);
                type = this.switchTypeIfRequired(originalType);
                forceCustomSuggestions = type != originalType;
            }

            final RequiredArgumentBuilder<SharedSuggestionProvider, ?> toReturn = RequiredArgumentBuilder.argument(this.getUsageTextForClient(), type);
            if (this.getCommand() != null) {
                toReturn.executes(x -> 0);
            }
            if (this.modifier != null || forceCustomSuggestions && !CommandUtil.checkForCustomSuggestions(previousNode)) {
                toReturn.suggests(SuggestionProviders.ASK_SERVER);
            } else if (this.getCustomSuggestions() != null) {
                toReturn.suggests((SuggestionProvider) this.getCustomSuggestions());
            }
            if (this.getRedirect() != null) {
                toReturn.forward((CommandNode) this.getRedirect(), (RedirectModifier) this.getRedirectModifier(), this.isFork());
            }
            return toReturn;
        }

        // ensure we send what we want to send to the client in terms of the usage string.
        final RequiredArgumentBuilder<CommandSourceStack, ?> builder = RequiredArgumentBuilder.argument(this.getUsageTextForClient(), type);
        builder.requires(this.getRequirement());
        builder.forward(this.getRedirect(), this.getRedirectModifier(), this.isFork());
        if (this.modifier != null) {
            builder.suggests((SuggestionProvider) SuggestionProviders.ASK_SERVER);
        } if (!CommandUtil.checkForCustomSuggestions(rootSuggestionNode)) {
            if (type != this.getType()) {
                builder.suggests((SuggestionProvider) SuggestionProviders.ASK_SERVER);
            } else {
                builder.suggests(this.getCustomSuggestions());
            }
        }
        if (this.getCommand() != null) {
            builder.executes(this.getCommand());
        }
        return (ArgumentBuilder) builder;
    }

    public final ArgumentParser<? extends T> getParser() {
        return this.parser;
    }

    @Override
    public void forceExecutor(final Command<CommandSourceStack> forcedExecutor) {
        this.executor = forcedExecutor;
    }

    @Override
    public boolean canForceRedirect() {
        return this.getChildren() == null || this.getChildren().isEmpty();
    }

    @Override
    public void forceRedirect(final CommandNode<CommandSourceStack> forcedRedirect) {
        this.forcedRedirect = forcedRedirect;
    }

    @Override
    public CommandNode<CommandSourceStack> getRedirect() {
        final CommandNode<CommandSourceStack> redirect = super.getRedirect();
        if (redirect != null) {
            return redirect;
        }
        if (this.canForceRedirect()) {
            return this.forcedRedirect;
        }
        return null;
    }

    @Override
    public Command<CommandSourceStack> getCommand() {
        final Command<CommandSourceStack> command = super.getCommand();
        if (command != null) {
            return command;
        }
        return this.executor;
    }

    @Override
    public String getUsageText() {
        if (this.usage != null) {
            return this.usage.usage(this.key.key());
        }
        return super.getUsageText();
    }

    private String getUsageTextForClient() {
        if (this.usage != null) {
            return this.usage.usage(this.key.key());
        }
        return this.getName();
    }

    public boolean isOptional() {
        return this.isOptional;
    }

    @Override
    public final void parse(final StringReader reader, final CommandContextBuilder<CommandSourceStack> contextBuilder) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final SpongeCommandContextBuilder builder = (SpongeCommandContextBuilder) contextBuilder;
        final T result = this.parser.parse(this.key, builder, (SpongeStringReader) reader, this.modifier);
        if (result != null) {
            builder.putEntry(this.key, result);
            final ParsedArgument<CommandSourceStack, T> parsed = new ParsedArgument<>(start, reader.getCursor(), result);
            builder.withArgumentInternal(this.getName(), parsed, false);
            builder.withNode(this, parsed.getRange());
        } else if (this.parser.doesNotRead()) {
            // Assume this is a null "optional" parser and add the node as read so that we dont end up with an empty context
            builder.withNode(this, StringRange.at(start));
        }
    }

    @Override
    public final CompletableFuture<Suggestions> listSuggestions(
            final CommandContext<CommandSourceStack> context,
            final SuggestionsBuilder builder) throws CommandSyntaxException {
        final CompletableFuture<Suggestions> suggestions;
        if (this.getCustomSuggestions() == null) {
            suggestions = this.parser.listSuggestions(context, builder);
        } else {
            suggestions = this.getCustomSuggestions().getSuggestions(context, builder);
        }
        // applies the modifier if there is one
        return this.suggestUsingModifier(context, builder, suggestions);
    }

    @Override
    public final Collection<String> getExamples() {
        return this.parser.getExamples();
    }

    @Override
    public void addChild(final CommandNode<CommandSourceStack> node) {
        super.addChild(node);
        this.nodeHolder.add(node);
    }

    private CompletableFuture<Suggestions> suggestUsingModifier(
            final CommandContext<?> context,
            final SuggestionsBuilder suggestionsBuilder,
            final CompletableFuture<Suggestions> suggestions) {
        if (this.modifier != null) {
            return suggestions.thenApply(x -> {
                final List<CommandCompletion> originalSuggestions =
                        x.getList().stream().map(SpongeCommandCompletion::from).collect(Collectors.toList());
                final List<CommandCompletion> modifiedSuggestions =
                        this.modifier.modifyCompletion((org.spongepowered.api.command.parameter.CommandContext) context, suggestionsBuilder.getRemaining(), new ArrayList<>(originalSuggestions));
                if (originalSuggestions.equals(modifiedSuggestions)) {
                    return x;
                }
                final SuggestionsBuilder newBuilder = suggestionsBuilder.restart();
                for (final CommandCompletion suggestion : modifiedSuggestions) {
                    newBuilder.suggest(suggestion.completion(), suggestion.tooltip().map(SpongeAdventure::asVanilla).orElse(null));
                }
                return newBuilder.build();
            });
        }
        return suggestionsBuilder.buildFuture();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.key, this.parser, this.modifier, this.usage, this.isComplexSuggestions, this.getCustomSuggestions());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final SpongeArgumentCommandNode<?> that = (SpongeArgumentCommandNode<?>) o;
        return this.isComplexSuggestions == that.isComplexSuggestions &&
                this.getRedirect() == that.getRedirect() && // See SuggestionArgumentNode for an explanation
                this.key.equals(that.key) &&
                this.parser.equals(that.parser) &&
                Objects.equals(this.modifier, that.modifier) &&
                Objects.equals(this.usage, that.usage) &&
                Objects.equals(this.getCustomSuggestions(), that.getCustomSuggestions());
    }

}
