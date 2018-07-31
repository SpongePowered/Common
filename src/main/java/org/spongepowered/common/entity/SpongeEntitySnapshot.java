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
package org.spongepowered.common.entity;

import static com.google.common.base.Preconditions.checkNotNull;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.Property;
import org.spongepowered.api.data.Queries;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityArchetype;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.event.CauseStackManager.StackFrame;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.data.DataProcessor;
import org.spongepowered.common.data.SpongeDataManager;
import org.spongepowered.common.data.persistence.NbtTranslator;
import org.spongepowered.common.data.util.DataQueries;
import org.spongepowered.common.data.util.DataUtil;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.interfaces.world.IMixinWorldInfo;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nullable;

public class SpongeEntitySnapshot implements EntitySnapshot {

    @Nullable private final UUID entityUuid;
    private final UUID worldUuid;
    private final EntityType entityType;
    private final Vector3d position;
    private final Vector3d rotation;
    private final Vector3d scale;
    private final ImmutableSet<ImmutableDataManipulator<?, ?>> vanillaManipulators;
    private final ImmutableSet<ImmutableDataManipulator<?, ?>> customManipulators;

    private final ImmutableBiMap<Key<?>, ImmutableValue<?>> map;
    @Nullable private final NBTTagCompound compound;
    @Nullable private final WeakReference<Entity> entityReference;
    // TODO write optimization to lazy load and evaluate all of the manipulators
    // for entities during events.
    private boolean isDirty = true;

    SpongeEntitySnapshot(SpongeEntitySnapshotBuilder builder) {
        this.entityType = builder.entityType;
        this.entityUuid = builder.entityId;
        if (builder.vanillaManipulators != null) {
            this.vanillaManipulators = ImmutableSet.copyOf(builder.vanillaManipulators);
            final ImmutableBiMap.Builder<Key<?>, ImmutableValue<?>> mapBuilder = ImmutableBiMap.builder();
            for (ImmutableDataManipulator<?, ?> manipulator : builder.vanillaManipulators) {
                for (Key key : manipulator.getKeys()) {
                    mapBuilder.put(key, (ImmutableValue<?>) manipulator.getValue(key).get());
                }
            }
            if (builder.customManipulators != null) {
                this.customManipulators = ImmutableSet.copyOf(builder.customManipulators);
                for (ImmutableDataManipulator<?, ?> manipulator : builder.customManipulators) {
                    for (Key key : manipulator.getKeys()) {
                        mapBuilder.put(key, (ImmutableValue<?>) manipulator.getValue(key).get());
                    }
                }
            } else {
                this.customManipulators = ImmutableSet.of();
            }
            this.map = mapBuilder.build();
        } else {
            this.vanillaManipulators = ImmutableSet.of();
            this.customManipulators = ImmutableSet.of();
            this.map = ImmutableBiMap.of();
        }
        this.compound = builder.compound == null ? null : builder.compound.copy();
        this.worldUuid = builder.worldId;
        this.position = builder.position == null ? Vector3d.ZERO : builder.position;
        this.rotation = builder.rotation == null ? Vector3d.ZERO : builder.rotation;
        this.scale = builder.scale == null ? Vector3d.ZERO : builder.scale;
        this.entityReference = builder.entityReference;
    }

    // internal use only
    public WeakReference<Entity> getEntityReference() {
        return this.entityReference;
    }

    @Override
    public Optional<UUID> getUniqueId() {
        return Optional.ofNullable(this.entityUuid);
    }

    @Override
    public Optional<Transform<World>> getTransform() {
        if (this.worldUuid == null) {
            return Optional.empty();
        }
        Optional<World> optional = SpongeImpl.getGame().getServer().getWorld(this.worldUuid);
        if (optional.isPresent()) {
            final Transform<World> transform = new Transform<>(optional.get(), this.position, this.rotation);
            return Optional.of(transform);
        }
        return Optional.empty();
    }

    @Override
    public EntityType getType() {
        return this.entityType;
    }

    @Override
    public Optional<Location<World>> getLocation() {
        if (this.worldUuid == null) {
            return Optional.empty();
        }
        Optional<World> optional = SpongeImpl.getGame().getServer().getWorld(this.worldUuid);
        if (optional.isPresent()) {
            final Location<World> location = new Location<>(optional.get(), this.position);
            return Optional.of(location);
        }
        return Optional.empty();
    }

