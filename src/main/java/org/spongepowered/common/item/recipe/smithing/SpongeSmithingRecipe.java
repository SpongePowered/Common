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
package org.spongepowered.common.item.recipe.smithing;

import static org.spongepowered.common.util.Constants.Recipe.SPONGE_TYPE;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.TransmuteResult;
import org.spongepowered.common.bridge.world.item.crafting.RecipeResultBridge;
import org.spongepowered.common.bridge.world.item.crafting.SmithingRecipeBridge;
import org.spongepowered.common.item.recipe.ResultFunctionRecipe;
import org.spongepowered.common.item.recipe.ingredient.IngredientResultUtil;
import org.spongepowered.common.util.Constants;

import java.util.Optional;

public class SpongeSmithingRecipe extends SmithingTransformRecipe implements ResultFunctionRecipe {

    public static final MapCodec<SpongeSmithingRecipe> SPONGE_CODEC =  RecordCodecBuilder.mapCodec(
            $$0 -> $$0.group(
                            Codec.STRING.fieldOf(SPONGE_TYPE).forGetter(a -> "custom"),
                            Ingredient.CODEC.optionalFieldOf(Constants.Recipe.SMITHING_TEMPLATE_INGREDIENT).forGetter($$0x -> ((SmithingRecipeBridge) $$0x).bridge$template()),
                            Ingredient.CODEC.optionalFieldOf(Constants.Recipe.SMITHING_BASE_INGREDIENT).forGetter($$0x -> ((SmithingRecipeBridge) $$0x).bridge$base()),
                            Ingredient.CODEC.optionalFieldOf(Constants.Recipe.SMITHING_ADDITION_INGREDIENT).forGetter($$0x -> ((SmithingRecipeBridge) $$0x).bridge$addition()),
                            ItemStack.CODEC.fieldOf(Constants.Recipe.RESULT).forGetter($$0x -> ((RecipeResultBridge) $$0x).bridge$result()),
                            IngredientResultUtil.CACHED_RESULT_FUNC_CODEC.optionalFieldOf(Constants.Recipe.SPONGE_RESULTFUNCTION).forGetter(ResultFunctionRecipe::resultFunctionId)
                    )
                    .apply($$0, SpongeSmithingRecipe::of)
    );

    private final String resultFunctionId;

    public static SpongeSmithingRecipe of(final String spongeType, final Optional<Ingredient> template, final Optional<Ingredient> base,
            final Optional<Ingredient> addition, final ItemStack resultIn, final Optional<String> resultFunctionId)
    {
        return new SpongeSmithingRecipe(template, base, addition, resultIn, resultFunctionId.orElse(null));
    }

    public SpongeSmithingRecipe(final Optional<Ingredient> template, final Optional<Ingredient> base,
            final Optional<Ingredient> addition, final ItemStack spongeResult, final String resultFunctionId) {
        super(template, base, addition, new TransmuteResult(spongeResult.getItemHolder(), spongeResult.getCount(), spongeResult.getComponentsPatch()));
        this.resultFunctionId = resultFunctionId;
    }

    @Override
    public Optional<String> resultFunctionId() {
        return Optional.ofNullable(this.resultFunctionId);
    }

    @Override
    public ItemStack assemble(final SmithingRecipeInput $$0, final HolderLookup.Provider $$1) {
        if (this.resultFunctionId != null) {
            return IngredientResultUtil.cachedResultFunction(this.resultFunctionId).apply($$0);
        }

        return super.assemble($$0, $$1);
    }

}
