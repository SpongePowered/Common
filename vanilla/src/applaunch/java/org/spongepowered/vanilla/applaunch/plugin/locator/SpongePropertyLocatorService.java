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
package org.spongepowered.vanilla.applaunch.plugin.locator;

import org.spongepowered.plugin.Environment;
import org.spongepowered.plugin.builtin.jvm.JVMPluginResource;
import org.spongepowered.plugin.builtin.jvm.locator.JVMPluginResourceLocatorService;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class SpongePropertyLocatorService implements JVMPluginResourceLocatorService {

    @Override
    public String name() {
        return "sponge_resources";
    }

    @Override
    public Set<JVMPluginResource> locatePluginResources(final Environment environment) {
        environment.logger().info("Locating '{}' resources...", this.name());

        final Set<JVMPluginResource> resources = new HashSet<>();
        final String resourcesProp = System.getProperty("sponge.resources");
        if (resourcesProp != null) {
            for (final String entry : resourcesProp.split(File.pathSeparator)) {
                if (entry.isBlank()) {
                    continue;
                }

                final Path[] paths = Stream.of(entry.split("&")).map(Path::of).toArray(Path[]::new);
                resources.add(JVMPluginResource.create(environment, this.name(), paths));
            }
        }

        final String fsProp = System.getProperty("sponge.rootJarFS");
        if (fsProp != null) {
            try {
                final FileSystem fs = FileSystems.getFileSystem(new URI(fsProp));
                final Path spongeMod = newJarInJar(fs.getPath("jars", "spongevanilla-mod.jar"));
                resources.add(JVMPluginResource.create(environment, this.name(), spongeMod));
            } catch (final Exception e) {
                environment.logger().error("Failed to locate spongevanilla-mod jar.");
            }
        }

        environment.logger().info("Located [{}] resource(s) for '{}'...", resources.size(), this.name());

        return resources;
    }

    private static Path newJarInJar(final Path jar) {
        try {
            URI jij = new URI("jij:" + jar.toAbsolutePath().toUri().getRawSchemeSpecificPart()).normalize();
            final Map<String, ?> env = Map.of("packagePath", jar);
            FileSystem jijFS = FileSystems.newFileSystem(jij, env);
            return jijFS.getPath("/"); // root of the archive to load
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
