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

import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.entity.EntityArchetype;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.common.bridge.data.DataContainerHolder;
import org.spongepowered.common.data.AbstractArchetype;
import org.spongepowered.common.data.SpongeDataManager;
import org.spongepowered.common.data.nbt.validation.RawDataValidator;
import org.spongepowered.common.data.nbt.validation.ValidationType;
import org.spongepowered.common.data.nbt.validation.ValidationTypes;
import org.spongepowered.common.data.persistence.NBTTranslator;
import org.spongepowered.common.data.provider.DataProviderLookup;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.util.Constants;
import org.spongepowered.math.vector.Vector3d;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public final class SpongeEntityArchetype extends AbstractArchetype<EntityType, EntitySnapshot, org.spongepowered.api.entity.Entity>
    implements EntityArchetype, DataContainerHolder.Mutable {

    // TODO actually validate stuff
    public static final ImmutableList<RawDataValidator> VALIDATORS = ImmutableList.of();

    private static final DataProviderLookup lookup = SpongeDataManager.getProviderRegistry().getProviderLookup(SpongeEntityArchetype.class);

    private @Nullable Vector3d position;

    SpongeEntityArchetype(final SpongeEntityArchetypeBuilder builder) {
        this(builder.entityType, builder.compound);
        this.position = builder.position;
    }

    private SpongeEntityArchetype(final EntityType<@NonNull ?> type, @Nullable final CompoundTag compound) {
        super(type, compound != null ? compound : new CompoundTag());
    }

    @Override
    public EntityType<@NonNull ?> type() {
        return this.type;
    }

    public @Nullable CompoundTag getData() {
        return this.compound;
    }

    @Override
    public DataProviderLookup getLookup() {
        return SpongeEntityArchetype.lookup;
    }

    public Optional<Vector3d> getPosition() {
        if (this.position != null) {
            return Optional.of(this.position);
        }
        if (!this.compound.contains(Constants.Entity.ENTITY_POSITION, Constants.NBT.TAG_LIST)) {
            return Optional.empty();
        }
        try {
            final ListTag pos = this.compound.getList(Constants.Entity.ENTITY_POSITION, Constants.NBT.TAG_DOUBLE);
            final double x = pos.getDouble(0);
            final double y = pos.getDouble(1);
            final double z = pos.getDouble(2);
            this.position = new Vector3d(x, y, z);
            return Optional.of(this.position);
        } catch (final Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public DataContainer data$getDataContainer() {
        return this.entityData();
    }

    @Override
    public void data$setDataContainer(final DataContainer container) {
        this.compound = NBTTranslator.INSTANCE.translate(Objects.requireNonNull(container, "DataContainer cannot be null!"));
    }

    @Override
    public DataContainer entityData() {
        return NBTTranslator.INSTANCE.translateFrom(this.compound);
    }

    @Override
    public Optional<org.spongepowered.api.entity.Entity> apply(final ServerLocation location) {
        if (!PlatformHooks.INSTANCE.getGeneralHooks().onServerThread()) {
            return Optional.empty();
        }
        final org.spongepowered.api.world.server.ServerWorld spongeWorld = location.world();
        final ServerLevel level = (ServerLevel) spongeWorld;

        final ResourceLocation key = net.minecraft.world.entity.EntityType.getKey((net.minecraft.world.entity.EntityType<?>) this.type);
        if (key == null) {
            return Optional.empty();
        }

        final CompoundTag compound = this.compound.copy();
        compound.putString(Constants.Entity.ENTITY_TYPE_ID, key.toString());
        final ListTag pos = new ListTag();
        pos.add(DoubleTag.valueOf(location.x()));
        pos.add(DoubleTag.valueOf(location.y()));
        pos.add(DoubleTag.valueOf(location.z()));
        compound.put(Constants.Entity.ENTITY_POSITION, pos);
        compound.remove(Constants.Entity.ENTITY_UUID);

        final @Nullable Entity entity = net.minecraft.world.entity.EntityType.loadEntityRecursive(compound, level, EntitySpawnReason.LOAD, e -> {
            e.snapTo(location.x(), location.y(), location.z());
            if (e instanceof Mob mobentity) {
                mobentity.yHeadRot = mobentity.getYRot();
                mobentity.yBodyRot = mobentity.getXRot();
            }
            return e;
        });

        if (entity == null) {
            return Optional.empty();
        }

        if (level.tryAddFreshEntityWithPassengers(entity)) {
            return Optional.of((org.spongepowered.api.entity.Entity) entity);
        }
        return Optional.empty();
    }

    @Override
    public EntitySnapshot toSnapshot(final ServerLocation location) {
        final SpongeEntitySnapshotBuilder builder = new SpongeEntitySnapshotBuilder();
        builder.entityType = this.type;
        final CompoundTag newCompound = this.compound.copy();
        final Vector3d pos = location.position();
        newCompound.put(Constants.Entity.ENTITY_POSITION, Constants.NBT.newDoubleNBTList(pos.x(), pos.y(), pos.z()));
        newCompound.putString(Constants.Sponge.World.WORLD_KEY, location.worldKey().formatted());
        builder.compound = newCompound;
        builder.worldKey = location.world().properties().key();
        builder.position = pos;
        builder.rotation = this.getRotation();
        builder.scale = Vector3d.ONE;
        return builder.build();
    }

    private Vector3d getRotation() {
        final ListTag tag = this.compound.getList("Rotation", 5);
        final float rotationYaw = tag.getFloat(0);
        final float rotationPitch = tag.getFloat(1);
        return new Vector3d(rotationPitch, rotationYaw, 0);
    }

    @Override
    public int contentVersion() {
        return Constants.Sponge.EntityArchetype.BASE_VERSION;
    }

    @Override
    public DataContainer toContainer() {
        return DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED)
                .set(Queries.CONTENT_VERSION, this.contentVersion())
                .set(Constants.Sponge.EntityArchetype.ENTITY_TYPE, this.type)
                .set(Constants.Sponge.EntityArchetype.ENTITY_DATA, this.entityData());
    }

    @Override
    protected ValidationType getValidationType() {
        return ValidationTypes.ENTITY.get();
    }

    @Override
    public SpongeEntityArchetype copy() {
        return new SpongeEntityArchetype(this.type, this.compound.copy());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final SpongeEntityArchetype that = (SpongeEntityArchetype) o;
        return Objects.equals(this.position, that.position);
    }

    @Override
    protected ImmutableList<RawDataValidator> getValidators() {
        return SpongeEntityArchetype.VALIDATORS;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.position);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SpongeEntityArchetype.class.getSimpleName() + "[", "]")
                .add("position=" + this.position)
                .add("type=" + this.type)
                .toString();
    }
}
