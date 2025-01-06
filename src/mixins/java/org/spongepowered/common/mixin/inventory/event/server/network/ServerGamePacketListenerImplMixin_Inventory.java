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
package org.spongepowered.common.mixin.inventory.event.server.network;

import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.world.inventory.container.MenuBridge;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.context.transaction.TransactionalCaptureSupplier;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;
import org.spongepowered.common.inventory.custom.SpongeInventoryMenu;
import org.spongepowered.common.item.util.ItemStackUtil;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin_Inventory {

    @Shadow public ServerPlayer player;

    @Redirect(method = "handleSetCreativeModeSlot",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/InventoryMenu;broadcastChanges()V"))
    private void impl$onBroadcastCreativeActionResult(final InventoryMenu inventoryMenu, final ServerboundSetCreativeModeSlotPacket packetIn) {
        final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance(this.player.serverLevel()).getPhaseContext();
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        final ItemStack itemstack = packetIn.itemStack();

        // TODO handle vanilla sending a bunch of creative events (previously ignoring events within 100ms)
        try (final EffectTransactor ignored = transactor.logCreativeClickContainer(packetIn.slotNum(), ItemStackUtil.snapshotOf(itemstack), this.player)) {
        }
        inventoryMenu.broadcastChanges();
    }

    @Redirect(method = "handleSetCreativeModeSlot",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity impl$onBroadcastCreativeActionResult(final ServerPlayer serverPlayer, final ItemStack stack, final boolean param1) {
        final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance(this.player.serverLevel()).getPhaseContext();
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        try (final EffectTransactor ignored = transactor.logCreativeClickContainer(-1, ItemStackUtil.snapshotOf(stack), this.player)) {
            return serverPlayer.drop(stack, param1);
        }
    }

    // Before setting this.player.inventory.selected
    @Inject(method = "handleSetCarriedItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundSetCarriedItemPacket;getSlot()I"),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;stopUsingItem()V"))
    )
    private void impl$onHandleSetCarriedItem(final ServerboundSetCarriedItemPacket packet, final CallbackInfo ci) {
        final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance(this.player.serverLevel()).getPhaseContext();
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        final int slotIdx = packet.getSlot();
        transactor.logPlayerCarriedItem(this.player, slotIdx);
        // TrackingUtil.processBlockCaptures called by SwitchHotbarScrollState
    }

    @Redirect(method = "handleContainerClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;sendAllDataToRemote()V"))
    private void impl$onSpectatorClick(final AbstractContainerMenu menu, final ServerboundContainerClickPacket packet) {
        final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance(this.player.serverLevel()).getPhaseContext();
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        try (final EffectTransactor ignored = transactor.logClickContainer(menu, packet.getSlotNum(), packet.getButtonNum(), packet.getClickType(),
            this.player
        )) {
            if (menu instanceof MenuBridge bridge) {
                final SpongeInventoryMenu spongeMenu = bridge.bridge$getMenu();
                if (spongeMenu != null) {
                    spongeMenu.onClick(packet.getSlotNum(), packet.getButtonNum(), packet.getClickType(), this.player, ((Container) menu));
                }
            }
            menu.sendAllDataToRemote();
        }
    }

    @Redirect(method = "handleUseItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItem(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult impl$onHandleUseItem(final ServerPlayerGameMode serverPlayerGameMode, final ServerPlayer param0,
            final Level param1, final ItemStack param2, final InteractionHand param3) {
        final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance(this.player.serverLevel()).getPhaseContext();
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        try (final EffectTransactor ignored = transactor.logPlayerInventoryChangeWithEffect(this.player, PlayerInventoryTransaction.EventCreator.STANDARD)) {
            final InteractionResult result = serverPlayerGameMode.useItem(param0, param1, param2, param3);
            this.player.inventoryMenu.broadcastChanges(); // capture
            return result;
        }
        // TrackingUtil.processBlockCaptures called by UseItemPacketState
    }

    @Redirect(method = "handleRenameItem(Lnet/minecraft/network/protocol/game/ServerboundRenameItemPacket;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;setItemName(Ljava/lang/String;)Z"))
    private boolean impl$onHandleRenameItem(final AnvilMenu menu, final String name) {
        final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance(this.player.serverLevel()).getPhaseContext();
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        try (final var ignored = transactor.logIgnoredInventory(this.player.containerMenu)) {
            return menu.setItemName(name);
        }
    }

    @Inject(method = "handleSelectTrade", at = @At("RETURN"))
    private void impl$onHandleSelectTrade(final ServerboundSelectTradePacket param0, final CallbackInfo ci) {
        if (this.player.containerMenu instanceof MerchantMenu) {
            final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance(this.player.serverLevel()).getPhaseContext();
            final TransactionalCaptureSupplier transactor = context.getTransactor();
            transactor.logSelectTrade(this.player, param0.getItem());
            this.player.containerMenu.broadcastChanges(); // capture
        }
    }

}
