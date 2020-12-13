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
import net.minecraft.entity.item.EyeOfEnderEntity;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.api.entity.projectile.EyeOfEnder;
import org.spongepowered.api.projectile.source.ProjectileSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.entity.projectile.ProjectileSourceSerializer;
import org.spongepowered.common.bridge.LocationTargetingBridge;
import org.spongepowered.common.entity.projectile.UnknownProjectileSource;
import org.spongepowered.common.mixin.core.entity.EntityMixin;
import org.spongepowered.math.vector.Vector3d;

@Mixin(EyeOfEnderEntity.class)
public abstract class EyeOfEnderEntityMixin extends EntityMixin implements LocationTargetingBridge {

    @Shadow private double targetX;
    @Shadow private double targetY;
    @Shadow private double targetZ;
    private ProjectileSource projectileSource = UnknownProjectileSource.UNKNOWN;

    @Override
    public void impl$readFromSpongeCompound(CompoundNBT compound) {
        super.impl$readFromSpongeCompound(compound);
        ProjectileSourceSerializer.readSourceFromNbt(compound, (EyeOfEnder) this);
    }

    @Override
    public void impl$writeToSpongeCompound(CompoundNBT compound) {
        super.impl$writeToSpongeCompound(compound);
        ProjectileSourceSerializer.writeSourceToNbt(compound, this.projectileSource, (Entity) null);
    }

    @Override
    public Vector3d bridge$getTargetedPosition() {
        return new Vector3d(this.targetX, this.targetY, this.targetZ);
    }

    @Override
    public void bridge$setTargetedPosition(Vector3d vec) {
        this.targetX = vec.getX();
        this.targetY = vec.getY();
        this.targetZ = vec.getZ();
    }

}
