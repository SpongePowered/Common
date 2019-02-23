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
import net.minecraft.util.registry.IRegistry;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.ImmutableStoredEnchantmentData;
import org.spongepowered.api.data.manipulator.mutable.StoredEnchantmentData;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.common.data.manipulator.mutable.item.SpongeStoredEnchantmentData;
import org.spongepowered.common.item.enchantment.SpongeEnchantment;
import org.spongepowered.common.data.processor.common.AbstractItemSingleDataProcessor;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.data.value.SpongeValueFactory;

import java.util.List;
import java.util.Optional;

public class StoredEnchantmentDataProcessor extends
        AbstractItemSingleDataProcessor<List<Enchantment>, StoredEnchantmentData, ImmutableStoredEnchantmentData> {

    public StoredEnchantmentDataProcessor() {
        super(stack -> stack.getItem().equals(Items.ENCHANTED_BOOK), Keys.STORED_ENCHANTMENTS);
    }

    @Override
    protected Value.Mutable<List<Enchantment>> constructMutableValue(List<Enchantment> actualValue) {
        return SpongeValueFactory.getInstance().createListValue(Keys.STORED_ENCHANTMENTS, actualValue);
    }

    @Override
    protected boolean set(ItemStack entity, List<Enchantment> value) {
        if (!entity.hasTag()) {
            entity.setTag(new NBTTagCompound());
        }
        NBTTagList list = new NBTTagList();
        for (Enchantment enchantment : value) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.putString(NbtDataUtil.ITEM_ENCHANTMENT_ID, IRegistry.ENCHANTMENT.getKey((net.minecraft.enchantment.Enchantment) enchantment.getType()).toString());
            tag.putShort(NbtDataUtil.ITEM_ENCHANTMENT_LEVEL, (short) enchantment.getLevel());
            list.add(tag);
        }
        entity.getTag().put(NbtDataUtil.ITEM_STORED_ENCHANTMENTS_LIST, list);
        return true;
    }

    @Override
    protected Optional<List<Enchantment>> getVal(ItemStack entity) {
        if (!entity.hasTag() || !entity.getTag().contains(NbtDataUtil.ITEM_STORED_ENCHANTMENTS_LIST, NbtDataUtil.TAG_LIST)) {
            return Optional.empty();
        }
        List<Enchantment> list = Lists.newArrayList();
        NBTTagList tags = entity.getTag().getList(NbtDataUtil.ITEM_STORED_ENCHANTMENTS_LIST, NbtDataUtil.TAG_COMPOUND);
        for (int i = 0; i < tags.size(); i++) {
            NBTTagCompound tag = tags.getCompound(i);
            list.add(new SpongeEnchantment(
                    (EnchantmentType) IRegistry.ENCHANTMENT.get(tag.getShort(NbtDataUtil.ITEM_ENCHANTMENT_ID)),
                    tag.getShort(NbtDataUtil.ITEM_ENCHANTMENT_LEVEL)));
        }
        return Optional.of(list);
    }

    @Override
    protected Value.Immutable<List<Enchantment>> constructImmutableValue(List<Enchantment> value) {
        return constructMutableValue(value).asImmutable();
    }

    @Override
    protected StoredEnchantmentData createManipulator() {
        return new SpongeStoredEnchantmentData();
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        if (supports(container)) {
            ItemStack stack = (ItemStack) container;
            if (stack.hasTag() && stack.getTag().contains(NbtDataUtil.ITEM_STORED_ENCHANTMENTS_LIST, NbtDataUtil.TAG_COMPOUND)) {
                stack.getTag().remove(NbtDataUtil.ITEM_STORED_ENCHANTMENTS_LIST);
            }
            return DataTransactionResult.successNoData();
        }
        return DataTransactionResult.failNoData();
    }
}
