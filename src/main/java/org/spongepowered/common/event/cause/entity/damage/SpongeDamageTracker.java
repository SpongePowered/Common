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
package org.spongepowered.common.event.cause.entity.damage;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.entity.damage.DamageStep;
import org.spongepowered.api.event.cause.entity.damage.DamageStepType;
import org.spongepowered.api.event.entity.DamageCalculationEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.registry.DefaultedRegistryReference;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.CreatorTrackedBridge;
import org.spongepowered.common.bridge.world.damagesource.DamageSourceBridge;
import org.spongepowered.common.bridge.world.level.chunk.LevelChunkBridge;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.util.VecHelper;

import java.util.ArrayList;
import java.util.List;

public class SpongeDamageTracker {
    private static final Logger LOGGER = LogManager.getLogger();

    protected final List<SpongeDamageStep> steps = new ArrayList<>();
    protected final DamageCalculationEvent.Pre preEvent;
    protected DamageCalculationEvent.Post postEvent;

    public SpongeDamageTracker(final DamageCalculationEvent.Pre preEvent) {
        this.preEvent = preEvent;
    }

    public DamageCalculationEvent.Pre preEvent() {
        return this.preEvent;
    }

    public DamageCalculationEvent.Post postEvent() {
        if (this.postEvent == null) {
            throw new IllegalStateException("Post event not yet fired");
        }
        return this.postEvent;
    }

    public SpongeDamageStep newStep(final DefaultedRegistryReference<DamageStepType> typeRef, final float damage, final Object... causes) {
        final DamageStepType type = typeRef.get();
        final SpongeDamageStep step = new SpongeDamageStep(type, damage, Cause.of(EventContext.empty(), List.of(causes)), this.preEvent.modifiersBefore(type), this.preEvent.modifiersAfter(type));

        if (this.postEvent != null) {
            LOGGER.warn("A new step {} is being captured after the post event.", step);
        }

        if (!this.steps.isEmpty()) {
            final SpongeDamageStep previous = this.steps.getLast();
            if (previous.state() != SpongeDamageStep.State.END) {
                LOGGER.warn("A new step {} is being captured but previous step {} hasn't finished.", step, previous);
                this.steps.removeLast();
            }
        }

        this.steps.add(step);
        return step;
    }

    public float startStep(final DefaultedRegistryReference<DamageStepType> typeRef, final float damage, final Object... causes) {
        return (float) this.newStep(typeRef, damage, causes).applyModifiersBefore();
    }

    public @Nullable SpongeDamageStep currentStep(final DefaultedRegistryReference<DamageStepType> typeRef) {
        if (this.steps.isEmpty()) {
            LOGGER.warn("Expected a current step of type {} but no step has been captured yet.", typeRef.location());
            return null;
        }

        final DamageStepType type = typeRef.get();
        final SpongeDamageStep step = this.steps.getLast();
        if (step.type() != type) {
            LOGGER.warn("Expected a current step of type {} but got {}.", type, step);
            return null;
        }
        return step;
    }

    public float endStep(final DefaultedRegistryReference<DamageStepType> typeRef, final float damage) {
        final SpongeDamageStep step = this.currentStep(typeRef);
        return step == null ? damage : (float) step.applyModifiersAfter(damage);
    }

    public boolean isSkipped(final DefaultedRegistryReference<DamageStepType> typeRef) {
        final SpongeDamageStep step = this.currentStep(typeRef);
        return step != null && step.isSkipped();
    }

    public @Nullable SpongeDamageStep lastStep(final DefaultedRegistryReference<DamageStepType> typeRef) {
        final DamageStepType type = typeRef.get();
        for (int i = this.steps.size() - 1; i >= 0; i--) {
            final SpongeDamageStep step = this.steps.get(i);
            if (step.type() == type) {
                return step;
            }
        }
        return null;
    }

    public float damageAfter(final DefaultedRegistryReference<DamageStepType> typeRef) {
        final SpongeDamageStep step = this.lastStep(typeRef);
        return step == null ? 0 : (float) step.damageAfterModifiers();
    }

