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

package org.spongepowered.common.mixin.core.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.entity.living.HumanEntity;

import java.util.Set;

@Mixin(EntityTrackerEntry.class)
public abstract class MixinEntityTrackerEntry {

    @Shadow
    public Entity trackedEntity;

    @Shadow
    public Set<EntityPlayerMP> trackingPlayers;

    @Shadow
    public abstract void func_151261_b(Packet packetIn);

    @Redirect(method = "updatePlayerEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/NetHandlerPlayServer;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 0))
    public void onSendSpawnPacket(NetHandlerPlayServer thisCtx, Packet spawnPacket, EntityPlayerMP playerIn) {
        if (!(this.trackedEntity instanceof HumanEntity)) {
            // This is the method call that was @Redirected
            thisCtx.sendPacket(spawnPacket);
            return;
        }
        HumanEntity human = (HumanEntity) this.trackedEntity;
        // Adds the GameProfile to the client
        thisCtx.sendPacket(human.createPlayerListPacket(S38PacketPlayerListItem.Action.ADD_PLAYER));
        // Actually spawn the human (a player)
        thisCtx.sendPacket(spawnPacket);
        // Remove from tab list
        S38PacketPlayerListItem removePacket = human.createPlayerListPacket(S38PacketPlayerListItem.Action.REMOVE_PLAYER);
        if (human.canRemoveFromListImmediately()) {
            thisCtx.sendPacket(removePacket);
        } else {
            human.pushPackets(playerIn, new Packet[0]); // Extra delay of 1 tick
            human.pushPackets(playerIn, removePacket);
        }
    }

    // The spawn packet for a human is a player
    @Inject(method = "func_151260_c", at = @At("HEAD"), cancellable = true)
    public void onGetSpawnPacket(CallbackInfoReturnable<Packet> cir) {
        if (this.trackedEntity instanceof HumanEntity) {
            cir.setReturnValue(new S0CPacketSpawnPlayer((EntityPlayer) this.trackedEntity));
        }
    }

    @Inject(method = "sendMetadataToAllAssociatedPlayers", at = @At("HEAD"))
    public void onSendMetadata(CallbackInfo ci) {
        if (!(this.trackedEntity instanceof HumanEntity)) {
            return;
        }
        HumanEntity human = (HumanEntity) this.trackedEntity;
        Packet[] packets = human.popQueuedPackets(null);
        for (EntityPlayerMP player : this.trackingPlayers) {
            if (packets != null) {
                for (Packet packet : packets) {
                    player.playerNetServerHandler.sendPacket(packet);
                }
            }
            Packet[] playerPackets = human.popQueuedPackets(player);
            if (playerPackets != null) {
                for (Packet packet : playerPackets) {
                    player.playerNetServerHandler.sendPacket(packet);
                }
            }
        }
    }
}
