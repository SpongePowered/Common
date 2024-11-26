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
package org.spongepowered.common.mixin.api.minecraft.world.level.block.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.api.block.entity.carrier.furnace.FurnaceBlockEntity;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.item.recipe.crafting.RecipeInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.util.Constants;

import java.util.Set;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin_API extends BaseContainerBlockEntityMixin_API implements FurnaceBlockEntity {

    // @formatter:off
    @Shadow int litTimeRemaining;
    @Shadow int litTotalTime;
    @Shadow int cookingTimer;
    @Shadow int cookingTotalTime;
    @Shadow protected NonNullList<ItemStack> items;
    // @formatter:on

    @Override
    protected Set<Value.Immutable<?>> api$getVanillaValues() {
        final Set<Value.Immutable<?>> values = super.api$getVanillaValues();

        values.add(this.remainingFuel().asImmutable());
        values.add(this.maxBurnTime().asImmutable());
        values.add(this.passedCookTime().asImmutable());
        values.add(this.maxCookTime().asImmutable());

        return values;
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
            .set(Constants.TileEntity.Furnace.BURN_TIME, this.litTimeRemaining)
            .set(Constants.TileEntity.Furnace.BURN_TIME_TOTAL, this.litTotalTime)
            .set(Constants.TileEntity.Furnace.COOK_TIME, this.cookingTimer)
            .set(Constants.TileEntity.Furnace.COOK_TIME_TOTAL, this.cookingTotalTime);
    }

    @Override
    public RecipeInput.Single asRecipeInput() {
        return (RecipeInput.Single) (Object) new SingleRecipeInput(this.items.get(0));
    }
}
