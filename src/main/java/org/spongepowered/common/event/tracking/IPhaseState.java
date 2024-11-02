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
package org.spongepowered.common.event.tracking;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.ticks.ScheduledTick;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.transaction.BlockTransactionReceipt;
import org.spongepowered.api.block.transaction.Operation;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.entity.SpawnType;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.item.inventory.container.ClickContainerEvent;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.World;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.bridge.server.TickTaskBridge;
import org.spongepowered.common.bridge.server.level.ServerLevelBridge;
import org.spongepowered.common.bridge.world.TrackedWorldBridge;
import org.spongepowered.common.bridge.world.level.TrackableBlockEventDataBridge;
import org.spongepowered.common.bridge.world.level.chunk.LevelChunkBridge;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.tracking.context.transaction.GameTransaction;
import org.spongepowered.common.event.tracking.context.transaction.block.ChangeBlock;
import org.spongepowered.common.event.tracking.context.transaction.world.SpawnEntityTransaction;
import org.spongepowered.common.event.tracking.phase.general.ExplosionContext;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhase;
import org.spongepowered.common.event.tracking.phase.tick.LocationBasedTickContext;
import org.spongepowered.common.event.tracking.phase.tick.TickPhase;
import org.spongepowered.common.world.BlockChange;

import java.util.ArrayDeque;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A literal phase state of which the {@link World} is currently running
 * in. As these should be enums, there's no data that should be stored on
 * this state.
 */
@DefaultQualifier(NonNull.class)
public interface IPhaseState<C extends PhaseContext<C>> {

    BiConsumer<CauseStackManager.StackFrame, ? extends PhaseContext<@NonNull ?>> DEFAULT_OWNER_NOTIFIER = (frame, ctx) -> {
        if (ctx.usedFrame == null) {
            ctx.usedFrame = new ArrayDeque<>();
        }
        ctx.usedFrame.push(frame); // WE NEED TO STORE THIS SO WE CAN PROPERLY POP THE FRAME
        if (ctx.creator != null) {
            frame.addContext(EventContextKeys.CREATOR, ctx.creator);
        }
        if (ctx.notifier != null) {
            frame.addContext(EventContextKeys.NOTIFIER, ctx.notifier);
        }
    };

    /**
     * Creates a minimalized {@link PhaseContext} for this specific state. In some cases,
     * the context will be pre-populated with captures and essentially 'set up' with
     * fields for the state's expected usage.
     *
     * @return The new phase context
     * @param server The PhaseTracker instance for thread handling
     */
    C createPhaseContext(PhaseTracker server);

    /**
     * Gets the frame modifier for default frame modifications, like pushing
     * the source of the phase, owner, notifier, etc. of the context. Used specifically
     * for lazy evaluating stack frames to push causes and contexts guaranteed at any point
     * in this state.
     * @return
     */
    @SuppressWarnings("unchecked")
    default BiConsumer<CauseStackManager.StackFrame, C> getFrameModifier() {
        return (BiConsumer<CauseStackManager.StackFrame, C>) IPhaseState.DEFAULT_OWNER_NOTIFIER; // Default does nothing
    }

    /**
     * Gets whether this state is considered an interaction, specifically to determine
     * whether a pre-block event check can be performed prior to actual block modifications
     * are done and potentially "captured" as a result. This is specific to allow mod compatibility
     * with common protection plugins having the ability to determine whether a proposed block
     * change is allowed or not.
     *
     * @return Whether this state is considered a player caused interaction or not
     */
    default boolean isInteraction() {
        return false;
    }

    /**
     * The exit point of any phase. Every phase should have an unwinding
     * process where if anything is captured, events should be thrown and
     * processed accordingly. The outcome of each phase is dependent on
     * the {@link IPhaseState} provided, as different states require different
     * handling.
     *
     * <p>Examples of this include: {@link PacketPhase}, {@link TickPhase}, etc.
     * </p>
     *
     * <p>Note that the {@link PhaseTracker} is only provided for easy access
     * to the {@link ServerLevel}, {@link ServerLevelBridge}, and
     * {@link World} instances.</p>
     *
     * @param phaseContext The context of the current state being unwound
     */
    void unwind(C phaseContext);

    /**
     * Performs any necessary custom logic after the provided {@link BlockSnapshot}
     * {@link Transaction} has taken place.
     * @param context The context for information
     * @param blockChange The block change performed
     * @param receipt The transaction of the old and new snapshots
     */
    default void postBlockTransactionApplication(
        final C context, final BlockChange blockChange,
        final BlockTransactionReceipt receipt
    ) { }

