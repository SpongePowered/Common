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
package org.spongepowered.common.mixin.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.impl.AdvancementCommand;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.command.brigadier.dispatcher.DelegatingCommandDispatcher;
import org.spongepowered.common.command.brigadier.dispatcher.SpongeNodePermissionCache;
import org.spongepowered.common.command.brigadier.tree.SpongeArgumentCommandNode;
import org.spongepowered.common.command.brigadier.tree.SpongeNode;
import org.spongepowered.common.command.manager.SpongeCommandManager;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.launch.Launcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(Commands.class)
public abstract class CommandsMixin {

    @Shadow public abstract CommandDispatcher<CommandSource> shadow$getDispatcher();
    @Shadow private void shadow$commandSourceNodesToSuggestionNodes(final CommandNode<CommandSource> rootCommandSource,
            final CommandNode<ISuggestionProvider> rootSuggestion,
            final CommandSource source,
            final Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> commandNodeToSuggestionNode) {
        throw new AssertionError("This shouldn't be callable");
    }

    private CauseStackManager.StackFrame impl$initFrame = null;
    private final WeakHashMap<ServerPlayerEntity, Map<CommandNode<CommandSource>, List<CommandNode<ISuggestionProvider>>>> impl$playerNodeCache =
            new WeakHashMap<>();

