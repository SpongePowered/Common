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
package org.spongepowered.forge.applaunch.loading.moddiscovery.locator;

import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import net.minecraftforge.fml.loading.ModDirTransformerDiscoverer;
import net.minecraftforge.fml.loading.StringUtils;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.common.applaunch.AppLaunch;
import org.spongepowered.forge.applaunch.loading.moddiscovery.AbstractModProvider;
import org.spongepowered.forge.applaunch.loading.moddiscovery.PluginFileParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class PluginsDirectoryLocator extends AbstractModProvider implements IModLocator {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public List<ModFileOrException> scanMods() {
        final List<Path> pluginDirectories = AppLaunch.pluginPlatform().pluginDirectories();

        final List<ModFileOrException> modFiles = new ArrayList<>();

        for (final Path pluginDirectory : pluginDirectories) {
            PluginsDirectoryLocator.LOGGER.debug("Scanning plugins directory '{}' for plugins", pluginDirectory);
            this.scanForPluginsIn(pluginDirectory).map((f) -> new ModFileOrException(f, null)).forEach(modFiles::add);
        }

        return modFiles;
    }

    @SuppressWarnings("UnstableApiUsage")
    private Stream<ModFile> scanForPluginsIn(final Path pluginsDirectory) {
        final List<Path> excluded = ModDirTransformerDiscoverer.allExcluded();
        return LamdbaExceptionUtils.uncheck(() -> Files.list(pluginsDirectory))
            .filter((p) -> !excluded.contains(p) && StringUtils.toLowerCase(p.getFileName().toString()).endsWith(".jar"))
            .sorted(Comparator.comparing((path) -> StringUtils.toLowerCase(path.getFileName().toString())))
            .map((p) -> PluginFileParser.newModFile(this, false, p))
            .filter(ModFile::identifyMods);
    }

    @Override
    public String name() {
        return "plugins directory";
    }
}
