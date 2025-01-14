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


import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.world.level.GameType;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.living.player.tab.TabList;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.common.accessor.network.protocol.game.ClientboundPlayerInfoUpdatePacketAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.profile.SpongeGameProfile;
import org.spongepowered.common.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

public final class SpongeTabList implements TabList {

    private static final net.minecraft.network.chat.Component EMPTY_COMPONENT = net.minecraft.network.chat.Component.literal(""); // TODO use empty?
    private final net.minecraft.server.level.ServerPlayer player;
    private @Nullable Component header;
    private @Nullable Component footer;
    private final Map<UUID, TabListEntry> entries = Maps.newHashMap();

    public SpongeTabList(final net.minecraft.server.level.ServerPlayer player) {
        this.player = player;
    }

    @Override
    public ServerPlayer player() {
        return (ServerPlayer) this.player;
    }

    @Override
    public Optional<Component> header() {
        return Optional.ofNullable(this.header);
    }

    @Override
    public TabList setHeader(final @Nullable Component header) {
        this.header = header;

        this.refreshClientHeaderFooter();

        return this;
    }

    @Override
    public Optional<Component> footer() {
        return Optional.ofNullable(this.footer);
    }

    @Override
    public TabList setFooter(final @Nullable Component footer) {
        this.footer = footer;

        this.refreshClientHeaderFooter();

        return this;
    }

    @Override
    public TabList setHeaderAndFooter(final @Nullable Component header, final @Nullable Component footer) {
        // Do not call the methods, set directly
        this.header = header;
        this.footer = footer;

        this.refreshClientHeaderFooter();

        return this;
    }

    @SuppressWarnings("ConstantConditions")
    private void refreshClientHeaderFooter() {
        // MC-98180 - Sending null as header or footer will cause an exception on the client
        final ClientboundTabListPacket packet = new ClientboundTabListPacket(
            this.header == null ? SpongeTabList.EMPTY_COMPONENT : SpongeAdventure.asVanilla(this.header),
            this.footer == null ? SpongeTabList.EMPTY_COMPONENT : SpongeAdventure.asVanilla(this.footer)
        );
        this.player.connection.send(packet);
    }

    @Override
    public Collection<TabListEntry> entries() {
        return Collections.unmodifiableCollection(this.entries.values());
    }

