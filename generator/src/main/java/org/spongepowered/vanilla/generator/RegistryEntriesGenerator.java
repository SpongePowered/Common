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
package org.spongepowered.vanilla.generator;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;

import java.io.IOException;
import java.util.Comparator;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

// Generates a constants file based on registry entries
class RegistryEntriesGenerator<V> implements Generator {

    private final String relativePackageName;
    private final String targetClassSimpleName;
    private final String registryTypeName;
    private final ResourceKey<? extends Registry<V>> registry;
    private final Predicate<V> filter;
    private final TypeName valueType;
    private final RegistryScope scopeOverride;
    private final TypeName registryValueType;

    RegistryEntriesGenerator(
        final String targetRelativePackage,
        final String targetClassSimpleName,
        final String registryTypeName,
        final TypeName valueType,
        final ResourceKey<? extends Registry<V>> registry
    ) {
        this(targetRelativePackage, targetClassSimpleName, registryTypeName, valueType, registry, $ -> true);
    }

    RegistryEntriesGenerator(
        final String targetRelativePackage,
        final String targetClassSimpleName,
        final String registryTypeName,
        final TypeName valueType,
        final ResourceKey<? extends Registry<V>> registry,
        final Predicate<V> filter
    ) {
        this(targetRelativePackage, targetClassSimpleName, registryTypeName, valueType, registry, filter, null);
    }

    RegistryEntriesGenerator(
        final String targetRelativePackage,
        final String targetClassSimpleName,
        final String registryTypeName,
        final TypeName valueType,
        final ResourceKey<? extends Registry<V>> registry,
        final Predicate<V> filter,
        final RegistryScope scopeOverride
    ) {
        this(targetRelativePackage, targetClassSimpleName, registryTypeName, valueType, registry, filter, scopeOverride, valueType);
    }

    public RegistryEntriesGenerator(final String relativePackageName,
            final String targetClassSimpleName,
            final String registryTypeName,
            final TypeName valueType,
            final ResourceKey<? extends Registry<V>> registry,
            final Predicate<V> filter,
            final RegistryScope scopeOverride,
            final TypeName registryValueType) {
        this.relativePackageName = relativePackageName;
        this.targetClassSimpleName = targetClassSimpleName;
        this.registryTypeName = registryTypeName;
        this.registry = registry;
        this.filter = filter;
        this.valueType = valueType;
        this.scopeOverride = scopeOverride;
        this.registryValueType = registryValueType;
    }

    @Override
    public String name() {
        return "elements of registry " + this.registry.location();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void generate(final Context ctx) throws IOException {
        final var clazz = Types.utilityClass(
            this.targetClassSimpleName,
            Generator.GENERATED_FILE_JAVADOCS
        );
        clazz.addAnnotation(Types.suppressWarnings("unused"));

        final RegistryScope scopeType;
        Registry<V> registry = (Registry<V>) BuiltInRegistries.REGISTRY.get(this.registry.location()).map(Holder.Reference::value).orElse(null);
        if (registry == null) {
            registry = ctx.registries().lookup(this.registry).orElse(null);
            if (registry == null) {
                throw new IllegalArgumentException("Unknown registry " + this.registry);
            }
            scopeType = this.scopeOverride != null ? this.scopeOverride : RegistryScope.WORLD;
        } else {
            scopeType = this.scopeOverride != null ? this.scopeOverride : RegistryScope.GAME;
        }

        clazz.addAnnotation(scopeType.registryScopeAnnotation());
        final var fieldType = ParameterizedTypeName.get(scopeType.registryReferenceType(), this.valueType);
        final var registryMethod = scopeType.registryGetter(this.registryTypeName, this.registryValueType);
        final var factoryMethod = scopeType.registryReferenceFactory(this.registryTypeName, this.valueType);

        final Registry<V> finalRegistry = registry;
        registry.stream()
            .filter(this.filter)
            .map(v -> this.makeField(this.targetClassSimpleName, fieldType, factoryMethod, finalRegistry.getKey(v), v instanceof FeatureElement fe ? fe.requiredFeatures() : null))
            .sorted(Comparator.comparing(f -> f.name))
            .forEachOrdered(clazz::addField);

        clazz.addMethod(registryMethod);
        clazz.addMethod(factoryMethod);

        ctx.write(this.relativePackageName, clazz.build());
        ctx.compilationUnit(this.relativePackageName, this.targetClassSimpleName);
    }

    private FieldSpec makeField(final String ownType, final TypeName fieldType, final MethodSpec factoryMethod, final ResourceLocation element, @Nullable final FeatureFlagSet featureFlagSet) {

        final FieldSpec.Builder builder =
                FieldSpec.builder(fieldType, Types.keyToFieldName(element.getPath()), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$L.$N($L)", ownType, factoryMethod, Types.resourceKey(element));
        if (featureFlagSet != null) {
            if (!featureFlagSet.isSubsetOf(FeatureFlags.VANILLA_SET)) {
                final var flags = FeatureFlags.REGISTRY.toNames(featureFlagSet).stream().map(rl -> rl.getNamespace().equals("minecraft") ? rl.getPath() : rl.getNamespace() + ":" + rl.getPath()).toArray();
                // Use this when new feature flags are introduced
//                if (featureFlagSet.contains(FeatureFlags.WINTER_DROP)) {
//                    var annotation = AnnotationSpec.builder(ClassName.get("org.spongepowered.api.util.annotation", "Experimental"))
//                            .addMember("value", "$S", flags).build();
//                    builder.addAnnotation(annotation).build();
//                    builder.addAnnotation(ApiStatus.Experimental.class).build();
//                }
            }


        }
        return builder.build();

    }
}
