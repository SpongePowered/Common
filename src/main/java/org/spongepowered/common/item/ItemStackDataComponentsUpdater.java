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
package org.spongepowered.common.item;

import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataContentUpdater;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.common.data.persistence.NBTTranslator;
import org.spongepowered.common.util.Constants;

class ItemStackDataComponentsUpdater implements DataContentUpdater {

    public static final DataContentUpdater INSTANCE = new ItemStackDataComponentsUpdater();

    @Override
    public int inputVersion() {
        return Constants.ItemStack.Data.REMOVED_DUPLICATE_DATA;
    }

    @Override
    public int outputVersion() {
        return Constants.ItemStack.Data.DATA_COMPONENTS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public DataView update(final DataView content) {
        final int count = content.getInt(Constants.ItemStack.V2.COUNT).get();
        final String type = content.getString(Constants.ItemStack.V2.TYPE).get();

        final DataContainer updated = DataContainer.createNew();
        updated.set(Constants.ItemStack.TYPE, type);
        updated.set(Constants.ItemStack.COUNT, count);

        final DataContainer components = DataContainer.createNew();
        content.getInt(Constants.ItemStack.V2.DAMAGE_VALUE).filter(dmg -> dmg != 0).ifPresent(dmg -> components.set(Constants.ItemStack.DAMAGE, dmg));

        content.getView(Constants.Sponge.UNSAFE_NBT).ifPresent(unsafe -> {
            // Get unsafe nbt data
            final CompoundTag tag = NBTTranslator.INSTANCE.translate(unsafe);
            // Build full ItemStack nbt
            final CompoundTag itemStackTag = new CompoundTag();
            itemStackTag.putString("id", type);
            itemStackTag.putInt("Count", count);
            itemStackTag.put("tag", tag);
            // Call the vanilla DataFixer
            var dataFixed = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, itemStackTag), 3692, 3833);
            // Convert the components back to our format
            var newComponents = NBTTranslator.INSTANCE.translate((CompoundTag) dataFixed.getElement("components", new CompoundTag()));
            newComponents.values(false).forEach(components::set);
        });

        updated.set(Constants.ItemStack.COMPONENTS, components);
        updated.set(Queries.CONTENT_VERSION, this.outputVersion());

        return updated;
    }
}
