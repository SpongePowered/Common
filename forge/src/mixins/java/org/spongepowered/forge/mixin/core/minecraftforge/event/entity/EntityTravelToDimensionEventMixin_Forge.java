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
package org.spongepowered.forge.mixin.core.minecraftforge.event.entity;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.forge.launch.bridge.event.ForgeEventBridge_Forge;

@Mixin(value = EntityTravelToDimensionEvent.class, remap = false)
public abstract class EntityTravelToDimensionEventMixin_Forge extends EntityEvent implements ForgeEventBridge_Forge<ChangeEntityWorldEvent.Pre> {

    // @formatter:off
    @Shadow @Final @Mutable private ResourceKey<Level> dimension;
    // @formatter:on

    private EntityTravelToDimensionEventMixin_Forge(Entity entity) {
        super(entity);
    }

    @Override
    public void bridge$syncFrom(final ChangeEntityWorldEvent.Pre event) {
        this.setCanceled(event.isCancelled());
        this.dimension = ((ServerLevel) event.destinationWorld()).dimension();
    }

    @Override
    public void bridge$syncTo(final ChangeEntityWorldEvent.Pre event) {
        event.setCancelled(this.isCanceled());
    }

    @Override
    public ChangeEntityWorldEvent.@Nullable Pre bridge$createSpongeEvent() {
        final Entity entity = this.getEntity();
        final ServerLevel toWorld = SpongeCommon.server().getLevel(this.dimension);
        return SpongeEventFactory.createChangeEntityWorldEventPre(PhaseTracker.getCauseStackManager().currentCause(),
                (org.spongepowered.api.entity.Entity) entity, (org.spongepowered.api.world.server.ServerWorld) entity.getCommandSenderWorld(),
                (org.spongepowered.api.world.server.ServerWorld) toWorld, (org.spongepowered.api.world.server.ServerWorld) toWorld);
    }

}
