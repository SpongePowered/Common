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
package org.spongepowered.neoforge.mixin.core.neoforge.event.entity.item;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.item.ItemEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.neoforge.launch.bridge.event.NeoEventBridge_Neo;

import java.util.ArrayList;

@Mixin(ItemTossEvent.class)
public abstract class ItemTossEventMixin_Neo extends ItemEvent implements NeoEventBridge_Neo<DropItemEvent.Dispense>, ICancellableEvent {

    //@formatter:off
    @Shadow private @Final Player player;
    //@formatter:on

    private ItemTossEventMixin_Neo(ItemEntity itemEntity) {
        super(itemEntity);
    }

    @Override
    public void bridge$syncFrom(DropItemEvent.Dispense event) {
        event.entities().stream()
            .filter(e -> e instanceof ItemEntity)
            .map(e -> (ItemEntity) e)
            .findFirst()
            .ifPresent(e -> this.getEntity().setItem(e.getItem()));
        this.setCanceled(event.isCancelled());
    }

    @Override
    public void bridge$syncTo(DropItemEvent.Dispense event) {
        event.setCancelled(this.isCanceled());
    }

    @Override
    public DropItemEvent.@Nullable Dispense bridge$createSpongeEvent() {
        if (this.player.level().isClientSide()) {
            return null;
        }
        try (final var frame = PhaseTracker.getInstance().pushCauseFrame()) {
            frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);
            final var cause = frame.currentCause();
            final var toSpawn = (Entity) this.getEntity();
            final var list = new ArrayList<Entity>();
            list.add(toSpawn);
            return SpongeEventFactory.createDropItemEventDispense(cause, list);
        }
    }
}
