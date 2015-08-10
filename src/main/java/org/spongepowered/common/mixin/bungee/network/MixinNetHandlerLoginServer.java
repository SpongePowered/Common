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
package org.spongepowered.common.mixin.bungee.network;

import com.google.common.base.Charsets;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.network.NetHandlerLoginServer;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.interfaces.IMixinNetHandlerLoginServer;
import org.spongepowered.common.interfaces.IMixinNetworkManager;

import java.util.UUID;

@Mixin(NetHandlerLoginServer.class)
public abstract class MixinNetHandlerLoginServer implements IMixinNetHandlerLoginServer {

    @Shadow public NetworkManager networkManager;
    @Shadow private com.mojang.authlib.GameProfile loginGameProfile;

    @Inject(method = "processLoginStart", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/NetHandlerLoginServer;"
            + "currentLoginState:Lnet/minecraft/server/network/NetHandlerLoginServer$LoginState;",
            opcode = Opcodes.PUTFIELD, ordinal = 1))
    public void initUuid(CallbackInfo ci) {
        UUID uuid;
        if (((IMixinNetworkManager) this.networkManager).getSpoofedUUID() != null) {
            uuid = ((IMixinNetworkManager) this.networkManager).getSpoofedUUID();
        } else {
            uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + this.loginGameProfile.getName()).getBytes(Charsets.UTF_8));
        }

        this.loginGameProfile = new com.mojang.authlib.GameProfile(uuid, this.loginGameProfile.getName());

        if (((IMixinNetworkManager) this.networkManager).getSpoofedProfile() != null) {
            for (Property property : ((IMixinNetworkManager) this.networkManager).getSpoofedProfile())
            {
                this.loginGameProfile.getProperties().put(property.getName(), property);
            }
        }
    }
}
