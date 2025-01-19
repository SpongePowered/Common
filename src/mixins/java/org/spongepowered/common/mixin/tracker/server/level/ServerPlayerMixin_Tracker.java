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
package org.spongepowered.common.mixin.tracker.server.level;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.common.bridge.world.entity.PlatformEntityBridge;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.mixin.tracker.world.entity.player.PlayerMixin_Tracker;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin_Tracker extends PlayerMixin_Tracker {

    /**
     * @author gabizou - June 4th, 2016
     * @author i509VCB - February 17th, 2020 - 1.14.4
     * @author gabizou - December 31st, 2021 - 1.16.5
     * @reason We inject a construct event for the item drop and conveniently
     * can redirect the super call.
     */
    @WrapOperation(
        method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;createItemStackToDrop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"
        )
    )
    @Nullable
    private ItemEntity tracker$throwItemDrop(
        final ServerPlayer instance, final ItemStack droppedItem, final boolean dropAround,
        final boolean traceItem, final Operation<ItemEntity> wrapped
    ) {
        if (droppedItem.isEmpty()) {
            return null;
        }
        if (((PlatformEntityBridge) this).bridge$isFakePlayer()) {
            return super.shadow$drop(droppedItem, dropAround, traceItem);
        }
        if (((LevelBridge) this.shadow$level()).bridge$isFake()) {
            return super.shadow$drop(droppedItem, dropAround, traceItem);
        }

        final double posX1 = this.shadow$getX();
        final double posY1 = this.shadow$getEyeY() - (double) 0.3F;
        final double posZ1 = this.shadow$getZ();
        // Now the real fun begins.
        final ItemStack item;
        final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(droppedItem);
        final List<ItemStackSnapshot> original = new ArrayList<>();
        original.add(snapshot);

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {

            item = SpongeCommonEventFactory.throwDropItemAndConstructEvent(
                (ServerPlayer) (Object) this, posX1, posY1, posZ1, snapshot, original, frame);

            if (item == null || item.isEmpty()) {
                return null;
            }
            return wrapped.call(instance, item, dropAround, traceItem);
        }
    }
}
