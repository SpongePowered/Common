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

import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.BlockPipeline;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.PipelineCursor;

public final class OldBlockOnReplaceEffect implements ProcessingSideEffect<BlockPipeline, PipelineCursor, BlockChangeArgs, BlockState> {
    private static final class Holder {
        static final OldBlockOnReplaceEffect INSTANCE = new OldBlockOnReplaceEffect();
    }

    OldBlockOnReplaceEffect() {
    }

    public static OldBlockOnReplaceEffect getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public EffectResult<@Nullable BlockState> processSideEffect(
        final BlockPipeline pipeline, final PipelineCursor oldState, final BlockChangeArgs args
    ) {
        // This replaces LevelChunk#setBlockState block:
        /*

        if ($$14) {
            if (this.level instanceof ServerLevel $$17) {
            // -- this block is this side effect
                if (hasBlockEntity && $$16) {
                    BlockEntity $$18 = this.level.getBlockEntity($$0);
                    if ($$18 != null) {
                        $$18.preRemoveSideEffects($$0, $$9, $$15);
                    }
                }

                if (hasBlockEntity) {
                    this.removeBlockEntity($$0);
                }

                if (($$2 & 1) != 0) {
                    $$9.affectNeighborsAfterRemoval($$17, $$0, $$15);
                }
                // -- end of side effect
            } else if (hasBlockEntity) {
                this.removeBlockEntity($$0);
            }
        }
        Notes:
        Since we know we're on the server context, we eliminate
        if (this.level instanceof ServerLevel $$17) {
        And likewise the check if hasBlockEntity, so we just fast forward into
        the checks for physics flags
         */
        boolean hasBlockEntity = oldState.state().hasBlockEntity();
        final var newState = args.newState();
        final var flag = args.flag();
        final var pos = oldState.pos();

        if (!oldState.state().is(newState.getBlock())) {
            return EffectResult.nullPass();
        }
        // Spogne adds the block physics flag
        if (hasBlockEntity && !flag.performBlockDestruction() && flag.performBlockPhysics()) {
            final var blockEntity = oldState.tileEntity();
            if (blockEntity != null) {
                blockEntity.preRemoveSideEffects(pos, newState);
            }
        }

        if (hasBlockEntity) {
            pipeline.getAffectedChunk().removeBlockEntity(pos);
        }

        if (flag.updateNeighbors()) {
            oldState.state().affectNeighborsAfterRemoval(pipeline.getServerWorld(), pos, flag.movingBlocks());
        }

        return EffectResult.nullPass();
    }
}
