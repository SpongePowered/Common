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
package org.spongepowered.common.block;

import org.spongepowered.api.block.transaction.ScheduleUpdateTicket;
import org.spongepowered.api.scheduler.TaskPriority;
import org.spongepowered.api.world.LocatableBlock;

import java.time.Duration;

public final class SpongeScheduleUpdateTicket<T> implements ScheduleUpdateTicket<T> {

    private final LocatableBlock block;
    private final T target;
    private final Duration delay;
    private final TaskPriority priority;
    private boolean valid = true;

    public SpongeScheduleUpdateTicket(final LocatableBlock block, final T target, final Duration delay, final TaskPriority priority) {
        this.block = block;
        this.target = target;
        this.delay = delay;
        this.priority = priority;
    }

    @Override
    public LocatableBlock block() {
        return this.block;
    }

    @Override
    public T target() {
        return this.target;
    }

    @Override
    public Duration delay() {
        return this.delay;
    }

    @Override
    public TaskPriority priority() {
        return this.priority;
    }

    @Override
    public boolean valid() {
        return this.valid;
    }

    @Override
    public void setValid(final boolean valid) {
        this.valid = valid;
    }
}
