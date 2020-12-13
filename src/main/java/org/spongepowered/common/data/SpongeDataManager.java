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
package org.spongepowered.common.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.minecraft.util.registry.Registry;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataHolderBuilder;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.data.DataManipulator.Mutable;
import org.spongepowered.api.data.DataProvider;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataContentUpdater;
import org.spongepowered.api.data.persistence.DataSerializable;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.persistence.DataTranslator;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.EntityArchetype;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.event.data.ChangeDataHolderEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.data.builder.item.SpongeItemStackSnapshotDataBuilder;
import org.spongepowered.common.data.key.KeyBasedDataListener;
import org.spongepowered.common.data.persistence.datastore.DataStoreRegistry;
import org.spongepowered.common.data.provider.CustomDataProvider;
import org.spongepowered.common.data.provider.DataProviderRegistry;
import org.spongepowered.common.entity.SpongeEntityArchetypeBuilder;
import org.spongepowered.common.entity.SpongeEntitySnapshotBuilder;
import org.spongepowered.common.item.SpongeItemStackBuilder;
import org.spongepowered.common.registry.MappedRegistry;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.world.storage.SpongePlayerData;
import org.spongepowered.common.world.storage.SpongePlayerDataBuilder;
import org.spongepowered.plugin.PluginContainer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Singleton
public final class SpongeDataManager implements DataManager {

    private static SpongeDataManager INSTANCE;

    public static SpongeDataManager getInstance() {
        return SpongeDataManager.INSTANCE;
    }

    private final DataStoreRegistry dataStoreRegistry;
    private final DataProviderRegistry dataProviderRegistry;
    private final Map<ResourceKey, SpongeDataRegistration> registrations;
    private final Map<Class<?>, DataBuilder<?>> builders;
    private final Map<Class<? extends DataHolder.Immutable<?>>, DataHolderBuilder.Immutable<?, ?>> immutableDataBuilderMap;
    private final Map<Class<? extends DataSerializable>, List<DataContentUpdater>> updatersMap;
    private final List<DataContentUpdater> customDataUpdaters;
    private final Map<String, List<SpongeDataRegistration>> registrationByPluginId;
    private final Map<String, SpongeDataRegistration> legacyRegistrations;
    private final List<KeyBasedDataListener<?>> keyListeners;

    @Inject
    private SpongeDataManager() {
        SpongeDataManager.INSTANCE = this;

        this.dataStoreRegistry = new DataStoreRegistry();
        this.dataProviderRegistry = new DataProviderRegistry();
        this.registrations = new HashMap<>();
        this.builders = new HashMap<>();
        this.immutableDataBuilderMap = new MapMaker()
                .concurrencyLevel(4)
                .makeMap();
        this.updatersMap = new IdentityHashMap<>();
        this.customDataUpdaters = new ArrayList<>();
        this.registrationByPluginId = new IdentityHashMap<>();
        this.legacyRegistrations = new HashMap<>();
        this.keyListeners = new ArrayList<>();
    }

    @Override
    public <T extends DataSerializable> void registerBuilder(final Class<T> clazz, final DataBuilder<T> builder) {
        Objects.requireNonNull(builder);

        if (this.builders.putIfAbsent(clazz, builder) != null) {
            SpongeCommon.getLogger().warn("A DataBuilder has already been registered for {}. Attempted to register {} instead.", clazz,
                    builder.getClass());
        } else if (!(builder instanceof AbstractDataBuilder)) {
            SpongeCommon.getLogger().warn("A custom DataBuilder is not extending AbstractDataBuilder! It is recommended that "
                    + "the custom data builder does extend it to gain automated content versioning updates and maintain "
                    + "simplicity. The offending builder's class is: {}", builder.getClass());
        }
    }

    @Override
    public <T extends DataSerializable> void registerContentUpdater(final Class<T> clazz, final DataContentUpdater updater) {
        Objects.requireNonNull(updater);

        final List<DataContentUpdater> updaters = this.updatersMap.computeIfAbsent(clazz, k -> new ArrayList<>());
        updaters.add(updater);
        updaters.sort(Constants.Functional.DATA_CONTENT_UPDATER_COMPARATOR);
    }

    public void registerCustomDataContentUpdater(final DataContentUpdater updater) {
        this.customDataUpdaters.add(updater);
    }

