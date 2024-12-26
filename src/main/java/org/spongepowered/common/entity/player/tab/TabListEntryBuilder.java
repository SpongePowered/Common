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
package org.spongepowered.common.entity.player.tab;


import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.tab.TabList;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.common.util.Preconditions;

import java.util.Objects;


public final class TabListEntryBuilder implements TabListEntry.Builder {

    private @Nullable TabList list;
    private @Nullable GameProfile profile;
    private @Nullable Component displayName;
    private int latency;
    private boolean listed = true;
    private @Nullable GameMode gameMode;
    private int weight;

    @Override
    public TabListEntry.Builder list(TabList list) {
        this.list = Objects.requireNonNull(list, "list");
        return this;
    }

    @Override
    public TabListEntry.Builder profile(GameProfile profile) {
        this.profile = Objects.requireNonNull(profile, "profile");
        return this;
    }

    @Override
    public TabListEntry.Builder displayName(@Nullable Component displayName) {
        this.displayName = displayName;
        return this;
    }

    @Override
    public TabListEntry.Builder latency(int latency) {
        this.latency = latency;
        return this;
    }

    @Override
    public TabListEntry.Builder gameMode(GameMode gameMode) {
        this.gameMode = Objects.requireNonNull(gameMode, "game mode");
        return this;
    }

    @Override
    public TabListEntry.Builder listed(boolean listed) {
        this.listed = listed;
        return this;
    }

    @Override
    public TabListEntry.Builder weight(int weight) {
        this.weight = weight;
        return this;
    }

    @Override
    public TabListEntry build() {
        Preconditions.checkState(this.list != null, "list must be set");
        Preconditions.checkState(this.profile != null, "profile must be set");
        Preconditions.checkState(this.gameMode != null, "game mode must be set");

        return new SpongeTabListEntry(this.list, this.profile, this.displayName, this.latency, this.gameMode, this.listed, this.weight, null);
    }

    @Override
    public TabListEntry.Builder from(TabListEntry value) {
        this.list = Objects.requireNonNull(value.list(), "list");
        this.profile = Objects.requireNonNull(value.profile(), "profile");
        this.displayName = value.displayName().orElse(null);
        this.latency = value.latency();
        this.gameMode = Objects.requireNonNull(value.gameMode(), "game mode");
        this.weight = value.weight();
        return this;
    }

    @Override
    public TabListEntry.Builder reset() {
        this.list = null;
        this.profile = null;
        this.displayName = null;
        this.latency = 0;
        this.gameMode = null;
        this.weight = 0;
        return this;
    }

}
