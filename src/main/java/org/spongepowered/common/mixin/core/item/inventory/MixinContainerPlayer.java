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
package org.spongepowered.common.mixin.core.item.inventory;

import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.slots.CraftingOutputAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.slots.EquipmentSlotAdapter;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.LensProvider;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.lens.impl.collections.SlotCollection;
import org.spongepowered.common.item.inventory.lens.impl.minecraft.container.ContainerPlayerInventoryLens;
import org.spongepowered.common.item.inventory.lens.impl.slots.CraftingOutputSlotLensImpl;

@Mixin(ContainerPlayer.class)
public abstract class MixinContainerPlayer extends MixinContainer implements LensProvider<IInventory, ItemStack> {

    @Override
    public Lens<IInventory, ItemStack> rootLens(Fabric<IInventory> fabric, InventoryAdapter<IInventory, ItemStack> adapter) {
        return new ContainerPlayerInventoryLens(adapter, inventory$getSlotProvider());
    }

    @Override
    public SlotProvider<IInventory, ItemStack> slotProvider(Fabric<IInventory> fabric, InventoryAdapter<IInventory, ItemStack> adapter) {
        SlotCollection.Builder builder = new SlotCollection.Builder()
                .add(1, CraftingOutputAdapter.class, (i) -> new CraftingOutputSlotLensImpl(i, (t) -> false, (t) -> false))
                .add(4)
                .add(4, EquipmentSlotAdapter.class)
                .add(36)
                .add(1);
        builder.add(this.inventorySlots.size() - 46); // Add additional slots (e.g. from mods)
        return builder.build();
    }
}
