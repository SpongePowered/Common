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
package org.spongepowered.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.Level;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.entity.BlockEntityArchetype;
import org.spongepowered.api.block.entity.BlockEntityType;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataManipulator;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.block.entity.SpongeBlockEntityArchetypeBuilder;
import org.spongepowered.common.bridge.data.DataCompoundHolder;
import org.spongepowered.common.bridge.data.DataContainerHolder;
import org.spongepowered.common.data.holder.SpongeImmutableDataHolder;
import org.spongepowered.common.data.persistence.NBTTranslator;
import org.spongepowered.common.data.provider.nbt.NBTDataType;
import org.spongepowered.common.data.provider.nbt.NBTDataTypes;
import org.spongepowered.common.event.tracking.BlockChangeFlagManager;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.block.BlockPhase;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.DataUtil;
import org.spongepowered.common.util.PrettyPrinter;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.BlockChange;
import org.spongepowered.common.world.SpongeBlockChangeFlag;
import org.spongepowered.math.vector.Vector3i;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

@DefaultQualifier(NonNull.class)
public final class SpongeBlockSnapshot implements BlockSnapshot, SpongeImmutableDataHolder<BlockSnapshot>, DataContainerHolder.Immutable<BlockSnapshot>, DataCompoundHolder {

    private final BlockState blockState;
    private final ResourceKey worldKey;
    private final Vector3i pos;
    final @Nullable CompoundTag compound;
    // Internal use only
    private final BlockPos blockPos;
    private final SpongeBlockChangeFlag changeFlag;
    @Nullable WeakReference<ServerLevel> world;
    @MonotonicNonNull public BlockChange blockChange; // used for post event

    SpongeBlockSnapshot(final BuilderImpl builder, final boolean copyCompound) {
        this.blockState = Objects.requireNonNull(builder.blockState);
        this.worldKey = Objects.requireNonNull(builder.worldKey);
        this.pos = Objects.requireNonNull(builder.coordinates);
        this.blockPos = VecHelper.toBlockPos(this.pos);
        if (copyCompound) {
            // defensive copy as the builder may further be modified
            this.compound = builder.compound == null ? null : builder.compound.copy();
        } else {
            // pooled builder has been reset so this won't be modified.
            this.compound = builder.compound;
        }
        this.changeFlag = builder.flag;
        this.world = builder.worldRef;
        builder.worldRef = null;
    }

    SpongeBlockSnapshot() {
        this.blockState = (BlockState) Blocks.AIR.defaultBlockState();
        this.worldKey = Constants.World.INVALID_WORLD_KEY;
        this.pos = Vector3i.ZERO;
        this.blockPos = BlockPos.ZERO;
        this.compound = null;
        this.changeFlag = null;
    }

    @Override
    public BlockState state() {
        return this.blockState;
    }

    public net.minecraft.world.level.block.state.BlockState nativeState() {
        return ((net.minecraft.world.level.block.state.BlockState) this.blockState);
    }

    @Override
    public BlockSnapshot withState(final BlockState blockState) {
        return this.createBuilder().blockState(blockState).build();
    }

    @Override
    public BlockSnapshot withContainer(final DataContainer container) {
        return BuilderImpl.pooled().build(container).get();
    }

    @Override
    public ResourceKey world() {
        return this.worldKey;
    }

    @Override
    public Vector3i position() {
        return this.pos;
    }

    @Override
    public Optional<ServerLocation> location() {
        return this.getServerWorld()
                .map(world -> ServerLocation.of((ServerWorld) world, this.pos));
    }

    @Override
    public BlockSnapshot withLocation(final ServerLocation location) {
        return SpongeBlockSnapshot.BuilderImpl.pooled().from(this).position(location.blockPosition()).world(location.worldKey()).build();
    }

