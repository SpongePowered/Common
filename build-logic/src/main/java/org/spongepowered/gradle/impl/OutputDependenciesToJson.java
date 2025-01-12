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
package org.spongepowered.gradle.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public abstract class OutputDependenciesToJson extends DefaultTask {

    private static final char[] hexChars = "0123456789abcdef".toCharArray();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * A single dependency.
     */
    record DependencyDescriptor(String group, String module, String version, String sha512) implements Comparable<DependencyDescriptor> {
        @Override
        public int compareTo(final DependencyDescriptor that) {
            final int group = this.group.compareTo(that.group);
            if (group != 0) {
                return group;
            }

            final int module = this.module.compareTo(that.module);
            if (module != 0) {
                return module;
            }

            return this.version.compareTo(that.version);
        }
    }

    /**
     * A manifest containing a list of dependencies.
     *
     * <p>At runtime, transitive dependencies won't be traversed, so this needs to
     * include direct + transitive depends.</p>
     */
    record DependencyManifest(Map<String, List<DependencyDescriptor>> dependencies) {}

    /**
     * Configuration to gather dependency artifacts from.
     */
    @Nested
    public abstract MapProperty<String, ConfigurationHolder> getDependencies();

    public final void dependencies(final String key, final NamedDomainObjectProvider<Configuration> config) {
        this.getDependencies().put(key, config.map(ConfigurationHolder::new));
    }

    /**
     * Excludes configuration, to remove certain entries from dependencies and
     * transitive dependencies of {@link #getDependencies()}.
     */
    @Internal
    public abstract SetProperty<ResolvedArtifactResult> getExcludedDependencies();

    public final void excludeDependencies(final NamedDomainObjectProvider<Configuration> config) {
        this.getExcludedDependencies().addAll(config.flatMap(conf -> conf.getIncoming().getArtifacts().getResolvedArtifacts()));
    }

    /**
     * Classifiers to include in the dependency manifest. The empty string identifies no classifier.
     */
    @Input
    public abstract SetProperty<String> getAllowedClassifiers();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    public OutputDependenciesToJson() {
        this.getAllowedClassifiers().add("");
    }

    @TaskAction
    public void generateDependenciesJson() {
        final Set<ModuleComponentIdentifier> excludedDeps = new HashSet<>();
        if (this.getExcludedDependencies().isPresent()) {
            for (final ResolvedArtifactResult result : this.getExcludedDependencies().get()) {
                if (result.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier) {
                    excludedDeps.add((ModuleComponentIdentifier) result.getId().getComponentIdentifier());
                }
            }
        }

        final Map<String, ConfigurationHolder> inputConfigs = this.getDependencies().get();
        final Map<String, List<DependencyDescriptor>> dependenciesMap = new TreeMap<>();

        for (final Map.Entry<String, ConfigurationHolder> entry : inputConfigs.entrySet()) {
            dependenciesMap.put(entry.getKey(), this.configToDescriptor(entry.getValue().getArtifacts().get(), excludedDeps));
        }
        final DependencyManifest manifest = new DependencyManifest(dependenciesMap);

        this.getLogger().info("Writing to {}", this.getOutputFile().get().getAsFile());
        try (final BufferedWriter writer = Files.newBufferedWriter(this.getOutputFile().get().getAsFile().toPath())) {
            OutputDependenciesToJson.GSON.toJson(manifest, writer);
        } catch (final IOException ex) {
            throw new GradleException("Failed to write dependencies manifest", ex);
        }
    }

    private List<DependencyDescriptor> configToDescriptor(final Set<ResolvedArtifactResult> conf, final Set<ModuleComponentIdentifier> excludedDeps) {
        return conf.stream()
            .filter(dep -> {
                final ComponentIdentifier ident = dep.getId().getComponentIdentifier();
                return ident instanceof ModuleComponentIdentifier && !excludedDeps.contains(ident);
            })
            .distinct()
            .map(dependency -> {
                final ModuleComponentIdentifier id = (ModuleComponentIdentifier) dependency.getId().getComponentIdentifier();

                final MessageDigest digest;
                try {
                    digest = MessageDigest.getInstance("SHA-512");
                } catch (final NoSuchAlgorithmException e) {
                    throw new GradleException("Failed to find digest algorithm", e);
                }

                try (final InputStream in = Files.newInputStream(dependency.getFile().toPath())) {
                    final byte[] buf = new byte[4096];
                    int read;
                    while ((read = in.read(buf)) != -1) {
                        digest.update(buf, 0, read);
                    }
                } catch (final IOException e) {
                    throw new GradleException("Failed to digest file for " + dependency, e);
                }

                return new DependencyDescriptor(
                    id.getGroup(),
                    id.getModule(),
                    id.getVersion(),
                    OutputDependenciesToJson.toHexString(digest.digest())
                );
            })
            .sorted(Comparator.naturalOrder()) // sort dependencies for stable output
            .collect(Collectors.toList());
    }

    public static String toHexString(final byte[] bytes) {
        final char[] chars = new char[bytes.length << 1];
        int i = 0;
        for (final byte b : bytes) {
            chars[i++] = OutputDependenciesToJson.hexChars[(b >> 4) & 15];
            chars[i++] = OutputDependenciesToJson.hexChars[b & 15];
        }
        return new String(chars);
    }

    public static class ConfigurationHolder {
        private final Provider<Set<ResolvedArtifactResult>> configuration;

        public ConfigurationHolder(final Configuration configuration) {
            this.configuration = configuration.getIncoming().getArtifacts().getResolvedArtifacts();
        }

        @Internal
        public Provider<Set<ResolvedArtifactResult>> getArtifacts() {
            return this.configuration;
        }
    }
}
