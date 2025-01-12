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
package org.spongepowered.common.mixin.api.minecraft.world.level.block.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import org.spongepowered.api.block.entity.TrialSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.accessor.world.level.block.entity.trialspawner.TrialSpawnerDataAccessor;

import java.util.Optional;

@Mixin(TrialSpawnerBlockEntity.class)
public abstract class TrialSpawnerBlockEntityMixin_API extends BlockEntityMixin_API implements TrialSpawner {

    @Shadow public abstract net.minecraft.world.level.block.entity.trialspawner.TrialSpawner getTrialSpawner();

    @Override
    public void spawnImmediately(final boolean force) {
        if (this.level.isClientSide) {
            return;
        }
        final var thisLevel = (ServerLevel) this.level;
        final var spawner = this.getTrialSpawner();
        final var spawnerData = (TrialSpawnerDataAccessor) spawner.getData();
        final var spawnerConfig = spawner.getConfig();

        final var additionalPlayers = spawner.getData().countAdditionalPlayers(this.worldPosition);
        final var nextSpawnAt = spawnerData.accessor$nextMobSpawnsAt();
        spawnerData.accessor$nextMobSpawnsAt(0);
        if (force || spawner.getData().isReadyToSpawnNextMob(thisLevel, spawnerConfig, additionalPlayers)) {
            // See TrialSpawnerState#tickAndGetNext
            spawner.spawnMob(thisLevel, this.worldPosition).ifPresent(spawned -> {
                spawnerData.accessor$currentMobs().add(spawned);
                spawnerData.accessor$totalMobsSpawned(spawnerData.accessor$totalMobsSpawned() + 1);
                spawnerData.accessor$nextMobSpawnsAt(this.level.getGameTime() + (long) spawnerConfig.ticksBetweenSpawn());
                spawnerConfig.spawnPotentialsDefinition().getRandom(this.level.getRandom()).ifPresent(next -> {
                    spawnerData.accessor$nextSpawnData(Optional.of(next));
                    spawner.markUpdated();
                });
            });
        } else {
            ((TrialSpawnerDataAccessor) spawnerData).accessor$nextMobSpawnsAt(nextSpawnAt);
        }
    }
}
