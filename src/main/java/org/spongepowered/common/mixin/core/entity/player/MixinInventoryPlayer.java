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
package org.spongepowered.common.mixin.core.entity.player;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.api.entity.living.Humanoid;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.entity.HumanInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.item.inventory.adapter.impl.MinecraftInventoryAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.comp.HotbarAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.slots.EquipmentSlotAdapter;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.lens.impl.collections.SlotCollection;
import org.spongepowered.common.item.inventory.lens.impl.fabric.DefaultInventoryFabric;
import org.spongepowered.common.item.inventory.lens.impl.minecraft.HumanInventoryLens;
import org.spongepowered.common.item.inventory.observer.InventoryEventArgs;

import java.util.Optional;

@Mixin(InventoryPlayer.class)
public abstract class MixinInventoryPlayer implements MinecraftInventoryAdapter, HumanInventory, IInventory {

    protected SlotCollection slots;
    protected Fabric<IInventory> inventory;
    protected HumanInventoryLens lens;

    private Humanoid carrier;
    private HotbarAdapter hotbar;

    @Inject(method = "<init>*", at = @At("RETURN"), remap = false, require = 1)
    private void onConstructed(EntityPlayer playerIn, CallbackInfo ci) {
        this.inventory = new DefaultInventoryFabric(this);
        this.slots = new SlotCollection.Builder().add(36).add(4, EquipmentSlotAdapter.class).build();
        this.lens = new HumanInventoryLens(this, this.slots);
        this.carrier = playerIn instanceof Humanoid ? (Humanoid) playerIn : null;
    }

    @Override
    public Lens<IInventory, net.minecraft.item.ItemStack> getRootLens() {
        return this.lens;
    }

    @Override
    public Fabric<IInventory> getInventory() {
        return this.inventory;
    }

    @Override
    public Inventory getChild(Lens<IInventory, ItemStack> lens) {
        return null;
    }

    @Override
    public Optional<Humanoid> getCarrier() {
        return Optional.ofNullable(this.carrier);
    }

    @Override
    public Hotbar getHotbar() {
        if (this.hotbar == null) {
            this.hotbar = (HotbarAdapter) this.lens.getHotbar().getAdapter(this.inventory, this);
        }
        return this.hotbar;
    }

    @Override
    public void notify(Object source, InventoryEventArgs eventArgs) {
    }

    @Override
    public SlotProvider<IInventory, ItemStack> getSlotProvider() {
        return this.slots;
    }

}
