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
package org.spongepowered.common.mixin.core.entity.ai.goal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.api.entity.living.animal.horse.HorseLike;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.entity.DismountTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.entity.EntityBridge;
import org.spongepowered.common.event.tracking.PhaseTracker;

@Mixin(RunAroundLikeCrazyGoal.class)
public abstract class RunAroundLikeCrazyGoalMixin extends GoalMixin {

    @Shadow @Final @Mutable private AbstractHorseEntity horseHost;

    /**
     * @author rexbut - December 16th, 2016
     * @author i509VCB - February 18th, 2020 - 1.14.4
     *
     * @reason - adjusted to support {@link DismountTypes}
     */
    @Overwrite
    public void tick() {
        if (!this.horseHost.isTame() && this.horseHost.getRNG().nextInt(50) == 0) {
            Entity entity = this.horseHost.getPassengers().get(0);

            if (entity == null) {
                return;
            }

            if (entity instanceof PlayerEntity) {
                int i = this.horseHost.getTemper();
                int j = this.horseHost.getMaxTemper();

                if (j > 0 && this.horseHost.getRNG().nextInt(j) < i) {
                    // Sponge start - Fire Tame Entity event
                    try (CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
                        frame.pushCause(entity);
                        if (SpongeCommon.postEvent(SpongeEventFactory.createTameEntityEvent(frame.getCurrentCause(), (HorseLike) this.horseHost))) {
                            return;
                        }
                    }
                    // Sponge end
                    this.horseHost.setTamedBy((PlayerEntity)entity);
                    return;
                }

                this.horseHost.increaseTemper(5);
            }

            // Sponge start - Throw an event before calling entity states
            // this.horseHost.removePassengers(); // Vanilla
            if (((EntityBridge) this.horseHost).bridge$removePassengers(DismountTypes.DERAIL.get())) {
                // Sponge end
                this.horseHost.makeMad();
                this.horseHost.world.setEntityState(this.horseHost, (byte)6);
            }
        }
    }
}
