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

import net.minecraft.nbt.CompoundTag;
import org.spongepowered.api.data.persistence.DataContentUpdater;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.common.data.persistence.NBTTranslator;
import org.spongepowered.common.util.Constants;

public final class ItemStackSnapshotDuplicateManipulatorUpdater implements DataContentUpdater {

    public static final DataContentUpdater INSTANCE = new ItemStackSnapshotDuplicateManipulatorUpdater();
    @Override
    public int inputVersion() {
        return Constants.ItemStack.Data.DUPLICATE_MANIPULATOR_DATA_VERSION;
    }

    @Override
    public int outputVersion() {
        return Constants.ItemStack.Data.REMOVED_DUPLICATE_DATA;
    }

    @SuppressWarnings("deprecation")
    @Override
    public DataView update(DataView content) {
        if (content.contains(Constants.Sponge.UNSAFE_NBT)) {
            CompoundTag compound = NBTTranslator.INSTANCE.translate(content.getView(Constants.Sponge.UNSAFE_NBT).get());
            if (compound.contains(Constants.Sponge.Data.V2.SPONGE_DATA)) {
                final CompoundTag spongeCompound = compound.getCompound(Constants.Sponge.Data.V2.SPONGE_DATA);
                if (spongeCompound.contains(Constants.Sponge.Data.V2.CUSTOM_MANIPULATOR_TAG_LIST)) {
                    spongeCompound.remove(Constants.Sponge.Data.V2.CUSTOM_MANIPULATOR_TAG_LIST);
                }
            }
            Constants.NBT.filterSpongeCustomData(compound);
            content.remove(Constants.Sponge.UNSAFE_NBT);
            if (!compound.isEmpty()) {
                content.set(Constants.Sponge.UNSAFE_NBT, NBTTranslator.INSTANCE.translate(compound));
            }
        }
        content.set(Queries.CONTENT_VERSION, this.outputVersion());
        return content;
    }
}
