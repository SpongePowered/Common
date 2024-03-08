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
package org.spongepowered.common.item.enchantment;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.common.util.Preconditions;

import java.util.Objects;
import java.util.Optional;


public final class SpongeEnchantmentBuilder extends AbstractDataBuilder<Enchantment> implements Enchantment.Builder {

    private @Nullable EnchantmentType enchantmentType;
    private @Nullable Integer level;

    public SpongeEnchantmentBuilder() {
        super(Enchantment.class, 1);
    }

    @Override
    public Enchantment.Builder from(final Enchantment value) {
        Objects.requireNonNull(value, "The enchantment to create a builder from cannot be null!");
        this.enchantmentType = value.type();
        this.level = value.level();
        return this;
    }

    @Override
    public Enchantment.Builder reset() {
        this.enchantmentType = null;
        this.level = null;
        return this;
    }

    @Override
    public Enchantment.Builder type(final EnchantmentType enchantmentType) {
        this.enchantmentType = Objects.requireNonNull(enchantmentType, "Enchantment type cannot be null!");
        return this;
    }

    @Override
    public Enchantment.Builder level(final int level) {
        Preconditions.checkArgument(level >= Short.MIN_VALUE, String.format("The specified level must be greater than %s (was %s)!", Short.MIN_VALUE, level));
        Preconditions.checkArgument(level <= Short.MAX_VALUE, String.format("The specified level must not be greater than %s (was %s)!", Short.MAX_VALUE, level));
        this.level = level;
        return this;
    }

    @Override
    public Enchantment build() {
        Preconditions.checkState(this.enchantmentType != null, "The enchantment type must be set!");
        Preconditions.checkState(this.level != null, "The level of the enchantment must be set!");
        return new SpongeEnchantment(this.enchantmentType, this.level);
    }

    @Override
    protected Optional<Enchantment> buildContent(final DataView container) throws InvalidDataException {
        Objects.requireNonNull(container, "The data view cannot be null!");
        if (!container.contains(Queries.ENCHANTMENT_ID, Queries.LEVEL)) {
            return Optional.empty();
        }
        final Optional<EnchantmentType> enchantmentType = container.getRegistryValue(Queries.ENCHANTMENT_ID, RegistryTypes.ENCHANTMENT_TYPE);
        final Optional<Integer> level = container.getInt(Queries.LEVEL);
        final Enchantment.Builder builder = Enchantment.builder();
        level.map(builder::level);
        return enchantmentType
            .map(builder::type)
            .map(Enchantment.Builder::build);
    }

}
