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
package org.spongepowered.common.mixin.plugin.entityactivation;

import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.AABB;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.explosive.fused.FusedExplosive;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.server.level.ServerLevelAccessor;
import org.spongepowered.common.accessor.world.entity.EntityAccessor;
import org.spongepowered.common.accessor.world.level.entity.PersistentEntitySectionManagerAccessor;
import org.spongepowered.common.accessor.world.phys.AABBAccessor;
import org.spongepowered.common.bridge.TrackableBridge;
import org.spongepowered.common.bridge.activation.ActivationCapabilityBridge;
import org.spongepowered.common.bridge.world.entity.EntityTypeBridge;
import org.spongepowered.common.bridge.world.entity.PlatformEntityBridge;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.bridge.world.level.chunk.ActiveChunkReferantBridge;
import org.spongepowered.common.bridge.world.level.chunk.LevelChunkBridge;
import org.spongepowered.common.config.SpongeGameConfigs;
import org.spongepowered.common.config.inheritable.EntityActivationRangeCategory;
import org.spongepowered.common.config.inheritable.GlobalConfig;
import org.spongepowered.common.config.inheritable.InheritableConfigHandle;
import org.spongepowered.common.config.inheritable.WorldConfig;

import java.util.HashMap;
import java.util.Map;

public final class EntityActivationRange {

    private static final ImmutableMap<Byte, String> activationTypeMappings = new ImmutableMap.Builder<Byte, String>()
        .put((byte) 1, "monster")
        .put((byte) 2, "creature")
        .put((byte) 3, "aquatic")
        .put((byte) 4, "ambient")
        .put((byte) 5, "misc")
        .build();

    static AABB maxBB = new AABB(0, 0, 0, 0, 0, 0);
    static AABB miscBB = new AABB(0, 0, 0, 0, 0, 0);
    static AABB creatureBB = new AABB(0, 0, 0, 0, 0, 0);
    static AABB monsterBB = new AABB(0, 0, 0, 0, 0, 0);
    static AABB aquaticBB = new AABB(0, 0, 0, 0, 0, 0);
    static AABB ambientBB = new AABB(0, 0, 0, 0, 0, 0);
    static AABB tileEntityBB = new AABB(0, 0, 0, 0, 0, 0);
    static Map<Byte, Integer> maxActivationRanges = new HashMap<>();

    /**
     * Initializes an entities type on construction to specify what group this
     * entity is in for activation ranges.
     *
     * @param entity Entity to get type for
     * @return group id
     */
    public static byte initializeEntityActivationType(final Entity entity) {
        if (entity instanceof Enemy) {
            return 1; // Monster
        } else if (entity instanceof Animal) {
            return 2; // Creature
        } else if (entity instanceof WaterAnimal) {
            return 3; // Aquatic
        } else if (entity instanceof AmbientCreature) {
            return 4; // Ambient
        } else {
            return 5; // Misc
        }
    }

