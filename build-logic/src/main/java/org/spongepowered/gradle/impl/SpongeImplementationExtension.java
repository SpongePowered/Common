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

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.SourceSet;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SpongeImplementationExtension {

    public static final String MIXIN_CONFIGS_PROPERTY = "mixinConfigs";

    private final Project project;
    private final Logger logger;

    @Inject
    public SpongeImplementationExtension(final Project project, final Logger logger) {
        this.project = project;
        this.logger = logger;
    }

    public void copyModulesExcludingProvided(final Configuration source, final Configuration provided, final Configuration target) {
        final DependencyHandler deps = this.project.getDependencies();

        final Map<ModuleIdentifier, String> providedModuleVersions = new HashMap<>();
        for (ResolvedArtifact artifact : provided.getResolvedConfiguration().getResolvedArtifacts()) {
            final ComponentIdentifier id = artifact.getId().getComponentIdentifier();
            if (id instanceof ModuleComponentIdentifier) {
                final ModuleComponentIdentifier moduleId = (ModuleComponentIdentifier) id;
                providedModuleVersions.put(moduleId.getModuleIdentifier(), moduleId.getVersion());
            }
        }

        for (ResolvedArtifact artifact : source.getResolvedConfiguration().getResolvedArtifacts()) {
            final ComponentIdentifier id = artifact.getId().getComponentIdentifier();
            if (id instanceof ModuleComponentIdentifier) {
                final ModuleComponentIdentifier moduleId = (ModuleComponentIdentifier) id;
                final ModuleIdentifier module = moduleId.getModuleIdentifier();
                final String version = moduleId.getVersion();

                final String providedVersion = providedModuleVersions.get(module);
                if (providedVersion == null) {
                    ModuleDependency dep = (ModuleDependency) deps.create(module.getGroup() + ":" + module.getName() + ":" + version);
                    dep.setTransitive(false);
                    target.getDependencies().add(dep);
                } else if (!providedVersion.equals(version)) {
                    this.logger.warn("Version mismatch for module {}. {} expects {} but {} has {}.", module, source.getName(), version, provided.getName(), providedVersion);
                }
            }

            // projects are not copied because we cannot always recreate properly a project dependency from the resolved artefact
        }
    }

    public void addDependencyToRuntimeOnly(final SourceSet source, final SourceSet target) {
        this.addDependencyTo(source, target.getRuntimeOnlyConfigurationName());
    }

    public void addDependencyToImplementation(final SourceSet source, final SourceSet target) {
        this.addDependencyTo(source, target.getImplementationConfigurationName());
    }

    public void addDependencyTo(final SourceSet source, final String targetConfig) {
        this.project.getDependencies().add(targetConfig, source.getOutput());
    }

    public String generateImplementationVersionString(final String apiVersion, final String minecraftVersion, final String implRecommendedVersion) {
        return this.generateImplementationVersionString(apiVersion, minecraftVersion, implRecommendedVersion, null);
    }

    public String generateImplementationVersionString(final String apiVersion, final String minecraftVersion, final String implRecommendedVersion, final String addedVersionInfo) {
        final String latestApiVersion = generateApiReleasedVersion(apiVersion);
        final String implementationVersion = latestApiVersion + "." + implRecommendedVersion;

        return Stream.of(minecraftVersion, addedVersionInfo, implementationVersion)
            .filter(Objects::nonNull)
            .collect(Collectors.joining("-"));
    }

    private static String generateApiReleasedVersion(final String apiVersion) {
        final String[] apiSplit = apiVersion.replace("-SNAPSHOT", "").split("\\.");
        final boolean isSnapshot = apiVersion.contains("-SNAPSHOT");

        // This is to determine if the split api version has at the least a minimum version.
        final String apiMajor = apiSplit[0];
        final String minorVersion;
        if (apiSplit.length > 1) {
            minorVersion = apiSplit[1];
        } else {
            minorVersion = "0";
        }
        final int latestReleasedVersion = Math.max(Integer.parseInt(minorVersion) - 1, 0);
        // And then here, we determine if the api version still has a patch version, to just ignore it.
        final String latestReleasedApiMinor = isSnapshot ? String.valueOf(latestReleasedVersion) : minorVersion;
        return apiMajor + "." + latestReleasedApiMinor;
    }

    public String generatePlatformBuildVersionString(final String apiVersion, final String minecraftVersion, final String implRecommendedVersion) {
        return this.generatePlatformBuildVersionString(apiVersion, minecraftVersion, implRecommendedVersion, null);
    }

    public String generatePlatformBuildVersionString(final String apiVersion, final String minecraftVersion, final String implRecommendedVersion, final String addedVersionInfo) {
        final boolean isRelease = !implRecommendedVersion.endsWith("-SNAPSHOT");

        this.logger.lifecycle("Detected Implementation Version {} as {}", implRecommendedVersion, isRelease ? "Release" : "Snapshot");
        final String apiReleaseVersion = generateApiReleasedVersion(apiVersion);
        final String rawBuildNumber = System.getenv("BUILD_NUMBER");
        final int buildNumber = Integer.parseInt(rawBuildNumber == null ? "0" : rawBuildNumber);
        final String implVersionAsReleaseCandidateOrRecommended;
        if (isRelease) {
            implVersionAsReleaseCandidateOrRecommended = apiReleaseVersion + '.' + implRecommendedVersion;
        } else {
            implVersionAsReleaseCandidateOrRecommended = apiReleaseVersion + '.' + implRecommendedVersion.replace("-SNAPSHOT", "") + "-RC" + buildNumber;
        }
        return Stream.of(minecraftVersion, addedVersionInfo, implVersionAsReleaseCandidateOrRecommended)
            .filter(Objects::nonNull)
            .collect(Collectors.joining("-"));
    }

    /**
     * Get all mixin configurations that should be applied to this project
     * @return the mixin configuration files that should be applied to this project
     */
    public Set<String> getMixinConfigurations() {
        return this.getNamedConfigurations(SpongeImplementationExtension.MIXIN_CONFIGS_PROPERTY);
    }

    public Set<String> getNamedConfigurations(final String name) {
        final Set<String> configs = new HashSet<>();

        // if we have a parent
        final Project parentProject = this.project.getParent();
        if (parentProject != null) {
            SpongeImplementationExtension
                .splitAndAddIfNonNull(configs, (String) parentProject.findProperty(name));
        }

        // own project
        SpongeImplementationExtension.splitAndAddIfNonNull(configs, (String) this.project.findProperty(name));
        return configs;
    }

    private static void splitAndAddIfNonNull(final Collection<String> collector, final @Nullable String property) {
        if (property == null) {
            return;
        }

        final String[] split = property.split(",");
        Collections.addAll(collector, split);
    }

}
