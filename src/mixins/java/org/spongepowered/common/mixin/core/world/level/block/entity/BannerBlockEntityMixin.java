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


import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.common.bridge.CustomNameableBridge;
import org.spongepowered.common.bridge.world.level.block.entity.BannerBlockEntityBridge;

import java.util.Objects;

import javax.annotation.Nullable;

@Mixin(BannerBlockEntity.class)
public abstract class BannerBlockEntityMixin extends BlockEntityMixin implements BannerBlockEntityBridge, CustomNameableBridge {

    @Shadow @Final @Mutable private net.minecraft.world.item.DyeColor baseColor;
    @Shadow @Nullable private Component name;

    @Unique
    private void impl$markDirtyAndUpdate() {
        this.shadow$setChanged();
        if (this.level != null && !this.level.isClientSide) {
            ((ServerLevel) this.level).getChunkSource().blockChanged(this.shadow$getBlockPos());
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public DyeColor bridge$getBaseColor() {
        return (DyeColor) (Object) this.baseColor;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void bridge$setBaseColor(final DyeColor baseColor) {
        Objects.requireNonNull(baseColor, "Null DyeColor!");
        try {
            this.baseColor = (net.minecraft.world.item.DyeColor) (Object) baseColor;
        } catch (final Exception e) {
            this.baseColor = net.minecraft.world.item.DyeColor.BLACK;
        }
        this.impl$markDirtyAndUpdate();
    }

    @Override
    public void bridge$setCustomDisplayName(final Component customName) {
        this.name = customName;
    }
}
