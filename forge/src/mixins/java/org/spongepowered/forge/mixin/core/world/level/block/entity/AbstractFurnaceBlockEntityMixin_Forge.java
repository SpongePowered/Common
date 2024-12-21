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
package org.spongepowered.forge.mixin.core.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.entity.carrier.furnace.FurnaceBlockEntity;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.entity.CookingEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.item.recipe.cooking.CookingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.block.entity.AbstractFurnaceBlockEntityBridge;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.inventory.adapter.impl.slots.SlotAdapter;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin_Forge implements AbstractFurnaceBlockEntityBridge {

    // @formatter:off
    @Shadow protected NonNullList<ItemStack> items;
    @Shadow int cookingProgress;
    @Shadow private boolean shadow$canBurn(RegistryAccess registryAccess, @Nullable RecipeHolder<?> recipe, SingleRecipeInput input, NonNullList<ItemStack> slots, int maxStackSize) {
        throw new UnsupportedOperationException("Shadowed canBurn");
    }
    // @formatter:on

    private boolean forge$filledWaterBucket;

    // Tick up and Start
    @Redirect(method = "serverTick",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;canBurn(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/crafting/RecipeHolder;Lnet/minecraft/world/item/crafting/SingleRecipeInput;Lnet/minecraft/core/NonNullList;I)Z",
            ordinal = 1))
    private static boolean forge$checkIfCanSmelt(final AbstractFurnaceBlockEntity entityIn,
                                                 final RegistryAccess registryAccess,
                                                 final RecipeHolder<?> recipe,
                                                 final SingleRecipeInput input,
                                                 final NonNullList<ItemStack> slots,
                                                 final int maxStackSize) {
        final var entity = (AbstractFurnaceBlockEntityMixin_Forge) (Object) (entityIn);
        if (!entity.shadow$canBurn(registryAccess, recipe, input, slots, maxStackSize)) {
            return false;
        }

        final ItemStackSnapshot fuel = ItemStackUtil.snapshotOf(slots.get(1));

        final Cause cause = PhaseTracker.getCauseStackManager().currentCause();
        if (entity.cookingProgress == 0) { // Start
            final CookingEvent.Start event = SpongeEventFactory.createCookingEventStart(cause, (FurnaceBlockEntity) entityIn, Optional.of(fuel),
                Optional.of((CookingRecipe) recipe.value()), Optional.of((ResourceKey) (Object) recipe.id()));
            SpongeCommon.post(event);
            return !event.isCancelled();
        } else { // Tick up
            final ItemStackSnapshot cooking = ItemStackUtil.snapshotOf(entity.items.get(0));
            final CookingEvent.Tick event = SpongeEventFactory.createCookingEventTick(cause, (FurnaceBlockEntity) entityIn, cooking, Optional.of(fuel),
                Optional.of((CookingRecipe) recipe.value()), Optional.of((ResourceKey) (Object) recipe.id()));
            SpongeCommon.post(event);
            return !event.isCancelled();
        }
    }

    // Tick down
    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"))
    private static int forge$resetCookTimeIfCancelled(final int newCookTime, final int zero, final int totalCookTime,
                                                      final ServerLevel level, final BlockPos entityPos, final BlockState state, final AbstractFurnaceBlockEntity entityIn) {
        final int clampedCookTime = Mth.clamp(newCookTime, zero, totalCookTime);
        final var entity = (AbstractFurnaceBlockEntityMixin_Forge) (Object) entityIn;
        final ItemStackSnapshot fuel = ItemStackUtil.snapshotOf(entity.items.get(1));
        final Cause cause = PhaseTracker.getCauseStackManager().currentCause();
        final var recipe = entity.bridge$getCurrentRecipe();
        final ItemStackSnapshot cooking = ItemStackUtil.snapshotOf(entity.items.get(0));
        final CookingEvent.Tick event = SpongeEventFactory.createCookingEventTick(cause, (FurnaceBlockEntity) entityIn, cooking, Optional.of(fuel),
                recipe.map(r -> (CookingRecipe) r.value()), recipe.map(r -> (ResourceKey) (Object) r.id()));
        SpongeCommon.post(event);
        if (event.isCancelled()) {
            return entity.cookingProgress; // dont tick down
        }

        return clampedCookTime;
    }

    // Finish
    @Inject(method = "burn", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"),
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/Blocks;WET_SPONGE:Lnet/minecraft/world/level/block/Block;", opcode = Opcodes.GETSTATIC)
        ))
    private void forge$captureBucketFill(final RegistryAccess registryAccess, final RecipeHolder<?> recipe, final SingleRecipeInput input, final NonNullList<ItemStack> slots, final int maxStackSize, final CallbackInfoReturnable<Boolean> cir) {
        this.forge$filledWaterBucket = true;
    }

    @Inject(
        method = "burn",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void forge$afterSmeltItem(
        final RegistryAccess registryAccess, final RecipeHolder<?> recipe, final SingleRecipeInput input,
        final NonNullList<ItemStack> slots, final int maxStackSize,
        final CallbackInfoReturnable<Boolean> cir,
        final ItemStack itemIn, final ItemStack recipeResult, final ItemStack itemOut
    ) {
        final FurnaceBlockEntity entity = (FurnaceBlockEntity) this;

        final List<SlotTransaction> transactions = new ArrayList<>();
        itemIn.grow(1);
        final ItemStackSnapshot originalSmeltItem = ItemStackUtil.snapshotOf(itemIn);
        itemIn.shrink(1);
        transactions.add(new SlotTransaction(entity.inventory().slot(0).get(), originalSmeltItem, ItemStackUtil.snapshotOf(itemIn)));

        final boolean hasFuel = !this.forge$filledWaterBucket;
        if (this.forge$filledWaterBucket) {
            transactions.add(new SlotTransaction(entity.inventory().slot(1).get(), ItemStackSnapshot.empty(), ItemStackUtil.snapshotOf(slots.get(1))));
        }
        this.forge$filledWaterBucket = false;

        if (itemOut.isEmpty()) {
            transactions.add(new SlotTransaction(entity.inventory().slot(2).get(), ItemStackSnapshot.empty(), ItemStackUtil.snapshotOf(recipeResult)));
        } else if (ItemStack.isSameItemSameComponents(itemOut, recipeResult)) {
            itemOut.shrink(1);
            final ItemStackSnapshot originalResult = ItemStackUtil.snapshotOf(itemOut);
            itemOut.grow(1);
            transactions.add(new SlotTransaction(entity.inventory().slot(2).get(), originalResult, ItemStackUtil.snapshotOf(itemOut)));
        }
        final Optional<ItemStackSnapshot> fuel = hasFuel && !slots.get(1).isEmpty() ? Optional.of(ItemStackUtil.snapshotOf(slots.get(1))) : Optional.empty();
        final CookingEvent.Finish event = SpongeEventFactory.createCookingEventFinish(PhaseTracker.getCauseStackManager().currentCause(), entity,
            fuel, Optional.of((CookingRecipe) recipe.value()), Optional.of((ResourceKey) (Object) recipe.id()), Collections.unmodifiableList(transactions));
        SpongeCommon.post(event);

        for (final SlotTransaction transaction : transactions) {
            transaction.custom().ifPresent(item -> slots.set(((SlotAdapter) transaction.slot()).getOrdinal(), ItemStackUtil.fromSnapshotToNative(item)));
        }
    }
}
