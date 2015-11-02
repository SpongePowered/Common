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
package org.spongepowered.common.interfaces;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.gen.GeneratorPopulator;
import org.spongepowered.api.world.gen.Populator;
import org.spongepowered.common.configuration.SpongeConfig;

import java.util.List;
import java.util.Optional;

public interface IMixinWorld {

    SpongeConfig<SpongeConfig.WorldConfig> getWorldConfig();

    ImmutableList<Populator> getPopulators();

    ImmutableList<GeneratorPopulator> getGeneratorPopulators();

    List<BlockSnapshot> getBlockBreakList();

    List<Entity> getCapturedEntities();

    List<Entity> getCapturedEntityItems();

    BlockSnapshot createSpongeBlockSnapshot(IBlockState state, IBlockState extended, BlockPos pos, int updateFlag);

    boolean isWorldSpawnerRunning();

    boolean isChunkSpawnerRunning();

    boolean capturingBlocks();

    boolean capturingTerrainGen();

    boolean processingCaptureCause();

    boolean restoringBlocks();

    Optional<BlockSnapshot> getCurrentTickBlock();

    Optional<Entity> getCurrentTickEntity();

    Optional<TileEntity> getCurrentTickTileEntity();

    void updateWorldGenerator();

    void handlePostTickCaptures(Cause cause);

    void setProcessingCaptureCause(boolean flag);

    void setWorldSpawnerRunning(boolean flag);

    void setChunkSpawnerRunning(boolean flag);

    void setCapturingTerrainGen(boolean flag);

    void setCapturingEntitySpawns(boolean flag);

    void setCurrentTickBlock(BlockSnapshot snapshot);

    long getWeatherStartTime();

    void setWeatherStartTime(long weatherStartTime);
}
