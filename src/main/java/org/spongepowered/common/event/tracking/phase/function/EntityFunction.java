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
package org.spongepowered.common.event.tracking.phase.function;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.ExperienceOrb;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.spawn.EntitySpawnCause;
import org.spongepowered.common.event.EventConsumer;
import org.spongepowered.common.event.InternalNamedCauses;
import org.spongepowered.common.event.tracking.CauseTracker;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.registry.type.event.InternalSpawnTypes;

import java.util.List;
import java.util.stream.Collectors;

public class EntityFunction {

    public interface Drops {


        /**
         * Specifically during the dropitems on death for an entity.
         */
        Drops DEATH_DROPS = (targetEntity, causeTracker, context, items) -> {
            final DamageSource damageSource = context.firstNamed(InternalNamedCauses.General.DAMAGE_SOURCE, DamageSource.class).get();
            final Cause cause = Cause.source(EntitySpawnCause.builder()
                        .entity(targetEntity)
                        .type(InternalSpawnTypes.DROPPED_ITEM)
                        .build())
                    .named(InternalNamedCauses.General.DAMAGE_SOURCE, damageSource)
                    .build();
            destructItemsWithCause(items, cause, causeTracker);
        };
        /**
         * Any extra things that occur during onDeathUpdate.
         */
        Drops DEATH_UPDATES = (targetEntity, causeTracker, context, items) -> {
            final Cause cause = Cause.source(EntitySpawnCause.builder()
                        .entity(targetEntity)
                        .type(InternalSpawnTypes.DROPPED_ITEM)
                        .build())
                    .build();
            destructItemsWithCause(items, cause, causeTracker);
        };

        void process(Entity targetEntity, CauseTracker causeTracker, PhaseContext context, List<Entity> items);
    }


    public interface Entities {

        /**
         * Specifically during the dropitems on death for an entity.
         */
        Entities DEATH_DROPS = (targetEntity, causeTracker, context, entities) -> {
            final DamageSource damageSource = context.firstNamed(InternalNamedCauses.General.DAMAGE_SOURCE, DamageSource.class).get();
            final List<Entity> experience = entities.stream().filter(entity -> entity instanceof ExperienceOrb).collect(Collectors.toList());
            if (!experience.isEmpty()) {
                final Cause cause = Cause.source(EntitySpawnCause.builder()
                        .entity(targetEntity)
                        .type(InternalSpawnTypes.EXPERIENCE)
                        .build())
                        .named(InternalNamedCauses.General.DAMAGE_SOURCE, damageSource)
                        .build();
                processEntitiesWithCause(experience, cause, causeTracker);

            }

            final List<Entity> other = entities.stream().filter(entity -> !(entity instanceof ExperienceOrb)).collect(Collectors.toList());
            if (!other.isEmpty()) {
                final Cause cause = Cause.source(EntitySpawnCause.builder()
                        .entity(targetEntity)
                        .type(InternalSpawnTypes.ENTITY_DEATH)
                        .build())
                        .named(InternalNamedCauses.General.DAMAGE_SOURCE, damageSource)
                        .build();
                processEntitiesWithCause(other, cause, causeTracker);

            }
        };
        /**
         * Any extra things that occur during onDeathUpdate.
         */
        Entities DEATH_UPDATES = (targetEntity, causeTracker, context, entities) -> {
            final List<Entity> experience = entities.stream().filter(entity -> entity instanceof ExperienceOrb).collect(Collectors.toList());
            if (!experience.isEmpty()) {
                final Cause cause = Cause.source(EntitySpawnCause.builder()
                        .entity(targetEntity)
                        .type(InternalSpawnTypes.EXPERIENCE)
                        .build())
                        .build();
                processEntitiesWithCause(experience, cause, causeTracker);

            }

            final List<Entity> other = entities.stream().filter(entity -> !(entity instanceof ExperienceOrb)).collect(Collectors.toList());
            if (!other.isEmpty()) {
                final Cause cause = Cause.source(EntitySpawnCause.builder()
                        .entity(targetEntity)
                        .type(InternalSpawnTypes.ENTITY_DEATH)
                        .build())
                        .build();
                processEntitiesWithCause(other, cause, causeTracker);
            }
        };

        void process(Entity targetEntity, CauseTracker causeTracker, PhaseContext context, List<Entity> entities);

    }

    static void destructItemsWithCause(List<Entity> items, Cause cause, CauseTracker causeTracker) {
        final List<EntitySnapshot> snapshots = items.stream().map(Entity::createSnapshot).collect(Collectors.toList());
        EventConsumer.event(SpongeEventFactory.createDropItemEventDestruct(cause, items, snapshots, causeTracker.getWorld()))
                .nonCancelled(event -> EntityListConsumer.FORCE_SPAWN.apply(event.getEntities(), causeTracker))
                .process();
    }

    static void processEntitiesWithCause(List<Entity> entityList, Cause cause, CauseTracker causeTracker) {
        final List<EntitySnapshot> snapshots = entityList.stream().map(Entity::createSnapshot).collect(Collectors.toList());
        EventConsumer.event(SpongeEventFactory.createSpawnEntityEvent(cause, entityList, snapshots, causeTracker.getWorld()))
                .nonCancelled(event -> EntityListConsumer.FORCE_SPAWN.apply(event.getEntities(), causeTracker))
                .process();
    }

}
