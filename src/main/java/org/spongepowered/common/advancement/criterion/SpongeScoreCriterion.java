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
package org.spongepowered.common.advancement.criterion;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.ICriterionInstance;
import org.spongepowered.api.advancement.criteria.ScoreAdvancementCriterion;
import org.spongepowered.api.advancement.criteria.trigger.FilteredTrigger;
import org.spongepowered.common.bridge.advancements.CriterionBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

public class SpongeScoreCriterion implements ScoreAdvancementCriterion, DefaultedAdvancementCriterion {

    public static boolean BYPASS_EVENT = false;
    public static final String INTERNAL_SUFFIX_BASE = "&score_goal_id=";

    private final String name;
    public final List<DefaultedAdvancementCriterion> internalCriteria;

    @SuppressWarnings("ConstantConditions")
    public SpongeScoreCriterion(final String name, final int goal, @Nullable final ICriterionInstance trigger) {
        this.internalCriteria = new ArrayList<>(goal);
        this.name = name;
        for (int i = 0; i < goal; i++) {
            final ICriterionInstance mctrigger;
            if (i == 0) {
                mctrigger = trigger == null ? SpongeScoreTrigger.Instance.of(goal) : trigger;
            } else {
                mctrigger = SpongeDummyTrigger.Instance.dummy();
            }
            final Criterion criterion = new Criterion(mctrigger);
            ((CriterionBridge) criterion).bridge$setScoreCriterion(this);
            ((CriterionBridge) criterion).bridge$setName(name + INTERNAL_SUFFIX_BASE + i);
            this.internalCriteria.add((DefaultedAdvancementCriterion) criterion);
        }
    }

    public SpongeScoreCriterion(final String name, final List<DefaultedAdvancementCriterion> internalCriteria) {
        this.name = name;
        this.internalCriteria = internalCriteria;
    }

    @Override
    public int getGoal() {
        return this.internalCriteria.size();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Optional<FilteredTrigger<?>> getTrigger() {
        // The first internal criterion holds the trigger
        return this.internalCriteria.get(0).getTrigger();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SpongeScoreCriterion)) {
            return false;
        }

        SpongeScoreCriterion that = (SpongeScoreCriterion) o;

        return this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
