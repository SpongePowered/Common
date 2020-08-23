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
package org.spongepowered.common.inject.provider;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.common.config.PluginConfigManager;
import org.spongepowered.plugin.PluginContainer;

import java.io.File;
import java.nio.file.Path;


/**
 * Welcome to the mess that is configuration.
 */
public final class PluginConfigurationModule extends AbstractModule {

    private static final TypeLiteral<ConfigurationLoader<CommentedConfigurationNode>> COMMENTED_CONFIGURATION_NODE_LOADER = new TypeLiteral<ConfigurationLoader<CommentedConfigurationNode>>() {};

    @Override
    protected void configure() {
        this.bind(Path.class).annotatedWith(ConfigDirAnnotation.NON_SHARED).toProvider(NonSharedDirAsPath.class);
        // Plugin-private directory config file
        this.bind(Path.class).annotatedWith(DefaultConfigAnnotation.NON_SHARED).toProvider(NonSharedPathAsPath.class);
        // Shared-directory config file
        this.bind(Path.class).annotatedWith(DefaultConfigAnnotation.SHARED).toProvider(SharedDirAsPath.class);
        // Loader for shared-directory config file
        this.bind(COMMENTED_CONFIGURATION_NODE_LOADER).annotatedWith(DefaultConfigAnnotation.SHARED).toProvider(SharedCommentedConfigurationNode.class);
        // Loader for plugin-private directory config file
        this.bind(COMMENTED_CONFIGURATION_NODE_LOADER).annotatedWith(DefaultConfigAnnotation.NON_SHARED).toProvider(PrivateCommentedConfigurationNode.class);
    }

    /**
     * Provides a non-shared (private) directory.
     *
     * {@literal @}ConfigDir(sharedRoot = false) Path configDir;
     */
    public static class NonSharedDirAsPath implements Provider<Path> {

        @Inject private PluginContainer container;

        @Override
        public Path get() {
            return PluginConfigManager.getPrivateRoot(this.container).getDirectory();
        }

    }

    /**
     * Provides a configuration path within a non-shared (private) directory.
     *
     * {@literal @}DefaultConfig(sharedRoot = false) Path configPath;
     */
    public static class NonSharedPathAsPath implements Provider<Path> {

        @Inject private PluginContainer container;

        @Override
        public Path get() {
            return PluginConfigManager.getPrivateRoot(this.container).getConfigPath();
        }

    }

    /**
     * Provides a configuration path within a shared directory.
     *
     * {@literal @}DefaultConfig(sharedRoot = true) Path configFile;
     */
    static class SharedDirAsPath implements Provider<Path> {

        @Inject private PluginContainer container;

        @Override
        public Path get() {
            return PluginConfigManager.getSharedRoot(this.container).getConfigPath();
        }

    }

    static abstract class CommentedConfigurationNodeProvider implements Provider<ConfigurationLoader<CommentedConfigurationNode>> {

        @Inject protected PluginContainer container;

        CommentedConfigurationNodeProvider() {
        }

    }

    static class SharedCommentedConfigurationNode extends CommentedConfigurationNodeProvider {

        @Override
        public ConfigurationLoader<CommentedConfigurationNode> get() {
            return PluginConfigManager.getSharedRoot(this.container).getConfig();
        }

    }

    static class PrivateCommentedConfigurationNode extends CommentedConfigurationNodeProvider {

        @Override
        public ConfigurationLoader<CommentedConfigurationNode> get() {
            return PluginConfigManager.getPrivateRoot(this.container).getConfig();
        }

    }

}
