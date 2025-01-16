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
package org.spongepowered.common.network.channel.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.ResourceLocation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.network.ClientSideConnection;
import org.spongepowered.api.network.EngineConnection;
import org.spongepowered.api.network.EngineConnectionState;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.ChannelException;
import org.spongepowered.api.network.channel.ChannelIOException;
import org.spongepowered.api.network.channel.packet.Packet;
import org.spongepowered.api.network.channel.packet.PacketBinding;
import org.spongepowered.api.network.channel.packet.PacketDispatcher;
import org.spongepowered.api.network.channel.packet.RequestPacket;
import org.spongepowered.api.network.channel.packet.RequestPacketHandler;
import org.spongepowered.api.network.channel.packet.basic.BasicHandshakePacketDispatcher;
import org.spongepowered.api.network.channel.packet.basic.BasicPacketChannel;
import org.spongepowered.common.network.PacketUtil;
import org.spongepowered.common.network.SpongeEngineConnection;
import org.spongepowered.common.network.channel.ChannelBuffers;
import org.spongepowered.common.network.channel.ChannelExceptionUtil;
import org.spongepowered.common.network.channel.ConnectionUtil;
import org.spongepowered.common.network.channel.PacketSender;
import org.spongepowered.common.network.channel.SpongeChannelManager;
import org.spongepowered.common.network.channel.TransactionResult;
import org.spongepowered.common.network.channel.TransactionStore;
import org.spongepowered.common.util.Constants;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class SpongeBasicPacketChannel extends AbstractPacketChannel implements BasicPacketChannel {

    private final BasicHandshakePacketDispatcher handshake = new BasicHandshakePacketDispatcher() {

        @Override
        public <R extends Packet> CompletableFuture<R> sendTo(final EngineConnection connection, final RequestPacket<R> packet) {
            ConnectionUtil.checkHandshakePhase(connection);
            final EngineConnectionState state = (EngineConnectionState) ((SpongeEngineConnection) connection).connection().getPacketListener();
            if (connection instanceof ClientSideConnection) {
                throw new UnsupportedOperationException(
                        "Request packets from the client to server are currently not supported for basic packet channels.");
            }
            final CompletableFuture<R> future = new CompletableFuture<>();
            this.sendRequestTo(connection, state, packet, future::complete, null, future);
            return future;
        }

        private <P extends RequestPacket<R>, R extends Packet> void sendRequestTo(final EngineConnection connection, final EngineConnectionState state,
              final P request, final @Nullable Consumer<R> success, final @Nullable Runnable sendSuccess, final CompletableFuture<?> future) {
            final SpongeTransactionalPacketBinding<P, R> binding =
                    (SpongeTransactionalPacketBinding) SpongeBasicPacketChannel.this.requireBinding(request.getClass());

            final ChannelBuf payload;
            try {
                payload = SpongeBasicPacketChannel.this.encodeLoginPayload(binding.opcode(), request);
            } catch (final Throwable ex) {
                SpongeBasicPacketChannel.this.handleException(connection, state, ex, future);
                return;
            }

            final TransactionStore transactionStore = ConnectionUtil.getTransactionStore(connection);
            final int transactionId = transactionStore.nextId();

            final net.minecraft.network.protocol.Packet<?> mcPacket = PacketUtil.createLoginPayloadRequest(new CustomQueryPayload() {
                                                                                                               @Override
                                                                                                               public ResourceLocation id() {
                                                                                                                   return (ResourceLocation) (Object) Constants.Channels.FML_LOGIN_WRAPPER_CHANNEL;
                                                                                                               }

                                                                                                               @Override
                                                                                                               public void write(FriendlyByteBuf var1) {
                                                                                                                   var1.writeBytes((FriendlyByteBuf)payload);
                                                                                                               }
                                                                                                           }
                    , transactionId);

            final TransactionData<P, R> transactionData = new TransactionData<>(request, binding, success, future);
            transactionStore.put(transactionId, SpongeBasicPacketChannel.this, transactionData);

            PacketSender.sendTo(connection, mcPacket, throwable -> {
                if (throwable != null) {
                    if (transactionStore.remove(transactionId) != null) {
                        // Failed before it could reach the client
                        SpongeBasicPacketChannel.this.handleException(connection, state, ChannelExceptionUtil.of(throwable), future);
                    }
                } else {
                    if (sendSuccess != null) {
                        sendSuccess.run();
                    }
                }
            });
        }

        private CompletableFuture<Void> sendNormalTo(final EngineConnection connection, final EngineConnectionState state, final Packet packet) {
            final PacketBinding<?> binding = SpongeBasicPacketChannel.this.requireBinding(packet.getClass());
            final CompletableFuture<Void> future = new CompletableFuture<>();

            final ChannelBuf payload;
            try {
                payload = SpongeBasicPacketChannel.this.encodePayload(binding.opcode(), packet);
            } catch (final Throwable ex) {
                SpongeBasicPacketChannel.this.handleException(connection, state, ChannelExceptionUtil.of(ex), future);
                return future;
            }

            final TransactionStore transactionStore = ConnectionUtil.getTransactionStore(connection);
            final int transactionId = transactionStore.nextId();
            final ResourceKey key = SpongeBasicPacketChannel.this.key();

            final net.minecraft.network.protocol.Packet<?> mcPacket = PacketUtil.createLoginPayloadRequest(new CustomQueryPayload() {
                                                                                                               @Override
                                                                                                               public ResourceLocation id() {
                                                                                                                   return (ResourceLocation) (Object) key;
                                                                                                               }

                                                                                                               @Override
                                                                                                               public void write(FriendlyByteBuf var1) {
                                                                                                                   var1.writeBytes((FriendlyByteBuf)payload);
                                                                                                               }
                                                                                                           }
                    , transactionId);

            transactionStore.put(transactionId, SpongeBasicPacketChannel.this, null);

            PacketSender.sendTo(connection, mcPacket, throwable -> {
                if (throwable == null) {
                    future.complete(null);
                } else if (transactionStore.remove(transactionId) != null) {
                    future.completeExceptionally(throwable);
                }
            });
            return future;
        }

        @Override
        public CompletableFuture<Void> sendTo(final EngineConnection connection, final Packet packet) {
            ConnectionUtil.checkHandshakePhase(connection);
            final EngineConnectionState state = (EngineConnectionState) ((SpongeEngineConnection) connection).connection().getPacketListener();
            if (connection instanceof ClientSideConnection) {
                throw new UnsupportedOperationException(
                        "Packets from the client to server are currently not supported for basic packet channels.");
            }
            if (packet instanceof RequestPacket) {
                final CompletableFuture<Void> future = new CompletableFuture<>();
                this.sendRequestTo(connection, state, (RequestPacket) packet,
                        result -> {}, () -> future.complete(null), future);
                return future;
            } else {
                return this.sendNormalTo(connection, state, packet);
            }
        }

        @Override
        public <R extends Packet> CompletableFuture<R> sendToServer(final RequestPacket<R> packet) {
            throw new UnsupportedOperationException(
                    "Request packets from the client to server are currently not supported for basic packet channels.");
        }

        @SuppressWarnings("deprecation")
        @Override
        public CompletableFuture<Void> sendToServer(final Packet packet) {
            throw new UnsupportedOperationException(
                    "Packets from the client to server are currently not supported for basic packet channels.");
        }
    };

    private final PacketDispatcher play = new PacketDispatcher() {

        @Override
        public boolean isSupportedBy(final EngineConnection connection) {
            return ConnectionUtil.getRegisteredChannels(connection).contains(SpongeBasicPacketChannel.this.key());
        }

        @Override
        public CompletableFuture<Void> sendTo(final EngineConnection connection, final Packet packet) {
            ConnectionUtil.checkPlayPhase(connection);
            final EngineConnectionState state = (EngineConnectionState) ((SpongeEngineConnection) connection).connection().getPacketListener();

            final PacketBinding<?> binding = SpongeBasicPacketChannel.this.requireBinding(packet.getClass());

            final CompletableFuture<Void> future = new CompletableFuture<>();
            if (!SpongeBasicPacketChannel.this.checkSupported(connection, state, future)) {
                return future;
            }

            final ChannelBuf payload;
            try {
                payload = SpongeBasicPacketChannel.this.encodePayload(binding.opcode(), packet);
            } catch (final Throwable ex) {
                SpongeBasicPacketChannel.this.handleException(connection, state, ex, future);
                return future;
            }

            final net.minecraft.network.protocol.Packet<?> mcPacket = PacketUtil.createPlayPayload(SpongeBasicPacketChannel.this.payloadType(), payload, connection.side());
            PacketSender.sendTo(connection, mcPacket, future);
            return future;
        }
    };

    public SpongeBasicPacketChannel(final int type, final ResourceKey key, final SpongeChannelManager registry) {
        super(type, key, registry);
    }

    @Override
    public BasicHandshakePacketDispatcher handshake() {
        return this.handshake;
    }

    @Override
    public PacketDispatcher play() {
        return this.play;
    }

    private ChannelBuf encodePayload(final int opcode, final Packet packet) {
        final ChannelBuf payload = this.manager().getBufferAllocator().buffer();
        payload.writeByte((byte) opcode);
        this.encodePayload(payload, packet);
        return payload;
    }

    // This only exists for forge compatibility
    private ChannelBuf encodeLoginPayload(final int opcode, final Packet packet) {
        final ChannelBuf loginPayload = this.manager().getBufferAllocator().buffer();
        final ChannelBuf payload = this.manager().getBufferAllocator().buffer();
        try {
            this.encodePayloadUnsafe(payload, packet);
            loginPayload.writeString(this.key().formatted());
            loginPayload.writeVarInt(payload.available() + 1);
            loginPayload.writeByte((byte) opcode);
            ChannelBuffers.write(loginPayload, payload);
        } finally {
            ChannelBuffers.release(payload);
        }
        return loginPayload;
    }

    private int readOpcode(final ChannelBuf payload) {
        return payload.readByte() & 0xff;
    }

    @Override
    protected void handlePlayPayload(final EngineConnection connection, final EngineConnectionState state, final ChannelBuf payload) {
        final int opcode = this.readOpcode(payload);
        final SpongePacketBinding<Packet> binding = this.requireBinding(opcode);
        final Packet packet = this.decodePayload(() -> binding.getPacketConstructor().get(), payload);

        if (binding instanceof SpongeHandlerPacketBinding) {
            this.handle(connection, state, (SpongeHandlerPacketBinding<Packet>) binding, packet);
        }
    }

    @Override
    protected void handleLoginRequestPayload(final EngineConnection connection, final EngineConnectionState state, final int transactionId, final ChannelBuf payload) {
        // Is currently always executed on the client,
        // the server always expects a response
        final int opcode = this.readOpcode(payload);
        final SpongePacketBinding<Packet> binding = this.requireBinding(opcode);
        final Packet packet = this.decodePayload(() -> binding.getPacketConstructor().get(), payload);

        // A normal packet binding
        if (binding instanceof SpongeHandlerPacketBinding) {
            final SpongeHandlerPacketBinding<Packet> handlerBinding = (SpongeHandlerPacketBinding<Packet>) binding;
            this.handle(connection, state, handlerBinding, packet);
            // The server always expects a response
            PacketSender.sendTo(connection, PacketUtil.createLoginPayloadResponse(null, transactionId));
        } else {
            // A transactional packet binding
            final SpongeTransactionalPacketBinding<RequestPacket<Packet>, Packet> transactionalBinding =
                    (SpongeTransactionalPacketBinding) binding;
            final RequestPacketHandler<Packet, Packet, EngineConnectionState> handler =
                    (RequestPacketHandler<Packet, Packet, EngineConnectionState>) transactionalBinding.getRequestHandler(state);
            boolean success = false;
            if (handler != null) {
                final SpongeRequestPacketResponse<Packet> response = new SpongeRequestPacketResponse<Packet>() {

                    @Override
                    protected void fail0(final ChannelException exception) {
                        final net.minecraft.network.protocol.Packet<?> mcPacket = PacketUtil.createLoginPayloadResponse(null, transactionId);
                        PacketSender.sendTo(connection, mcPacket);
                    }

                    @Override
                    protected void success0(final Packet response) {
                        try {
                            final ChannelBuf responsePayload = SpongeBasicPacketChannel.this.encodeLoginPayload(transactionalBinding.opcode(), response);
                            final net.minecraft.network.protocol.Packet<?> mcPacket = PacketUtil.createLoginPayloadResponse(var1 -> {
                                var1.writeBytes((FriendlyByteBuf)responsePayload);
                            }, transactionId);
                            PacketSender.sendTo(connection, mcPacket);
                        } catch (final Throwable ex) {
                            SpongeBasicPacketChannel.this.handleException(connection, state, new ChannelIOException("Failed to encode response packet", ex), null);
                        }
                    }
                };
                try {
                    handler.handleRequest(packet, state, response);
                    success = true;
                } catch (final Throwable ex) {
                    this.handleException(connection, state, new ChannelIOException("Failed to handle request packet", ex), null);
                }
            }
            if (!success) {
                final net.minecraft.network.protocol.Packet<?> mcPacket = PacketUtil.createLoginPayloadResponse(null, transactionId);
                PacketSender.sendTo(connection, mcPacket);
            }
        }
    }

    @Override
    protected void handleTransactionResponse(final EngineConnection connection, final EngineConnectionState state, final Object stored, final TransactionResult result) {
        final TransactionData<RequestPacket<Packet>, Packet> transactionData =
                (TransactionData<RequestPacket<Packet>, Packet>) stored;
        this.handleTransactionResponse(connection, state, transactionData, result);
    }

    private <P extends RequestPacket<R>, R extends Packet> void handleTransactionResponse(final EngineConnection connection,
            final EngineConnectionState state, final TransactionData<P, R> transactionData, final TransactionResult result) {
        final SpongeTransactionalPacketBinding<P, R> binding = transactionData.binding;
        final P request = transactionData.request;
        if (result.isSuccess()) {
            ChannelBuf responsePayload = result.getPayload();
            responsePayload.readString(); // FML wrapper channel key
            final int actualLength = responsePayload.readVarInt();
            responsePayload = responsePayload.readSlice(actualLength);
            try {
                final int opcode = this.readOpcode(responsePayload);

                final Supplier<R> responseConstructor;
                if (binding instanceof SpongeFixedTransactionalPacketBinding) {
                    // The response packet uses the same binding and opcode,
                    // so the returned opcode can be ignored.
                    responseConstructor = ((SpongeFixedTransactionalPacketBinding<P, R>) binding).getResponsePacketConstructor();
                } else {
                    // The response type was registered separately to a different
                    // opcode, so lookup using the opcode.
                    final SpongeHandlerPacketBinding<R> responseBinding = (SpongeHandlerPacketBinding<R>) this.byOpcode.get(opcode);
                    if (responseBinding == null) {
                        throw new ChannelIOException("Unknown packet opcode: " + opcode);
                    }
                    responseConstructor = responseBinding.getPacketConstructor();
                }

                final R responsePacket = this.decodePayload(responseConstructor, responsePayload);
                if (transactionData.success != null) {
                    transactionData.success.accept(responsePacket);
                }

                this.handleResponse(connection, state, binding, request, responsePacket);
            } catch (final Throwable ex) {
                this.handleException(connection, state, ex, transactionData.future);
            }
        } else {
            this.handleException(connection, state, result.getCause(), transactionData.future);
            this.handleResponseFailure(connection, state, binding, request, result.getCause());
        }
    }
}
