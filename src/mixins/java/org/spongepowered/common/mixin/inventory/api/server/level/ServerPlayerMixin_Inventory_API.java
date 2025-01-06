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
package org.spongepowered.common.mixin.inventory.api.server.level;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.world.inventory.container.ContainerBridge;
import org.spongepowered.common.event.inventory.InventoryEventFactory;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhase;
import org.spongepowered.common.mixin.inventory.api.world.entity.player.PlayerMixin_Inventory_API;

import java.util.Optional;

@Mixin(net.minecraft.server.level.ServerPlayer.class)
public abstract class ServerPlayerMixin_Inventory_API extends PlayerMixin_Inventory_API implements ServerPlayer {

    @Shadow public abstract void shadow$doCloseContainer();

    @Override
    public Optional<Container> openInventory() {
        return Optional.ofNullable((Container) this.containerMenu);
    }

    @Override
    public Optional<Container> openInventory(final Inventory inventory) throws IllegalArgumentException {
        return this.openInventory(inventory, null);
    }

    @SuppressWarnings({"unchecked", "ConstantConditions", "rawtypes"})
    @Override
    public Optional<Container> openInventory(final Inventory inventory, final Component displayName) {
        final ContainerBridge openContainer = (ContainerBridge) this.containerMenu;
        if (openContainer.bridge$isInUse()) {
            throw new UnsupportedOperationException("This player is currently modifying an open container. Opening a new one must be delayed!");
        }
        return Optional.ofNullable((Container) InventoryEventFactory.displayContainer((net.minecraft.server.level.ServerPlayer) (Object) this, inventory, displayName));
    }

    @Override
    public boolean closeInventory() throws IllegalArgumentException {
        final net.minecraft.world.inventory.AbstractContainerMenu openContainer = this.containerMenu;
        if (((ContainerBridge) openContainer).bridge$isInUse()) {
            throw new UnsupportedOperationException("This player is currently modifying an open container. Closing it must be delayed!");
        }
        // Create Close_Window to capture item drops
        final net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) (Object) this;
        try (final PhaseContext<@NonNull ?> ctx = PacketPhase.General.CLOSE_WINDOW.createPhaseContext(PhaseTracker.getWorldInstance(player.serverLevel()))
                .source(this)
                .packetPlayer(player)
                .isClientSide(false)
        ) {
            ctx.buildAndSwitch();
            this.shadow$doCloseContainer();

            if (!TrackingUtil.processBlockCaptures(ctx)) {
                return false;
            }
        }
        return true;
    }

}
