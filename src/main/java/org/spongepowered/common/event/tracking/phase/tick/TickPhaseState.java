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
package org.spongepowered.common.event.tracking.phase.tick;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PooledPhaseState;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.phase.general.ExplosionContext;

abstract class TickPhaseState<C extends TickContext<C>> extends PooledPhaseState<C> implements IPhaseState<C> {

    @Override
    public void unwind(final C phaseContext) { }

    @Override
    public void associateNeighborStateNotifier(final C context, final @Nullable BlockPos sourcePos, final Block block, final BlockPos notifyPos,
                                               final ServerLevel minecraftWorld, final PlayerTracker.Type notifier) {

    }

    @Override
    public void appendNotifierPreBlockTick(final ServerLevel world, final BlockPos pos, final C context, final LocationBasedTickContext<@NonNull ?> phaseContext) {

    }

    @Override
    public void appendContextPreExplosion(final ExplosionContext explosionContext, final C context) {

    }

    private final String desc = TrackingUtil.phaseStateToString("Tick", this);

    @Override
    public String toString() {
        return this.desc;
    }
}
