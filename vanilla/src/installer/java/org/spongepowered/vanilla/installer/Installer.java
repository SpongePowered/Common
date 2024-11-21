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
package org.spongepowered.vanilla.installer;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.objectmapping.meta.NodeResolver;
import org.spongepowered.libs.LibraryManager;
import org.spongepowered.vanilla.installer.library.TinyLogger;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class Installer {

    private final Path directory;
    private final LibraryManager libraryManager;
    private final ConfigurationLoader<CommentedConfigurationNode> loader;
    private final LauncherConfig config;

    public Installer(final Path directory) throws ConfigurateException {
        this.directory = directory;
        final Path launcherConfigFile = this.directory.resolve("launcher.conf");
        this.loader = HoconConfigurationLoader.builder()
                .path(launcherConfigFile)
                .defaultOptions(options ->
                    options.shouldCopyDefaults(true)
                            .implicitInitialization(true)
                            .serializers(builder -> builder.registerAnnotatedObjects(ObjectMapper.factoryBuilder()
                                                         .addNodeResolver(NodeResolver.onlyWithSetting())
                                                                                             .build()))
                )
                .build();
        this.config = this.loadConfig();
        this.libraryManager = new LibraryManager(
            TinyLogger.INSTANCE,
            this.config.checkLibraryHashes,
            Paths.get(this.config.librariesDirectory.replace("${BASE_DIRECTORY}", directory.toAbsolutePath().toString())),
            this.getClass().getResource("/libraries.json")
        );
    }

    private LauncherConfig loadConfig() throws ConfigurateException {
        final CommentedConfigurationNode node = this.loader.load();
        final LauncherConfig ret = node.get(LauncherConfig.class);

        // Write back to apply any additions to the file
        this.loader.save(node);
        return ret;
    }

    public Path getDirectory() {
        return this.directory;
    }

    public LibraryManager getLibraryManager() {
        return this.libraryManager;
    }

    public LauncherConfig getLauncherConfig() {
        return this.config;
    }
}
