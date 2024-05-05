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
package org.spongepowered.common.mixin.core.network.protocol.game;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import org.spongepowered.api.resourcepack.ResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.network.protocol.game.ClientboundResourcePackPacketBridge;
import org.spongepowered.common.resourcepack.SpongeResourcePack;

import java.net.URISyntaxException;
import java.util.UUID;

@Mixin(ClientboundResourcePackPushPacket.class)
public abstract class ClientboundResourcePackPushPacketMixin implements ClientboundResourcePackPacketBridge {

    @Shadow private String url;
    @Shadow private String hash;

    private ResourcePack impl$pack;

    @Inject(method = "<init>(Ljava/util/UUID;Ljava/lang/String;Ljava/lang/String;ZLnet/minecraft/network/chat/Component;)V", at = @At("RETURN") , remap = false)
    private void impl$setResourcePackOrThrowException(final UUID uuid, final String url, final String hash, final boolean required, final Component prompt, final CallbackInfo ci) {
        try {
            this.impl$pack = SpongeResourcePack.create(url, hash, prompt == null ? net.kyori.adventure.text.Component.empty() : SpongeAdventure.asAdventure(prompt));
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void bridge$setSpongePack(final ResourcePack pack) {
        this.impl$pack = pack;
        this.url = ((SpongeResourcePack) pack).getUrlString();
        this.hash = pack.hash().orElse("");
    }

    @Override
    public ResourcePack bridge$getSpongePack() {
        return this.impl$pack;
    }

}
