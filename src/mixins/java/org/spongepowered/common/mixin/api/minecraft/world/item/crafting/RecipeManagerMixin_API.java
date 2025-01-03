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
package org.spongepowered.common.mixin.api.minecraft.world.item.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager.CachedCheck;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.item.inventory.ItemStackLike;
import org.spongepowered.api.item.recipe.Recipe;
import org.spongepowered.api.item.recipe.RecipeManager;
import org.spongepowered.api.item.recipe.RecipeType;
import org.spongepowered.api.item.recipe.cooking.CookingRecipe;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.inventory.util.InventoryUtil;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(net.minecraft.world.item.crafting.RecipeManager.class)
public abstract class RecipeManagerMixin_API implements RecipeManager {

    // @formatter:off
    @Shadow public abstract Optional<? extends net.minecraft.world.item.crafting.RecipeHolder<?>> shadow$byKey(ResourceLocation recipeId);
    @Shadow protected abstract <I extends RecipeInput, T extends net.minecraft.world.item.crafting.Recipe<I>> Collection<RecipeHolder<T>> shadow$byType(net.minecraft.world.item.crafting.RecipeType<T> recipeTypeIn);
    @Shadow public abstract Collection<net.minecraft.world.item.crafting.RecipeHolder<?>> shadow$getRecipes();
    @Shadow public abstract <I extends RecipeInput, T extends net.minecraft.world.item.crafting.Recipe<I>> Optional<RecipeHolder<T>> shadow$getRecipeFor(net.minecraft.world.item.crafting.RecipeType<T> recipeTypeIn, I inventoryIn, net.minecraft.world.level.Level worldIn);

    // @formatter:on

    @SuppressWarnings({"DataFlowIssue", "unchecked"})
    @Override
    public Optional<Recipe<?>> byKey(final ResourceKey key) {
        Objects.requireNonNull(key);
        return this.shadow$byKey((ResourceLocation) (Object) key).map(RecipeHolder::value).map(Recipe.class::cast);
    }

    @Override
    @SuppressWarnings(value = {"unchecked", "rawtypes"})
    public Collection<Recipe<?>> all() {
        return (Collection) this.shadow$getRecipes().stream().map(RecipeHolder::value).toList();
    }

    @Override
    @SuppressWarnings(value = {"unchecked", "rawtypes"})
    public <T extends Recipe<?>> Collection<T> allOfType(final RecipeType<T> type) {
        Objects.requireNonNull(type);
        return this.shadow$byType((net.minecraft.world.item.crafting.RecipeType)type).stream().map(h -> ((RecipeHolder)h).value()).toList();
    }

    @Override
    public <T extends Recipe<?>> Collection<T> findByResult(final RecipeType<T> type, final ItemStackLike result) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(result);
        return this.allOfType(type).stream()
                .filter(r -> r.exemplaryResult().equals(result.asImmutable()))
                .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings(value = {"unchecked", "rawtypes"})
    public <I extends org.spongepowered.api.item.recipe.crafting.RecipeInput, T extends Recipe<I>> Optional<T> findMatchingRecipe(final RecipeType<T> type, final I inventory, final ServerWorld world) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(inventory);
        Objects.requireNonNull(world);
        if (!(inventory instanceof Container)) {
            return Optional.empty();
        }

        final var mcRecipeType = (net.minecraft.world.item.crafting.RecipeType) type;
        final var checker = net.minecraft.world.item.crafting.RecipeManager.createCheck(mcRecipeType);
        final var level = (Level) world;

        return InventoryUtil.toCraftingInput(inventory).flatMap(in -> RecipeManagerMixin_API.impl$getRecipe(level, checker, in));
    }

    private static <I extends RecipeInput, T extends net.minecraft.world.item.crafting.Recipe<I>> Optional<Recipe<?>> impl$getRecipe(final Level level, final CachedCheck<I, T> checker, final I input) {
        return checker.getRecipeFor(input, level).map(RecipeHolder::value).map(Recipe.class::cast);
    }

    @Override
    @SuppressWarnings(value = {"unchecked", "rawtypes"})
    public <T extends CookingRecipe> Optional<T> findCookingRecipe(final RecipeType<T> type, final ItemStackLike ingredient) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(ingredient);

        final SingleRecipeInput input = new SingleRecipeInput(ItemStackUtil.fromLikeToNative(ingredient));
        return this.shadow$getRecipeFor((net.minecraft.world.item.crafting.RecipeType) type, input, null).map(h -> ((RecipeHolder<?>)h).value());
    }
}
