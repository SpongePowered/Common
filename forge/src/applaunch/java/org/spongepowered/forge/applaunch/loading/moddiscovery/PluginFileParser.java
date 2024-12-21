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
package org.spongepowered.forge.applaunch.loading.moddiscovery;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import net.minecraftforge.fml.loading.moddiscovery.AbstractModProvider;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModFileParser;
import net.minecraftforge.fml.loading.moddiscovery.ModJarMetadata;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModProvider;
import org.spongepowered.common.applaunch.AppLaunch;
import org.spongepowered.common.applaunch.metadata.PluginMetadataFixer;
import org.spongepowered.common.applaunch.plugin.PluginPlatformConstants;
import org.spongepowered.forge.applaunch.loading.metadata.PluginFileConfigurable;
import org.spongepowered.plugin.metadata.builtin.MetadataContainer;
import org.spongepowered.plugin.metadata.builtin.MetadataParser;

import java.io.Reader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class PluginFileParser {
    private static final String MODS_TOML   = "META-INF/mods.toml";
    private static final String MODULE_INFO = "module-info.class";

    private static Constructor<ModJarMetadata> modJarMetadataConstructor;

    static {
        try {
            PluginFileParser.modJarMetadataConstructor = ModJarMetadata.class.getDeclaredConstructor();
            PluginFileParser.modJarMetadataConstructor.setAccessible(true);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static IModFileInfo parsePluginFileInfo(final IModFile iModFile) {
        final ModFile modFile = (ModFile) iModFile;
        AppLaunch.logger().debug("Considering plugin file candidate {}", modFile.getFilePath());

        final Path metadataFile = modFile.findResource(PluginPlatformConstants.METADATA_FILE_LOCATION);
        if (Files.notExists(metadataFile)) {
            AppLaunch.logger().debug("Plugin file '{}' is missing a 'sponge_plugins.json' metadata file in META-INF", modFile);
            return null;
        }

        try {
            final MetadataContainer container;
            try (final Reader reader = Files.newBufferedReader(metadataFile, StandardCharsets.UTF_8)) {
                container = MetadataParser.read(reader);
            }

            final PluginFileConfigurable config = new PluginFileConfigurable(PluginMetadataFixer.fixPluginIds(container));
            return new ModFileInfo(modFile, config, (info) -> {}, List.of());
        } catch (final Exception e) {
            AppLaunch.logger().warn("Could not read metadata for plugin file '{}'", modFile, e);
            return null;
        }
    }

    private static IModFileInfo parseModFileInfo(final IModFile iModFile) {
        return ModFileParser.modsTomlParser(iModFile);
    }

    private static IModFileInfo parseLibraryFileInfo(final IModFile iModFile) {
        return DummyModProvider.INSTANCE.manifestParser(iModFile);
    }

    private static ModJarMetadata newModJarMetadata() {
        try {
            return modJarMetadataConstructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean useModJarMetadata(final SecureJar jar) {
        final SecureJar.ModuleDataProvider data = jar.moduleDataProvider();
        if (data.findFile(PluginFileParser.MODULE_INFO).isPresent()) {
            return false;
        }
        return data.findFile(PluginFileParser.MODS_TOML).isPresent() || data.findFile(PluginPlatformConstants.METADATA_FILE_LOCATION).isPresent();
    }

    public static ModFile newModFile(final IModProvider provider, boolean allowUnknown, final Path... paths) {
        final ModJarMetadata mjm = newModJarMetadata();
        final SecureJar jar = SecureJar.from(j -> useModJarMetadata(j) ? mjm : JarMetadata.from(j, paths), paths);

        final SecureJar.ModuleDataProvider data = jar.moduleDataProvider();
        final String type = data.getManifest().getMainAttributes().getValue("FMLModType");

        if (data.findFile(PluginFileParser.MODS_TOML).isPresent()) {
            ModFile modFile = new ModFile(jar, provider, PluginFileParser::parseModFileInfo, type == null ? "MOD" : type);
            mjm.setModFile(modFile);
            return modFile;
        }

        if (data.findFile(PluginPlatformConstants.METADATA_FILE_LOCATION).isPresent()) {
            ModFile modFile = new ModFile(jar, provider, PluginFileParser::parsePluginFileInfo, type == null ? "MOD" : type);
            mjm.setModFile(modFile);
            return modFile;
        }

        if (!allowUnknown && type == null) {
            throw new IllegalArgumentException("Unknown mod file type");
        }

        return new ModFile(jar, provider, PluginFileParser::parseLibraryFileInfo, type == null ? "GAMELIBRARY" : type);
    }

    public static ModFile newLibraryFile(final IModProvider provider, final Path... paths) {
        final SecureJar jar = SecureJar.from(paths);
        return new ModFile(jar, provider, PluginFileParser::parseLibraryFileInfo, "GAMELIBRARY");
    }

    private static class DummyModProvider extends AbstractModProvider {
        private static final DummyModProvider INSTANCE = new DummyModProvider();

        @Override
        public String name() {
            return "dummy";
        }

        @Override
        public IModFileInfo manifestParser(IModFile mod) {
            return super.manifestParser(mod);
        }
    }
}
