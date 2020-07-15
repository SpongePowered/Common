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
package org.spongepowered.common.mixin.api.mcp.entity;

import com.google.common.base.Preconditions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.attribute.Attribute;
import org.spongepowered.api.entity.attribute.type.AttributeType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import java.util.Set;

@Mixin(value = LivingEntity.class, priority = 999)
public abstract class LivingEntityMixin_API extends EntityMixin_API implements Living {

    @Shadow public abstract ItemStack shadow$getHeldItem(Hand hand);
    @Shadow public abstract float shadow$getHealth();
    @Shadow public abstract IAttributeInstance shadow$getAttribute(IAttribute attribute);

    @Override
    public Text getTeamRepresentation() {
        return Text.of(this.shadow$getUniqueID().toString());
    }

    @Override
    public Optional<Attribute> getAttribute(final AttributeType type) {
        Preconditions.checkNotNull(type, "AttributeType cannot be null");
        return Optional.ofNullable((Attribute) this.shadow$getAttribute((IAttribute) type));
    }

    @Override
    protected Set<Value.Immutable<?>> api$getVanillaValues() {
        final Set<Value.Immutable<?>> values = super.api$getVanillaValues();

        values.add(this.health().asImmutable());
        values.add(this.maxHealth().asImmutable());
        values.add(this.lastAttacker().asImmutable());
        values.add(this.headRotation().asImmutable());

        this.lastDamageReceived().map(Value::asImmutable).ifPresent(values::add);

        return values;
    }

}