    /**
     * Initialize entity activation state.
     *
     * @param entity Entity to check
     */
    public static void initializeEntityActivationState(final Entity entity) {
        final ActivationCapabilityBridge spongeEntity = (ActivationCapabilityBridge) entity;
        if (entity.level().isClientSide()) {
            return;
        }

        // types that should always be active
        if (entity instanceof Player && !((PlatformEntityBridge) entity).bridge$isFakePlayer()
            || entity instanceof ThrowableProjectile
            || entity instanceof EnderDragon
            || entity instanceof EnderDragonPart
            || entity instanceof WitherBoss
            || entity instanceof AbstractHurtingProjectile
            || entity instanceof LightningBolt
            || entity instanceof PrimedTnt
            || entity instanceof Painting
            || entity instanceof EndCrystal
            || entity instanceof FireworkRocketEntity
            || entity instanceof FallingBlockEntity) // Always tick falling blocks
        {
            return;
        }

        final InheritableConfigHandle<WorldConfig> configAdapter = SpongeGameConfigs.getForWorld(entity.level());
        final EntityActivationRangeCategory config = configAdapter.get().entityActivationRange;

        final EntityTypeBridge type = (EntityTypeBridge) entity.getType();
        final ResourceLocation key = EntityType.getKey(entity.getType());
        final byte activationType = spongeEntity.activation$getActivationType();
        final String activationTypeName = EntityActivationRange.activationTypeMappings.getOrDefault(activationType, "misc");
        if (!type.bridge$isActivationRangeInitialized()) {
            EntityActivationRange.addEntityToConfig(config.autoPopulate, key, activationType, activationTypeName);
            type.bridge$setActivationRangeInitialized(true);
        }

        final EntityActivationRangeCategory.ModSubCategory entityMod = config.mods.get(key.getNamespace());
        final int defaultActivationRange = config.globalRanges.get(activationTypeName);

        if (entityMod == null) {
            // use default activation range
            spongeEntity.activation$setActivationRange(defaultActivationRange);
            if (defaultActivationRange > 0) {
                spongeEntity.activation$setDefaultActivationState(false);
            }
        } else {
            if (!entityMod.enabled) {
                spongeEntity.activation$setDefaultActivationState(true);
                return;
            }

            final Integer defaultModActivationRange = entityMod.defaultRanges.get(activationTypeName);
            final Integer entityActivationRange = entityMod.entities.get(key.getPath());
            if (defaultModActivationRange != null && entityActivationRange == null) {
                spongeEntity.activation$setActivationRange(defaultModActivationRange);
                if (defaultModActivationRange > 0) {
                    spongeEntity.activation$setDefaultActivationState(false);
                }
            } else if (entityActivationRange != null) {
                spongeEntity.activation$setActivationRange(entityActivationRange);
                if (entityActivationRange > 0) {
                    spongeEntity.activation$setDefaultActivationState(false);
                }
            }
        }
    }

    /**
     * Utility method to grow an AABB without creating a new AABB or touching
     * the pool, so we can re-use ones we have.
     *
     * @param target The AABB to modify
     * @param source The AABB to get initial coordinates from
     * @param x The x value to expand by
     * @param y The y value to expand by
     * @param z The z value to expand by
     * @return An AABB
     */
    public static AABB growBb(final AABB target, final AABB source, final int x, final int y, final int z) {
        ((AABBAccessor) target).accessor$setMinX(source.minX - x);
        ((AABBAccessor) target).accessor$setMinY(source.minY - y);
        ((AABBAccessor) target).accessor$setMinZ(source.minZ - z);
        ((AABBAccessor) target).accessor$setMaxX(source.maxX + x);
        ((AABBAccessor) target).accessor$setMaxY(source.maxY + y);
        ((AABBAccessor) target).accessor$setMaxZ(source.maxZ + z);
        return target;
    }

    /**
     * Find what entities are in range of the players in the world and set
     * active if in range.
     *
     * @param world The world to perform activation checks in
     */
    public static void activateEntities(final ServerLevel world) {
        if (((LevelBridge) world).bridge$isFake()) {
            return;
        }

        for (final ServerPlayer player : world.players()) {
            int maxRange = 0;
            for (final Integer range : EntityActivationRange.maxActivationRanges.values()) {
                if (range > maxRange) {
                    maxRange = range;
                }
            }

            maxRange = Math.min((((ServerWorld) world).properties().viewDistance() << 4) - 8, maxRange);
            ((ActivationCapabilityBridge) player).activation$setActivatedTick(SpongeCommon.server().getTickCount());
            final AABB aabb = EntityActivationRange.maxBB;
            EntityActivationRange.growBb(aabb, player.getBoundingBox(), maxRange, 256, maxRange);

            final int i = Mth.floor(aabb.minX / 16.0D);
            final int j = Mth.floor(aabb.maxX / 16.0D);
            final int k = Mth.floor(aabb.minZ / 16.0D);
            final int l = Mth.floor(aabb.maxZ / 16.0D);

            for (int i1 = i; i1 <= j; ++i1) {
                for (int j1 = k; j1 <= l; ++j1) {
                    final LevelChunk chunk = world.getChunkSource().getChunkNow(i1, j1);
                    if (chunk != null) {
                        EntityActivationRange.activateChunkEntities(player, chunk);
                    }
                }
            }
        }
    }

