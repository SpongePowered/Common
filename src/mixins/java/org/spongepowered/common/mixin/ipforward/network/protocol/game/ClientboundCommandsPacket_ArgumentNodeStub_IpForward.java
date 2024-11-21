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
package org.spongepowered.common.mixin.ipforward.network.protocol.game;

import com.mojang.brigadier.arguments.ArgumentType;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * If a proxy server tries to inspect the commands sent
 * to the client, it is unable to parse the elements
 * that contain mod specific information due to being
 * unaware of their structure.
 *
 * To fix this, the proxy server expects that non-vanilla
 * elements are wrapped in a magic argument that it can
 * detect to skip its content.
 *
 * The proxy server is expected to unwrap these arguments
 * afterward to allow the client to parse the commands
 * as normal.
 */
@Mixin(targets = "net/minecraft/network/protocol/game/ClientboundCommandsPacket$ArgumentNodeStub")
public final class ClientboundCommandsPacket_ArgumentNodeStub_IpForward {

    //Magic value that was chosen by the proxy implementation.
    private static final int ipForward$MOD_ARGUMENT_ID = -256;

    @Inject(method = "serializeCap(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo$Template;)V", at = @At("HEAD"), cancellable = true)
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void ipForward$onSerializeCap(
        final FriendlyByteBuf buf, final ArgumentTypeInfo<A, T> $$1, final ArgumentTypeInfo.Template<A> $$2, final CallbackInfo ci) {
        final @Nullable ResourceLocation key = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey($$1);
        if (key == null || ClientboundCommandsPacket_ArgumentNodeStub_IpForward.ipForward$isVanillaArgument(key)) {
            return;
        }

        ci.cancel();

        final FriendlyByteBuf commandData = new FriendlyByteBuf(Unpooled.buffer());
        $$1.serializeToNetwork((T) $$2, commandData);

        buf.writeVarInt(ClientboundCommandsPacket_ArgumentNodeStub_IpForward.ipForward$MOD_ARGUMENT_ID);
        buf.writeVarInt(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId($$1));
        buf.writeVarInt(commandData.readableBytes());
        buf.writeBytes(commandData);
    }

    private static boolean ipForward$isVanillaArgument(final ResourceLocation key) {
        return switch (key.getNamespace()) {
            case "brigadier" -> true;
            case "minecraft" -> switch (key.getPath()) {
                case "test_argument", "test_class" -> false; //These are normally under IS_RUNNING_IN_IDE, proxies cant handle them.
                default -> true;
            };
            default -> false;
        };
    }
}
