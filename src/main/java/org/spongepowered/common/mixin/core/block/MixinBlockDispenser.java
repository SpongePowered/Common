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
package org.spongepowered.common.mixin.core.block;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableDirectionalData;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.data.ImmutableDataCachingUtil;
import org.spongepowered.common.data.manipulator.immutable.block.ImmutableSpongeDirectionalData;
import org.spongepowered.common.data.util.DirectionResolver;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseData;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.block.BlockPhase;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

@Mixin(BlockDispenser.class)
public abstract class MixinBlockDispenser extends MixinBlock {

    private ItemStackSnapshot originalItem;
    private PhaseContext<?> context;

    @Shadow protected abstract void dispense(World worldIn, BlockPos pos);

    @Override
    public ImmutableList<ImmutableDataManipulator<?, ?>> getManipulators(IBlockState blockState) {
        return ImmutableList.<ImmutableDataManipulator<?, ?>>of(getDirectionalData(blockState));
    }

    @Override
    public boolean supports(Class<? extends ImmutableDataManipulator<?, ?>> immutable) {
        return ImmutableDirectionalData.class.isAssignableFrom(immutable);
    }

    @Override
    public Optional<BlockState> getStateWithData(IBlockState blockState, ImmutableDataManipulator<?, ?> manipulator) {
        if (manipulator instanceof ImmutableDirectionalData) {
            return Optional.of((BlockState) blockState.withProperty(BlockDispenser.FACING, DirectionResolver.getFor(((ImmutableDirectionalData) manipulator).direction().get())));
        }
        return super.getStateWithData(blockState, manipulator);
    }

    @Override
    public <E> Optional<BlockState> getStateWithValue(IBlockState blockState, Key<? extends BaseValue<E>> key, E value) {
        if (key.equals(Keys.DIRECTION)) {
            return Optional.of((BlockState) blockState.withProperty(BlockDispenser.FACING, DirectionResolver.getFor((Direction) value)));
        }
        return super.getStateWithValue(blockState, key, value);
    }

    private ImmutableDirectionalData getDirectionalData(IBlockState blockState) {
        return ImmutableDataCachingUtil.getManipulator(ImmutableSpongeDirectionalData.class,
                DirectionResolver.getFor(blockState.getValue(BlockDispenser.FACING)));
    }

    @Inject(method = "dispense", at = @At(value = "HEAD"))
    public void onDispenseHead(World worldIn, BlockPos pos, CallbackInfo ci) {
        final IBlockState state = worldIn.getBlockState(pos);
        final SpongeBlockSnapshot spongeBlockSnapshot = ((IMixinWorldServer) worldIn).createSpongeBlockSnapshot(state, state, pos, BlockChangeFlags.ALL);
        final IMixinChunk mixinChunk = (IMixinChunk) worldIn.getChunkFromBlockCoords(pos);
        this.context = BlockPhase.State.DISPENSE.createPhaseContext()
            .source(spongeBlockSnapshot)
            .owner(() -> mixinChunk.getBlockOwner(pos))
            .notifier(() -> mixinChunk.getBlockNotifier(pos))
            .buildAndSwitch();
    }

    @Inject(method = "dispense", at = @At(value = "RETURN"))
    public void onDispenseReturn(World worldIn, BlockPos pos, CallbackInfo ci) {
        this.context.close();
    }

    @Redirect(method = "dispense", at = @At(value = "INVOKE", target = "Lnet/minecraft/dispenser/IBehaviorDispenseItem;dispense(Lnet/minecraft/dispenser/IBlockSource;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"))
    public ItemStack onSpongeDispense(IBehaviorDispenseItem ibehaviordispenseitem, IBlockSource source, ItemStack stack) {
        this.originalItem = ItemStackUtil.snapshotOf(stack);
        return ibehaviordispenseitem.dispense(source, stack);
    }

    @Redirect(method = "dispense", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntityDispenser;setInventorySlotContents(ILnet/minecraft/item/ItemStack;)V"))
    public void onSetInventoryContents(TileEntityDispenser dispenser, int index, @Nullable ItemStack stack) {
        final PhaseData phaseData = PhaseTracker.getInstance().getCurrentPhaseData();
        final PhaseContext<?> context = phaseData.context;
        // If we captured nothing, simply set the slot contents and return
        if (context.getCapturedItemsOrEmptyList().isEmpty()) {
            dispenser.setInventorySlotContents(index, stack);
            return;
        }
        final ItemStack dispensedItem = context.getCapturedItems().get(0).getItem();
        final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(dispensedItem);
        final List<ItemStackSnapshot> original = new ArrayList<>();
        original.add(snapshot);
        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(dispenser);
            final DropItemEvent.Pre dropEvent = SpongeEventFactory.createDropItemEventPre(frame.getCurrentCause(), ImmutableList.of(snapshot), original);
            SpongeImpl.postEvent(dropEvent);
            if (dropEvent.isCancelled()) {
                dispenser.setInventorySlotContents(index, (net.minecraft.item.ItemStack) this.originalItem.createStack());
                context.getCapturedItems().clear();
                return;
            }
            if (dropEvent.getDroppedItems().isEmpty()) {
                context.getCapturedItems().clear();
            }

            dispenser.setInventorySlotContents(index, stack);
        }
    }

}
