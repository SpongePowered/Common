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
package org.spongepowered.common.event.tracking.context.transaction.block;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.event.tracking.BlockChangeFlagManager;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.GameTransaction;
import org.spongepowered.common.event.tracking.context.transaction.effect.BlockAddedEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.CheckBlockPostPlacementIsSameEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.ChunkChangeCompleteEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.OldBlockOnReplaceEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.SetBlockToChunkSectionEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.UpdateChunkLightManagerEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.UpdateHeightMapEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.UpdateLightSideEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.UpdateOrCreateNewTileEntityPostPlacementEffect;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.ChunkPipeline;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.PrettyPrinter;
import org.spongepowered.common.world.BlockChange;
import org.spongepowered.common.world.SpongeBlockChangeFlag;

import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

@DefaultQualifier(NonNull.class)
public final class ChangeBlock extends BlockEventBasedTransaction {

    public final SpongeBlockSnapshot original;
    final int originalOpacity;
    final BlockState newState;
    final SpongeBlockChangeFlag blockChangeFlag;
    @Nullable public BlockEntity queuedRemoval;
    @Nullable public BlockEntity queuedAdd;

    public ChangeBlock(
        final SpongeBlockSnapshot attachedSnapshot, final BlockState newState,
        final SpongeBlockChangeFlag blockChange
    ) {
        super(attachedSnapshot.getBlockPos(), (BlockState) attachedSnapshot.state(), attachedSnapshot.world());
        this.original = attachedSnapshot;
        this.newState = newState;
        this.blockChangeFlag = blockChange;
        this.originalOpacity = this.originalState.getLightBlock();
    }

    public BlockState getNewState() {
        return this.newState;
    }

    public SpongeBlockChangeFlag getBlockChangeFlag() {
        return this.blockChangeFlag;
    }

    public void populateChunkEffects(final ChunkPipeline.Builder builder) {

        builder.addEffect(SetBlockToChunkSectionEffect.getInstance());
        builder.addEffect(UpdateHeightMapEffect.getInstance());
        builder.addEffect(UpdateChunkLightManagerEffect.getInstance());
        builder.addEffect(UpdateLightSideEffect.getInstance());
        builder.addEffect(OldBlockOnReplaceEffect.getInstance());
        builder.addEffect(CheckBlockPostPlacementIsSameEffect.getInstance());
        builder.addEffect(BlockAddedEffect.getInstance());
        builder.addEffect(UpdateOrCreateNewTileEntityPostPlacementEffect.getInstance());
        builder.addEffect(ChunkChangeCompleteEffect.getInstance());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ChangeBlock.class.getSimpleName() + "[", "]")
            .add("affectedPosition=" + this.affectedPosition)
            .add("originalState=" + this.originalState)
            .add("originalOpacity=" + this.originalOpacity)
            .add("newState=" + this.newState)
            .add("blockChangeFlag=" + this.blockChangeFlag)
            .add("queuedRemoval=" + this.queuedRemoval)
            .add("queuedAdd=" + this.queuedAdd)
            .add("worldKey=" + this.worldKey)
            .add("cancelled=" + this.cancelled)
            .toString();
    }

    @Override
    public Optional<BiConsumer<PhaseContext<@NonNull ?>, CauseStackManager.StackFrame>> getFrameMutator(
        @Nullable GameTransaction<@NonNull ?> parent
    ) {
        return Optional.of(PhaseContext::addCreatorAndNotifierToCauseStack);
    }

    @Override
    public boolean acceptTileAddition(final BlockEntity tileEntity) {
        if (this.queuedAdd == tileEntity) {
            return true;
        }
        if (this.queuedAdd != null) {
            return false;
        }
        if (!this.affectedPosition.equals(tileEntity.getBlockPos())) {
            return false;
        }
        this.queuedAdd = tileEntity;
        return true;
    }

    @Override
    public boolean acceptTileRemoval(final @Nullable BlockEntity tileentity) {
        if (this.queuedRemoval == tileentity) {
            return true;
        }
        if (this.queuedRemoval != null) {
            return false;
        }
        if (!this.affectedPosition.equals(tileentity.getBlockPos())) {
            return false;
        }
        this.queuedRemoval = tileentity;
        return true;
    }

    @Override
    public boolean acceptTileReplacement(final @Nullable BlockEntity existing, final BlockEntity proposed) {
        return this.acceptTileRemoval(existing) && this.acceptTileAddition(proposed);
    }

    @Override
    public void restore(PhaseContext<?> context, ChangeBlockEvent.All event) {
        this.original.restore(true, BlockChangeFlagManager.fromNativeInt(Constants.BlockChangeFlags.FORCED_RESTORE));
    }

    @Override
    public void addToPrinter(final PrettyPrinter printer) {
        printer.add("ChangeBlock")
            .add(" %s : %s", "Original Block", this.original)
            .add(" %s : %s", "New State", this.newState)
            .add(" %s : %s", "RemovedTile", this.queuedRemoval)
            .add(" %s : %s", "AddedTile", this.queuedAdd)
            .add(" %s : %s", "ChangeFlag", this.blockChangeFlag);
    }

    @Override
    protected SpongeBlockSnapshot getResultingSnapshot() {
        final SpongeBlockSnapshot.BuilderImpl builder = SpongeBlockSnapshot.BuilderImpl.pooled()
                .position(this.original.position())
                .blockState((org.spongepowered.api.block.BlockState) this.newState);
        if (this.original.getServerWorld().isPresent()) {
            builder.world(this.original.getServerWorld().get());
        } else {
            builder.world(this.original.world());
        }
        return builder.build();
    }

    @Override
    protected SpongeBlockSnapshot getOriginalSnapshot() {
        return this.original;
    }

    @Override
    public boolean absorbBlockDropsPreparation(
        final PhaseContext<@NonNull ?> context, final PrepareBlockDropsTransaction transaction
    ) {
        return this.original.blockChange == BlockChange.BREAK
            && this.affectedPosition.equals(transaction.affectedPosition)
            && this.original.state() == transaction.getOriginalSnapshot().state();
    }
}
