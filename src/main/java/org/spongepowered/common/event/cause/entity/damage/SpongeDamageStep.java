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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.DamageStep;
import org.spongepowered.api.event.cause.entity.damage.DamageStepType;

import java.util.List;
import java.util.StringJoiner;

public final class SpongeDamageStep implements DamageStep {
    private static final Logger LOGGER = LogManager.getLogger();

    private final DamageStepType type;
    private final Cause cause;
    private final List<DamageModifier> modifiersBefore;
    private final List<DamageModifier> modifiersAfter;

    private State state = State.BEFORE;
    private boolean skipped;

    private final double damageBeforeModifiers;
    private double damageBeforeStep;
    private double damageAfterStep;
    private double damageAfterModifiers;

    public SpongeDamageStep(final DamageStepType type, final double initialDamage, final Cause cause,
                            final List<DamageModifier> modifiersBefore, final List<DamageModifier> modifiersAfter) {
        this.type = type;
        this.cause = cause;
        this.damageBeforeModifiers = initialDamage;
        this.modifiersBefore = ImmutableList.copyOf(modifiersBefore);
        this.modifiersAfter = ImmutableList.copyOf(modifiersAfter);
    }

    @Override
    public DamageStepType type() {
        return this.type;
    }

    @Override
    public Cause cause() {
        return this.cause;
    }

    @Override
    public boolean isSkipped() {
        return this.skipped;
    }

    @Override
    public void setSkipped(boolean skipped) {
        if (this.state != State.BEFORE) {
            throw new IllegalStateException("Step can only be skipped before occurring");
        }
        this.skipped = skipped;
    }

    @Override
    public double damageBeforeModifiers() {
        return this.damageBeforeModifiers;
    }

    @Override
    public double damageBeforeStep() {
        if (this.state == State.BEFORE) {
            throw new IllegalStateException("Before modifiers haven't finished");
        }
        return this.damageBeforeStep;
    }

    @Override
    public double damageAfterStep() {
        if (this.state == State.BEFORE) {
            throw new IllegalStateException("Step hasn't started");
        }
        if (this.state == State.STEP) {
            throw new IllegalStateException("Step hasn't finished");
        }
        return this.damageAfterStep;
    }

    @Override
    public double damageAfterModifiers() {
        if (this.state != State.END) {
            throw new IllegalStateException("Modifiers haven't finished");
        }
        return this.damageAfterModifiers;
    }

    @Override
    public List<DamageModifier> modifiersBefore() {
        return this.modifiersBefore;
    }

    @Override
    public List<DamageModifier> modifiersAfter() {
        return this.modifiersAfter;
    }

    public State state() {
        return this.state;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SpongeDamageStep.class.getSimpleName() + "[", "]")
            .add("type=" + this.type)
            .add("cause=" + this.cause)
            .add("damageBeforeModifiers=" + this.damageBeforeModifiers)
            .add("damageBeforeStep=" + this.damageBeforeStep)
            .add("damageAfterStep=" + this.damageAfterStep)
            .add("damageAfterModifiers=" + this.damageAfterModifiers)
            .add("modifiersBefore=" + this.modifiersBefore)
            .add("modifiersAfter=" + this.modifiersAfter)
            .add("state=" + this.state)
            .toString();
    }

    public double applyModifiersBefore() {
        if (this.state != State.BEFORE) {
            throw new IllegalStateException();
        }

        double damage = this.damageBeforeModifiers;
        for (DamageModifier modifier : this.modifiersBefore) {
            try {
                damage = modifier.modify(this, damage);
            } catch (Exception e) {
                LOGGER.error("Failed to apply modifier {} before step {}", modifier, this, e);
            }
        }

        this.damageBeforeStep = damage;
        this.state = State.STEP;

        return damage;
    }

    public double applyModifiersAfter(double damage) {
        if (this.state != State.STEP) {
            throw new IllegalStateException();
        }

        if (this.skipped) {
            damage = this.damageBeforeStep;
        }

        this.damageAfterStep = damage;
        this.state = State.AFTER;

        for (DamageModifier modifier : this.modifiersBefore) {
            try {
                damage = modifier.modify(this, damage);
            } catch (Exception e) {
                LOGGER.error("Failed to apply modifier {} after step {}", modifier, this, e);
            }
        }

        this.damageAfterModifiers = damage;
        this.state = State.END;

        return damage;
    }

    public enum State {
        BEFORE,
        STEP,
        AFTER,
        END
    }
}
