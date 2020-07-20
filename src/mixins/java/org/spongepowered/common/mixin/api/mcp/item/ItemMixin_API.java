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
package org.spongepowered.common.mixin.api.mcp.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Optional;

@Mixin(Item.class)
public abstract class ItemMixin_API implements ItemType {

    @Shadow public abstract int shadow$getMaxStackSize();
    @Shadow public abstract String shadow$getTranslationKey();

    @Nullable protected BlockType blockType = null;

    private ResourceKey api$key;

    @Override
    public final ResourceKey getKey() {
        if (this.api$key == null) {
            this.api$key = (ResourceKey) (Object) Registry.ITEM.getKey((Item) (Object) this);
        }
        return this.api$key;
    }

    @Override
    public Component asComponent() {
        return TranslatableComponent.of(this.shadow$getTranslationKey());
    }

    @Override
    public int getMaxStackQuantity() {
        return this.shadow$getMaxStackSize();
    }

    @Override
    public Optional<BlockType> getBlock() {
        return Optional.ofNullable(this.blockType);
    }

}
