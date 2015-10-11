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
package org.spongepowered.common.data.processor.value.item;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.data.DataTransactionBuilder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.common.Sponge;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;

public class ItemDisplayNameValueProcessor extends AbstractSpongeValueProcessor<ItemStack, Text, Value<Text>> {

    public ItemDisplayNameValueProcessor() {
        super(ItemStack.class, Keys.DISPLAY_NAME);
    }

    @Override
    protected Value<Text> constructValue(Text defaultValue) {
        return new SpongeValue<>(Keys.DISPLAY_NAME, Texts.of(), defaultValue);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean set(ItemStack container, Text value) {
        final String legacy = Texts.legacy().to(value);
        if (container.getItem() == Items.written_book) {
            NBTTagCompound mainCompound = container.getTagCompound();
            mainCompound.setString(NbtDataUtil.ITEM_BOOK_TITLE, legacy);
        } else {
            container.setStackDisplayName(legacy);
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Optional<Text> getVal(ItemStack container) {
        if (container.getItem() == Items.written_book) {
            final NBTTagCompound mainCompound = container.getTagCompound();
            final String titleString = mainCompound.getString(NbtDataUtil.ITEM_BOOK_TITLE);
            return Optional.of(Texts.legacy().fromUnchecked(titleString));
        }
        final NBTTagCompound mainCompound = container.getSubCompound(NbtDataUtil.ITEM_DISPLAY, false);
        if (mainCompound != null && mainCompound.hasKey(NbtDataUtil.ITEM_DISPLAY_NAME, NbtDataUtil.TAG_STRING)) {
            final String displayString = mainCompound.getString(NbtDataUtil.ITEM_DISPLAY_NAME);
            return Optional.of(Texts.legacy().fromUnchecked(displayString));
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected ImmutableValue<Text> constructImmutableValue(Text value) {
        return new ImmutableSpongeValue<>(Keys.DISPLAY_NAME, Texts.of(), value);
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        if (container instanceof ItemStack) {
            final DataTransactionBuilder builder = DataTransactionBuilder.builder();
            final Optional<Text> optional = getValueFromContainer(container);
            if (optional.isPresent()) {
                try {
                    ((ItemStack) container).clearCustomName();
                    return builder.replace(new ImmutableSpongeValue<>(Keys.DISPLAY_NAME, optional.get())).result(DataTransactionResult.Type.SUCCESS).build();
                } catch (Exception e) {
                    Sponge.getLogger().error("There was an issue removing the displayname from an itemstack!", e);
                    return builder.result(DataTransactionResult.Type.ERROR).build();
                }
            } else {
                return builder.result(DataTransactionResult.Type.SUCCESS).build();
            }
        }
        return DataTransactionBuilder.failNoData();    }
}
