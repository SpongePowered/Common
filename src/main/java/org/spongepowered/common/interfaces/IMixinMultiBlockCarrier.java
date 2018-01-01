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
package org.spongepowered.common.interfaces;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.util.EnumFacing;
import org.spongepowered.api.item.inventory.EmptyInventory;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.MultiBlockCarrier;
import org.spongepowered.api.item.inventory.SingleBlockCarrier;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.registry.provider.DirectionFacingProvider;

import java.util.Optional;

public interface IMixinMultiBlockCarrier extends MultiBlockCarrier {

    default Location<World> getLocation() {
        return this.getLocations().get(0);
    }

    @Override
    default Optional<Inventory> getInventory(Location<World> at) {
        if (this.getLocations().contains(at)) {
            return Optional.of(this.getInventory());
        }
        return Optional.empty();
    }

    @Override
    default Optional<Inventory> getInventory(Location<World> at, Direction from) {
        return this.getInventory(at);
    }

    @Override
    default Inventory getInventory(Direction from) {
        return IMixinSingleBlockCarrier.getInventory(from, this);
    }
}
