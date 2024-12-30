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

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.entity.damage.DamageStep;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.DamageCalculationEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.world.entity.TrackedAttackBridge;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.List;

public class SpongeAttackTracker extends SpongeDamageTracker {
    private final ItemStack weapon;
    private final ItemStackSnapshot weaponSnapshot;

    private float attackStrength;
    private boolean strongSprint = false;

    public SpongeAttackTracker(final DamageCalculationEvent.Pre preEvent, final ItemStack weapon) {
        super(preEvent);
        this.weapon = weapon;
        this.weaponSnapshot = ItemStackUtil.snapshotOf(weapon);
    }

    @Override
    public AttackEntityEvent.Pre preEvent() {
        return (AttackEntityEvent.Pre) super.preEvent();
    }

    @Override
    public AttackEntityEvent.Post postEvent() {
        return (AttackEntityEvent.Post) super.postEvent();
    }

    public ItemStack weapon() {
        return this.weapon;
    }

    public ItemStackSnapshot weaponSnapshot() {
        return this.weaponSnapshot;
    }

    public float attackStrength() {
        return this.attackStrength;
    }

    public void setAttackStrength(final float attackStrength) {
        this.attackStrength = attackStrength;
    }

    public boolean isStrongSprint() {
        return this.strongSprint;
    }

    public void setStrongSprint(final boolean strongSprint) {
        this.strongSprint = strongSprint;
    }

    public boolean callAttackPostEvent(final Entity entity, final DamageSource source, final float finalDamage, final float knockbackModifier) {
        final List<DamageStep> steps = this.preparePostEvent();

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            SpongeDamageTracker.generateCauseFor(source, frame);

            final AttackEntityEvent.Post event = SpongeEventFactory.createAttackEntityEventPost(frame.currentCause(),
                this.preEvent.originalBaseDamage(), this.preEvent.baseDamage(), finalDamage, finalDamage, knockbackModifier, knockbackModifier, entity, steps);

            this.postEvent = event;
            return SpongeCommon.post(event);
        }
    }

    public static @Nullable SpongeAttackTracker callAttackPreEvent(final Entity entity, final DamageSource source, final float baseDamage, final ItemStack weapon) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            SpongeDamageTracker.generateCauseFor(source, frame);

            final AttackEntityEvent.Pre event = SpongeEventFactory.createAttackEntityEventPre(frame.currentCause(), baseDamage, baseDamage, entity);
            if (SpongeCommon.post(event)) {
                return null;
            }

            return new SpongeAttackTracker(event, weapon);
        }
    }

    public static @Nullable SpongeAttackTracker of(final DamageSource source) {
        return source.getDirectEntity() instanceof TrackedAttackBridge tracked ? tracked.attack$tracker() : null;
    }
}
