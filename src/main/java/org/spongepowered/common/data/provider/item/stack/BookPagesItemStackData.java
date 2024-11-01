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
package org.spongepowered.common.data.provider.item.stack;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.util.Predicates;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public final class BookPagesItemStackData {

    private BookPagesItemStackData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(ItemStack.class)
                    .create(Keys.APPLIED_ENCHANTMENTS)
                        .get(h -> BookPagesItemStackData.get(h, DataComponents.ENCHANTMENTS))
                        .set((h, v) -> BookPagesItemStackData.set(h, v, iv -> iv.stream().filter(Predicates.distinctBy(Enchantment::type)), DataComponents.ENCHANTMENTS))
                        .delete(h -> BookPagesItemStackData.delete(h, DataComponents.ENCHANTMENTS))
                    .create(Keys.STORED_ENCHANTMENTS)
                        .get(h -> BookPagesItemStackData.get(h, DataComponents.STORED_ENCHANTMENTS))
                        .set((h, v) -> BookPagesItemStackData.set(h, v, Collection::stream, DataComponents.STORED_ENCHANTMENTS))
                        .delete(h -> BookPagesItemStackData.delete(h, DataComponents.STORED_ENCHANTMENTS));
    }
    // @formatter:on

    private static List<Enchantment> get(final ItemStack holder, final DataComponentType<ItemEnchantments> component) {
        return holder.getOrDefault(component, ItemEnchantments.EMPTY).entrySet().stream()
                .map(e -> Enchantment.of((EnchantmentType) (Object) e.getKey().value(), e.getIntValue())).toList();
    }

    private static boolean set(final ItemStack holder, final List<Enchantment> value, final Function<List<Enchantment>, Stream<Enchantment>> filter,
            final DataComponentType<ItemEnchantments> component) {
        if (value.isEmpty()) {
            return BookPagesItemStackData.delete(holder, component);
        }
        final var registry = SpongeCommon.vanillaRegistry(Registries.ENCHANTMENT);
        holder.update(component, ItemEnchantments.EMPTY, ench -> {
            final ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ench);
            mutable.keySet().clear();
            value.forEach(e -> {
                var enchHolder = registry.wrapAsHolder((net.minecraft.world.item.enchantment.Enchantment) (Object) e.type());
                mutable.set(enchHolder, e.level());
            });
            return mutable.toImmutable();
        });
        return true;
    }

    private static boolean delete(final ItemStack holder, final DataComponentType<ItemEnchantments> component) {
        holder.remove(component);
        return true;
    }
}
