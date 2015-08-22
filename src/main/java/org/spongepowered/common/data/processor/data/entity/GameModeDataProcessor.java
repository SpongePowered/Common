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
package org.spongepowered.common.data.processor.data.entity;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldSettings;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataTransactionBuilder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableGameModeData;
import org.spongepowered.api.data.manipulator.mutable.entity.GameModeData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.entity.player.gamemode.GameMode;
import org.spongepowered.common.Sponge;
import org.spongepowered.common.data.ImmutableDataCachingUtil;
import org.spongepowered.common.data.manipulator.immutable.entity.ImmutableSpongeGameModeData;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongeGameModeData;
import org.spongepowered.common.data.processor.common.AbstractSpongeDataProcessor;
import org.spongepowered.common.data.util.DataUtil;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;

public class GameModeDataProcessor extends AbstractSpongeDataProcessor<GameModeData, ImmutableGameModeData> {

    @Override
    public boolean supports(DataHolder dataHolder) {
        return dataHolder instanceof Player;
    }

    @Override
    public Optional<GameModeData> from(DataHolder dataHolder) {
        if (supports(dataHolder)) {
            if (dataHolder instanceof EntityPlayerMP) {
                return Optional.<GameModeData>of(new SpongeGameModeData(
                        (GameMode) (Object) ((EntityPlayerMP) dataHolder).theItemInWorldManager.getGameType()));
            }
        }
        return Optional.absent();
    }

    @Override
    public Optional<GameModeData> fill(DataHolder dataHolder, GameModeData manipulator, MergeFunction overlap) {
        if (dataHolder instanceof EntityPlayerMP) {
            final GameModeData original = from(dataHolder).get();
            return Optional.of(manipulator.set(Keys.GAME_MODE, overlap.merge(manipulator, original).type().get()));
        }
        return Optional.absent();
    }

    @Override
    public Optional<GameModeData> fill(DataContainer container, GameModeData gameModeData) {
        final String modeId = DataUtil.getData(container, Keys.GAME_MODE, String.class);
        final Optional<GameMode> optional = Sponge.getSpongeRegistry().getType(GameMode.class, modeId);
        if (optional.isPresent()) {
            gameModeData.set(Keys.GAME_MODE, optional.get());
        }
        return Optional.absent();
    }

    @Override
    public DataTransactionResult set(DataHolder dataHolder, GameModeData manipulator, MergeFunction function) {
        Preconditions.checkNotNull(function, "MergeFunction cannot be null!");
        if (dataHolder instanceof EntityPlayerMP) {
            final GameMode oldMode =
                    (GameMode) (Object) ((EntityPlayerMP) dataHolder).theItemInWorldManager.getGameType();
            final GameModeData oldData = from(dataHolder).get();
            final ImmutableValue<GameMode> newMode = function.merge(oldData, manipulator).type().asImmutable();
            try {
                ((EntityPlayerMP) dataHolder).setGameType((WorldSettings.GameType) (Object) newMode.get());
                return DataTransactionBuilder.successReplaceResult(ImmutableDataCachingUtil
                        .getWildValue(ImmutableSpongeValue.class, Keys.GAME_MODE, oldMode, oldMode), newMode);
            } catch (Exception e) {
                return DataTransactionBuilder.errorResult(newMode);
            }
        }
        return DataTransactionBuilder.failResult(manipulator.getValues());
    }

    @Override
    public Optional<ImmutableGameModeData> with(Key<? extends BaseValue<?>> key, Object value,
            ImmutableGameModeData immutable) {
        if (!key.equals(Keys.GAME_MODE)) {
            return Optional.absent();
        }
        final ImmutableGameModeData data = new ImmutableSpongeGameModeData((GameMode) value);
        return Optional.of(data);
    }

    @Override
    public DataTransactionResult remove(DataHolder dataHolder) {
        return DataTransactionBuilder.failNoData();
    }

    @Override
    public Optional<GameModeData> createFrom(DataHolder dataHolder) {
        return from(dataHolder);
    }
}
