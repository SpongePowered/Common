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
package org.spongepowered.test.data;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;
import org.spongepowered.test.LoadableModule;

@Plugin("decoratedpotdata")
public class PotDataTest implements LoadableModule {

    private final PluginContainer plugin;

    @Inject
    public PotDataTest(final PluginContainer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable(final CommandContext ctx) {
        Sponge.eventManager().registerListeners(this.plugin, new PotDataTest.InteractListener());
    }

    public static class InteractListener {
        @Listener
        private void onInteractBlock(final InteractBlockEvent.Secondary event, @Root ServerPlayer player) {
            if (!event.block().state().type().isAnyOf(BlockTypes.DECORATED_POT)) {
                return;
            }
            player.world().blockEntity(event.block().position())
                .ifPresentOrElse(be -> {
                    final var pos = be.blockPosition();
                    final var front = be.get(Keys.POT_FRONT_DECORATION).get();
                    final var frontResourceKey = front.key(RegistryTypes.ITEM_TYPE);
                    final var frontItem = HoverEvent.showItem(frontResourceKey, 1);
                    final var back = be.get(Keys.POT_BACK_DECORATION).get();
                    final var backResourceKey = back.key(RegistryTypes.ITEM_TYPE);
                    final var backItem = HoverEvent.showItem(backResourceKey, 1);
                    final var left = be.get(Keys.POT_LEFT_DECORATION).get();
                    final var leftResourceKey = left.key(RegistryTypes.ITEM_TYPE);
                    final var leftItem = HoverEvent.showItem(leftResourceKey, 1);
                    final var right = be.get(Keys.POT_RIGHT_DECORATION).get();
                    final var rightResourceKey = right.key(RegistryTypes.ITEM_TYPE);
                    final var rightItem = HoverEvent.showItem(rightResourceKey, 1);
                    final var message = Component.text("Here's the block decorations"
                    ).append(Component.newline()).append(
                        Component.text("Front: ").append(Component.text(frontResourceKey.toString()).hoverEvent(frontItem))
                    ).append(Component.newline()).append(
                        Component.text("Back: ").append(Component.text(backResourceKey.toString()).hoverEvent(backItem))
                    ).append(Component.newline()).append(
                        Component.text("Left: ").append(Component.text(leftResourceKey.toString()).hoverEvent(leftItem))
                    ).append(Component.newline()).append(
                        Component.text("Right: ").append(Component.text(rightResourceKey.toString()).hoverEvent(rightItem))
                    );
                    player.sendMessage(message);
                }, () -> {
                    player.sendMessage(Component.text("No decorated pot found"));
                });
        }
    }
}
