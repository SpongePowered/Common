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

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.dimension.DimensionType;
import org.spongepowered.api.world.dimension.DimensionTypes;
import org.spongepowered.common.bridge.world.storage.WorldInfoBridge;
import org.spongepowered.common.applaunch.config.core.ConfigHandle;
import org.spongepowered.common.applaunch.config.core.InheritableConfigHandle;
import org.spongepowered.common.applaunch.config.core.SpongeConfigs;
import org.spongepowered.common.config.customdata.CustomDataConfig;
import org.spongepowered.common.applaunch.config.inheritable.WorldConfig;
import org.spongepowered.common.config.tracker.TrackerConfig;
import org.spongepowered.common.world.server.SpongeWorldManager;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * SpongeCommon configurations that need to interact with game state
 */
public class SpongeGameConfigs {

    static final Logger LOGGER = LogManager.getLogger();

    static ConfigHandle<TrackerConfig> trackerConfigAdapter;
    static ConfigHandle<CustomDataConfig> customDataConfigAdapter;

    public static CompletableFuture<CommentedConfigurationNode> savePluginsInMetricsConfig(final Map<String, Tristate> entries) {
        return SpongeConfigs.getCommon().updateSetting("metrics.plugin-states", entries,
                        new TypeToken<Map<String, Tristate>>() { private static final long serialVersionUID = -1L;});
    }

    public static ConfigHandle<CustomDataConfig> getCustomData() {
        if (customDataConfigAdapter == null) {
            customDataConfigAdapter = SpongeConfigs.create(new CustomDataConfig(), CustomDataConfig.FILE_NAME);
        }
        return customDataConfigAdapter;
    }

    public static ConfigHandle<TrackerConfig> getTracker() {
        if (trackerConfigAdapter == null) {
            trackerConfigAdapter = SpongeConfigs.create(new TrackerConfig(), TrackerConfig.FILE_NAME);
        }
        return trackerConfigAdapter;
    }

    public static InheritableConfigHandle<WorldConfig> getForWorld(final org.spongepowered.api.world.World<?> spongeWorld) {
        return getForWorld((net.minecraft.world.World) spongeWorld);
    }

    public static InheritableConfigHandle<WorldConfig> getForWorld(final net.minecraft.world.World mcWorld) {
        return ((WorldInfoBridge) mcWorld.getWorldInfo()).bridge$getConfigAdapter();
    }

    public static InheritableConfigHandle<WorldConfig> createWorld(final @Nullable DimensionType legacyType, final ResourceKey world) {
        // Path format: config/sponge/worlds/<world-namespace>/<world-value>.conf
        final Path configPath = SpongeConfigs.getDirectory().resolve(Paths.get("worlds", world.getNamespace(), world.getValue() + ".conf"));
        if (legacyType != null) {
            // Legacy config path: config/sponge/worlds/<dim-namespace>/<dim-value>/<world-name>/world.conf
            final String legacyName = getLegacyWorldName(world);
            if (legacyName != null) {
                final Path legacyPath = SpongeConfigs.getDirectory().resolve(Paths.get("worlds", legacyType.getKey().getNamespace(),
                        getLegacyValue(legacyType.getKey()), legacyName, "world.conf"));
                if (legacyPath.toFile().isFile() && !configPath.toFile().isFile()) {
                    try {
                        Files.createDirectories(configPath.getParent());
                        Files.move(legacyPath, configPath);
                        final Path legacyParent = legacyPath.getParent();
                        try (DirectoryStream<Path> str = Files.newDirectoryStream(legacyParent)) {
                            if (!str.iterator().hasNext()) {
                                Files.delete(legacyParent);
                            }
                        }
                    } catch (final IOException ex) {
                        LOGGER.error("Unable to migrate config for world {} from legacy location {}", world, legacyPath, ex);
                    }
                }
            }
        }
        try {
            @SuppressWarnings("deprecation") // deprecated to discourage usage outside of this method
            final InheritableConfigHandle<WorldConfig> config = new InheritableConfigHandle<>(new WorldConfig(), SpongeConfigs.createLoader(configPath),
                    SpongeConfigs.getGlobalInheritable());
            config.load();
            return config;
        } catch (final IOException | ObjectMappingException ex) {
            LOGGER.error("Unable to load configuration for world {}. Sponge will use a "
                    + "fallback configuration with default values that will not save.", world, ex);
            return SpongeConfigs.createDetached();
        }
    }

    private static String getLegacyValue(final ResourceKey dimensionType) {
        if (dimensionType.equals(DimensionTypes.THE_NETHER.get().getKey())) {
            return "nether";
        } else {
            return dimensionType.getValue();
        }
    }

    private static @Nullable String getLegacyWorldName(final ResourceKey world) {
        if (world.equals(SpongeWorldManager.VANILLA_OVERWORLD)) {
            return "world";
        } else if (world.equals(SpongeWorldManager.VANILLA_THE_END)) {
            return "DIM1";
        } else if (world.equals(SpongeWorldManager.VANILLA_THE_NETHER)) {
            return "DIM-1";
        }
        return null;
    }
}
