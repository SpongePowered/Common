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

import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.data.Keys;
import org.spongepowered.common.accessor.world.item.AdventureModePredicateAccessor;
import org.spongepowered.common.accessor.world.item.enchantment.ItemEnchantmentsAccessor;
import org.spongepowered.common.data.provider.DataProviderRegistrator;

import java.util.Collections;
import java.util.List;

public final class HideFlagsItemStackData {

    private HideFlagsItemStackData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(ItemStack.class)
                    .create(Keys.HIDE_ATTRIBUTES)
                        .get(h -> h.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).showInTooltip())
                        .set((h, v) -> h.update(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY, p -> new ItemAttributeModifiers(p.modifiers(), !v)))
                    .create(Keys.HIDE_CAN_DESTROY)
                        .get(h -> h.has(DataComponents.CAN_BREAK) && !h.get(DataComponents.CAN_BREAK).showInTooltip())
                        .set((h, v) -> h.set(DataComponents.CAN_BREAK, HideFlagsItemStackData.newAdventureModePredicate(h, !v)))
                    .create(Keys.HIDE_CAN_PLACE)
                        .get(h -> {
                            final var predicate = h.get(DataComponents.CAN_BREAK);
                            if (predicate == null) {
                                return false;
                            }
                            return !predicate.showInTooltip();
                        })
                        .set((h, v) -> h.set(DataComponents.CAN_PLACE_ON, HideFlagsItemStackData.newAdventureModePredicate(h, !v)))
                    .create(Keys.HIDE_ENCHANTMENTS)
                        .get(h -> ((ItemEnchantmentsAccessor)h.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)).accessor$showInTooltip())
                        .set((h, v) -> h.set(DataComponents.ENCHANTMENTS, HideFlagsItemStackData.newItemEnchantments(h, !v)))
                    .create(Keys.HIDE_STORED_ENCHANTMENTS)
                        .get(h -> ((ItemEnchantmentsAccessor)h.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)).accessor$showInTooltip())
                        .set((h, v) -> h.set(DataComponents.STORED_ENCHANTMENTS, HideFlagsItemStackData.newItemEnchantments(h, !v)))
                    .create(Keys.HIDE_MISCELLANEOUS)
                        .get(h -> h.get(DataComponents.HIDE_ADDITIONAL_TOOLTIP) != Unit.INSTANCE)
                        .set((h, v) -> h.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, v ? Unit.INSTANCE : null))
                    .create(Keys.HIDE_UNBREAKABLE)
                        .get(h -> h.has(DataComponents.UNBREAKABLE) && !h.get(DataComponents.UNBREAKABLE).showInTooltip())
                        .set((h, v) -> {
                            if (h.has(DataComponents.UNBREAKABLE)) {
                                h.set(DataComponents.UNBREAKABLE, new Unbreakable(v));
                            } // else TODO not supported?
                        })
                    .create(Keys.HIDE_TOOLTIP)
                        .get(h -> h.get(DataComponents.HIDE_ADDITIONAL_TOOLTIP) != Unit.INSTANCE)
                        .set((h, v) -> h.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, v ? Unit.INSTANCE : null))
        ;


        // TODO missing show_in_tooltip (DYED_COLOR, more?)
    }
    // @formatter:on

    @NotNull
    private static AdventureModePredicate newAdventureModePredicate(final ItemStack h, final boolean showInTooltip) {
        if (h.has(DataComponents.CAN_BREAK)) {
            final List<BlockPredicate> $$0 = ((AdventureModePredicateAccessor) h.get(DataComponents.CAN_BREAK)).accessor$predicates();
            return new AdventureModePredicate($$0, showInTooltip);
        }
        return new AdventureModePredicate(Collections.emptyList(), showInTooltip);
    }

    @NotNull
    private static ItemEnchantments newItemEnchantments(final ItemStack h, final boolean showInTooltip) {
        final ItemEnchantmentsAccessor enchantments = (ItemEnchantmentsAccessor) h.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);;
        return ItemEnchantmentsAccessor.invoker$new(enchantments.accessor$enchantments(), showInTooltip);
    }

}
