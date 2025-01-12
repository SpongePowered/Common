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
package org.spongepowered.common.mixin.core.server.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.LookAt;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.RotateEntityEvent;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3d;

import java.util.Set;

@Mixin(TeleportCommand.class)
public abstract class TeleportCommandMixin {

    /**
     * @author Zidane
     * @reason Have the teleport command respect our events
     */
    @Overwrite
    // TODO check if this is still correct - check if we can get rid of the overwrite
    private static void performTeleport(CommandSourceStack source, Entity entityIn, ServerLevel worldIn, double x, double y, double z,
            Set<Relative> relativeList, float yaw, float pitch, @Nullable LookAt facing) {

        double actualX = x;
        double actualY = y;
        double actualZ = z;
        double actualYaw = yaw;
        double actualPitch = pitch;

        if (!(entityIn instanceof ServerPlayer)) {
            actualYaw = Mth.wrapDegrees(yaw);
            actualPitch = Mth.wrapDegrees(pitch);
            actualPitch = Mth.clamp(actualPitch, -90.0F, 90.0F);
        }

        if (worldIn == entityIn.level()) {
            try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
                frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.COMMAND);

                // TODO Should honor the relative list before the event..

                if (ShouldFire.MOVE_ENTITY_EVENT) {
                    final MoveEntityEvent posEvent = SpongeEventFactory.createMoveEntityEvent(frame.currentCause(),
                            (org.spongepowered.api.entity.Entity) entityIn, VecHelper.toVector3d(entityIn.position()),
                            new Vector3d(x, y, z), new Vector3d(x, y, z));

                    if (SpongeCommon.post(posEvent)) {
                        return;
                    }

                    actualX = posEvent.destinationPosition().x();
                    actualY = posEvent.destinationPosition().y();
                    actualZ = posEvent.destinationPosition().z();
                }

                if (ShouldFire.ROTATE_ENTITY_EVENT) {
                    final RotateEntityEvent rotateEvent = SpongeEventFactory.createRotateEntityEvent(frame.currentCause(),
                            (org.spongepowered.api.entity.Entity) entityIn, new Vector3d(actualPitch, actualYaw, 0),
                            new Vector3d(pitch, yaw, 0));

                    SpongeCommon.post(rotateEvent);
                    actualYaw = rotateEvent.isCancelled() ? entityIn.getYRot() : rotateEvent.toRotation().y();
                    actualPitch = rotateEvent.isCancelled() ? entityIn.getXRot() : rotateEvent.toRotation().x();
                }

                if (entityIn instanceof ServerPlayer sp) {

                    ChunkPos chunkpos = new ChunkPos(new BlockPos((int) actualX, (int) actualY, (int) actualZ));
                    worldIn.getChunkSource().addTicketWithRadius(TicketType.FORCED, chunkpos, 1);

                    entityIn.stopRiding();

                    if (sp.isSleeping()) {
                        sp.stopSleepInBed(true, true);
                    }

                    sp.connection.teleport(
                        new PositionMoveRotation(new Vec3( actualX, actualY, actualZ), Vec3.ZERO, (float) actualYaw, (float) actualPitch), relativeList);
                } else {
                    entityIn.moveTo(actualX, actualY, actualZ, (float) actualYaw, (float) actualPitch);
                }

                entityIn.setYHeadRot((float) actualYaw);
            }
        } else {
            if (entityIn instanceof ServerPlayer sp) {
                // To ensure mod code is caught, handling the world change for players happens in teleport
                // Teleport will create a frame but we want to ensure it'll be the command movement type
                // TODO check if this is still correct
                PhaseTracker.getCauseStackManager().addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.COMMAND);
                sp.teleportTo(worldIn, x, y, z, relativeList, yaw, pitch, true);
                PhaseTracker.getCauseStackManager().removeContext(EventContextKeys.MOVEMENT_TYPE);
            } else {
                try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
                    frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.COMMAND);

                    final ServerLevel fromWorld = (ServerLevel) entityIn.getCommandSenderWorld();

                    final ChangeEntityWorldEvent.Pre preEvent = PlatformHooks.INSTANCE.getEventHooks().callChangeEntityWorldEventPre(entityIn,
                            worldIn);
                    if (SpongeCommon.post(preEvent)) {
                        return;
                    }

                    final ChangeEntityWorldEvent.Reposition posEvent =
                            SpongeEventFactory.createChangeEntityWorldEventReposition(frame.currentCause(),
                                    (org.spongepowered.api.entity.Entity) entityIn,
                                    (org.spongepowered.api.world.server.ServerWorld) entityIn.getCommandSenderWorld(),
                                    VecHelper.toVector3d(entityIn.position()), new Vector3d(x, y, z), preEvent.originalDestinationWorld(),
                                    new Vector3d(x, y, z), preEvent.destinationWorld());

                    if (SpongeCommon.post(posEvent)) {
                        return;
                    }

                    entityIn.unRide();
                    final Entity result = entityIn.getType().create(worldIn, EntitySpawnReason.DIMENSION_TRAVEL);
                    if (result == null) {
                        return;
                    }

                    if (ShouldFire.ROTATE_ENTITY_EVENT) {
                        final RotateEntityEvent rotateEvent = SpongeEventFactory.createRotateEntityEvent(frame.currentCause(),
                                (org.spongepowered.api.entity.Entity) entityIn, new Vector3d(entityIn.getXRot(), entityIn.getYRot(), 0),
                                new Vector3d(actualPitch, actualYaw, 0));

                        if (!SpongeCommon.post(rotateEvent)) {
                            actualYaw = Mth.wrapDegrees(rotateEvent.toRotation().y());
                            actualPitch = Mth.wrapDegrees(rotateEvent.toRotation().x());
                            actualPitch = Mth.clamp(actualPitch, -90.0F, 90.0F);
                        } else {
                            actualYaw = entityIn.getYRot();
                            actualPitch = entityIn.getXRot();
                        }
                    }

                    result.restoreFrom(entityIn);
                    result.moveTo(posEvent.destinationPosition().x(), posEvent.destinationPosition().y(),
                            posEvent.destinationPosition().z(), (float) actualYaw, (float) actualPitch);
                    result.setYHeadRot((float) actualYaw);
                    worldIn.addDuringTeleport(result);
                    entityIn.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);

                    Sponge.eventManager().post(SpongeEventFactory.createChangeEntityWorldEventPost(
                            PhaseTracker.getCauseStackManager().currentCause(),
                            (org.spongepowered.api.entity.Entity) result,
                            (ServerWorld) fromWorld,
                            preEvent.originalDestinationWorld(),
                            preEvent.destinationWorld()
                    ));
                }
            }
        }

        if (facing != null) {
            facing.perform(source, entityIn);
        }

        if (!(entityIn instanceof LivingEntity) || !((LivingEntity)entityIn).isFallFlying()) {
            entityIn.setDeltaMovement(entityIn.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
            entityIn.setOnGround(true);
        }

        if (entityIn instanceof PathfinderMob) {
            ((PathfinderMob)entityIn).getNavigation().stop();
        }
    }
}
