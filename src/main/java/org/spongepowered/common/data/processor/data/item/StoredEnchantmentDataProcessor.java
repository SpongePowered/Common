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
package org.spongepowered.common.data.processor.data.item;

import com.google.common.collect.Lists;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.item.ImmutableStoredEnchantmentData;
import org.spongepowered.api.data.manipulator.mutable.item.StoredEnchantmentData;
import org.spongepowered.api.data.meta.ItemEnchantment;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.item.Enchantment;
import org.spongepowered.common.data.manipulator.mutable.item.SpongeStoredEnchantmentData;
import org.spongepowered.common.data.processor.common.AbstractItemSingleDataProcessor;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.data.value.SpongeValueFactory;

import java.util.List;
import java.util.Optional;

public class StoredEnchantmentDataProcessor extends AbstractItemSingleDataProcessor<List<ItemEnchantment>, ListValue<ItemEnchantment>,
        StoredEnchantmentData, ImmutableStoredEnchantmentData> {

    public StoredEnchantmentDataProcessor() {
        super(stack -> stack.getItem().equals(Items.ENCHANTED_BOOK), Keys.STORED_ENCHANTMENTS);
    }

    @Override
    protected ListValue<ItemEnchantment> constructValue(List<ItemEnchantment> actualValue) {
        return SpongeValueFactory.getInstance().createListValue(Keys.STORED_ENCHANTMENTS, actualValue, Lists.newArrayList());
    }

    @Override
    protected boolean set(ItemStack entity, List<ItemEnchantment> value) {
        if (!entity.hasTagCompound()) {
            entity.setTagCompound(new NBTTagCompound());
        }
        NBTTagList list = new NBTTagList();
        for (ItemEnchantment enchantment : value) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setShort(NbtDataUtil.ITEM_ENCHANTMENT_ID, (short) net.minecraft.enchantment.Enchantment
                    .getEnchantmentID((net.minecraft.enchantment.Enchantment) enchantment.getEnchantment()));
            tag.setShort(NbtDataUtil.ITEM_ENCHANTMENT_LEVEL, (short) enchantment.getLevel());
            list.appendTag(tag);
        }
        entity.getTagCompound().setTag(NbtDataUtil.ITEM_STORED_ENCHANTMENTS_LIST, list);
        return true;
    }

    @Override
    protected Optional<List<ItemEnchantment>> getVal(ItemStack entity) {
        if (!entity.hasTagCompound() || !entity.getTagCompound().hasKey(NbtDataUtil.ITEM_STORED_ENCHANTMENTS_LIST, NbtDataUtil.TAG_LIST)) {
            return Optional.empty();
        }
        List<ItemEnchantment> list = Lists.newArrayList();
        NBTTagList tags = entity.getTagCompound().getTagList(NbtDataUtil.ITEM_STORED_ENCHANTMENTS_LIST, NbtDataUtil.TAG_COMPOUND);
        for (int i = 0; i < tags.tagCount(); i++) {
            NBTTagCompound tag = tags.getCompoundTagAt(i);
            list.add(new ItemEnchantment(
                    (Enchantment) net.minecraft.enchantment.Enchantment.getEnchantmentByID(tag.getShort(NbtDataUtil.ITEM_ENCHANTMENT_ID)),
                    tag.getShort(NbtDataUtil.ITEM_ENCHANTMENT_LEVEL)));
        }
        return Optional.of(list);
    }

    @Override
    protected ImmutableValue<List<ItemEnchantment>> constructImmutableValue(List<ItemEnchantment> value) {
        return constructValue(value).asImmutable();
    }

    @Override
    protected StoredEnchantmentData createManipulator() {
        return new SpongeStoredEnchantmentData();
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        if (supports(container)) {
            ItemStack stack = (ItemStack) container;
            if (stack.hasTagCompound() && stack.getTagCompound().hasKey(NbtDataUtil.ITEM_STORED_ENCHANTMENTS_LIST, NbtDataUtil.TAG_COMPOUND)) {
                stack.getTagCompound().removeTag(NbtDataUtil.ITEM_STORED_ENCHANTMENTS_LIST);
            }
            return DataTransactionResult.successNoData();
        }
        return DataTransactionResult.failNoData();
    }
}
