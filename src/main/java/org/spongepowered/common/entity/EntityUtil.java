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
package org.spongepowered.common.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.SpawnData;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.EntityArchetype;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.util.weighted.WeightedObject;
import org.spongepowered.api.util.weighted.WeightedSerializableObject;
import org.spongepowered.api.util.weighted.WeightedTable;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.world.entity.EntityAccessor;
import org.spongepowered.common.bridge.CreatorTrackedBridge;
import org.spongepowered.common.bridge.data.VanishableBridge;
import org.spongepowered.common.data.persistence.NBTTranslator;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.util.Constants;
import org.spongepowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class EntityUtil {

    private EntityUtil() {
    }

    public static void despawnFilteredEntities(final Iterable<? extends Entity> originalEntities, final SpawnEntityEvent event) {
        if (event.isCancelled()) {
            for (final Entity e : originalEntities) {
                e.discard();
            }
        } else {
            for (Entity e : originalEntities) {
                if (!event.entities().contains(e)) {
                    e.discard();
                }
            }
        }
    }

    public static boolean processEntitySpawn(final org.spongepowered.api.entity.Entity entity, final Supplier<Optional<UUID>> supplier, final Consumer<Entity> spawner) {
        final Entity minecraftEntity = (Entity) entity;
        if (minecraftEntity instanceof ItemEntity) {
            final ItemStack item = ((ItemEntity) minecraftEntity).getItem();
            if (!item.isEmpty()) {
                final Optional<Entity> customEntityItem = Optional.ofNullable(PlatformHooks.INSTANCE.getWorldHooks().getCustomEntityIfItem(minecraftEntity));
                if (customEntityItem.isPresent()) {
                    // Bypass spawning the entity item, since it is established that the custom entity is spawned.
                    final Entity entityToSpawn = customEntityItem.get();
                    supplier.get()
                        .ifPresent(spawned -> {
                            if (entityToSpawn instanceof CreatorTrackedBridge) {
                                ((CreatorTrackedBridge) entityToSpawn).tracker$setTrackedUUID(PlayerTracker.Type.CREATOR, spawned);
                            }
                        });
                    if (entityToSpawn.isRemoved()) {
                        ((EntityAccessor) entityToSpawn).invoker$unsetRemoved();
                    }
                    // Since forge already has a new event thrown for the entity, we don't need to throw
                    // the event anymore as sponge plugins getting the event after forge mods will
                    // have the modified entity list for entities, so no need to re-capture the entities.
                    entityToSpawn.level().addFreshEntity(entityToSpawn);
                    return true;
                }
            }
        }

        // Allowed to call force spawn directly since we've applied creator and custom item logic already
        spawner.accept((Entity) entity);
        return true;
    }

    public static Collection<org.spongepowered.api.entity.Entity> spawnEntities(
            final Iterable<? extends org.spongepowered.api.entity.Entity> entities,
            final Predicate<org.spongepowered.api.entity.Entity> selector,
            final Consumer<Entity> spawning) {

        final List<org.spongepowered.api.entity.Entity> entitiesToSpawn = new ArrayList<>();
        for (final org.spongepowered.api.entity.Entity e : entities) {
            if (selector.test(e)) {
                entitiesToSpawn.add(e);
            }
        }
        final SpawnEntityEvent.Custom event = SpongeEventFactory.createSpawnEntityEventCustom(PhaseTracker.getCauseStackManager().currentCause(), entitiesToSpawn);
        if (Sponge.eventManager().post(event)) {
            return Collections.emptyList();
        }
        for (final org.spongepowered.api.entity.Entity entity : event.entities()) {
            EntityUtil.processEntitySpawn(entity, Optional::empty, spawning);
        }
        return Collections.unmodifiableCollection(new ArrayList<>(event.entities()));
    }

    /**
     * This is used to create the "dropping" motion for items caused by players. This
     * specifically was being used (and should be the correct math) to drop from the
     * player, when we do item stack captures preventing entity items being created.
     *
     * @param dropAround True if it's being "dropped around the player like dying"
     * @param player The player to drop around from
     * @param random The random instance
     * @return The motion vector
     */
    @SuppressWarnings("unused")
    private static Vector3d createDropMotion(final boolean dropAround, final Player player, final Random random) {
        double x;
        double y;
        double z;
        if (dropAround) {
            final float f = random.nextFloat() * 0.5F;
            final float f1 = random.nextFloat() * ((float) Math.PI * 2F);
            x = -Mth.sin(f1) * f;
            z = Mth.cos(f1) * f;
            y = 0.20000000298023224D;
        } else {
            float f2 = 0.3F;
            x = -Mth.sin(player.getYRot() * 0.017453292F) * Mth.cos(player.getXRot() * 0.017453292F) * f2;
            z = Mth.cos(player.getYRot() * 0.017453292F) * Mth.cos(player.getXRot() * 0.017453292F) * f2;
            y = - Mth.sin(player.getXRot() * 0.017453292F) * f2 + 0.1F;
            final float f3 = random.nextFloat() * ((float) Math.PI * 2F);
            f2 = 0.02F * random.nextFloat();
            x += Math.cos(f3) * f2;
            y += (random.nextFloat() - random.nextFloat()) * 0.1F;
            z += Math.sin(f3) * f2;
        }
        return new Vector3d(x, y, z);
    }


    public static boolean isUntargetable(final Entity from, final Entity target) {
        if (((VanishableBridge) target).bridge$vanishState().untargetable()) {
            return true;
        }
        // Temporary fix for https://bugs.mojang.com/browse/MC-149563
        return from.level() != target.level();
    }

    public static EntityArchetype toArchetype(final SpawnData logic) {
        final var tag = logic.entityToSpawn();
        final var resourceLocation = tag.getString(Constants.Entity.ENTITY_TYPE_ID);
        final var entityTypeRegistry = SpongeCommon.vanillaRegistry(Registries.ENTITY_TYPE);
        final var type = entityTypeRegistry.getOptional(ResourceLocation.parse(resourceLocation))
            .map(org.spongepowered.api.entity.EntityType.class::cast)
            .orElse(EntityTypes.PIG.get());

        return SpongeEntityArchetypeBuilder.pooled()
                .type(type)
                .entityData(NBTTranslator.INSTANCE.translateFrom(tag))
                .build();
    }

    public static SpawnData toSpawnData(final EntityArchetype value) {
        final var tag = NBTTranslator.INSTANCE.translate(value.entityData());
        if (!tag.contains(Constants.Entity.ENTITY_TYPE_ID)) {
            final ResourceKey key = (ResourceKey) (Object) EntityType.getKey((EntityType<?>) value.type());
            tag.putString(Constants.Entity.ENTITY_TYPE_ID, key.toString());
        }

        // TODO customSpawnRules & equipment support
        return new SpawnData(tag, Optional.empty(), Optional.empty());
    }

    public static WeightedTable<EntityArchetype> toWeightedArchetypes(final WeightedList<SpawnData> spawnData) {
        final WeightedTable<EntityArchetype> possibleEntities = new WeightedTable<>();

        for (final var weightedEntity : spawnData.unwrap()) {
            final CompoundTag nbt = weightedEntity.value().entityToSpawn();
            final String resourceLocation = nbt.getString(Constants.Entity.ENTITY_TYPE_ID);
            final var mcType = SpongeCommon.vanillaRegistry(Registries.ENTITY_TYPE).getOptional(ResourceLocation.parse(resourceLocation));
            final var type = mcType.map(org.spongepowered.api.entity.EntityType.class::cast).orElse(EntityTypes.PIG.get());

            final EntityArchetype archetype = SpongeEntityArchetypeBuilder.pooled()
                    .type(type)
                    .entityData(NBTTranslator.INSTANCE.translateFrom(nbt))
                    .build();

            possibleEntities.add(new WeightedSerializableObject<>(archetype, weightedEntity.weight()));
        }
        return possibleEntities;
    }

    public static WeightedList<SpawnData> toSpawnPotentials(final WeightedTable<EntityArchetype> table) {
        final WeightedList.Builder<SpawnData> builder = WeightedList.builder();
        for (final var entry : table) {
            if (entry instanceof final WeightedObject<EntityArchetype> weightedObj) {
                final var tag = NBTTranslator.INSTANCE.translate(weightedObj.get().entityData());
                if (!tag.contains(Constants.Entity.ENTITY_TYPE_ID)) {
                    final var key = EntityType.getKey((EntityType<?>) weightedObj.get().type());
                    tag.putString(Constants.Entity.ENTITY_TYPE_ID, key.toString());
                }
                // TODO customSpawnRules & equipment support
                builder.add(new SpawnData(tag, Optional.empty(), Optional.empty()), (int) entry.weight());
            }
        }
        return builder.build();
    }
}
