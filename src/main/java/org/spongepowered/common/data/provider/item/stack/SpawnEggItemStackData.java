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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.EntityArchetype;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.entity.SpongeEntityArchetypeBuilder;

public final class SpawnEggItemStackData {

    // @formatter:off
    @SuppressWarnings({"rawTypes", "rawtypes", "deprecation"})
    public static void register(final DataProviderRegistrator registrator) {
        registrator.asMutable(ItemStack.class)
                .create(Keys.ENTITY_TO_SPAWN)
                    .supports(stack -> !stack.isEmpty() && stack.getItem() instanceof SpawnEggItem)
                    .get(stack -> {
                        final Item item = stack.getItem();
                        final SpawnEggItem eggItem = (SpawnEggItem) item;
                        final EntityType<?> type = eggItem.getType(SpongeCommon.vanillaRegistryAccess(), stack);
                        final var tag = stack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY).getUnsafe();

                        final EntityArchetype.Builder builder = EntityArchetype.builder().type((org.spongepowered.api.entity.EntityType<?>) type);
                        ((SpongeEntityArchetypeBuilder)builder).entityData(tag);

                        return builder.build();
                    })
            .asImmutable(ItemStack.class)
                .create(Keys.ENTITY_TYPE)
                    .supports(stack -> !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.SpawnEggItem)
                    .get(stack -> {
                        final Item item = stack.getItem();
                        final SpawnEggItem eggItem = (SpawnEggItem) item;
                        return (org.spongepowered.api.entity.EntityType) eggItem.getType(SpongeCommon.vanillaRegistryAccess(), stack);
                    })
        ;
    }
    // @formatter:on

}
