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
package org.spongepowered.common.network;

import com.mojang.authlib.GameProfile;
import net.kyori.adventure.text.Component;
import net.minecraft.network.Connection;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.network.EngineConnection;
import org.spongepowered.api.network.EngineConnectionState;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.network.ConnectionBridge;
import org.spongepowered.common.bridge.network.ServerLoginPacketListenerImplBridge;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.profile.SpongeGameProfile;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public abstract class SpongeEngineConnection implements EngineConnection {

    protected final Connection connection;

    private final AtomicReference<EventFireState> eventFireState;

    private @MonotonicNonNull GameProfile gameProfile;

    public SpongeEngineConnection(final Connection connection) {
        this.connection = connection;

        this.eventFireState = new AtomicReference<>(EventFireState.NONE);
    }

    @Override
    public boolean active() {
        return this.connection.isConnected();
    }

    @Override
    public Optional<EngineConnectionState> state() {
        if (this.active()) {
            final EngineConnectionState state = (EngineConnectionState) this.connection.getPacketListener();
            if (!(state instanceof ServerLoginPacketListenerImplBridge loginBridge) || loginBridge.bridge$isIntentDone()) {
                return Optional.of(state);
            }
            return Optional.of(this.createIntentState(state.transferred()));
        }
        return Optional.empty();
    }

    @Override
    public InetSocketAddress address() {
        return ((ConnectionBridge) this.connection).bridge$getAddress();
    }

    @Override
    public InetSocketAddress virtualHost() {
        return ((ConnectionBridge) this.connection).bridge$getVirtualHost();
    }

    @Override
    public void close() {
        this.connection.disconnect(net.minecraft.network.chat.Component.translatable("disconnect.disconnected"));
    }

    @Override
    public void close(final Component reason) {
        this.connection.disconnect(SpongeAdventure.asVanilla(reason));
    }

    public void setGameProfile(final GameProfile gameProfile) {
        this.gameProfile = gameProfile;
    }

    public Connection connection() {
        return this.connection;
    }

    public void disconnected() {
        if (this.eventFireState.getAndSet(EventFireState.DISCONNECTED).shouldFireDisconnectionImmediately()) {
            this.fireDisconnectEvent();
        }
    }

    public boolean postGuardedEvent(final ServerSideConnectionEvent event) {
        if (!this.eventFireState.compareAndSet(EventFireState.NONE, EventFireState.IN_FLIGHT)
                && !this.eventFireState.compareAndSet(EventFireState.POST, EventFireState.IN_FLIGHT)) {
            return false;
        }
        SpongeCommon.post(event);
        if (!this.eventFireState.compareAndSet(EventFireState.IN_FLIGHT, EventFireState.POST)) {
            this.fireDisconnectEvent();
        }
        return event instanceof Cancellable cancellable && cancellable.isCancelled();
    }

    private void fireDisconnectEvent() {
        this.eventFireState.set(EventFireState.DISCONNECTED);
        final ServerSideConnectionEvent.Disconnect event = SpongeEventFactory.createServerSideConnectionEventDisconnect(
                PhaseTracker.getCauseStackManager().currentCause(), (ServerSideConnection) this, Optional.ofNullable(this.gameProfile).map(SpongeGameProfile::of));
        SpongeCommon.post(event);
    }

    protected abstract EngineConnectionState.Intent createIntentState(boolean transferred);

    private enum EventFireState {
        NONE,
        IN_FLIGHT,
        POST,
        DISCONNECTED;

        boolean shouldFireDisconnectionImmediately() {
            return this == EventFireState.POST;
        }
    }
}
