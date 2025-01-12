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
package org.spongepowered.common.mixin.api.minecraft.world.entity.animal;

import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.living.animal.Fox;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Interface.Remap;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Mixin(net.minecraft.world.entity.animal.Fox.class)
@Implements(@Interface(iface = Fox.class, prefix = "fox$", remap = Remap.NONE))
public abstract class FoxMixin_API extends AnimalMixin_API implements Fox {

    // @formatter:off
    @Shadow abstract Stream<EntityReference<LivingEntity>> shadow$getTrustedEntities();
    // @formatter:on


    @Intrinsic
    public boolean fox$trusts(UUID uniqueId) {
        return this.shadow$getTrustedEntities().map(EntityReference::getUUID).anyMatch(uniqueId::equals);
    }

    @Override
    protected Set<Value.Immutable<?>> api$getVanillaValues() {
        final Set<Value.Immutable<?>> values = super.api$getVanillaValues();

        values.add(this.requireValue(Keys.FOX_TYPE).asImmutable());
        values.add(this.requireValue(Keys.IS_CROUCHING).asImmutable());
        values.add(this.requireValue(Keys.IS_DEFENDING).asImmutable());
        values.add(this.requireValue(Keys.IS_FACEPLANTED).asImmutable());
        values.add(this.requireValue(Keys.IS_INTERESTED).asImmutable());
        values.add(this.requireValue(Keys.IS_POUNCING).asImmutable());
        values.add(this.requireValue(Keys.IS_SLEEPING).asImmutable());

        this.getValue(Keys.FIRST_TRUSTED).map(Value::asImmutable).ifPresent(values::add);
        this.getValue(Keys.SECOND_TRUSTED).map(Value::asImmutable).ifPresent(values::add);

        return values;
    }

}
