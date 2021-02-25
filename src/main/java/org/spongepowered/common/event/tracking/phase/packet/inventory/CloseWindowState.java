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
package org.spongepowered.common.event.tracking.phase.packet.inventory;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.phase.packet.BasicPacketContext;
import org.spongepowered.common.event.tracking.phase.packet.BasicPacketState;
import org.spongepowered.common.item.util.ItemStackUtil;

public final class CloseWindowState extends BasicPacketState {

    @Override
    public void populateContext(ServerPlayer playerMP, Packet<?> packet, BasicPacketContext context) {
        context.openContainer(playerMP.containerMenu);
    }

    @Override
    public void unwind(BasicPacketContext context) {
        final ServerPlayer player = context.getSource(ServerPlayer.class).get();
        final AbstractContainerMenu container = context.getOpenContainer();
        ItemStackSnapshot lastCursor = context.getCursor();
        ItemStackSnapshot newCursor = ItemStackUtil.snapshotOf(player.inventory.getCarried());
        final CauseStackManager stackManager = PhaseTracker.getCauseStackManager();
        if (lastCursor != null) {
            stackManager.pushCause(player);
            InteractContainerEvent.Close event =
                    SpongeCommonEventFactory.callInteractInventoryCloseEvent(container, player, lastCursor, newCursor, true);
            if (event.isCancelled()) {
                stackManager.popCause();
                return;
            }
            stackManager.popCause();
        }

        if (context.getTransactor().isEmpty()) {
            return;
        }

        try (CauseStackManager.StackFrame frame = stackManager.pushCauseFrame()) {
            frame.pushCause(player);
            frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);
            // items
//            context.getCapturedItemsSupplier().acceptAndClearIfNotEmpty(items -> {
//                final List<Entity> entities = items
//                    .stream()
//                    .map(entity -> (Entity) entity)
//                    .collect(Collectors.toList());
//                if (!entities.isEmpty()) {
//                    SpongeCommonEventFactory.callDropItemClose(entities, context, () -> Optional.of(((ServerPlayer) player).getUser()));
//                }
//            });
        }
        // TODO - Determine if we need to pass the supplier or perform some parameterized
        //  process if not empty method on the capture object.
        TrackingUtil.processBlockCaptures(context);

    }

}
