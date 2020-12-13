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
package org.spongepowered.common.ban;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import net.kyori.adventure.text.Component;
import net.minecraft.server.management.IPBanEntry;
import net.minecraft.server.management.IPBanList;
import net.minecraft.server.management.ProfileBanEntry;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.BanType;
import org.spongepowered.api.service.ban.BanTypes;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.server.management.IPBanListAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.profile.SpongeGameProfile;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Date;

import javax.annotation.Nullable;

public final class SpongeBanBuilder implements Ban.Builder {

    private GameProfile profile;
    private InetAddress address;
    private BanType banType;
    @Nullable private Component reason;
    private Instant start = Instant.now();
    @Nullable private Instant end;
    @Nullable private Component source;

    @Override
    public Ban.Builder profile(final GameProfile profile) {
        checkNotNull(profile, "Profile cannot be null!");
        checkState(this.banType == BanTypes.PROFILE.get(),
            "Cannot set a GameProfile if the BanType is not BanTypes.PROFILE!");
        this.profile = profile;
        return this;
    }

    @Override
    public Ban.Builder address(final InetAddress address) {
        checkNotNull(address, "Address cannot be null!");
        checkState(this.banType == BanTypes.IP.get(), "Cannot set an InetAddress if the BanType is not BanTypes.IP!");
        this.address = address;
        return this;
    }

    @Override
    public Ban.Builder type(final BanType type) {
        checkNotNull(type, "BanType cannot be null!");
        if (type == BanTypes.IP.get()) {
            this.profile = null;
        } else {
            this.address = null;
        }
        this.banType = type;
        return this;
    }

    @Override
    public Ban.Builder reason(@Nullable final Component reason) {
        this.reason = reason;
        return this;
    }

    @Override
    public Ban.Builder startDate(final Instant instant) {
        checkNotNull(instant, "Start date cannot be null!");
        this.start = instant;
        return this;
    }

    @Override
    public Ban.Builder expirationDate(@Nullable final Instant instant) {
        this.end = instant;
        return this;
    }

    @Override
    public Ban.Builder source(@Nullable final Component source) {
        this.source = source;
        return this;
    }

    @Override
    public Ban build() {
        checkState(this.banType != null, "BanType cannot be null!");

        final String sourceName = this.source != null ? SpongeAdventure.legacySection(this.source) : null;
        final String reason = this.reason != null ? SpongeAdventure.legacySection(this.reason) : null;

        if (this.banType == BanTypes.PROFILE.get()) {
            checkState(this.profile != null, "User cannot be null!");
            return (Ban) new ProfileBanEntry(SpongeGameProfile.toMcProfile(this.profile.withoutProperties()),
                    Date.from(this.start), sourceName, this.toDate(this.end), reason);
        }
        checkState(this.address != null, "Address cannot be null!");

        // This *should* be a static method, but apparently not...
        final IPBanList ipBans = SpongeCommon.getServer().getPlayerList().getBannedIPs();
        return (Ban) new IPBanEntry(((IPBanListAccessor) ipBans).accessor$addressToString(new InetSocketAddress(this.address, 0)),
                Date.from(this.start), sourceName, this.toDate(this.end), reason);
    }

    private Date toDate(final Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    @Override
    public Ban.Builder from(final Ban ban) {
        this.reset();
        this.banType = ban.getType();

        if (this.banType.equals(BanTypes.PROFILE.get())) {
            this.profile = ((Ban.Profile) ban).getProfile();
        } else {
            this.address = ((Ban.Ip) ban).getAddress();
        }

        this.reason = ban.getReason().orElse(null);
        this.start = ban.getCreationDate();
        this.end = ban.getExpirationDate().orElse(null);
        this.source = ban.getBanSource().orElse(null);
        return this;
    }

    @Override
    public Ban.Builder reset() {
        this.profile = null;
        this.address = null;
        this.banType = null;
        this.reason = null;
        this.start = Instant.now();
        this.end = null;
        this.source = null;
        return this;
    }
}
