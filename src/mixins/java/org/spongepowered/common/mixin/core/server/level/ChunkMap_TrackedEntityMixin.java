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
package org.spongepowered.common.mixin.core.server.level;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.DataPerspective;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.data.contextual.ContextualData;
import org.spongepowered.common.data.contextual.PerspectiveContainer;
import org.spongepowered.common.data.contextual.util.ContextualPacketUtil;
import org.spongepowered.common.entity.living.human.HumanEntity;

import java.util.stream.Stream;

@Mixin(targets = "net/minecraft/server/level/ChunkMap$TrackedEntity")
public abstract class ChunkMap_TrackedEntityMixin {

    @Shadow @Final Entity entity;

    /**
     * @author gabizou
     * @reason Because of the public availability of some methods, a packet
     * being sent for a "vanished" entity is not permissible since the vanished
     * entity is being "removed" from clients by way of literally being mimiced being
     * "untracked". This safeguards the players being updated erroneously.
     * @author gabizou
     * @reason Instead of attempting to fetch the player set within
     * {@link org.spongepowered.common.mixin.core.server.level.ServerEntityMixin#impl$sendHumanMetadata(CallbackInfo)},
     * we can take advantage of the fact that:
     *  1) We already have the entity being checked
     *  2) We already have the players being iterated
     *  3) This achieves the same functionality without adding new accessors etc.
     */
    @Redirect(method = "broadcast(Lnet/minecraft/network/protocol/Packet;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerConnection;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void impl$sendQueuedHumanPackets(final ServerPlayerConnection serverPlayNetHandler, final Packet<?> packetIn) {
        final @Nullable PerspectiveContainer<?, ?> contextualData = ((ContextualData) this.entity).dataPerception((DataPerspective) serverPlayNetHandler.getPlayer());
        final ValueContainer dataPerception = contextualData != null ? contextualData : (ValueContainer) this.entity;
        if (dataPerception.require(Keys.VANISH_STATE).invisible()) {
            return;
        }

        if (contextualData != null && packetIn instanceof final ClientboundSetEntityDataPacket entityDataPacket) {
            serverPlayNetHandler.send(ContextualPacketUtil.createContextualPacket(entityDataPacket, contextualData));
        } else {
            serverPlayNetHandler.send(packetIn);
        }

        if (this.entity instanceof HumanEntity && serverPlayNetHandler instanceof ServerGamePacketListenerImpl) {
            final ServerPlayer player = ((ServerGamePacketListenerImpl) serverPlayNetHandler).player;
            final Stream<Packet<?>> packets = ((HumanEntity) this.entity).popQueuedPackets(player);
            packets.forEach(player.connection::send);
        }
    }

    @Redirect(method = "updatePlayer(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;broadcastToPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z"))
    private boolean impl$isSpectatedOrVanished(final Entity entity, final ServerPlayer player) {
        if (((DataPerspective) entity).getDataPerception((DataPerspective) player).require(Keys.VANISH_STATE).invisible()) {
            return false;
        }

        return entity.broadcastToPlayer(player);
    }

}
