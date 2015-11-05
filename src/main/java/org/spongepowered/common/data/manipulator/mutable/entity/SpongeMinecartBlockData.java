package org.spongepowered.common.data.manipulator.mutable.entity;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import net.minecraft.init.Blocks;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableMinecartBlockData;
import org.spongepowered.api.data.manipulator.mutable.entity.MinecartBlockData;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.common.data.manipulator.immutable.entity.ImmutableSpongeMinecartBlockData;
import org.spongepowered.common.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Map;

public class SpongeMinecartBlockData extends AbstractData<MinecartBlockData, ImmutableMinecartBlockData> implements MinecartBlockData {

    private BlockState block;
    private int offset;

    public SpongeMinecartBlockData() {
        this((BlockState) Blocks.air.getDefaultState(), 6);
    }

    public SpongeMinecartBlockData(BlockState block, int offset) {
        super(MinecartBlockData.class);

        this.block = Preconditions.checkNotNull(block);
        this.offset = offset;

        registerGettersAndSetters();
    }

    @Override
    public Value<BlockState> block() {
        return new SpongeValue<>(Keys.REPRESENTED_BLOCK, (BlockState) Blocks.air.getDefaultState(), this.block);
    }

    @Override
    public Value<Integer> offset() {
        return new SpongeValue<>(Keys.OFFSET, 6, this.offset);
    }

    @Override
    public MinecartBlockData copy() {
        return new SpongeMinecartBlockData(this.block, this.offset);
    }

    @Override
    public ImmutableMinecartBlockData asImmutable() {
        return new ImmutableSpongeMinecartBlockData(this.block, this.offset);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compareTo(MinecartBlockData o) {
        Map oTraits = o.block().get().getTraitMap();
        Map traits = this.block.getTraitMap();
        return ComparisonChain.start()
                .compare(oTraits.entrySet().containsAll(traits.entrySet()), traits.entrySet().containsAll(oTraits.entrySet()))
                .compare((Integer) this.offset, o.offset().get())
                .result();
    }

    @Override
    public DataContainer toContainer() {
        return new MemoryDataContainer()
                .set(Keys.REPRESENTED_BLOCK, this.block)
                .set(Keys.OFFSET, this.offset);
    }

    public BlockState getBlock() {
        return block;
    }

    public void setBlock(BlockState block) {
        this.block = Preconditions.checkNotNull(block);
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    protected void registerGettersAndSetters() {
        registerKeyValue(Keys.REPRESENTED_BLOCK, SpongeMinecartBlockData.this::block);
        registerKeyValue(Keys.OFFSET, SpongeMinecartBlockData.this::offset);

        registerFieldGetter(Keys.REPRESENTED_BLOCK, SpongeMinecartBlockData.this::getBlock);
        registerFieldGetter(Keys.OFFSET, SpongeMinecartBlockData.this::getOffset);

        registerFieldSetter(Keys.REPRESENTED_BLOCK, SpongeMinecartBlockData.this::setBlock);
        registerFieldSetter(Keys.OFFSET, SpongeMinecartBlockData.this::setOffset);
    }

}
