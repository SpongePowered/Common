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
package org.spongepowered.common.server;

import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.util.locale.Locales;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.commands.CommandSourceBridge;
import org.spongepowered.common.bridge.commands.CommandSourceProviderBridge;
import org.spongepowered.common.service.server.permission.SpongeSystemSubject;

import java.util.Locale;

@SuppressWarnings("deprecation")
public final class ServerConsoleSystemSubject extends SpongeSystemSubject implements CommandSourceProviderBridge, CommandSource, CommandSourceBridge {

    @Override
    public String identifier() {
        return "console";
    }

    @Override
    public void sendMessage(final @NonNull Identity identity, final @NonNull Component message, final @NonNull MessageType type) {
        SpongeCommon.server().sendSystemMessage(SpongeAdventure.asVanilla(message));
    }

    @Override
    public CommandSourceStack bridge$getCommandSource(final Cause cause) {
        return new CommandSourceStack(this,
                Vec3.ZERO,
                Vec2.ZERO,
                SpongeCommon.server().getLevel(Level.OVERWORLD),
                4,
                "System Subject",
                net.minecraft.network.chat.Component.literal("System Subject"),
                SpongeCommon.server(),
                null);
    }

    @Override
    public void sendSystemMessage(net.minecraft.network.chat.Component var1) {
        SpongeCommon.logger().info(var1.getString());
    }

    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return true;
    }

    @Override
    @NonNull
    public Locale locale() {
        // If we're running a client, then get the client's locale rather than assume the
        // default locale.
        if (SpongeCommon.game().isClientAvailable()) {
            return SpongeCommon.game().client().locale();
        }
        // Dedicated servers only support the default locale.
        return Locales.DEFAULT;
    }

}
