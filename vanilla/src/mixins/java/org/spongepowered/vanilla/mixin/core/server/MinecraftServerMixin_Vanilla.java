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
package org.spongepowered.vanilla.mixin.core.server;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import net.minecraft.command.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeBootstrap;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.user.SpongeUserManager;
import org.spongepowered.vanilla.VanillaServer;
import org.spongepowered.vanilla.hooks.VanillaPacketHooks;

import java.io.File;
import java.net.Proxy;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin_Vanilla implements VanillaServer {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void vanilla$setPacketHooks(File p_i50590_1_, Proxy p_i50590_2_, DataFixer dataFixerIn, Commands p_i50590_4_,
            YggdrasilAuthenticationService p_i50590_5_, MinecraftSessionService p_i50590_6_, GameProfileRepository p_i50590_7_,
            PlayerProfileCache p_i50590_8_, IChunkStatusListenerFactory p_i50590_9_, String p_i50590_10_, CallbackInfo ci) {
        PlatformHooks.getInstance().setPacketHooks(new VanillaPacketHooks());
    }
    @Redirect(method = "loadWorlds",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/management/PlayerList;func_212504_a(Lnet/minecraft/world/server/ServerWorld;)V"))
    private void vanilla$onSaveHandlerBeingSetToPlayerList(final PlayerList playerList, final ServerWorld p_212504_1_) {
        playerList.func_212504_a(p_212504_1_);
        ((SpongeUserManager) this.getUserManager()).init();
    }

    @Inject(method = "stopServer", at = @At(value = "HEAD"), cancellable = true)
    private void vanilla$callStoppingEngineEvent(CallbackInfo ci) {
        SpongeBootstrap.getLifecycle().callStoppingEngineEvent(this);
    }
}
