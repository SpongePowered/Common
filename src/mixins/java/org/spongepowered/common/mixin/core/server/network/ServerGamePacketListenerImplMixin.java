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
package org.spongepowered.common.mixin.core.server.network;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockBreakAckPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.value.ListValue;
import org.spongepowered.api.entity.living.Humanoid;
import org.spongepowered.api.entity.living.player.PlayerChatFormatter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.entity.ChangeSignEvent;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.RotateEntityEvent;
import org.spongepowered.api.event.entity.living.AnimateHandEvent;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.network.protocol.game.ServerboundMovePlayerPacketAccessor;
import org.spongepowered.common.accessor.server.level.ServerPlayerGameModeAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.network.ConnectionHolderBridge;
import org.spongepowered.common.bridge.server.level.ServerPlayerBridge;
import org.spongepowered.common.bridge.server.network.ServerGamePacketListenerImplBridge;
import org.spongepowered.common.command.manager.SpongeCommandManager;
import org.spongepowered.common.command.registrar.BrigadierBasedRegistrar;
import org.spongepowered.common.data.value.ImmutableSpongeListValue;
import org.spongepowered.common.entity.player.tab.SpongeTabList;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.packet.BasicPacketContext;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhase;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.CommandUtil;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin implements ServerGamePacketListenerImplBridge, ConnectionHolderBridge {

    // @formatter:off
    @Shadow @Final public Connection connection;
    @Shadow public net.minecraft.server.level.ServerPlayer player;
    @Shadow @Final private MinecraftServer server;
    @Shadow private int receivedMovePacketCount;
    @Shadow private int knownMovePacketCount;
    @Shadow private int chatSpamTickCount;

    @Shadow public abstract void shadow$teleport(double x, double y, double z, float yaw, float pitch, Set<ClientboundPlayerPositionPacket.RelativeArgument> relativeArguments);
    @Shadow protected abstract void shadow$filterTextPacket(List<String> p_244537_1_, Consumer<List<String>> p_244537_2_);
    @Shadow public abstract void shadow$resetPosition();
    // @formatter:on

    private int impl$ignorePackets;

    @Override
    public Connection bridge$getConnection() {
        return this.connection;
    }

    @Inject(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V",
        at = @At("HEAD")
    )
    private void impl$onClientboundPacketSend(final Packet<?> packet, final GenericFutureListener<? extends Future<? super Void>> listener, final CallbackInfo ci) {
        if (packet instanceof ClientboundPlayerInfoPacket) {
            ((SpongeTabList) ((ServerPlayer) this.player).tabList()).updateEntriesOnSend((ClientboundPlayerInfoPacket) packet);
        }
    }

    @Inject(method = "handleCustomCommandSuggestions", at = @At(value = "NEW", target = "com/mojang/brigadier/StringReader", remap = false),
            cancellable = true)
    private void impl$getSuggestionsFromNonBrigCommand(final ServerboundCommandSuggestionPacket packet, final CallbackInfo ci) {
        final String rawCommand = packet.getCommand();
        final String[] command = CommandUtil.extractCommandString(rawCommand);
        final CommandCause cause = CommandCause.create();
        final SpongeCommandManager manager = SpongeCommandManager.get(this.server);
        if (!rawCommand.contains(" ")) {
            final SuggestionsBuilder builder = new SuggestionsBuilder(command[0], 0);
            if (command[0].isEmpty()) {
                manager.getAliasesForCause(cause).forEach(builder::suggest);
            } else {
                manager.getAliasesThatStartWithForCause(cause, command[0]).forEach(builder::suggest);
            }
            this.connection.send(new ClientboundCommandSuggestionsPacket(packet.getId(), builder.build()));
            ci.cancel();
        } else {
            final Optional<CommandMapping> mappingOptional =
                    manager.commandMapping(command[0].toLowerCase(Locale.ROOT))
                            .filter(x -> !(x.registrar() instanceof BrigadierBasedRegistrar));
            if (mappingOptional.isPresent()) {
                final CommandMapping mapping = mappingOptional.get();
                if (mapping.registrar().canExecute(cause, mapping)) {
                    final SuggestionsBuilder builder = CommandUtil.createSuggestionsForRawCommand(rawCommand, command, cause, mapping);
                    this.connection.send(new ClientboundCommandSuggestionsPacket(packet.getId(), builder.build()));
                } else {
                    this.connection.send(new ClientboundCommandSuggestionsPacket(packet.getId(), Suggestions.empty().join()));
                }
                ci.cancel();
            }
        }
    }

    @Redirect(method = "handleCustomCommandSuggestions",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/brigadier/CommandDispatcher;parse(Lcom/mojang/brigadier/StringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/ParseResults;",
                    remap = false
            )
    )
    private ParseResults<CommandSourceStack> impl$informParserThisIsASuggestionCheck(final CommandDispatcher<CommandSourceStack> commandDispatcher,
            final StringReader command,
            final Object source) {
        return SpongeCommandManager.get(this.server).getDispatcher().parse(command, (CommandSourceStack) source, true);
    }

    /**
     * Specifically hooks the reach distance to use the forge hook.
     */
    @ModifyConstant(
            method = "handleInteract",
            constant = @Constant(doubleValue = 36.0D)
    )
    private double impl$getPlatformReach(final double thirtySix, ServerboundInteractPacket p_147340_1_) {
        return PlatformHooks.INSTANCE.getGeneralHooks().getEntityReachDistanceSq(this.player, p_147340_1_.getTarget(this.player.level));
    }

    @Inject(method = "handleMovePlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isPassenger()Z"),
            cancellable = true
    )
    private void impl$callMoveEntityEvent(final ServerboundMovePlayerPacket packetIn, final CallbackInfo ci) {
        final ServerboundMovePlayerPacketAccessor packetInAccessor = (ServerboundMovePlayerPacketAccessor) packetIn;

        // receivedMovePacketCount will be incremented later
        final boolean goodMovementPacket = (this.receivedMovePacketCount + 1) - this.knownMovePacketCount <= 5;
        final boolean fireMoveEvent = goodMovementPacket && packetInAccessor.accessor$hasPos() && ShouldFire.MOVE_ENTITY_EVENT;
        final boolean fireRotationEvent = goodMovementPacket && packetInAccessor.accessor$hasRot() && ShouldFire.ROTATE_ENTITY_EVENT;

        // During login, minecraft sends a packet containing neither the 'moving' or 'rotating' flag set - but only once.
        // We don't fire an event to avoid confusing plugins.
        if (!fireMoveEvent && !fireRotationEvent) {
            return;
        }

        final ServerPlayer player = (ServerPlayer) this.player;
        final Vector3d fromRotation = player.rotation();
        final Vector3d fromPosition = player.position();

        Vector3d toPosition = new Vector3d(packetIn.getX(this.player.getX()),
                packetIn.getY(this.player.getY()), packetIn.getZ(this.player.getZ()));
        Vector3d toRotation = new Vector3d(packetIn.getYRot(this.player.yRot),
                packetIn.getXRot(this.player.xRot), 0);

        final boolean significantRotation = fromRotation.distanceSquared(toRotation) > (.15f * .15f);

        final Vector3d originalToPosition = toPosition;
        final Vector3d originalToRotation = toRotation;

        boolean cancelMovement = false;
        // Call move & rotate event as needed...
        if (fireMoveEvent) {
            PhaseTracker.getCauseStackManager().addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.NATURAL);
            final MoveEntityEvent event = SpongeEventFactory.createMoveEntityEvent(PhaseTracker.getCauseStackManager().currentCause(), player, fromPosition,
                    toPosition, toPosition);
            if (SpongeCommon.post(event)) {
                cancelMovement = true;
            } else {
                toPosition = event.destinationPosition();
            }
            PhaseTracker.getCauseStackManager().removeContext(EventContextKeys.MOVEMENT_TYPE);
        }

        if (significantRotation && fireRotationEvent) {
            final RotateEntityEvent event = SpongeEventFactory.createRotateEntityEvent(PhaseTracker.getCauseStackManager().currentCause(), player, fromRotation,
                    toRotation);
            if (SpongeCommon.post(event)) {
                toRotation = fromRotation;
            } else {
                toRotation = event.toRotation();
            }
        }

        // At this point, we cancel out and let the "confirmed teleport" code run through to update the
        // player position and update the player's relation in the chunk manager.
        if (cancelMovement) {
            // This will both cancel the movement and notify the client about the new rotation if any.
            // The position is absolute so the momentum will be reset by the client.
            // The rotation is relative so the head movement is still smooth.
            // The client thinks its current rotation is originalToRotation so the new rotation is relative to that.
            this.player.absMoveTo(fromPosition.x(), fromPosition.y(), fromPosition.z(),
                    (float) originalToRotation.x(), (float) originalToRotation.y());
            this.shadow$teleport(fromPosition.x(), fromPosition.y(), fromPosition.z(),
                    (float) toRotation.x(), (float) toRotation.y(),
                    EnumSet.of(ClientboundPlayerPositionPacket.RelativeArgument.X_ROT, ClientboundPlayerPositionPacket.RelativeArgument.Y_ROT));
            ci.cancel();
            return;
        }

        // Handle event results
        if (!toPosition.equals(originalToPosition) || !toRotation.equals(originalToRotation)) {
            // Notify the client about the new position and new rotation.
            // Both are relatives so the client will keep its momentum.
            // The client thinks its current position is originalToPosition so the new position is relative to that.
            this.player.absMoveTo(originalToPosition.x(), originalToPosition.y(), originalToPosition.z(),
                    (float) originalToRotation.x(), (float) originalToRotation.y());
            this.shadow$teleport(toPosition.x(), toPosition.y(), toPosition.z(),
                    (float) toRotation.x(), (float) toRotation.y(),
                    EnumSet.allOf(ClientboundPlayerPositionPacket.RelativeArgument.class));
            ci.cancel();
        }
    }

    @Inject(
            method = "handleInteract",
            cancellable = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;interactAt(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
            )
    )
    public void impl$onRightClickAtEntity(final ServerboundInteractPacket p_147340_1, final CallbackInfo ci) {
        final Entity entity = p_147340_1.getTarget(this.player.getLevel());
        final ItemStack itemInHand = p_147340_1.getHand() == null ? ItemStack.EMPTY : this.player.getItemInHand(p_147340_1.getHand());
        final InteractEntityEvent.Secondary event = SpongeCommonEventFactory
                .callInteractEntityEventSecondary(this.player, itemInHand, entity, p_147340_1.getHand(), VecHelper.toVector3d(p_147340_1.getLocation()));
        if (event.isCancelled()) {
            ci.cancel();
        } else {
            this.impl$ignorePackets++;
        }
    }

    @Inject(
            method = "handleInteract",
            cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;attack(Lnet/minecraft/world/entity/Entity;)V")
    )
    public void impl$onLeftClickEntity(final ServerboundInteractPacket p_147340_1_, final CallbackInfo ci) {
        final Entity entity = p_147340_1_.getTarget(this.player.getLevel());

        final InteractEntityEvent.Primary event = SpongeCommonEventFactory.callInteractEntityEventPrimary(this.player,
                this.player.getItemInHand(this.player.getUsedItemHand()), entity, this.player.getUsedItemHand());
        if (event.isCancelled()) {
            ci.cancel();
        } else {
            this.impl$ignorePackets++;
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "handleAnimate",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"),
            cancellable = true)
    private void impl$throwAnimationAndInteractEvents(final ServerboundSwingPacket packetIn, final CallbackInfo ci) {
        if (PhaseTracker.getInstance().getPhaseContext().isEmpty()) {
            return;
        }
        final InteractionHand hand = packetIn.getHand();

        if (!((ServerPlayerGameModeAccessor) this.player.gameMode).accessor$isDestroyingBlock()) {
            if (this.impl$ignorePackets > 0) {
                this.impl$ignorePackets--;
            } else {
                if (ShouldFire.INTERACT_ITEM_EVENT_PRIMARY) {
                    final Vec3 startPos = this.player.getEyePosition(1);
                    final Vec3 endPos = startPos.add(this.player.getLookAngle().scale(5d)); // TODO hook for blockReachDistance?
                    HitResult result = this.player.getLevel().clip(new ClipContext(startPos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.player));
                    if (result.getType() == HitResult.Type.MISS) {
                        final ItemStack heldItem = this.player.getItemInHand(hand);
                        SpongeCommonEventFactory.callInteractItemEventPrimary(this.player, heldItem, hand);
                    }
                }
            }
        }

        if (ShouldFire.ANIMATE_HAND_EVENT) {
            final HandType handType = (HandType) (Object) hand;
            final ItemStack heldItem = this.player.getItemInHand(hand);

            try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
                frame.addContext(EventContextKeys.USED_ITEM, ItemStackUtil.snapshotOf(heldItem));
                frame.addContext(EventContextKeys.USED_HAND, handType);
                final AnimateHandEvent event =
                        SpongeEventFactory.createAnimateHandEvent(frame.currentCause(), handType, (Humanoid) this.player);
                if (SpongeCommon.post(event)) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;drop(Z)Z"))
    public void impl$dropItem(final ServerboundPlayerActionPacket p_147345_1_, final CallbackInfo ci) {
        this.impl$ignorePackets++;
    }

    @Redirect(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;handleBlockBreakAction(Lnet/minecraft/core/BlockPos;Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;Lnet/minecraft/core/Direction;I)V"))
    public void impl$callInteractBlockPrimaryEvent(final ServerPlayerGameMode playerInteractionManager, final BlockPos p_225416_1_,
            final ServerboundPlayerActionPacket.Action p_225416_2_, final Direction p_225416_3_, final int p_225416_4_) {
        final BlockSnapshot snapshot = ((org.spongepowered.api.world.server.ServerWorld) (playerInteractionManager.level)).createSnapshot(VecHelper.toVector3i(p_225416_1_));
        final InteractBlockEvent.Primary event = SpongeCommonEventFactory.callInteractBlockEventPrimary(p_225416_2_, this.player, this.player.getItemInHand(
                InteractionHand.MAIN_HAND), snapshot, InteractionHand.MAIN_HAND, p_225416_3_);
        if (event instanceof Cancellable && ((Cancellable) event).isCancelled()) {
            this.player.connection.send(new ClientboundBlockBreakAckPacket(p_225416_1_, playerInteractionManager.level.getBlockState(p_225416_1_), p_225416_2_, false, "block action restricted"));
            this.impl$ignorePackets++;
        } else {
            if (p_225416_2_ == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
                if (!Objects.equals(((ServerPlayerGameModeAccessor) playerInteractionManager).accessor$destroyPos(), p_225416_1_)) {
                    return; // prevents Mismatch in destroy block pos warning
                }
            }
            playerInteractionManager.handleBlockBreakAction(p_225416_1_, p_225416_2_, p_225416_3_, p_225416_4_);
            if (p_225416_2_ == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                this.impl$ignorePackets++;
            }
        }
    }

    @Redirect(method = "onDisconnect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V"))
    public void impl$handlePlayerDisconnect(final PlayerList playerList, final net.minecraft.network.chat.Component component, final ChatType chatType, UUID uuid) {
        // If this happens, the connection has not been fully established yet so we've kicked them during ClientConnectionEvent.Login,
        // but FML has created this handler earlier to send their handshake. No message should be sent, no disconnection event should
        // be fired either.
        if (this.player.connection == null) {
            return;
        }
        final ServerPlayer spongePlayer = (ServerPlayer) this.player;

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(this.player);
            final Component message = SpongeAdventure.asAdventure(component);
            Audience audience = Sponge.server().broadcastAudience();
            final ServerSideConnectionEvent.Disconnect event = SpongeEventFactory.createServerSideConnectionEventDisconnect(
                    PhaseTracker.getCauseStackManager().currentCause(), audience, Optional.of(audience), message, message,
                    spongePlayer.connection(), spongePlayer);
            SpongeCommon.post(event);
            event.audience().ifPresent(a -> a.sendMessage(spongePlayer, event.message()));
        }

        ((ServerPlayerBridge) this.player).bridge$getWorldBorderListener().onPlayerDisconnect();
    }

    @Redirect(method = "handleSignUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;filterTextPacket(Ljava/util/List;Ljava/util/function/Consumer;)V"))
    private void impl$switchToSignPhaseState(ServerGamePacketListenerImpl serverPlayNetHandler, List<String> p_244537_1_, Consumer<List<String>> p_244537_2_) {
        try (final BasicPacketContext context = PacketPhase.General.UPDATE_SIGN.createPhaseContext(PhaseTracker.getInstance())
                .packetPlayer(this.player)
                .buildAndSwitch()
        ) {

            this.shadow$filterTextPacket(p_244537_1_, p_244537_2_);
        }
    }

    @Redirect(method = "updateSignText", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/List;size()I"))
    private int impl$callChangeSignEvent(final List<String> list, ServerboundSignUpdatePacket p_244542_1_, List<String> p_244542_2_) {
        final SignBlockEntity blockEntity = (SignBlockEntity) this.player.level.getBlockEntity(p_244542_1_.getPos());
        final ListValue<Component> originalLinesValue = ((Sign) blockEntity).getValue(Keys.SIGN_LINES)
                .orElseGet(() -> new ImmutableSpongeListValue<>(Keys.SIGN_LINES, ImmutableList.of()));

        final List<Component> newLines = new ArrayList<>();
        for (final String line : list) {
            // Sponge Start - While Vanilla does some strip formatting, it doesn't catch everything. This patches an exploit that allows color
            // signs to be created.
            newLines.add(Component.text(SharedConstants.filterText(line)));
        }

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(this.player);
            final ListValue.Mutable<Component> newLinesValue = ListValue.mutableOf(Keys.SIGN_LINES, newLines);
            final ChangeSignEvent event = SpongeEventFactory.createChangeSignEvent(PhaseTracker.getCauseStackManager().currentCause(),
                    originalLinesValue.asImmutable(), newLinesValue,
                    (Sign) blockEntity);
            final ListValue<Component> toApply = SpongeCommon.post(event) ? originalLinesValue : newLinesValue;
            ((Sign) blockEntity).offer(toApply);
        }

        return 0;
    }

    @Override
    public void bridge$captureCurrentPlayerPosition() {
        this.shadow$resetPosition();
    }

    @Redirect(method = "handleChat(Ljava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V") )
    private void impl$postChatMessageEventAndSend(final PlayerList playerList, final net.minecraft.network.chat.Component component, final ChatType chatType, final UUID playerUUID, final String initialMessage) {
        final ServerPlayer player = (ServerPlayer) this.player;
        final PlayerChatFormatter chatFormatter = player.chatFormatter();
        Component currentMessage = Component.text(initialMessage);
        if (component instanceof TranslatableComponent && ((TranslatableComponent) component).getArgs().length == 2) {
            if (((TranslatableComponent) component).getArgs()[1] instanceof net.minecraft.network.chat.Component) {
                currentMessage = SpongeAdventure.asAdventure((net.minecraft.network.chat.Component) ((TranslatableComponent) component).getArgs()[1]);
            } else if (((TranslatableComponent) component).getArgs()[1] instanceof String) {
                currentMessage = Component.text((String) ((TranslatableComponent) component).getArgs()[1]);
            }
        }

        try (final CauseStackManager.StackFrame frame = PhaseTracker.SERVER.pushCauseFrame()) {
            frame.pushCause(this.player);
            final Audience audience = (Audience) this.server;
            // Forge's event is accounted for in here.
            final PlayerChatEvent event = SpongeEventFactory.createPlayerChatEvent(frame.currentCause(), audience, Optional.of(audience), chatFormatter, Optional.of(chatFormatter), currentMessage, currentMessage);
            if (SpongeCommon.post(event)) {
                // We reduce the chatSpamTickCount by 20 to account for the fact we cancelled the event (the method increments by 20).
                // Otherwise, we need do nothing.
                this.chatSpamTickCount -= 20;
            } else {
                event.chatFormatter().ifPresent(formatter ->
                        event.audience().map(SpongeAdventure::unpackAudiences).ifPresent(targets -> {
                            for (final Audience target : targets) {
                                formatter.format(player, target, event.message(), event.originalMessage()).ifPresent(formattedMessage ->
                                        target.sendMessage(player, formattedMessage));
                            }
                        })
                );
            }
        }
    }


}
