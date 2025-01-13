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
package org.spongepowered.common.mixin.inventory.event.server.level;

import net.minecraft.server.level.ServerLevel;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.world.entity.EntityBridge;
import org.spongepowered.common.bridge.world.entity.player.PlayerInventoryBridge;
import org.spongepowered.common.bridge.world.inventory.ViewableInventoryBridge;
import org.spongepowered.common.event.inventory.InventoryEventFactory;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.context.transaction.TransactionalCaptureSupplier;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhaseUtil;
import org.spongepowered.common.event.tracking.phase.packet.inventory.SwapHandItemsState;
import org.spongepowered.common.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.inventory.fabric.Fabric;
import org.spongepowered.common.inventory.lens.Lens;
import org.spongepowered.common.inventory.lens.impl.minecraft.PlayerInventoryLens;
import org.spongepowered.common.inventory.lens.slots.SlotLens;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.mixin.inventory.event.entity.player.PlayerMixin_Inventory;

import java.util.Collections;
import java.util.Map;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin_Inventory extends PlayerMixin_Inventory {

    // @formatter:off
    @Shadow public ServerGamePacketListenerImpl connection;
    @Nullable private EffectTransactor inventory$effectTransactor = null;
    @Nullable private Object inventory$menuProvider;
    // @formatter:on

    @Shadow public abstract ServerLevel shadow$serverLevel();
    // @formatter:on

    // Ignore
    ServerPlayerMixin_Inventory(final EntityType<?> param0, final Level param1) {
        super(param0, param1);
    }

    // -- Overrides from PlayerMixin_Inventory

    @Override
    protected void impl$beforeSetItemSlot(final EquipmentSlot param0, final ItemStack param1, final CallbackInfo ci) {
        final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance(this.shadow$serverLevel()).getPhaseContext();
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        final PlayerInventoryTransaction.EventCreator eventCreator = context.getState() instanceof SwapHandItemsState ?
                PlayerInventoryTransaction.EventCreator.SWAP_HAND : PlayerInventoryTransaction.EventCreator.STANDARD;
        this.inventory$effectTransactor = transactor.logPlayerInventoryChangeWithEffect((Player) (Object) this, eventCreator);
    }

    @Override
    protected void impl$afterSetItemSlot(final EquipmentSlot param0, final ItemStack param1, final CallbackInfo ci) {
        try (final EffectTransactor ignored = this.inventory$effectTransactor) {
            this.inventoryMenu.broadcastChanges(); // for capture
        } finally {
            this.inventory$effectTransactor = null;
        }
    }

    @Inject(method = "drop(Z)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;removeFromSelected(Z)Lnet/minecraft/world/item/ItemStack;"))
    protected void impl$beforeRemoveItem(final boolean param0, final CallbackInfoReturnable<Boolean> cir) {
        final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance(this.shadow$serverLevel()).getPhaseContext();
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        this.inventory$effectTransactor = transactor.logDropFromPlayerInventory((ServerPlayer) (Object) this, param0);
    }

    @Inject(method = "drop(Z)Z",
            at = @At(value = "RETURN"))
    protected void impl$onPlayerDrop(final boolean param0, final CallbackInfoReturnable<Boolean> cir) {
        try (final EffectTransactor ignored = this.inventory$effectTransactor) {
            this.containerMenu.broadcastChanges(); // for capture
        } finally {
            this.inventory$effectTransactor = null;
        }
    }

    protected void inventory$onTouch(final Entity entity, final Player player) {
        if (entity instanceof ItemEntity) {
            entity.playerTouch(player); // ItemEntityMixin_Inventory creates transactions for pickup event
            return;
        }
        if (!((EntityBridge) entity).bridge$isPlayerTouchDeclared()) {
            entity.playerTouch(player);
            return;
        }
        final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance(this.shadow$serverLevel()).getPhaseContext();
        try (final EffectTransactor ignored = context.getTransactor().logPlayerInventoryChangeWithEffect(player, PlayerInventoryTransaction.EventCreator.STANDARD)) {
            entity.playerTouch(player);
            this.inventoryMenu.broadcastChanges(); // capture
        }

    }

    @Override
    protected void inventory$onHandleHandSwap(final Map<EquipmentSlot, ItemStack> map, final CallbackInfo ci) {
        // For players ChangeInventoryEvent.SwapHand is called somewhere else
    }

    @Override
    protected void inventory$onUpdateUsingItem(final LivingEntity thisPlayer) {
        final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance(this.shadow$serverLevel()).getPhaseContext();
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        try (final EffectTransactor ignored = transactor.logPlayerInventoryChangeWithEffect((ServerPlayer) (Object) this, PlayerInventoryTransaction.EventCreator.STANDARD)) {
            this.shadow$completeUsingItem();
            this.inventoryMenu.broadcastChanges();
        }
    }

    // -- Normal redirects

    @Inject(
        method = "openHorseInventory",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;initMenu(Lnet/minecraft/world/inventory/AbstractContainerMenu;)V",
            shift = At.Shift.AFTER
        )
    )
    private void impl$afterOpenHorseInventory(final AbstractHorse $$0, final Container $$1, final CallbackInfo ci) {
        PhaseTracker.getWorldInstance(this.shadow$serverLevel()).getPhaseContext()
            .getTransactor()
            .logContainerSet((ServerPlayer) (Object) this);
    }

    @Inject(method = "openHorseInventory", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;initMenu(Lnet/minecraft/world/inventory/AbstractContainerMenu;)V"))
    private void impl$onOpenHorseInventory(final AbstractHorse horse, final Container inventoryIn, final CallbackInfo ci) {
        this.inventory$menuProvider = inventoryIn;
    }

    @Inject(method = "initMenu", at = @At("HEAD"))
    private void impl$onInitMenu(final AbstractContainerMenu $$0, final CallbackInfo ci) {
        if (this.inventory$menuProvider instanceof ViewableInventoryBridge bridge) {
            bridge.viewableBridge$addPlayer(((ServerPlayer) (Object) this));
        }
    }

    @Inject(method = "doCloseContainer", at = @At("RETURN"))
    private void impl$onDoCloseContainer(final CallbackInfo ci) {
        this.impl$onCloseMenu();
    }

    private void impl$onCloseMenu() {
        if (this.inventory$menuProvider instanceof ViewableInventoryBridge bridge) {
            bridge.viewableBridge$removePlayer(((ServerPlayer) (Object) this));
        }
        this.inventory$menuProvider = null;
    }

    @WrapOperation(
        method = "openHorseInventory",
        at = @At(
            value = "NEW",
            target = "(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/animal/horse/AbstractHorse;I)Lnet/minecraft/world/inventory/HorseInventoryMenu;"
        )
    )
    private HorseInventoryMenu impl$transactHorseInventoryMenuCreationWithEffect(
        final int $$0, final net.minecraft.world.entity.player.Inventory $$1, final Container $$2, final AbstractHorse $$3, final int $$4,
        final Operation<HorseInventoryMenu> original, final @Cancellable CallbackInfo ci
    ) {
        final HorseInventoryMenu menu = original.call($$0, $$1, $$2, $$3, $$4);
        if (!InventoryEventFactory.callInteractContainerOpenEvent((ServerPlayer) (Object) this, menu)) {
            ci.cancel();
            return menu;
        }
        this.connection.send(new ClientboundHorseScreenOpenPacket(menu.containerId, $$4, $$3.getId()));
        return menu;
    }

    // -- small bridge-like methods


    @SuppressWarnings("ConstantConditions")
    @Override
    protected Slot impl$getSpongeSlot(
        final EquipmentSlot equipmentSlot
    ) {
        final EquipmentType equipmentType = (EquipmentType) (Object) equipmentSlot;
        final PlayerInventoryBridge inventory = (PlayerInventoryBridge) ((net.minecraft.server.level.ServerPlayer) (Object) this).getInventory();
        final Lens lens = ((InventoryAdapter) inventory).inventoryAdapter$getRootLens();
        final Fabric fabric = ((InventoryAdapter) inventory).inventoryAdapter$getFabric();
        if (lens instanceof PlayerInventoryLens) {
            final SlotLens slotLens = ((PlayerInventoryLens) lens).getEquipmentLens().getSlotLens(equipmentType);
            return slotLens.getAdapter(fabric, (Inventory) inventory);
        }
        throw new IllegalStateException("Unknown Lens for Player Inventory: " + lens.getClass().getName());
    }

    @WrapMethod(method = "doCloseContainer")
    private void impl$onPreDoCloseContainer(final Operation<Void> original) {
        final PhaseTracker tracker = PhaseTracker.getWorldInstance(this.shadow$serverLevel());
        final ItemStackSnapshot resultingCursor = ItemStackUtil.snapshotOf(this.containerMenu.getCarried());
        final Transaction<ItemStackSnapshot> cursorTransaction = new Transaction<>(resultingCursor, resultingCursor);
        final InteractContainerEvent.Close event = SpongeEventFactory.createInteractContainerEventClose(
            tracker.currentCause(), (org.spongepowered.api.item.inventory.Container) this.containerMenu,
            cursorTransaction, (Inventory) this.containerMenu, Collections.emptyList());
        SpongeCommon.post(event);
        PacketPhaseUtil.handleCursorRestore((ServerPlayer) (Object) this, event.cursorTransaction(), event.isCancelled());
        if (event.isCancelled() && !(this.containerMenu instanceof InventoryMenu)) {
            final PhaseContext<@NonNull ?> context = tracker.getPhaseContext();
            if (context.isClientSide()) {
                final net.minecraft.world.inventory.Slot slot = this.containerMenu.getSlot(0);
                final net.minecraft.world.Container slotInventory = slot.container;
                final net.minecraft.network.chat.@Nullable Component title;
                // TODO get name from last open
                if (slotInventory instanceof MenuProvider) {
                    title = ((MenuProvider) slotInventory).getDisplayName();
                } else {
                    // expected fallback for unknown types
                    title = null;
                }
                this.connection.send(new ClientboundOpenScreenPacket(this.containerMenu.containerId, this.containerMenu.getType(), title));
            }
            return;
        }
        this.connection.send(new ClientboundContainerClosePacket(this.containerMenu.containerId));
        try (final CauseStackManager.StackFrame frame = tracker.pushCauseFrame()) {
            frame.pushCause(event);
            original.call();
        }
    }

    @WrapOperation(method = "doCloseContainer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;removed(Lnet/minecraft/world/entity/player/Player;)V"))
    private void impl$onDoCloseContainerCaptureRemoval(final AbstractContainerMenu instance, final Player player, final Operation<Void> original) {
        final PhaseTracker tracker = PhaseTracker.getWorldInstance(this.shadow$serverLevel());
        final PhaseContext<@NonNull ?> context = tracker.getPhaseContext();
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        try (final EffectTransactor ignored = transactor.logPlayerInventoryChangeWithEffect(player, PlayerInventoryTransaction.EventCreator.STANDARD)) {
            original.call(instance, player);
            instance.broadcastChanges();
            TrackingUtil.processBlockCaptures(context);
        }
    }
}
