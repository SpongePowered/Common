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

import com.llamalad7.mixinextras.sugar.Cancellable;
import com.mojang.authlib.GameProfile;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.resource.ResourcePackCallback;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.network.EngineConnection;
import org.spongepowered.api.network.EngineConnectionState;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.network.ConnectionBridge;
import org.spongepowered.common.bridge.network.protocol.game.ClientboundResourcePackPacketBridge;
import org.spongepowered.common.bridge.server.network.ServerCommonPacketListenerImplBridge;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.network.channel.SpongeChannelManager;
import org.spongepowered.common.network.channel.SpongeChannelPayload;
import org.spongepowered.common.profile.SpongeGameProfile;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin implements ServerCommonPacketListenerImplBridge {

    // @formatter:off
    @Shadow @Final protected Connection connection;
    @Shadow @Final protected MinecraftServer server;
    @Shadow public abstract void shadow$send(final Packet<?> $$0);
    @Shadow public abstract void shadow$disconnect(Component reason);
    @Shadow protected abstract GameProfile shadow$playerProfile();
    // @formatter:on

    private Map<UUID, ResourcePackInfo> impl$resourcePackInfos = new ConcurrentHashMap<>();
    private Map<UUID, ResourcePackCallback> impl$resourcePackCallbacks = new ConcurrentHashMap<>();

    @ModifyVariable(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private @Nullable Packet<?> impl$onClientboundPacketSend(final Packet<?> packet, @Cancellable CallbackInfo ci) {
        final @Nullable Packet<?> modifiedPacket = this.impl$modifyClientBoundPacket(packet);
        if (modifiedPacket == null) {
            ci.cancel();
        }
        return modifiedPacket;
    }

    public @Nullable Packet<?> impl$modifyClientBoundPacket(final Packet<?> packet) {
        if (packet instanceof ClientboundResourcePackPushPacket packPacket) {
            this.impl$resourcePackInfos.put(packPacket.id(), ((ClientboundResourcePackPacketBridge) (Object) packPacket).bridge$getPackInfo());
        }
        return packet;
    }

    @Inject(method = "handleResourcePackResponse", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
            shift = At.Shift.AFTER))
    private void impl$onHandleResourcePackResponse(final ServerboundResourcePackPacket $$0, final CallbackInfo ci) {
        final @Nullable ResourcePackInfo pack = this.impl$resourcePackInfos.get($$0.id());
        if (pack == null) {
            return;
        }

        final @Nullable ServerPlayer player;
        if ((Object) this instanceof ServerGamePacketListenerImpl gamePacketListener) {
            player = gamePacketListener.player;
        } else {
            player = null;
        }

        final ResourcePackStatus status = switch ($$0.action()) {
            case ACCEPTED -> ResourcePackStatus.ACCEPTED;
            case DECLINED -> ResourcePackStatus.DECLINED;
            case INVALID_URL -> ResourcePackStatus.INVALID_URL;
            case FAILED_DOWNLOAD -> ResourcePackStatus.FAILED_DOWNLOAD;
            case DOWNLOADED -> ResourcePackStatus.DOWNLOADED;
            case FAILED_RELOAD -> ResourcePackStatus.FAILED_RELOAD;
            case DISCARDED -> ResourcePackStatus.DISCARDED;
            case SUCCESSFULLY_LOADED -> ResourcePackStatus.SUCCESSFULLY_LOADED;
            default -> throw new AssertionError();
        };

        final @Nullable ResourcePackCallback callback = this.impl$resourcePackCallbacks.get($$0.id());
        if (callback != null && player != null) {
            callback.packEventReceived($$0.id(), status, (Audience) player);
        }

        if ($$0.action().isTerminal()) {
            this.impl$resourcePackInfos.remove($$0.id());
            this.impl$resourcePackCallbacks.remove($$0.id());
        }

        SpongeCommon.post(SpongeEventFactory.createResourcePackStatusEvent(
                PhaseTracker.getCauseStackManager().currentCause(),
                (ServerSideConnection) ((ConnectionBridge) this.connection).bridge$getEngineConnection(),
                pack,
                Optional.ofNullable((org.spongepowered.api.entity.living.player.server.ServerPlayer) player),
                SpongeGameProfile.of(this.shadow$playerProfile()),
                status
        ));
    }

    @Override
    public void bridge$sendResourcePacks(final @NonNull ResourcePackRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.replace()) {
            this.bridge$clearResourcePacks();
        }
        final Optional<Component> prompt = SpongeAdventure.asVanillaOpt(request.prompt());
        for (final ResourcePackInfo pack : request.packs()) {
            final ClientboundResourcePackPushPacket packet =
                    new ClientboundResourcePackPushPacket(pack.id(), pack.uri().toASCIIString(), pack.hash(), request.required(), prompt);
            ((ClientboundResourcePackPacketBridge) (Object) packet).bridge$setPackInfo(pack);
            if (request.callback() != ResourcePackCallback.noOp()) {
                this.impl$resourcePackCallbacks.put(pack.id(), request.callback());
            }
            this.shadow$send(packet);
        }
    }

    @Override
    public void bridge$removeResourcePacks(final @NonNull UUID id, final @NonNull UUID @NonNull... others) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(id, "others");
        this.shadow$send(new ClientboundResourcePackPopPacket(Optional.of(id)));
        for (final UUID other : others) {
            this.shadow$send(new ClientboundResourcePackPopPacket(Optional.of(other)));
        }
    }

    @Override
    public void bridge$clearResourcePacks() {
        this.shadow$send(new ClientboundResourcePackPopPacket(Optional.empty()));
    }

    @Inject(method = "handleCustomPayload", at = @At(value = "HEAD"), cancellable = true)
    private void impl$onHandleCustomPayload(final ServerboundCustomPayloadPacket packet, final CallbackInfo ci) {
        if (packet.payload() instanceof final SpongeChannelPayload payload) {
            this.server.execute(() -> this.impl$handleSpongePayload(payload));

            ci.cancel();
        }
    }

    protected void impl$handleSpongePayload(final SpongeChannelPayload payload) {
        final SpongeChannelManager channelRegistry = (SpongeChannelManager) Sponge.channelManager();
        final EngineConnection connection = ((ConnectionBridge) this.connection).bridge$getEngineConnection();
        channelRegistry.handlePlayPayload(connection, (EngineConnectionState) this, payload.id(), payload.consumer());
    }
}
