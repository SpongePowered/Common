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
package org.spongepowered.neoforge.mixin.core.neoforge.event.entity.player;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.neoforge.launch.bridge.event.NeoEventBridge_Neo;

@Mixin(value = PlayerEvent.PlayerChangedDimensionEvent.class, remap = false)
public final class  PlayerEvent_PlayerChangedDimensionEventMixin_Neo implements NeoEventBridge_Neo {

    // @formatter:off
    @Shadow @Final private ResourceKey<Level> fromDim;
    @Shadow @Final private ResourceKey<Level> toDim;
    // @formatter:on

    @Override
    public void bridge$syncFrom(final Event event) {
        // nothing to do -- informational only
    }

    @Override
    public void bridge$syncTo(final Event event) {
        // nothing to do -- informational only
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Event bridge$createSpongeEvent() {
        final PlayerEvent.PlayerChangedDimensionEvent thisEvent = (PlayerEvent.PlayerChangedDimensionEvent) (Object) this;
        return SpongeEventFactory.createChangeEntityWorldEventPost(
                PhaseTracker.getCauseStackManager().currentCause(),
                (Entity) thisEvent.getEntity(),
                (ServerWorld) SpongeCommon.server().getLevel(this.fromDim),
                (ServerWorld) SpongeCommon.server().getLevel(this.toDim),
                (ServerWorld) SpongeCommon.server().getLevel(this.toDim)
        );
    }

}
