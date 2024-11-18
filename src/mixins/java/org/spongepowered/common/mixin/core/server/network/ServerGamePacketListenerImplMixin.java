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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.value.ListValue;
import org.spongepowered.api.entity.living.Humanoid;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.entity.ChangeSignEvent;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.entity.living.AnimateHandEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.network.protocol.game.ServerboundMovePlayerPacketAccessor;
import org.spongepowered.common.accessor.network.protocol.game.ServerboundMoveVehiclePacketAccessor;
import org.spongepowered.common.accessor.server.level.ServerPlayerGameModeAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.server.level.ServerPlayerBridge;
import org.spongepowered.common.bridge.server.network.ServerGamePacketListenerImplBridge;
import org.spongepowered.common.command.manager.SpongeCommandManager;
import org.spongepowered.common.command.registrar.BrigadierBasedRegistrar;
import org.spongepowered.common.entity.player.tab.SpongeTabList;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.packet.BasicPacketContext;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhase;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.network.channel.SpongeChannelPayload;
import org.spongepowered.common.profile.SpongeGameProfile;
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
import java.util.concurrent.CompletableFuture;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin extends ServerCommonPacketListenerImplMixin implements ServerGamePacketListenerImplBridge {

    // @formatter:off
    @Shadow public net.minecraft.server.level.ServerPlayer player;
    @Shadow private double vehicleFirstGoodX;
    @Shadow private double vehicleFirstGoodY;
    @Shadow private double vehicleFirstGoodZ;

    @Shadow public abstract void shadow$teleport(PositionMoveRotation pitch, Set<Relative> relativeArguments);
    @Shadow protected abstract CompletableFuture<List<FilteredText>> shadow$filterTextPacket(final List<String> $$0);
    @Shadow protected abstract void shadow$performUnsignedChatCommand(final String $$0);
    @Shadow protected abstract void shadow$performSignedChatCommand(ServerboundChatCommandSignedPacket $$0, LastSeenMessages $$1);
    @Shadow protected abstract ParseResults<CommandSourceStack> shadow$parseCommand(final String $$0);
    // @formatter:on

    private int impl$ignorePackets;

    @Override
    public void impl$modifyClientBoundPacket(final Packet<?> packet) {
        super.impl$modifyClientBoundPacket(packet);
        if (packet instanceof ClientboundPlayerInfoUpdatePacket infoPacket) {
            ((SpongeTabList) ((ServerPlayer) this.player).tabList()).updateEntriesOnSend(infoPacket);
        }
    }

    @Override
    public void bridge$incrementIgnorePackets() {
        this.impl$ignorePackets++;
    }

    @Inject(method = "handleCustomCommandSuggestions", at = @At(value = "NEW", target = "(Ljava/lang/String;)Lcom/mojang/brigadier/StringReader;", remap = false),
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

    @Inject(method = "handleMovePlayer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;getYRot(F)F"),
        cancellable = true,
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;updateAwaitingTeleport()Z"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isPassenger()Z")))
    private void impl$callMoveEntityEvent(final ServerboundMovePlayerPacket packetIn, final CallbackInfo ci) {
        final boolean fireMoveEvent = packetIn.hasPosition();
        final boolean fireRotationEvent = packetIn.hasRotation();

        // During login, minecraft sends a packet containing neither the 'moving' or 'rotating' flag set - but only once.
        // We don't fire an event to avoid confusing plugins.
        if (!fireMoveEvent && !fireRotationEvent) {
            return;
        }

        final ServerPlayer player = (ServerPlayer) this.player;
        final Vector3d fromPosition = player.position();
        final Vector3d fromRotation = player.rotation();

        final Vector3d originalToPosition = new Vector3d(packetIn.getX(this.player.getX()),
                packetIn.getY(this.player.getY()), packetIn.getZ(this.player.getZ()));
        final Vector3d originalToRotation = new Vector3d(packetIn.getXRot(this.player.getXRot()),
                packetIn.getYRot(this.player.getYRot()), 0);

        // common checks and throws are done here.
        final @Nullable Vector3d toPosition;
        if (fireMoveEvent) {
            try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
                frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.NATURAL);
                toPosition = SpongeCommonEventFactory.callMoveEvent(
                        player,
                        fromPosition,
                        originalToPosition);
            }
        } else {
            toPosition = originalToPosition;
        }

        // Rotation result
        Vector3d toRotation;
        if (fireRotationEvent) {
            toRotation = SpongeCommonEventFactory.callRotateEvent(
                    player,
                    fromRotation,
                    originalToRotation);
            if (toRotation == null) {
                toRotation = fromRotation;
            }
        } else {
            toRotation = originalToRotation;
        }

        // At this point, we cancel out and let the "confirmed teleport" code run through to update the
        // player position and update the player's relation in the chunk manager.
        if (toPosition == null) {
            // This will both cancel the movement and notify the client about the new rotation if any.
            // The position is absolute so the momentum will be reset by the client.
            // The rotation is relative so the head movement is still smooth.
            // The client thinks its current rotation is originalToRotation so the new rotation is relative to that.
            // The rotation values can be out of "valid" range so set them directly to the same value the client has.
            this.player.absMoveTo(fromPosition.x(), fromPosition.y(), fromPosition.z());
            this.player.setXRot((float) originalToRotation.x());
            this.player.setYRot((float) originalToRotation.y());
            this.shadow$teleport(new PositionMoveRotation(
                    VecHelper.toVanillaVector3d(fromPosition),
                    Vec3.ZERO,
                    (float) (toRotation.y() - originalToRotation.y()), (float) (toRotation.x() - originalToRotation.x())
                ),
                Relative.ROTATION
            );
            ci.cancel();
            return;
        }

        // Handle event results
        if (!toPosition.equals(originalToPosition)) {
            // Notify the client about the new position and new rotation.
            // Both are relatives so the client will keep its momentum.
            // The client thinks its current position is originalToPosition so the new position is relative to that.
            // The rotation values can be out of "valid" range so set them directly to the same value the client has.
            this.player.absMoveTo(originalToPosition.x(), originalToPosition.y(), originalToPosition.z());
            this.player.setXRot((float) originalToRotation.x());
            this.player.setYRot((float) originalToRotation.y());
            this.shadow$teleport(new PositionMoveRotation(
                    VecHelper.toVanillaVector3d(toPosition.sub(originalToPosition)),
                    Vec3.ZERO,
                    (float) (toRotation.y() - originalToRotation.y()), (float) (toRotation.x() - originalToRotation.x())
                ),
                Relative.ALL);
            ci.cancel();
        } else if (!toRotation.equals(originalToRotation)) {
            // Notify the client about the new rotation.
            // Both are relatives so the client will keep its momentum.
            // The rotation values can be out of "valid" range so set them directly to the same value the client has.
            this.player.setXRot((float) originalToRotation.x());
            this.player.setYRot((float) originalToRotation.y());
            this.shadow$teleport(new PositionMoveRotation(
                    Vec3.ZERO,
                    Vec3.ZERO,
                    (float) (toRotation.y() - originalToRotation.y()), (float) (toRotation.x() - originalToRotation.x())
                ),
                EnumSet.of(Relative.X, Relative.Y, Relative.Z, Relative.X_ROT, Relative.Y_ROT, Relative.DELTA_X, Relative.DELTA_Y, Relative.DELTA_Z));

            // Let MC handle the movement but override the rotation.
            ((ServerboundMovePlayerPacketAccessor) packetIn).accessor$yRot((float) toRotation.y());
            ((ServerboundMovePlayerPacketAccessor) packetIn).accessor$xRot((float) toRotation.x());
            ((ServerboundMovePlayerPacketAccessor) packetIn).accessor$hasRot(true);
        }
    }

    @Inject(
            method = "handleMoveVehicle",
            cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getControllingPassenger()Lnet/minecraft/world/entity/LivingEntity;")
    )
    private void impl$handleVehicleMoveEvent(final ServerboundMoveVehiclePacket param0, final CallbackInfo ci) {
        final ServerboundMoveVehiclePacketAccessor packet = (ServerboundMoveVehiclePacketAccessor) param0;
        final Entity rootVehicle = this.player.getRootVehicle();
        final Vector3d fromRotation = new Vector3d(rootVehicle.getYRot(), rootVehicle.getXRot(), 0);

        // Use the position of the last movement with an event or the current player position if never called
        // We need this because we ignore very small position changes as to not spam as many move events.
        final Vector3d fromPosition = VecHelper.toVector3d(rootVehicle.position());

        final Vector3d originalToPosition = new Vector3d(param0.getX(), param0.getY(), param0.getZ());
        final Vector3d originalToRotation = new Vector3d(param0.getYRot(), param0.getXRot(), 0);

        // common checks and throws are done here.
        final @Nullable Vector3d toPosition = SpongeCommonEventFactory.callMoveEvent(
                (org.spongepowered.api.entity.Entity) rootVehicle,
                fromPosition,
                originalToPosition
        );

        Vector3d toRotation = SpongeCommonEventFactory.callRotateEvent(
                (org.spongepowered.api.entity.Entity) rootVehicle,
                fromRotation,
                originalToRotation
        );
        if (toRotation == null) {
            toRotation = fromRotation;
        }

        if (toPosition == null) {
            // no point doing all that processing, just account for a potential rotation change.
            if (!fromRotation.equals(toRotation)) {
                rootVehicle.absMoveTo(rootVehicle.getX(), rootVehicle.getY(), rootVehicle.getZ(), (float) toRotation.y(), (float) toRotation.x());
            }
            this.connection.send(new ClientboundMoveVehiclePacket(rootVehicle));
            ci.cancel();
            return;
        }

        if (!toPosition.equals(originalToPosition) || !toRotation.equals(originalToRotation)) {
            // notify the client about the new position
            rootVehicle.absMoveTo(toPosition.x(), toPosition.y(), toPosition.z(), (float) toRotation.y(), (float) toRotation.x());
            this.connection.send(new ClientboundMoveVehiclePacket(rootVehicle));

            // update the packet, let MC take care of the rest.
            packet.accessor$x(toPosition.x());
            packet.accessor$y(toPosition.y());
            packet.accessor$z(toPosition.z());
            packet.accessor$yRot((float) toRotation.x());
            packet.accessor$xRot((float) toRotation.y());

            // set the first and last good position now so we don't cause the "moved too quickly" warnings.
            this.vehicleFirstGoodX = toPosition.x();
            this.vehicleFirstGoodY = toPosition.y();
            this.vehicleFirstGoodZ = toPosition.z();
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
                    final HitResult result = this.player.serverLevel().clip(new ClipContext(startPos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.player));
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

    @Redirect(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;handleBlockBreakAction(Lnet/minecraft/core/BlockPos;Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;Lnet/minecraft/core/Direction;II)V"))
    public void impl$callInteractBlockPrimaryEvent(final ServerPlayerGameMode playerInteractionManager, final BlockPos pos,
            final ServerboundPlayerActionPacket.Action act, final Direction dir, final int maxBuildHeight, final int sequence) {
        final ServerLevel level = ((ServerPlayerGameModeAccessor) playerInteractionManager).accessor$level();
        final BlockSnapshot snapshot = ((org.spongepowered.api.world.server.ServerWorld) level)
            .createSnapshot(VecHelper.toVector3i(pos));
        final InteractBlockEvent.Primary event = SpongeCommonEventFactory.callInteractBlockEventPrimary(act, this.player, this.player.getItemInHand(
                InteractionHand.MAIN_HAND), snapshot, InteractionHand.MAIN_HAND, dir);
        if (event instanceof Cancellable && ((Cancellable) event).isCancelled()) {
            this.player.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
            this.player.connection.ackBlockChangesUpTo(sequence);
            this.impl$ignorePackets++;
        } else {
            if (act == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
                if (!Objects.equals(((ServerPlayerGameModeAccessor) playerInteractionManager).accessor$destroyPos(), pos)) {
                    return; // prevents Mismatch in destroy block pos warning
                }
            }
            playerInteractionManager.handleBlockBreakAction(pos, act, dir, maxBuildHeight, sequence);
            if (act == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                this.impl$ignorePackets++;
            }
        }
    }

    @Redirect(method = "removePlayerFromWorld", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    public void impl$handlePlayerDisconnect(final PlayerList instance, final net.minecraft.network.chat.Component $$0, final boolean $$1) {
        // If this happens, the connection has not been fully established yet so we've kicked them during ClientConnectionEvent.Login,
        // but FML has created this handler earlier to send their handshake. No message should be sent, no disconnection event should
        // be fired either.
        if (this.player.connection == null) {
            return;
        }
        final ServerPlayer spongePlayer = (ServerPlayer) this.player;

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(this.player);
            final Component message = SpongeAdventure.asAdventure($$0);
            final Audience audience = Sponge.server().broadcastAudience();
            final ServerSideConnectionEvent.Leave event = SpongeEventFactory.createServerSideConnectionEventLeave(
                    PhaseTracker.getCauseStackManager().currentCause(), audience, Optional.of(audience), message, message,
                    spongePlayer.connection(), spongePlayer, SpongeGameProfile.of(this.player.getGameProfile()), false);
            SpongeCommon.post(event);
            if (!event.isMessageCancelled()) {
                event.audience().ifPresent(a -> a.sendMessage(spongePlayer, event.message()));
            }
        }

        ((ServerPlayerBridge) this.player).bridge$getWorldBorderListener().onPlayerDisconnect();
    }

    @Redirect(method = "handleSignUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;filterTextPacket(Ljava/util/List;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<List<FilteredText>> impl$switchToSignPhaseState(final ServerGamePacketListenerImpl instance, final List<String> $$0) {
        try (final BasicPacketContext context = PacketPhase.General.UPDATE_SIGN.createPhaseContext(PhaseTracker.getInstance())
                .packetPlayer(this.player)
                .buildAndSwitch()
        ) {
            return this.shadow$filterTextPacket($$0);
        }
    }

    @Redirect(method = "updateSignText",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/SignBlockEntity;updateSignText(Lnet/minecraft/world/entity/player/Player;ZLjava/util/List;)V"))
    private void impl$callChangeSignEvent(final SignBlockEntity sign, final Player player, final boolean isFrontText, final List<FilteredText> list) {
        final SignText oldText = isFrontText ? sign.getFrontText() : sign.getBackText();
        final ListValue.Immutable<Component> originalLines = ((Sign.SignText) oldText).lines().asImmutable();

        final List<Component> newLines = new ArrayList<>();
        for (final FilteredText line : list) {
            // TODO is this still needed?
            // Sponge Start - While Vanilla does some strip formatting, it doesn't catch everything. This patches an exploit that allows color
            // signs to be created.
            newLines.add(Component.text(StringUtil.filterText(line.filtered())));
        }

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(this.player);
            final ListValue.Mutable<Component> newLinesValue = ListValue.mutableOf(Keys.SIGN_LINES, newLines);
            final ChangeSignEvent event = SpongeEventFactory.createChangeSignEvent(PhaseTracker.getCauseStackManager().currentCause(),
                    originalLines, newLinesValue,
                    (Sign) sign,
                    isFrontText);
            final ListValue<Component> toApply = SpongeCommon.post(event) ? originalLines : newLinesValue;
            this.impl$updateSignText(sign, player, isFrontText, toApply.get());
        }

    }

    /**
     * Mirrors {@link SignBlockEntity#updateSignText(Player, boolean, List)}
     */
    private void impl$updateSignText(final SignBlockEntity sign, final Player player, final boolean isFrontText, final List<Component> lines) {
        if (!sign.isWaxed() && player.getUUID().equals(sign.getPlayerWhoMayEdit()) && sign.getLevel() != null) {

            if (player.isTextFilteringEnabled()) {
                // TODO text filtering?
            }
            sign.updateText(signText -> {
                for (int i = 0; i < lines.size(); i++) {
                    signText = signText.setMessage(i, SpongeAdventure.asVanilla(lines.get(i)));
                }
                return signText;
            }, isFrontText);

            sign.setAllowedPlayerEditor(null);
            sign.getLevel().sendBlockUpdated(sign.getBlockPos(), sign.getBlockState(), sign.getBlockState(), 3);
        } else {
            // TODO maybe not ignore this?
            // LOGGER.warn("Player {} just tried to change non-editable sign", $$0.getName().getString());
        }
    }

    @Redirect(method = "lambda$handleChatCommand$7", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;performUnsignedChatCommand(Ljava/lang/String;)V"))
    public void impl$onPerformChatCommand(final ServerGamePacketListenerImpl instance, final String $$0) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(this.player);
            frame.addContext(EventContextKeys.COMMAND, $$0);
            this.shadow$performUnsignedChatCommand($$0);
        }
    }

    @Redirect(method = "lambda$handleSignedChatCommand$8", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;performSignedChatCommand(Lnet/minecraft/network/protocol/game/ServerboundChatCommandSignedPacket;Lnet/minecraft/network/chat/LastSeenMessages;)V"))
    public void impl$onPerformSignedChatCommand(final ServerGamePacketListenerImpl instance, final ServerboundChatCommandSignedPacket $$0, final LastSeenMessages $$1) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(this.player);
            frame.addContext(EventContextKeys.COMMAND, $$0.command());
            this.shadow$performSignedChatCommand($$0, $$1);
        }
    }

    @Inject(method = "handleCustomPayload", at = @At(value = "HEAD"), cancellable = true)
    private void impl$onHandleCustomPayload(final ServerboundCustomPayloadPacket packet, final CallbackInfo ci) {
        if (packet.payload() instanceof final SpongeChannelPayload payload) {
            this.server.execute(() -> this.impl$handleSpongePayload(payload));

            ci.cancel();
        }
    }
}