    @Override
    public List<ImmutableDataManipulator<?, ?>> getManipulators() {
        return ImmutableList.copyOf(this.customManipulators);
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        final List<DataView> dataList = DataUtil.getSerializedImmutableManipulatorList(this.customManipulators);
        final DataContainer container = DataContainer.createNew()
                .set(Queries.CONTENT_VERSION, getContentVersion())
                .set(Queries.WORLD_ID, this.worldUuid.toString())
                .set(DataQueries.ENTITY_TYPE, this.entityType.getId())
                .createView(DataQueries.SNAPSHOT_WORLD_POSITION)
                .set(Queries.POSITION_X, this.position.getX())
                .set(Queries.POSITION_Y, this.position.getY())
                .set(Queries.POSITION_Z, this.position.getZ())
                .getContainer()
                .createView(DataQueries.ENTITY_ROTATION)
                .set(Queries.POSITION_X, this.rotation.getX())
                .set(Queries.POSITION_Y, this.rotation.getY())
                .set(Queries.POSITION_Z, this.rotation.getZ())
                .getContainer()
                .createView(DataQueries.ENTITY_SCALE)
                .set(Queries.POSITION_X, this.scale.getX())
                .set(Queries.POSITION_Y, this.scale.getY())
                .set(Queries.POSITION_Z, this.scale.getZ())
                .getContainer()
                .set(DataQueries.DATA_MANIPULATORS, dataList);

        if (this.entityUuid != null) {
            container.set(DataQueries.ENTITY_ID, this.entityUuid.toString());
        }
        if (this.compound != null) {
            container.set(DataQueries.UNSAFE_NBT, NbtTranslator.getInstance().translateFrom(this.compound));
        }

        for (ImmutableDataManipulator<?, ?> manipulator : this.vanillaManipulators) {
            for (Key k : manipulator.getKeys()) {
                manipulator.get(k).ifPresent(value -> container.set(k.getQuery(), value));
            }
        }

        return container;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ImmutableDataManipulator<?, ?>> Optional<T> get(Class<T> containerClass) {
        for (ImmutableDataManipulator<?, ?> manipulator : this.customManipulators) {
            if (containerClass.isInstance(manipulator)) {
                return Optional.of((T) manipulator);
            }
        }
        for (ImmutableDataManipulator<?, ?> manipulator : this.vanillaManipulators) {
            if (containerClass.isInstance(manipulator)) {
                return Optional.of((T) manipulator);
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <T extends ImmutableDataManipulator<?, ?>> Optional<T> getOrCreate(Class<T> containerClass) {
        final Optional<T> optional = get(containerClass);
        if (optional.isPresent()) {
            return optional;
        }
        // try harder
        final Optional<DataProcessor> processorOptional = DataUtil.getWildImmutableProcessor(containerClass);
        if (processorOptional.isPresent()) {
            if (processorOptional.get().supports(this.entityType)) {
                return Optional
                        .of((T) SpongeDataManager.getInstance().getWildBuilderForImmutable(containerClass).get().create().asImmutable());
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean supports(Class<? extends ImmutableDataManipulator<?, ?>> containerClass) {
        for (ImmutableDataManipulator<?, ?> manipulator : this.customManipulators) {
            if (containerClass.isInstance(manipulator)) {
                return true;
            }
        }
        final Optional<DataProcessor> processorOptional = DataUtil.getWildImmutableProcessor(containerClass);
        return processorOptional.isPresent() && processorOptional.get().supports(this.entityType);
    }

    @Override
    public <E> Optional<EntitySnapshot> transform(Key<? extends BaseValue<E>> key, Function<E, E> function) {
        checkNotNull(key);
        checkNotNull(function);
        ImmutableValue<?> value = this.map.get(key);
        if (value == null || value.equals(function.apply((E) value))) {
            return Optional.empty();
        }
        final ImmutableSet.Builder<ImmutableDataManipulator<?, ?>> builder = ImmutableSet.builder();
        boolean createNew = false;
        for (ImmutableDataManipulator<?, ?> manipulator : this.customManipulators) {
            if (manipulator.supports(key)) {
                createNew = true;
                builder.add(manipulator.with(key, checkNotNull(function.apply(manipulator.get(key).orElse(null)))).get());
            } else {
                builder.add(manipulator);
            }
        }
        if (createNew) {
            final SpongeEntitySnapshotBuilder snapshotBuilder = createBuilder();
            snapshotBuilder.customManipulators = builder.build();
            return Optional.of(snapshotBuilder.build());
        }
        return Optional.empty();
    }

    @Override
    public <E> Optional<EntitySnapshot> with(Key<? extends BaseValue<E>> key, E value) {
        return transform(key, input -> value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<EntitySnapshot> with(BaseValue<?> value) {
        return with((Key<? extends BaseValue<Object>>) value.getKey(), value.get());
    }

    @Override
    public Optional<EntitySnapshot> with(ImmutableDataManipulator<?, ?> valueContainer) {
        return Optional.of(createBuilder().add(valueContainer).build());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Optional<EntitySnapshot> with(Iterable<ImmutableDataManipulator<?, ?>> valueContainers) {
        final Builder builder = createBuilder();
        for (ImmutableDataManipulator manipulator : valueContainers) {
            builder.add(manipulator);
        }
        return Optional.of(builder.build());
    }

    @Override
    public Optional<EntitySnapshot> without(Class<? extends ImmutableDataManipulator<?, ?>> containerClass) {
        if (!supports(containerClass)) {
            return Optional.empty();
        }
        final ImmutableSet.Builder<ImmutableDataManipulator<?, ?>> builder = ImmutableSet.builder();
        for (ImmutableDataManipulator<?, ?> manipulator : this.customManipulators) {
            if (!containerClass.isAssignableFrom(manipulator.getClass())) {
                builder.add(manipulator);
            }
        }
        final SpongeEntitySnapshotBuilder snapshotBuilder = createBuilder();
        snapshotBuilder.customManipulators = builder.build();
        return Optional.of(snapshotBuilder.build());
    }

    @Override
    public EntitySnapshot merge(EntitySnapshot that) {
        return this;
    }

    @Override
    public EntitySnapshot merge(EntitySnapshot that, MergeFunction function) {
        return this;
    }

    @Override
    public List<ImmutableDataManipulator<?, ?>> getContainers() {
        return this.getManipulators();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> Optional<E> get(Key<? extends BaseValue<E>> key) {
        ImmutableValue<?> value = this.map.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((E) value.get());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E, V extends BaseValue<E>> Optional<V> getValue(Key<V> key) {
        ImmutableValue<?> value = this.map.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((V) value.asMutable());
    }

    @Override
    public boolean supports(Key<?> key) {
        return this.map.containsKey(key);
    }

    @Override
    public EntitySnapshot copy() {
        return this;
    }

    @Override
    public Set<Key<?>> getKeys() {
        return this.map.keySet();
    }

    @Override
    public Set<ImmutableValue<?>> getValues() {
        return this.map.values();
    }

    @Override
    public UUID getWorldUniqueId() {
        return this.worldUuid;
    }

    @Override
    public Vector3i getPosition() {
        return this.position.toInt();
    }

    @Override
    public EntitySnapshot withLocation(Location<World> location) {
        checkNotNull(location, "location");
        final SpongeEntitySnapshotBuilder builder = createBuilder();
        builder.position = location.getPosition();
        builder.worldId = location.getExtent().getUniqueId();
        NBTTagCompound newCompound = this.compound.copy();
        newCompound.setInteger("Dimension", ((IMixinWorldInfo) location.getExtent().getProperties()).getDimensionId());
        builder.compound = newCompound;
        return builder.build();
    }

    private SpongeEntitySnapshotBuilder createBuilder() {
        return new SpongeEntitySnapshotBuilder()
                .type(this.getType())
                .rotation(this.rotation)
                .scale(this.scale);
    }

    public Optional<NBTTagCompound> getCompound() {
        if (this.compound == null) {
            return Optional.empty();
        }
        return Optional.of(this.compound.copy());
    }

    @Override
    public Optional<Entity> restore() {
        if (this.entityReference != null) {
            Entity entity = this.entityReference.get();
            if (entity != null) {
                return Optional.of(entity);
            }
        }
        Optional<World> world = SpongeImpl.getGame().getServer().getWorld(this.worldUuid);
        if (!world.isPresent()) {
            return Optional.empty();
        }
        if (this.entityUuid != null) {
            Optional<Entity> entity = world.get().getEntity(this.entityUuid);
            if (entity.isPresent()) {
                return entity;
            }
        }
        try (StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.PLUGIN);
            Entity newEntity = world.get().createEntity(getType(), this.position);
            if (newEntity != null) {
                net.minecraft.entity.Entity nmsEntity = (net.minecraft.entity.Entity) newEntity;
                if (this.compound != null) {
                    nmsEntity.readFromNBT(this.compound);
                }
    
                boolean spawnResult = world.get().spawnEntity((Entity) nmsEntity);
                if (spawnResult) {
                    return Optional.of((Entity) nmsEntity);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public <T extends Property<?, ?>> Optional<T> getProperty(Class<T> propertyClass) {
        return Optional.empty();
    }

    @Override
    public Collection<Property<?, ?>> getApplicableProperties() {
        return ImmutableList.of();
    }

    @Override
    public EntityArchetype createArchetype() {
        EntityArchetype.Builder builder = new SpongeEntityArchetypeBuilder();
        builder.type(this.entityType);
        if (this.compound != null) {
            builder.entityData(NbtTranslator.getInstance().translate(this.compound));
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("uniqueId", this.entityUuid)
                .add("entityType", this.entityType)
                .add("position", this.position)
                .add("rotation", this.rotation)
                .add("scale", this.scale)
                .toString();
    }

    public Optional<ImmutableSet<ImmutableDataManipulator<?, ?>>> getVanillaManipulators() {
        return Optional.of(this.vanillaManipulators);
    }
}
