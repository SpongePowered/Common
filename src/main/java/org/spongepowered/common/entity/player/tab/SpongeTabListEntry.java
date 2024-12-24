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
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.tab.TabList;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.common.util.Preconditions;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;


/*
 * This is intentionally not a mixin of SPacketPlayerListItem.AddPlayerData.
 */
public final class SpongeTabListEntry implements TabListEntry {

    private final SpongeTabList list;
    private final GameProfile profile;
    private @Nullable Component displayName;
    private int latency;
    private GameMode gameMode;
    private boolean listed;
    private int weight;
    private boolean updateWithoutSend;
    private final ProfilePublicKey.Data profilePublicKey;

    public SpongeTabListEntry(
        final TabList list, final GameProfile profile, @Nullable final Component displayName, final int latency, final GameMode gameMode,
            final boolean listed, final int weight, final ProfilePublicKey.Data profilePublicKey) {
        Preconditions.checkState(list instanceof SpongeTabList, "list is not a SpongeTabList");
        this.list = (SpongeTabList) list;
        this.profile = Objects.requireNonNull(profile, "profile");
        this.displayName = displayName;
        this.latency = latency;
        this.gameMode = Objects.requireNonNull(gameMode, "game mode");
        this.listed = listed;
        this.weight = weight;
        this.profilePublicKey = profilePublicKey;
    }

    @Override
    public TabList list() {
        return this.list;
    }

    @Override
    public GameProfile profile() {
        return this.profile;
    }

    @Override
    public Optional<Component> displayName() {
        return Optional.ofNullable(this.displayName);
    }

    @Override
    public TabListEntry setDisplayName(@Nullable final Component displayName) {
        this.displayName = displayName;
        this.sendUpdate(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);
        return this;
    }

    @Override
    public int latency() {
        return this.latency;
    }

    @Override
    public TabListEntry setLatency(final int latency) {
        this.latency = latency;
        this.sendUpdate(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY);
        return this;
    }

    @Override
    public GameMode gameMode() {
        return this.gameMode;
    }

    @Override
    public TabListEntry setGameMode(final GameMode gameMode) {
        this.gameMode = java.util.Objects.requireNonNull(gameMode, "game mode");
        this.sendUpdate(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE);
        return this;
    }

    @Override
    public boolean listed() {
        return this.listed;
    }

    @Override
    public SpongeTabListEntry setListed(boolean listed) {
        this.listed = listed;
        this.sendUpdate(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED);
        return this;
    }

    @Override
    public int weight() {
        return this.weight;
    }

    @Override
    public SpongeTabListEntry setWeight(int weight) {
        this.weight = weight;
        this.sendUpdate(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER);
        return this;
    }

    public ProfilePublicKey.Data profilePublicKey() {
        return this.profilePublicKey;
    }

    private void sendUpdate(final ClientboundPlayerInfoUpdatePacket.Action action) {
        // We may be updating our values, so we don't want to send any updates
        // since that will result in a continuous loop.
        if (this.updateWithoutSend) {
            this.updateWithoutSend = false;
            return;
        }

        this.list.sendUpdate(this, EnumSet.of(action));
    }

    /**
     * Set the {@link #updateWithoutSend} state to prevent sending on next update.
     */
    public void updateWithoutSend() {
        this.updateWithoutSend = true;
    }

    @Override
    public boolean equals(@Nullable final Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        final TabListEntry that = (TabListEntry) other;
        return Objects.equals(this.profile.uniqueId(), that.profile().uniqueId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.profile);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SpongeTabListEntry.class.getSimpleName() + "[", "]")
                .add("profile=" + this.profile)
                .add("latency=" + this.latency)
                .add("displayName=" + this.displayName)
                .add("gameMode=" + this.gameMode)
                .add("listed=" + this.listed)
                .toString();
    }

}
