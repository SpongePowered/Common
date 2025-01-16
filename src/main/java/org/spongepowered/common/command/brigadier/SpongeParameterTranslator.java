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
package org.spongepowered.common.command.brigadier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.common.command.brigadier.argument.ArgumentParser;
import org.spongepowered.common.command.brigadier.argument.CustomArgumentParser;
import org.spongepowered.common.command.brigadier.tree.SpongeArgumentCommandNode;
import org.spongepowered.common.command.brigadier.tree.SpongeArgumentCommandNodeBuilder;
import org.spongepowered.common.command.brigadier.tree.SpongeCommandExecutorWrapper;
import org.spongepowered.common.command.brigadier.tree.SpongeFlagLiteralCommandNode;
import org.spongepowered.common.command.brigadier.tree.SpongeLiteralCommandNode;
import org.spongepowered.common.command.brigadier.tree.SpongeNode;
import org.spongepowered.common.command.parameter.SpongeParameterKey;
import org.spongepowered.common.command.parameter.SpongeParameterValue;
import org.spongepowered.common.command.parameter.multi.SpongeFirstOfParameter;
import org.spongepowered.common.command.parameter.multi.SpongeMultiParameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class SpongeParameterTranslator {

    public static final SpongeParameterTranslator INSTANCE = new SpongeParameterTranslator();

    private SpongeParameterTranslator() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Collection<LiteralCommandNode<CommandSourceStack>> createCommandTree(final Command.Parameterized command, final Collection<String> aliases) {
        if (aliases.isEmpty()) {
            throw new IllegalArgumentException("Aliases must not be empty.");
        }
        // create the first literal.
        final Collection<String> sortedAliases = aliases.stream().sorted().collect(Collectors.toList());
        final Iterator<String> aliasIterator = sortedAliases.iterator();

        final String baseAlias = aliasIterator.next();
        final SpongeCommandExecutorWrapper executor = command.executor().map(SpongeCommandExecutorWrapper::new).orElse(null);

        // Create the defining characteristics of the node.
        final LiteralArgumentBuilder<CommandSourceStack> basicNode = LiteralArgumentBuilder.literal(baseAlias);

        basicNode.requires((Predicate) command.executionRequirements());
        if (command.isTerminal() && executor != null) {
            basicNode.executes(executor);
        }

        final SpongeLiteralCommandNode commandNode = new SpongeLiteralCommandNode(basicNode);
        if (executor != null) {
            this.createAndAttachNode(Collections.singleton(commandNode), command.parameters(), executor, true, true);
        }

        for (final Parameter.Subcommand subcommand : command.subcommands()) {
            final Collection<LiteralCommandNode<CommandSourceStack>> builtSubcommand =
                    this.createCommandTree(subcommand.command(), subcommand.aliases());
            builtSubcommand.forEach(commandNode::addChild);
        }

        this.createFlags(commandNode, command.flags(), executor);

        final List<LiteralCommandNode<CommandSourceStack>> allCommandNodes = new ArrayList<>();
        allCommandNodes.add(commandNode);
        final Collection<CommandNode<CommandSourceStack>> children = commandNode.getChildren();
        while (aliasIterator.hasNext()) {
            final LiteralArgumentBuilder<CommandSourceStack> redirectedNode = LiteralArgumentBuilder.literal(aliasIterator.next());
            redirectedNode.executes(commandNode.getCommand());
            redirectedNode.requires(commandNode.getRequirement());
            // This would be redirectedNode.redirect(commandNode), but because of a bug
            // in Brigadier that impacts the client we have to do this.
            //
            // The problem is a faulty equality check for command nodes - they don't
            // consider redirects (I suspect because they use redirects to redirect to
            // a previous node, causing a stack overflow (we'd get this with flags). The
            // big problem generally comes in when you have two nodes with the same name
            // and a redirect (or not) - the client tries to be clever and de-duplicates
            // them but it doesn't account for the redirect...
            //
            // See https://github.com/Mojang/brigadier/pull/89 for details. If that is
            // merged, the above can be substituted in here.
            for (final CommandNode<CommandSourceStack> child : children) {
                redirectedNode.then(child);
            }
            allCommandNodes.add(new SpongeLiteralCommandNode(redirectedNode));
        }

        return Collections.unmodifiableCollection(allCommandNodes);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void createFlags(final LiteralCommandNode<CommandSourceStack> node, final List<Flag> flags,
            final @Nullable SpongeCommandExecutorWrapper wrapper) {
        final Collection<CommandNode<CommandSourceStack>> nodesToAddChildrenTo = new ArrayList<>();
        for (final Flag flag : flags) {
            // first create the literal.
            final Iterator<String> aliasIterator = flag.aliases().iterator();
            final LiteralArgumentBuilder<CommandSourceStack> flagLiteral = LiteralArgumentBuilder.literal(aliasIterator.next());
            flagLiteral.requires((Predicate) flag.requirement());
            final Collection<? extends CommandNode<CommandSourceStack>> toBeRedirected;
            final SpongeFlagLiteralCommandNode flagNode = new SpongeFlagLiteralCommandNode(flagLiteral, flag);
            if (flag.associatedParameter().isPresent()) {
                toBeRedirected = this.createAndAttachNode(
                        Collections.singleton(flagNode),
                        Collections.singletonList(flag.associatedParameter().get()),
                        wrapper,
                        node.getCommand() != null,
                        false
                );
            } else {
                toBeRedirected = Collections.singletonList(flagNode);
            }
            for (final CommandNode<CommandSourceStack> candidate : toBeRedirected) {
                if (candidate instanceof SpongeNode && ((SpongeNode) candidate).canForceRedirect()) {
                    ((SpongeNode) candidate).forceRedirect(node);
                } else {
                    nodesToAddChildrenTo.add(candidate);
                }
            }
            node.addChild(flagNode);

            while (aliasIterator.hasNext()) {
                final LiteralArgumentBuilder<CommandSourceStack> nextFlag = LiteralArgumentBuilder
                        .<CommandSourceStack>literal(aliasIterator.next())
                        .executes(flagNode.getCommand());
                if (flagNode.getRedirect() != null) {
                    nextFlag.redirect(flagNode.getRedirect());
                } else {
                    nextFlag.redirect(flagNode);
                }
                node.addChild(new SpongeFlagLiteralCommandNode(nextFlag, flag));
            }
        }

        if (!nodesToAddChildrenTo.isEmpty()) {

            for (final CommandNode<CommandSourceStack> target : node.getChildren()) {
                if (!target.getChildren().isEmpty() && target instanceof SpongeFlagLiteralCommandNode) {
                    nodesToAddChildrenTo.forEach(x -> x.addChild(((SpongeFlagLiteralCommandNode) target).cloneWithRedirectToThis()));
                } else {
                    nodesToAddChildrenTo.forEach(x -> x.addChild(target));
                }
            }

        }

    }

    private Collection<? extends CommandNode<CommandSourceStack>> createAndAttachNode(
            final Collection<? extends CommandNode<CommandSourceStack>> parents,
            final List<Parameter> children,
            final @Nullable SpongeCommandExecutorWrapper executorWrapper,
            final boolean shouldTerminate,
            final boolean allowSubcommands) {

        final Set<CommandNode<CommandSourceStack>> tailNodes = new HashSet<>(parents);
        final Set<CommandNode<CommandSourceStack>> nodesToAttachTo = new HashSet<>(parents);
        final ListIterator<Parameter> parameterIterator = children.listIterator();
        while (parameterIterator.hasNext()) {
            final Parameter parameter = parameterIterator.next();
            final boolean hasNext = parameterIterator.hasNext();

            if (parameter instanceof Parameter.Subcommand) {
                final Parameter.Subcommand subcommands = ((Parameter.Subcommand) parameter);
                if (!allowSubcommands) {
                    throw new IllegalStateException("Subcommands are not allowed for this element (subcommands were " +
                            String.join(", ", subcommands.aliases() + ")!"));
                }
                if (hasNext) {
                    // this is a failure condition because there cannot be a parameter after a subcommand.
                    throw new IllegalStateException("A parameter cannot be placed after a subcommand parameter!");
                }
                // If the child is a subcommand, get the subcommand and attach it. At this point,
                // we're done with the chain and we break out.
                final Collection<LiteralCommandNode<CommandSourceStack>> nodes = this.createCommandTree(
                        subcommands.command(),
                        subcommands.aliases()
                );
                for (final LiteralCommandNode<CommandSourceStack> node : nodes) {
                    nodesToAttachTo.forEach(x -> x.addChild(node));
                }
                // No further attaching should be done in this scenario
                return Collections.emptyList();
            } else {
                final boolean isOptional;
                final boolean isTerminal;
                final Collection<CommandNode<CommandSourceStack>> parametersToAttachTo;
                if (parameter instanceof SpongeMultiParameter) {
                    isOptional = parameter.isOptional();
                    isTerminal = !hasNext || parameter.isTerminal();
                    // In these cases, we delegate to the parameter, which may return here.
                    if (parameter instanceof SpongeFirstOfParameter) {
                        // take each parameter in turn and evaluate it, returns the terminal nodes of each block
                        parametersToAttachTo = new ArrayList<>();
                        for (final Parameter p : ((SpongeFirstOfParameter) parameter).childParameters()) {
                            final Collection<? extends CommandNode<CommandSourceStack>> branchNodesToAttachTo = this.createAndAttachNode(
                                    nodesToAttachTo,
                                    Collections.singletonList(p),
                                    executorWrapper,
                                    isTerminal,
                                    allowSubcommands);
                            parametersToAttachTo.addAll(branchNodesToAttachTo);
                        }
                    } else {
                        // not so fancy stuff, it's a sequence, returns the terminal nodes of the block
                        parametersToAttachTo = new ArrayList<>(this.createAndAttachNode(
                                nodesToAttachTo,
                                ((SpongeMultiParameter) parameter).childParameters(),
                                executorWrapper,
                                isTerminal,
                                allowSubcommands));
                    }
                } else {
                    // We have a Parameter.Value
                    parametersToAttachTo = new ArrayList<>();
                    final SpongeParameterValue<?> valueParameter = (SpongeParameterValue<?>) parameter;
                    final boolean isConsumeAll = valueParameter.willConsumeAllRemaining();
                    if (isConsumeAll && hasNext) {
                        // this should not happen.
                        throw new IllegalStateException("A parameter that consumes all must be at the end of a parameter chain.");
                    }

                    isOptional = valueParameter.isOptional();
                    isTerminal = (shouldTerminate && !hasNext) || valueParameter.isTerminal();

                    // Process the next element if it exists
                    StringBuilder suffix = null;
                    final Set<String> names = nodesToAttachTo
                            .stream()
                            .flatMap(x -> x.getChildren().stream())
                            .map(CommandNode::getName)
                            .collect(Collectors.toSet());
                    String key = valueParameter.key().key();
                    while (names.contains(key)) {
                        if (suffix == null) {
                            suffix = new StringBuilder(String.valueOf(names.size()));
                        } else {
                            suffix.append("_").append(names.size());
                        }
                        // prevents duplication
                        key = key + suffix;
                    }
                    final SpongeArgumentCommandNodeBuilder<?> thisNode = SpongeParameterTranslator
                            .createArgumentNodeBuilders(valueParameter, suffix == null ? null : suffix.toString());

                    if (isTerminal) {
                        thisNode.executes(executorWrapper);
                    }

                    // Apply the node to the parent.
                    final CommandNode<CommandSourceStack> builtNode = thisNode.build();
                    if (isConsumeAll) {
                        // the built child will return to the previous node, which will allow this to be called again.
                        builtNode.addChild(thisNode.redirect(builtNode).build());
                    }

                    parametersToAttachTo.add(builtNode);

                    // Filter out optional nodes that have a matching type, they are nearly impossible to "merge" correctly.
                    if (isOptional) {
                        final Set<CommandNode<CommandSourceStack>> conflictingNodes = nodesToAttachTo.stream()
                            .filter(n -> n instanceof final SpongeArgumentCommandNode<?> argumentNodeToAttachTo
                                && argumentNodeToAttachTo.isOptional() && argumentNodeToAttachTo.key().type().equals(valueParameter.key().type()))
                            .collect(Collectors.toSet());
                        if (!conflictingNodes.isEmpty()) {
                            nodesToAttachTo.removeIf(n -> n.getChildren().stream().anyMatch(conflictingNodes::contains));
                        }
                    }

                    // Make sure the nodes we need to attach to have the nodes we need to
                    nodesToAttachTo.forEach(x -> x.addChild(builtNode));
                }

                // If this is not optional, then we clear the "toAttachTo" list because we do not want to skip the parameter.
                if (!isOptional) {
                    tailNodes.clear();
                    nodesToAttachTo.clear();
                }

                tailNodes.addAll(parametersToAttachTo);
                nodesToAttachTo.addAll(parametersToAttachTo);
            }
        }

        // If we should make any terminal parameters actually terminal, we do that now.
        if (shouldTerminate) {
            for (final CommandNode<CommandSourceStack> node : tailNodes) {
                // These are therefore terminal.
                if (node instanceof SpongeNode) { // they should be, but just in case
                    ((SpongeNode) node).forceExecutor(executorWrapper);
                }
            }
        }

        return tailNodes;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> @NonNull SpongeArgumentCommandNodeBuilder<T> createArgumentNodeBuilders(
            final SpongeParameterValue<T> parameter,
            final @Nullable String suffix) {

        ArgumentParser<T> type = parameter.argumentTypeIfStandard();

        if (type == null) {
            type = new CustomArgumentParser<>(parameter.parsers(), parameter.completer(), false);
        }

        final SpongeArgumentCommandNodeBuilder<T> argumentBuilder = new SpongeArgumentCommandNodeBuilder<>(
                SpongeParameterKey.getSpongeKey(parameter.key()),
                type,
                parameter.completer(),
                parameter.modifier().orElse(null),
                parameter.valueUsage().orElse(null),
                suffix,
                parameter.isOptional()
        );
        // CommandCause is mixed into CommandSource, so this is okay.
        argumentBuilder.requires((Predicate) parameter.requirement());
        return argumentBuilder;
    }

}