    @Override
    public boolean restore(final boolean force, final BlockChangeFlag flag) {
        // TODO - rewrite with the PhaseTracker being the hook or use SpongeImplHooks to do the restore.

        final Optional<ServerLevel> optionalWorld = this.getServerWorld();
        if (!optionalWorld.isPresent()) {
            return false;
        }

        final ServerLevel world = optionalWorld.get();
        // We need to deterministically define the context as nullable if we don't need to enter.
        // this way we guarantee an exit.
        try (final PhaseContext<?> context = BlockPhase.State.RESTORING_BLOCKS.createPhaseContext(PhaseTracker.getWorldInstance(world))) {
            context.buildAndSwitch();
            final BlockPos pos = VecHelper.toBlockPos(this.pos);
            if (!world.isInWorldBounds(pos)) { // Invalid position. Inline this check
                return false;
            }
            final net.minecraft.world.level.block.state.BlockState current = world.getBlockState(pos);
            final net.minecraft.world.level.block.state.BlockState replaced = (net.minecraft.world.level.block.state.BlockState) this.blockState;
            if (!force && (current.getBlock() != replaced.getBlock() || current != replaced)) {
                return false;
            }

            // Pre-emptively remove 'unsimilar' block entities to avoid drops
            // being created during block change removals
            if (!current.is(((net.minecraft.world.level.block.state.BlockState) this.blockState).getBlock())) {
                world.removeBlockEntity(pos);
            }
            world.setBlock(pos, replaced, BlockChangeFlagManager.andNotifyClients(flag).rawFlag());
            if (this.compound != null) {
                @Nullable BlockEntity te = world.getBlockEntity(pos);
                if (te != null) {
                    te.setBlockState((net.minecraft.world.level.block.state.BlockState) this.blockState);
                    te.loadWithComponents(this.compound, world.registryAccess());
                } else {
                    // Because, some mods will "unintentionally" only obey some of the rules but not all.
                    // In cases like this, we need to directly just say "fuck it" and deserialize from the compound directly.
                    try {

                        te = BlockEntity.loadStatic(pos, (net.minecraft.world.level.block.state.BlockState) this.blockState, this.compound, world.registryAccess());
                        if (te != null) {
                            world.getChunk(pos).setBlockEntity(te);
                        }
                    } catch (final Exception e) {
                        // Seriously? The mod should be broken then.
                        final PrettyPrinter printer = new PrettyPrinter(60).add("Unable to restore").centre().hr()
                            .add("A mod is not correctly deserializing a TileEntity that is being restored. ")
                            .addWrapped(60, "Note that this is not the fault of Sponge. Sponge is understanding that "
                                + "a block is supposed to have a TileEntity, but the mod is breaking the contract"
                                + "on how to re-create the tile entity. Please open an issue with the offending mod.")
                            .add("Here's the provided compound:");
                        printer.add();
                        try {
                            printer.addWrapped(80, "%s : %s", "This compound", this.compound);
                        } catch (final Throwable error) {
                            printer.addWrapped(
                                80,
                                "Unable to get the string of this compound. Printing out some of the entries to better assist"
                            );

                        }
                        printer.add()
                            .add("Desired World: " + this.worldKey)
                            .add("Position: " + this.pos)
                            .add("Desired BlockState: " + this.blockState);
                        printer.add();
                        printer.log(SpongeCommon.logger(), Level.ERROR);
                        return true; // I mean, I guess. the block was set up, but not the tile entity.
                    }

                }

                if (te != null) {
                    te.setChanged();
                }

            }
            // Finally, mark the location as being updated.
            world.getChunkSource().blockChanged(pos);
            return true;
        }
    }

    @Override
    public Optional<UUID> creator() {
        return Optional.empty();
    }

    @Override
    public Optional<UUID> notifier() {
        return Optional.empty();
    }

    @Override
    public Optional<BlockEntityArchetype> createArchetype() {
        if (this.compound == null) {
            return Optional.empty();
        }

        final String blockEntityId = this.compound.getString(Constants.Item.BLOCK_ENTITY_ID);

        final CompoundTag compound = this.compound.copy();
        compound.remove(Constants.Sponge.BlockSnapshot.TILE_ENTITY_POSITION_X);
        compound.remove(Constants.Sponge.BlockSnapshot.TILE_ENTITY_POSITION_Y);
        compound.remove(Constants.Sponge.BlockSnapshot.TILE_ENTITY_POSITION_Z);
        compound.remove(Constants.Item.BLOCK_ENTITY_ID);

        return Optional.of(SpongeBlockEntityArchetypeBuilder.pooled()
                .state(this.state())
                .blockEntity((BlockEntityType) SpongeCommon.vanillaRegistry(Registries.BLOCK_ENTITY_TYPE).getOptional(ResourceLocation.tryParse(blockEntityId)).orElse(null))
                .blockEntityData(NBTTranslator.INSTANCE.translate(compound))
                .build());
    }