    @Override
    public Optional<TabListEntry> entry(final UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "unique id");
        return Optional.ofNullable(this.entries.get(uniqueId));
    }

    @Override
    public TabList addEntry(final TabListEntry entry) throws IllegalArgumentException {
        Objects.requireNonNull(entry, "builder");
        Preconditions.checkState(entry.list().equals(this), "the provided tab list entry was not created for this tab list");

        this.addEntry(entry, true);

        return this;
    }

    private void addEntry(final ClientboundPlayerInfoUpdatePacket.Entry entry) {
        final GameProfile profile = entry.profile();
        if (!this.entries.containsKey(profile.getId())) {
            final net.minecraft.network.chat.@Nullable Component displayName = entry.displayName();
            this.addEntry(new SpongeTabListEntry(
                    this,
                    SpongeGameProfile.of(profile),
                    displayName == null ? null : SpongeAdventure.asAdventure(displayName),
                    entry.latency(),
                    (GameMode) (Object) entry.gameMode(),
                    entry.listed(),
                    entry.listOrder(),
                    entry.chatSession() == null ? null : entry.chatSession().profilePublicKey()
            ), false);
        }
    }

    private void addEntry(final TabListEntry entry, final boolean exceptionOnDuplicate) {
        final UUID uniqueId = entry.profile().uniqueId();

        @Nullable final TabListEntry prev = this.entries.putIfAbsent(uniqueId, entry);
        if (exceptionOnDuplicate && prev != null) {
            throw new IllegalArgumentException("cannot add duplicate entry");
        }

        if (prev == null) {
            this.sendUpdate(entry, EnumSet.allOf(ClientboundPlayerInfoUpdatePacket.Action.class));
        }
    }

    @Override
    public Optional<TabListEntry> removeEntry(final UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "unique id");

        final TabListEntry entry = this.entries.remove(uniqueId);
        if (entry != null) {
            this.player.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(entry.profile().uniqueId())));
            return Optional.of(entry);
        }
        return Optional.empty();
    }

    /**
     * Send an entry update.
     *
     * @param entry The entry to update
     * @param actions The update action to perform
     */
    @SuppressWarnings("ConstantConditions")
    void sendUpdate(final TabListEntry entry, final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions) {
        final ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(actions, List.of());
        final RemoteChatSession.Data chatSessionData = ((SpongeTabListEntry) entry).profilePublicKey() == null ? null : new RemoteChatSession.Data(entry.profile().uuid(), ((SpongeTabListEntry) entry).profilePublicKey());
        final net.minecraft.network.chat.Component displayName = entry.displayName().isPresent() ? SpongeAdventure.asVanilla(entry.displayName().get()) : null;
        final ClientboundPlayerInfoUpdatePacket.Entry data = new ClientboundPlayerInfoUpdatePacket.Entry(entry.profile().uniqueId(), SpongeGameProfile.toMcProfile(entry.profile()),
            entry.listed(), entry.latency(), (GameType) (Object) entry.gameMode(), displayName, false, entry.weight(), chatSessionData);
        ((ClientboundPlayerInfoUpdatePacketAccessor) packet).accessor$entries(List.of(data));
        this.player.connection.send(packet);
    }

    /**
     * Update this tab list with data from the provided packet.
     *
     * <p>This method should not be called manually, it is automatically
     * called in the player's network connection when the packet is sent.</p>
     *
     * <p>We also filter out entries that the client does not see.</p>
     *
     * @param packet The packet to process
     */
    @SuppressWarnings("ConstantConditions")
    public @Nullable ClientboundPlayerInfoUpdatePacket updateEntriesOnSend(final ClientboundPlayerInfoUpdatePacket packet) {
        @MonotonicNonNull List<ClientboundPlayerInfoUpdatePacket.Entry> filteredEntries = null;
        final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = packet.actions();
        for (int i = 0; i < packet.entries().size(); i++) {
            final ClientboundPlayerInfoUpdatePacket.Entry update = packet.entries().get(i);
            if (actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                // If an entry with the same id exists nothing will be done
                this.addEntry(update);
            }

            final @Nullable TabListEntry entry = this.entry(update.profileId()).orElse(null);
            if (entry != null) {
                if (filteredEntries != null) {
                    filteredEntries.add(update);
                }
                if (actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME)) {
                    ((SpongeTabListEntry) entry).updateWithoutSend();
                    entry.setDisplayName(update.displayName() == null ? null : SpongeAdventure.asAdventure(update.displayName()));
                }
                if (actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY)) {
                    ((SpongeTabListEntry) entry).updateWithoutSend();
                    entry.setLatency(update.latency());
                }
                if (actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE)) {
                    ((SpongeTabListEntry) entry).updateWithoutSend();
                    entry.setGameMode((GameMode) (Object) update.gameMode());
                }
                if (actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED)) {
                    ((SpongeTabListEntry) entry).updateWithoutSend();
                    entry.setListed(update.listed());
                }
            } else if (filteredEntries == null) {
                if (packet.entries().size() == 1) {
                    return null;
                }
                filteredEntries = new ArrayList<>(packet.entries().subList(0, i));
            }
        }
        if (filteredEntries == null) {
            return packet;
        }
        final var filteredPacket = new ClientboundPlayerInfoUpdatePacket(packet.actions(), List.of());
        ((ClientboundPlayerInfoUpdatePacketAccessor)filteredPacket).accessor$entries(filteredEntries);
        return filteredPacket;
    }

    public @Nullable ClientboundPlayerInfoRemovePacket updateEntriesOnSend(final ClientboundPlayerInfoRemovePacket packet) {
        @MonotonicNonNull List<UUID> filteredProfileIds = null;
        for (int i = 0; i < packet.profileIds().size(); i++) {
            final UUID uniqueId = packet.profileIds().get(i);
            final TabListEntry entry = this.entries.remove(uniqueId);
            if (entry != null) {
                if (filteredProfileIds != null) {
                    filteredProfileIds.add(uniqueId);
                }
            } else if (filteredProfileIds == null) {
                if (packet.profileIds().size() == 1) {
                    return null;
                }
                filteredProfileIds = new ArrayList<>(packet.profileIds().subList(0, i));
            }
        }
        if (filteredProfileIds == null) {
            return packet;
        }
        return new ClientboundPlayerInfoRemovePacket(filteredProfileIds);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SpongeTabList.class.getSimpleName() + "[", "]")
                .add("player=" + this.player)
                .add("header=" + this.header)
                .add("footer=" + this.footer)
                .add("entries=" + this.entries)
                .toString();
    }

}
