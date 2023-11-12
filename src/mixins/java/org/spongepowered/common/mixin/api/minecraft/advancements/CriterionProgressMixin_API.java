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
package org.spongepowered.common.mixin.api.minecraft.advancements;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.resources.ResourceLocation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.advancement.AdvancementProgress;
import org.spongepowered.api.advancement.criteria.AdvancementCriterion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.advancements.AdvancementProgressBridge;
import org.spongepowered.common.bridge.advancements.CriterionProgressBridge;

import java.time.Instant;
import java.util.Optional;

@Mixin(CriterionProgress.class)
public abstract class CriterionProgressMixin_API implements org.spongepowered.api.advancement.criteria.CriterionProgress {

    @Shadow @Nullable private Instant obtained;

    @Override
    public AdvancementCriterion criterion() {
        return ((CriterionProgressBridge) this).bridge$getCriterion();
    }

    @Override
    public boolean achieved() {
        return this.obtained != null;
    }

    @Override
    public Optional<Instant> get() {
        return this.obtained == null ? Optional.empty() : Optional.of(this.obtained);
    }

    @Override
    public Instant grant() {
        if (this.obtained != null) {
            return this.obtained;
        }
        final AdvancementProgress advancementProgress = ((CriterionProgressBridge) this).bridge$getAdvancementProgress();
        final ResourceLocation key = ((AdvancementProgressBridge) advancementProgress).bridge$getAdvancementKey();
        final org.spongepowered.api.advancement.Advancement advancement = advancementProgress.advancement();
        final AdvancementHolder holder = new AdvancementHolder(key, (Advancement) (Object) advancement);
        ((AdvancementProgressBridge) advancementProgress).bridge$getPlayerAdvancements().award(holder, this.criterion().name());
        return this.obtained;
    }

    @Override
    public Optional<Instant> revoke() {
        if (this.obtained == null) {
            return Optional.empty();
        }
        final Instant instant = this.obtained;
        final AdvancementProgress advancementProgress = ((CriterionProgressBridge) this).bridge$getAdvancementProgress();
        final ResourceLocation key = ((AdvancementProgressBridge) advancementProgress).bridge$getAdvancementKey();
        final org.spongepowered.api.advancement.Advancement advancement = advancementProgress.advancement();
        final AdvancementHolder holder = new AdvancementHolder(key, (Advancement) (Object) advancement);
        ((AdvancementProgressBridge) advancementProgress).bridge$getPlayerAdvancements().revoke(holder, this.criterion().name());
        return Optional.of(instant);
    }

}
