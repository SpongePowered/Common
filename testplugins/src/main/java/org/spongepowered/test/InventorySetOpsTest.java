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
package org.spongepowered.test;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.item.inventory.type.GridInventory;
import org.spongepowered.api.item.inventory.type.InventoryColumn;
import org.spongepowered.api.item.inventory.type.InventoryRow;
import org.spongepowered.api.plugin.Plugin;

import javax.inject.Inject;

/**
 * Tests intersect union and containsInventory
 */
@Plugin(id = "inventorysetoperationstest", name = "Inventory Set Operations Test", description = "A plugin to test inventory set operations")
public class InventorySetOpsTest {

    @Inject private Logger logger;

    @Listener
    public void onStart(GameStartedServerEvent event) {
        testIntersect();
        testUnion();

    }

    @Listener
    public void onCmd(SendCommandEvent event) {
        testIntersect(); // TODO remove me once this is all working
        testUnion(); // TODO remove me once this is all working

        if (event.getCause().root() instanceof Player) {
            Player player = (Player) event.getCause().root();

            {
                Inventory chest = Inventory.builder().build(this);
                GridInventory grid = chest.query(GridInventory.class);
                InventoryColumn firstCol = grid.getColumn(0).get();
                InventoryColumn secondCol = grid.getColumn(1).get();
                InventoryRow row2 = grid.getRow(1).get();
                Inventory result = firstCol.union(secondCol).union(row2);
                int i = 1;
                for (Inventory inventory : result.slots()) {
                    inventory.set(ItemStack.of(ItemTypes.APPLE, i++));
                }
                player.openInventory(chest, Cause.source(this).build());
            }
        }
    }

    @Listener
    public void onMidas(ChangeInventoryEvent.Held event, @Root Player player)
    {
        Inventory hotbar = event.getTargetInventory().query(Hotbar.class);
        for (SlotTransaction transaction : event.getTransactions()) {
            if (hotbar.containsInventory(transaction.getSlot())) {
                transaction.setCustom(ItemStack.of(ItemTypes.GOLD_NUGGET, transaction.getOriginal().getCount()));
                System.out.println("You got midas hands!");
            }
        }
    }

    private void testIntersect() {
        Inventory chest = Inventory.builder().build(this);
        Inventory firstSlots = chest.query(SlotIndex.of(0));
        Inventory firstRow = chest.query(InventoryRow.class).first();
        Inventory firstCol = chest.query(InventoryColumn.class).first();
        Inventory intersection = firstSlots.intersect(firstCol).intersect(firstRow);
        Preconditions.checkArgument(intersection.capacity() == 1, "This should be the first slot only!");
        logger.info("Intersect works!");

    }

    private void testUnion() {

        Inventory chest = Inventory.builder().build(this);
        Inventory firstSlots = chest.query(SlotIndex.of(0));
        Inventory firstRow = chest.query(InventoryRow.class).first();
        GridInventory grid = chest.query(GridInventory.class);
        InventoryColumn firstCol = grid.getColumn(0).get();
        InventoryColumn secondCol = grid.getColumn(1).get();
        Inventory union = firstSlots.union(firstCol).union(firstRow);
        Inventory union2 = firstCol.union(firstRow);
        Inventory union3 = firstCol.union(secondCol);
        Preconditions.checkArgument(union.capacity() == 11, "This should include all eleven slot of the first row and column!");
        Preconditions.checkArgument(union2.capacity() == 11, "This should include all eleven slot of the first row and column!");
        Preconditions.checkArgument(union3.capacity() == 6, "This should include all six slot of the first 2 columns!");
        logger.info("Union works!");
    }


}
