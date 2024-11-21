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

import cpw.mods.modlauncher.Environment;
import cpw.mods.modlauncher.Launcher;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.moddiscovery.AbstractModProvider;
import net.minecraftforge.forgespi.locating.IDependencyLocator;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.forge.applaunch.loading.moddiscovery.library.Log4JLogger;
import org.spongepowered.forge.applaunch.transformation.SpongeForgeTransformationService;
import org.spongepowered.libs.LibraryManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// works with ForgeProductionBootstrap to make this whole thing go
public class SpongeForgeDependencyLocator extends AbstractModProvider implements IDependencyLocator {
    private static final Logger LOGGER = LogManager.getLogger();

    private LibraryManager libraryManager;

    @Override
    public List<IModFile> scanMods(Iterable<IModFile> loadedMods) {
        final List<IModFile> modFiles = new ArrayList<>();

        // Add Sponge-specific libraries
        if (FMLEnvironment.production) {
            try {
                this.libraryManager.validate();
            } catch (final Exception ex) {
                throw new RuntimeException("Failed to download and validate Sponge libraries", ex);
            }
            this.libraryManager.finishedProcessing();

            for (final LibraryManager.Library library : this.libraryManager.getAll("main")) {
                final Path path = library.file();
                SpongeForgeDependencyLocator.LOGGER.debug("Proposing jar {} as a game library", path);

                final IModLocator.ModFileOrException fileOrException = createMod(path);
                if (fileOrException.ex() != null) {
                    throw fileOrException.ex();
                }
                modFiles.add(fileOrException.file());
            }
        }

        return modFiles;
    }

    @Override
    protected String getDefaultJarModType() {
        return IModFile.Type.GAMELIBRARY.name();
    }

    @Override
    public String name() {
        return "spongeforge";
    }

    @Override
    public void initArguments(final Map<String, ?> arguments) {
        final Environment env = Launcher.INSTANCE.environment();
        this.libraryManager = new LibraryManager(
                new Log4JLogger(LogManager.getLogger(LibraryManager.class)),
                env.getProperty(SpongeForgeTransformationService.Keys.CHECK_LIBRARY_HASHES.get()).orElse(true),
                env.getProperty(SpongeForgeTransformationService.Keys.LIBRARIES_DIRECTORY.get())
                        .orElseThrow(() -> new IllegalStateException("no libraries available")),
                SpongeForgeModLocator.class.getResource("libraries.json")
        );
    }
}