    /**
     * Specifically gets whether this state ignores any attempts at storing
     * or retrieving an owner/notifier from a particular {@link BlockPos}
     * within a {@link net.minecraft.world.level.Level} or {@link LevelChunk}.
     *
     * <p>Specifically used in
     * {@code ChunkMixin_OwnershipTracked#bridge$addTrackedBlockPosition(Block, BlockPos, User, PlayerTracker.Type)}
     * to make sure that the current state would be providing said information,
     * instead of spending the processing to query for it.</p>
     *
     * @return Simple true false to determine whether this phase is providing owner/notifier information
     */
    default boolean tracksCreatorsAndNotifiers() {
        return true;
    }

    /**
     * Gets whether this state will allow entities to spawn, in general, not whether they're captured,
     * directly spawned, or throw an event, but whether the entity will be *able* to spawn. In general
     * this is returned {@code false} for block restoration, since restoring blocks is a restorative
     * process, we should not be respawning any entities as a side effect.
     *
     * @return True if entities are allowed to spawn
     */
    default boolean doesAllowEntitySpawns() {
        return true;
    }

    default boolean doesBlockEventTracking(final C context) {
        return true;
    }

    /**
     * Gets whether this state fires {@link org.spongepowered.api.event.entity.CollideEntityEvent}s.
     * This is used for firing the events and for related optimizations.
     *
     * @return Whether this state should fire entity collision events
     */
    default boolean allowsEntityCollisionEvents() {
        return false;
    }

    /**
     * Gets whether this state will ignore {@link net.minecraft.world.level.Level#blockEvent(BlockPos, Block, int, int)}
     * additions when potentially performing notification updates etc. Usually true for world generation.
     *
     * @return False if block events are to be processed in some way by the state
     */
    default boolean ignoresBlockEvent() {
        return false;
    }

    /**
     * Gets whether this state requires a post state entry for any captured objects. Usually
     * does not, get used uless this is already a post state, or an invalid packet state.
     * TODO - Investigate whether world generation states could use this.
     *
     * @return True if this state is expecting to be unwound with an unwinding state to cpature additional changes
     */
    default boolean requiresPost() {
        return true;
    }

    /**
     * Associates any notifier/owner information from expected states that will assuredly provide
     * said information. In some states, like world gen, there is no information to provide.
     *
     * @param unwindingContext The unwinding context providing context information
     * @param sourcePos The source position performing the notification
     * @param block The block type providing the notification
     * @param notifyPos The notified position
     * @param minecraftWorld The world
     * @param notifier The tracker type (owner or notifier)
     */
    default void associateNeighborStateNotifier(final C unwindingContext, final @Nullable BlockPos sourcePos, final Block block, final BlockPos notifyPos,
        final ServerLevel minecraftWorld, final PlayerTracker.Type notifier) {

    }

    /**
     * Provides additional information from this state in the event an explosion is going to be
     * performed, providing information like entity owners, notifiers, or potentially even sources
     * from blocks.
     *
     * @param explosionContext The explosion context to populate
     * @param currentPhaseData The current context to provide information
     */
    default void appendContextPreExplosion(final ExplosionContext explosionContext, final C currentPhaseData) {

    }

    /**
     * Appends additional information from the block's position in the world to provide notifier/owner
     * information. Overridden in world generation states to reduce chunk lookup costs and since
     * world generation does not track owners/notifiers.
     *  @param world The world reference
     * @param pos The position being updated
     * @param context The context
     * @param phaseContext the block tick context being entered
     */
    default void appendNotifierPreBlockTick(final ServerLevel world, final BlockPos pos, final C context, final LocationBasedTickContext<@NonNull ?> phaseContext) {
        final LevelChunk chunk = world.getChunkAt(pos);
        final LevelChunkBridge mixinChunk = (LevelChunkBridge) chunk;
        if (chunk != null && !chunk.isEmpty()) {
            mixinChunk.bridge$getBlockCreatorUUID(pos).ifPresent(phaseContext::creator);
            mixinChunk.bridge$getBlockNotifierUUID(pos).ifPresent(phaseContext::notifier);
        }
    }

    /**
     * Appends any additional information to the block tick context from this context.
     */
    default void appendNotifierToBlockEvent(final C context,
        final TrackedWorldBridge mixinWorldServer, final BlockPos pos, final TrackableBlockEventDataBridge blockEvent
    ) {

    }

    /**
     * Attempts to capture the player using the item stack in this state. Some states do not care for
     * this information. Usually packets do care and some scheduled tasks.
     *  @param playerIn
     * @param context
     */
    default void capturePlayerUsingStackToBreakBlock(
        final @Nullable ServerPlayer playerIn, final C context
    ) {

    }

    /**
     * Used in the {@link org.spongepowered.api.event.EventManager} and mod event manager equivalent for
     * world generation tasks to avoid event listener state entrance due to listeners
     * during world generation performing various operations that should not be tracked.
     *
     * <p>Refer to spongeforge issue:
     * https://github.com/SpongePowered/SpongeForge/issues/2407#issuecomment-415850841
     * for more information and context of why this is needed.
     * </p>
     *
     * @return True if an {@link org.spongepowered.common.event.tracking.phase.plugin.PluginPhase.Listener#GENERAL_LISTENER}
     *     is to be entered during this state
     */
    default boolean allowsEventListener() {
        return true;
    }

