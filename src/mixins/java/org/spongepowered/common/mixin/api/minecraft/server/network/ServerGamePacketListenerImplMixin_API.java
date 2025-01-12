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
package org.spongepowered.common.mixin.api.minecraft.server.network;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.network.ServerConnectionState;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.network.ConnectionBridge;
import org.spongepowered.common.profile.SpongeGameProfile;

@Mixin(ServerGamePacketListenerImpl.class)
@Implements(@Interface(iface = ServerConnectionState.Game.class, prefix = "api$", remap = Interface.Remap.NONE))
public abstract class ServerGamePacketListenerImplMixin_API extends ServerCommonPacketListenerImplMixin_API implements ServerConnectionState.Game {

    // @formatter:off
    @Shadow public net.minecraft.server.level.ServerPlayer player;
    // @formatter:on

    @Override
    public ServerSideConnection connection() {
        return (ServerSideConnection) ((ConnectionBridge) this.connection).bridge$getEngineConnection();
    }

    @Override
    public GameProfile profile() {
        return SpongeGameProfile.of(this.shadow$playerProfile());
    }

    @Override
    public ServerPlayer player() {
        return (ServerPlayer) this.player;
    }

    //TODO: Fix me
    /*@Intrinsic
    public int api$latency() {
        return this.shadow$latency();
    }*/
}
