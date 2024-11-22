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
package org.spongepowered.common.registry;

import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.registry.DuplicateRegistrationException;
import org.spongepowered.api.registry.Registry;
import org.spongepowered.api.registry.RegistryHolder;
import org.spongepowered.api.registry.RegistryRoots;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.api.registry.ValueNotFoundException;
import org.spongepowered.common.accessor.core.MappedRegistryAccessor;
import org.spongepowered.common.accessor.resources.ResourceKeyAccessor;
import org.spongepowered.common.bridge.core.MappedRegistryBridge;
import org.spongepowered.common.bridge.core.WritableRegistryBridge;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class RegistryHolderLogic implements RegistryHolder {

    private final Map<ResourceKey, net.minecraft.core.Registry<net.minecraft.core.Registry<?>>> roots = new Object2ObjectOpenHashMap<>();

    public RegistryHolderLogic() {
        this.roots.put(
            (ResourceKey) (Object) ResourceLocation.withDefaultNamespace("root"),
            new MappedRegistry<>(
                net.minecraft.resources.ResourceKey.createRegistryKey((ResourceLocation) (Object) RegistryRoots.MINECRAFT),
                Lifecycle.experimental()
            )
        );
        final ResourceLocation sponge = ResourceLocation.fromNamespaceAndPath("sponge", "root");
        this.roots.put(
            (ResourceKey) (Object) sponge,
            new MappedRegistry<>(
                ResourceKeyAccessor.invoker$create(
                    sponge,
                    sponge
                ),
                Lifecycle.stable()
            )
        );
    }

    public RegistryHolderLogic(final RegistryAccess dynamicAccess) {
        this();

        final WritableRegistry root = (WritableRegistry) this.roots.get(ResourceLocation.withDefaultNamespace("root"));
        // Add the dynamic registries. These are server-scoped in Vanilla

        dynamicAccess.registries().forEach(entry -> root.register(entry.key(), entry.value(), RegistrationInfo.BUILT_IN));
        root.freeze();
    }

    public void setRootMinecraftRegistry(final net.minecraft.core.Registry<net.minecraft.core.Registry<?>> rootRegistry) {
        this.roots.put(RegistryRoots.MINECRAFT, rootRegistry);
    }

    @Override
    public <T> Registry<T> registry(final RegistryType<T> type) {
        final var root = this.roots.get(Objects.requireNonNull(type, "type").root());
        if (root == null) {
            throw new ValueNotFoundException(String.format("No '%s' root registry has been defined", type.root()));
        }
        final var registry = root.getOptional((ResourceLocation) (Object) type.location())
            .orElseThrow(() -> new ValueNotFoundException(String.format("No '%s' registry has been defined in root '%s'", type.location(), type.root())));
        return (Registry<T>) registry;
    }

    @Override
    public <T> Optional<Registry<T>> findRegistry(final RegistryType<T> type) {
        final net.minecraft.core.Registry<net.minecraft.core.Registry<?>> root = this.roots.get(Objects.requireNonNull(type, "type").root());
        if (root == null) {
            return Optional.empty();
        }
        return (Optional<Registry<T>>) (Object) root.getOptional((ResourceLocation) (Object) type.location());
    }

    @Override
    public Stream<Registry<?>> streamRegistries(final ResourceKey root) {
        final net.minecraft.core.Registry<net.minecraft.core.Registry<?>> rootRegistry = this.roots.get(Objects.requireNonNull(root, "root"));
        if (rootRegistry == null) {
            return Stream.empty();
        }
        return (Stream<Registry<?>>) (Object) rootRegistry.stream();
    }

    public <T> Registry<T> createRegistry(final RegistryType<T> type, final @Nullable Map<ResourceKey, T> defaultValues) {
        return this.createRegistry(type, defaultValues != null ? () -> defaultValues : null, false);
    }

    public <T> Registry<T> createRegistry(final RegistryType<T> type, final @Nullable Supplier<Map<ResourceKey, T>> defaultValues) {
        return this.createRegistry(type, defaultValues, false);
    }

    public <T> Registry<T> createRegistry(final RegistryType<T> type, final @Nullable Supplier<Map<ResourceKey, T>> defaultValues,
        final boolean isDynamic) {
        return this.createRegistry(type, InitialRegistryData.noIds(defaultValues), this.registrySupplier(isDynamic, null), false);
    }

    @SuppressWarnings("unchecked")
    public <T> Function<net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<T>>, net.minecraft.core.Registry<T>> registrySupplier(
            final boolean isDynamic,
            final @Nullable BiConsumer<net.minecraft.resources.ResourceKey<T>, T> callback) {
        if (callback == null) {
            return (key) -> {
                final MappedRegistry<T> reg = new MappedRegistry<>(key, Lifecycle.stable());
                ((WritableRegistryBridge<T>)reg).bridge$setDynamic(isDynamic);
                return reg;
            };
        } else {
            return (key) -> {
                final CallbackRegistry<T> reg = new CallbackRegistry<>(key, Lifecycle.stable(), callback);
                ((WritableRegistryBridge<T>) (Object) reg).bridge$setDynamic(isDynamic);
                return reg;
            };
        }
    }

    public <T> Registry<T> createRegistry(final RegistryType<T> type, final @Nullable InitialRegistryData<T> defaultValues,
            final Function<net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<T>>, net.minecraft.core.Registry<T>> registrySupplier, final boolean replace) {
        final net.minecraft.core.Registry<net.minecraft.core.Registry<?>> root = this.roots.get(Objects.requireNonNull(type, "type").root());
        if (root == null) {
            throw new ValueNotFoundException(String.format("No '%s' root registry has been defined", type.root()));
        }
        var registry = root.getValue((ResourceLocation) (Object) type.location());
        final boolean exists = registry != null;
        if (!replace && exists) {
            throw new DuplicateRegistrationException(String.format("Registry '%s' in root '%s' has already been defined", type.location(), type.root()));
        }
        final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<T>> key;
        if (Registries.ROOT_REGISTRY_NAME.equals(type.root())) {
            key = net.minecraft.resources.ResourceKey.createRegistryKey((ResourceLocation) (Object) type.location());
        } else {
            key = ResourceKeyAccessor.invoker$create(
                    (ResourceLocation) (Object) RegistryRoots.SPONGE,
                    (ResourceLocation) (Object) type.location()
            );
        }
        registry = registrySupplier.apply(key);

        if (defaultValues != null) {
            final MappedRegistry<T> mr = (MappedRegistry<T>) registry;
            defaultValues.forEach((vk, vi, vv) -> {
                mr.register(
                    net.minecraft.resources.ResourceKey.create(key, (ResourceLocation) (Object) vk),
                    vv,
                    RegistrationInfo.BUILT_IN
                );
                vi.ifPresent(id -> {
                    if (mr.getId(vv) != id) {
                        throw new IllegalStateException("Registry entry " + vk + " was expected to have id of " + id + " but was instead " + mr.getId(vv));
                    }
                });
            });
        }

        // This is so wrong and dirty and only because we don't have layered registries...
        final boolean frozen = ((MappedRegistryAccessor<net.minecraft.core.Registry<T>>) root).accessor$frozen();

        if (replace && exists) {
            ((MappedRegistryAccessor<net.minecraft.core.Registry<T>>) root).accessor$frozen(false);
            ((MappedRegistryBridge<net.minecraft.core.Registry<T>>) root).bridge$forceRemoveValue(key);
        }

        ((WritableRegistry) root).register(key, registry, RegistrationInfo.BUILT_IN);
        if (registry instanceof CallbackRegistry) {
            ((CallbackRegistry<?>) registry).setCallbackEnabled(true);
        }
        ((MappedRegistryAccessor<net.minecraft.core.Registry<T>>) root).accessor$frozen(frozen);

        return (Registry<T>) registry;
    }

    public void freezeSpongeRootRegistry() {
        this.roots.get(RegistryRoots.SPONGE).freeze();
    }

    public void freezeSpongeDynamicRegistries() {
        this.roots.get(RegistryRoots.SPONGE).forEach(net.minecraft.core.Registry::freeze);
    }
}
