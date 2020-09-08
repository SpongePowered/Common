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
package org.spongepowered.common.mixin.core.entity.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.EndGatewayTileEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.mixin.core.entity.projectile.ThrowableEntityMixin;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3d;

import javax.annotation.Nullable;

@Mixin(EnderPearlEntity.class)
public abstract class EnderPearlEntityMixin extends ThrowableEntityMixin {

    @Shadow private LivingEntity perlThrower;

    private double impl$damageAmount;

    @ModifyArg(method = "onImpact",
            at = @At(value = "INVOKE",
                target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    private float impl$onAttackEntityFromWithDamage(final float damage) {
        return (float) this.impl$damageAmount;
    }

    @Override
    public void impl$readFromSpongeCompound(final CompoundNBT compound) {
        super.impl$readFromSpongeCompound(compound);
        if (compound.contains(Constants.Sponge.Entity.Projectile.PROJECTILE_DAMAGE_AMOUNT)) {
            this.impl$damageAmount = compound.getDouble(Constants.Sponge.Entity.Projectile.PROJECTILE_DAMAGE_AMOUNT);
        }
    }

    @Override
    public void impl$writeToSpongeCompound(final CompoundNBT compound) {
        super.impl$writeToSpongeCompound(compound);
        compound.putDouble(Constants.Sponge.Entity.Projectile.PROJECTILE_DAMAGE_AMOUNT, this.impl$damageAmount);
    }

    @Redirect(method = "onImpact", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/EndGatewayTileEntity;teleportEntity(Lnet/minecraft/entity/Entity;)V"))
    private void impl$createCauseFrameForGatewayTeleport(EndGatewayTileEntity endGatewayTileEntity, Entity entityIn) {
        if (this.shadow$getEntityWorld().isRemote) {
            return;
        }

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(entityIn);
            frame.pushCause(this);
            endGatewayTileEntity.teleportEntity(entityIn);
        }
    }

    @Inject(
            method = "onImpact",
            at = @At(value = "RETURN", ordinal = 2, shift = At.Shift.BY, by = 2),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    private void impl$callMoveEntityEventForThrower(RayTraceResult result, CallbackInfo ci, LivingEntity entity) {
        if (this.shadow$getEntityWorld().isRemote) {
            return;
        }
        
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(this.perlThrower);
            frame.pushCause(this);
            frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.ENDER_PEARL);

            final MoveEntityEvent event = SpongeEventFactory.createMoveEntityEvent(frame.getCurrentCause(),
                    (org.spongepowered.api.entity.Entity) entity, VecHelper.toVector3d(entity.getPositionVector()),
                    VecHelper.toVector3d(this.shadow$getPositionVector()), VecHelper.toVector3d(this.shadow$getPositionVector()));
            if (SpongeCommon.postEvent(event)) {
                // Eventhough the event is made, the pearl was still created so remove it anyways
                this.shadow$remove();
                return;
            }

            // This seems odd but we move the pearl so that the pearl's logic will move the living entity later in the impact method
            final Vector3d destinationPosition = event.getDestinationPosition();
            this.shadow$setPosition(destinationPosition.getX(), destinationPosition.getY(), destinationPosition.getZ());
        }
    }

    @Override
    @Nullable
    public Entity changeDimension(final DimensionType dimensionIn) {
        final Entity entity = super.changeDimension(dimensionIn);

        if (entity instanceof EnderPearlEntity) {
            // We actually teleported so...
            this.perlThrower = null;
            this.owner = null;
        }

        return entity;
    }
}
