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
package org.spongepowered.common.mixin.core.ban;

import net.minecraft.server.management.UserList;
import net.minecraft.server.management.UserListBans;
import net.minecraft.server.management.UserListEntry;
import net.minecraft.server.management.UserListIPBans;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@Mixin(UserListIPBans.class) // This is a bad MCP name, it's really IPBanList
public abstract class MixinIPBanList extends UserList {

    @Shadow public abstract String addressToString(SocketAddress address);

    public MixinIPBanList(File saveFile) {
        super(saveFile);
    }

    @Override
    public boolean hasEntry(Object object) {
        String entry = (String) object;
        if (entry.equals("local")) { // Check for single player
            return false;
        }

        try {
            return Sponge.getServiceManager().provideUnchecked(BanService.class).isBanned(InetAddress.getByName((String) object));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Error parsing Ban IP address!", e);
        }
    }

    @Override
    public UserListEntry getEntry(Object object) {
        String entry = (String) object;
        if (entry.equals("local")) { // Check for single player
            return null;
        }

        try {
            return (UserListEntry) Sponge.getServiceManager().provideUnchecked(BanService.class).getBanFor(InetAddress.getByName((String) object)).orElse(null);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Error parsing Ban IP address!", e);
        }
    }

    @Override
    public void removeEntry(Object object) {
        String entry = (String) object;
        if (entry.equals("local")) { // Check for single player
            return;
        }

        try {
            Sponge.getServiceManager().provideUnchecked(BanService.class).pardon(InetAddress.getByName((String) object));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Error parsing Ban IP address!", e);
        }
    }

    @Override
    public String[] getKeys() {
        List<String> ips = new ArrayList<>();
        for (Ban.Ip ban: Sponge.getServiceManager().provideUnchecked(BanService.class).getIpBans()) {
            ips.add(this.addressToString(new InetSocketAddress(ban.getAddress(), 0)));
        }
        return ips.toArray(new String[ips.size()]);
    }

    @Override
    public void addEntry(UserListEntry entry) {
        Sponge.getServiceManager().provideUnchecked(BanService.class).addBan((Ban) entry);
    }

    @Override
    public boolean isEmpty() {
        return Sponge.getServiceManager().provideUnchecked(BanService.class).getIpBans().isEmpty();
    }
}
