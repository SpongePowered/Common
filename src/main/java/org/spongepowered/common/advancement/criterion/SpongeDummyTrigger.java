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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.advancements.criterion.AbstractCriterionTrigger;
import net.minecraft.advancements.criterion.CriterionInstance;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.common.accessor.advancements.CriteriaTriggersAccessor;

public class SpongeDummyTrigger extends AbstractCriterionTrigger<SpongeDummyTrigger.Instance> {

    public static final SpongeDummyTrigger DUMMY_TRIGGER = CriteriaTriggersAccessor.accessor$register(new SpongeDummyTrigger(new ResourceLocation("sponge:dummy")));

    private ResourceLocation resourceLocation;

    private SpongeDummyTrigger(ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    @Override
    public ResourceLocation getId() {
        return this.resourceLocation;
    }

    @Override
    public SpongeDummyTrigger.Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
        return new SpongeDummyTrigger.Instance(this.resourceLocation);
    }

    public static class Instance extends CriterionInstance {

        public Instance(ResourceLocation criterionIn) {
            super(criterionIn);
        }

        public static Instance dummy() {
            return new Instance(SpongeDummyTrigger.DUMMY_TRIGGER.getId());
        }

        @Override
        public JsonElement serialize() {
            return new JsonObject();
        }
    }
}
