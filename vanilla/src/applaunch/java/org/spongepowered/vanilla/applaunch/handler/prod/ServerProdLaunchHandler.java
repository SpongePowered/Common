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
package org.spongepowered.vanilla.applaunch.handler.prod;

import cpw.mods.modlauncher.api.ITransformingClassLoader;
import cpw.mods.modlauncher.api.ITransformingClassLoaderBuilder;
import org.spongepowered.vanilla.applaunch.Constants;
import org.spongepowered.vanilla.applaunch.Main;
import org.spongepowered.vanilla.applaunch.VanillaCommandLine;
import org.spongepowered.vanilla.applaunch.VanillaLaunchTargets;
import org.spongepowered.vanilla.applaunch.plugin.VanillaPluginEngine;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ServerProdLaunchHandler extends AbstractVanillaProdLaunchHandler {

    private final Path remappedJar = VanillaCommandLine.librariesDirectory.resolve(Constants.Libraries.MINECRAFT_PATH_PREFIX)
            .resolve(Constants.Libraries.MINECRAFT_VERSION_TARGET).resolve(Constants.Libraries.MINECRAFT_SERVER_JAR_NAME +
                    "_remapped.jar");

    @Override
    public String name() {
        return VanillaLaunchTargets.SERVER_PRODUCTION.getLaunchTarget();
    }

    @Override
    public void configureTransformationClassLoader(final ITransformingClassLoaderBuilder builder) {
        final Path path;
        try {
            path = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (final URISyntaxException ex) {
            throw new RuntimeException(ex);
        }

        builder.addTransformationPath(path);
        builder.addTransformationPath(this.remappedJar);

        super.configureTransformationClassLoader(builder);
    }

    @Override
    protected void launchService0(final String[] arguments, final ITransformingClassLoader launchClassLoader) throws Exception {
        Class.forName("org.spongepowered.vanilla.launch.DedicatedServerLauncher", true, launchClassLoader.getInstance())
                .getMethod("launch", VanillaPluginEngine.class, Boolean.class, String[].class)
                .invoke(null, Main.getInstance().getPluginEngine(), Boolean.TRUE, arguments);
    }
}
