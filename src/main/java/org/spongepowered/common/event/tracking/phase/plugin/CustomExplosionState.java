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
package org.spongepowered.common.event.tracking.phase.plugin;

import static org.spongepowered.common.event.tracking.TrackingUtil.iterateChangeBlockEvents;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.CauseStackManager.StackFrame;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.IEntitySpecificItemDropsState;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.interfaces.world.IMixinLocation;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.BlockChange;

import java.util.ArrayList;
import java.util.List;

final class CustomExplosionState extends PluginPhaseState<ExplosionContext> implements IEntitySpecificItemDropsState<ExplosionContext> {
    @Override
    public ExplosionContext createPhaseContext() {
        return new ExplosionContext()
            .addEntityCaptures()
            .addEntityDropCaptures()
            .addBlockCaptures();
    }

    @Override
    public void unwind(ExplosionContext context) {

        final Explosion explosion = context.getExplosion();
        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.TNT_IGNITE);
            frame.pushCause(explosion);
            if (context.getNotifier().isPresent()) {
                frame.addContext(EventContextKeys.NOTIFIER, context.getNotifier().get());
            }
            if (context.getOwner().isPresent()) {
                frame.addContext(EventContextKeys.OWNER, context.getOwner().get());
            }
            context.getCapturedBlockSupplier()
                .acceptAndClearIfNotEmpty(
                    blocks -> processBlockCaptures(blocks, explosion, Sponge.getCauseStackManager().getCurrentCause(), context));
            context.getCapturedEntitySupplier()
                .acceptAndClearIfNotEmpty(entities -> {
                    SpongeCommonEventFactory.callSpawnEntity(entities, context);

                });
        }
    }

    @Override
    public boolean canSwitchTo(IPhaseState<?> state) {
        return true;
    }

    @Override
    public boolean tracksBlockSpecificDrops() {
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void processBlockCaptures(List<BlockSnapshot> snapshots, Explosion explosion, Cause cause, PhaseContext<?> context) {
        if (snapshots.isEmpty()) {
            return;
        }
        ImmutableList<Transaction<BlockSnapshot>>[] transactionArrays = new ImmutableList[TrackingUtil.EVENT_COUNT];
        ImmutableList.Builder<Transaction<BlockSnapshot>>[] transactionBuilders = new ImmutableList.Builder[TrackingUtil.EVENT_COUNT];
        for (int i = 0; i < TrackingUtil.EVENT_COUNT; i++) {
            transactionBuilders[i] = new ImmutableList.Builder<>();
        }
        final List<ChangeBlockEvent> blockEvents = new ArrayList<>();

        for (BlockSnapshot snapshot : snapshots) {
            // This processes each snapshot to assign them to the correct event in the next area, with the
            // correct builder array entry.
            TrackingUtil.TRANSACTION_PROCESSOR.apply(transactionBuilders).accept(TrackingUtil.TRANSACTION_CREATION.apply(snapshot));
        }
        for (int i = 0; i < TrackingUtil.EVENT_COUNT; i++) {
            // Build each event array
            transactionArrays[i] = transactionBuilders[i].build();
        }

        // Clear captured snapshots after processing them
        context.getCapturedBlocksOrEmptyList().clear();

        final ChangeBlockEvent[] mainEvents = new ChangeBlockEvent[BlockChange.values().length];
        // This likely needs to delegate to the phase in the event we don't use the source object as the main object causing the block changes
        // case in point for WorldTick event listeners since the players are captured non-deterministically
        try (StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            if(context.getNotifier().isPresent()) {
                frame.addContext(EventContextKeys.NOTIFIER, context.getNotifier().get());
            }
            if(context.getOwner().isPresent()) {
                frame.addContext(EventContextKeys.OWNER, context.getOwner().get());
            }
            try {
                this.associateAdditionalCauses(this, context, frame);
            } catch (Exception e) {
                // TODO - this should be a thing to associate additional objects in the cause, or context, but for now it's just a simple
                // try catch to avoid bombing on performing block changes.
            }
            // Creates the block events accordingly to the transaction arrays
            iterateChangeBlockEvents(transactionArrays, blockEvents, mainEvents); // Needs to throw events
            // We create the post event and of course post it in the method, regardless whether any transactions are invalidated or not
    
            // Copied from TrackingUtil#throwMultiEventsAndCreatePost
            for (BlockChange blockChange : BlockChange.values()) {
                final ChangeBlockEvent mainEvent = mainEvents[blockChange.ordinal()];
                if (mainEvent != null) {
                    frame.pushCause(mainEvent);
                }
            }
            final ImmutableList<Transaction<BlockSnapshot>> transactions = transactionArrays[TrackingUtil.MULTI_CHANGE_INDEX];
    
            final ExplosionEvent.Post postEvent = SpongeEventFactory.createExplosionEventPost(cause, explosion, transactions);
            if (postEvent == null) { // Means that we have had no actual block changes apparently?
                return;
            }
            SpongeImpl.postEvent(postEvent);
    
            final List<Transaction<BlockSnapshot>> invalid = new ArrayList<>();
    
            boolean noCancelledTransactions = true;
    
            // Iterate through the block events to mark any transactions as invalid to accumilate after (since the post event contains all
            // transactions of the preceeding block events)
            for (ChangeBlockEvent blockEvent : blockEvents) { // Need to only check if the event is cancelled, If it is, restore
                if (blockEvent.isCancelled()) {
                    noCancelledTransactions = false;
                    // Don't restore the transactions just yet, since we're just marking them as invalid for now
                    for (Transaction<BlockSnapshot> transaction : Lists.reverse(blockEvent.getTransactions())) {
                        transaction.setValid(false);
                    }
                }
            }
    
            // Finally check the post event
            if (postEvent.isCancelled()) {
                // Of course, if post is cancelled, just mark all transactions as invalid.
                noCancelledTransactions = false;
                for (Transaction<BlockSnapshot> transaction : postEvent.getTransactions()) {
                    transaction.setValid(false);
                }
            }
    
            // Now we can gather the invalid transactions that either were marked as invalid from an event listener - OR - cancelled.
            // Because after, we will restore all the invalid transactions in reverse order.
            for (Transaction<BlockSnapshot> transaction : postEvent.getTransactions()) {
                if (!transaction.isValid()) {
                    invalid.add(transaction);
                    // Cancel any block drops performed, avoids any item drops, regardless
                    final Location<World> location = transaction.getOriginal().getLocation().orElse(null);
                    if (location != null) {
                        final BlockPos pos = VecHelper.toBlockPos(location);
                        context.getBlockItemDropSupplier().removeAllIfNotEmpty(pos);
                    }
                }
            }
    
            if (!invalid.isEmpty()) {
                // We need to set this value and return it to signify that some transactions were cancelled
                noCancelledTransactions = false;
                // NOW we restore the invalid transactions (remember invalid transactions are from either plugins marking them as invalid
                // or the events were cancelled), again in reverse order of which they were received.
                for (Transaction<BlockSnapshot> transaction : Lists.reverse(invalid)) {
                    transaction.getOriginal().restore(true, BlockChangeFlags.NONE);
                    // Cancel any block drops or harvests for the block change.
                    // This prevents unnecessary spawns.
                    final Location<World> location = transaction.getOriginal().getLocation().orElse(null);
                    if (location != null) {
                        final BlockPos pos = VecHelper.toBlockPos(location);
                        context.getBlockDropSupplier().removeAllIfNotEmpty(pos);
                    }
                }
            }
            TrackingUtil.performBlockAdditions(postEvent.getTransactions(), this, context, noCancelledTransactions);
        }
    }

    @Override
    public boolean shouldCaptureBlockChangeOrSkip(ExplosionContext phaseContext, BlockPos pos) {
        final Vector3i blockPos = VecHelper.toVector3i(pos);
        for (final BlockSnapshot capturedSnapshot : phaseContext.getCapturedBlocks()) {
            if (capturedSnapshot.getPosition().equals(blockPos)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean requiresBlockCapturing() {
        return false;
    }
}
