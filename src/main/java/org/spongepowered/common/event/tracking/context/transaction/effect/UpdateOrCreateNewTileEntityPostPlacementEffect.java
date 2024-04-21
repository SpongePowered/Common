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
package org.spongepowered.common.event.tracking.context.transaction.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.accessor.world.level.chunk.LevelChunkAccessor;
import org.spongepowered.common.bridge.world.level.block.state.BlockStateBridge;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.BlockPipeline;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.PipelineCursor;

public final class UpdateOrCreateNewTileEntityPostPlacementEffect implements ProcessingSideEffect<BlockPipeline, PipelineCursor, BlockChangeArgs, BlockState> {

    private static final class Holder {
        static final UpdateOrCreateNewTileEntityPostPlacementEffect INSTANCE = new UpdateOrCreateNewTileEntityPostPlacementEffect();
    }
    UpdateOrCreateNewTileEntityPostPlacementEffect() {
    }

    public static UpdateOrCreateNewTileEntityPostPlacementEffect getInstance() {
        return Holder.INSTANCE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public EffectResult<@Nullable BlockState> processSideEffect(
        final BlockPipeline pipeline, final PipelineCursor oldState, final BlockChangeArgs args
    ) {
        final var newState = args.newState();
        final ServerLevel serverWorld = pipeline.getServerWorld();
        final LevelChunk chunk = pipeline.getAffectedChunk();
        if (((BlockStateBridge) newState).bridge$hasTileEntity()) {
        final @Nullable BlockEntity maybeNewTileEntity = chunk.getBlockEntity(oldState.pos(), LevelChunk.EntityCreationType.CHECK);
            if (maybeNewTileEntity == null) {
                // var15 = ((EntityBlock)var12).newBlockEntity(var1, var2); // Vanilla
                // tileentity1 = state.createTileEntity(this.world); // Forge
                // We cast to our bridge for easy access
                @Nullable final BlockEntity newBlockEntity = ((BlockStateBridge) newState).bridge$createNewTileEntity(serverWorld, oldState.pos());
                if (newBlockEntity != null) {
                    chunk.addAndRegisterBlockEntity(newBlockEntity);
                }
            } else {
                maybeNewTileEntity.setBlockState(newState);
                ((LevelChunkAccessor) chunk).accessor$updateBlockEntityTicker(maybeNewTileEntity);
            }
        }
        return EffectResult.nullPass();
    }
}