    @Override
    public BlockSnapshot withRawData(final DataView container) throws InvalidDataException {
        return BuilderImpl.pooled().buildContent(container).orElseThrow(InvalidDataException::new);
    }

    @Override
    public boolean validateRawData(final DataView container) {
        return BuilderImpl.pooled().buildContent(container).isPresent();
    }

    @Override
    public BlockSnapshot copy() {
        return this;
    }

    @Override
    public int contentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        final DataContainer container = DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED)
                .set(Queries.CONTENT_VERSION, this.contentVersion())
                .set(Queries.WORLD_KEY, this.worldKey.asString())
                .createView(Constants.Sponge.SNAPSHOT_WORLD_POSITION)
                .set(Queries.POSITION_X, this.pos.x())
                .set(Queries.POSITION_Y, this.pos.y())
                .set(Queries.POSITION_Z, this.pos.z())
                .container()
                .set(Constants.Block.BLOCK_STATE, this.blockState);
        if (this.compound != null) {
            container.set(Constants.Sponge.UNSAFE_NBT, NBTTranslator.INSTANCE.translateFrom(this.compound));
        }
        return container;
    }

    public Optional<ServerLevel> getServerWorld() {
        @Nullable ServerLevel world = this.world != null ? this.world.get() : null;
        if (world == null) {
            world = (ServerLevel) Sponge.server().worldManager().world(this.worldKey).orElse(null);
            if (world != null) {
                this.world = new WeakReference<>(world);
            }
        }
        return Optional.ofNullable(world);
    }

    public Optional<CompoundTag> getCompound() {
        return this.compound == null ? Optional.empty() : Optional.of(this.compound.copy());
    }

    public BuilderImpl createBuilder() {
        final BuilderImpl builder = BuilderImpl.pooled();
        builder.blockState(this.blockState)
               .position(this.pos);
        final Optional<ServerLevel> optionalWorld = this.getServerWorld();
        if (optionalWorld.isPresent()) {
            builder.world(optionalWorld.get());
        } else {
            builder.world(this.worldKey);
        }
        if (this.compound != null) {
            builder.addUnsafeCompound(this.compound);
        }
        return builder;
    }

    @Override
    public DataContainer data$getDataContainer() {
        if (this.compound == null) {
            return DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED);
        }
        return NBTTranslator.INSTANCE.translate(this.compound);
    }

    @Override
    public BlockSnapshot data$withDataContainer(final DataContainer container) {
        final BuilderImpl builder = this.createBuilder();
        builder.compound = NBTTranslator.INSTANCE.translate(container);;
        return builder.build();
    }

    @Override
    public CompoundTag data$getCompound() {
        return this.compound == null ? new CompoundTag() : this.compound.copy();
    }

    @Override
    public void data$setCompound(final CompoundTag nbt) {
        // do nothing this is immutable
    }

    @Override
    public List<DataHolder> impl$delegateDataHolder() {
        return Arrays.asList(this, this.state(), this.state().type());
    }

    @Override
    public NBTDataType data$getNBTDataType() {
        return NBTDataTypes.BLOCK_ENTITY;
    }

    // Used internally for restores

    public SpongeBlockChangeFlag getChangeFlag() {
        return this.changeFlag;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final SpongeBlockSnapshot that = (SpongeBlockSnapshot) o;
        return this.blockState.equals(that.blockState) &&
               this.changeFlag == that.changeFlag &&
               Objects.equals(this.worldKey, that.worldKey) &&
               Objects.equals(this.pos, that.pos) &&
               Objects.equals(this.compound, that.compound);
    }

    @Override
    public int hashCode() {
        return Objects
            .hash(this.blockState,
                this.worldKey,
                this.pos,
                this.changeFlag,
                this.compound);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SpongeBlockSnapshot.class.getSimpleName() + "[", "]")
            .add("world=" + this.worldKey)
            .add("position=" + this.blockPos)
            .add("blockState=" + this.blockState)
            .toString();
    }

    public static final class BuilderImpl extends AbstractDataBuilder<@NonNull BlockSnapshot> implements BlockSnapshot.Builder {
        private static final Deque<BuilderImpl> pool = new ConcurrentLinkedDeque<>();

        public static BuilderImpl unpooled() {
            return new BuilderImpl(false);
        }

        public static BuilderImpl pooled() {
            final BuilderImpl builder = BuilderImpl.pool.pollFirst();
            if (builder != null) {
                return builder.reset();
            }
            return new BuilderImpl(true);
        }

        BlockState blockState;
        ResourceKey worldKey;
        @Nullable UUID creatorUniqueId;
        @Nullable UUID notifierUniqueId;
        Vector3i coordinates;
        @Nullable List<DataManipulator.Immutable> manipulators;
        @Nullable CompoundTag compound;
        SpongeBlockChangeFlag flag = (SpongeBlockChangeFlag) BlockChangeFlags.ALL;
        @Nullable WeakReference<ServerLevel> worldRef;
        private final boolean pooled;


        private BuilderImpl(final boolean pooled) {
            super(BlockSnapshot.class, 1);
            this.pooled = pooled;
        }

        @Override
        public @NonNull BuilderImpl world(final @NonNull ServerWorldProperties worldProperties) {
            this.worldKey = Objects.requireNonNull(worldProperties).key();
            this.worldRef = null;
            return this;
        }

        public BuilderImpl world(final ResourceKey key) {
            this.worldKey = Objects.requireNonNull(key);
            this.worldRef = null;
            return this;
        }

        public BuilderImpl world(final ServerLevel world) {
            this.worldKey = ((ServerWorld) Objects.requireNonNull(world)).key();
            this.worldRef = new WeakReference<>(world);
            return this;
        }

        @Override
        public @NonNull BuilderImpl blockState(final @NonNull BlockState blockState) {
            this.blockState = Objects.requireNonNull(blockState);
            return this;
        }

        public BuilderImpl blockState(final net.minecraft.world.level.block.state.BlockState blockState) {
            this.blockState = Objects.requireNonNull((BlockState) blockState);
            return this;
        }


        @Override
        public @NonNull BuilderImpl position(final @NonNull Vector3i position) {
            this.coordinates = Objects.requireNonNull(position);
            if (this.compound != null) {
                this.compound.putInt(Constants.Sponge.BlockSnapshot.TILE_ENTITY_POSITION_X, position.x());
                this.compound.putInt(Constants.Sponge.BlockSnapshot.TILE_ENTITY_POSITION_Y, position.y());
                this.compound.putInt(Constants.Sponge.BlockSnapshot.TILE_ENTITY_POSITION_Z, position.z());
            }
            return this;
        }

        @Override
        public BlockSnapshot.@NonNull Builder from(final @NonNull ServerLocation location) {
            return this.from(location.createSnapshot());
        }

        @Override
        public @NonNull BuilderImpl creator(final UUID uuid) {
            this.creatorUniqueId = Objects.requireNonNull(uuid);
            return this;
        }

        @Override
        public @NonNull BuilderImpl notifier(final UUID uuid) {
            this.notifierUniqueId = Objects.requireNonNull(uuid);
            return this;
        }

        @Override
        public <V> BlockSnapshot.@NonNull Builder add(final @NonNull Key<@NonNull ? extends Value<V>> key, final @NonNull V value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);

            this.blockState = this.blockState.with(key, value)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Key %s is not supported for block state %s",
                    key.key().asString(),
                    this.blockState.toString())));
            return this;
        }

        @Override
        public @NonNull BuilderImpl from(final BlockSnapshot holder) {
            Objects.requireNonNull(holder);

            this.blockState = holder.state();
            this.worldKey = holder.world();
            if (holder.creator().isPresent()) {
                this.creatorUniqueId = holder.creator().get();
            }
            if (holder.notifier().isPresent()) {
                this.notifierUniqueId = holder.notifier().get();
            }
            this.coordinates = holder.position();
            return this;
        }

        public BuilderImpl from(final SpongeBlockSnapshot snapshot) {
            Objects.requireNonNull(snapshot);

            this.blockState = snapshot.state();
            this.worldKey = snapshot.world();
            this.worldRef = snapshot.world;
            if (snapshot.compound != null) {
                // make a copy so that any changes to this compound in the builder
                // (position) won't accidently be reflected in the original snapshot.
                this.compound = snapshot.compound.copy();
            } else {
                this.compound = null;
            }
            this.coordinates = snapshot.position();
            this.flag = snapshot.getChangeFlag();
            return this;
        }

        public BlockState getBlockState() {
            return this.blockState;
        }

        public ResourceKey getWorldKey() {
            return this.worldKey;
        }

        public @Nullable UUID getCreatorUniqueId() {
            return this.creatorUniqueId;
        }

        public Vector3i getCoordinates() {
            return this.coordinates;
        }

        public @Nullable List<DataManipulator.Immutable> getManipulators() {
            return this.manipulators;
        }

        public @Nullable CompoundTag getCompound() {
            return this.compound;
        }

        public SpongeBlockChangeFlag getFlag() {
            return this.flag;
        }

        @Override
        public @NonNull BuilderImpl reset() {
            this.blockState = (BlockState) Blocks.AIR.defaultBlockState();
            this.worldKey = Constants.World.INVALID_WORLD_KEY;
            this.creatorUniqueId = null;
            this.notifierUniqueId = null;
            this.coordinates = null;
            this.manipulators = null;
            this.compound = null;
            this.flag = null;
            return this;
        }

        @Override
        public @NonNull SpongeBlockSnapshot build() {
            Objects.requireNonNull(this.blockState, "BlockState cannot be null!");
            final SpongeBlockSnapshot spongeBlockSnapshot = new SpongeBlockSnapshot(this, !this.pooled);
            this.reset();
            if (this.pooled) {
                BuilderImpl.pool.push(this);
            }
            return spongeBlockSnapshot;
        }

        @Override
        protected @NonNull Optional<BlockSnapshot> buildContent(final DataView container) throws InvalidDataException {

            if (!container.contains(Constants.Block.BLOCK_STATE, Constants.Sponge.SNAPSHOT_WORLD_POSITION)) {
                return Optional.empty();
            }

            // if we have no world-key check if we can find by uuid
            if (!container.contains(Queries.WORLD_KEY)) {
                if (!container.contains(Constants.Sponge.BlockSnapshot.WORLD_UUID)) {
                    return Optional.empty();
                }
                final UUID uuid = UUID.fromString(container.getString(Constants.Sponge.BlockSnapshot.WORLD_UUID).get());
                Sponge.server().worldManager().worldKey(uuid).ifPresent(worldKey -> container.set(Queries.WORLD_KEY, worldKey));
            }

            DataUtil.checkDataExists(container, Constants.Block.BLOCK_STATE);
            DataUtil.checkDataExists(container, Queries.WORLD_KEY);
            final BuilderImpl builder = BuilderImpl.pooled();
            final ResourceKey worldKey = container.getResourceKey(Queries.WORLD_KEY).get();
            final Vector3i coordinate = DataUtil.getPosition3i(container);
            final Optional<String> creatorUuid = container.getString(Queries.CREATOR_ID);
            final Optional<String> notifierUuid = container.getString(Queries.NOTIFIER_ID);

            final BlockState blockState = container.getSerializable(Constants.Block.BLOCK_STATE, BlockState.class).get();

            builder.blockState(blockState).world(worldKey).position(coordinate);

            creatorUuid.ifPresent(s -> builder.creator(UUID.fromString(s)));
            notifierUuid.ifPresent(s -> builder.notifier(UUID.fromString(s)));
            container.getView(Constants.Sponge.UNSAFE_NBT)
                .map(dataView -> NBTTranslator.INSTANCE.translate(dataView))
                .ifPresent(builder::addUnsafeCompound);
            return Optional.of(builder.build());
        }

        public BuilderImpl addUnsafeCompound(final CompoundTag compound) {
            Objects.requireNonNull(compound);

            this.compound = compound.copy();
            return this;
        }

        public BuilderImpl flag(final BlockChangeFlag flag) {
            this.flag = (SpongeBlockChangeFlag) flag;
            return this;
        }

        public BuilderImpl tileEntity(final BlockEntity added) {
            this.compound = added.saveWithFullMetadata(added.getLevel().registryAccess());
            return this;
        }
    }

    public static final class FactoryImpl implements Factory {

        static final class Holder {
            // Prevents this from calling the registry bootstrapping early.
            static final SpongeBlockSnapshot EMPTY = new SpongeBlockSnapshot();
        }

        @Override
        public BlockSnapshot empty() {
            return FactoryImpl.Holder.EMPTY;
        }
    }
}
