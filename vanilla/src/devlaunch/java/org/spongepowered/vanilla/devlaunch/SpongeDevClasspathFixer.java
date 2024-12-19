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
package org.spongepowered.vanilla.devlaunch;

import net.minecraftforge.bootstrap.api.BootstrapClasspathModifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpongeDevClasspathFixer implements BootstrapClasspathModifier {
    private static final boolean DEBUG = Boolean.getBoolean("bsl.debug");

    @Override
    public String name() {
        return "sponge-dev";
    }

    /**
     * IntelliJ only builds source sets that are explicitly on the classpath.
     * So we declare everything into the initial classpath then we sort things and put them back in the right classloaders.
     * Since different IDEs might want to put things in different locations, we sadly have to identify the resources by their filenames.
     */
    @Override
    public boolean process(final List<Path[]> classpath) {
        final Set<String> bootLibs = Set.of(System.getProperty("sponge.dev.boot").split(";"));
        final Set<String> gameShadedLibs = Set.of(System.getProperty("sponge.dev.gameShaded").split(";"));

        final Map<String, List<Path>> bootSourceSets = new HashMap<>();
        final Map<String, List<Path>> unknownProjects = new HashMap<>();

        final List<Path> spongeImplUnion = new ArrayList<>();
        final List<Path> gameLibs = new ArrayList<>();

        final AtomicBoolean hasAPISourceSet = new AtomicBoolean(false);

        final boolean modified = classpath.removeIf((paths) -> {
            if (paths.length != 1) { // empty or already merged by another service
                return false;
            }

            final Path path = paths[0];
            final SourceSet sourceSet = SourceSet.identify(path);
            if (sourceSet != null) {
                if (DEBUG) {
                    System.out.println("SourceSet (" + sourceSet + "): " + path);
                }

                switch (sourceSet.project()) {
                    case "modlauncher-transformers":
                        bootSourceSets.computeIfAbsent("transformers", k -> new LinkedList<>()).add(path);
                        break;
                    case "SpongeAPI":
                        switch (sourceSet.name()) {
                            case "ap":
                                // ignore
                                break;
                            case "main":
                                hasAPISourceSet.set(true);
                                // no break
                            default:
                                spongeImplUnion.add(path);
                                break;
                        }
                        break;
                    case "Sponge", "vanilla":
                        switch (sourceSet.name()) {
                            case "devlaunch":
                                // ignore
                                break;
                            case "applaunch":
                                bootSourceSets.computeIfAbsent("applaunch", k -> new LinkedList<>()).add(path);
                                break;
                            default:
                                spongeImplUnion.add(path);
                                break;
                        }
                        break;
                    default:
                        unknownProjects.computeIfAbsent(sourceSet.project(), k -> new LinkedList<>()).add(path);
                        break;
                }
                return true;
            }

            final String fileName = path.getFileName().toString();

            if (bootLibs.contains(fileName)) {
                if (DEBUG) {
                    System.out.println("Boot: " + path);
                }
                return false;
            }

            if (gameShadedLibs.contains(fileName)) {
                if (DEBUG) {
                    System.out.println("Sponge: " + path);
                }
                spongeImplUnion.add(path);
                return true;
            }

            if (DEBUG) {
                System.out.println("Game: " + path);
            }
            gameLibs.add(path);
            return true;
        });

        if (!modified) {
            return false;
        }

        for (final List<Path> sourceSets : bootSourceSets.values()) {
            classpath.add(sourceSets.toArray(Path[]::new));
        }

        if (hasAPISourceSet.get()) {
            spongeImplUnion.removeIf((path) -> {
                if (Files.isRegularFile(path)) {
                    final String fileName = path.getFileName().toString();
                    if (fileName.startsWith("spongeapi") && fileName.endsWith(".jar")) {
                        if (DEBUG) {
                            System.out.println("Removing spongeapi jar in favor of sourceset: " + path);
                        }
                        return true;
                    }
                }
                return false;
            });
        }

        final StringBuilder gameResourcesEnvBuilder = new StringBuilder();
        for (final Path resource : gameLibs) {
            gameResourcesEnvBuilder.append(resource).append(';');
        }
        for (final List<Path> project : unknownProjects.values()) {
            for (final Path resource : project) {
                gameResourcesEnvBuilder.append(resource).append('&');
            }
            gameResourcesEnvBuilder.setCharAt(gameResourcesEnvBuilder.length() - 1, ';');
        }
        for (final Path resource : spongeImplUnion) {
            gameResourcesEnvBuilder.append(resource).append('&');
        }
        gameResourcesEnvBuilder.setLength(gameResourcesEnvBuilder.length() - 1);
        final String gameResourcesEnv = gameResourcesEnvBuilder.toString();

        if (DEBUG) {
            System.out.println("Game resources env: " + gameResourcesEnv);
        }
        System.setProperty("sponge.gameResources", gameResourcesEnv);
        return true;
    }
}
