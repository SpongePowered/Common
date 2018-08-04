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

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketClientSettings;
import net.minecraft.network.play.client.CPacketClientStatus;
import net.minecraft.network.play.client.CPacketPlayer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.phase.TrackingPhases;
import org.spongepowered.common.event.tracking.phase.packet.PacketContext;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhase;
import org.spongepowered.common.interfaces.entity.player.IMixinEntityPlayerMP;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

public class PacketUtil {

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void onProcessPacket(Packet packetIn, INetHandler netHandler) {
        if (netHandler instanceof NetHandlerPlayServer) {
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                EntityPlayerMP packetPlayer = ((NetHandlerPlayServer) netHandler).player;
                frame.pushCause(packetPlayer);
                if (creativeExploitCheck(packetIn, packetPlayer)) {
                    return;
                }
    
                // Don't process movement capture logic if player hasn't moved
                boolean ignoreMovementCapture = false;
                if (packetIn instanceof CPacketPlayer) {
                    CPacketPlayer movingPacket = ((CPacketPlayer) packetIn);
                    if (movingPacket instanceof CPacketPlayer.Rotation) {
                        ignoreMovementCapture = true;
                    } else if (packetPlayer.posX == movingPacket.x && packetPlayer.posY == movingPacket.y && packetPlayer.posZ == movingPacket.z) {
                        ignoreMovementCapture = true;
                    }
                }
                if (ignoreMovementCapture || (packetIn instanceof CPacketClientSettings)) {
                    packetIn.processPacket(netHandler);
                } else {
                    final ItemStackSnapshot cursor = ItemStackUtil.snapshotOf(packetPlayer.inventory.getItemStack());
                    IPhaseState<? extends PacketContext<?>> packetState = TrackingPhases.PACKET.getStateForPacket(packetIn);
                    if (packetState == null) {
                        throw new IllegalArgumentException("Found a null packet phase for packet: " + packetIn.getClass());
                    }
                    // At the very least make an unknown packet state case.
                    PhaseContext<?> context = PacketPhase.General.UNKNOWN.createPhaseContext();
                    if (!TrackingPhases.PACKET.isPacketInvalid(packetIn, packetPlayer, packetState)) {
                        context = packetState.createPhaseContext()
                            .source(packetPlayer)
                            .packetPlayer(packetPlayer)
                            .packet(packetIn)
                            .cursor(cursor);
    
                        TrackingPhases.PACKET.populateContext(packetIn, packetPlayer, packetState, context);
                        context.owner((Player) packetPlayer);
                        context.notifier((Player) packetPlayer);
                    }
                    try (PhaseContext<?> packetContext = context) {
                        packetContext.buildAndSwitch();
                        packetIn.processPacket(netHandler);
    
                    }
    
                    if (packetIn instanceof CPacketClientStatus) {
                        // update the reference of player
                        packetPlayer = ((NetHandlerPlayServer) netHandler).player;
                    }
                    ((IMixinEntityPlayerMP) packetPlayer).setPacketItem(ItemStack.EMPTY);
                }
            }
        } else { // client
            packetIn.processPacket(netHandler);
        }
    }

    // Overridden by MixinPacketUtil for exploit check
    private static boolean creativeExploitCheck(Packet<?> packetIn, EntityPlayerMP playerMP) {
        return false;
    }
}