    @Override
    public <T extends DataSerializable> Optional<DataContentUpdater> getWrappedContentUpdater(final Class<T> clazz, final int fromVersion,
            final int toVersion) {
        if (fromVersion == toVersion) {
            throw new IllegalArgumentException("Attempting to convert to the same version!");
        }
        if (fromVersion < toVersion) {
            throw new IllegalArgumentException("Attempting to backwards convert data! This isn't supported!");
        }
        final List<DataContentUpdater> updaters = this.updatersMap.get(clazz);
        if (updaters == null) {
            return Optional.empty();
        }
        return SpongeDataManager.getWrappedContentUpdater(clazz, fromVersion, toVersion, updaters);
    }

    public Optional<DataContentUpdater> getWrappedCustomContentUpdater(final Class<Mutable> mutableClass, final int version, final int currentCustomData) {
        return SpongeDataManager.getWrappedContentUpdater(mutableClass, version, currentCustomData, this.customDataUpdaters);
    }

    private static Optional<DataContentUpdater> getWrappedContentUpdater(final Class<?> clazz, final int fromVersion, final int toVersion, final List<DataContentUpdater> updaters) {
        ImmutableList.Builder<DataContentUpdater> builder = ImmutableList.builder();
        int version = fromVersion;
        for (DataContentUpdater updater : updaters) {
            if (updater.getInputVersion() == version) {
                if (updater.getOutputVersion() > toVersion) {
                    continue;
                }
                version = updater.getOutputVersion();
                builder.add(updater);
            }
        }
        if (version < toVersion || version > toVersion) { // There wasn't a registered updater for the version being requested
            final Exception e = new IllegalStateException("The requested content version for: " + clazz.getSimpleName() + " was requested, "
                                                    + "\nhowever, the versions supplied: from "+ fromVersion + " to " + toVersion + " is impossible"
                                                    + "\nas the latest version registered is: " + version+". Please notify the developer of"
                                                    + "\nthe requested consumed DataSerializable of this error.");
            e.printStackTrace();
            return Optional.empty();
        }
        return Optional.of(new DataUpdaterDelegate(builder.build(), fromVersion, toVersion));
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T extends DataSerializable> Optional<DataBuilder<T>> getBuilder(final Class<T> clazz) {
        final DataBuilder<?> dataBuilder = this.builders.get(clazz);
        if (dataBuilder != null) {
            return Optional.of((DataBuilder<T>) dataBuilder);
        }
        return Optional.ofNullable((DataBuilder<T>) this.immutableDataBuilderMap.get(clazz));
    }

    @Override
    public <T extends DataSerializable> Optional<T> deserialize(final Class<T> clazz, final DataView dataView) {
        Objects.requireNonNull(dataView);

        return this.getBuilder(clazz).flatMap(builder -> builder.build(dataView));
    }

    @Override
    public <T extends DataHolder.Immutable<T>, B extends DataHolderBuilder.Immutable<T, B>> void register(final Class<T> holderClass, final B builder) {
        Objects.requireNonNull(builder);

        final DataHolderBuilder.Immutable<?, ?> previous = this.immutableDataBuilderMap.putIfAbsent(holderClass, builder);
        if (previous != null) {
            throw new IllegalStateException("Already registered the DataUtil for " + holderClass.getCanonicalName());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DataHolder.Immutable<T>, B extends DataHolderBuilder.Immutable<T, B>> Optional<B> getImmutableBuilder(final Class<T> holderClass) {
        return Optional.ofNullable((B) this.immutableDataBuilderMap.get(Objects.requireNonNull(holderClass)));
    }

    public void registerKeyListeners() {
        this.keyListeners.forEach(this::registerKeyListener0);
        this.keyListeners.clear();
    }

    private void registerKeyListener0(final KeyBasedDataListener<?> listener) {
        Sponge.getEventManager().registerListener(listener.getOwner(), ChangeDataHolderEvent.ValueChange.class, listener);
    }

    @Override
    public void registerLegacyManipulatorIds(final String legacyId, final DataRegistration registration) {
        Objects.requireNonNull(legacyId);
        Objects.requireNonNull(registration);

        final SpongeDataRegistration previous = this.legacyRegistrations.putIfAbsent(legacyId, (SpongeDataRegistration) registration);
        if (previous != null) {
            throw new IllegalStateException("Legacy registration id already registered: id" + legacyId + " for registration: " + registration);
        }
    }

    public Optional<DataRegistration> getRegistrationForLegacyId(final String id) {
        return Optional.ofNullable(this.legacyRegistrations.get(id));
    }

    @SuppressWarnings(value = {"unchecked", "rawtypes"})
    @Override
    public <T> Optional<DataTranslator<T>> getTranslator(final Class<T> objectClass) {
        final Registry<DataTranslator> registry = SpongeCommon.getRegistry().getCatalogRegistry().getRegistry(DataTranslator.class);
        final DataTranslator reverseMapping = ((MappedRegistry<DataTranslator, Class>) registry).getReverseMapping(objectClass);

        return Optional.ofNullable(reverseMapping);
    }

    @Override
    public Collection<DataRegistration> getAllRegistrationsFor(final PluginContainer container) {
        return Collections.unmodifiableCollection(this.registrationByPluginId.getOrDefault(container.getMetadata().getId(), Collections.emptyList()));
    }

    @Override
    public DataContainer createContainer() {
        return new MemoryDataContainer();
    }

    @Override
    public DataContainer createContainer(final DataView.SafetyMode safety) {
        return new MemoryDataContainer(safety);
    }

    public <E extends DataHolder> void registerKeyListener(final KeyBasedDataListener<E> keyListener) {
        Objects.requireNonNull(keyListener);

        this.keyListeners.add(keyListener);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void registerCustomDataRegistration(final SpongeDataRegistration registration) {
        if (!registration.key.getNamespace().equals(registration.plugin.getMetadata().getId())) {
            throw new IllegalStateException(String.format("Registration namespace (%s) is not matching plugin id (%s)", registration.key, registration.plugin.getMetadata().getId()));
        }

        this.registrations.put(registration.getKey(), registration);
        this.registrationByPluginId.computeIfAbsent(registration.getPluginContainer().getMetadata().getId(), k -> new ArrayList<>()).add(registration);

        for (final DataStore dataStore : registration.getDataStores()) {
            this.dataStoreRegistry.register(dataStore, registration.getKeys());
        }

        for (final Key key : registration.getKeys()) {
            this.registerCustomDataProviderForKey(registration, key);
        }
    }

    private <V extends Value<E>, E> void registerCustomDataProviderForKey(final SpongeDataRegistration registration, final Key<V> key) {
        final Collection<DataProvider<V, E>> providers = registration.getProvidersFor(key);

        final Set<Type> dataStoreSupportedTokens = new HashSet<>();
        this.dataStoreRegistry.getDataStores(key).stream().map(DataStore::getSupportedTypes).forEach(dataStoreSupportedTokens::addAll);

        for (final DataProvider<V, E> provider : providers) {
            this.dataProviderRegistry.register(provider);
            dataStoreSupportedTokens.removeIf(provider::isSupported);
        }

        // For all tokens supported by a datastore register a CustomDataProvider
        if (!dataStoreSupportedTokens.isEmpty()) {
            this.dataProviderRegistry.register(new CustomDataProvider<>(key, dataStoreSupportedTokens));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void registerDataRegistration(final SpongeDataRegistration registration) {
        this.registrations.put(registration.getKey(), registration);
        this.registrationByPluginId.computeIfAbsent(registration.getPluginContainer().getMetadata().getId(), k -> new ArrayList<>()).add(registration);
        for (final DataStore dataStore : registration.getDataStores()) {
            this.dataStoreRegistry.register(dataStore, registration.getKeys());
        }
        for (final Key key : registration.getKeys()) {
            final Collection<DataProvider<?, ?>> providers = registration.getProvidersFor(key);
            for (DataProvider<?, ?> provider : providers) {
                this.dataProviderRegistry.register(provider);
            }
        }
    }

    public void registerDefaultProviders() {
        this.dataProviderRegistry.registerDefaultProviders();
    }

    public static DataStoreRegistry getDatastoreRegistry() {
        return SpongeDataManager.INSTANCE.dataStoreRegistry;
    }

    public static DataProviderRegistry getProviderRegistry() {
        return SpongeDataManager.INSTANCE.dataProviderRegistry;
    }

    public Optional<DataRegistration> getRegistration(final ResourceKey key) {
        return Optional.ofNullable(this.registrations.get(key));
    }

    public void registerDefaultBuilders() {
        this.registerBuilder(ItemStack.class, new SpongeItemStackBuilder());
        this.registerBuilder(ItemStackSnapshot.class, new SpongeItemStackSnapshotDataBuilder());
        this.registerBuilder(EntitySnapshot.class, new SpongeEntitySnapshotBuilder());
        this.registerBuilder(EntityArchetype.class, new SpongeEntityArchetypeBuilder());
        this.registerBuilder(SpongePlayerData.class, new SpongePlayerDataBuilder());
    }
}
