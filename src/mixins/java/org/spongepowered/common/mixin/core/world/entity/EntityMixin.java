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
package org.spongepowered.common.mixin.core.world.entity;

import com.google.common.collect.ImmutableList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LavaCauldronBlock;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.effect.VanishState;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.entity.DismountType;
import org.spongepowered.api.event.cause.entity.DismountTypes;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.data.ChangeDataHolderEvent;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.IgniteEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.portal.PortalLogic;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.server.level.ChunkMapAccessor;
import org.spongepowered.common.accessor.server.level.ChunkMap_TrackedEntityAccessor;
import org.spongepowered.common.accessor.world.entity.EntityAccessor;
import org.spongepowered.common.accessor.world.entity.PortalProcessorAccessor;
import org.spongepowered.common.bridge.commands.CommandSourceProviderBridge;
import org.spongepowered.common.bridge.data.DataCompoundHolder;
import org.spongepowered.common.bridge.data.SpongeDataHolderBridge;
import org.spongepowered.common.bridge.data.TransientBridge;
import org.spongepowered.common.bridge.data.VanishableBridge;
import org.spongepowered.common.bridge.world.entity.EntityBridge;
import org.spongepowered.common.bridge.world.entity.PlatformEntityBridge;
import org.spongepowered.common.bridge.world.entity.PortalProcessorBridge;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.data.DataUtil;
import org.spongepowered.common.data.provider.nbt.NBTDataType;
import org.spongepowered.common.data.provider.nbt.NBTDataTypes;
import org.spongepowered.common.data.value.ImmutableSpongeValue;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.DamageEventUtil;
import org.spongepowered.common.util.ReflectionUtil;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityBridge, PlatformEntityBridge, VanishableBridge, CommandSourceProviderBridge, DataCompoundHolder, TransientBridge {

    // @formatter:off

    @Shadow public abstract Level shadow$level();
    @Shadow public int invulnerableTime;
    @Shadow @Final protected RandomSource random;
    @Shadow @Final protected SynchedEntityData entityData;
    @Shadow public float yRotO;
    @Shadow @Nullable private Entity vehicle;
    @Shadow private ImmutableList<Entity> passengers;
    @Shadow private net.minecraft.world.phys.Vec3 position;
    @Shadow private int remainingFireTicks;
    @Shadow protected UUID uuid;
    @Shadow private EntityDimensions dimensions;

    @Shadow protected abstract void shadow$unsetRemoved();
    @Shadow public abstract void shadow$setRemoved(Entity.RemovalReason reason);
    @Shadow public abstract void shadow$setPos(double x, double y, double z);
    @Shadow public abstract double shadow$getX();
    @Shadow public abstract double shadow$getY();
    @Shadow public abstract double shadow$getZ();
    @Shadow public abstract void shadow$remove(Entity.RemovalReason reason);
    @Shadow public abstract void shadow$discard();
    @Shadow public abstract boolean shadow$isRemoved();
    @Shadow public abstract int shadow$getId();
    @Shadow public abstract boolean shadow$isVehicle();
    @Shadow public abstract void shadow$playSound(SoundEvent soundIn, float volume, float pitch);
    @Shadow public abstract boolean shadow$isInvisible();
    @Shadow public abstract void shadow$setInvisible(boolean invisible);
    @Shadow public abstract EntityType<?> shadow$getType();
    @Shadow public abstract void shadow$teleportTo(double x, double y, double z);
    @Shadow public abstract CommandSourceStack shadow$createCommandSourceStackForNameResolution(ServerLevel level);
    @Shadow public abstract net.minecraft.world.phys.Vec3 shadow$position();
    @Shadow @Nullable public abstract ItemEntity shadow$spawnAtLocation(ServerLevel $$0, ItemStack stack, float offsetY);
    @Shadow @Nullable public abstract Entity shadow$getVehicle();
    @Shadow public abstract AABB shadow$getBoundingBox();
    @Shadow @Nullable public abstract PlayerTeam shadow$getTeam();
    @Shadow public abstract void shadow$clearFire();
    @Shadow protected abstract void shadow$setSharedFlag(int flag, boolean set);
    @Shadow public abstract SynchedEntityData shadow$getEntityData();
    @Shadow public abstract net.minecraft.world.phys.Vec3 shadow$getDeltaMovement();
    @Shadow public abstract void shadow$setDeltaMovement(net.minecraft.world.phys.Vec3 motion);
    @Shadow public abstract void shadow$unRide();
    @Shadow protected abstract void shadow$removeAfterChangingDimensions();
    @Shadow public abstract float shadow$getYRot();
    @Shadow public abstract float shadow$getXRot();
    @Shadow public abstract void shadow$setYRot(final float param0);
    @Shadow protected abstract Vec3 shadow$collide(Vec3 param0);
    @Shadow public abstract boolean shadow$fireImmune();
    @Shadow public abstract boolean shadow$onGround();
    @Shadow @Nullable protected abstract String shadow$getEncodeId();
    @Shadow @javax.annotation.Nullable public PortalProcessor portalProcess;
    // @formatter:on

    @Shadow public abstract void move(final MoverType $$0, final Vec3 $$1);

    @Shadow public abstract boolean save(final CompoundTag $$0);

    @Shadow public abstract Vec3 calculateViewVector(final float $$0, final float $$1);

    @Shadow public abstract Level level();

    @Shadow public abstract boolean startRiding(final Entity $$0);

    private boolean impl$isConstructing = true;
    private VanishState impl$vanishState = VanishState.unvanished();
    protected boolean impl$transient = false;
    protected boolean impl$moveEventsFired = false;
    protected boolean impl$hasCustomFireImmuneTicks = false;
    protected short impl$fireImmuneTicks = 0;
    private BlockPos impl$lastCollidedBlockPos;
    private Boolean impl$playerTouchDeclared;

    // When changing custom data it is serialized on to this.
    // On writeInternal the SpongeData tag is added to the new CompoundNBT accordingly
    // In a Forge environment the ForgeData tag is managed by forge
    // Structure: tileNbt - ForgeData - SpongeData - customdata
    private CompoundTag impl$customDataCompound;

    @Override
    public boolean bridge$isConstructing() {
        return this.impl$isConstructing;
    }

    @Override
    public void bridge$fireConstructors() {
        this.impl$isConstructing = false;
    }

    @Override
    public boolean bridge$isPlayerTouchDeclared() {
        if (this.impl$playerTouchDeclared == null) {
            this.impl$playerTouchDeclared = ReflectionUtil.isPlayerTouchDeclared(this.getClass());
        }
        return this.impl$playerTouchDeclared;
    }

    @Override
    public boolean bridge$setPosition(final Vector3d position) {
        return this.bridge$setLocation(ServerLocation.of((ServerWorld) this.shadow$level(), position));
    }

    @Override
    public boolean bridge$setLocation(final ServerLocation location) {
        if (this.shadow$isRemoved() || ((LevelBridge) location.world()).bridge$isFake()) {
            return false;
        }

        final ServerLevel originalWorld = (ServerLevel) this.shadow$level();
        final PhaseTracker phaseTracker = PhaseTracker.getWorldInstance(originalWorld);
        try (final CauseStackManager.StackFrame frame = phaseTracker.pushCauseFrame()) {
            frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.PLUGIN);

            final ServerLevel originalDestinationWorld = (ServerLevel) location.world();
            final ServerLevel destinationWorld;
            final @org.checkerframework.checker.nullness.qual.Nullable Vector3d destinationPosition;

            final boolean isChangeOfWorld = this.shadow$level() != originalDestinationWorld;
            if (isChangeOfWorld) {
                final ChangeEntityWorldEvent.Pre event = PlatformHooks.INSTANCE.getEventHooks()
                        .callChangeEntityWorldEventPre((Entity) (Object) this, originalDestinationWorld);
                if (event.isCancelled() || ((LevelBridge) event.destinationWorld()).bridge$isFake()) {
                    return false;
                }

                destinationWorld = (ServerLevel) event.destinationWorld();
                final ChangeEntityWorldEvent.Reposition repositionEvent =
                        this.bridge$fireRepositionEvent(event.originalDestinationWorld(), event.destinationWorld(), location.position());
                if (repositionEvent.isCancelled()) {
                    return false;
                }
                destinationPosition = repositionEvent.destinationPosition();
            } else {
                destinationWorld = (ServerLevel) this.shadow$level();
                destinationPosition = this.impl$fireMoveEvent(phaseTracker, location.position());
                if (destinationPosition == null) {
                    return false;
                }
            }

            final boolean completed = this.impl$setLocation(isChangeOfWorld, destinationWorld, destinationPosition);

            if (isChangeOfWorld) {
                Sponge.eventManager().post(SpongeEventFactory.createChangeEntityWorldEventPost(
                        phaseTracker.currentCause(),
                        (org.spongepowered.api.entity.Entity) this,
                        (ServerWorld) originalWorld,
                        (ServerWorld) originalDestinationWorld,
                        (ServerWorld) destinationWorld
                ));
            }
            return completed;
        }
    }

    /**
     * {@link Entity#teleportTo(double, double, double)}
     * {@link Entity#teleportTo(ServerLevel, double, double, double, Set, float, float)}
     */
    protected boolean impl$setLocation(final boolean isChangeOfWorld, final ServerLevel level, final Vector3d pos) {
        // TODO post teleport ticket needed?

        if (!isChangeOfWorld) {
            this.shadow$teleportTo(pos.x(), pos.y(), pos.z());
            return true;
        }

        this.shadow$unRide();
        /*TODO old code had:
        this.bridge$remove(Entity.RemovalReason.CHANGED_DIMENSION, true); // but via PlatformServerLevelBridge#bridge$removeEntity
        this.bridge$revive();
        level.addDuringTeleport((Entity) (Object) this);
         */
        var entity = this.shadow$getType().create(level, EntitySpawnReason.DIMENSION_TRAVEL);
        if (entity == null) {
            return false;
        }
        entity.restoreFrom((Entity) (Object) this);
        entity.moveTo(pos.x(), pos.y(), pos.z());
        this.shadow$setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
        level.addDuringTeleport(entity);
        // TODO old code had reset empty time? on original/new world
        return true;
    }

    @Override
    public boolean bridge$dismountRidingEntity(final DismountType type) {
        if (!this.shadow$level().isClientSide && ShouldFire.RIDE_ENTITY_EVENT_DISMOUNT) {
            try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
                frame.pushCause(this);
                frame.addContext(EventContextKeys.DISMOUNT_TYPE, type);
                if (SpongeCommon.post(SpongeEventFactory.
                        createRideEntityEventDismount(frame.currentCause(), (org.spongepowered.api.entity.Entity) this.shadow$getVehicle()))) {
                    return false;
                }
            }
        }

        final Entity tempEntity = this.shadow$getVehicle();
        if (tempEntity != null) {
            this.vehicle = null;
            ((EntityAccessor) tempEntity).invoker$removePassenger((Entity) (Object) this);
        }
        return true;
    }

    @Override
    public boolean bridge$removePassengers(final DismountType type) {
        boolean dismount = false;
        for (int i = this.passengers.size() - 1; i >= 0; --i) {
            dismount = ((EntityBridge) this.passengers.get(i)).bridge$dismountRidingEntity(type) || dismount;
        }
        return dismount;
    }

    @Override
    public boolean bridge$isInvisible() {
        return this.shadow$isInvisible();
    }

    @Override
    public void bridge$setInvisible(final boolean invisible) {
        this.shadow$setInvisible(invisible);
        if (invisible) {
            ((SpongeDataHolderBridge) this).bridge$offer(Keys.IS_INVISIBLE, true);
        } else {
            ((SpongeDataHolderBridge) this).bridge$remove(Keys.IS_INVISIBLE);
        }
    }

    @Override
    public VanishState bridge$vanishState() {
        return this.impl$vanishState;
    }

    @Override
    public void bridge$vanishState(VanishState state) {
        this.impl$vanishState = state;

        final ChunkMap_TrackedEntityAccessor trackerAccessor = ((ChunkMapAccessor) ((ServerWorld) this.shadow$level()).chunkManager()).accessor$entityMap().get(this.shadow$getId());
        if (trackerAccessor == null) {
            return;
        }

        if (this.bridge$vanishState().invisible()) {
            for (final ServerPlayerConnection playerConnection : trackerAccessor.accessor$seenBy()) {
                trackerAccessor.accessor$removePlayer(playerConnection.getPlayer());
            }

            if ((Entity) (Object) this instanceof ServerPlayer) {
                for (final ServerPlayer entityPlayerMP : SpongeCommon.server().getPlayerList().getPlayers()) {
                    if ((Object) this == entityPlayerMP) {
                        continue;
                    }
                    entityPlayerMP.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(this.uuid)));
                }
            }
        } else {
            for (final ServerPlayer entityPlayerMP : SpongeCommon.server().getPlayerList().getPlayers()) {
                if ((Object) this == entityPlayerMP) {
                    continue;
                }
                if ((Entity) (Object) this instanceof ServerPlayer player) {
                    entityPlayerMP.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));
                }
                trackerAccessor.accessor$updatePlayer(entityPlayerMP);
            }
        }
    }

    @Override
    public boolean bridge$isTransient() {
        return this.shadow$getEncodeId() == null;
    }

    @Override
    public void bridge$setTransient(final boolean value) {
        this.impl$transient = value;
    }

    @Override
    public void bridge$setFireImmuneTicks(final int ticks) {
        this.impl$hasCustomFireImmuneTicks = true;
        this.impl$fireImmuneTicks = (short) ticks;
    }

    @Override
    public CommandSourceStack bridge$getCommandSource(final Cause cause) {
        return this.shadow$createCommandSourceStackForNameResolution((ServerLevel) this.shadow$level());
    }

    /**
     * @author faithcaio
     * @reason handle dimension change events see {@link #bridge$changeDimension(DimensionTransition)}
     *
     * TODO we may need to separate into Vanilla and Forge again
     */
    @Overwrite
    public Entity teleport(final TeleportTransition transition) {
        return this.bridge$changeDimension(transition);
    }

    @Inject(method = "setAsInsidePortal", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/entity/Entity;portalProcess:Lnet/minecraft/world/entity/PortalProcessor;"))
    public void impl$onCreatePortalProcessor(final Portal $$0, final BlockPos $$1, final CallbackInfo ci) {
        if (((LevelBridge) this.shadow$level()).bridge$isFake()) {
            return;
        }

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()){
            var be = this.shadow$level().getBlockEntity(this.portalProcess.getEntryPosition());
            if (be != null) {
                frame.pushCause(be);
            }
            frame.pushCause(this);
            var portal = ((PortalProcessorAccessor)this.portalProcess).accessor$portal();
            ((PortalProcessorBridge)this.portalProcess).bridge$init(this.shadow$level());
            frame.pushCause(portal);

            final int portalTransitionTime = $$0.getPortalTransitionTime((ServerLevel) this.shadow$level(), (Entity) (Object) this);
            var event = SpongeEventFactory.createInvokePortalEventEnter(frame.currentCause(),
                    (org.spongepowered.api.entity.Entity) this,
                    Optional.empty(),
                    (PortalLogic) $$0,
                    portalTransitionTime);
            if (SpongeCommon.post(event)) {
                this.portalProcess = null;
            } else {
                event.customPortalTransitionTime().ifPresent(customTime -> {
                    ((PortalProcessorBridge)this.portalProcess).bridge$setTransitionTime(customTime);
                });
            }
        }
    }

    /**
     * See {@link PortalProcessorMixin#impl$onGetPortalDestination} for portal events
     */
    @Redirect(method = "handlePortal",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/world/entity/Entity;"))
    public Entity impl$onChangeDimension(final Entity instance, final TeleportTransition transition) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(this);
            var be = this.shadow$level().getBlockEntity(this.portalProcess.getEntryPosition());
            if (be != null) {
                frame.pushCause(be);
            }
            final var portal = ((PortalProcessorAccessor) this.portalProcess).accessor$portal();
            frame.pushCause(portal);
            var movementType = portal == Blocks.END_GATEWAY ? MovementTypes.END_GATEWAY : MovementTypes.PORTAL;
            frame.addContext(EventContextKeys.MOVEMENT_TYPE, movementType);
            frame.addContext(EventContextKeys.PORTAL_LOGIC, (PortalLogic) portal);
            // TODO frame.addContext(EventContextKeys.PORTAL, transition);
            // TODO 2-dim portal?
            this.impl$moveEventsFired = true;
            return instance.teleport(transition);
        } finally {
            this.impl$moveEventsFired = false;
        }
    }

    /**
     * This is effectively an overwrite of changeDimension.
     *
     * @return The {@link Entity} that is either this one, or replaces this one
     */
    @SuppressWarnings("ConstantConditions")
    @Nullable
    public Entity bridge$changeDimension(final TeleportTransition transition) {
        final Entity thisEntity = (Entity) (Object) this;
        if (!(thisEntity.level() instanceof ServerLevel oldLevel) || this.shadow$isRemoved()) { // Sponge inverted check
            return null;
        }

        // TODO context/events for non-players
        var newLevel = transition.newLevel();
        var passengers = thisEntity.getPassengers();
        List<Entity> recreatedPassengers = new ArrayList<>();
        this.shadow$unRide();
        for (final Entity passenger : passengers) {
            var recreatedPassenger = passenger.teleport(transition);
            if (recreatedPassenger != null) {
                recreatedPassengers.add(recreatedPassenger);
            }
        }

        ProfilerFiller filler = Profiler.get();
        filler.push("changeDimension");

        var newEntity = oldLevel.dimension() == newLevel.dimension() ? thisEntity : thisEntity.getType().create(newLevel, EntitySpawnReason.DIMENSION_TRAVEL);
        if (newEntity != null) {
            if (thisEntity != newEntity) {
                newEntity.restoreFrom(thisEntity);
                this.shadow$removeAfterChangingDimensions();
            }

            newEntity.moveTo(transition.position().x, transition.position().y, transition.position().z, transition.yRot(), newEntity.getXRot());
            newEntity.setDeltaMovement(transition.deltaMovement());
            if (thisEntity != newEntity) {
                newLevel.addDuringTeleport(newEntity);
            }

            for (Entity recreatedPassenger : recreatedPassengers) {
                recreatedPassenger.startRiding(newEntity, true);
            }

            oldLevel.resetEmptyTime();
            newLevel.resetEmptyTime();
            transition.postTeleportTransition().onTransition(newEntity);

        }

        // TODO impl$fireMoveEvent when not changing dimensions (e.g. EndGateways)

        filler.pop();
        return newEntity;
    }

    @Override
    public final ChangeEntityWorldEvent.Reposition bridge$fireRepositionEvent(final ServerWorld originalDestinationWorld,
            final ServerWorld targetWorld,
            final Vector3d destinationPosition) {

        this.impl$moveEventsFired = true;
        final ChangeEntityWorldEvent.Reposition reposition = SpongeEventFactory.createChangeEntityWorldEventReposition(
                PhaseTracker.getCauseStackManager().currentCause(),
                (org.spongepowered.api.entity.Entity) this,
                (ServerWorld) this.shadow$level(),
                VecHelper.toVector3d(this.position),
                destinationPosition,
                originalDestinationWorld,
                destinationPosition,
                targetWorld
        );

        SpongeCommon.post(reposition);
        return reposition;
    }

    @Override
    public CompoundTag data$getCompound() {
        return this.impl$customDataCompound;
    }

    @Override
    public void data$setCompound(final CompoundTag nbt) {
        this.impl$customDataCompound = nbt;
    }

    @Override
    public NBTDataType data$getNBTDataType() {
        return NBTDataTypes.ENTITY;
    }


    protected final @org.checkerframework.checker.nullness.qual.Nullable Vector3d impl$fireMoveEvent(
            final PhaseTracker phaseTracker, final Vector3d originalDestinationPosition) {
        final boolean hasMovementContext = phaseTracker.currentContext().containsKey(EventContextKeys.MOVEMENT_TYPE);
        if (!hasMovementContext) {
            phaseTracker.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.PLUGIN);
        }

        final MoveEntityEvent event = SpongeEventFactory.createMoveEntityEvent(phaseTracker.currentCause(),
                (org.spongepowered.api.entity.Entity) this, VecHelper.toVector3d(this.shadow$position()),
                originalDestinationPosition,
                originalDestinationPosition);

        if (!hasMovementContext) {
            phaseTracker.popCause();
            phaseTracker.removeContext(EventContextKeys.MOVEMENT_TYPE);
        }

        if (SpongeCommon.post(event)) {
            return null;
        }

        return event.destinationPosition();
    }

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/Entity;vehicle:Lnet/minecraft/world/entity/Entity;",
            ordinal = 0
        ),
        cancellable = true
    )
    private void impl$onStartRiding(final Entity vehicle, final boolean force,
        final CallbackInfoReturnable<Boolean> ci) {
        if (!this.shadow$level().isClientSide && ShouldFire.RIDE_ENTITY_EVENT_MOUNT) {
            PhaseTracker.getCauseStackManager().pushCause(this);
            if (SpongeCommon.post(SpongeEventFactory.createRideEntityEventMount(PhaseTracker.getCauseStackManager().currentCause(), (org.spongepowered.api.entity.Entity) vehicle))) {
                ci.cancel();
            }
            PhaseTracker.getCauseStackManager().popCause();
        }
    }

    /**
     * @author rexbut - December 16th, 2016
     * @reason - adjusted to support {@link DismountTypes}
     */
    @Overwrite
    public void stopRiding() {
        final Entity vehicle = this.shadow$getVehicle();
        if (vehicle != null) {
            if (vehicle.isRemoved()) {
                this.bridge$dismountRidingEntity(DismountTypes.DEATH.get());
            } else {
                this.bridge$dismountRidingEntity(DismountTypes.PLAYER.get());
            }
        }
    }

