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
package org.spongepowered.common.mixin.core.entity;

import co.aikar.timings.Timing;
import net.minecraft.block.BlockState;
import net.minecraft.block.PortalInfo;
import net.minecraft.block.material.Material;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.TeleportationRepositioner;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.TicketType;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.entity.DismountType;
import org.spongepowered.api.event.cause.entity.DismountTypes;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.IgniteEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.util.Transform;
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
import org.spongepowered.common.accessor.entity.EntityAccessor;
import org.spongepowered.common.bridge.TimingBridge;
import org.spongepowered.common.bridge.command.CommandSourceProviderBridge;
import org.spongepowered.common.bridge.data.CustomDataHolderBridge;
import org.spongepowered.common.bridge.data.DataCompoundHolder;
import org.spongepowered.common.bridge.data.InvulnerableTrackedBridge;
import org.spongepowered.common.bridge.data.VanishableBridge;
import org.spongepowered.common.bridge.entity.EntityBridge;
import org.spongepowered.common.bridge.entity.EntityTypeBridge;
import org.spongepowered.common.bridge.entity.GrieferBridge;
import org.spongepowered.common.bridge.entity.PlatformEntityBridge;
import org.spongepowered.common.bridge.entity.player.ServerPlayerEntityBridge;
import org.spongepowered.common.bridge.util.DamageSourceBridge;
import org.spongepowered.common.bridge.world.PlatformServerWorldBridge;
import org.spongepowered.common.bridge.world.WorldBridge;
import org.spongepowered.common.data.provider.nbt.NBTDataType;
import org.spongepowered.common.data.provider.nbt.NBTDataTypes;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.cause.entity.damage.DamageEventHandler;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.entity.EntityPhase;
import org.spongepowered.common.event.tracking.phase.entity.TeleportContext;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.MinecraftBlockDamageSource;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.portal.NetherPortalType;
import org.spongepowered.common.world.portal.PlatformTeleporter;
import org.spongepowered.math.vector.Vector3d;

