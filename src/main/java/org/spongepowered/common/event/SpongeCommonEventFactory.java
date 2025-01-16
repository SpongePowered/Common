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
package org.spongepowered.common.event;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapIndex;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.entity.Jukebox;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.type.InstrumentType;
import org.spongepowered.api.data.type.NotePitch;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.effect.sound.music.MusicDisc;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.action.CreateMapEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.CollideBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.api.event.entity.CollideEntityEvent;
import org.spongepowered.api.event.entity.ConstructEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.RotateEntityEvent;
import org.spongepowered.api.event.entity.ai.SetAITargetEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.event.sound.PlaySoundEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.map.MapInfo;
import org.spongepowered.api.projectile.source.ProjectileSource;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.DefaultWorldKeys;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.CreatorTrackedBridge;
import org.spongepowered.common.bridge.map.MapIdTrackerBridge;
import org.spongepowered.common.bridge.server.level.ServerLevelBridge;
import org.spongepowered.common.bridge.world.TrackedWorldBridge;
import org.spongepowered.common.bridge.world.entity.EntityBridge;
import org.spongepowered.common.bridge.world.entity.PlatformEntityBridge;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.bridge.world.level.chunk.LevelChunkBridge;
import org.spongepowered.common.bridge.world.storage.MapItemSavedDataBridge;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.entity.projectile.UnknownProjectileSource;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.map.SpongeMapStorage;
import org.spongepowered.common.registry.provider.DirectionFacingProvider;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.server.SpongeLocatableBlockBuilder;
import org.spongepowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SpongeCommonEventFactory {

    private static final double MOVEMENT_GRID_POINTS_PER_BLOCK = 8.0d;
    private static final double ROTATION_GRID_POINTS_PER_UNIT = 20.0f;

    @SuppressWarnings("unchecked")
    public static <T extends net.minecraft.world.entity.Entity> CollideEntityEvent callCollideEntityEvent(
        final net.minecraft.world.entity.@Nullable Entity sourceEntity,
        final List<T> entities
    ) {

        final PhaseTracker phaseTracker = PhaseTracker.getInstance();
        final PhaseContext<@NonNull ?> currentContext = phaseTracker.getPhaseContext();
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            if (sourceEntity != null) {
                // We only want to push the source entity if it's not the current entity being ticked or "sourced". They will be already pushed.
                if (currentContext.getSource() != sourceEntity) {
                    frame.pushCause(sourceEntity);
                }
            }
            currentContext.addCreatorAndNotifierToCauseStack(frame);

            final List<Entity> spEntities = (List<Entity>) (List<?>) entities;
            final CollideEntityEvent event =
                    SpongeEventFactory.createCollideEntityEvent(PhaseTracker.getCauseStackManager().currentCause(), spEntities);
            SpongeCommon.post(event);
            return event;
        }
    }

    /**
     * This simulates the blocks a piston moves and calls the event for saner
     * debugging.
     *
     * @return if the event was cancelled
     */
    public static boolean handlePistonEvent(
        final TrackedWorldBridge world, final BlockPos pos, final net.minecraft.world.level.block.state.BlockState blockstate, final int eventId
    ) {
        final boolean extending = (eventId == 0);
        final net.minecraft.core.Direction direction = blockstate.getValue(DirectionalBlock.FACING);
        final LocatableBlock locatable = new SpongeLocatableBlockBuilder().world((ServerWorld) world).state((BlockState) blockstate).position(pos.getX(), pos.getY(), pos.getZ()).build();

        // Sets toss out duplicate values (even though there shouldn't be any)
        final HashSet<ServerLocation> locations = new HashSet<>();
        locations.add(ServerLocation.of((ServerWorld) world, pos.getX(), pos.getY(), pos.getZ()));

        final PistonStructureResolver movedBlocks = new PistonStructureResolver((ServerLevel) world, pos, direction, extending);
        movedBlocks.resolve(); // calculates blocks to be moved

        Stream.concat(movedBlocks.getToPush().stream(), movedBlocks.getToDestroy().stream())
            .map(block -> ServerLocation.of((ServerWorld) world, block.getX(), block.getY(), block.getZ()))
            .collect(Collectors.toCollection(() -> locations)); // SUPER
        // efficient
        // code!

        // If the piston is extending and there are no blocks to destroy, add the offset location for protection purposes
        if (extending && movedBlocks.getToDestroy().isEmpty()) {
            final List<BlockPos> movedPositions = movedBlocks.getToPush();
            final BlockPos offsetPos;
            // If there are no blocks to move, add the offset of piston
            if (movedPositions.isEmpty()) {
                offsetPos = pos.relative(direction);
            } else {
                // Add the offset of last block set to move
                offsetPos = movedPositions.get(movedPositions.size() - 1).relative(direction);
            }
            locations.add(ServerLocation.of((ServerWorld) world, offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()));
        }

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getInstance().pushCauseFrame()) {
            if (extending) {
                frame.addContext(EventContextKeys.PISTON_EXTEND, (ServerWorld) world);
            } else {
                frame.addContext(EventContextKeys.PISTON_RETRACT, (ServerWorld) world);
            }
            return SpongeCommonEventFactory.callChangeBlockEventPre((ServerLevelBridge) world, ImmutableList.copyOf(locations), locatable)
                .isCancelled();
        }
    }

    public static ChangeBlockEvent.Pre callChangeBlockEventPre(final ServerLevelBridge worldIn, final BlockPos pos) {

        return SpongeCommonEventFactory.callChangeBlockEventPre(worldIn, ImmutableList.of(
            ServerLocation.of((ServerWorld) worldIn, pos.getX(), pos.getY(), pos.getZ())), null);
    }

    public static ChangeBlockEvent.Pre callChangeBlockEventPre(final ServerLevelBridge worldIn, final BlockPos pos, final Object source) {
        return SpongeCommonEventFactory.callChangeBlockEventPre(worldIn, ImmutableList.of(
            ServerLocation.of((ServerWorld) worldIn, pos.getX(), pos.getY(), pos.getZ())), source);
    }

    /**
     * Processes pre block event data then fires event.
     *
     * @param worldIn The world
     * @param locations The locations affected
     * @param source The source of event
     * @return The event
     */
    private static ChangeBlockEvent.Pre callChangeBlockEventPre(final ServerLevelBridge worldIn, final ImmutableList<ServerLocation> locations, @Nullable Object source) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            final PhaseContext<@NonNull ?> phaseContext = PhaseTracker.getInstance().getPhaseContext();
            if (source == null) {
                source = phaseContext.getSource() == null ? worldIn : phaseContext.getSource();
            }

            // TODO - All of this bit should be nuked since PhaseContext has lazy initializing frames.
            net.minecraft.world.entity.player.Player player = null;
            frame.pushCause(source);
            if (source instanceof Player) {
                player = (net.minecraft.world.entity.player.Player) source;
                if (((PlatformEntityBridge) player).bridge$isFakePlayer()) {
                    frame.addContext(EventContextKeys.FAKE_PLAYER, (Player) player);
                }
            }
            if (phaseContext.getCreator().isPresent()) {
                phaseContext.getCreator().ifPresent(creator -> frame.addContext(EventContextKeys.CREATOR, creator));
            } else if (player instanceof ServerPlayer) {
                frame.addContext(EventContextKeys.CREATOR, ((ServerPlayer) player).uniqueId());
            }

            phaseContext.applyNotifierIfAvailable(notifier -> frame.addContext(EventContextKeys.NOTIFIER, notifier));

            final ChangeBlockEvent.Pre event =
                SpongeEventFactory.createChangeBlockEventPre(frame.currentCause(), locations,
                    (ServerWorld) worldIn
                );
            SpongeCommon.post(event);
            return event;
        }
    }

    public static InteractEntityEvent.Primary callInteractEntityEventPrimary(final net.minecraft.server.level.ServerPlayer player, final ItemStack stack, final net.minecraft.world.entity.Entity entity, final InteractionHand hand) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            SpongeCommonEventFactory.applyCommonInteractContext(player, stack, hand, null, entity, frame);
            final InteractEntityEvent.Primary event = SpongeEventFactory.createInteractEntityEventPrimary(frame.currentCause(), (Entity) entity);
            if (entity instanceof Player && !((ServerWorld) player.level()).properties().pvp()) {
                event.setCancelled(true); // if PvP is disabled for world, cancel
            }
            SpongeCommon.post(event);
            return event;
        }
    }

    public static InteractEntityEvent.Secondary callInteractEntityEventSecondary(final net.minecraft.server.level.ServerPlayer player, final ItemStack stack, final net.minecraft.world.entity.Entity entity,
            final InteractionHand hand, final @Nullable Vector3d hitVec) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            SpongeCommonEventFactory.applyCommonInteractContext(player, stack, hand, null, entity, frame);
            final InteractEntityEvent.Secondary event = hitVec == null ?
                    SpongeEventFactory.createInteractEntityEventSecondaryOn(frame.currentCause(), (Entity) entity) :
                    SpongeEventFactory.createInteractEntityEventSecondaryAt(frame.currentCause(), (Entity) entity, hitVec);
            SpongeCommon.post(event);
            return event;
        }
    }

    public static InteractItemEvent.Primary callInteractItemEventPrimary(final net.minecraft.world.entity.player.Player player, final ItemStack stack, final InteractionHand hand) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            SpongeCommonEventFactory.applyCommonInteractContext(player, stack, hand, null, null, frame);
            final InteractItemEvent.Primary event = SpongeEventFactory.createInteractItemEventPrimary(frame.currentCause(), ItemStackUtil.snapshotOf(stack));
            SpongeCommon.post(event);
            return event;
        }
    }

    public static InteractItemEvent.Secondary.Pre callInteractItemEventSecondary(final net.minecraft.world.entity.player.Player player, final ItemStack stack, final InteractionHand hand) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            SpongeCommonEventFactory.applyCommonInteractContext(player, stack, hand, null, null, frame);
            final var event = SpongeEventFactory.createInteractItemEventSecondaryPre(frame.currentCause(), ItemStackUtil.snapshotOf(stack));
            SpongeCommon.post(event);
            return event;
        }

    }

    public static InteractBlockEvent.Primary callInteractBlockEventPrimary(final ServerboundPlayerActionPacket.Action action,
            final net.minecraft.world.entity.player.Player player, final ItemStack heldItem, final BlockSnapshot blockSnapshot, final InteractionHand hand,
            final net.minecraft.core.@Nullable Direction side) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            SpongeCommonEventFactory.applyCommonInteractContext(player, heldItem, hand, blockSnapshot, null, frame);
            final Direction direction;
            if (side != null) {
                direction = DirectionFacingProvider.INSTANCE.getKey(side).get();
            } else {
                direction = Direction.NONE;
            }

            final InteractBlockEvent.Primary event;
            switch (action) {
                case START_DESTROY_BLOCK:
                    event = SpongeEventFactory.createInteractBlockEventPrimaryStart(frame.currentCause(), blockSnapshot, direction);
                    break;
                case ABORT_DESTROY_BLOCK:
                    event = SpongeEventFactory.createInteractBlockEventPrimaryStop(frame.currentCause(), blockSnapshot, direction);
                    break;
                case STOP_DESTROY_BLOCK:
                    event = SpongeEventFactory.createInteractBlockEventPrimaryFinish(frame.currentCause(), blockSnapshot, direction);
                    break;
                default:
                    throw new IllegalStateException("unreachable code");
            }

            SpongeCommon.post(event);
            return event;
        }
    }

    public static InteractBlockEvent.Secondary.Pre callInteractBlockEventSecondary(final net.minecraft.world.entity.player.Player player, final ItemStack heldItem, final Vector3d hitVec, final BlockSnapshot targetBlock, final Direction targetSide, final InteractionHand hand) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            SpongeCommonEventFactory.applyCommonInteractContext(player, heldItem, hand, targetBlock, null, frame);
            final InteractBlockEvent.Secondary.Pre event = SpongeEventFactory.createInteractBlockEventSecondaryPre(frame.currentCause(),
                    Tristate.UNDEFINED, Tristate.UNDEFINED, Tristate.UNDEFINED, Tristate.UNDEFINED, targetBlock, hitVec,
                    targetSide);
            SpongeCommon.post(event);
            return event;
        }
    }

    public static void applyCommonInteractContext(
        final net.minecraft.world.entity.player.Player player, final ItemStack stack, final InteractionHand hand, @Nullable
    final BlockSnapshot targetBlock,
            final net.minecraft.world.entity.@Nullable Entity entity, final CauseStackManager.StackFrame frame) {
        if (((PlatformEntityBridge) player).bridge$isFakePlayer()) {
            frame.addContext(EventContextKeys.FAKE_PLAYER, (Player) player);
        } else {
            frame.pushCause(player);
            frame.addContext(EventContextKeys.CREATOR, player.getUUID());
            frame.addContext(EventContextKeys.NOTIFIER, player.getUUID());
        }

        if (!stack.isEmpty()) {
            frame.addContext(EventContextKeys.USED_ITEM, ItemStackUtil.snapshotOf(stack));
        }
        frame.addContext(EventContextKeys.USED_HAND, (HandType) (Object) hand);
        if (targetBlock != null) {
            frame.addContext(EventContextKeys.BLOCK_HIT, targetBlock);
        }
        if (entity != null) {
            frame.addContext(EventContextKeys.ENTITY_HIT, (Entity) entity);
        }
    }

    /**
     * Performs the logic necessary to post the {@link MoveEntityEvent position event} for an {@link Entity}.
     *
     * @param entity The event
     */
    public static void callNaturalMoveEntityEvent(final net.minecraft.world.entity.Entity entity) {
        if (entity.isRemoved()) {
            return;
        }

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(entity);
            frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.NATURAL);

            final Vector3d originalToPosition = ((Entity) entity).position();
            final @Nullable Vector3d finalPosition =
                    SpongeCommonEventFactory.callMoveEvent(
                            (Entity) entity,
                            new Vector3d(entity.xOld, entity.yOld, entity.zOld),
                            originalToPosition);

            if (finalPosition == null) {
                entity.snapTo(entity.xOld, entity.yOld, entity.zOld);
            } else if (!finalPosition.equals(originalToPosition)) {
                entity.snapTo(finalPosition.x(), finalPosition.y(), finalPosition.z());
            }
        }
    }

    /**
     * Performs the logic necessary to post the {@link RotateEntityEvent rotation event} for an {@link Entity}.
     *
     * @param entity The event
     */
    public static void callNaturalRotateEntityEvent(final net.minecraft.world.entity.Entity entity) {
        if (entity.isRemoved() || (entity.getXRot() == entity.xRotO && entity.getYRot() == entity.yRotO)) {
            return;
        }

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(entity);

            // x and y are the opposite way around to the rotation in the API.
            final @Nullable Vector3d finalRotation =
                    SpongeCommonEventFactory.callRotateEvent(
                            (Entity) entity,
                            new Vector3d(entity.yRotO, entity.xRotO, 0),
                            new Vector3d(entity.getYRot(), entity.getXRot(), 0)
                    );

            if (finalRotation == null) {
                entity.setXRot(entity.xRotO);
                entity.setYRot(entity.yRotO);
            } else {
                entity.setXRot((float) finalRotation.y());
                entity.setYRot((float) finalRotation.x());
            }
        }
    }

    public static @Nullable Vector3d callMoveEvent(
            final org.spongepowered.api.entity.Entity movingEntity,
            final Vector3d fromPosition,
            final Vector3d toPosition) {

        final Cause cause = PhaseTracker.getCauseStackManager().currentCause();
        // Call move & rotate event as needed...
        if (ShouldFire.MOVE_ENTITY_EVENT && fromPosition != null && toPosition != null &&
                !fromPosition.mul(SpongeCommonEventFactory.MOVEMENT_GRID_POINTS_PER_BLOCK).toInt()
                        .equals(toPosition.mul(SpongeCommonEventFactory.MOVEMENT_GRID_POINTS_PER_BLOCK).toInt())) {
            final MoveEntityEvent event = SpongeEventFactory.createMoveEntityEvent(cause, movingEntity, fromPosition,
                    toPosition, toPosition);
            if (SpongeCommon.post(event)) {
                return null;
            } else {
                return event.destinationPosition();
            }
        } else {
            return toPosition;
        }
    }

    public static @Nullable Vector3d callRotateEvent(
            final org.spongepowered.api.entity.Entity movingEntity,
            final Vector3d fromRotation,
            final Vector3d toRotation
    ) {

        final Cause cause = PhaseTracker.getCauseStackManager().currentCause();
        if (ShouldFire.ROTATE_ENTITY_EVENT && fromRotation != null && toRotation != null &&
                !fromRotation.mul(SpongeCommonEventFactory.ROTATION_GRID_POINTS_PER_UNIT).toInt()
                        .equals(toRotation.mul(SpongeCommonEventFactory.ROTATION_GRID_POINTS_PER_UNIT).toInt())) {
            final RotateEntityEvent event = SpongeEventFactory.createRotateEntityEvent(cause, movingEntity, fromRotation,
                    toRotation);
            if (SpongeCommon.post(event)) {
                return null;
            } else {
                return event.toRotation();
            }
        } else {
            return toRotation;
        }
    }

    public static DestructEntityEvent.Death callDestructEntityEventDeath(final LivingEntity entity, final @Nullable DamageSource source) {
        return SpongeCommonEventFactory.callDestructEntityEventDeath(entity, source, Audience.empty());
    }

    public static DestructEntityEvent.Death callDestructEntityEventDeath(final LivingEntity entity, final @Nullable DamageSource source,
            final Audience originalChannel) {

        final Component originalMessage;
        Optional<UUID> sourceCreator = Optional.empty();
        final boolean messageCancelled = false;

        if (source != null && source.getDirectEntity() instanceof CreatorTrackedBridge creatorBridge) {
            sourceCreator = creatorBridge.tracker$getCreatorUUID();
        }

        originalMessage = SpongeAdventure.asAdventure(entity.getCombatTracker().getDeathMessage());
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            if (source != null) {
                frame.pushCause(source);
            }

            sourceCreator.ifPresent(user -> frame.addContext(EventContextKeys.CREATOR, user));

            final DestructEntityEvent.Death event = SpongeEventFactory.createDestructEntityEventDeath(frame.currentCause(),
                    originalChannel, Optional.of(originalChannel), originalMessage, originalMessage, (Living) entity,
                ((ServerLevel) entity.level()).getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY), messageCancelled);
            SpongeCommon.post(event);

            return event;
        }
    }

    public enum CollisionType
    {
        MOVE, FALL, STEP_ON, INSIDE
    }

    public static boolean handleCollideBlockEvent(final Block block, final Level world, final BlockPos pos,
            final net.minecraft.world.level.block.state.BlockState state,
            final net.minecraft.world.entity.Entity entity, final Direction direction, final CollisionType type) {
        if (world.isClientSide) {
            return false;
        }

        if (world.isClientSide() || pos.getY() < world.getMinY()) {
            return false;
        }

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(entity);

            if (entity instanceof CreatorTrackedBridge) {
                final CreatorTrackedBridge spongeEntity = (CreatorTrackedBridge) entity;
                spongeEntity.tracker$getCreatorUUID().ifPresent(user -> frame.addContext(EventContextKeys.CREATOR, user));
            }

            // TODO: Add target side support
            final ServerLocation loc = ServerLocation.of((ServerWorld) world, VecHelper.toVector3d(pos));
            final CollideBlockEvent event;
            switch (type) {
                case MOVE:
                    event = SpongeEventFactory.createCollideBlockEventMove(frame.currentCause(), (BlockState) state, loc, direction);
                    break;
                case FALL:
                    event = SpongeEventFactory.createCollideBlockEventFall(frame.currentCause(), (BlockState) state, loc, direction);
                    break;
                case STEP_ON:
                    event = SpongeEventFactory.createCollideBlockEventStepOn(frame.currentCause(), (BlockState) state, loc, direction);
                    break;
                case INSIDE:
                    event = SpongeEventFactory.createCollideBlockEventInside(frame.currentCause(), (BlockState) state, loc, direction);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown type " + type);
            }
            final boolean cancelled = SpongeCommon.post(event);
            if (!cancelled) {
                final EntityBridge spongeEntity = (EntityBridge) entity;
                if (!pos.equals(spongeEntity.bridge$getLastCollidedBlockPos())) {
                    final PhaseContext<?> context = PhaseTracker.getInstance().getPhaseContext();
                    context.applyNotifierIfAvailable(notifier -> {
                        final LevelChunkBridge spongeChunk = (LevelChunkBridge) world.getChunkAt(pos);
                        spongeChunk.bridge$addTrackedBlockPosition(block, pos, notifier, PlayerTracker.Type.NOTIFIER);

                    });
                }
            }
            return cancelled;
        }
    }

    public static boolean handleCollideImpactEvent(final net.minecraft.world.entity.Entity projectile, final @Nullable ProjectileSource projectileSource,
            final HitResult movingObjectPosition) {
        final HitResult.Type movingObjectType = movingObjectPosition.getType();
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(projectile);
            frame.addContext(EventContextKeys.PROJECTILE_SOURCE, projectileSource == null
                    ? UnknownProjectileSource.UNKNOWN
                    : projectileSource);
            final Optional<UUID> creator = PhaseTracker.getInstance().getPhaseContext().getCreator();
            creator.ifPresent(user -> frame.addContext(EventContextKeys.CREATOR, user));

            final ServerLocation impactPoint = ServerLocation.of((ServerWorld) projectile.level(), VecHelper.toVector3d(movingObjectPosition.getLocation()));
            boolean cancelled = false;

            if (movingObjectType == HitResult.Type.BLOCK) {
                final BlockHitResult blockMovingObjectPosition = (BlockHitResult) movingObjectPosition;
                final BlockPos blockPos = blockMovingObjectPosition.getBlockPos();
                if (blockPos.getY() < projectile.level().getMinY()) {
                    return false;
                }

                final BlockSnapshot targetBlock = ((ServerWorld) projectile.level()).createSnapshot(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                final Direction side = DirectionFacingProvider.INSTANCE.getKey(blockMovingObjectPosition.getDirection()).get();

                final CollideBlockEvent.Impact event = SpongeEventFactory.createCollideBlockEventImpact(frame.currentCause(),
                        impactPoint, targetBlock.state(),
                        targetBlock.location().get(), side);
                cancelled = SpongeCommon.post(event);
                // Track impact block if event is not cancelled
                if (!cancelled && creator.isPresent()) {
                    final BlockPos targetPos = VecHelper.toBlockPos(impactPoint.blockPosition());
                    final LevelChunkBridge spongeChunk = (LevelChunkBridge) projectile.level().getChunkAt(targetPos);
                    spongeChunk.bridge$addTrackedBlockPosition((Block) targetBlock.state().type(), targetPos, creator.get(), PlayerTracker.Type.NOTIFIER);
                }
            } else if (movingObjectType == HitResult.Type.ENTITY) { // entity
                final EntityHitResult entityMovingObjectPosition = (EntityHitResult) movingObjectPosition;
                final ArrayList<Entity> entityList = new ArrayList<>();
                entityList.add((Entity) entityMovingObjectPosition.getEntity());
                final CollideEntityEvent.Impact event = SpongeEventFactory.createCollideEntityEventImpact(frame.currentCause(), entityList, impactPoint);
                        cancelled = SpongeCommon.post(event);
            }

            return cancelled;
        }
    }

    public static SetAITargetEvent callSetAttackTargetEvent(final @Nullable Entity target, final Agent agent) {
        final SetAITargetEvent event = SpongeEventFactory.createSetAITargetEvent(PhaseTracker.getCauseStackManager().currentCause(), agent, Optional.ofNullable(target));
        SpongeCommon.post(event);
        return event;
    }

    /**
     * @author gabizou - April 19th, 2018
     * Creates two events here:
     * - {@link DropItemEvent}
     * - {@link ConstructEntityEvent}
     *
     * This is to reduce the code size from normal entity drops and player drops.
     * While player drops usually require performing position and motion modifications,
     * we return the item stack if it is to be thrown (this allows the event to have a
     * say in what item is dropped).
     *
     * @param entity The entity throwing the item
     * @param posX The position x for the item stack to spawn
     * @param posY The position y for the item stack to spawn
     * @param posZ The position z for the item stack to spawn
     * @param snapshot The item snapshot of the item to drop
     * @param original The original list to be used
     * @param frame
     * @return The item if it is to be spawned, null if to be ignored
     */
    public static @Nullable ItemStack throwDropItemAndConstructEvent(final net.minecraft.world.entity.Entity entity, final double posX, final double posY,
        final double posZ, final ItemStackSnapshot snapshot, final List<ItemStackSnapshot> original, final CauseStackManager.StackFrame frame) {
        final ItemStack item;

        frame.pushCause(entity);

        // FIRST we want to throw the DropItemEvent.PRE
        final DropItemEvent.Pre dropEvent = SpongeEventFactory.createDropItemEventPre(frame.currentCause(),
            ImmutableList.of(snapshot), original);
        SpongeCommon.post(dropEvent);
        if (dropEvent.isCancelled()) {
            return null;
        }
        if (dropEvent.droppedItems().isEmpty()) {
            return null;
        }

        // SECOND throw the ConstructEntityEvent
        frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);
        final ConstructEntityEvent.Pre event = SpongeEventFactory.createConstructEntityEventPre(frame.currentCause(), ServerLocation.of((ServerWorld) entity.level(), posX, posY, posZ), new Vector3d(0, 0, 0), EntityTypes.ITEM.get());
        frame.removeContext(EventContextKeys.SPAWN_TYPE);
        SpongeCommon.post(event);
        if (event.isCancelled()) {
            return null;
        }

        item = event.isCancelled() ? null : ItemStackUtil.fromSnapshotToNative(dropEvent.droppedItems().get(0));
        if (item == null) {
            return null;
        }
        return item;
    }

    public static PlaySoundEvent.@Nullable Broadcast callPlaySoundBroadcastEvent(final CauseStackManager.StackFrame frame, final LevelBridge bridge,
        final BlockPos pos, final int effectID) {
        final Supplier<SoundType> soundType;
        final float volume;
        if (effectID == Constants.WorldEvents.PLAY_WITHER_SPAWN_EVENT) {
            soundType = SoundTypes.ENTITY_WITHER_SPAWN;
            volume = 1.0F;
        } else if (effectID == Constants.WorldEvents.PLAY_ENDERDRAGON_DEATH_EVENT) {
            soundType = SoundTypes.ENTITY_ENDER_DRAGON_DEATH;
            volume = 5.0F;
        } else if (effectID == Constants.WorldEvents.PLAY_BLOCK_END_PORTAL_SPAWN_EVENT) {
            soundType = SoundTypes.BLOCK_END_PORTAL_SPAWN;
            volume = 1.0F;
        } else {
            return null;
        }
        final ServerLocation location = ServerLocation.of((ServerWorld) bridge, pos.getX(), pos.getY(), pos.getZ());
        final PlaySoundEvent.Broadcast event = SpongeEventFactory.createPlaySoundEventBroadcast(frame.currentCause(), location,
            Sound.Source.HOSTILE, soundType.get(), 1.0F, volume);
        SpongeCommon.post(event);
        return event;
    }

    public static PlaySoundEvent.FromJukebox callPlaySoundFromJukeboxEvent(final Cause cause, final JukeboxBlockEntity jukebox,
        final MusicDisc recordType, final int data) {
        final Jukebox apiJuke = (Jukebox) jukebox;
        final ServerLocation location = (ServerLocation) apiJuke.location();
        final PlaySoundEvent.FromJukebox
            event =
            data == 0 ? SpongeEventFactory
                .createPlaySoundEventFromJukeboxStart(cause, apiJuke, location, recordType, Sound.Source.RECORD, recordType.sound(), 1.0F, 4.0F)
                      : SpongeEventFactory
                .createPlaySoundEventFromJukeboxStop(cause, apiJuke, location, recordType, Sound.Source.RECORD, recordType.sound(), 1.0F, 4.0F);
        SpongeCommon.post(event);
        return event;
    }

    @SuppressWarnings("ConstantConditions")
    public static PlaySoundEvent.AtEntity callPlaySoundAtEntityEvent(final Cause cause, final net.minecraft.world.entity.player.@Nullable Player entity,
        final LevelBridge worldMixin, final double x, final double y, final double z, final net.minecraft.sounds.SoundSource category,
        final SoundEvent name, final float pitch, final float volume) {
        final ServerLocation location = ServerLocation.of((ServerWorld) worldMixin, x, y, z);
        final PlaySoundEvent.AtEntity event = SpongeEventFactory.createPlaySoundEventAtEntity(cause, location,
            Optional.ofNullable((ServerPlayer) entity), SpongeAdventure.asAdventure(category), (SoundType) (Object) name, pitch, volume);
        SpongeCommon.post(event);
        return event;
    }

    public static PlaySoundEvent.NoteBlock callPlaySoundNoteBlockEvent(final Cause cause, final World world, final BlockPos pos, final SoundEvent soundEvent, final InstrumentType instrument, final NotePitch notePitch, final Float pitch) {
        final ServerLocation location = ServerLocation.of((ServerWorld) world, pos.getX(), pos.getY(), pos.getZ());
        final PlaySoundEvent.NoteBlock event = SpongeEventFactory.createPlaySoundEventNoteBlock(cause, instrument, location, notePitch, Sound.Source.RECORD, (SoundType) (Object) soundEvent, pitch, 3.0F);
        SpongeCommon.post(event);
        return event;
    }

    /**
     * Returns MapInfo of newly created map, if the event was not cancelled.
     *
     * @param cause Cause of the event
     * @return MapInfo if event was not cancelled
     */
    public static Optional<MapInfo> fireCreateMapEvent(final Cause cause) {
        return SpongeCommonEventFactory.fireCreateMapEvent(cause, Collections.emptySet());
    }

    public static Optional<MapInfo> fireCreateMapEvent(final Cause cause, final Set<Value<?>> values) {

        final ServerLevel defaultWorld = (ServerLevel) Sponge.server().worldManager().world(DefaultWorldKeys.DEFAULT).get();
        final MapIdTrackerBridge mapIdTrackerBridge = (MapIdTrackerBridge) defaultWorld.getDataStorage()
                .computeIfAbsent(MapIndex.factory(), Constants.Map.MAP_INDEX_DATA_NAME);

        final MapItemSavedData mapData = MapItemSavedData.createFresh(0, 0, (byte) 0, false, false, Level.END);

        final MapInfo mapInfo = (MapInfo) mapData;
        for (final Value<?> value : values) {
            mapInfo.offer(value);
        }

        final CreateMapEvent event = SpongeEventFactory.createCreateMapEvent(cause, mapInfo);
        SpongeCommon.post(event);
        if (event.isCancelled()) {
            return Optional.empty();
        }

        final int id = mapIdTrackerBridge.bridge$getHighestMapId().orElse(-1) + 1;
        // Advance map id.
        final MapId mcId = defaultWorld.getFreeMapId();
        if (id != mcId.id()) {
            // TODO: REMOVE OR replace for Integer.MAX_VALUE
            SpongeCommon.logger().warn("Map size corruption, vanilla only allows " + Integer.MAX_VALUE + "! " +
                    "Expected next number was not equal to the true next number.");
            SpongeCommon.logger().warn("Expected: " + id + ". Got: " + mcId);
            SpongeCommon.logger().warn("Automatically cancelling map creation");
            mapIdTrackerBridge.bridge$setHighestMapId(id - 1);
            return Optional.empty();
        }

        defaultWorld.setMapData(new MapId(id), mapData);
        ((MapItemSavedDataBridge) mapData).bridge$initMapId(id);

        ((SpongeMapStorage) Sponge.server().mapStorage()).addMapInfo(mapInfo);

        return Optional.of(mapInfo);
    }

}