    // We augment the CommandDispatcher with our own methods using a wrapper, so we need to make sure it's replaced here.
    @Redirect(method = "<init>", at = @At(
            value = "NEW",
            args = "class=com/mojang/brigadier/CommandDispatcher",
            remap = false
    ))
    private CommandDispatcher<CommandSource> impl$useSpongeDispatcher() {
        return new DelegatingCommandDispatcher();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/command/impl/AdvancementCommand;register(Lcom/mojang/brigadier/CommandDispatcher;)V"))
    private void impl$setupStackFrameOnInit(final CommandDispatcher<CommandSource> dispatcher) {
        ((SpongeCommandManager) SpongeCommon.getGame().getCommandManager()).reset();
        this.impl$initFrame = PhaseTracker.getCauseStackManager().pushCauseFrame();
        this.impl$initFrame.pushCause(Launcher.getInstance().getMinecraftPlugin());
        AdvancementCommand.register(dispatcher);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void impl$tellDispatcherCommandsAreRegistered(final CallbackInfo ci) {
        this.impl$initFrame.popCause();
        PhaseTracker.getCauseStackManager().popCauseFrame(this.impl$initFrame);
        this.impl$initFrame = null;
    }

    /*
     * Hides nodes that we have marked as "hidden"
     */
    @Redirect(method = "commandSourceNodesToSuggestionNodes",
            slice = @Slice(
                    from = @At("HEAD"),
                    to = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z")
            ),
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/tree/CommandNode;getChildren()Ljava/util/Collection;", remap = false))
    private Collection<CommandNode<CommandSource>> impl$handleHiddenChildrenAndEnsureUnsortedLoop(final CommandNode<CommandSource> commandNode) {
        return this.impl$getChildrenFromNode(commandNode);
    }

    @Redirect(method = "commandSourceNodesToSuggestionNodes",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/brigadier/tree/CommandNode;createBuilder()Lcom/mojang/brigadier/builder/ArgumentBuilder;",
                    remap = false))
    private ArgumentBuilder<?, ?> impl$createArgumentBuilder(
            final CommandNode<CommandSource> commandNode,
            final CommandNode<CommandSource> rootCommandSource,
            final CommandNode<ISuggestionProvider> rootSuggestion,
            final CommandSource source,
            final Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> commandNodeToSuggestionNode) {
        if (commandNode instanceof SpongeArgumentCommandNode) {
            return ((SpongeArgumentCommandNode<?>) commandNode).createBuilderForSuggestions(rootSuggestion, commandNodeToSuggestionNode);
        }

        return commandNode.createBuilder();
    }

    @SuppressWarnings("unchecked")
    @Redirect(method = "commandSourceNodesToSuggestionNodes", at =
        @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", remap = false))
    private <K, V> V impl$preventPutIntoMapIfNodeIsComplex(final Map<K, V> map,
            final K key,
            final V value,
            final CommandNode<CommandSource> rootCommandSource,
            final CommandNode<ISuggestionProvider> rootSuggestion,
            final CommandSource source,
            final Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> commandNodeToSuggestionNode) {
        // This may have been done in impl$createArgumentBuilder
        if (!map.containsKey(key)) {
            // done here because this check is applicable
            final ServerPlayerEntity e = (ServerPlayerEntity) source.getEntity();
            final Map<CommandNode<CommandSource>,List<CommandNode<ISuggestionProvider>>> playerNodes = this.impl$playerNodeCache.get(e);
            if (!playerNodes.containsKey(key)) {
                final List<CommandNode<ISuggestionProvider>> children = new ArrayList<>();
                children.add((CommandNode<ISuggestionProvider>) value);
                playerNodes.put((CommandNode<CommandSource>) key, children);
            }

            // If the current root suggestion has already got a custom suggestion and this node has a custom suggestion,
            // we need to swap it out.
            if (value instanceof ArgumentCommandNode && this.impl$alreadyHasCustomSuggestionsOnNode(rootSuggestion)) {
                rootSuggestion.addChild(this.impl$cloneArgumentCommandNode((ArgumentCommandNode<ISuggestionProvider, ?>) value));
            } else {
                rootSuggestion.addChild((CommandNode<ISuggestionProvider>) value);
            }
            return map.put(key, value);
        }
        return null; // it's ignored anyway.
    }

    @Redirect(method = "commandSourceNodesToSuggestionNodes",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/brigadier/tree/CommandNode;addChild(Lcom/mojang/brigadier/tree/CommandNode;)V",
                    remap = false))
    private void impl$preventAddChild(final CommandNode<ISuggestionProvider> root, final CommandNode<ISuggestionProvider> newChild) {
        // no-op, we did this above.
    }

    @Redirect(method = "commandSourceNodesToSuggestionNodes",
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/tree/CommandNode;canUse(Ljava/lang/Object;)Z", remap = false))
    private boolean impl$testPermissionAndPreventRecalculationWhenSendingNodes(
            final CommandNode<CommandSource> commandNode,
            final Object source,
            final CommandNode<CommandSource> rootCommandNode,
            final CommandNode<ISuggestionProvider> rootSuggestion,
            final CommandSource sourceButTyped,
            final Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> commandNodeToSuggestionNode
    ) {
        if (SpongeNodePermissionCache.canUse(
                rootCommandNode instanceof RootCommandNode, this.shadow$getDispatcher(), commandNode, sourceButTyped)) {

            boolean shouldContinue = true;
            if (commandNode instanceof SpongeArgumentCommandNode && ((SpongeArgumentCommandNode<?>) commandNode).isComplex()) {
                shouldContinue = false;
                final ServerPlayerEntity e = (ServerPlayerEntity) sourceButTyped.getEntity();
                final boolean hasCustomSuggestionsAlready = this.impl$alreadyHasCustomSuggestionsOnNode(rootSuggestion);
                final CommandNode<ISuggestionProvider> finalCommandNode = ((SpongeArgumentCommandNode<?>) commandNode).getComplexSuggestions(
                        rootSuggestion,
                        commandNodeToSuggestionNode,
                        this.impl$playerNodeCache.get(e),
                        !hasCustomSuggestionsAlready
                );
                if (!this.impl$getChildrenFromNode(commandNode).isEmpty()) {
                    this.shadow$commandSourceNodesToSuggestionNodes(commandNode, finalCommandNode, sourceButTyped, commandNodeToSuggestionNode);
                }
            }

            if (shouldContinue) {
                // If we've already created the node, then just attach what we already have.
                final ServerPlayerEntity e = (ServerPlayerEntity) sourceButTyped.getEntity();
                final List<CommandNode<ISuggestionProvider>> suggestionProviderCommandNode = this.impl$playerNodeCache.get(e).get(commandNode);
                if (suggestionProviderCommandNode != null) {
                    shouldContinue = false;
                    boolean hasCustomSuggestionsAlready = this.impl$alreadyHasCustomSuggestionsOnNode(rootSuggestion);
                    for (final CommandNode<ISuggestionProvider> node : suggestionProviderCommandNode) {
                        // If we have custom suggestions, we need to limit it to one node, otherwise we trigger a bug
                        // in the client where it'll send more than one custom suggestion request - which is fine, except
                        // the client will then ignore all but one of them. This is a problem because we then end up with
                        // no suggestions - CompletableFuture.allOf(...) will contain an exception if a future is cancelled,
                        // meaning thenRun(...) does not run, which is how displaying the suggestions works...
                        //
                        // Because we don't control the client, we have to work around it here.
                        if (hasCustomSuggestionsAlready && node instanceof ArgumentCommandNode) {
                            final ArgumentCommandNode<ISuggestionProvider, ?> argNode = (ArgumentCommandNode<ISuggestionProvider, ?>) node;
                            if (argNode.getCustomSuggestions() != null) {
                                // Rebuild the node without the custom suggestions.
                                rootSuggestion.addChild(this.impl$cloneArgumentCommandNode(argNode));
                                continue;
                            }
                        } else if (node instanceof ArgumentCommandNode && ((ArgumentCommandNode<?, ?>) node).getCustomSuggestions() != null) {
                            hasCustomSuggestionsAlready = true; // no more custom suggestions
                        }
                        rootSuggestion.addChild(node);
                    }
                }
            }
            return shouldContinue;
        }
        return false;
    }

    @Redirect(method = "send", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/Commands;commandSourceNodesToSuggestionNodes"
            + "(Lcom/mojang/brigadier/tree/CommandNode;Lcom/mojang/brigadier/tree/CommandNode;Lnet/minecraft/command/CommandSource;Ljava/util/Map;)V"))
    private void impl$addNonBrigSuggestions(
            final Commands commands,
            final CommandNode<CommandSource> p_197052_1_,
            final CommandNode<ISuggestionProvider> p_197052_2_,
            final CommandSource p_197052_3_,
            final Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> p_197052_4_,
            final ServerPlayerEntity playerEntity) {
        try {
            this.impl$playerNodeCache.put(playerEntity, new HashMap<>());
            this.shadow$commandSourceNodesToSuggestionNodes(p_197052_1_, p_197052_2_, p_197052_3_, p_197052_4_);
        } finally {
            this.impl$playerNodeCache.remove(playerEntity);
        }
        for (final CommandNode<ISuggestionProvider> node :
                ((SpongeCommandManager) Sponge.getGame().getCommandManager()).getNonBrigadierSuggestions((CommandCause) p_197052_3_)) {
            p_197052_2_.addChild(node);
        }
    }

    private Collection<CommandNode<CommandSource>> impl$getChildrenFromNode(final CommandNode<CommandSource> parentNode) {
        if (parentNode instanceof SpongeNode) {
            return ((SpongeNode) parentNode).getChildrenForSuggestions();
        }
        return parentNode.getChildren();
    }

    private ArgumentCommandNode<ISuggestionProvider, ?> impl$cloneArgumentCommandNode(final ArgumentCommandNode<ISuggestionProvider, ?> toClone) {
        final RequiredArgumentBuilder<ISuggestionProvider, ?> builder = toClone.createBuilder();
        builder.suggests(null);
        for (final CommandNode<ISuggestionProvider> node : toClone.getChildren()) {
            builder.then(node);
        }
        return builder.build();
    }

    private boolean impl$alreadyHasCustomSuggestionsOnNode(final CommandNode<ISuggestionProvider> rootSuggestion) {
        return rootSuggestion.getChildren()
                .stream()
                .filter(x -> x instanceof ArgumentCommandNode)
                .anyMatch(x -> ((ArgumentCommandNode<?, ?>) x).getCustomSuggestions() != null);
    }

}
