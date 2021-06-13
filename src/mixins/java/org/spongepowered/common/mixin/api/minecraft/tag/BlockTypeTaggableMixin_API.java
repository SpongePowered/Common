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
package org.spongepowered.common.mixin.api.minecraft.tag;

import net.minecraft.world.level.block.Block;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.tag.Tag;
import org.spongepowered.api.tag.TagType;
import org.spongepowered.api.tag.TagTypes;
import org.spongepowered.api.tag.Taggable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.tag.TagUtil;

import java.util.Collection;

@Mixin(Block.class)
public abstract class BlockTypeTaggableMixin_API implements Taggable<BlockType> {

    @Override
    public TagType<BlockType> tagType() {
        return TagTypes.BLOCK_TYPE.get();
    }

    @Override
    public Collection<Tag<BlockType>> tags() {
        return TagUtil.getAssociatedTags((BlockType) this, RegistryTypes.BLOCK_TYPE_TAGS);
    }
}
