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
package org.spongepowered.common.event.tracking.context.transaction.type;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.block.ScheduleBlockUpdateEvent;
import org.spongepowered.api.event.entity.HarvestEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.item.inventory.AffectSlotEvent;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.container.ClickContainerEvent;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.registry.DefaultedRegistryReference;
import org.spongepowered.api.registry.RegistryKey;
import org.spongepowered.api.registry.RegistryScope;
import org.spongepowered.api.registry.RegistryScopes;
import org.spongepowered.common.registry.SpongeRegistryTypes;

@SuppressWarnings("unused")
@RegistryScopes(scopes = RegistryScope.GAME)
public final class TransactionTypes {

    // @formatter:off

    // SORTFIELDS:ON

    public static final DefaultedRegistryReference<TransactionType<ChangeBlockEvent.All>> BLOCK = TransactionTypes.key(ResourceKey.sponge("block"));

    public static final DefaultedRegistryReference<TransactionType<ChangeInventoryEvent>> CHANGE_INVENTORY_EVENT = TransactionTypes.key(ResourceKey.sponge("change_inventory"));

    public static final DefaultedRegistryReference<TransactionType<ClickContainerEvent>> CLICK_CONTAINER_EVENT = TransactionTypes.key(ResourceKey.sponge("click_container"));

    public static final DefaultedRegistryReference<TransactionType<HarvestEntityEvent>> ENTITY_DEATH_DROPS = TransactionTypes.key(ResourceKey.sponge("entity_death_drops"));

    public static final DefaultedRegistryReference<TransactionType<InteractContainerEvent>> INTERACT_CONTAINER_EVENT = TransactionTypes.key(ResourceKey.sponge("interact_container"));

    public static final DefaultedRegistryReference<TransactionType<NotifyNeighborBlockEvent>> NEIGHBOR_NOTIFICATION = TransactionTypes.key(ResourceKey.sponge("neighbor_notification"));

    public static final DefaultedRegistryReference<TransactionType<ScheduleBlockUpdateEvent<?>>> SCHEDULE_BLOCK_UPDATE = TransactionTypes.key(ResourceKey.sponge("schedule_block_update"));

    public static final DefaultedRegistryReference<TransactionType<AffectSlotEvent>> SLOT_CHANGE = TransactionTypes.key(ResourceKey.sponge("slot_change"));

    public static final DefaultedRegistryReference<TransactionType<SpawnEntityEvent>> SPAWN_ENTITY = TransactionTypes.key(ResourceKey.sponge("spawn_entity"));

    // SORTFIELDS:OFF

    // @formatter:on

    private TransactionTypes() {
    }

    private static <T extends Event & Cancellable> DefaultedRegistryReference<TransactionType<T>> key(final ResourceKey location) {
        return RegistryKey.of(SpongeRegistryTypes.TRACKER_TRANSACTION_TYPE, location).asDefaultedReference(Sponge::game);
    }
}
