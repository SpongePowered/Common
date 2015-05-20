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

package org.spongepowered.common.data.utils.entities;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spongepowered.common.data.DataTransactionBuilder.fail;

import com.google.common.base.Optional;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataPriority;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.entity.SkinData;
import org.spongepowered.api.service.persistence.InvalidDataException;
import org.spongepowered.common.data.SpongeDataProcessor;
import org.spongepowered.common.data.manipulators.entities.SpongeSkinData;
import org.spongepowered.common.entity.living.HumanEntity;

import java.util.UUID;

public class SpongeSkinDataProcessor implements SpongeDataProcessor<SkinData> {

    @Override
    public Optional<SkinData> build(DataView container) throws InvalidDataException {
        DataQuery skinQuery = DataQuery.of("skinUuid");
        if (container.contains(skinQuery)) {
            Object obj = container.get(skinQuery).get();
            SkinData skinData = null;
            if (obj instanceof String) {
                obj = UUID.fromString((String) obj);
            }
            if (obj instanceof UUID) {
                skinData = new SpongeSkinData((UUID) obj);
            }
            return Optional.fromNullable(skinData);
        }
        return Optional.absent();
    }

    @Override
    public SkinData create() {
        return new SpongeSkinData(new UUID(0, 0));
    }

    @Override
    public Optional<SkinData> createFrom(DataHolder dataHolder) {
        if (!(dataHolder instanceof HumanEntity)) {
            return Optional.absent();
        }
        return Optional.of(((HumanEntity) dataHolder).createSkinData());
    }

    @Override
    public Optional<SkinData> getFrom(DataHolder holder) {
        if (!(holder instanceof HumanEntity)) {
            return Optional.absent();
        }
        return ((HumanEntity) holder).getSkinData();
    }

    @Override
    public Optional<SkinData> fillData(DataHolder holder, SkinData manipulator, DataPriority priority) {
        if (!(holder instanceof HumanEntity)) {
            return Optional.absent();
        }
        switch (checkNotNull(priority)) {
            case DATA_HOLDER:
            case PRE_MERGE:
                return ((HumanEntity) holder).fillSkinData(manipulator);
            default:
                return Optional.absent();
        }
    }

    @Override
    public DataTransactionResult setData(DataHolder dataHolder, SkinData manipulator, DataPriority priority) {
        if (dataHolder instanceof HumanEntity) {
            switch (checkNotNull(priority)) {
                case DATA_MANIPULATOR:
                    return ((HumanEntity) dataHolder).setSkinData(manipulator);
                case DATA_HOLDER:
                case POST_MERGE:
                    // TODO
                default:
                    return fail(manipulator);
            }
        }
        return fail(manipulator);
    }

    @Override
    public boolean remove(DataHolder dataHolder) {
        if (!(dataHolder instanceof HumanEntity)) {
            return false;
        }
        return ((HumanEntity) dataHolder).removeSkin();
    }

}
