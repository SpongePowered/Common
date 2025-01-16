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
package org.spongepowered.common.mixin.inventory.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.bridge.world.inventory.InventoryBridge;
import org.spongepowered.common.event.inventory.InventoryEventFactory;
import org.spongepowered.common.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.inventory.lens.Lens;
import org.spongepowered.common.inventory.lens.impl.comp.EquipmentInventoryLens;
import org.spongepowered.common.inventory.lens.slots.SlotLens;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.Map;
import java.util.Optional;

@Mixin(value = LivingEntity.class)
public abstract class LivingEntityMixin_Inventory extends Entity {

    // @formatter:off
    @Shadow @Final private Map<EquipmentSlot, ItemStack> lastEquipmentItems;

    @Shadow public abstract void shadow$setItemSlot(EquipmentSlot slotIn, ItemStack stack);
    @Shadow protected abstract void shadow$completeUsingItem();
    // @formatter:on


    protected LivingEntityMixin_Inventory(final EntityType<?> param0, final Level param1) {
        super(param0, param1);
    }

    @Inject(method = "handleHandSwap", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;broadcast(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/protocol/Packet;)V"))
    protected void inventory$onHandleHandSwap(final Map<EquipmentSlot, ItemStack> map, final CallbackInfo ci) {
        final Slot mainHand = this.impl$getSpongeSlot(EquipmentSlot.MAINHAND);
        final var lastMainHand = this.lastEquipmentItems.get(EquipmentSlot.MAINHAND);
        final boolean customMainHand = this.impl$throwEquipmentEvent(EquipmentSlot.MAINHAND, mainHand, map.get(EquipmentSlot.MAINHAND), lastMainHand);
        final var lastOffHand = this.lastEquipmentItems.get(EquipmentSlot.OFFHAND);
        final Slot offHand = this.impl$getSpongeSlot(EquipmentSlot.OFFHAND);
        final boolean customOffHand = this.impl$throwEquipmentEvent(EquipmentSlot.OFFHAND, offHand, map.get(EquipmentSlot.OFFHAND), lastOffHand);
        if (customMainHand || customOffHand) {
            ci.cancel(); // If canceled or customized let handleEquipmentChanges send packets instead
        }
    }

    @Inject(method = "handleEquipmentChanges",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"))
    protected void inventory$onHandleEquipmentChanges(final Map<EquipmentSlot, ItemStack> map, final CallbackInfo ci) {
        map.entrySet().forEach(entry -> {
            final Slot slotAdapter = this.impl$getSpongeSlot(entry.getKey());
            final ItemStack oldStack = this.lastEquipmentItems.get(entry.getKey());
            entry.setValue(this.impl$callEquipmentEvent(entry.getKey(), slotAdapter, entry.getValue(), oldStack));
        });
    }

    @Redirect(method = "updateUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;completeUsingItem()V"))
    protected void inventory$onUpdateUsingItem(final LivingEntity thisEntity) {
        this.shadow$completeUsingItem();
    }

    private ItemStack impl$callEquipmentEvent(final EquipmentSlot equipmentslottype, final Slot slot, final ItemStack newStack, final ItemStack oldStack) {
        final ChangeEntityEquipmentEvent event = InventoryEventFactory.callChangeEntityEquipmentEvent((LivingEntity) (Object) this,
                ItemStackUtil.snapshotOf(oldStack), ItemStackUtil.snapshotOf(newStack), slot);
        if (event.isCancelled()) {
            this.shadow$setItemSlot(equipmentslottype, oldStack);
            return oldStack;
        }
        final Transaction<@NonNull ItemStackSnapshot> transaction = event.transaction();
        if (!transaction.isValid()) {
            this.shadow$setItemSlot(equipmentslottype, oldStack);
            return oldStack;
        }
        final Optional<ItemStackSnapshot> optional = transaction.custom();
        if (optional.isPresent()) {
            final ItemStack custom = ItemStackUtil.fromSnapshotToNative(optional.get());
            this.shadow$setItemSlot(equipmentslottype, custom);
            return custom;
        }
        return newStack;
    }

    private boolean impl$throwEquipmentEvent(final EquipmentSlot equipmentslottype, final Slot slot, final ItemStack newStack, final ItemStack oldStack) {
        final ChangeEntityEquipmentEvent event = InventoryEventFactory.callChangeEntityEquipmentEvent((LivingEntity) (Object) this,
                ItemStackUtil.snapshotOf(oldStack), ItemStackUtil.snapshotOf(newStack), slot);
        if (event.isCancelled()) {
            this.shadow$setItemSlot(equipmentslottype, oldStack);
            return true;
        }
        final Transaction<@NonNull ItemStackSnapshot> transaction = event.transaction();
        if (!transaction.isValid()) {
            this.shadow$setItemSlot(equipmentslottype, oldStack);
            return true;
        }
        final Optional<ItemStackSnapshot> optional = transaction.custom();
        if (optional.isPresent()) {
            final ItemStack custom = ItemStackUtil.fromSnapshotToNative(optional.get());
            this.shadow$setItemSlot(equipmentslottype, custom);
            return true;
        }
        return false;
    }

    @SuppressWarnings("ConstantConditions")
    protected Slot impl$getSpongeSlot(final EquipmentSlot equipmentSlot) {
        final EquipmentType equipmentType = (EquipmentType) (Object) equipmentSlot;
        if (this instanceof InventoryBridge) {
            final InventoryAdapter adapter = ((InventoryBridge) this).bridge$getAdapter();
            final Lens lens = adapter.inventoryAdapter$getRootLens();
            if (lens instanceof EquipmentInventoryLens) {
                final SlotLens slotLens = ((EquipmentInventoryLens) lens).getSlotLens(equipmentType);
                return slotLens.getAdapter(adapter.inventoryAdapter$getFabric(), (Inventory) adapter);
            }
            throw new IllegalStateException("Expected EquipmentInventoryLens for " + this.getClass().getName() + " Inventory but found: " + lens.getClass().getName());
        }

        throw new IllegalStateException("Living Entity has no InventoryAdapter: " + this.getClass().getName());
    }

}
