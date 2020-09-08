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
package org.spongepowered.vanilla.applaunch;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.util.PathConverter;
import joptsimple.util.PathProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Manifest;

public final class VanillaCommandLine {

    private static final OptionParser PARSER = new OptionParser();
    private static final ArgumentAcceptingOptionSpec<String> LAUNCH_TARGET_ARG = PARSER.accepts("launchTarget", "Launch Target")
            .withRequiredArg();
    private static final ArgumentAcceptingOptionSpec<Path> GAME_DIRECTORY_ARG = PARSER.accepts("gameDir", "Alternative game directory")
            .withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.DIRECTORY_EXISTING)).defaultsTo(Paths.get("."));
    private static final ArgumentAcceptingOptionSpec<Path> LIBRARIES_DIRECTORY_ARG = PARSER.accepts("librariesDir", "Alternative libraries directory")
            .withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.DIRECTORY_EXISTING)).defaultsTo(Paths.get("libraries"));
    private static final ArgumentAcceptingOptionSpec<Boolean> DOWNLOAD_MINECRAFT_JAR = PARSER.accepts("downloadMinecraftJar", "If true, we'll "
            + "download the Minecraft jar.").withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
    private static final ArgumentAcceptingOptionSpec<Boolean> CHECK_MINECRAFT_JAR_HASH = PARSER.accepts("checkMinecraftJarHash", "If true, we'll "
            + "verify the Minecraft jar hash (to protect against corruption/etc).").withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
    private static final ArgumentAcceptingOptionSpec<Boolean> DOWNLOAD_SRG_MAPPINGS = PARSER.accepts("downloadSrgMappings", "If true, we'll "
            + "download the SRG mappings").withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);

    public static String[] RAW_ARGS;
    public static VanillaLaunchTargets launchTarget;
    public static Path gameDirectory, librariesDirectory;
    public static boolean downloadMinecraftJar, checkMinecraftJarHash, downloadSrgMappings;

    public static void configure(String[] args) throws IOException {
        VanillaCommandLine.PARSER.allowsUnrecognizedOptions();

        final OptionSet options = VanillaCommandLine.PARSER.parse(args);

        String launchTarget = options.valueOf(VanillaCommandLine.LAUNCH_TARGET_ARG);
        boolean manifestTarget = false;
        if (launchTarget == null) {
            // Try the manifest before we give up
            try (final InputStream stream = VanillaCommandLine.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
                if (stream != null) {
                    final Manifest manifest = new Manifest(stream);
                    launchTarget = manifest.getMainAttributes().getValue(Constants.ManifestAttributes.LAUNCH_TARGET);
                    manifestTarget = true;
                }
            }
        }

        if (launchTarget == null) {
            throw new RuntimeException("No launch target has been specified! Check your run configs/manifest...");
        }

        VanillaCommandLine.launchTarget = VanillaLaunchTargets.from(launchTarget);
        if (VanillaCommandLine.launchTarget == null) {
            throw new RuntimeException("Invalid launch target specified!");
        }

        if (manifestTarget) {
            String[] temp = new String[args.length + 2];
            System.arraycopy(args, 0, temp, 0, args.length);
            temp[args.length] = "--launchTarget";
            temp[args.length + 1] = launchTarget;
            args = temp;
        }

        VanillaCommandLine.gameDirectory = options.valueOf(VanillaCommandLine.GAME_DIRECTORY_ARG);
        VanillaCommandLine.librariesDirectory = options.valueOf(VanillaCommandLine.LIBRARIES_DIRECTORY_ARG);
        VanillaCommandLine.downloadMinecraftJar = options.valueOf(VanillaCommandLine.DOWNLOAD_MINECRAFT_JAR);
        VanillaCommandLine.checkMinecraftJarHash = options.valueOf(VanillaCommandLine.CHECK_MINECRAFT_JAR_HASH);
        VanillaCommandLine.downloadSrgMappings = options.valueOf(VanillaCommandLine.DOWNLOAD_SRG_MAPPINGS);

        VanillaCommandLine.RAW_ARGS = args;
    }

    private VanillaCommandLine() {
    }
}
