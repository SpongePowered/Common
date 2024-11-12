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
package org.spongepowered.forge.mixin.core.server.network;

import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.crafting.CraftingInventory;
import org.spongepowered.api.item.inventory.query.QueryTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.context.transaction.TransactionalCaptureSupplier;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin_Forge implements ServerGamePacketListener {

    @Shadow public ServerPlayer player;

    @Redirect(method = "handlePlaceRecipe",
        at = @At(value = "INVOKE", target = "net/minecraft/world/inventory/RecipeBookMenu.handlePlacement(ZZLnet/minecraft/world/item/crafting/RecipeHolder;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/player/Inventory;)Lnet/minecraft/world/inventory/RecipeBookMenu$PostPlaceAction;"))
    private RecipeBookMenu.PostPlaceAction forge$onPlaceRecipe(
        final RecipeBookMenu recipeBookMenu, final boolean shift, final boolean isCreative, final RecipeHolder<?> recipe,
        final ServerLevel serverLevel, final net.minecraft.world.entity.player.Inventory inventory) {
        final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance(this.player.serverLevel()).getPhaseContext();
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        final var player = this.player;

        final Inventory craftInv = ((Inventory) player.containerMenu).query(QueryTypes.INVENTORY_TYPE.get().of(CraftingInventory.class));
        final RecipeBookMenu.PostPlaceAction result;
        if (!(craftInv instanceof CraftingInventory)) {
            result = recipeBookMenu.handlePlacement(shift,isCreative, recipe, serverLevel, inventory);
            SpongeCommon.logger().warn("Detected crafting without a InventoryCrafting!? Crafting Event will not fire.");
            return result;
        }

        try (final EffectTransactor ignored = transactor.logPlaceRecipe(shift, recipe, player, (CraftingInventory) craftInv)) {
            result = recipeBookMenu.handlePlacement(shift,isCreative, recipe, serverLevel, inventory);
            player.containerMenu.broadcastChanges();
        }
        return result;
    }
}
