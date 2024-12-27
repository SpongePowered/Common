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
package org.spongepowered.forge.mixin.tracker.server.level;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.context.transaction.ResultingTransactionBySideEffect;
import org.spongepowered.common.event.tracking.context.transaction.TransactionalCaptureSupplier;
import org.spongepowered.common.event.tracking.context.transaction.effect.InventoryEffect;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin_Forge_Tracker {

    // @formatter:off
    @Shadow @Final protected ServerPlayer player;
    // @formatter:on

    @Redirect(
        method = "destroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;mineBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V"
        )
    )
    private void forgeTracker$pushTransactionAndEffect(
        final ItemStack itemStack, final Level level, final BlockState state, final BlockPos pos,
        final Player player
    ) {
        final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance(player.level()).getPhaseContext();
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        // Log the prepare drops here because Forge rewrites
        // this method to call mine block before calling destroy block
        // and mods will possibly already have performed modifications on
        // the block before we can capture it in level.destroy.
        transactor.logBlockDrops(level, pos, state, level.getBlockEntity(pos));

        itemStack.mineBlock(level, state, pos, player);
        // Needs to get logged as a sideeffect under the BlockChange
        try (EffectTransactor ignored = context.getTransactor().pushEffect(new ResultingTransactionBySideEffect(
            InventoryEffect.getInstance()))) {
            transactor.logPlayerInventoryChange(this.player, PlayerInventoryTransaction.EventCreator.STANDARD);
            this.player.inventoryMenu.broadcastChanges();
        }
    }
}
