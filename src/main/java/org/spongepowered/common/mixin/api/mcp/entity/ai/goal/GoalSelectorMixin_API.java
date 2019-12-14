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
package org.spongepowered.common.mixin.api.mcp.entity.ai.goal;

import com.google.common.collect.ImmutableList;
import org.spongepowered.api.entity.ai.GoalExecutor;
import org.spongepowered.api.entity.ai.GoalExecutorType;
import org.spongepowered.api.entity.ai.goal.Goal;
import org.spongepowered.api.entity.ai.goal.GoalType;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.entity.ai.GoalSelectorBridge;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.ai.goal.GoalSelector;

@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin_API<O extends Agent> implements GoalExecutor<O> {

    @Shadow @Final private Set<GoalSelector.EntityAITaskEntry> taskEntries;
    @Shadow @Final private Set<GoalSelector.EntityAITaskEntry> executingTaskEntries;

    @Shadow public abstract void shadow$addTask(int priority, net.minecraft.entity.ai.goal.Goal task);
    @Shadow public abstract void shadow$removeTask(net.minecraft.entity.ai.goal.Goal task);

    @SuppressWarnings("unchecked")
    @Override
    public O getOwner() {
        return (O) ((GoalSelectorBridge) this).bridge$getOwner();
    }

    @Override
    public GoalExecutorType getType() {
        return ((GoalSelectorBridge) this).bridge$getType();
    }

    @Override
    public GoalExecutor<O> addGoal(final int priority, final Goal<? extends O> task) {
        this.shadow$addTask(priority, (net.minecraft.entity.ai.goal.Goal) task);
        return this;
    }

    @Override
    public GoalExecutor<O> removeGoal(final Goal<? extends O> goal) {
        this.shadow$removeTask((net.minecraft.entity.ai.goal.Goal) goal);
        return  this;
    }

    @Override
    public GoalExecutor<O> removeGoals(final GoalType type) {
        final Iterator<GoalSelector.EntityAITaskEntry> iterator = this.taskEntries.iterator();

        while (iterator.hasNext()) {
            final GoalSelector.EntityAITaskEntry entityaitaskentry = iterator.next();
            final net.minecraft.entity.ai.goal.Goal otherAiBase = entityaitaskentry.action;
            final Goal<?> otherTask = (Goal<?>) otherAiBase;

            if (otherTask.getType().equals(type)) {
                if (this.executingTaskEntries.contains(entityaitaskentry)) {
                    otherAiBase.resetTask();
                    this.executingTaskEntries.remove(entityaitaskentry);
                }

                iterator.remove();
            }
        }

        return this;
    }

    @Override
    public List<? super Goal<? extends O>> getTasksByType(final GoalType type) {
        final ImmutableList.Builder<Goal<?>> tasks = ImmutableList.builder();

        for (final GoalSelector.EntityAITaskEntry entry : this.taskEntries) {
            final Goal<?> task = (Goal<?>) entry.action;

            if (task.getType().equals(type)) {
                tasks.add(task);
            }
        }

        return tasks.build();
    }

    @Override
    public List<? super Goal<? extends O>> getTasks() {
        final ImmutableList.Builder<Goal<?>> tasks = ImmutableList.builder();
        for (final Object o : this.taskEntries) {
            final GoalSelector.EntityAITaskEntry entry = (GoalSelector.EntityAITaskEntry) o;

            tasks.add((Goal<?>) entry.action);
        }
        return tasks.build();
    }

    @Override
    public void clear() {
        this.taskEntries.clear();
    }
}
