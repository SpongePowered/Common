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
package org.spongepowered.common.data.processor.common;

import com.google.common.collect.Sets;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.common.Sponge;
import org.spongepowered.common.data.util.NbtDataUtil;

import java.util.Optional;
import java.util.Set;

public final class BreakablePlaceableUtils {

    public static boolean set(ItemStack stack, String nbtKey, Set<BlockType> value) {
        NBTTagCompound stackTag = stack.getTagCompound();

        if (value.isEmpty()) {
            if (stackTag != null) {
                stackTag.removeTag(nbtKey);
                if (stackTag.hasNoTags()) {
                    stack.setTagCompound(null);
                }
            }
        } else {
            NBTTagList breakableIds = new NBTTagList();
            for (BlockType breakable : value) {
                String id = breakable.getId();
                if (id.startsWith("minecraft:")) {
                    id = id.substring("minecraft:".length());
                }
                breakableIds.appendTag(new NBTTagString(id));
            }
            if (stackTag == null) {
                stackTag = new NBTTagCompound();
                stack.setTagCompound(stackTag);
            }
            stackTag.setTag(nbtKey, breakableIds);
        }

        return true;
    }

    public static Optional<Set<BlockType>> get(ItemStack stack, String nbtKey) {
    	NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return Optional.empty();
        }
        NBTTagList blockIds = tag.getTagList(nbtKey, NbtDataUtil.TAG_STRING);
        if (blockIds.hasNoTags()) {
            return Optional.empty();
        }
        Set<BlockType> blockTypes = Sets.newHashSetWithExpectedSize(blockIds.tagCount());
        for (int i = 0; i < blockIds.tagCount(); i++) {
            Optional<BlockType> blockType = Sponge.getGame().getRegistry()
                    .getType(BlockType.class, blockIds.getStringTagAt(i));
            if (blockType.isPresent()) {
                blockTypes.add(blockType.get());
            }
        }
        return Optional.of(blockTypes);
    }

}
