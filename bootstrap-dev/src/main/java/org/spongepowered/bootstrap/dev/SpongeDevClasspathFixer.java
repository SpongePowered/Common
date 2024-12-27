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
package org.spongepowered.bootstrap.dev;

import net.minecraftforge.bootstrap.api.BootstrapClasspathModifier;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
        final Path spongeRoot = Paths.get(System.getProperty("sponge.dev.root")).toAbsolutePath();
        final Set<String> ignoredLibs = Set.of("bootstrap-dev.jar");
        final Set<String> bootLibs = Set.of(System.getProperty("sponge.dev.boot").split(File.pathSeparator));
        final Set<String> gameShadedLibs = Set.of(System.getProperty("sponge.dev.gameShaded").split(File.pathSeparator));

        // boot layer
        final Multimap<String, Path> bootUnions = new Multimap<>();

        // game or plugin layer
        final Multimap<String, Path> unions = new Multimap<>();
        final List<Path> libs = new ArrayList<>();

        final AtomicBoolean hasAPISourceSet = new AtomicBoolean(false);

        final boolean modified = classpath.removeIf((paths) -> {
            if (paths.length != 1) { // empty or already merged by another service
                return false;
            }

            final Path path = paths[0];
            final SourceSet sourceSet = SourceSet.identify(path);
            if (sourceSet != null) {
                if (sourceSet.project().startsWith(spongeRoot)) {
                    if (DEBUG) {
                        System.out.println("Sponge SourceSet (" + sourceSet + "): " + path);
                    }

                    final String projectName = spongeRoot.relativize(sourceSet.project()).toString();
                    switch (projectName) {
                        case "bootstrap-dev":
                            // ignore
                            break;
                        case "modlauncher-transformers", "library-manager":
                            bootUnions.add(projectName, path);
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
                                    unions.add("sponge", path);
                                    break;
                            }
                            break;
                        case "", "vanilla", "forge":
                            switch (sourceSet.name()) {
                                case "applaunch":
                                    bootUnions.add("applaunch", path);
                                    break;
                                case "lang":
                                    unions.add("lang", path);
                                    break;
                                default:
                                    unions.add("sponge", path);
                                    break;
                            }
                            break;
                        default:
                            unions.add(projectName, path);
                            break;
                    }
                } else {
                    if (DEBUG) {
                        System.out.println("External SourceSet (" + sourceSet + "): " + path);
                    }

                    unions.add(sourceSet.project().toString(), path);
                }
                return true;
            }

            final String fileName = path.getFileName().toString();

            if (ignoredLibs.contains(fileName)) {
                if (DEBUG) {
                    System.out.println("Ignored: " + path);
                }
                return true;
            }

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
                unions.add("sponge", path);
                return true;
            }

            if (DEBUG) {
                System.out.println("Game: " + path);
            }
            libs.add(path);
            return true;
        });

        if (!modified) {
            return false;
        }

        for (final List<Path> sourceSets : bootUnions.values()) {
            classpath.add(sourceSets.toArray(Path[]::new));
        }

        if (hasAPISourceSet.get()) {
            unions.get("sponge").removeIf((path) -> {
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

        final StringBuilder resourcesEnvBuilder = new StringBuilder();
        for (final Path resource : libs) {
            resourcesEnvBuilder.append(resource).append(File.pathSeparator);
        }
        for (final List<Path> project : unions.values()) {
            for (final Path resource : project) {
                resourcesEnvBuilder.append(resource).append('&');
            }
            resourcesEnvBuilder.setCharAt(resourcesEnvBuilder.length() - 1, File.pathSeparatorChar);
        }
        resourcesEnvBuilder.setLength(resourcesEnvBuilder.length() - 1);
        final String resourcesEnv = resourcesEnvBuilder.toString();

        if (DEBUG) {
            System.out.println("Resources env: " + resourcesEnv);
        }
        System.setProperty("sponge.resources", resourcesEnv);
        return true;
    }
}
