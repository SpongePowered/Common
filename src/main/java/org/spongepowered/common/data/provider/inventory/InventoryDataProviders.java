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
package org.spongepowered.common.data.provider.inventory;

import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.common.bridge.inventory.InventoryBridge;
import org.spongepowered.common.data.provider.DataProviderRegistry;
import org.spongepowered.common.data.provider.DataProviderRegistryBuilder;
import org.spongepowered.common.inventory.custom.CustomInventory;
import org.spongepowered.common.inventory.util.InventoryUtil;

import java.util.Optional;
import java.util.UUID;

public class InventoryDataProviders extends DataProviderRegistryBuilder {

    public InventoryDataProviders(DataProviderRegistry registry) {
        super(registry);
    }

    @Override
    public void register() {
        // Lens Providers
        this.register(new GenericSlotLensDataProvider<>(Keys.EQUIPMENT_TYPE));
        this.register(new GenericSlotLensDataProvider<>(Keys.SLOT_INDEX));
        this.register(new GenericSlotLensDataProvider<>(Keys.SLOT_POSITION));
        this.register(new GenericSlotLensDataProvider<>(Keys.SLOT_SIDE));

        this.register(new GenericImmutableInventoryDataProvider<Integer>(Keys.MAX_STACK_SIZE.get()) {
            @Override protected Optional<Integer> getFrom(Inventory dataHolder) {
                return Optional.of(((InventoryBridge)dataHolder).bridge$getAdapter().inventoryAdapter$getFabric().fabric$getMaxStackSize());
            }
        });
        this.register(new GenericImmutableInventoryDataProvider<PluginContainer>(Keys.PLUGIN.get()) {
            @Override protected Optional<PluginContainer> getFrom(Inventory dataHolder) {
                return Optional.ofNullable(InventoryUtil.getPluginContainer(dataHolder));
            }
        });
        this.register(new GenericImmutableInventoryDataProvider<UUID>(Keys.UNIQUE_ID.get()) {
            @Override protected Optional<UUID> getFrom(Inventory dataHolder) {
                if (dataHolder instanceof CustomInventory) {
                    return Optional.ofNullable(((CustomInventory) dataHolder).getIdentity());
                }
                return Optional.empty();
            }
        });
    }
}