import javax.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityBridge, PlatformEntityBridge, VanishableBridge, InvulnerableTrackedBridge,
        TimingBridge, CommandSourceProviderBridge, DataCompoundHolder {

    // @formatter:off
    @Shadow public net.minecraft.world.World level;
    @Shadow public float yRot;
    @Shadow public float xRot;
    @Shadow public int invulnerableTime;
    @Shadow public boolean removed;
    @Shadow public float walkDistO;
    @Shadow public float walkDist;
    @Shadow @Final protected Random random;
    @Shadow @Final protected EntityDataManager entityData;
    @Shadow public float yRotO;
    @Shadow protected int portalTime;
    @Shadow @Nullable private Entity vehicle;
    @Shadow @Final private List<Entity> passengers;
    @Shadow protected boolean onGround;
    @Shadow public float fallDistance;
    @Shadow protected BlockPos portalEntrancePos;
    @Shadow private net.minecraft.util.math.vector.Vector3d position;
    @Shadow private BlockPos blockPosition;
    @Shadow public double xo;
    @Shadow public double yo;
    @Shadow public double zo;

    @Shadow public abstract void shadow$setPos(double x, double y, double z);
    @Shadow public abstract double shadow$getX();
    @Shadow public abstract double shadow$getY();
    @Shadow public abstract double shadow$getZ();
    @Shadow public abstract void shadow$remove();
    @Shadow public abstract void shadow$setCustomName(@Nullable ITextComponent name);
    @Shadow public abstract boolean shadow$hurt(DamageSource source, float amount);
    @Shadow public abstract int shadow$getId();
    @Shadow public abstract boolean shadow$isVehicle();
    @Shadow public abstract void shadow$playSound(SoundEvent soundIn, float volume, float pitch);
    @Shadow protected abstract void shadow$removePassenger(Entity passenger);
    @Shadow public abstract boolean shadow$isInvisible();
    @Shadow public abstract void shadow$setInvisible(boolean invisible);
    @Shadow protected abstract int shadow$getFireImmuneTicks();
    @Shadow public abstract EntityType<?> shadow$getType();
    @Shadow public abstract boolean shadow$isInWater();
    @Shadow public abstract boolean shadow$isPassenger();
    @Shadow public abstract void shadow$teleportTo(double x, double y, double z);
    @Shadow public abstract int shadow$getMaxAirSupply();
    @Shadow public abstract void shadow$doEnchantDamageEffects(LivingEntity entityLivingBaseIn, Entity entityIn);
    @Shadow public abstract CommandSource shadow$createCommandSourceStack();
    @Shadow public abstract World shadow$getCommandSenderWorld();
    @Shadow public abstract net.minecraft.util.math.vector.Vector3d shadow$position();
    @Shadow public abstract MinecraftServer shadow$getServer();
    @Shadow public abstract void shadow$setLevel(World worldIn);
    @Shadow @Nullable public abstract ItemEntity shadow$spawnAtLocation(ItemStack stack, float offsetY);
    @Shadow protected abstract void shadow$setRot(float yaw, float pitch);
    @Shadow @Nullable public abstract Entity shadow$getVehicle();
    @Shadow public abstract boolean shadow$isInvulnerableTo(DamageSource source);
    @Shadow public abstract AxisAlignedBB shadow$getBoundingBox();
    @Shadow public abstract boolean shadow$isSprinting();
    @Shadow public abstract boolean shadow$isAlliedTo(Entity entityIn);
    @Shadow public abstract double shadow$distanceToSqr(Entity entityIn);
    @Shadow public abstract SoundCategory shadow$getSoundSource();
    @Shadow @Nullable public abstract Team shadow$getTeam();
    @Shadow public abstract void shadow$clearFire();
    @Shadow protected abstract void shadow$setSharedFlag(int flag, boolean set);
    @Shadow public abstract EntityDataManager shadow$getEntityData();
    @Shadow public abstract void shadow$moveTo(double x, double y, double z);
    @Shadow public abstract void shadow$absMoveTo(double x, double y, double z, float yaw, float pitch);
    @Shadow public abstract net.minecraft.util.math.vector.Vector3d shadow$getDeltaMovement();
    @Shadow public abstract void shadow$setDeltaMovement(net.minecraft.util.math.vector.Vector3d motion);
    @Shadow public abstract void shadow$unRide();
    @Shadow protected abstract Optional<TeleportationRepositioner.Result> shadow$getExitPortal(
            net.minecraft.world.server.ServerWorld targetWorld, BlockPos targetPosition, boolean isNether);
    @Shadow protected abstract net.minecraft.util.math.vector.Vector3d shadow$getRelativePortalPosition(Direction.Axis direction$axis,
            TeleportationRepositioner.Result teleportationrepositioner$result);
    @Shadow protected abstract void shadow$removeAfterChangingDimensions();
    @Shadow public abstract void shadow$absMoveTo(double p_242281_1_, double p_242281_3_, double p_242281_5_);
    @Shadow protected abstract int shadow$getPermissionLevel();

    // @formatter:on

    private boolean impl$isConstructing = true;
    private boolean impl$untargetable = false;
    private boolean impl$isVanished = false;
    private boolean impl$pendingVisibilityUpdate = false;
    private int impl$visibilityTicks = 0;
    private boolean impl$collision = true;
    private boolean impl$invulnerable = false;
    private boolean impl$transient = false;
    private boolean impl$shouldFireRepositionEvent = true;
    private WeakReference<ServerWorld> impl$originalDestinationWorld = null;
    protected boolean impl$hasCustomFireImmuneTicks = false;
    protected boolean impl$dontCreateExitPortal = false;
    protected short impl$fireImmuneTicks = 0;

    // When changing custom data it is serialized on to this.
    // On writeInternal the SpongeData tag is added to the new CompoundNBT accordingly
    // In a Forge environment the ForgeData tag is managed by forge
    // Structure: tileNbt - ForgeData - SpongeData - customdata
    private CompoundNBT impl$customDataCompound;

    @Override
    public boolean bridge$isConstructing() {
        return this.impl$isConstructing;
    }

    @Override
    public void bridge$fireConstructors() {
        this.impl$isConstructing = false;
    }

    @Override
    public boolean bridge$setLocation(final ServerLocation location) {
        if (this.removed || ((WorldBridge) location.getWorld()).bridge$isFake()) {
            return false;
        }

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(SpongeCommon.getActivePlugin());
            frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.PLUGIN);

            final net.minecraft.util.math.vector.Vector3d originalPosition = this.shadow$position();

            net.minecraft.world.server.ServerWorld destinationWorld = (net.minecraft.world.server.ServerWorld) location.getWorld();

            if (this.shadow$getCommandSenderWorld() != destinationWorld) {
                final ChangeEntityWorldEvent.Pre event = SpongeEventFactory.createChangeEntityWorldEventPre(frame.getCurrentCause(),
                        (org.spongepowered.api.entity.Entity) this, (ServerWorld) this.shadow$getCommandSenderWorld(), location.getWorld(),
                        location.getWorld());
                if (SpongeCommon.postEvent(event) && ((WorldBridge) event.getDestinationWorld()).bridge$isFake()) {
                    return false;
                }

                final ChangeEntityWorldEvent.Reposition repositionEvent =
                        SpongeEventFactory.createChangeEntityWorldEventReposition(frame.getCurrentCause(),
                                (org.spongepowered.api.entity.Entity) this, (ServerWorld) this.shadow$getCommandSenderWorld(),
                                VecHelper.toVector3d(this.shadow$position()), location.getPosition(), event.getOriginalDestinationWorld(),
                                location.getPosition(), event.getDestinationWorld());

                if (SpongeCommon.postEvent(repositionEvent)) {
                    return false;
                }

                destinationWorld = (net.minecraft.world.server.ServerWorld) event.getDestinationWorld();

                this.shadow$setPos(repositionEvent.getDestinationPosition().getX(),
                        repositionEvent.getDestinationPosition().getY(), repositionEvent.getDestinationPosition().getZ());
            } else {
                final Vector3d destination;
                if (ShouldFire.MOVE_ENTITY_EVENT) {
                    final MoveEntityEvent event = SpongeEventFactory.createMoveEntityEvent(frame.getCurrentCause(),
                            (org.spongepowered.api.entity.Entity) this, VecHelper.toVector3d(this.shadow$position()),
                            location.getPosition(), location.getPosition());
                    if (SpongeCommon.postEvent(event)) {
                        return false;
                    }
                    destination = event.getDestinationPosition();
                } else {
                    destination = location.getPosition();
                }
                this.shadow$setPos(destination.getX(), destination.getY(), destination.getZ());
            }

            if (!destinationWorld.getChunkSource().hasChunk((int) this.shadow$getX() >> 4, (int) this.shadow$getZ() >> 4)) {
                // Roll back the position
                this.shadow$setPos(originalPosition.x, originalPosition.y, originalPosition.z);
                return false;
            }

            ((Entity) (Object) this).unRide();

            final net.minecraft.world.server.ServerWorld originalWorld = (net.minecraft.world.server.ServerWorld) this.shadow$getCommandSenderWorld();
            ((PlatformServerWorldBridge) this.shadow$getCommandSenderWorld()).bridge$removeEntity((Entity) (Object) this, true);
            this.bridge$revive();
            this.shadow$setLevel(destinationWorld);
            destinationWorld.addFromAnotherDimension((Entity) (Object) this);

            originalWorld.resetEmptyTime();
            destinationWorld.resetEmptyTime();

            final ChunkPos chunkPos = new ChunkPos((int) this.shadow$getX() >> 4, (int) this.shadow$getZ() >> 4);
            destinationWorld.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkPos, 1, ((Entity) (Object) this).getId());
        }

        return true;
    }

    @Override
    public boolean bridge$dismountRidingEntity(final DismountType type) {
        if (!this.level.isClientSide && ShouldFire.RIDE_ENTITY_EVENT_DISMOUNT) {
            try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
                frame.pushCause(this);
                frame.addContext(EventContextKeys.DISMOUNT_TYPE, type);
                if (SpongeCommon.postEvent(SpongeEventFactory.
                        createRideEntityEventDismount(frame.getCurrentCause(), (org.spongepowered.api.entity.Entity) this.shadow$getVehicle()))) {
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
    public boolean bridge$getIsInvulnerable() {
        return this.impl$invulnerable;
    }

    @Override
    public boolean bridge$isInvisible() {
        return this.shadow$isInvisible();
    }

    @Override
    public void bridge$setInvisible(final boolean invisible) {
        this.shadow$setInvisible(invisible);
        if (invisible) {
            final CompoundNBT spongeData = this.data$getSpongeData();
            spongeData.putBoolean(Constants.Sponge.Entity.IS_INVISIBLE, true);
        } else {
            if (this.data$hasSpongeData()) {
                this.data$getSpongeData().remove(Constants.Sponge.Entity.IS_INVISIBLE);
            }
        }
    }

    @Override
    public boolean bridge$isVanished() {
        return this.impl$isVanished;
    }

    @Override
    public void bridge$setVanished(final boolean vanished) {
        this.impl$isVanished = vanished;
        this.impl$pendingVisibilityUpdate = true;
        this.impl$visibilityTicks = 20;
        if (vanished) {
            final CompoundNBT spongeData = this.data$getSpongeData();
            spongeData.putBoolean(Constants.Sponge.Entity.IS_VANISHED, true);
        } else {
            if (this.data$hasSpongeData()) {
                final CompoundNBT spongeData = this.data$getSpongeData();
                spongeData.remove(Constants.Sponge.Entity.IS_VANISHED);
                spongeData.remove(Constants.Sponge.Entity.VANISH_UNCOLLIDEABLE);
                spongeData.remove(Constants.Sponge.Entity.VANISH_UNTARGETABLE);
            }
        }
    }

    @Override
    public boolean bridge$isUncollideable() {
        return this.impl$collision;
    }

    @Override
    public void bridge$setUncollideable(final boolean prevents) {
        this.impl$collision = prevents;
    }

    @Override
    public boolean bridge$isUntargetable() {
        return this.impl$untargetable;
    }

    @Override
    public void bridge$setUntargetable(final boolean untargetable) {
        this.impl$untargetable = untargetable;
    }

    @Override
    public Timing bridge$getTimingsHandler() {
        return ((EntityTypeBridge) this.shadow$getType()).bridge$getTimings();
    }

    @Override
    public void bridge$setInvulnerable(final boolean value) {
        this.impl$invulnerable = value;
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
    public CommandSource bridge$getCommandSource(final Cause cause) {
        return this.shadow$createCommandSourceStack();
    }

    @Override
    public void bridge$setTransform(final Transform transform) {
        this.shadow$setPos(transform.getPosition().getX(), transform.getPosition().getY(), transform.getPosition().getZ());
        this.shadow$setRot((float) transform.getYaw(), (float) transform.getPitch());
    }

    @Redirect(method = "findDimensionEntryPoint", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getExitPortal"
                    + "(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Z)Ljava/util/Optional;"))
    private Optional<TeleportationRepositioner.Result> impl$redirectGetExitPortal(
            final Entity thisEntity,
            final net.minecraft.world.server.ServerWorld targetWorld,
            final BlockPos targetPosition,
            final boolean targetIsNether) {
        try {
            return this.shadow$getExitPortal(targetWorld, targetPosition, targetIsNether);
        } finally {
            // Reset for the next attempt.
            this.impl$dontCreateExitPortal = false;
        }
    }

    @Override
    public Entity bridge$portalRepositioning(final boolean createEndPlatform,
            final net.minecraft.world.server.ServerWorld serverworld,
            final net.minecraft.world.server.ServerWorld targetWorld,
            final PortalInfo portalinfo) {
        serverworld.getProfiler().popPush("reloading");
        final Entity entity = this.shadow$getType().create(targetWorld);
        if (entity != null) {
            entity.restoreFrom((Entity) (Object) this);
            entity.moveTo(portalinfo.pos.x, portalinfo.pos.y, portalinfo.pos.z, portalinfo.yRot, entity.xRot);
            entity.setDeltaMovement(portalinfo.speed);
            targetWorld.addFromAnotherDimension(entity);
            if (createEndPlatform && targetWorld.dimension() == World.END) {
                net.minecraft.world.server.ServerWorld.makeObsidianPlatform(targetWorld);
            }
        }
        return entity;
    }

    @Override
    public void bridge$postPortalForceChangeTasks(final Entity entity, final net.minecraft.world.server.ServerWorld targetWorld,
            final boolean isVanilla) {
        this.shadow$removeAfterChangingDimensions();
        this.level.getProfiler().pop();
        ((net.minecraft.world.server.ServerWorld) this.level).resetEmptyTime();
        targetWorld.resetEmptyTime();
        this.level.getProfiler().pop();
    }

    /**
     * This is effectively an overwrite of changeDimension: required due to
     * Forge changing the signature.
     *
     * @author dualspiral - 18th December 2020 - 1.16.4
     *
     * @param originalDestinationWorld The original target world
     * @param platformTeleporter performs additional teleportation logic, as required.
     * @return The {@link Entity} that is either this one, or replaces this one
     */
    @SuppressWarnings("ConstantConditions")
    @org.checkerframework.checker.nullness.qual.Nullable
    public Entity bridge$changeDimension(final net.minecraft.world.server.ServerWorld originalDestinationWorld, final PlatformTeleporter platformTeleporter) {
        // Sponge Start
        if (this.shadow$getCommandSenderWorld().isClientSide || this.removed) {
            return null;
        }

        final boolean isPlayer = ((Object) this) instanceof ServerPlayerEntity;

        final TeleportContext contextToSwitchTo =
                EntityPhase.State.PORTAL_DIMENSION_CHANGE.createPhaseContext(PhaseTracker.getInstance()).worldChange();
        if (isPlayer) {
            contextToSwitchTo.player();
        }
        try (final TeleportContext context = contextToSwitchTo.buildAndSwitch();
                final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(this);
            frame.pushCause(platformTeleporter.getPortalType());
            frame.addContext(EventContextKeys.MOVEMENT_TYPE, platformTeleporter.getMovementType());

            this.impl$originalDestinationWorld = new WeakReference<>((ServerWorld) originalDestinationWorld);

            final ChangeEntityWorldEvent.Pre preChangeEvent =
                    PlatformHooks.INSTANCE.getEventHooks().callChangeEntityWorldEventPre((Entity) (Object) this, originalDestinationWorld);
            if (preChangeEvent.isCancelled()) {
                return null;
            }
            final net.minecraft.world.server.ServerWorld targetWorld = (net.minecraft.world.server.ServerWorld) preChangeEvent.getDestinationWorld();
            final Vector3d currentPosition = VecHelper.toVector3d(this.shadow$position());

            // If a player, set the fact they are changing dimensions
            this.bridge$setPlayerChangingDimensions();

            final net.minecraft.world.server.ServerWorld serverworld = (net.minecraft.world.server.ServerWorld) this.level;
            final RegistryKey<World> registrykey = serverworld.dimension();
            if (isPlayer && registrykey == World.END && targetWorld.dimension() == World.OVERWORLD && platformTeleporter.isVanilla()) { // avoids modded dimensions
                return ((ServerPlayerEntityBridge) this).bridge$performGameWinLogic();
            } else {
                // Sponge Start: Redirect the find portal call to the teleporter.

                // If this is vanilla, this will house our Reposition Event and return an appropriate
                // portal info
                final PortalInfo portalinfo = platformTeleporter.getPortalInfo((Entity) (Object) this, serverworld, targetWorld, currentPosition);
                // Sponge End
                if (portalinfo != null) {
                    // Only start teleporting if we have somewhere to go.
                    this.bridge$playerPrepareForPortalTeleport(serverworld, targetWorld);
                    try {
                        // Sponge Start: wrap the teleportation logic within a function to allow for modification
                        // of the teleporter
                        final Vector3d originalDestination = new Vector3d(portalinfo.pos.x, portalinfo.pos.y, portalinfo.pos.z);
                        final Entity transportedEntity =
                                platformTeleporter.performTeleport((Entity) (Object) this, serverworld, targetWorld, this.xRot,
                                        createEndPlatform -> this
                                                .bridge$portalRepositioning(createEndPlatform, serverworld, targetWorld, portalinfo));
                        // Make sure the right object was returned
                        this.bridge$validateEntityAfterTeleport(transportedEntity, platformTeleporter);

                        // If we need to reposition: well... reposition.
                        // Downside: portals won't come with us, but with how it's implemented in Forge,
                        // not sure how we'd do this.
                        //
                        // If this is vanilla, we've already fired and dealt with the event
                        if (transportedEntity != null && this.impl$shouldFireRepositionEvent) {
                            final Vector3d destination = VecHelper.toVector3d(this.shadow$position());
                            final ChangeEntityWorldEvent.Reposition reposition = SpongeEventFactory.createChangeEntityWorldEventReposition(
                                    PhaseTracker.getCauseStackManager().getCurrentCause(),
                                    (org.spongepowered.api.entity.Entity) transportedEntity,
                                    (org.spongepowered.api.world.server.ServerWorld) serverworld,
                                    currentPosition,
                                    destination,
                                    (org.spongepowered.api.world.server.ServerWorld) originalDestinationWorld,
                                    originalDestination,
                                    (org.spongepowered.api.world.server.ServerWorld) targetWorld
                            );
                            final Vector3d finalPosition;
                            if (reposition.isCancelled()) {
                                // send them back to the original destination
                                finalPosition = originalDestination;
                            } else if (reposition.getDestinationPosition() != destination) {
                                finalPosition = reposition.getDestinationPosition();
                            } else {
                                finalPosition = null;
                            }

                            if (finalPosition != null) {
                                // TODO: Rollback captures during phase - anything generated needs to vanish here
                                // Issue chunk ticket of type Portal, even if a portal isn't being created here.
                                final BlockPos ticketPos = VecHelper.toBlockPos(finalPosition);
                                targetWorld.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(ticketPos), 3, ticketPos);

                                this.shadow$absMoveTo(finalPosition.getX(), finalPosition.getY(), finalPosition.getZ());
                            }
                        }

                        this.bridge$postPortalForceChangeTasks(transportedEntity, targetWorld, platformTeleporter.getPortalType() instanceof NetherPortalType);
                        // Call post event
                        PlatformHooks.INSTANCE.getEventHooks().callChangeEntityWorldEventPost((Entity) (Object) this, serverworld, targetWorld);
                    } catch (final RuntimeException e) {
                        // nothing throws a checked exception in this block, but we want to catch unchecked stuff and try to recover
                        // just in case a mod does something less than clever.
                        if ((Object) this instanceof ServerPlayerEntity) {
                            this.bridge$postPortalForceChangeTasks((Entity) (Object) this, (net.minecraft.world.server.ServerWorld) this.level, false);
                        }
                        throw e;
                    }
                    // Sponge End
                } else {
                    // Didn't work out.
                    return null;
                }
            }

            return (Entity) (Object) this;
        } finally {
            // Reset for the next attempt.
            this.impl$shouldFireRepositionEvent = true;
            this.impl$originalDestinationWorld = null;
        }
    }

    /**
     * This is from Entity#findDimensionEntryPoint, for determning the destination position before
     * a portal is created (lambda in the return statement after getExitPortal)
     *
     * This is only fired if a portal exists, thus the blockstate checks are okay.
     */
    private Vector3d impl$getEntityPositionInPotentialExitPortal(final TeleportationRepositioner.Result result) {
        final BlockState blockstate = this.level.getBlockState(this.portalEntrancePos);
        final Direction.Axis direction$axis;
        final net.minecraft.util.math.vector.Vector3d vector3d;
        if (blockstate.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            direction$axis = blockstate.getValue(BlockStateProperties.HORIZONTAL_AXIS);
            final TeleportationRepositioner.Result teleportationrepositioner$result = TeleportationRepositioner.getLargestRectangleAround(this.portalEntrancePos, direction$axis, 21, Direction.Axis.Y, 21, (p_242276_2_) -> {
                return this.level.getBlockState(p_242276_2_) == blockstate;
            });
            vector3d = this.shadow$getRelativePortalPosition(direction$axis, teleportationrepositioner$result);
        } else {
            vector3d = new net.minecraft.util.math.vector.Vector3d(0.5D, 0.0D, 0.0D);
        }
        return VecHelper.toVector3d(vector3d);
    }

    @Inject(method = "getExitPortal", cancellable = true, at = @At("RETURN"))
    private void impl$fireRepositionEventWhenFindingAPortal(final net.minecraft.world.server.ServerWorld targetWorld,
            final BlockPos targetPosition,
            final boolean targetIsNether,
            final CallbackInfoReturnable<Optional<TeleportationRepositioner.Result>> cir) {
        if (this.impl$shouldFireRepositionEvent) {
            // This exists as we're injecting at return
            final Optional<TeleportationRepositioner.Result> result = cir.getReturnValue();
            final Vector3d destinationPosition = result.map(this::impl$getEntityPositionInPotentialExitPortal)
                    .orElseGet(() -> VecHelper.toVector3d(targetPosition));
            final ServerWorld originalDestinationWorld;
            if (this.impl$originalDestinationWorld != null && this.impl$originalDestinationWorld.get() != null) {
                originalDestinationWorld = this.impl$originalDestinationWorld.get();
            } else {
                originalDestinationWorld = (ServerWorld) targetWorld;
            }

            final ChangeEntityWorldEvent.Reposition reposition = this.bridge$fireRepositionEvent(
                    originalDestinationWorld,
                    (ServerWorld) targetWorld,
                    destinationPosition
            );
            if (!reposition.isCancelled() && reposition.getDestinationPosition() != destinationPosition) {
                this.impl$dontCreateExitPortal = true;
                // Something changed so we want to re-rerun this loop.
                // TODO: There is an open question here about whether we want to force the creation of a portal in this
                //  scenario, or whether we're happy if the repositioning will put someone in a nearby portal.
                cir.setReturnValue(this.shadow$getExitPortal(targetWorld, VecHelper.toBlockPos(reposition.getDestinationPosition()), targetIsNether));
            }
        }
    }

    @Override
    public final ChangeEntityWorldEvent.Reposition bridge$fireRepositionEvent(final ServerWorld originalDestinationWorld,
            final ServerWorld targetWorld,
            final Vector3d destinationPosition) {

        this.impl$shouldFireRepositionEvent = false;
        final ChangeEntityWorldEvent.Reposition reposition = SpongeEventFactory.createChangeEntityWorldEventReposition(
                PhaseTracker.getCauseStackManager().getCurrentCause(),
                (org.spongepowered.api.entity.Entity) this,
                (ServerWorld) this.level,
                VecHelper.toVector3d(this.position),
                destinationPosition,
                originalDestinationWorld,
                destinationPosition,
                targetWorld
        );

        SpongeCommon.postEvent(reposition);
        return reposition;
    }

    @Override
    public CompoundNBT data$getCompound() {
        return this.impl$customDataCompound;
    }

    @Override
    public void data$setCompound(final CompoundNBT nbt) {
        this.impl$customDataCompound = nbt;
    }

    @Override
    public NBTDataType data$getNBTDataType() {
        return NBTDataTypes.ENTITY;
    }

    /**
     * @author Zidane
     * @reason This is a catch-all method to ensure MoveEntityEvent is fired with
     *         useful information
     */
    @Overwrite
    public final void teleportToWithTicket(final double x, final double y, final double z) {
        if (this.level instanceof net.minecraft.world.server.ServerWorld) {
            // Sponge start
            final PhaseTracker server = PhaseTracker.SERVER;
            final Vector3d destinationPosition;
            boolean hasMovementContext = true;
            if (ShouldFire.MOVE_ENTITY_EVENT) {
                if (!server.getCurrentContext().containsKey(EventContextKeys.MOVEMENT_TYPE)) {
                    hasMovementContext = false;
                    server.pushCause(SpongeCommon.getActivePlugin());
                    server.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.PLUGIN);
                }

                final MoveEntityEvent event = SpongeEventFactory.createMoveEntityEvent(server.getCurrentCause(),
                        (org.spongepowered.api.entity.Entity) this, VecHelper.toVector3d(this.shadow$position()), new Vector3d(x, y, z),
                        new Vector3d(x, y, z));

                if (!hasMovementContext) {
                    server.popCause();
                    server.removeContext(EventContextKeys.MOVEMENT_TYPE);
                }

                if (SpongeCommon.postEvent(event)) {
                    return;
                }

                destinationPosition = event.getDestinationPosition();
            } else {
                destinationPosition = new Vector3d(x, y, z);
            }
            // Sponge end
            final ChunkPos chunkpos = new ChunkPos(new BlockPos(destinationPosition.getX(), destinationPosition.getY(), destinationPosition.getZ()));
            ((net.minecraft.world.server.ServerWorld)this.level).getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkpos, 0,
                    this.shadow$getId());
            this.level.getChunk(chunkpos.x, chunkpos.z);
            this.shadow$teleportTo(destinationPosition.getX(), destinationPosition.getY(), destinationPosition.getZ());
        }
    }

    @Inject(method = "startRiding(Lnet/minecraft/entity/Entity;Z)Z",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/Entity;vehicle:Lnet/minecraft/entity/Entity;",
            ordinal = 0
        ),
        cancellable = true
    )
    private void impl$onStartRiding(final Entity vehicle, final boolean force,
        final CallbackInfoReturnable<Boolean> ci) {
        if (!this.level.isClientSide && ShouldFire.RIDE_ENTITY_EVENT_MOUNT) {
            PhaseTracker.getCauseStackManager().pushCause(this);
            if (SpongeCommon.postEvent(SpongeEventFactory.createRideEntityEventMount(PhaseTracker.getCauseStackManager().getCurrentCause(), (org.spongepowered.api.entity.Entity) vehicle))) {
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
        if (this.shadow$getVehicle() != null) {
            if (this.shadow$getVehicle().removed) {
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
        if (!this.world.isClientSide && !SpongeHooks.checkEntitySpeed(((Entity) (Object) this), vec3d.getX(), vec3d.getY(), vec3d.getZ())) {
            ci.cancel();
        }
    }
*/
    @Redirect(method = "lavaHurt",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;hurt(Lnet/minecraft/util/DamageSource;F)Z"
        )
    )
    private boolean impl$createLavaBlockDamageSource(final Entity entity, final DamageSource source, final float damage) {
        if (this.level.isClientSide) { // Short circuit
            return entity.hurt(source, damage);
        }
        try {
            final AxisAlignedBB bb = this.shadow$getBoundingBox().inflate(-0.10000000149011612D, -0.4000000059604645D, -0.10000000149011612D);
            final ServerLocation location = DamageEventHandler.findFirstMatchingBlock((Entity) (Object) this, bb, block ->
                block.getMaterial() == Material.LAVA);
            final MinecraftBlockDamageSource lava = new MinecraftBlockDamageSource("lava", location);
            ((DamageSourceBridge) (Object) lava).bridge$setLava(); // Bridge to bypass issue with using accessor mixins within mixins
            return entity.hurt(DamageSource.LAVA, damage);
        } finally {
            // Since "source" is already the DamageSource.LAVA object, we can simply re-use it here.
            ((DamageSourceBridge) source).bridge$setLava();
        }

    }

    /*
    @SuppressWarnings("ConstantConditions")
    @Inject(method = "setPosition",
        at = @At("HEAD"))
    private void impl$capturePlayerPosition(final double x, final double y, final double z, final CallbackInfo ci) {
        if ((Entity) (Object) this instanceof ServerPlayerEntity) {
            final ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            if (player.connection != null) {
                ((ServerPlayNetHandlerBridge) player.connection).bridge$captureCurrentPlayerPosition();
            }
        }
    }


    @SuppressWarnings({"ConstantConditions", "RedundantCast"})
    @Inject(method = "tick",
        at = @At("RETURN"))
    private void impl$updateVanishState(final CallbackInfo callbackInfo) {
        if (this.impl$pendingVisibilityUpdate && !this.world.isClientSide) {
            final EntityTrackerAccessor trackerAccessor = ((ChunkManagerAccessor) ((ServerWorld) this.world).getChunkProvider().chunkManager).accessor$getEntityTrackers().get(this.shadow$getEntityId());
            if (trackerAccessor != null && this.impl$visibilityTicks % 4 == 0) {
                if (this.impl$isVanished) {
                    for (final ServerPlayerEntity entityPlayerMP : trackerAccessor.accessor$getTrackingPlayers()) {
                        entityPlayerMP.connection.sendPacket(new SDestroyEntitiesPacket(this.shadow$getEntityId()));
                        if ((Entity) (Object) this instanceof ServerPlayerEntity) {
                            entityPlayerMP.connection.sendPacket(
                                new SPlayerListItemPacket(SPlayerListItemPacket.Action.REMOVE_PLAYER, (ServerPlayerEntity) (Object) this));
                        }
                    }
                } else {
                    this.impl$visibilityTicks = 1;
                    this.impl$pendingVisibilityUpdate = false;
                    for (final ServerPlayerEntity entityPlayerMP : SpongeCommon.getServer().getPlayerList().getPlayers()) {
                        if ((Entity) (Object) this == entityPlayerMP) {
                            continue;
                        }
                        if ((Entity) (Object) this instanceof ServerPlayerEntity) {
                            final IPacket<?> packet = new SPlayerListItemPacket(SPlayerListItemPacket.Action.ADD_PLAYER, (ServerPlayerEntity) (Object) this);
                            entityPlayerMP.connection.sendPacket(packet);
                        }
                        trackerAccessor.accessor$getEntry().sendSpawnPackets(entityPlayerMP.connection::sendPacket);
                    }
                }
            }
            if (this.impl$visibilityTicks > 0) {
                this.impl$visibilityTicks--;
            } else {
                this.impl$pendingVisibilityUpdate = false;
            }
        }
    }

    */

    /**
     * Hooks into vanilla's writeToNBT to call {@link #impl$writeToSpongeCompound}.
     *
     * <p> This makes it easier for other entity mixins to override writeToNBT
     * without having to specify the <code>@Inject</code> annotation. </p>
     *
     * @param compound The compound vanilla writes to (unused because we write to SpongeData)
     * @param ci (Unused) callback info
     */
    @Inject(method = "saveWithoutId", at = @At("HEAD"))
    private void impl$spongeWriteToNBT(final CompoundNBT compound, final CallbackInfoReturnable<CompoundNBT> ci) {
        this.impl$writeToSpongeCompound(this.data$getSpongeData());
    }

/*
    @Override
    public void bridge$setImplVelocity(final Vector3d velocity) {
        this.motion = VecHelper.toVec3d(velocity);
        this.velocityChanged = true;
    }

    @Redirect(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;onEntityWalk(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)V"
        )
    )
    private void impl$onEntityCollideWithBlockThrowEventSponge(final Block block, final net.minecraft.world.World world,
        final BlockPos pos, final Entity entity) {
        // if block can't collide, return
        if (!((BlockBridge) block).bridge$hasCollideLogic()) {
            return;
        }

        if (world.isClientSide) {
            block.onEntityWalk(world, pos, entity);
            return;
        }

        final BlockState state = world.getBlockState(pos);
        if (!SpongeCommonEventFactory.handleCollideBlockEvent(block, world, pos, state, entity, Direction.NONE)) {
            block.onEntityWalk(world, pos, entity);
            this.impl$lastCollidedBlockPos = pos;
        }

    }

    @Redirect(method = "doBlockCollisions",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;onEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)V"
        )
    ) // doBlockCollisions
    private void impl$onEntityCollideWithBlockState(final BlockState blockState,
        final net.minecraft.world.World worldIn, final BlockPos pos, final Entity entityIn) {
        // if block can't collide, return
        if (!((BlockBridge) blockState.getBlock()).bridge$hasCollideWithStateLogic()) {
            return;
        }

        if (world.isClientSide) {
            blockState.onEntityCollision(world, pos, entityIn);
            return;
        }

        if (!SpongeCommonEventFactory.handleCollideBlockEvent(blockState.getBlock(), world, pos, blockState, entityIn, Direction.NONE)) {
            blockState.onEntityCollision(world, pos, entityIn);
            this.impl$lastCollidedBlockPos = pos;
        }

    }

    @Redirect(method = "updateFallState",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/Block;onFallenUpon(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;F)V"))
    private void impl$onBlockFallenUpon(final Block block, final net.minecraft.world.World world, final BlockPos pos,
        final Entity entity, final float fallDistance) {
        if (world.isClientSide) {
            block.onFallenUpon(world, pos, entity, fallDistance);
            return;
        }

        final BlockState state = world.getBlockState(pos);
        if (!SpongeCommonEventFactory.handleCollideBlockEvent(block, world, pos, state, entity, Direction.UP)) {
            block.onFallenUpon(world, pos, entity, fallDistance);
            this.impl$lastCollidedBlockPos = pos;
        }

    }
*/

    /**
         * @author gabizou - January 4th, 2016
         * @reason gabizou - January 27th, 2016 - Rewrite to a redirect
         *     <p>
         *     This prevents sounds from being sent to the server by entities that are vanished
         *//*

    @Redirect(method = "playSound",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;isSilent()Z"))
    private boolean impl$checkIsSilentOrInvis(final Entity entity) {
        return entity.isSilent() || this.impl$isVanished;
    }

    @Redirect(method = "applyEntityCollision",
        at = @At(value = "FIELD",
            target = "Lnet/minecraft/entity/Entity;noClip:Z",
            opcode = Opcodes.GETFIELD))
    private boolean impl$applyEntityCollisionCheckVanish(final Entity entity) {
        return entity.noClip || ((VanishableBridge) entity).bridge$isVanished();
    }

    @Redirect(method = "doWaterSplashEffect",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particles/IParticleData;DDDDDD)V"))
    private void impl$spawnParticle(final net.minecraft.world.World world, final IParticleData particleTypes,
        final double xCoord, final double yCoord, final double zCoord,
        final double xOffset, final double yOffset, final double zOffset) {
        if (!this.impl$isVanished) {
            this.world.addParticle(particleTypes, xCoord, yCoord, zCoord, xOffset, yOffset, zOffset);
        }
    }

    @Redirect(method = "createRunningParticles",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particles/IParticleData;DDDDDD)V"))
    private void impl$runningSpawnParticle(final net.minecraft.world.World world, final IParticleData particleTypes,
        final double xCoord, final double yCoord, final double zCoord,
        final double xOffset, final double yOffset, final double zOffset) {
        if (!this.impl$isVanished) {
            this.world.addParticle(particleTypes, xCoord, yCoord, zCoord, xOffset, yOffset, zOffset);
        }
    }
    */

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
/*
    @javax.annotation.Nullable
    @Overwrite
    @Nullable
    public ItemEntity entityDropItem(final ItemStack stack, final float offsetY) {
        // Sponge Start
        // Gotta stick with the client side handling things
        if (this.world.isClientSide) {
            // Sponge End - resume normal client code. Server side we will handle it elsewhere
            if (stack.isEmpty()) {
                return null;
            } else {
                final ItemEntity entityitem = new ItemEntity(this.world, this.posX, this.posY + (double) offsetY, this.posZ, stack);
                entityitem.setDefaultPickupDelay();
                this.world.addEntity(entityitem);
                return entityitem;
            }
        }
        // Sponge - Redirect server sided code to handle through the PhaseTracker
        return EntityUtil.entityOnDropItem((Entity) (Object) this, stack, offsetY, ((Entity) (Object) this).posX, ((Entity) (Object) this).posZ);
    }

    @Nullable
    @Override
    public BlockPos bridge$getLastCollidedBlockPos() {
        return this.impl$lastCollidedBlockPos;
    }
*/

/*
    @Redirect(method = "setFire",
        at = @At(value = "FIELD",
            target = "Lnet/minecraft/entity/Entity;fire:I",
            opcode = Opcodes.PUTFIELD)
    )
    private void impl$ThrowIgniteEventForFire(final Entity entity, final int ticks) {
        if (((WorldBridge) this.world).bridge$isFake() || !ShouldFire.IGNITE_ENTITY_EVENT) {
            this.fire = ticks; // Vanilla functionality
            return;
        }
        if (this.fire < 1 && !this.impl$isImmuneToFireForIgniteEvent()) {
            try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {

                frame.pushCause(((org.spongepowered.api.entity.Entity) this).getLocation().getWorld());
                final IgniteEntityEvent event = SpongeEventFactory.
                    createIgniteEntityEvent(frame.getCurrentCause(), ticks, ticks, (org.spongepowered.api.entity.Entity) this);

                if (SpongeCommon.postEvent(event)) {
                    this.fire = 0;
                    return; // set fire ticks to 0
                }
                this.fire = event.getFireTicks();
            }
        }
    }

    */

    @Redirect(method = "getEncodeId", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;canSerialize()Z"))
    private boolean impl$respectTransientFlag(final EntityType entityType) {
        if (!entityType.canSerialize()) {
            return false;
        }

        return !this.impl$transient;
    }

    /*
    @Redirect(method = "onStruckByLightning",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    private boolean impl$ThrowDamageEventWithLightingSource(
        final Entity entity, final DamageSource source, final float damage, final LightningBoltEntity lightningBolt) {
        if (!this.world.isClientSide) {
            return entity.attackEntityFrom(source, damage);
        }
        try {
            final EntityDamageSource lightning = new EntityDamageSource("lightningBolt", lightningBolt);
            ((DamageSourceBridge) lightning).bridge$setLightningSource();
            return entity.attackEntityFrom(DamageSource.LIGHTNING_BOLT, damage);
        } finally {
            ((DamageSourceBridge) source).bridge$setLightningSource();
        }
    }

    @Inject(method = "remove()V",
        at = @At(value = "RETURN"))
    private void impl$createDestructionEventOnDeath(final CallbackInfo ci) {
        if (ShouldFire.DESTRUCT_ENTITY_EVENT
            && !((WorldBridge) this.world).bridge$isFake()
            && !((Entity) (Object) this instanceof MobEntity)) {

            this.impl$destructCause = PhaseTracker.getCauseStackManager().getCurrentCause();
        }
    }
*/

    @Inject(method = "getFireImmuneTicks", at = @At(value = "HEAD"), cancellable = true)
    private void impl$getFireImmuneTicks(final CallbackInfoReturnable<Integer> ci) {
        if (this.impl$hasCustomFireImmuneTicks) {
            ci.setReturnValue((int) this.impl$fireImmuneTicks);
        }
    }

    // TODO overrides for ForgeData
    // @Shadow private CompoundNBT customEntityData;
    // @Override CompoundNBT data$getForgeData()
    // @Override CompoundNBT data$getForgeData()
    // @Override CompoundNBT data$hasForgeData()
    // @Override CompoundNBT cleanEmptySpongeData()

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void impl$WriteSpongeDataToCompound(final CompoundNBT compound, final CallbackInfoReturnable<CompoundNBT> ci) {
        if (this.data$hasSpongeData()) {
            final CompoundNBT forgeCompound = compound.getCompound(Constants.Forge.FORGE_DATA);
            // If we are in Forge data is already present
            if (forgeCompound != this.data$getForgeData()) {
                if (forgeCompound.isEmpty()) { // In vanilla this should be an new detached empty compound
                    compound.put(Constants.Forge.FORGE_DATA, forgeCompound);
                }
                // Get our nbt data and write it to the compound
                forgeCompound.put(Constants.Sponge.SPONGE_DATA, this.data$getSpongeData());
            }
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void impl$ReadSpongeDataFromCompound(final CompoundNBT compound, final CallbackInfo ci) {
        // If we are in Forge data is already present
        this.data$setCompound(compound); // For vanilla we set the incoming nbt
        if (this.data$hasSpongeData()) {
            this.impl$readFromSpongeCompound(this.data$getSpongeData());
        } else {
            this.data$setCompound(null); // No data? No need to keep the nbt
        }
    }

    /**
     * Read extra data (SpongeData) from the entity's NBT tag. This is
     * meant to be overridden for each impl based mixin that has to store
     * custom fields based on it's implementation. Examples can include:
     * vanishing booleans, maximum air, maximum boat speeds and modifiers,
     * etc.
     *
     * @param compound The SpongeData compound to read from
     */
    protected void impl$readFromSpongeCompound(final CompoundNBT compound) {
        // Deserialize our data...
        CustomDataHolderBridge.syncTagToCustom(this);

        if (this instanceof GrieferBridge && ((GrieferBridge) this).bridge$isGriefer() && compound.contains(Constants.Sponge.Entity.CAN_GRIEF)) {
            ((GrieferBridge) this).bridge$setCanGrief(compound.getBoolean(Constants.Sponge.Entity.CAN_GRIEF));
        }
        if (compound.contains(Constants.Sponge.Entity.IS_VANISHED, Constants.NBT.TAG_BYTE)) {
            this.bridge$setVanished(compound.getBoolean(Constants.Sponge.Entity.IS_VANISHED));
            this.bridge$setUncollideable(compound.getBoolean(Constants.Sponge.Entity.VANISH_UNCOLLIDEABLE));
            this.bridge$setUntargetable(compound.getBoolean(Constants.Sponge.Entity.VANISH_UNTARGETABLE));
        }
        if (compound.contains(Constants.Sponge.Entity.IS_INVISIBLE, Constants.NBT.TAG_BYTE)) {
            this.bridge$setInvisible(compound.getBoolean(Constants.Sponge.Entity.IS_INVISIBLE));
        }

        this.data$setCompound(null); // For vanilla this will be recreated empty in the next call - for Forge it reuses the existing compound instead
        // ReSync our data (includes failed data)
        CustomDataHolderBridge.syncCustomToTag(this);
    }

    /**
     * Write extra data (SpongeData) to the entity's NBT tag. This is
     * meant to be overridden for each impl based mixin that has to store
     * custom fields based on it's implementation. Examples can include:
     * vanishing booleans, maximum air, maximum boat speeds and modifiers,
     * etc.
     *
     * @param compound The SpongeData compound to write to
     */
    protected void impl$writeToSpongeCompound(final CompoundNBT compound) {
        CustomDataHolderBridge.syncCustomToTag(this);

        if (this instanceof GrieferBridge && ((GrieferBridge) this).bridge$isGriefer() && ((GrieferBridge) this).bridge$canGrief()) {
            compound.putBoolean(Constants.Sponge.Entity.CAN_GRIEF, true);
        }
        if (this.bridge$isVanished()) {
            compound.putBoolean(Constants.Sponge.Entity.IS_VANISHED, true);
            compound.putBoolean(Constants.Sponge.Entity.VANISH_UNCOLLIDEABLE, this.bridge$isUncollideable());
            compound.putBoolean(Constants.Sponge.Entity.VANISH_UNTARGETABLE, this.bridge$isUntargetable());
        }
        if (this.shadow$isInvisible()) {
            compound.putBoolean(Constants.Sponge.Entity.IS_INVISIBLE, true);
        }
    }

    /**
     * Overridden method for Players to determine whether this entity is immune to fire
     * such that {@link IgniteEntityEvent}s are not needed to be thrown as they cannot
     * take fire damage, nor do they light on fire.
     *
     * @return True if this entity is immune to fire.
     */
    protected boolean impl$canCallIgniteEntityEvent() {
        return false;
    }
}