    /**
     * Checks for the activation state of all entities in this chunk.
     *
     * @param chunk Chunk to check for activation
     */
    private static void activateChunkEntities(final ServerPlayer player, final LevelChunk chunk) {
        final PersistentEntitySectionManager<Entity> entityManager = ((ServerLevelAccessor) chunk.getLevel()).accessor$getEntityManager();
        final EntitySectionStorage<Entity> entitySectionStorage = ((PersistentEntitySectionManagerAccessor<Entity>) entityManager).accessor$sectionStorage();
        entitySectionStorage.getExistingSectionsInChunk(chunk.getPos().toLong()).flatMap(EntitySection::getEntities).forEach(entity -> {
            if (!entity.chunkPosition().equals(chunk.getPos())) {
                return;
            }
            final ActivationCapabilityBridge spongeEntity = (ActivationCapabilityBridge) entity;
            final long currentTick = SpongeCommon.server().getTickCount();
            if (!((TrackableBridge) entity).bridge$shouldTick()) {
                return;
            }
            if (currentTick <= spongeEntity.activation$getActivatedTick()) {
                return;
            }
            if (spongeEntity.activation$getDefaultActivationState()) {
                EntityActivationRange.initializeEntityActivationState(entity);
                spongeEntity.activation$setActivatedTick(currentTick);
                return;
            }

            // check for entity type overrides
            AABB aabb;
            switch (spongeEntity.activation$getActivationType()) {
                case 5:
                    aabb = EntityActivationRange.miscBB;
                    break;
                case 4:
                    aabb = EntityActivationRange.ambientBB;
                    break;
                case 3:
                    aabb = EntityActivationRange.aquaticBB;
                    break;
                case 2:
                    aabb = EntityActivationRange.creatureBB;
                    break;
                default:
                    aabb = EntityActivationRange.monsterBB;
                    break;
            }

            final int bbActivationRange = spongeEntity.activation$getActivationRange();
            EntityActivationRange.growBb(aabb, player.getBoundingBox(), bbActivationRange, 256, bbActivationRange);
            if (aabb.intersects(entity.getBoundingBox())) {
                spongeEntity.activation$setActivatedTick(currentTick);
            }
        });
    }

    /**
     * If an entity is not in range, do some more checks to see if we should
     * give it a shot.
     *
     * @param entity Entity to check
     * @return Whether entity should still be maintained active
     */
    public static boolean checkEntityImmunities(final Entity entity) {
        // quick checks.
        if (entity.isInWater() || ((EntityAccessor) entity).accessor$remainingFireTicks() > 0) {
            return true;
        }
        if (!(entity instanceof Projectile)) {
            if (!entity.getPassengers().isEmpty() || entity.getVehicle() != null) {
                return true;
            }
        } else if (!entity.onGround()) {
            return true;
        }

        // special cases.
        if (entity instanceof LivingEntity) {
            final LivingEntity living = (LivingEntity) entity;
            if (living.hurtTime > 0 || living.getActiveEffects().size() > 0) {
                return true;
            }

            if (entity instanceof Mob && ((LivingEntity) entity).getLastHurtByMob() != null || ((LivingEntity) entity).getLastHurtMob() != null) {
                return true;
            }
            if (entity instanceof Villager && ((Villager) entity).canBreed()) {
                return true;
            }
            if (entity instanceof Animal) {
                final Animal animal = (Animal) entity;
                if (animal.isBaby() || animal.isInLove()) {
                    return true;
                }
                if (entity instanceof Sheep && ((Sheep) entity).isSheared()) {
                    return true;
                }
            }

            return entity instanceof FusedExplosive && ((FusedExplosive) entity).get(Keys.IS_PRIMED).orElse(false);
        }
        return false;
    }