    protected List<DamageStep> preparePostEvent() {
        if (this.postEvent != null) {
            throw new IllegalStateException("Post event already fired");
        }

        if (!this.steps.isEmpty()) {
            final SpongeDamageStep last = this.steps.getLast();
            if (last.state() != SpongeDamageStep.State.END) {
                LOGGER.warn("Calling post event but last step {} hasn't finished.", last);
                return ImmutableList.copyOf(this.steps.subList(0, this.steps.size() - 1));
            }
        }

        return ImmutableList.copyOf(this.steps);
    }

    public float callDamagePostEvent(final Entity entity, final float finalDamage) {
        final List<DamageStep> steps = this.preparePostEvent();

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            SpongeDamageTracker.generateCauseFor((DamageSource) this.preEvent.source(), frame);

            final DamageEntityEvent.Post event = SpongeEventFactory.createDamageEntityEventPost(frame.currentCause(),
                this.preEvent.originalBaseDamage(), this.preEvent.baseDamage(), finalDamage, finalDamage, entity, steps);

            this.postEvent = event;
            if (SpongeCommon.post(event)) {
                return 0;
            }
            return (float) event.finalDamage();
        }
    }

    protected static void generateCauseFor(final DamageSource source, final CauseStackManager.StackFrame frame) {
        if (source.getDirectEntity() instanceof org.spongepowered.api.entity.Entity entity) {
            if (!(entity instanceof Player) && entity instanceof CreatorTrackedBridge creatorBridge) {
                creatorBridge.tracker$getCreatorUUID().ifPresent(creator -> frame.addContext(EventContextKeys.CREATOR, creator));
                creatorBridge.tracker$getNotifierUUID().ifPresent(notifier -> frame.addContext(EventContextKeys.NOTIFIER, notifier));
            }
        } else if (((DamageSourceBridge) source).bridge$blockLocation() != null) {
            final ServerLocation location = ((DamageSourceBridge) source).bridge$blockLocation();
            final BlockPos blockPos = VecHelper.toBlockPos(location);
            final LevelChunkBridge chunkBridge = (LevelChunkBridge) ((net.minecraft.world.level.Level) location.world()).getChunkAt(blockPos);
            chunkBridge.bridge$getBlockCreatorUUID(blockPos).ifPresent(creator -> frame.addContext(EventContextKeys.CREATOR, creator));
            chunkBridge.bridge$getBlockNotifierUUID(blockPos).ifPresent(notifier -> frame.addContext(EventContextKeys.NOTIFIER, notifier));
        }
        frame.pushCause(source);
    }

    public static @Nullable SpongeDamageTracker callDamagePreEvent(final Entity entity, final DamageSource source, final float baseDamage) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            SpongeDamageTracker.generateCauseFor(source, frame);

            final DamageEntityEvent.Pre event = SpongeEventFactory.createDamageEntityEventPre(frame.currentCause(), baseDamage, baseDamage, entity);
            if (SpongeCommon.post(event)) {
                return null;
            }

            return new SpongeDamageTracker(event);
        }
    }

    public static DamageEntityEvent.@Nullable Post callDamageEvents(final Entity entity, final DamageSource source, final float baseDamage) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            SpongeDamageTracker.generateCauseFor(source, frame);

            final DamageEntityEvent.Pre preEvent = SpongeEventFactory.createDamageEntityEventPre(frame.currentCause(), baseDamage, baseDamage, entity);
            if (SpongeCommon.post(preEvent)) {
                return null;
            }

            final DamageEntityEvent.Post postEvent = SpongeEventFactory.createDamageEntityEventPost(frame.currentCause(),
                preEvent.originalBaseDamage(), preEvent.baseDamage(), preEvent.baseDamage(), preEvent.baseDamage(), entity, List.of());
            if (SpongeCommon.post(postEvent)) {
                return null;
            }

            return postEvent;
        }
    }
}
