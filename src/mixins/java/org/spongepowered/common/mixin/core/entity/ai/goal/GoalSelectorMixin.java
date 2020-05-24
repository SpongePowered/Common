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

import com.google.common.base.MoreObjects;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.ai.goal.GoalExecutor;
import org.spongepowered.api.entity.ai.goal.GoalExecutorType;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.ai.goal.GoalEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.bridge.entity.EntityBridge;
import org.spongepowered.common.bridge.entity.ai.GoalBridge;
import org.spongepowered.common.bridge.entity.ai.GoalSelectorBridge;
import org.spongepowered.common.event.ShouldFire;

import java.util.Set;

import javax.annotation.Nullable;

@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin implements GoalSelectorBridge {

    @Shadow @Final private Set<PrioritizedGoal> goals;
    @Nullable private MobEntity owner;
    @Nullable private GoalExecutorType type;
    private boolean initialized;

    /**
     * @author gabizou - February 1st, 2016
     *
     * Purpose: Rewrites the overwrite to use a redirect when an entry is being added
     * to the task entries. We throw an event for plugins to potentially cancel.
     */
    @Redirect(method = "addGoal", at = @At(value = "INVOKE", target =  "Ljava/util/Set;add(Ljava/lang/Object;)Z", remap = false))
    private boolean onAddEntityTask(final Set<PrioritizedGoal> goals, final Object task, final int priority, final Goal base) {
        ((GoalBridge) base).bridge$setGoalExecutor((GoalExecutor<?>) this);
        if (!ShouldFire.A_I_TASK_EVENT_ADD || this.owner == null || ((EntityBridge) this.owner).bridge$isConstructing()) {
            // Event is fired in bridge$fireConstructors
            return goals.add(new PrioritizedGoal(priority, base));
        }
        final GoalEvent.Add event = SpongeEventFactory.createGoalEventAdd(Sponge.getCauseStackManager().getCurrentCause(), priority, priority,
                (Agent) this.owner, (GoalExecutor<?>) this, (org.spongepowered.api.entity.ai.goal.Goal<?>) base);
        SpongeImpl.postEvent(event);
        if (event.isCancelled()) {
            ((GoalBridge) base).bridge$setGoalExecutor(null);
            return false;
        }
        return goals.add(new PrioritizedGoal(event.getPriority(), base));
    }

    @Override
    public MobEntity bridge$getOwner() {
        return this.owner;
    }

    @Override
    public void bridge$setOwner(final MobEntity owner) {
        this.owner = owner;
    }

    @Override
    public GoalExecutorType bridge$getType() {
        return this.type;
    }

    @Override
    public void bridge$setType(final GoalExecutorType type) {
        this.type = type;
    }

    /**
     * @author Zidane - November 30th, 2015
     * @author Faithcaio - 2020-05-24 update to 1.14
     *
     * @reason Integrate Sponge events into the AI task removal.
     *
     * @param task the base
     */
    @SuppressWarnings({"rawtypes"})
    @Overwrite
    public void removeGoal(final Goal task) {
        this.goals.removeIf(prioritizedGoal -> {
            if (prioritizedGoal.getGoal() == task) {
                if (ShouldFire.A_I_TASK_EVENT_REMOVE && this.owner != null && !((EntityBridge) this.owner).bridge$isConstructing()) {
                    GoalEvent.Remove event = SpongeEventFactory.createGoalEventRemove(Sponge.getCauseStackManager().getCurrentCause(),
                            (Agent) this.owner, (GoalExecutor) this, (org.spongepowered.api.entity.ai.goal.Goal) task, prioritizedGoal.getPriority());
                    SpongeImpl.postEvent(event);
                    if (event.isCancelled()) {
                        return false;
                    }
                }
                if (prioritizedGoal.isRunning()) {
                    prioritizedGoal.resetTask();
                }
                return true;
            }
            return false;
        });
    }

    /**
     * @author Zidane - February 22, 2016
     * @reason Use SpongeAPI's method check instead of exposing mutex bits
     *
     * @param taskEntry1 The task entry to check compatibility
     * @param taskEntry2 The second entry to check compatibility
     * @return Whether the two tasks are compatible or "can run at the same time"
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Overwrite
    // TODO this no longer works
    private boolean areTasksCompatible(final PrioritizedGoal taskEntry1, final PrioritizedGoal taskEntry2) {
        return (((org.spongepowered.api.entity.ai.goal.Goal) taskEntry2.getGoal()).canRunConcurrentWith((org.spongepowered.api.entity.ai.goal.Goal) taskEntry1.getGoal()));
    }

    @Override
    public boolean bridge$initialized() {
        return this.initialized;
    }

    @Override
    public void bridge$setInitialized(final boolean initialized) {
        this.initialized = initialized;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(this.bridge$getOwner())
                .addValue(this.bridge$getType())
                .toString();
    }
}
