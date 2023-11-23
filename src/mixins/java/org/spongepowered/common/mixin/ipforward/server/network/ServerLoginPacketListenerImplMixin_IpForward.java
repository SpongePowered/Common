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
package org.spongepowered.common.mixin.ipforward.server.network;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Charsets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.applaunch.config.common.IpForwardingCategory;
import org.spongepowered.common.applaunch.config.core.SpongeConfigs;
import org.spongepowered.common.bridge.network.ConnectionBridge_IpForward;
import org.spongepowered.common.ipforward.velocity.VelocityForwardingInfo;

import java.util.UUID;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin_IpForward {

    // @formatter:off
    @Shadow @Final private MinecraftServer server;
    @Shadow @Final public Connection connection;
    @Shadow private GameProfile authenticatedProfile;
    // @formatter:on

    private boolean ipForward$sentVelocityForwardingRequest;

    // Velocity
    @Inject(method = "handleHello", at = @At("HEAD"), cancellable = true)
    private void ipForward$sendVelocityIndicator(final CallbackInfo info) {
        if (!this.server.usesAuthentication() && SpongeConfigs.getCommon().get().ipForwarding.mode == IpForwardingCategory.Mode.MODERN) {
            checkState(!this.ipForward$sentVelocityForwardingRequest, "Sent additional login start message!");
            this.ipForward$sentVelocityForwardingRequest = true;

            VelocityForwardingInfo.sendQuery((ServerLoginPacketListenerImpl) (Object) this);
        }
    }

    // Bungee
    @Inject(method = "startClientVerification",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/server/network/ServerLoginPacketListenerImpl;authenticatedProfile:Lcom/mojang/authlib/GameProfile;",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.AFTER))
    private void bungee$initUuid(final CallbackInfo ci) {
        if (this.authenticatedProfile == this.server.getSingleplayerProfile()) {
            return;
        }

        if (!this.server.usesAuthentication() && SpongeConfigs.getCommon().get().ipForwarding.mode == IpForwardingCategory.Mode.LEGACY) {
            final UUID uuid;
            if (((ConnectionBridge_IpForward) this.connection).bungeeBridge$getSpoofedUUID() != null) {
                uuid = ((ConnectionBridge_IpForward) this.connection).bungeeBridge$getSpoofedUUID();
            } else {
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + this.authenticatedProfile.getName()).getBytes(Charsets.UTF_8));
            }

            this.authenticatedProfile = new GameProfile(uuid, this.authenticatedProfile.getName());

            if (((ConnectionBridge_IpForward) this.connection).bungeeBridge$getSpoofedProfile() != null) {
                for (final Property property : ((ConnectionBridge_IpForward) this.connection).bungeeBridge$getSpoofedProfile()) {
                    this.authenticatedProfile.getProperties().put(property.name(), property);
                }
            }
        }
    }
}
