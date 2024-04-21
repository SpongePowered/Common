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
package org.spongepowered.common.event.tracking.context.transaction.inventory;

import com.google.common.collect.ImmutableList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Direction;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.GameTransaction;
import org.spongepowered.common.event.tracking.context.transaction.type.TransactionTypes;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.PrettyPrinter;
import org.spongepowered.math.vector.Vector3d;

import java.util.Optional;
import java.util.function.BiConsumer;

public class InteractItemTransaction extends GameTransaction<InteractBlockEvent.Secondary.Composite> {


    private final Vector3d hitVec;
    private final BlockSnapshot snapshot;
    private final Direction direction;
    private final InteractionHand hand;
    private final ItemStackSnapshot stack;

    public InteractItemTransaction(ServerPlayer playerIn, ItemStack stackIn, Vector3d hitVec, BlockSnapshot snapshot, Direction direction, InteractionHand handIn) {
        super(TransactionTypes.INTERACT_BLOCK_SECONDARY.get());
        this.stack = ItemStackUtil.snapshotOf(stackIn);
        this.hitVec = hitVec;
        this.snapshot = snapshot;
        this.direction = direction;
        this.hand = handIn;
    }


    @Override
    public Optional<BiConsumer<PhaseContext<@NonNull ?>, CauseStackManager.StackFrame>> getFrameMutator(
        @Nullable GameTransaction<@NonNull ?> parent
    ) {
        return Optional.empty();
    }

    @Override
    public void addToPrinter(PrettyPrinter printer) {

    }

    @Override
    public Optional<InteractBlockEvent.Secondary.Composite> generateEvent(
        final PhaseContext<@NonNull ?> context,
        final @Nullable GameTransaction<@NonNull ?> parent,
        final ImmutableList<GameTransaction<InteractBlockEvent.Secondary.Composite>> gameTransactions,
        final Cause currentCause
    ) {
        return Optional.empty();
    }

    @Override
    public void restore(PhaseContext<@NonNull ?> context, InteractBlockEvent.Secondary.Composite event) {

    }

    @Override
    public boolean markCancelledTransactions(
        final InteractBlockEvent.Secondary.Composite event,
        final ImmutableList<? extends GameTransaction<InteractBlockEvent.Secondary.Composite>> gameTransactions) {
        return false;
    }
}
