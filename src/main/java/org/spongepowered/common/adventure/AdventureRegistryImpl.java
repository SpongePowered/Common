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
package org.spongepowered.common.adventure;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.Index;
import org.spongepowered.api.adventure.AdventureRegistry;
import org.spongepowered.common.adventure.AdventureRegistryImpl.ForIndex;
import java.util.Optional;
import java.util.Set;

public final class AdventureRegistryImpl implements AdventureRegistry {
    public static final AdventureRegistry INSTANCE = new AdventureRegistryImpl();

    private final OfType<TextDecoration> decorations = new ForIndex<>(TextDecoration.NAMES);
    private final OfType<NamedTextColor> namedColors = new ForIndex<>(NamedTextColor.NAMES);
    private final OfType<ClickEvent.Action> clickEventActions = new ForIndex<>(ClickEvent.Action.NAMES);
    private final OfType<HoverEvent.Action<?>> hoverEventActions = new ForIndex<>(HoverEvent.Action.NAMES);
    private final OfType<BossBar.Color> bossBarColors = new ForIndex<>(BossBar.Color.NAMES);
    private final OfType<BossBar.Overlay> bossBarOverlays = new ForIndex<>(BossBar.Overlay.NAMES);
    private final OfType<BossBar.Flag> bossBarFlags = new ForIndex<>(BossBar.Flag.NAMES);
    private final OfType<Sound.Source> soundSources = new ForIndex<>(Sound.Source.NAMES);

    @Override
    public OfType<TextDecoration> getDecorations() {
        return this.decorations;
    }

    @Override
    public OfType<NamedTextColor> getNamedColors() {
        return this.namedColors;
    }

    @Override
    public OfType<ClickEvent.Action> getClickEventActions() {
        return this.clickEventActions;
    }

    @Override
    public OfType<HoverEvent.Action<?>> getHoverEventActions() {
        return this.hoverEventActions;
    }

    @Override
    public OfType<BossBar.Color> getBossBarColors() {
        return this.bossBarColors;
    }

    @Override
    public OfType<BossBar.Overlay> getBossBarOverlays() {
        return this.bossBarOverlays;
    }

    @Override
    public OfType<BossBar.Flag> getBossBarFlags() {
        return this.bossBarFlags;
    }

    @Override
    public OfType<Sound.Source> getSoundSources() {
        return this.soundSources;
    }

    static class ForIndex<T> implements OfType<T> {
        private final Index<String, T> registry;

        ForIndex(final Index<String, T> registry) {
            this.registry = registry;
        }

        @Override
        public String getKey(final T value) {
            return this.registry.key(value);
        }

        @Override
        public Optional<T> getValue(final String key) {
            return Optional.ofNullable(this.registry.value(key));
        }

        @Override
        public Set<String> keys() {
            return this.registry.keys();
        }
    }
}
