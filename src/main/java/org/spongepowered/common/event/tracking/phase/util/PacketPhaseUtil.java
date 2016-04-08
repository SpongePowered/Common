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
package org.spongepowered.common.event.tracking.phase.util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.common.item.inventory.adapter.impl.slots.SlotAdapter;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.util.List;

public final class PacketPhaseUtil {

    public static void handleSlotRestore(EntityPlayerMP player, List<SlotTransaction> slotTransactions) {
        for (SlotTransaction slotTransaction : slotTransactions) {
            final SlotAdapter slot = (SlotAdapter) slotTransaction.getSlot();
            final int slotNumber = slot.slotNumber;
            final ItemStack originalStack = ItemStackUtil.fromSnapshotToNative(slotTransaction.getOriginal());

            // TODO: fix below
            /*if (originalStack == null) {
                slot.clear();
            } else {
                slot.offer((org.spongepowered.api.item.inventory.ItemStack) originalStack);
            }*/

            final Slot nmsSlot = player.inventoryContainer.getSlot(slotNumber);
            if (nmsSlot != null) {
                nmsSlot.putStack(originalStack);
            }

            player.playerNetServerHandler.sendPacket(new S2FPacketSetSlot(player.openContainer.windowId, slotNumber, originalStack));
        }
    }

    public static void handleCustomCursor(EntityPlayerMP player, ItemStackSnapshot customCursor) {
        ItemStack cursor = ItemStackUtil.fromSnapshotToNative(customCursor);
        player.inventory.setItemStack(cursor);
        player.playerNetServerHandler.sendPacket(new S2FPacketSetSlot(-1, -1, cursor));
    }

    public static void handleCustomSlot(EntityPlayerMP player, List<SlotTransaction> slotTransactions) {
        for (SlotTransaction slotTransaction : slotTransactions) {
            if (slotTransaction.isValid() && slotTransaction.getCustom().isPresent()) {
                final SlotAdapter slot = (SlotAdapter) slotTransaction.getSlot();
                final int slotNumber = slot.slotNumber;
                final ItemStack customStack = ItemStackUtil.fromSnapshotToNative(slotTransaction.getFinal());

                // TODO: fix below
                /*if (customStack == null) {
                    slot.clear();
                } else {
                    slot.offer((org.spongepowered.api.item.inventory.ItemStack) customStack);
                }*/

                final Slot nmsSlot = player.inventoryContainer.getSlot(slotNumber);
                if (nmsSlot != null) {
                    nmsSlot.putStack(customStack);
                }

                player.playerNetServerHandler.sendPacket(new S2FPacketSetSlot(player.openContainer.windowId, slotNumber, customStack));
            }
        }
    }

    public static void validateCapturedTransactions(int slotId, Container openContainer, List<SlotTransaction> capturedTransactions) {
        if (capturedTransactions.size() == 0 && slotId >= 0) {
            final Slot slot = openContainer.getSlot(slotId);
            if (slot != null) {
                final SlotTransaction slotTransaction = new SlotTransaction(new SlotAdapter(slot), ItemStackSnapshot.NONE, ItemStackSnapshot.NONE);
                capturedTransactions.add(slotTransaction);
            }
        }
    }
}
