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
package org.spongepowered.common.event.tracking.context.transaction;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.ticks.ScheduledTick;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.event.cause.entity.SpawnType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.crafting.CraftingInventory;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.scheduler.ScheduledUpdate;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.bridge.world.TrackedWorldBridge;
import org.spongepowered.common.bridge.world.inventory.container.TrackedContainerBridge;
import org.spongepowered.common.bridge.world.level.TrackableBlockEventDataBridge;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.UnwindingPhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.block.AddBlockEventTransaction;
import org.spongepowered.common.event.tracking.context.transaction.block.AddTileEntity;
import org.spongepowered.common.event.tracking.context.transaction.block.ChangeBlock;
import org.spongepowered.common.event.tracking.context.transaction.block.NeighborNotification;
import org.spongepowered.common.event.tracking.context.transaction.block.PrepareBlockDropsTransaction;
import org.spongepowered.common.event.tracking.context.transaction.block.RemoveBlockEntity;
import org.spongepowered.common.event.tracking.context.transaction.block.ReplaceBlockEntity;
import org.spongepowered.common.event.tracking.context.transaction.block.ScheduleUpdateTransaction;
import org.spongepowered.common.event.tracking.context.transaction.effect.BlockAddedEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.EntityPerformingDropsEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.InventoryEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.PrepareBlockDrops;
import org.spongepowered.common.event.tracking.context.transaction.inventory.ClickCreativeMenuTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.ClickMenuTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.CloseMenuTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.ContainerSlotTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.CraftingPreviewTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.CraftingTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.DropFromPlayerInventoryTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.ExplicitInventoryOmittedTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.InventoryTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.OpenMenuTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlaceRecipeTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.SelectTradeTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.SetCarriedItemTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.SetPlayerContainerTransaction;
import org.spongepowered.common.event.tracking.context.transaction.inventory.ShiftCraftingResultTransaction;
import org.spongepowered.common.event.tracking.context.transaction.world.EntityPerformingDropsTransaction;
import org.spongepowered.common.event.tracking.context.transaction.world.SpawnEntityTransaction;
import org.spongepowered.common.event.tracking.phase.tick.EntityTickContext;
import org.spongepowered.common.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.world.BlockChange;
import org.spongepowered.common.world.SpongeBlockChangeFlag;
import org.spongepowered.common.world.volume.VolumeStreamUtils;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A non-public interface to serve as a dumping ground for the various
 * {@link GameTransaction game transactions} that are being created and recorded
 * such that they all <i>mostly flow</i> through the same function of calling
 * some {@code create} function and successively a
 * {@link #logTransaction(StatefulTransaction)}.
 *
 * <p>Most transactions are singularly contained, but some transactions are
 * markers for transaction boundaries and absorb certain transactions.
 *
 * @see StatefulTransaction#parentAbsorber()
 */
@SuppressWarnings({"Deprecation", "DeprecatedIsStillUsed"})
interface TransactionSink {

    @Deprecated
    void logTransaction(StatefulTransaction transaction);

    EffectTransactor pushEffect(final ResultingTransactionBySideEffect effect);

    default ChangeBlock logBlockChange(final SpongeBlockSnapshot originalBlockSnapshot, final BlockState newState,
        final BlockChangeFlag flags
    ) {
        final ChangeBlock changeBlock = new ChangeBlock(
            originalBlockSnapshot, newState, (SpongeBlockChangeFlag) flags
        );
        this.logTransaction(changeBlock);
        return changeBlock;
    }

    @SuppressWarnings("ConstantConditions")
    default void logBlockEvent(
        final BlockState state, final TrackedWorldBridge serverWorld, final BlockPos pos,
        final TrackableBlockEventDataBridge blockEvent
    ) {
        final WeakReference<ServerLevel> worldRef = new WeakReference<>((ServerLevel) serverWorld);
        final Supplier<ServerLevel> worldSupplier = () -> Objects.requireNonNull(worldRef.get(), "ServerWorld dereferenced");
        final @Nullable BlockEntity tileEntity = ((ServerLevel) serverWorld).getBlockEntity(pos);
        final SpongeBlockSnapshot original = TrackingUtil.createPooledSnapshot(
            state,
            pos,
            BlockChangeFlags.NONE,
            Constants.World.DEFAULT_BLOCK_CHANGE_LIMIT,
            tileEntity,
            worldSupplier,
            Optional::empty, Optional::empty
        );
        original.blockChange = BlockChange.MODIFY;
        final AddBlockEventTransaction transaction = new AddBlockEventTransaction(original, blockEvent);
        this.logTransaction(transaction);
    }

    @SuppressWarnings("ConstantConditions")
    default EffectTransactor logBlockDrops(
        final Level serverWorld, final BlockPos pos, final BlockState state,
        final @Nullable BlockEntity tileEntity
    ) {
        final WeakReference<ServerLevel> worldRef = new WeakReference<>((ServerLevel) serverWorld);
        final Supplier<ServerLevel> worldSupplier = () -> Objects.requireNonNull(worldRef.get(), "ServerWorld dereferenced");
        final SpongeBlockSnapshot original = TrackingUtil.createPooledSnapshot(
            state,
            pos,
            BlockChangeFlags.NONE,
            Constants.World.DEFAULT_BLOCK_CHANGE_LIMIT,
            tileEntity,
            worldSupplier,
            Optional::empty, Optional::empty
        );
        original.blockChange = BlockChange.MODIFY;
        final PrepareBlockDropsTransaction transaction = new PrepareBlockDropsTransaction(pos, state, original);
        this.logTransaction(transaction);
        return this.pushEffect(new ResultingTransactionBySideEffect(PrepareBlockDrops.getInstance()));
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    default <T> void logScheduledUpdate(final ServerLevel serverWorld, final ScheduledTick<T> data) {
        final WeakReference<ServerLevel> worldRef = new WeakReference<>(serverWorld);
        final WeakReference<ScheduledTick<T>> dataRef = new WeakReference<>(data);
        final Supplier<ServerLevel> worldSupplier = () -> Objects.requireNonNull(worldRef.get(), "ServerWorld dereferenced");
        final Supplier<ScheduledTick<T>> dataSupplier = () -> Objects.requireNonNull(dataRef.get(), "Data dereferenced");
        final ScheduledUpdate<T> spongeData = (ScheduledUpdate<T>) (Object) data;
        final ScheduleUpdateTransaction<T> transaction = new ScheduleUpdateTransaction<>(worldSupplier, dataSupplier, data.pos(), data.type(), spongeData.delay(), spongeData.priority());
        this.logTransaction(transaction);
    }

    default void logNeighborNotification(
        final Supplier<ServerLevel> serverWorldSupplier, final BlockPos immutableFrom, final Block blockIn,
        final BlockPos immutableTarget, final BlockState targetBlockState,
        final @Nullable BlockEntity existingTile
    ) {
        final NeighborNotification notificationTransaction = new NeighborNotification(serverWorldSupplier, targetBlockState, immutableTarget, blockIn, immutableFrom, existingTile);
        this.logTransaction(notificationTransaction);
    }

    @SuppressWarnings("ConstantConditions")
    default void logEntitySpawn(
        final PhaseContext<@NonNull ?> current, final TrackedWorldBridge serverWorld,
        final Entity entityIn
    ) {
        final WeakReference<ServerLevel> worldRef = new WeakReference<>((ServerLevel) serverWorld);
        final Supplier<ServerLevel> worldSupplier = () -> Objects.requireNonNull(worldRef.get(), "ServerWorld dereferenced");
        final Supplier<SpawnType> contextualType = current.getSpawnTypeForTransaction(entityIn);
        final SpawnEntityTransaction transaction = new SpawnEntityTransaction(worldSupplier, entityIn, contextualType);
        this.logTransaction(transaction);
    }


    @SuppressWarnings("ConstantConditions")
    default WrapperTransaction logWrapper() {
        final var transaction = new WrapperTransaction<>();
        this.logTransaction(transaction);
        this.pushEffect(new ResultingTransactionBySideEffect(BlockAddedEffect.getInstance()));
        return transaction;
    }


    default boolean logTileReplacement(
        final BlockPos pos, final @Nullable BlockEntity existing, final @Nullable BlockEntity proposed,
        final Supplier<ServerLevel> worldSupplier
    ) {
        if (proposed == null) {
            return false;
        }
        this.logTransaction(new ReplaceBlockEntity(pos, existing, proposed, worldSupplier));
        return true;
    }

    default boolean logTileAddition(
        final BlockEntity tileEntity,
        final Supplier<ServerLevel> worldSupplier, final LevelChunk chunk
    ) {

        this.logTransaction(this.createTileAdditionTransaction(tileEntity, worldSupplier, chunk));

        return true;
    }
    default AddTileEntity createTileAdditionTransaction(final BlockEntity tileentity,
        final Supplier<ServerLevel> worldSupplier, final LevelChunk chunk
    ) {
        final Supplier<LevelChunk> weaklyReferencedSupplier = VolumeStreamUtils.createWeaklyReferencedSupplier(chunk, "LevelChunk");

        return new AddTileEntity(tileentity, worldSupplier, weaklyReferencedSupplier);
    }

    default boolean logTileRemoval(final @Nullable BlockEntity tileentity, final Supplier<ServerLevel> worldSupplier) {
        if (tileentity == null) {
            return false;
        }
        this.logTransaction(new RemoveBlockEntity(tileentity, worldSupplier));
        return true;
    }

    default @Nullable EffectTransactor ensureEntityDropTransactionEffect(final Entity entity) {
        final EntityPerformingDropsTransaction transaction = new EntityPerformingDropsTransaction(entity);
        this.logTransaction(transaction);
        if (transaction.recorded()) {
            return this.pushEffect(new ResultingTransactionBySideEffect(EntityPerformingDropsEffect.getInstance()));
        }
        return null;
    }


    /**
     * Called with a created {@link SlotTransaction} that's been created and
     * possibly already recorded by {@link TrackedContainerBridge#bridge$detectAndSendChanges(boolean)}
     * performing transaction handling and submitting to be recorded through
     * here. The caveat with this system is that since it's reliant on having
     * the transactions created as a side effect of {@link AbstractContainerMenu#broadcastChanges()},
     * it's possible that certain transactions are "too late" or remain uncaptured
     * until the next tick.
     *
     * @param phaseContext The context
     * @param newTransaction The slot transaction in relation to the menu
     * @param abstractContainerMenu The container menu
     */
    /*
     Non-Javadoc: Known areas where we are keeping transactions recorded:
     - events - during EventListenerPhaseContext
     - commands - during CommandPhaseContext see below
     - place/use ServerPlayerGameModeMixin_Tracker#useItemOn
     - Dispenser equip PlayerEntityMixin_Inventory#setItemSlot
     - eating etc. LivingEntityMixin_Inventory#completeUsingItem
     - throw/use ServerGamePacketListenerImplMixin_Inventory#impl$onHandleUseItem
     - breaking blocks ServerPlayerGameModeMixin_Tracker#impl$onMineBlock
     - exp pickup with mending PlayerEntityMixin_Inventory#inventory$onTouch
     - attack PlayerMixin#attack
     - armor/shield damage LivingEntityMixin#bridge$damageEntity
     - elytra damage LivingEntityMixin#inventory$onElytraUse
     - consume arrow (BowItem#releaseUsing - shrink on stack) LivingEntityMixin#impl$onStopPlayerUsing
     - villager trade select ServerGamePacketListenerImplMixin_Inventory#impl$onHandleSelectTrade
     - close inventory adding back to inventory ServerPlayerEntityMixin_Inventory#impl$onCloseContainer
     - use on entity - ServerGamePacketListenerImplMixin_Inventory#impl$onInteractAt/impl$onInteractOn
     */
    default void logSlotTransaction(
        final PhaseContext<@NonNull ?> phaseContext, final SlotTransaction newTransaction,
        final AbstractContainerMenu abstractContainerMenu
    ) {
        // Inventory change during event or command
        if (abstractContainerMenu instanceof InventoryMenu) {
            if (phaseContext instanceof UnwindingPhaseContext) {
                return;
            }
            if (phaseContext instanceof EntityTickContext) {
                // TODO remove warning when we got all cases covered
                SpongeCommon.logger().warn("Ignoring slot transaction on InventoryMenu during {}. {}\nNo Event will be fired for this", phaseContext.getClass().getSimpleName(), newTransaction);
                return;
            }
        }
        final ContainerSlotTransaction transaction = new ContainerSlotTransaction(abstractContainerMenu, newTransaction);
        this.logTransaction(transaction);
    }

    default void logPlayerCarriedItem(final Player player, final int newSlot) {
        final SetCarriedItemTransaction transaction = new SetCarriedItemTransaction(player, newSlot);
        this.logTransaction(transaction);
    }

    default void logPlayerInventoryChange(final Player player, final PlayerInventoryTransaction.EventCreator eventCreator) {
        final PlayerInventoryTransaction transaction = new PlayerInventoryTransaction(player, eventCreator);
        this.logTransaction(transaction);
    }

    default EffectTransactor logClickContainer(
        final AbstractContainerMenu menu, final int slotNum, final int buttonNum, final ClickType clickType, final Player player
    ) {
        @Nullable Slot slot = null;
        if (buttonNum >= 0) { // Try to get valid slot - might not be present e.g. for drag-events
            slot = ((InventoryAdapter) menu).inventoryAdapter$getSlot(slotNum).orElse(null);
        }
        final ClickMenuTransaction transaction = new ClickMenuTransaction(
            player, menu, slotNum, buttonNum, clickType, slot, ItemStackUtil.snapshotOf(player.containerMenu.getCarried()));
        this.logTransaction(transaction);
        return this.pushEffect(new ResultingTransactionBySideEffect(InventoryEffect.getInstance()));
    }

    default EffectTransactor logPlayerInventoryChangeWithEffect(final Player player, final PlayerInventoryTransaction.EventCreator eventCreator) {
        final PlayerInventoryTransaction transaction = new PlayerInventoryTransaction(player, eventCreator);
        this.logTransaction(transaction);
        return this.pushEffect(new ResultingTransactionBySideEffect(InventoryEffect.getInstance()));
    }

    default EffectTransactor logCreativeClickContainer(final int slotNum, final ItemStackSnapshot creativeStack, final Player player) {
        final ClickCreativeMenuTransaction transaction = new ClickCreativeMenuTransaction(player, slotNum, creativeStack);
        this.logTransaction(transaction);
        return this.pushEffect(new ResultingTransactionBySideEffect(InventoryEffect.getInstance()));
    }


    default EffectTransactor logDropFromPlayerInventory(final ServerPlayer player, final boolean dropAll) {
        final DropFromPlayerInventoryTransaction transaction = new DropFromPlayerInventoryTransaction(player, dropAll);
        this.logTransaction(transaction);
        return this.pushEffect(new ResultingTransactionBySideEffect(InventoryEffect.getInstance()));
    }

    default EffectTransactor logOpenInventory(final Player player) {
        final OpenMenuTransaction transaction = new OpenMenuTransaction(player);
        this.logTransaction(transaction);
        return this.pushEffect(new ResultingTransactionBySideEffect(InventoryEffect.getInstance()));
    }

    default EffectTransactor logCloseInventory(final Player player, final boolean clientSource) {
        final CloseMenuTransaction transaction = new CloseMenuTransaction(player, clientSource);
        this.logTransaction(transaction);
        return this.pushEffect(new ResultingTransactionBySideEffect(InventoryEffect.getInstance()));
    }

    default EffectTransactor logPlaceRecipe(final boolean shift, final RecipeHolder<Recipe<?>> recipe, final ServerPlayer player, final CraftingInventory craftInv) {
        final PlaceRecipeTransaction transaction = new PlaceRecipeTransaction(player, shift, recipe, craftInv);
        this.logTransaction(transaction);
        return this.pushEffect(new ResultingTransactionBySideEffect(InventoryEffect.getInstance()));
    }

    default void logSelectTrade(final ServerPlayer player, final int item) {
        final SelectTradeTransaction transaction = new SelectTradeTransaction(player, item);
        this.logTransaction(transaction);
    }

    default void logShiftCraftingResult(final net.minecraft.world.inventory.Slot slot, final ItemStack result) {
        final ShiftCraftingResultTransaction transaction = new ShiftCraftingResultTransaction(slot, result);
        this.logTransaction(transaction);
    }
    default void logContainerSet(final Player player) {
        final SetPlayerContainerTransaction transaction = new SetPlayerContainerTransaction(player);
        this.logTransaction(transaction);
    }

    default void logCraftingPreview(final ServerPlayer player, final CraftingInventory craftingInventory,
        final CraftingContainer craftSlots) {
        final CraftingPreviewTransaction transaction = new CraftingPreviewTransaction(player, craftingInventory, craftSlots);
        this.logTransaction(transaction);
    }

    default void logCrafting(final Player player, @Nullable final ItemStack craftedStack, final CraftingInventory craftInv,
        @Nullable final RecipeHolder<CraftingRecipe> lastRecipe) {
        final CraftingTransaction transaction = new CraftingTransaction(player, craftedStack, craftInv, lastRecipe);
        this.logTransaction(transaction);
    }

    default EffectTransactor logIgnoredInventory(AbstractContainerMenu containerMenu) {
        final ExplicitInventoryOmittedTransaction transaction = new ExplicitInventoryOmittedTransaction(containerMenu);
        this.logTransaction(transaction);
        return this.pushEffect(new ResultingTransactionBySideEffect(InventoryEffect.getInstance()));
    }

    default EffectTransactor logInventoryTransaction(final AbstractContainerMenu containerMenu) {
        final InventoryTransaction transaction = new InventoryTransaction((Inventory) containerMenu);
        this.logTransaction(transaction);
        return this.pushEffect(new ResultingTransactionBySideEffect(InventoryEffect.getInstance()));
    }
}