    /**
     * Checks if the entity is active for this tick.
     *
     * @param entity The entity to check for activity
     * @return Whether the given entity should be active
     */
    public static boolean checkIfActive(final Entity entity) {
        // Never safe to skip fireworks or entities not yet added to chunk
        if (entity instanceof Player || entity.level().isClientSide() || entity.touchingUnloadedChunk() || entity instanceof FireworkRocketEntity) {
            return true;
        }
        final LevelChunkBridge activeChunk = ((ActiveChunkReferantBridge) entity).bridge$getActiveChunk();
        if (activeChunk == null) {
            // Should never happen but just in case for mods, always tick
            return true;
        }

        if (!activeChunk.bridge$isActive()) {
            return false;
        }

        // If in forced chunk or is player
        if (activeChunk.bridge$isPersistedChunk() || ((PlatformEntityBridge) entity).bridge$isFakePlayer() && entity instanceof ServerPlayer) {
            return true;
        }

        final long currentTick = SpongeCommon.server().getTickCount();
        final ActivationCapabilityBridge spongeEntity = (ActivationCapabilityBridge) entity;
        boolean isActive = spongeEntity.activation$getActivatedTick() >= currentTick || spongeEntity.activation$getDefaultActivationState();

        // Should this entity tick?
        if (!isActive) {
            if ((currentTick - spongeEntity.activation$getActivatedTick() - 1) % 20 == 0) {
                // Check immunities every 20 ticks.
                if (EntityActivationRange.checkEntityImmunities(entity)) {
                    // Triggered some sort of immunity, give 20 full ticks before we check again.
                    spongeEntity.activation$setActivatedTick(currentTick + 20);
                }
                isActive = true;
            }
            // Add a little performance juice to active entities. Skip 1/4 if not immune.
        } else if (!spongeEntity.activation$getDefaultActivationState() && entity.tickCount % 4 == 0 && !EntityActivationRange
            .checkEntityImmunities(entity)) {
            isActive = false;
        }

        if (isActive && !activeChunk.bridge$areNeighborsLoaded()) {
            isActive = false;
        }

        return isActive;
    }

    public static void addEntityToConfig(
        final boolean autoPopulate, final ResourceLocation key, final byte activationType, final String activationTypeName
    ) {
        final InheritableConfigHandle<GlobalConfig> globalConfig = SpongeGameConfigs.getGlobalInheritable();
        final EntityActivationRangeCategory activationConfig = globalConfig.get().entityActivationRange;

        boolean requiresSave = false;

        final Integer defaultRange = activationConfig.globalRanges.getOrDefault(activationTypeName, 32);
        Integer range = defaultRange;

        EntityActivationRangeCategory.ModSubCategory modSubCategory = activationConfig.mods.get(key.getNamespace());
        if (autoPopulate && modSubCategory == null) {
            modSubCategory = new EntityActivationRangeCategory.ModSubCategory();
            activationConfig.mods.put(key.getNamespace(), modSubCategory);
            requiresSave = true;
        }
        if (modSubCategory != null) {
            final Integer modActivationRange = modSubCategory.defaultRanges.get(activationTypeName);
            if (autoPopulate && modActivationRange == null) {
                modSubCategory.defaultRanges.put(activationTypeName, defaultRange);
                requiresSave = true;
            } else if (modActivationRange != null && modActivationRange > range) {
                range = modActivationRange;
            }

            final Integer entityActivationRange = modSubCategory.entities.get(key.getPath());
            if (autoPopulate && entityActivationRange == null) {
                modSubCategory.entities.put(key.getPath(), modSubCategory.defaultRanges.get(activationTypeName));
                requiresSave = true;
            }
            if (entityActivationRange != null && entityActivationRange > range) {
                range = entityActivationRange;
            }
        }

        // check max ranges
        final int newRange = range;
        EntityActivationRange.maxActivationRanges
            .compute(activationType, (k, maxRange) -> maxRange == null || newRange > maxRange ? newRange : maxRange);

        if (autoPopulate && requiresSave) {
            globalConfig.save();
        }
    }
}
