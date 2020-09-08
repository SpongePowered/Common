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
package org.spongepowered.common.event.tracking.context.transaction;

import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.server.ServerWorld;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.block.SpongeBlockSnapshotBuilder;
import org.spongepowered.common.bridge.tileentity.TileEntityBridge;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.util.PrettyPrinter;
import org.spongepowered.math.vector.Vector3i;

import java.util.Optional;
import java.util.function.BiConsumer;

@DefaultQualifier(NonNull.class)
public final class RemoveTileEntity extends BlockEventBasedTransaction {

    final TileEntity removed;
    final SpongeBlockSnapshot tileSnapshot;

    RemoveTileEntity(final TileEntity removed, final SpongeBlockSnapshot attachedSnapshot) {
        super(attachedSnapshot.getBlockPos(), (BlockState) attachedSnapshot.getState());
        this.removed = removed;
        this.tileSnapshot = attachedSnapshot;
    }

    @Override
    public Optional<BiConsumer<PhaseContext<@NonNull ?>, CauseStackManager.StackFrame>> getFrameMutator() {
        return Optional.empty();
    }

    @Override
    public void addToPrinter(final PrettyPrinter printer) {
        printer.add("RemoveTileEntity")
            .add(" %s : %s", this.affectedPosition, ((TileEntityBridge) this.removed).bridge$getPrettyPrinterString())
            .add(" %s : %s", this.affectedPosition, this.originalState)
        ;
    }

    @Override
    public void restore() {
        this.tileSnapshot.restore(true, BlockChangeFlags.NONE);
    }

    @Override
    protected SpongeBlockSnapshot getResultingSnapshot() {
        return SpongeBlockSnapshotBuilder.pooled()
            .world((ServerWorld) this.removed.getWorld())
            .position(new Vector3i(this.affectedPosition.getX(), this.affectedPosition.getY(), this.affectedPosition.getZ()))
            .blockState(this.originalState)
            .build()
            ;
    }

    @Override
    protected SpongeBlockSnapshot getOriginalSnapshot() {
        return this.tileSnapshot;
    }
}
