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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.minecraft.client.KeyMapping;
import net.minecraft.locale.Language;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.common.accessor.client.KeyMappingAccessor;
import org.spongepowered.common.launch.Launch;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ComponentFlattenerProvider {
    private static final Pattern LOCALIZATION_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?s");
    static final ComponentFlattener INSTANCE;

    static {
        final ComponentFlattener.Builder builder = ComponentFlattener.basic().toBuilder();

        if (!Launch.instance().dedicatedServer()) {
            builder.mapper(KeybindComponent.class, ComponentFlattenerProvider::resolveKeybind);
        }

        builder.complexMapper(TranslatableComponent.class, (component, consumer) -> {
            final String key = component.key();
            for (final Translator registry : GlobalTranslator.translator().sources()) {
                if (registry instanceof TranslationRegistry && ((TranslationRegistry) registry).contains(key)) {
                    consumer.accept(GlobalTranslator.render(component, Locale.getDefault()));
                    return;
                }
            }

            final /* @NonNull */ String translated = Language.getInstance().getOrDefault(key);
            final Matcher matcher = ComponentFlattenerProvider.LOCALIZATION_PATTERN.matcher(translated);
            final List<TranslationArgument> args = component.arguments();
            int argPosition = 0;
            int lastIdx = 0;
            while (matcher.find()) {
                // append prior
                if (lastIdx < matcher.start()) {
                    consumer.accept(Component.text(translated.substring(lastIdx, matcher.start())));
                }
                lastIdx = matcher.end();

                final /* @Nullable */ String argIdx = matcher.group(1);
                // calculate argument position
                if (argIdx != null) {
                    try {
                        final int idx = Integer.parseInt(argIdx);
                        if (idx < args.size()) {
                            consumer.accept(ComponentLike.unbox(args.get(idx)));
                        }
                    } catch (final NumberFormatException ex) {
                        // ignore, drop the format placeholder
                    }
                } else {
                    final int idx = argPosition++;
                    if (idx < args.size()) {
                        consumer.accept(ComponentLike.unbox(args.get(idx)));
                    }
                }
            }

            // append tail
            if (lastIdx < translated.length()) {
                consumer.accept(Component.text(translated.substring(lastIdx)));
            }
        });

        INSTANCE = builder.build();
    }

    @OnlyIn(Dist.CLIENT)
    private static String resolveKeybind(final KeybindComponent component) {
        final KeyMapping mapping = KeyMappingAccessor.accessor$ALL().get(component.keybind());
        if (mapping != null) {
            return mapping.getTranslatedKeyMessage().getString();
        }
        return component.keybind();
    }
}
