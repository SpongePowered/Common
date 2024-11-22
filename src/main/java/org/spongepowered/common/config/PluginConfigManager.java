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
package org.spongepowered.common.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Singleton;
import org.spongepowered.api.config.ConfigManager;
import org.spongepowered.api.config.ConfigRoot;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.reference.WatchServiceListener;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;

import javax.inject.Inject;

/**
 * Implementation of service to manage configurations.
 */
@Singleton
public final class PluginConfigManager implements ConfigManager {

    private final TypeSerializerCollection serializers;
    private final WatchServiceListener listener;

    @Inject
    PluginConfigManager(final DataSerializableTypeSerializer dataSerializableSerializer, final DataTranslatableTypeSerializer dataTranslatableSerializer) throws IOException {
        // TODO: Move this onto the async scheduler, rather than shared FJ pool?
        this.listener = WatchServiceListener.builder()
                .threadFactory(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Sponge-WatchService-%d").build())
                .build();

        this.serializers = TypeSerializerCollection.defaults().childBuilder()
                // We have a separate type serializer for CatalogTypes, so we explicitly discount them here.
                // See https://github.com/SpongePowered/SpongeCommon/issues/1348
                .register(DataSerializableTypeSerializer::accepts, dataSerializableSerializer)
                .registerAll(SpongeAdventure.CONFIGURATE.serializers())
                .register(dataTranslatableSerializer::accepts, dataTranslatableSerializer)
                .build();
    }

    @Override
    public ConfigRoot sharedConfig(final PluginContainer container) {
        return new PluginConfigRoot(this.serializers, container.metadata().id().toLowerCase(),
                                    SpongeCommon.pluginConfigDirectory());
    }

    @Override
    public ConfigRoot pluginConfig(final PluginContainer container) {
        return new PluginConfigRoot(this.serializers, container.metadata().id().toLowerCase(),
                                    SpongeCommon.pluginConfigDirectory().resolve(container.metadata().id().toLowerCase()));
    }

    @Override
    public TypeSerializerCollection serializers() {
        return this.serializers;
    }

    @Override
    public WatchServiceListener watchServiceListener() {
        return this.listener;
    }

    public static ConfigurationOptions getOptions(final TypeSerializerCollection serializers) {
        return ConfigurationOptions.defaults()
                .serializers(serializers);
    }

    public void close() throws IOException {
        this.listener.close();
    }
}
