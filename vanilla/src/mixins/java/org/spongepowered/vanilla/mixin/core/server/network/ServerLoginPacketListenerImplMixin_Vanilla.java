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
package org.spongepowered.vanilla.mixin.core.server.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.network.protocol.login.ServerboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.objectweb.asm.Opcodes;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.network.EngineConnection;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.server.network.ServerLoginPacketListenerImplBridge;
import org.spongepowered.common.network.channel.ConnectionUtil;
import org.spongepowered.common.network.channel.SpongeChannelManager;
import org.spongepowered.common.network.channel.TransactionStore;

import javax.annotation.Nullable;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin_Vanilla implements ServerLoginPacketListener, ServerLoginPacketListenerImplBridge {

    // @formatter:off
    @Shadow @Final MinecraftServer server;
    @Shadow ServerLoginPacketListenerImpl.State state;

    // Handshake phase:
    // 1. Sync registered plugin channels
    // 2. Post handshake event and plugins can start sending login payloads
    // 3. Wait until the client responded for each of the plugin' requests
    @Shadow @Nullable GameProfile gameProfile;
    @Shadow protected abstract GameProfile shadow$createFakeProfile(GameProfile $$0);

    //@formatter:on

    private static final int HANDSHAKE_NOT_STARTED = 0;
    private static final int HANDSHAKE_CLIENT_TYPE = 1;
    private static final int HANDSHAKE_SYNC_CHANNEL_REGISTRATIONS = 2;
    private static final int HANDSHAKE_CHANNEL_REGISTRATION = 3;
    private static final int HANDSHAKE_SYNC_PLUGIN_DATA = 4;

    private int impl$handshakeState = ServerLoginPacketListenerImplMixin_Vanilla.HANDSHAKE_NOT_STARTED;

    @Inject(method = "handleCustomQueryPacket", at = @At("HEAD"), cancellable = true)
    private void onResponsePayload(final ServerboundCustomQueryPacket packet, final CallbackInfo ci) {
        ci.cancel();

        final SpongeChannelManager channelRegistry = (SpongeChannelManager) Sponge.channelManager();
        this.server.execute(() -> channelRegistry.handleLoginResponsePayload((EngineConnection) this, packet));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void impl$onTick(final CallbackInfo ci) {
        if (this.state == ServerLoginPacketListenerImpl.State.NEGOTIATING) {
            final ServerSideConnection connection = (ServerSideConnection) this;
            if (this.impl$handshakeState == ServerLoginPacketListenerImplMixin_Vanilla.HANDSHAKE_NOT_STARTED) {
                this.impl$handshakeState = ServerLoginPacketListenerImplMixin_Vanilla.HANDSHAKE_CLIENT_TYPE;

                ((SpongeChannelManager) Sponge.channelManager()).requestClientType(connection).thenAccept(result -> {
                    this.impl$handshakeState = ServerLoginPacketListenerImplMixin_Vanilla.HANDSHAKE_SYNC_CHANNEL_REGISTRATIONS;
                });

            } else if (this.impl$handshakeState == ServerLoginPacketListenerImplMixin_Vanilla.HANDSHAKE_SYNC_CHANNEL_REGISTRATIONS) {
                this.impl$handshakeState = ServerLoginPacketListenerImplMixin_Vanilla.HANDSHAKE_CHANNEL_REGISTRATION;

                ((SpongeChannelManager) Sponge.channelManager()).sendLoginChannelRegistry(connection).thenAccept(result -> {
                    final Cause cause = Cause.of(EventContext.empty(), this);
                    final ServerSideConnectionEvent.Handshake event =
                            SpongeEventFactory.createServerSideConnectionEventHandshake(cause, connection);
                    SpongeCommon.post(event);
                    this.impl$handshakeState = ServerLoginPacketListenerImplMixin_Vanilla.HANDSHAKE_SYNC_PLUGIN_DATA;
                });
            } else if (this.impl$handshakeState == ServerLoginPacketListenerImplMixin_Vanilla.HANDSHAKE_SYNC_PLUGIN_DATA) {
                final TransactionStore store = ConnectionUtil.getTransactionStore(connection);
                if (store.isEmpty()) {
                    this.state = ServerLoginPacketListenerImpl.State.READY_TO_ACCEPT;
                }
            }
        }
    }

    @Inject(method = "handleHello", at = @At("RETURN"))
    private void impl$onProcessLoginStart(final ServerboundHelloPacket packet, final CallbackInfo ci) {
        if (this.state == ServerLoginPacketListenerImpl.State.READY_TO_ACCEPT) {
            this.state = ServerLoginPacketListenerImpl.State.NEGOTIATING;
        }
    }

    @Inject(method = "handleHello(Lnet/minecraft/network/protocol/login/ServerboundHelloPacket;)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/server/network/ServerLoginPacketListenerImpl$State;READY_TO_ACCEPT:Lnet/minecraft/server/network/ServerLoginPacketListenerImpl$State;",
            opcode = Opcodes.GETSTATIC),
        cancellable = true)
    private void vanilla$fireAuthEventOffline(final CallbackInfo ci) {
        // Move this check up here, so that the UUID isn't null when we fire the event
        if (!this.gameProfile.isComplete()) {
            this.gameProfile = this.shadow$createFakeProfile(this.gameProfile);
        }

        if (this.bridge$fireAuthEvent()) {
            ci.cancel();
        }
    }
}
