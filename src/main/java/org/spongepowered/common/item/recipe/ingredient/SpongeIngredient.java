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
package org.spongepowered.common.item.recipe.ingredient;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.item.inventory.ItemStackLike;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class SpongeIngredient extends Ingredient {

    // Only used to allow old recipes to load before they get regenerated
    private static final Codec<ItemStack> LEGACY_STACK_CODEC = RecordCodecBuilder.create(b -> b.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("ItemType").forGetter(ItemStack::getItem),
            Codec.INT.fieldOf("Count").forGetter(ItemStack::getCount)).apply(b, (type, qty) -> {
        SpongeCommon.logger().info("Found legacy recipe");
        return new ItemStack(type, qty);
    }));
    private static final Codec<ItemStack> STACK_CODEC = Codec.xor(ItemStack.CODEC, LEGACY_STACK_CODEC).xmap(to -> to.map(i -> i, i -> i), Either::left);

    private static final Codec<SpongeRawIngredient> RAW_CODEC = RecordCodecBuilder.create(
            builder -> builder.group(
                    Codec.STRING.optionalFieldOf(SpongeItemList.INGREDIENT_TYPE, "vanilla").forGetter(raw -> raw.type),
                    Codec.list(STACK_CODEC).optionalFieldOf(SpongeItemList.INGREDIENT_ITEM, List.of()).forGetter(raw -> raw.stacks),
                    Codec.STRING.optionalFieldOf(SpongePredicateItemList.INGREDIENT_PREDICATE).forGetter(raw -> raw.predicateId)
            ).apply(builder, SpongeRawIngredient::new)
    );

    public static final Codec<SpongeIngredient> CODEC = RAW_CODEC.flatXmap(raw -> {
        switch (raw.type) {
            case "vanilla" -> {
                return DataResult.error(() -> "Vanilla Ingredient");
            }
            case SpongeStackItemList.TYPE_STACK -> {
                final SpongeStackItemList spongeStackItemList = new SpongeStackItemList(raw.stacks.toArray(new ItemStack[0]));
                final SpongeIngredient ingredient = new SpongeIngredient(raw.type, spongeStackItemList, null);
                return DataResult.success(ingredient);
            }
            case SpongePredicateItemList.TYPE_PREDICATE -> {
                if (raw.predicateId.isEmpty()) {
                    return DataResult.error(() -> "Missing sponge:predicate for custom ingredient of type " + raw.type);
                }
                final Predicate<ItemStack> predicate = SpongeIngredient.cachedPredicate(raw.predicateId.get());
                if (predicate == null) {
                    return DataResult.error(() -> "Could not find predicate for custom ingredient with id " + raw.predicateId.get());
                }
                final var predicateList = new SpongePredicateItemList(raw.predicateId.get(), predicate, raw.stacks.toArray(new ItemStack[0]));
                final SpongeIngredient ingredient = new SpongeIngredient(raw.type, predicateList, raw.predicateId.orElse(null));
                return DataResult.success(ingredient);
            }
            default -> {
                return DataResult.error(() -> "Unknown ingredient type " + raw.type);
            }
        }
    }, spongeIngredient -> {
        switch (spongeIngredient.type) {
            case SpongeStackItemList.TYPE_STACK -> {
                final var stacks = Arrays.asList(spongeIngredient.itemList.stacks);
                return DataResult.success(new SpongeRawIngredient(spongeIngredient.type, stacks, Optional.empty()));
            }
            case SpongePredicateItemList.TYPE_PREDICATE -> {
                final var stacks = Arrays.asList(spongeIngredient.itemList.stacks);
                return DataResult.success(new SpongeRawIngredient(spongeIngredient.type, stacks, Optional.ofNullable(spongeIngredient.predicateId)));
            }
        }
        throw new NotImplementedException("Serializing SpongeIngredient is not implemented yet.");
    });
    public final String type;
    public final String predicateId;
    private final SpongeItemList itemList;

    record SpongeRawIngredient(String type, List<ItemStack> stacks, Optional<String> predicateId) {

    }

    public SpongeIngredient(final String type, final SpongeItemList itemList, final String predicateId) {
        super(HolderSet.direct(itemList.stream().toList()));
        this.type = type;
        this.predicateId = predicateId;
        this.itemList = itemList;
    }

    public static void clearCache() {
        SpongeIngredient.cachedPredicates.clear();
    }

    @Override
    public boolean test(final ItemStack testStack) {
        if (testStack == null) {
            return false;
        }

        return this.itemList.test(testStack);
    }

    public static SpongeIngredient spongeFromStacks(net.minecraft.world.item.ItemStack... stacks) {
        final SpongeStackItemList itemList = new SpongeStackItemList(stacks);
        return new SpongeIngredient(SpongeStackItemList.TYPE_STACK, itemList, null);
    }

    private final static Map<String, Predicate<ItemStack>> cachedPredicates = new HashMap<>();

    public static SpongeIngredient spongeFromPredicate(ResourceKey key, Predicate<? super ItemStackLike> predicate,
            net.minecraft.world.item.ItemStack... exemplaryIngredients) {
        final Predicate<ItemStack> mcPredicate = stack -> predicate.test(ItemStackUtil.fromNative(stack));
        final Predicate<ItemStack> registeredPredicate = SpongeIngredient.cachedPredicates.get(key.toString());
        if (registeredPredicate instanceof WrappedPredicate wrapped) {
            wrapped.setPredicate(mcPredicate);
        } else if (registeredPredicate != null) {
            SpongeCommon.logger().warn(MessageFormat.format("Predicate ingredient registered twice! {} was replaced.", key.toString()));
        } else {
            SpongeIngredient.cachedPredicates.put(key.toString(), mcPredicate);
        }
        final SpongePredicateItemList itemList = new SpongePredicateItemList(key.toString(), mcPredicate, exemplaryIngredients);
        return new SpongeIngredient(SpongePredicateItemList.TYPE_PREDICATE, itemList, key.toString());
    }

    public static Predicate<ItemStack> cachedPredicate(String id) {

        return SpongeIngredient.cachedPredicates.computeIfAbsent(id, k -> new WrappedPredicate(id));
    }

    public static class WrappedPredicate implements Predicate<ItemStack> {

        private final String key;
        private Predicate<ItemStack> predicate;

        public WrappedPredicate(String key) {
            this.key = key;
        }

        public void setPredicate(final Predicate<ItemStack> predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean test(final ItemStack itemStack) {
            if (this.predicate == null) {
                SpongeCommon.logger().error(key + " ingredient predicate was not registered. Is the plugin loaded?");
                return false;
            }
            return this.predicate.test(itemStack);
        }
    }


}
