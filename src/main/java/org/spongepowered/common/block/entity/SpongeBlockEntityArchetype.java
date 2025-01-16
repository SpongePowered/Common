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
package org.spongepowered.common.block.entity;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.BlockEntityType;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.data.AbstractArchetype;
import org.spongepowered.common.data.SpongeDataManager;
import org.spongepowered.common.data.nbt.validation.RawDataValidator;
import org.spongepowered.common.data.nbt.validation.ValidationType;
import org.spongepowered.common.data.nbt.validation.ValidationTypes;
import org.spongepowered.common.data.persistence.NBTTranslator;
import org.spongepowered.common.data.provider.DataProviderLookup;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.VecHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public final class SpongeBlockEntityArchetype extends AbstractArchetype<BlockEntityType, BlockSnapshot, BlockEntity> implements
        org.spongepowered.api.block.entity.BlockEntityArchetype {

    final BlockState blockState;

    // TODO actually validate stuff
    public static final ImmutableList<RawDataValidator> VALIDATORS = ImmutableList.of();

    private static final DataProviderLookup lookup = SpongeDataManager.getProviderRegistry().getProviderLookup(SpongeBlockEntityArchetype.class);

    private net.minecraft.world.level.block.entity.@Nullable BlockEntity cachedBlockEntity;

    SpongeBlockEntityArchetype(final SpongeBlockEntityArchetypeBuilder builder) {
        super(builder.type, NBTTranslator.INSTANCE.translate(builder.data));
        this.blockState = builder.blockState;
    }

    @Override
    public BlockState state() {
        return this.blockState;
    }

    @Override
    public BlockEntityType blockEntityType() {
        return this.type;
    }

    @Override
    public DataContainer blockEntityData() {
        return NBTTranslator.INSTANCE.translateFrom(this.compound);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Optional<BlockEntity> apply(final ServerLocation location) {
        final BlockState currentState = location.block();
        final Block currentBlock = ((net.minecraft.world.level.block.state.BlockState) currentState).getBlock();
        final Block newBlock = ((net.minecraft.world.level.block.state.BlockState) this.blockState).getBlock();
        final Level minecraftWorld = (net.minecraft.world.level.Level) location.world();

        final BlockPos blockpos = VecHelper.toBlockPos(location);
        if (currentBlock != newBlock) {
            ((org.spongepowered.api.world.World) minecraftWorld).setBlock(blockpos.getX(), blockpos.getY(), blockpos.getZ(), this.blockState,
                    BlockChangeFlags.ALL);
        }
        final CompoundTag compound = this.compound.copy();

        final net.minecraft.world.level.block.entity.@Nullable BlockEntity tileEntity = minecraftWorld.getBlockEntity(blockpos);
        if (tileEntity == null) {
            return Optional.empty();
        }
        compound.putInt(Constants.TileEntity.X_POS, blockpos.getX());
        compound.putInt(Constants.TileEntity.Y_POS, blockpos.getY());
        compound.putInt(Constants.TileEntity.Z_POS, blockpos.getZ());
        tileEntity.setBlockState((net.minecraft.world.level.block.state.BlockState) currentState);
        tileEntity.loadWithComponents(compound, minecraftWorld.registryAccess());
        return Optional.of((org.spongepowered.api.block.entity.BlockEntity) tileEntity);
    }

    @Override
    public BlockSnapshot toSnapshot(final ServerLocation location) {
        final SpongeBlockSnapshot.BuilderImpl builder = SpongeBlockSnapshot.BuilderImpl.pooled();
        return builder.blockState(this.blockState)
            .addUnsafeCompound(this.compound.copy())
            .world(location.worldKey())
            .position(location.blockPosition())
            .build();
    }

    @Override
    public int contentVersion() {
        return Constants.Sponge.BlockEntityArchetype.BASE_VERSION;
    }

    @Override
    public DataContainer toContainer() {
        return DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED)
                .set(Queries.CONTENT_VERSION, this.contentVersion())
                .set(Constants.Sponge.BlockEntityArchetype.BLOCK_ENTITY_TYPE, this.type)
                .set(Constants.Sponge.BlockEntityArchetype.BLOCK_STATE, this.blockState)
                .set(Constants.Sponge.BlockEntityArchetype.BLOCK_ENTITY_DATA, this.blockEntityData())
                ;
    }

    @Override
    public DataProviderLookup getLookup() {
        return SpongeBlockEntityArchetype.lookup;
    }

    @Override
    protected ValidationType getValidationType() {
        return ValidationTypes.BLOCK_ENTITY.get();
    }

    @Override
    public SpongeBlockEntityArchetype copy() {
        final SpongeBlockEntityArchetypeBuilder builder = SpongeBlockEntityArchetypeBuilder.pooled();
        builder.type = this.type;
        builder.data = NBTTranslator.INSTANCE.translate(this.compound);
        builder.blockState = this.blockState;
        return builder.build();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final SpongeBlockEntityArchetype that = (SpongeBlockEntityArchetype) o;
        return this.blockState.equals(that.blockState);
    }

    @Override
    protected ImmutableList<RawDataValidator> getValidators() {
        return SpongeBlockEntityArchetype.VALIDATORS;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.blockState);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SpongeBlockEntityArchetype.class.getSimpleName() + "[", "]")
                .add("type=" + this.type)
                .add("state=" + this.blockState)
                .add("data=" + this.compound).toString();
    }

    public List<DataHolder> impl$delegateDataHolder() {
        if (this.cachedBlockEntity == null) {
            final CompoundTag compound = this.compound.copy();
            compound.putString(Constants.Item.BLOCK_ENTITY_ID, SpongeCommon.vanillaRegistry(Registries.BLOCK_ENTITY_TYPE).getKey((net.minecraft.world.level.block.entity.BlockEntityType<?>) this.type).toString());
            this.cachedBlockEntity = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(new BlockPos(0, 0, 0), (net.minecraft.world.level.block.state.BlockState) this.blockState, compound, SpongeCommon.vanillaRegistryAccess());
        }
        return Arrays.asList(this, (DataHolder) this.cachedBlockEntity);
    }
}
