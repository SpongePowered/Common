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
package org.spongepowered.common.event.tracking.context.transaction;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.world.server.ServerWorld;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.HarvestEntityEvent;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.util.PrettyPrinter;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class EntityPerformingDropsTransaction extends GameTransaction<HarvestEntityEvent> {

    final Supplier<ServerWorld> worldSupplier;
    final Entity destroyingEntity;
    final CompoundNBT entityTag;
    final Supplier<Optional<DamageSource>> lastAttacker;

    EntityPerformingDropsTransaction(
        final Supplier<ServerWorld> worldSupplier,
        final Entity destroyingEntity, final CompoundNBT entityTag,
        final Supplier<Optional<DamageSource>> lastAttacker
    ) {
        super(TransactionType.ENTITY_DEATH_DROPS);
        this.worldSupplier = worldSupplier;
        this.destroyingEntity = destroyingEntity;
        this.entityTag = entityTag;
        this.lastAttacker = lastAttacker;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Optional<BiConsumer<PhaseContext<@NonNull ?>, CauseStackManager.StackFrame>> getFrameMutator() {
        return Optional.of((context, stackFrame) -> {
            stackFrame.pushCause(this.destroyingEntity);
            this.lastAttacker.get()
                .ifPresent(attacker -> stackFrame.addContext((Supplier) EventContextKeys.LAST_DAMAGE_SOURCE, attacker));
        });
    }

    @Override
    public void addToPrinter(final PrettyPrinter printer) {

    }

    @Override
    public boolean acceptEntityDrops(
        final Entity entity
    ) {
        return this.destroyingEntity == entity;
    }

    @Override
    public boolean isUnbatchable() {
        return true;
    }

    @Override
    public HarvestEntityEvent generateEvent(
        final PhaseContext<@NonNull ?> context, final ImmutableList<GameTransaction<HarvestEntityEvent>> gameTransactions,
        final Cause currentCause
    ) {
        return SpongeEventFactory.createHarvestEntityEvent(currentCause, (org.spongepowered.api.entity.Entity) this.destroyingEntity);
    }

    @Override
    public void restore() {
        this.destroyingEntity.getType()
            .spawn(this.worldSupplier.get(),
                this.entityTag, null, null, this.destroyingEntity.getPosition(),
            SpawnReason.COMMAND, false, false);
    }

    @Override
    public boolean markCancelledTransactions(
        final HarvestEntityEvent event,
        final ImmutableList<? extends GameTransaction<HarvestEntityEvent>> gameTransactions
    ) {
        return false;
    }
}
