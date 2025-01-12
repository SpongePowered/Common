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
package org.spongepowered.common.accessor.world.level;

import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccessor {

    @Accessor("spawnDelay") int accessor$spawnDelay();

    @Accessor("spawnDelay") void accessor$spawnDelay(final int spawnDelay);

    @Accessor("spawnPotentials") WeightedList<SpawnData> accessor$spawnPotentials();

    @Accessor("spawnPotentials") void accessor$spawnPotentials(WeightedList<SpawnData> newData);

    @Accessor("nextSpawnData") SpawnData accessor$nextSpawnData();
    @Accessor("nextSpawnData") void accessor$nextSpawnData(SpawnData spawnData);

    @Accessor("minSpawnDelay") int accessor$minSpawnDelay();

    @Accessor("minSpawnDelay") void accessor$minSpawnDelay(final int minSpawnDelay);

    @Accessor("maxSpawnDelay") int accessor$maxSpawnDelay();

    @Accessor("maxSpawnDelay") void accessor$maxSpawnDelay(final int maxSpawnDelay);

    @Accessor("spawnCount") int accessor$spawnCount();

    @Accessor("spawnCount") void accessor$spawnCount(final int spawnCount);

    @Accessor("maxNearbyEntities") int accessor$maxNearbyEntities();

    @Accessor("maxNearbyEntities") void accessor$maxNearbyEntities(final int maxNearbyEntities);

    @Accessor("requiredPlayerRange") int accessor$requiredPlayerRange();

    @Accessor("requiredPlayerRange") void accessor$requiredPlayerRange(final int requiredPlayerRange);

    @Accessor("spawnRange") int accessor$spawnRange();

    @Accessor("spawnRange") void accessor$spawnRange(final int spawnRange);

}