/*
    @Inject(method = "move",
        at = @At("HEAD"),
        cancellable = true)
    private void impl$onSpongeMoveEntity(final MoverType type, final Vec3d vec3d, final CallbackInfo ci) {
        if (!this.world.isClientSide && !SpongeHooks.checkEntitySpeed(((Entity) (Object) this), vec3d.x(), vec3d.y(), vec3d.z())) {
            ci.cancel();
        }
    }
*/
    @Redirect(method = "lavaHurt",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"
        )
    )
    private boolean impl$createLavaBlockDamageSource(final Entity instance, ServerLevel serverLevel, DamageSource source, float damage) {
        final AABB bb = this.shadow$getBoundingBox().inflate(-0.10000000149011612D, -0.4000000059604645D, -0.10000000149011612D);
        final ServerLocation location = DamageEventUtil.findFirstMatchingBlock(instance, bb, block ->
            block.is(Blocks.LAVA) || block.getBlock() instanceof LavaCauldronBlock);
        if (location != null) {
            var blockSource = org.spongepowered.api.event.cause.entity.damage.source.DamageSource.builder()
                    .from((org.spongepowered.api.event.cause.entity.damage.source.DamageSource) source).block(location)
                    .block(location.createSnapshot()).build();
            return instance.hurtServer(serverLevel, (DamageSource) blockSource, damage);
        }
        return instance.hurtServer(serverLevel, source, damage);
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 impl$onMoveCollide(final Entity entity, final Vec3 originalMove) {
        final Vec3 afterCollide = this.shadow$collide(originalMove);
        if (ShouldFire.COLLIDE_BLOCK_EVENT_MOVE && !originalMove.equals(afterCollide)) {
            // We had a collision! Try to find the colliding block
            final Vec3 position = new Vec3(this.shadow$getX() + afterCollide.x, this.shadow$getY() + afterCollide.y, this.shadow$getZ() + afterCollide.z);
            final AABB boundingBox = this.dimensions.makeBoundingBox(position)
                    .expandTowards(originalMove.x - afterCollide.x, originalMove.y - afterCollide.y, originalMove.z - afterCollide.z);

            Optional<Vec3> closestPoint = Optional.empty();
            for (final VoxelShape shape : this.shadow$level().getBlockCollisions((Entity) (Object) this, boundingBox)) {
                final Optional<Vec3> shapeClosestPoint = shape.closestPointTo(position);
                if (shapeClosestPoint.isPresent()) {
                    if (!closestPoint.isPresent()) {
                        closestPoint = shapeClosestPoint;
                    } else if (position.distanceToSqr(closestPoint.get()) > position.distanceToSqr(shapeClosestPoint.get())) {
                        closestPoint = shapeClosestPoint;
                    }
                }
            }

            final BlockPos pos = closestPoint.map(p -> BlockPos.containing(p.x, p.y, p.z)).orElse(BlockPos.containing(position.x, position.y, position.z));
            final BlockState state = this.shadow$level().getBlockState(pos);
            final org.spongepowered.api.util.Direction dir = org.spongepowered.api.util.Direction.closest(new Vector3d(originalMove.x, originalMove.y, originalMove.z));
            if (!state.isAir() && SpongeCommonEventFactory.handleCollideBlockEvent(state.getBlock(), this.shadow$level(), pos, state,
                    (Entity) (Object) this, dir, SpongeCommonEventFactory.CollisionType.MOVE)) {
                return originalMove;
            }
        }
        return afterCollide;
    }

    @Redirect(method = "checkFallDamage",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;fallOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;F)V"))
    private void impl$onFallOnCollide(final Block block, final Level world, final BlockState state, final BlockPos pos, final Entity entity, final float fallDistance) {
        if (!ShouldFire.COLLIDE_BLOCK_EVENT_FALL || world.isClientSide) {
            block.fallOn(world, state, pos, entity, fallDistance);
            return;
        }

        if (!SpongeCommonEventFactory.handleCollideBlockEvent(block, world, pos, state, entity, org.spongepowered.api.util.Direction.UP, SpongeCommonEventFactory.CollisionType.FALL)) {
            block.fallOn(world, state, pos, entity, fallDistance);
            this.impl$lastCollidedBlockPos = pos;
        }
    }

    @Redirect(
            method = "applyEffectsFromBlocks(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V"
            )
    )
    private void impl$onStepOnCollide(final Block block, final Level world, final BlockPos pos, final BlockState state, final Entity entity) {
        if (!ShouldFire.COLLIDE_BLOCK_EVENT_STEP_ON || world.isClientSide) {
            block.stepOn(world, pos, state, entity);
            return;
        }

        final org.spongepowered.api.util.Direction dir = org.spongepowered.api.util.Direction.NONE;
        if (!SpongeCommonEventFactory.handleCollideBlockEvent(block, world, pos, state, entity, dir, SpongeCommonEventFactory.CollisionType.STEP_ON)) {
            block.stepOn(world, pos, state, entity);
            this.impl$lastCollidedBlockPos = pos;
        }

    }

    @Redirect(method = "checkInsideBlocks(Ljava/util/List;Ljava/util/List;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;entityInside(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)V"
            )
    ) // doBlockCollisions
    private void impl$onCheckInsideBlocksCollide(final BlockState blockState, final Level worldIn, final BlockPos pos, final Entity entityIn) {
        if (!ShouldFire.COLLIDE_BLOCK_EVENT_INSIDE || worldIn.isClientSide || blockState.isAir()) {
            blockState.entityInside(worldIn, pos, entityIn);
            return;
        }

        final org.spongepowered.api.util.Direction dir = org.spongepowered.api.util.Direction.NONE;
        if (!SpongeCommonEventFactory.handleCollideBlockEvent(blockState.getBlock(), worldIn, pos, blockState, entityIn, dir, SpongeCommonEventFactory.CollisionType.INSIDE)) {
            blockState.entityInside(worldIn, pos, entityIn);
            this.impl$lastCollidedBlockPos = pos;
        }

    }

    @Redirect(method = "isSilent", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/network/syncher/SynchedEntityData;get(Lnet/minecraft/network/syncher/EntityDataAccessor;)Ljava/lang/Object;"
    ))
    private Object impl$checkIsSilentOrInvis(final SynchedEntityData data, final EntityDataAccessor<Boolean> key) {
        return data.get(key) || !this.bridge$vanishState().createsSounds();
    }

    @Redirect(method = "push(Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/Entity;noPhysics:Z",
            opcode = Opcodes.GETFIELD))
    private boolean impl$applyEntityCollisionCheckVanish(final Entity entity) {
        return entity.noPhysics || ((VanishableBridge) entity).bridge$vanishState().ignoresCollisions();
    }

    @Redirect(method = "doWaterSplashEffect",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void impl$spawnParticle(
        final Level instance, final ParticleOptions particleOptions, final double xCoord, final double yCoord,
        final double zCoord, final double xOffset, final double yOffset, final double zOffset
    ) {
        if (this.bridge$vanishState().createsParticles()) {
            this.shadow$level().addParticle(particleOptions, xCoord, yCoord, zCoord, xOffset, yOffset, zOffset);
        }
    }

    @Redirect(method = "spawnSprintParticle",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void impl$runningSpawnParticle(
        final Level instance, final ParticleOptions particleOptions, final double xCoord, final double yCoord,
        final double zCoord, final double xOffset, final double yOffset, final double zOffset
    ) {
        if (this.bridge$vanishState().createsParticles()) {
            instance.addParticle(particleOptions, xCoord, yCoord, zCoord, xOffset, yOffset, zOffset);
        }
    }


    /**
     * @return
     * @author gabizou - January 30th, 2016
     * @author blood - May 12th, 2016
     * @author gabizou - June 2nd, 2016
     *     <p>
     *     TODO from i509VCB: gabizou's remider to refactor this code here
     * @reason Rewrites the method entirely for several reasons:
     *     1) If we are in a forge environment, we do NOT want forge to be capturing the item entities, because we handle them ourselves
     *     2) If we are in a client environment, we should not perform any sort of processing whatsoever.
     *     3) This method is entirely managed from the standpoint where our events have final say, as per usual.
     */

    @Inject(
        method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At("HEAD"),
        cancellable = true
    )
    public void impl$throwDropItemConstructEvent(
        final ServerLevel serverLevel, ItemStack stack, float offsetY, CallbackInfoReturnable<ItemEntity> cir
    ) {
        if (stack.isEmpty()) {
            cir.setReturnValue(null);
            return;
        }
        if (((LevelBridge) serverLevel).bridge$isFake()) {
            return;
        }
        // Now the real fun begins.
        final ItemStack item;
        final double posX = this.shadow$position().x;
        final double posY = this.shadow$position().y + offsetY;
        final double posZ = this.shadow$position().z;

        // FIRST we want to throw the DropItemEvent.PRE
        final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(stack);
        final List<ItemStackSnapshot> original = new ArrayList<>();
        original.add(snapshot);

        // We want to frame ourselves here, because of the two events we have to throw, first for the drop item event, then the constructentityevent.
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            // Perform the event throws first, if they return false, return null
            item = SpongeCommonEventFactory.throwDropItemAndConstructEvent(
                (Entity) (Object) this, posX, posY, posZ, snapshot, original, frame);

            if (item == null || item.isEmpty()) {
                cir.setReturnValue(null);
                return;
            }
            final ItemEntity entityitem = new ItemEntity(this.shadow$level(), posX, posY, posZ, item);
            entityitem.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(entityitem);
            cir.setReturnValue(entityitem);
        }
    }

    @org.checkerframework.checker.nullness.qual.Nullable
    @Override
    public BlockPos bridge$getLastCollidedBlockPos() {
        return this.impl$lastCollidedBlockPos;
    }


    @Redirect(method = "setRemainingFireTicks",
        at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/Entity;remainingFireTicks:I",
            opcode = Opcodes.PUTFIELD)
    )
    private void impl$ThrowIgniteEventForFire(final Entity entity, final int ticks) {
        if (!((LevelBridge) this.shadow$level()).bridge$isFake() && ShouldFire.IGNITE_ENTITY_EVENT &&
            this.remainingFireTicks < 1 && ticks >= Constants.Entity.MINIMUM_FIRE_TICKS &&
            this.impl$canCallIgniteEntityEvent()) {

            final PhaseTracker phaseTracker = PhaseTracker.getWorldInstance((ServerLevel) entity.level());
            try (final CauseStackManager.StackFrame frame = phaseTracker.pushCauseFrame()) {

                frame.pushCause(((org.spongepowered.api.entity.Entity) this).location().world());
                final IgniteEntityEvent event = SpongeEventFactory.
                    createIgniteEntityEvent(frame.currentCause(), Ticks.of(ticks), Ticks.of(ticks), (org.spongepowered.api.entity.Entity) this);

                if (SpongeCommon.post(event)) {
                    // Don't do anything
                    return;
                }
                final DataTransactionResult transaction = DataTransactionResult.builder()
                    .replace(new ImmutableSpongeValue<>(Keys.FIRE_TICKS, Ticks.of(Math.max(this.remainingFireTicks, 0))))
                    .success(new ImmutableSpongeValue<>(Keys.FIRE_TICKS, event.fireTicks()))
                    .result(DataTransactionResult.Type.SUCCESS)
                    .build();

                final ChangeDataHolderEvent.ValueChange valueChange = SpongeEventFactory.createChangeDataHolderEventValueChange(
                    phaseTracker.currentCause(),
                    transaction,
                    (DataHolder.Mutable) this);

                Sponge.eventManager().post(valueChange);
                if (valueChange.isCancelled()) {
                    //If the event is cancelled, well, don't change the underlying value.
                    return;
                }
                valueChange.endResult().successfulValue(Keys.FIRE_TICKS)
                    .map(Value::get)
                    .map(t -> (int) t.ticks())
                    .ifPresent(t -> this.remainingFireTicks = t);
            }
            return;
        }
        this.remainingFireTicks = ticks; // Vanilla functionality
    }

    @Redirect(
        method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/Entity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;gameEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void impl$ignoreGameEventIfVanished(final Level instance, final Entity entity, final Holder<GameEvent> gameEvent, final Vec3 vec) {
        if (entity instanceof VanishableBridge && ((VanishableBridge) entity).bridge$vanishState().triggerVibrations()) {
            instance.gameEvent(entity, gameEvent, vec);
        }
    }

    @Redirect(method = "getEncodeId", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
    private boolean impl$respectTransientFlag(final EntityType entityType) {
        if (!entityType.canSerialize()) {
            return false;
        }

        return !this.impl$transient;
    }

    @Inject(method = "getFireImmuneTicks", at = @At(value = "HEAD"), cancellable = true)
    private void impl$getFireImmuneTicks(final CallbackInfoReturnable<Integer> ci) {
        if (this.impl$hasCustomFireImmuneTicks) {
            ci.setReturnValue((int) this.impl$fireImmuneTicks);
        }
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void impl$WriteSpongeDataToCompound(final CompoundTag compound, final CallbackInfoReturnable<CompoundTag> ci) {
        if (DataUtil.syncDataToTag(this)) {
            compound.merge(this.data$getCompound());
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void impl$ReadSpongeDataFromCompound(final CompoundTag compound, final CallbackInfo ci) {
        // TODO If we are in Forge data is already present
        this.data$setCompound(compound); // For vanilla we set the incoming nbt
        // Deserialize custom data...
        DataUtil.syncTagToData(this);
        this.data$setCompound(null); // done reading
    }

    /**
     * Overridden method for Players to determine whether this entity is not immune to
     * fire such that {@link IgniteEntityEvent}s are not needed to be thrown as they
     * cannot take fire damage, nor do they light on fire.
     *
     * @return True if this entity is not immune to fire.
     */
    protected boolean impl$canCallIgniteEntityEvent() {
        return !this.shadow$fireImmune();
    }

    protected void impl$callExpireEntityEvent() {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(this);
            Sponge.eventManager().post(SpongeEventFactory.createExpireEntityEvent(frame.currentCause(), (org.spongepowered.api.entity.Entity) this));
        }
    }
    @Inject(method = "discard", at = @At("TAIL"))
    private void impl$throwExpireForDiscards(final CallbackInfo ci) {
        SpongeCommon.post(SpongeEventFactory.createExpireEntityEvent(PhaseTracker.getInstance().currentCause(), (org.spongepowered.api.entity.Entity) this));
    }

    /*@Redirect(
        method = "setRemainingFireTicks",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/Entity;remainingFireTicks:I",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void impl$callIgnite(Entity entity, int value) {

    }*/

    @Redirect(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
    private boolean impl$allowRidingAnything(final EntityType<?> instance) {
        //Vanilla has started to prevent riding non-serializable entities.
        //This results in players being unable to ride other players.
        return true;
    }
}
