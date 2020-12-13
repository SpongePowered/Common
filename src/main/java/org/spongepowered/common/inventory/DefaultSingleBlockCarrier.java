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
package org.spongepowered.common.inventory;

import net.minecraft.inventory.ISidedInventory;
import org.spongepowered.api.item.inventory.BlockCarrier;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.SingleBlockCarrier;
import org.spongepowered.api.item.inventory.query.Query;
import org.spongepowered.api.item.inventory.query.QueryTypes;
import org.spongepowered.api.item.inventory.slot.SlotMatchers;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.util.Direction;
import org.spongepowered.common.registry.provider.DirectionFacingProvider;
import org.spongepowered.common.util.MissingImplementationException;

import java.util.Arrays;

public interface DefaultSingleBlockCarrier extends SingleBlockCarrier {

    @Override
    default Inventory getInventory(Direction from) {
        return DefaultSingleBlockCarrier.getInventory(from, this);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    default CarriedInventory<? extends Carrier> getInventory() {
        if (this instanceof CarriedInventory) {
            return (CarriedInventory) this;
        }
        // override for non CarriedInventory
        throw new MissingImplementationException("SingleBlockCarrier", "getInventory");
    }

    @SuppressWarnings("deprecation")
    static Inventory getInventory(Direction from, BlockCarrier thisThing) {
        if (thisThing instanceof ISidedInventory) {
            net.minecraft.util.Direction facing = DirectionFacingProvider.getInstance().get(from).get();
            int[] slots = ((ISidedInventory) thisThing).getSlotsForFace(facing);

            if (slots.length == 0) {
                return new EmptyInventoryImpl(thisThing.getInventory());
            }

            // build query for each slot
            Query.Builder builder = Query.builder();
            Arrays.stream(slots).mapToObj(slot -> QueryTypes.KEY_VALUE.get().of(SlotMatchers.index(slot)))
                    .forEach(builder::and);
            Query query = builder.build();

            return thisThing.getInventory().query(query);
        }
        return thisThing.getInventory();
    }
}
