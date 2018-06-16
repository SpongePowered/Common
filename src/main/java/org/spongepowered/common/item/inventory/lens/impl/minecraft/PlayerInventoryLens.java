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
package org.spongepowered.common.item.inventory.lens.impl.minecraft;

import static org.spongepowered.api.data.Property.Operator.DELEGATE;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.api.data.Property;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.lens.comp.EquipmentInventoryLens;
import org.spongepowered.common.item.inventory.lens.comp.MainPlayerInventoryLens;
import org.spongepowered.common.item.inventory.lens.impl.RealLens;
import org.spongepowered.common.item.inventory.lens.impl.comp.EquipmentInventoryLensImpl;
import org.spongepowered.common.item.inventory.lens.impl.comp.MainPlayerInventoryLensImpl;
import org.spongepowered.common.item.inventory.lens.impl.comp.OrderedInventoryLensImpl;
import org.spongepowered.common.item.inventory.lens.impl.fabric.ContainerFabric;
import org.spongepowered.common.item.inventory.lens.impl.slots.SlotLensImpl;
import org.spongepowered.common.item.inventory.lens.slots.SlotLens;
import org.spongepowered.common.item.inventory.property.EquipmentSlotTypeImpl;
import org.spongepowered.common.item.inventory.property.SlotIndexImpl;

import java.util.Optional;

public class PlayerInventoryLens extends RealLens {

    private static final int EQUIPMENT = 4;
    private static final int OFFHAND = 1;

    private MainPlayerInventoryLensImpl main;
    private EquipmentInventoryLensImpl equipment;
    private SlotLens<IInventory, ItemStack> offhand;
    private final boolean isContainer;

    public PlayerInventoryLens(InventoryAdapter<IInventory, ItemStack> adapter, SlotProvider<IInventory, ItemStack> slots) {
        super(0, adapter.getFabric().getSize(), adapter, slots);
        this.isContainer = false;
        this.init(slots);
    }

    /**
     * Constructor for ContainerPlayer Inventory
     *
     * @param base The base index
     * @param size The size
     * @param slots The slots
     */
    public PlayerInventoryLens(int base, int size, SlotProvider<IInventory, ItemStack> slots) {
        super(base, size, PlayerInventory.class, slots);
        this.isContainer = true;
        this.init(slots);
    }

    @Override
    protected void init(SlotProvider<IInventory, ItemStack> slots) {
        // Adding slots
        for (int ord = 0, slot = this.base; ord < this.size; ord++, slot++) {
            this.addChild(slots.getSlot(slot), new SlotIndexImpl(ord, DELEGATE));
        }

        int base = this.base;
        if (this.isContainer) {
            this.equipment = new EquipmentInventoryLensImpl(base, EQUIPMENT, 1, slots, true);
            base += EQUIPMENT; // 4
            this.main = new MainPlayerInventoryLensImpl(base, slots, true);
            base += this.main.slotCount();
            this.offhand = slots.getSlot(base);
            base += OFFHAND;
        } else {
            this.main = new MainPlayerInventoryLensImpl(base, slots, false);
            base += this.main.slotCount();
            this.equipment = new EquipmentInventoryLensImpl(base, EQUIPMENT, 1, slots, false);
            base += EQUIPMENT;
            this.offhand = slots.getSlot(base);
            base += OFFHAND;
        }

        finishInit(slots, base);

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public InventoryAdapter<IInventory, ItemStack> getAdapter(Fabric<IInventory> inv, Inventory parent) {
        if (this.isContainer && inv instanceof ContainerFabric) {
            // If Lens is for Container extract the PlayerInventory
            Container container = ((ContainerFabric) inv).getContainer();
            Optional carrier = ((CarriedInventory) container).getCarrier();
            if (carrier.isPresent() && carrier.get() instanceof Player) {
                return ((InventoryAdapter) ((Player) carrier.get()).getInventory());
            }
        }
        return super.getAdapter(inv, parent);
    }

    private void finishInit(SlotProvider<IInventory, ItemStack> slots, int base) {
        this.addSpanningChild(this.main);
        this.addSpanningChild(this.equipment);
        this.addSpanningChild(this.offhand, new EquipmentSlotTypeImpl(EquipmentTypes.OFF_HAND));

        // Additional Slots for bigger modded inventories
        int additionalSlots = this.size - base;
        if (additionalSlots > 0) {
            this.addSpanningChild(new OrderedInventoryLensImpl(base, additionalSlots, 1, slots));
        }
    }

    public MainPlayerInventoryLens<IInventory, ItemStack> getMainLens() {
        return this.main;
    }

    public EquipmentInventoryLens<IInventory, ItemStack> getEquipmentLens() {
        return this.equipment;
    }

    public SlotLens<IInventory, ItemStack> getOffhandLens() {
        return this.offhand;
    }
}
