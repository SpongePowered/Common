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


import com.google.common.collect.Lists;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class SpongeRandomEnchantmentListBuilder implements Enchantment.RandomListBuilder {

    private @Nullable Integer seed;
    private @Nullable Integer option;
    private @Nullable Integer level;
    private @Nullable List<Enchantment> pool;
    private @Nullable ItemStack item;

    private final RandomSource randomSource = RandomSource.create();

    @Override
    public Enchantment.RandomListBuilder seed(final int seed) {
        this.seed = seed;
        return this;
    }

    @Override
    public Enchantment.RandomListBuilder option(final int option) {
        this.option = option;
        return this;
    }

    @Override
    public Enchantment.RandomListBuilder level(final int level) {
        this.level = level;
        return this;
    }

    @Override
    public Enchantment.RandomListBuilder fixedPool(final List<Enchantment> pool) {
        this.pool = pool;
        return this;
    }

    @Override
    public Enchantment.RandomListBuilder item(final ItemStack item) {
        this.item = item;
        return this;
    }

    @Override
    public List<Enchantment> build() throws IllegalStateException {
        Objects.requireNonNull(this.seed, "The random seed cannot be null");
        Objects.requireNonNull(this.option, "The option cannot be null");
        Objects.requireNonNull(this.level, "The level cannot be null");
        List<EnchantmentInstance> enchantments;
        if (this.pool == null || this.pool.isEmpty()) {
            Objects.requireNonNull(this.item, "The item cannot be null");
            this.randomSource.setSeed(this.seed + this.option);
            final var registry = SpongeCommon.vanillaRegistry(Registries.ENCHANTMENT);
            var stream = registry.listElements().map($$0x -> (Holder<net.minecraft.world.item.enchantment.Enchantment>)$$0x);
            enchantments = EnchantmentHelper.selectEnchantment(this.randomSource, ItemStackUtil.toNative(this.item), this.level, stream);
        } else {
            this.randomSource.setSeed(this.seed + this.option);
            enchantments = this.basedOfFixedPool(this.randomSource, this.pool);
        }

        return SpongeRandomEnchantmentListBuilder.fromNative(enchantments);
    }

    /**
     * See {@link EnchantmentHelper#selectEnchantment}
     */
    private List<EnchantmentInstance> basedOfFixedPool(final RandomSource randomIn, final List<Enchantment> pool) {
        final List<EnchantmentInstance> list = Lists.<EnchantmentInstance>newArrayList();

        final List<EnchantmentInstance> list1 = SpongeRandomEnchantmentListBuilder.toNative(pool);

        // Same code as net.minecraft.enchantment.EnchantmentHelper#selectEnchantment
        if (!list1.isEmpty())
        {
            WeightedRandom.getRandomItem(randomIn, list1).ifPresent(list::add);

            while (randomIn.nextInt(50) <= this.level)
            {
                EnchantmentHelper.filterCompatibleEnchantments(list1, Util.lastOf(list));

                if (list1.isEmpty())
                {
                    break;
                }

                WeightedRandom.getRandomItem(randomIn, list1).ifPresent(list::add);
                this.level /= 2;
            }
        }

        return list;
    }

    public static List<Enchantment> fromNative(final List<EnchantmentInstance> list) {
        return list.stream().map(data -> Enchantment.of(((EnchantmentType) (Object) data.enchantment.value()), data.level)).collect(Collectors.toList());
    }

    public static List<EnchantmentInstance> toNative(final List<Enchantment> list) {
        final var registry = SpongeCommon.vanillaRegistry(Registries.ENCHANTMENT);
        return list.stream().map(ench -> {
                    var mcEnch = registry.wrapAsHolder((net.minecraft.world.item.enchantment.Enchantment) (Object) ench.type());
                    return new EnchantmentInstance(mcEnch, ench.level());
                }
        ).collect(Collectors.toList());
    }

    @Override
    public Enchantment.RandomListBuilder reset() {
        this.pool = null;
        this.level = null;
        this.option = null;
        this.seed = null;
        this.item = null;
        return this;
    }
}
