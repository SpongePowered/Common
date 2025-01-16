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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.GameTransaction;
import org.spongepowered.common.event.tracking.context.transaction.type.TransactionTypes;
import org.spongepowered.common.util.PrettyPrinter;
import org.spongepowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class InteractItemWithBlockTransaction extends CompositeTransaction<InteractBlockEvent.Secondary.Post> {

    private final Vector3d hitVec;
    private final BlockSnapshot snapshot;
    private final Direction direction;
    private final ServerPlayer player;
    private final Tristate originalBlockResult, blockResult, originalItemResult, itemResult;

    public InteractItemWithBlockTransaction(
        final ServerPlayer playerIn, final Vector3d hitVec, final BlockSnapshot snapshot,
        final Direction direction,
        final Tristate originalBlockResult, final Tristate useBlockResult,
        final Tristate originalUseItemResult, final Tristate useItemResult) {
        super(TransactionTypes.INTERACT_BLOCK_SECONDARY.get());
        this.player = playerIn;
        this.hitVec = hitVec;
        this.snapshot = snapshot;
        this.direction = direction;
        this.originalBlockResult = originalBlockResult;
        this.blockResult = useBlockResult;
        this.originalItemResult = originalUseItemResult;
        this.itemResult = useItemResult;
    }


    @Override
    public Optional<BiConsumer<PhaseContext<@NonNull ?>, CauseStackManager.StackFrame>> getFrameMutator(
        @Nullable GameTransaction<@NonNull ?> parent
    ) {
        return Optional.of((context, frame) -> {
            frame.pushCause(this.player);
        });
    }

    @Override
    public void addToPrinter(PrettyPrinter printer) {

    }

    @Override
    public Optional<InteractBlockEvent.Secondary.Post> generateEvent(
        final PhaseContext<@NonNull ?> context,
        final @Nullable GameTransaction<@NonNull ?> parent,
        final ImmutableList<GameTransaction<InteractBlockEvent.Secondary.Post>> gameTransactions,
        final Cause currentCause
    ) {
        final var root = SpongeEventFactory.createInteractBlockEventSecondary(currentCause,
            this.originalBlockResult, this.blockResult,
            this.originalItemResult, this.itemResult,
            this.snapshot, this.hitVec,
            this.direction
        );
        final List<Event> list = new ArrayList<>();
        final InteractBlockEvent.Secondary.Post composite = SpongeEventFactory.createInteractBlockEventSecondaryPost(currentCause, root, list);
        return Optional.of(composite);
    }

    @Override
    public void restore(PhaseContext<@NonNull ?> context, InteractBlockEvent.Secondary.Post event) {

    }

}
