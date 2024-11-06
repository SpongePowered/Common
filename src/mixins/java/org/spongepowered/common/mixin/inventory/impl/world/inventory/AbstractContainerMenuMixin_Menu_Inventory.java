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
package org.spongepowered.common.mixin.inventory.impl.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.world.inventory.AbstractContainerMenu_InventoryBridge;
import org.spongepowered.common.bridge.world.inventory.container.MenuBridge;
import org.spongepowered.common.inventory.custom.SpongeInventoryMenu;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin_Menu_Inventory implements MenuBridge {

    // @formatter:off
    @Shadow @Final private NonNullList<ItemStack> lastSlots;
    @Shadow @Final public NonNullList<Slot> slots;

    @Shadow public abstract void shadow$sendAllDataToRemote();
    @Shadow protected abstract boolean shadow$tryItemClickBehaviourOverride(Player $$0, ClickAction $$1, Slot $$2, ItemStack $$3, ItemStack $$4);
    // @formatter:on

    @Nullable private SpongeInventoryMenu impl$menu;

    @Override
    public void bridge$setMenu(final SpongeInventoryMenu menu) {
        this.impl$menu = menu;
    }

    @Nullable @Override
    public SpongeInventoryMenu bridge$getMenu() {
        return this.impl$menu;
    }

    // Called when clicking in an inventory
    // InventoryMenu Callback
    @Inject(method = "doClick", at = @At(value = "HEAD"), cancellable = true)
    private void impl$onClick(final int slotId, final int dragType, final ClickType clickTypeIn, final Player player, final CallbackInfo ci) {
        final SpongeInventoryMenu menu = this.bridge$getMenu();
        if (menu != null) {
            if (!menu.onClick(slotId, dragType, clickTypeIn, player, (org.spongepowered.api.item.inventory.Container) this)) {
                ci.cancel();
                // Accept all changes made by plugin
                for (int i = 0; i < this.slots.size(); i++) {
                    final Slot slot = this.slots.get(i);
                    this.lastSlots.set(i, slot.getItem().copy());
                }
                // and update client
                this.shadow$sendAllDataToRemote();
            }
        }
    }

    // Called when a Container is closed
    // InventoryMenu Callback and resetting viewed and menu state
    @Inject(method = "removed", at = @At(value = "HEAD"))
    private void impl$onOnContainerClosed(final Player player, final CallbackInfo ci) {
        final SpongeInventoryMenu menu = this.bridge$getMenu();
        if (menu != null) {
            menu.onClose(player, (org.spongepowered.api.item.inventory.Container) this);
        }
        this.bridge$setMenu(null);
    }

    @Redirect(method = "doClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;mayPlace(Lnet/minecraft/world/item/ItemStack;)Z"))
    public boolean impl$onClickMayPlace(final Slot slot, final ItemStack stack) {
        return this.impl$onMayPlace(slot, stack);
    }

    @Redirect(method = "moveItemStackTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;mayPlace(Lnet/minecraft/world/item/ItemStack;)Z"))
    public boolean impl$onMoveMayPlace(final Slot slot, final ItemStack stack) {
        return this.impl$onMayPlace(slot, stack);
    }

    private boolean impl$onMayPlace(final Slot slot, final ItemStack stack) {
        if (this.bridge$isReadonlyMenu(slot)) {
            ((AbstractContainerMenu_InventoryBridge) this).bridge$markDirty();
            return false;
        }
        return slot.mayPlace(stack);
    }

    @Redirect(method = "doClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;mayPickup(Lnet/minecraft/world/entity/player/Player;)Z"))
    public boolean impl$verifyReadOnlyMenu(final Slot slot, final Player player) {
        if (this.bridge$isReadonlyMenu(slot)) {
            ((AbstractContainerMenu_InventoryBridge) this).bridge$markDirty();
            return false;
        }
        return slot.mayPickup(player);
    }

    @Redirect(method = "moveItemStackTo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;", ordinal = 0))
    public ItemStack impl$beforeMergeStack(final Slot slot) {
        if (this.bridge$isReadonlyMenu(slot)) {
            ((AbstractContainerMenu_InventoryBridge) this).bridge$markDirty();
            return ItemStack.EMPTY;
        }
        return slot.getItem();
    }

    @Override
    public boolean bridge$isReadonlyMenu(final Slot slot) {
        return this.impl$menu != null && slot.container == this.impl$menu.inventory()
                && this.impl$menu.isReadOnly(slot.index);
    }

    @Redirect(method = "doClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;tryItemClickBehaviourOverride(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/ClickAction;Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    public boolean impl$onTryItemClickBehaviourOverride(final AbstractContainerMenu instance, final Player $$0, final ClickAction $$1, final Slot $$2, final ItemStack $$3, final ItemStack $$4) {
        if (this.bridge$isReadonlyMenu($$2)) {
            ((AbstractContainerMenu_InventoryBridge) this).bridge$markDirty();
            return true;
        }
        return this.shadow$tryItemClickBehaviourOverride($$0, $$1, $$2, $$3, $$4);
    }
}
