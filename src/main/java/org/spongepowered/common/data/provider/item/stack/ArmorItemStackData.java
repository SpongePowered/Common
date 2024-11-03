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

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.recipe.smithing.ArmorTrim;
import org.spongepowered.api.registry.RegistryEntry;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.common.data.provider.DataProviderRegistrator;

public final class ArmorItemStackData {

    private ArmorItemStackData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(ItemStack.class)
                    .create(Keys.ARMOR_MATERIAL)
                        .get(h -> {
                            final @Nullable Equippable equippable = h.get(DataComponents.EQUIPPABLE);
                            if (equippable == null) {
                                return null;
                            }
                            return equippable.model()
                                .map(rl -> (ResourceKey) (Object) rl)
                                .flatMap(rk -> RegistryTypes.ARMOR_MATERIAL.get().findEntry(rk))
                                .map(RegistryEntry::value)
                                .orElse(null);
                        })
                        .supports(h -> h.getItem() instanceof ArmorItem)
                    .create(Keys.ARMOR_TRIM)
                        .get(h -> {
                            final net.minecraft.world.item.equipment.trim.@Nullable ArmorTrim trim = h.get(DataComponents.TRIM);
                            if (trim != null) {
                                return (ArmorTrim) (Object) trim;
                            }
                            return null;
                        })
                        .set((h, v) -> {
                            if (v == null) {
                                h.remove(DataComponents.TRIM);
                                return;
                            }
                            h.set(DataComponents.TRIM, (net.minecraft.world.item.equipment.trim.ArmorTrim) (Object) v);
                        })
                        .delete(h -> h.remove(DataComponents.TRIM))
                    .supports(h -> h.getItem() instanceof ArmorItem)
                    .create(Keys.DAMAGE_ABSORPTION)
                        .get(h -> {
                            final @Nullable ItemAttributeModifiers modifiersContainer = h.get(DataComponents.ATTRIBUTE_MODIFIERS);
                            if (modifiersContainer == null) {
                                return null;
                            }
                            return modifiersContainer.modifiers().stream()
                                .filter(e1 -> e1.attribute() == Attributes.ARMOR)
                                .findFirst()
                                .map(e -> e.modifier().amount())
                                .orElse(null);
                        })
                        .supports(h -> h.getItem() instanceof ArmorItem)
                    .create(Keys.EQUIPMENT_TYPE)
                        .get(h -> {
                            final @Nullable Equippable equippable = h.get(DataComponents.EQUIPPABLE);
                            if (equippable == null) {
                                return null;
                            }

                            return (EquipmentType) (Object) equippable.slot();
                        })
                        .supports(h -> h.getItem() instanceof ArmorItem);
    }
    // @formatter:on
}
