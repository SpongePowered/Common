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
package org.spongepowered.common.mixin.core.world.item;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.bridge.data.SpongeDataHolderBridge;
import org.spongepowered.common.bridge.data.DataCompoundHolder;
import org.spongepowered.common.bridge.world.WorldBridge;
import org.spongepowered.common.data.DataUtil;
import org.spongepowered.common.data.provider.nbt.NBTDataType;
import org.spongepowered.common.data.provider.nbt.NBTDataTypes;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;

@Mixin(net.minecraft.world.item.ItemStack.class)
public abstract class ItemStackMixin implements SpongeDataHolderBridge, DataCompoundHolder {

    // @formatter:off
    @Shadow private boolean emptyCacheFlag;
    @Shadow private CompoundTag tag;

    @Shadow public abstract void shadow$setTag(@Nullable CompoundTag nbt);
    @Shadow @Nullable public abstract CompoundTag shadow$getTag();
    // @formatter:on

    @Override
    public CompoundTag data$getCompound() {
        return this.shadow$getTag();
    }

    @Override
    public void data$setCompound(final CompoundTag nbt) {
        this.shadow$setTag(nbt);
    }

    // Add our manipulators when creating copies from this ItemStack:
    @SuppressWarnings("ConstantConditions")
    @Inject(method = "copy", at = @At("RETURN"))
    private void impl$onCopy(final CallbackInfoReturnable<ItemStack> info) {
        ((SpongeDataHolderBridge) (Object) info.getReturnValue()).bridge$mergeDeserialized(this.bridge$getManipulator());
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "split", at = @At("RETURN"))
    private void impl$onSplit(final int amount, final CallbackInfoReturnable<net.minecraft.world.item.ItemStack> info) {
        ((SpongeDataHolderBridge) (Object) info.getReturnValue()).bridge$mergeDeserialized(this.bridge$getManipulator());
    }

    // Read custom data from nbt
    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void impl$onRead(final CompoundTag compound, final CallbackInfo info) {
        if (!this.emptyCacheFlag) {
            DataUtil.syncTagToData(this); // Deserialize
            DataUtil.syncDataToTag(this); // Sync back after reading
        }
        // Prune empty stack tag compound if its empty to enable stacking.
        if (this.tag != null && this.tag.isEmpty()) {
            this.tag = null;
        }
    }

    @Inject(method = "setTag", at = @At("RETURN"))
    private void impl$onSet(final CompoundTag compound, final CallbackInfo callbackInfo) {
        if (this.shadow$getTag() != compound) {
            this.bridge$clear();
        }
        if (compound != null && !compound.isEmpty()) {
            DataUtil.syncTagToData(this); // Deserialize
            DataUtil.syncDataToTag(this); // Sync back after reading
        }
    }


    @Inject(method = "mineBlock", at = @At("HEAD"))
    private void impl$capturePlayerUsingItemstack(final Level worldIn, final BlockState blockIn, final BlockPos pos, final Player playerIn,
        final CallbackInfo ci) {
        if (!((WorldBridge) worldIn).bridge$isFake()) {
            final PhaseContext<@NonNull ?> context = PhaseTracker.getInstance().getPhaseContext();
            context.capturePlayerUsingStackToBreakBlock((org.spongepowered.api.item.inventory.ItemStack) this, (ServerPlayer) playerIn);
        }
    }

    @Inject(method = "mineBlock", at = @At("RETURN"))
    private void impl$nullOutCapturedPlayer(final Level worldIn, final BlockState blockIn, final BlockPos pos, final Player playerIn,
        final CallbackInfo ci) {
        if (!((WorldBridge) worldIn).bridge$isFake()) {
            final PhaseContext<@NonNull ?> context = PhaseTracker.getInstance().getPhaseContext();
            context.capturePlayerUsingStackToBreakBlock((org.spongepowered.api.item.inventory.ItemStack) this, null);
        }
    }

    @Override
    public NBTDataType data$getNBTDataType() {
        return NBTDataTypes.ITEMSTACK;
    }
}
