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
package org.spongepowered.common.event.tracking.phase.packet.inventory;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.entity.SpawnType;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.GameTransaction;
import org.spongepowered.common.event.tracking.context.transaction.world.SpawnEntityTransaction;
import org.spongepowered.common.event.tracking.phase.packet.PacketState;

import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class CloseWindowState extends PacketState<CloseWindowContext> {

    @Override
    protected CloseWindowContext createNewContext(final PhaseTracker tracker) {
        return new CloseWindowContext(this, tracker);
    }

    @Override
    public Supplier<SpawnType> getSpawnTypeForTransaction(final CloseWindowContext context, final Entity entityToSpawn) {
        return SpawnTypes.DROPPED_ITEM;
    }

    @Override
    public SpawnEntityEvent createSpawnEvent(final CloseWindowContext context, final @Nullable GameTransaction<@NonNull ?> parent,
            final ImmutableList<Tuple<Entity, SpawnEntityTransaction.DummySnapshot>> collect, final Cause currentCause) {
        return SpongeEventFactory.createDropItemEventClose(currentCause,
                collect.stream()
                        .map(t -> (org.spongepowered.api.entity.Entity) t.first())
                        .collect(Collectors.toList())
        );
    }
}
