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
package org.spongepowered.common.mixin.core.item.inventory;

import static org.spongepowered.api.data.DataQuery.of;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataManipulator;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.manipulator.DisplayNameData;
import org.spongepowered.api.data.manipulator.item.AuthorData;
import org.spongepowered.api.data.manipulator.item.DurabilityData;
import org.spongepowered.api.data.manipulator.item.PagedData;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.persistence.InvalidDataException;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.data.SpongeManipulatorRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("serial")
@NonnullByDefault
@Mixin(net.minecraft.item.ItemStack.class)
public abstract class MixinItemStack implements ItemStack {

    @Shadow public int stackSize;

    @Shadow public abstract int getItemDamage();
    @Shadow public abstract void setItemDamage(int meta);
    @Shadow public abstract int getMaxStackSize();
    @Shadow public abstract NBTTagCompound getTagCompound();
    @Shadow(prefix = "shadow$")
    public abstract Item shadow$getItem();

    @Override
    public ItemType getItem() {
        return (ItemType) shadow$getItem();
    }

    @Override
    public int getQuantity() {
        return this.stackSize;
    }

    @Override
    public void setQuantity(int quantity) throws IllegalArgumentException {
        if (quantity > this.getMaxStackQuantity()) {
            throw new IllegalArgumentException("Quantity (" + quantity + ") exceeded the maximum stack size (" + this.getMaxStackQuantity() + ")");
        } else {
            this.stackSize = quantity;
        }
    }

    @Override
    public int getMaxStackQuantity() {
        return getMaxStackSize();
    }

    @Override
    public Collection<DataManipulator<?>> getManipulators() {
        final List<DataManipulator<?>> manipulators = Lists.newArrayList();
        final Optional<DisplayNameData> optDisplayName = SpongeManipulatorRegistry.getInstance().getUtil(DisplayNameData.class).get().createFrom
                (this);
        if (optDisplayName.isPresent()) {
            manipulators.add(optDisplayName.get());
        }
        if (getItem() == Items.written_book || getItem() == Items.writable_book) {
            final Optional<AuthorData> optAuthor = SpongeManipulatorRegistry.getInstance().getUtil(AuthorData.class).get().createFrom(this);
            final Optional<PagedData> optPages = SpongeManipulatorRegistry.getInstance().getUtil(PagedData.class).get().createFrom(this);
            if (optAuthor.isPresent()) {
                manipulators.add(optAuthor.get());
            }
            if (optPages.isPresent()) {
                manipulators.add(optPages.get());
            }
        } else if (getItem() instanceof ItemTool || getItem() instanceof ItemArmor || getItem() instanceof ItemSword) {
            manipulators.add(SpongeManipulatorRegistry.getInstance().getUtil(DurabilityData.class).get().createFrom(this).get());
        }

        return Collections.unmodifiableList(manipulators);
    }

    @Override
    public boolean validateRawData(DataContainer container) {
        return false;
    }

    @Override
    public void setRawData(DataContainer container) throws InvalidDataException {

    }

    @Override
    public DataContainer toContainer() {
        return new MemoryDataContainer()
                .set(of("ItemType"), this.getItem().getId())
                .set(of("Quantity"), this.getQuantity())
                .set(of("Data"), getManipulators());
    }
}
