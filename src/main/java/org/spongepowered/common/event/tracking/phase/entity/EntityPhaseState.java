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
package org.spongepowered.common.event.tracking.phase.entity;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.ExperienceOrb;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.IEntitySpecificItemDropsState;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.phase.TrackingPhase;
import org.spongepowered.common.event.tracking.phase.TrackingPhases;
import org.spongepowered.common.registry.type.event.SpawnTypeRegistryModule;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public abstract class EntityPhaseState<E extends EntityContext<E>> implements IPhaseState<E>, IEntitySpecificItemDropsState<E> {

    private final String className = this.getClass().getSimpleName();

    @Override
    public final TrackingPhase getPhase() {
        return TrackingPhases.ENTITY;
    }

    @Override
    public boolean doesCaptureEntityDrops(E context) {
        return true;
    }

    // TODO - Find out whether this is used anymore or not.

    @Nullable
    public net.minecraft.entity.Entity returnTeleportResult(PhaseContext<?> context, MoveEntityEvent.Teleport.Portal event) {
        return null;
    }
    @Override
    public void unwind(E context) {

    }

    @Override
    public String toString() {
        return this.getPhase() + "{" + this.className + "}";
    }

    void standardSpawnCapturedEntities(PhaseContext<?> context, List<Entity> entities) {
        // Separate experience orbs from other entity drops
        final List<Entity> experience = entities.stream()
            .filter(entity -> entity instanceof ExperienceOrb)
            .collect(Collectors.toList());
        if (!experience.isEmpty()) {
            Sponge.getCauseStackManager().addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.EXPERIENCE);
            SpongeCommonEventFactory.callSpawnEntity(experience, context);

        }

        // Now process other entities, this is separate from item drops specifically
        final List<Entity> other = entities.stream()
            .filter(entity -> !(entity instanceof ExperienceOrb))
            .collect(Collectors.toList());
        if (!other.isEmpty()) {
            Sponge.getCauseStackManager().addContext(EventContextKeys.SPAWN_TYPE, SpawnTypeRegistryModule.ENTITY_DEATH);
            SpongeCommonEventFactory.callSpawnEntity(experience, context);
        }
    }
}


