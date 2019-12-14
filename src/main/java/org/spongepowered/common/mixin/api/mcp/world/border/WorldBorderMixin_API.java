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
package org.spongepowered.common.mixin.api.mcp.world.border;

import org.spongepowered.api.world.ChunkPreGenerate;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldBorder;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.world.pregen.SpongeChunkPreGenerateTask;
import org.spongepowered.math.vector.Vector3d;

@Mixin(net.minecraft.world.border.WorldBorder.class)
@Implements(@Interface(iface = WorldBorder.class, prefix = "apiBorder$"))
public abstract class WorldBorderMixin_API implements WorldBorder {

    @Shadow public abstract double getCenterX();
    @Shadow public abstract double getCenterZ();
    @Shadow public abstract double getTargetSize();
    @Shadow public abstract void setTransition(double newSize);
    @Shadow public abstract void setTransition(double oldSize, double newSize, long time);
    @Shadow public abstract long getTimeUntilTarget();
    @Shadow public abstract double getDamageBuffer();
    @Shadow public abstract void setDamageBuffer(double buffer);
    @Shadow public abstract void shadow$setCenter(double x, double z);
    @Shadow public abstract double shadow$getDamageAmount();
    @Shadow public abstract void shadow$setDamageAmount(double amount);
    @Shadow public abstract int shadow$getWarningTime();
    @Shadow public abstract void shadow$setWarningTime(int time);
    @Shadow public abstract int shadow$getWarningDistance();
    @Shadow public abstract void shadow$setWarningDistance(int distance);
    @Shadow public abstract double shadow$getDiameter();

    @Intrinsic
    public int apiBorder$getWarningTime() {
        return this.shadow$getWarningTime();
    }

    @Intrinsic
    public void apiBorder$setWarningTime(final int time) {
        this.shadow$setWarningTime(time);
    }

    @Intrinsic
    public int apiBorder$getWarningDistance() {
        return this.shadow$getWarningDistance();
    }

    @Intrinsic
    public void apiBorder$setWarningDistance(final int distance) {
        this.shadow$setWarningDistance(distance);
    }

    @Override
    public double getNewDiameter() {
        return this.getTargetSize();
    }

    @Intrinsic
    public double apiBorder$getDiameter() {
        return this.shadow$getDiameter();
    }

    @Override
    public void setDiameter(final double diameter) {
        this.setTransition(diameter);
    }

    @Override
    public void setDiameter(final double diameter, final long time) {
        this.setTransition(this.getDiameter(), diameter, time);
    }

    @Override
    public void setDiameter(final double startDiameter, final double endDiameter, final long time) {
        this.setTransition(startDiameter, endDiameter, time);
    }

    @Override
    public long getTimeRemaining() {
        return this.getTimeUntilTarget();
    }

    @Override
    public Vector3d getCenter() {
        return new Vector3d(this.getCenterX(), 0, this.getCenterZ());
    }

    @Intrinsic
    public void apiBorder$setCenter(final double x, final double z) {
        this.shadow$setCenter(x, z);
    }

    @Override
    public double getDamageThreshold() {
        return this.getDamageBuffer();
    }

    @Override
    public void setDamageThreshold(final double distance) {
        this.setDamageBuffer(distance);
    }

    @Intrinsic
    public double apiBorder$getDamageAmount() {
        return this.shadow$getDamageAmount();
    }

    @Intrinsic
    public void apiBorder$setDamageAmount(final double damage) {
        this.shadow$setDamageAmount(damage);
    }

    @Override
    public ChunkPreGenerate.Builder newChunkPreGenerate(final World world) {
        return new SpongeChunkPreGenerateTask.Builder(world, this.getCenter(), this.getNewDiameter());
    }
}