    /**
     * Specifically captures a block change by {@link org.spongepowered.common.event.tracking.context.transaction.TransactionalCaptureSupplier#logBlockChange(SpongeBlockSnapshot, BlockState, BlockChangeFlag)}
     * such that the change of a {@link BlockState} will be appropriately logged, along with any changes of tile entities being removed
     * or added, likewise, this will avoid duplicating transactions later after the fact, in the event that multiple changes are taking
     * place, including but not withstanding, tile entity replacements after the fact.
     * @return
     */
    default ChangeBlock createTransaction(final C phaseContext,
        final SpongeBlockSnapshot originalBlockSnapshot, final BlockState newState, final BlockChangeFlag flags) {
        final ChangeBlock changeBlock = phaseContext.getTransactor()
            .logBlockChange(originalBlockSnapshot, newState, flags);

        return changeBlock;
    }

    default BlockChange associateBlockChangeWithSnapshot(
        final C phaseContext, final BlockState newState,
        final BlockState currentState
    ) {
        final Block newBlock = newState.getBlock();
        final Block currentBlock = currentState.getBlock();
        if (newBlock == Blocks.AIR) {
            return BlockChange.BREAK;
        } else if (newBlock != currentBlock) {
            if (newState == currentState.getFluidState().createLegacyBlock()) {
                return BlockChange.BREAK;
            }
            return BlockChange.PLACE;
        }
        return BlockChange.MODIFY;
    }

    /**
     * Gets whether this {@link IPhaseState} entry with a provided {@link PhaseContext}
     * will be allowed to register it's {@link #getFrameModifier()} to push along the
     * {@link CauseStackManager}. In certain cases, there are states that can have
     * excessive modifiers being pushed and popped with and without causes that may cause
     * performance degredation due to the excessive amounts of how many recyclings occur
     * with {@link CauseStackManager#currentCause()} lacking a cached context
     * and therefor needing to re-create the context each and every time.
     *
     * @param phaseContext The appropriate phase context
     * @return True if the modifiers should be pushed to the manager
     */
    default boolean shouldProvideModifiers(final C phaseContext) {
        return true;
    }
    default boolean isRestoring() {
        return false;
    }

    default Supplier<SpawnType> getSpawnTypeForTransaction(final C context, final Entity entityToSpawn) {
        if (entityToSpawn instanceof ItemEntity) {
            return SpawnTypes.DROPPED_ITEM;
        }
        return SpawnTypes.PASSIVE;
    }

    default SpawnEntityEvent createSpawnEvent(final C context,
        final @Nullable GameTransaction<@NonNull ?> parent,
        final ImmutableList<Tuple<Entity, SpawnEntityTransaction.DummySnapshot>> collect,
        final Cause currentCause
    ) {
        return SpongeEventFactory.createSpawnEntityEvent(currentCause,
            collect.stream()
                .map(t -> (org.spongepowered.api.entity.Entity) t.first())
                .collect(Collectors.toList())
        );
    }

    default void populateLootContext(final C phaseContext, final LootParams.Builder lootBuilder) {

    }

    default Operation getBlockOperation(
        final C phaseContext, final SpongeBlockSnapshot original, final SpongeBlockSnapshot result
    ) {
        return this.associateBlockChangeWithSnapshot(phaseContext, result.nativeState(), original.nativeState()).toOperation();
    }

    default void foldContextForThread(final C context, final TickTaskBridge returnValue) {
    }

    default void associateScheduledTickUpdate(final C asContext, final ServerLevel level,
        final ScheduledTick<?> entry
    ) {
        asContext.getTransactor().logScheduledUpdate(level, entry);
    }

    default boolean isApplyingStreams() {
        return false;
    }

    default Supplier<ResourceKey> attemptWorldKey(final C context) {
        return () -> {
            throw new IllegalStateException("Unable to provide a ServerLevel");
        };
    }

    default @Nullable ClickContainerEvent createContainerEvent(
        final C context, final Cause cause,
        final ServerPlayer serverPlayer, final Container openContainer,
        final Transaction<ItemStackSnapshot> transaction,
        final List<SlotTransaction> slotTransactions, final List<org.spongepowered.api.entity.Entity> capturedEntities,
        final int usedButton, final @Nullable Slot slot
    ) {
        return null;
    }

    default boolean doesContainerCaptureEntitySpawn(final C context, final Entity entityIn) {
        return false;
    }

    default boolean captureModifiedContainer(final AbstractContainerMenu container) {
        return false;
    }
}
