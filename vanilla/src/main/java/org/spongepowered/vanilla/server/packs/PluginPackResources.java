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
package org.spongepowered.vanilla.server.packs;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PluginPackResources extends AbstractPackResources {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final PluginContainer container;
    private final PackMetadataSection metadata;
    private final @Nullable Path pluginRoot;

    public PluginPackResources(final PackLocationInfo info, final PluginContainer container, final @Nullable Path pluginRoot) {
        super(info);
        this.container = container;
        this.metadata = new PackMetadataSection(Component.literal("Plugin Resources"), 6, Optional.empty());
        this.pluginRoot = pluginRoot;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(final String... var1) {
        final String rawPath = String.join("/", var1);
        return this.getResource(rawPath);
    }

    private IoSupplier<InputStream> getResource(final String rawPath) {
        final Optional<URI> uri = this.container.locateResource(rawPath);
        if (uri.isEmpty()) {
            return null;
        }
        return () -> uri.get().toURL().openStream();
    }

    @Override
    public IoSupplier<InputStream> getResource(final PackType type, final ResourceLocation loc) {
        return this.getResource(String.format(Locale.ROOT, "%s/%s/%s", type.getDirectory(), loc.getNamespace(), loc.getPath()));
    }

    @Override
    public void listResources(final PackType type, final String namespace, final String path, final ResourceOutput out) {
        try {
            final Path root = this.typeRoot(type);
            final Path namespaceDir = root.resolve(namespace);
            final Path resourcesDir = namespaceDir.resolve(path);
            try (final Stream<Path> stream = Files.walk(resourcesDir)) {
                stream.filter(Files::isRegularFile)
                        .filter(filePath -> !filePath.getFileName().toString().endsWith(".mcmeta"))
                        .map(namespaceDir::relativize)
                        .map(filePath -> convertResourcePath(namespace, filePath))
                        .filter(Objects::nonNull)
                        .forEach(loc -> out.accept(loc, this.getResource(type, loc)));
            }
        } catch (final IOException ignored) {
        }
    }

    @Nullable
    private ResourceLocation convertResourcePath(final String namespace, final Path resourcePath) {
        final String path = resourcePath.toString();
        final ResourceLocation location = ResourceLocation.tryBuild(namespace, path);
        if (location == null)
            LOGGER.warn("Invalid path in plugin pack: {}:{}, ignoring", namespace, path);
        return location;
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(final MetadataSectionType<T> deserializer) throws IOException {
        if (deserializer.name().equals("pack")) {
            return (T) this.metadata;
        }
        return null;
    }

    @Override
    public Set<String> getNamespaces(final PackType type) {
        try {
            final @Nullable Path root = this.typeRoot(type);
            if (root != null) {
                try (final Stream<Path> stream = Files.list(root)) {
                    return stream.map(Path::getFileName)
                            .map(Object::toString)
                            .filter(s -> {
                                if (s.equals(s.toLowerCase(Locale.ROOT))) {
                                    return true;
                                } else {
                                    SpongeCommon.logger().warn("Pack: ignored non-lowercased namespace: {} in {}", s,
                                            root.toAbsolutePath().toString());
                                    return false;
                                }
                            })
                            .collect(Collectors.toSet());
                }
            }
        } catch (final IOException e) {
            // ignored
        }
        return Collections.emptySet();
    }

    @Override
    public void close() {

    }

    private @Nullable Path typeRoot(final PackType type) throws IOException {
        if (this.pluginRoot == null) {
            return null;
        }
        return this.pluginRoot.resolve(type.getDirectory());
    }

}
