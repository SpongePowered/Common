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
package org.spongepowered.common.data.manipulator.mutable.entity;

import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableSkinData;
import org.spongepowered.api.data.manipulator.mutable.entity.SkinData;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.common.data.manipulator.immutable.entity.ImmutableSpongeSkinData;
import org.spongepowered.common.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.common.data.processor.data.entity.SkinDataProcessor;
import org.spongepowered.common.data.util.DataQueries;
import org.spongepowered.common.data.value.mutable.SpongeValue;

public class SpongeSkinData extends AbstractData<SkinData, ImmutableSkinData> implements SkinData {

    private ProfileProperty skin;
    private boolean updateGameProfile;

    public SpongeSkinData() {
        this(SkinDataProcessor.EMPTY_SKIN, false);
    }

    public SpongeSkinData(ProfileProperty skin, boolean updateGameProfile) {
        super(SkinData.class);
        this.skin = skin;
        this.updateGameProfile = updateGameProfile;
        this.registerGettersAndSetters();
    }

    @Override
    public SkinData copy() {
        return new SpongeSkinData(this.skin, this.updateGameProfile);
    }

    @Override
    public ImmutableSkinData asImmutable() {
        return new ImmutableSpongeSkinData(this.skin, this.updateGameProfile);
    }

    @Override
    protected void registerGettersAndSetters() {
        registerFieldGetter(Keys.SKIN, () -> this.skin);
        registerFieldSetter(Keys.SKIN, (skin) -> this.skin = skin);
        registerKeyValue(Keys.SKIN, this::skin);

        registerFieldGetter(Keys.UPDATE_GAME_PROFILE, () -> this.updateGameProfile);
        registerFieldSetter(Keys.UPDATE_GAME_PROFILE, (updateTabList) -> this.updateGameProfile = updateTabList);
        registerKeyValue(Keys.UPDATE_GAME_PROFILE, this::updateGameProfile);
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
                .set(DataQueries.SKIN, this.skin)
                .set(DataQueries.UPDATE_TAB_LIST, this.updateGameProfile);
    }

    @Override
    public Value<ProfileProperty> skin() {
        return new SpongeValue<>(Keys.SKIN, SkinDataProcessor.EMPTY_SKIN, this.skin);
    }

    @Override
    public Value<Boolean> updateGameProfile() {
        return new SpongeValue<>(Keys.UPDATE_GAME_PROFILE, false, this.updateGameProfile);
    }

}
