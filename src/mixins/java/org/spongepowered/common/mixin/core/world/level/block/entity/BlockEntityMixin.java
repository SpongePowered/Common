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
package org.spongepowered.common.mixin.core.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.data.DataCompoundHolder;
import org.spongepowered.common.bridge.world.level.block.entity.BlockEntityBridge;
import org.spongepowered.common.data.DataUtil;
import org.spongepowered.common.data.provider.nbt.NBTDataType;
import org.spongepowered.common.data.provider.nbt.NBTDataTypes;

import java.util.StringJoiner;

@Mixin(net.minecraft.world.level.block.entity.BlockEntity.class)
public abstract class BlockEntityMixin implements BlockEntityBridge, DataCompoundHolder {

    //@formatter:off
    @Shadow @Final private BlockEntityType<?> type;
    @Shadow @Nullable private BlockState blockState;
    @Shadow protected net.minecraft.world.level.Level level;
    @Shadow @Final protected BlockPos worldPosition;

    @Shadow public abstract BlockPos shadow$getBlockPos();
    @Shadow public abstract BlockState shadow$getBlockState();
    @Shadow public abstract void shadow$setChanged();
    //@formatter:on

    private CompoundTag impl$customData;

    @Override
    public CompoundTag data$getCompound() {
        return this.impl$customData;
    }

    @Override
    public void data$setCompound(final CompoundTag nbt) {
        this.impl$customData = nbt;
    }

    @Override
    public NBTDataType data$getNBTDataType() {
        return NBTDataTypes.BLOCK_ENTITY;
    }

    @Inject(method = "saveMetadata", at = @At("RETURN"))
    private void impl$writeSpongeData(final CompoundTag $$0, final CallbackInfo ci) {
        if (DataUtil.syncDataToTag(this)) {
            $$0.merge(this.data$getCompound());
        }
    }

    @Inject(method = "loadWithComponents", at = @At("RETURN"))
    private void impl$readSpongeData(final CompoundTag compound, HolderLookup.Provider $$1, final CallbackInfo ci) {
        // TODO If we are in Forge data is already present
        this.data$setCompound(compound); // For vanilla we set the incoming nbt
        // Deserialize custom data...
        DataUtil.syncTagToData(this);
        this.data$setCompound(null); // done reading
    }

    @Override
    public String toString() {
        final ResourceKey key = (ResourceKey) (Object) BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.type);

        return new StringJoiner(", ", BlockEntityMixin.class.getSimpleName() + "[", "]")
                .add("type=" + key)
                .add("world=" + this.level)
                .add("pos=" + this.worldPosition)
                .add("blockstate=" + this.blockState)
                .toString();
    }

    protected StringJoiner getPrettyPrinterStringHelper() {
        final ResourceKey key = (ResourceKey) (Object) BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.type);

        return new StringJoiner(", ", BlockEntityMixin.class.getSimpleName() + "[", "]")
                .add("type=" + key)
                .add("world=" + ((ServerWorld) this.level).key())
                .add("pos=" + this.worldPosition);
    }

    @Override
    public String bridge$getPrettyPrinterString() {
        return this.getPrettyPrinterStringHelper().toString();
    }
}
