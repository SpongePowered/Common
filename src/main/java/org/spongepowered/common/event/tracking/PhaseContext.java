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
package org.spongepowered.common.event.tracking;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.event.tracking.context.BlockItemDropsSupplier;
import org.spongepowered.common.event.tracking.context.BlockItemEntityDropsSupplier;
import org.spongepowered.common.event.tracking.context.CaptureBlockPos;
import org.spongepowered.common.event.tracking.context.CapturedBlockEntitySpawnSupplier;
import org.spongepowered.common.event.tracking.context.CapturedBlocksSupplier;
import org.spongepowered.common.event.tracking.context.CapturedEntitiesSupplier;
import org.spongepowered.common.event.tracking.context.CapturedItemStackSupplier;
import org.spongepowered.common.event.tracking.context.CapturedItemsSupplier;
import org.spongepowered.common.event.tracking.context.CapturedMultiMapSupplier;
import org.spongepowered.common.event.tracking.context.CapturedSupplier;
import org.spongepowered.common.event.tracking.context.EntityItemDropsSupplier;
import org.spongepowered.common.event.tracking.context.EntityItemEntityDropsSupplier;
import org.spongepowered.common.event.tracking.context.GeneralizedContext;
import org.spongepowered.common.event.tracking.context.ICaptureSupplier;
import org.spongepowered.common.event.tracking.context.ItemDropData;
import org.spongepowered.common.event.tracking.phase.general.GeneralPhase;
import org.spongepowered.common.interfaces.entity.player.IMixinInventoryPlayer;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * Similar to {@link Cause} except it can be built continuously and retains no
 * real side effects. Strictly speaking this object exists to avoid confusion
 * between what is suggested to be a {@link Cause} for an {@link Event} versus
 * the context of which a {@link IPhaseState} is being completed with.
 */
@SuppressWarnings("unchecked")
public class PhaseContext<P extends PhaseContext<P>> implements AutoCloseable {

    @Nullable private static PhaseContext<?> EMPTY;
    /**
     * Default flagged empty PhaseContext that can be used for stubbing in corner cases.
     * @return
     */
    public static PhaseContext<?> empty() {
        if (EMPTY == null) {
            EMPTY = new GeneralizedContext(GeneralPhase.State.COMPLETE).markEmpty();
        }
        return EMPTY;
    }



    protected final IPhaseState<? extends P> state; // Only temporary to verify the state creation with constructors
    protected boolean isCompleted = false;
    // Only used in hard debugging instances.
    @Nullable private StackTraceElement[] stackTrace;

    // Single type bulk captures
    @Nullable private CapturedBlocksSupplier blocksSupplier;
    @Nullable private CapturedItemsSupplier capturedItemsSupplier;
    @Nullable private CapturedEntitiesSupplier capturedEntitiesSupplier;
    @Nullable private CapturedItemStackSupplier capturedItemStackSupplier;

    // Per block captures (useful for things like explosions to capture multiple targets at a time)
    @Nullable private CapturedMultiMapSupplier<BlockPos, net.minecraft.entity.Entity> blockEntitySpawnSupplier;
    @Nullable private BlockItemDropsSupplier blockItemDropsSupplier;
    @Nullable private BlockItemEntityDropsSupplier blockItemEntityDropsSupplier;
    @Nullable private CaptureBlockPos captureBlockPos;

    // Per entity captures (useful for things like explosions to capture multiple targets at a time)
    @Nullable private EntityItemDropsSupplier entityItemDropsSupplier;
    @Nullable private EntityItemEntityDropsSupplier entityItemEntityDropsSupplier;

    // General
    @Nullable protected User owner;
    @Nullable protected User notifier;
    private boolean processImmediately;
    private boolean allowsBlockEvents = true; // Defaults to allow block events
    private boolean allowsEntityEvents = true;
    private boolean allowsBulkBlockCaptures = true; // Defaults to allow block captures
    private boolean allowsBulkEntityCaptures = true;
    @Nullable Deque<CauseStackManager.StackFrame> usedFrame;

    @Nullable private Object source;

    public P source(Object owner) {
        checkState(!this.isCompleted, "Cannot add a new object to the context if it's already marked as completed!");
        this.source = owner;
        return (P) this;
    }

    public P owner(Supplier<Optional<User>> supplier) {
        supplier.get().ifPresent(this::owner);
        return (P) this;
    }

    public P owner(User owner) {
        checkState(!this.isCompleted, "Cannot add a new object to the context if it's already marked as completed!");
        if (this.owner != null) {
            throw new IllegalStateException("Owner for this phase context is already set!");
        }
        this.owner = checkNotNull(owner, "Owner cannot be null!");
        return (P) this;
    }

    public P notifier(Supplier<Optional<User>> supplier) {
        supplier.get().ifPresent(this::notifier);
        return (P) this;
    }

    public P notifier(User notifier) {
        checkState(!this.isCompleted, "Cannot add a new object to the context if it's already marked as completed!");
        if (this.notifier != null) {
            throw new IllegalStateException("Notifier for this phase context is already set!");
        }
        this.notifier = checkNotNull(notifier, "Notifier cannot be null!");
        return (P) this;
    }

    private void checkBlockSuppliers() {
        checkState(this.blocksSupplier == null, "BlocksSuppler is already set!");
        checkState(this.blockItemEntityDropsSupplier == null, "BlockItemEntityDropsSupplier is already set!");
        checkState(this.blockItemDropsSupplier == null, "BlockItemDropsSupplier is already set!");
        checkState(this.blockEntitySpawnSupplier == null, "BlockEntitySpawnSupplier is already set!");
        checkState(this.captureBlockPos == null, "CaptureBlockPos is already set!");
    }

    public P addBlockCaptures() {
        checkState(!this.isCompleted, "Cannot add a new object to the context if it's already marked as completed!");
        this.checkBlockSuppliers();

        this.blocksSupplier = new CapturedBlocksSupplier();
        this.blockItemEntityDropsSupplier = new BlockItemEntityDropsSupplier();
        this.blockItemDropsSupplier = new BlockItemDropsSupplier();
        this.blockEntitySpawnSupplier = new CapturedBlockEntitySpawnSupplier();
        this.captureBlockPos = new CaptureBlockPos();
        return (P) this;
    }

    public P addCaptures() {
        checkState(!this.isCompleted, "Cannot add a new object to the context if it's already marked as completed!");
        this.checkBlockSuppliers();
        checkState(this.capturedItemsSupplier == null, "CapturedItemsSupplier is already set!");
        checkState(this.capturedEntitiesSupplier == null, "CapturedEntitiesSupplier is already set!");
        checkState(this.capturedItemStackSupplier == null, "CapturedItemStackSupplier is already set!");

        this.blocksSupplier = new CapturedBlocksSupplier();
        this.blockItemEntityDropsSupplier = new BlockItemEntityDropsSupplier();
        this.blockItemDropsSupplier = new BlockItemDropsSupplier();
        this.capturedItemsSupplier = new CapturedItemsSupplier();
        this.capturedEntitiesSupplier = new CapturedEntitiesSupplier();
        this.capturedItemStackSupplier = new CapturedItemStackSupplier();

        this.blockEntitySpawnSupplier = new CapturedBlockEntitySpawnSupplier();
        this.captureBlockPos = new CaptureBlockPos();
        return (P) this;
    }

    public P addEntityCaptures() {
        checkState(!this.isCompleted, "Cannot add a new object to the context if it's already marked as completed!");
        checkState(this.capturedItemsSupplier == null, "CapturedItemsSupplier is already set!");
        checkState(this.capturedEntitiesSupplier == null, "CapturedEntitiesSupplier is already set!");
        checkState(this.capturedItemStackSupplier == null, "CapturedItemStackSupplier is already set!");

        this.capturedItemsSupplier = new CapturedItemsSupplier();
        this.capturedEntitiesSupplier = new CapturedEntitiesSupplier();
        this.capturedItemStackSupplier = new CapturedItemStackSupplier();
        return (P) this;
    }

    public P addEntityDropCaptures() {
        checkState(!this.isCompleted, "Cannot add a new object to the context if it's already marked as completed!");
        checkState(this.entityItemDropsSupplier == null, "EntityItemDropsSupplier is already set!");
        checkState(this.entityItemEntityDropsSupplier == null, "EntityItemEntityDropsSupplier is already set!");

        this.entityItemDropsSupplier = new EntityItemDropsSupplier();
        this.entityItemEntityDropsSupplier = new EntityItemEntityDropsSupplier();
        return (P) this;
    }

    public P setBulkBlockCaptures(boolean captures) {
        this.allowsBulkBlockCaptures = captures;
        return (P) this;
    }

    public boolean allowsBulkBlockCaptures() {
        return this.allowsBulkBlockCaptures;
    }

    public P setBlockEvents(boolean events) {
        this.allowsBlockEvents = events;
        return (P) this;
    }

    public boolean allowsBlockEvents() {
        return this.allowsBlockEvents;
    }

    protected P setEntitySpawnEvents(boolean b) {
        this.allowsEntityEvents = b;
        return (P) this;
    }

    public boolean allowsEntityEvents() {
        return this.allowsEntityEvents;
    }

    protected P setBulkEntityCaptures(boolean b) {
        this.allowsBulkEntityCaptures = b;
        return (P) this;
    }

    public boolean allowsBulkEntityCaptures() {
        return this.allowsBulkEntityCaptures;
    }

    public P buildAndSwitch() {
        this.isCompleted = true;
        if (SpongeImpl.getGlobalConfig().getConfig().getPhaseTracker().generateStackTracePerStateEntry()) {
            this.stackTrace = new Exception("Debug Trace").getStackTrace();
        }
        PhaseTracker.getInstance().switchToPhase(this.state, this);
        return (P) this;
    }

    public boolean isComplete() {
        return this.isCompleted;
    }

    public PrettyPrinter printCustom(PrettyPrinter printer, int indent) {
        String s = String.format("%1$"+indent+"s", "");
        if (this.owner != null) {
            printer.add(s + "- %s: %s", "Owner", this.owner);
        }
        if (this.source != null) {
            printer.add(s + "- %s: %s", "Source", this.source);
        }
        if (this.blocksSupplier != null && !this.blocksSupplier.isEmpty()) {
            printer.add(s + "- %s: %s", "CapturedBlocks", this.blocksSupplier);
        }
        if (this.blockItemDropsSupplier != null && !this.blockItemDropsSupplier.isEmpty()) {
            printer.add(s + "- %s: %s", "BlockItemDrops", this.blockItemDropsSupplier);
        }
        if (this.blockItemEntityDropsSupplier != null && !this.blockItemEntityDropsSupplier.isEmpty()) {
            printer.add(s + "- %s: %s", "BlockItemEntityDrops", this.blockItemEntityDropsSupplier);
        }
        if (this.capturedItemsSupplier != null && !this.capturedItemsSupplier.isEmpty()) {
            printer.add(s + "- %s: %s", "CapturedItems", this.capturedItemsSupplier);
        }
        if (this.capturedEntitiesSupplier != null && !this.capturedEntitiesSupplier.isEmpty()) {
            printer.add(s + "- %s: %s", "CapturedEntities", this.capturedEntitiesSupplier);
        }
        if (this.capturedItemStackSupplier != null && !this.capturedItemStackSupplier.isEmpty()) {
            printer.add(s + "- %s: %s", "CapturedItemStack", this.capturedItemStackSupplier);
        }
        if (this.entityItemDropsSupplier != null && !this.entityItemDropsSupplier.isEmpty()) {
            printer.add(s + "- %s: %s", "EntityItemDrops", this.entityItemDropsSupplier);
        }
        if (this.entityItemEntityDropsSupplier != null && !this.entityItemEntityDropsSupplier.isEmpty()) {
            printer.add(s + "- %s: %s", "EntityItemEntityDrops", this.entityItemEntityDropsSupplier);
        }
        if (this.blockEntitySpawnSupplier != null && !this.blockEntitySpawnSupplier.isEmpty()) {
            printer.add(s + "- %s: %s", "BlockEntitySpawns", this.blockEntitySpawnSupplier);
        }
        if (this.captureBlockPos != null) {
            printer.add(s + "- %s: %s", "CapturedBlockPosition", this.captureBlockPos);
        }
        return printer;
    }


    public boolean notAllCapturesProcessed() {
        // we can safely pop the frame here since this is only called when we're checking for processing

        return
                isNonEmpty(this.blocksSupplier)
                || isNonEmpty(this.blockItemDropsSupplier)
                || isNonEmpty(this.blockItemEntityDropsSupplier)
                || isNonEmpty(this.capturedItemsSupplier)
                || isNonEmpty(this.capturedEntitiesSupplier)
                || isNonEmpty(this.capturedItemStackSupplier)
                || isNonEmpty(this.entityItemDropsSupplier)
                || isNonEmpty(this.entityItemEntityDropsSupplier)
                || isNonEmpty(this.blockEntitySpawnSupplier);
    }

    private boolean isNonEmpty(@Nullable ICaptureSupplier supplier) {
        return supplier != null && !supplier.isEmpty();
    }


    public boolean shouldProcessImmediately() {
        return this.processImmediately;
    }

    public void setProcessImmediately(boolean state) {
        this.processImmediately = state;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getSource(Class<T> sourceClass) {
        if (this.source == null) {
            return Optional.empty();
        }
        if (sourceClass.isInstance(this.source)) {
            return Optional.of((T) this.source);
        }
        return Optional.empty();
    }

    @Nullable
    public Object getSource() {
        return this.source;
    }

    public <T> T requireSource(Class<T> targetClass) {
        return getSource(targetClass)
                .orElseThrow(TrackingUtil.throwWithContext("Expected to be ticking over at a location!", this));
    }

    public Optional<User> getOwner() {
        return Optional.ofNullable(this.owner);
    }

    public Optional<User> getNotifier() {
        return Optional.ofNullable(this.notifier);
    }

    public List<Entity> getCapturedEntities() throws IllegalStateException {
        if (this.capturedEntitiesSupplier == null) {
            throw TrackingUtil.throwWithContext("Intended to capture entity spawns!", this).get();
        }
        return this.capturedEntitiesSupplier.get();
    }

    public CapturedSupplier<Entity> getCapturedEntitySupplier() throws IllegalStateException {
        if (this.capturedEntitiesSupplier == null) {
            throw TrackingUtil.throwWithContext("Intended to capture entity spawns!", this).get();
        }
        return this.capturedEntitiesSupplier;
    }

    public List<EntityItem> getCapturedItems() throws IllegalStateException {
        if (this.capturedItemsSupplier == null) {
            throw TrackingUtil.throwWithContext("Intended to capture dropped item entities!", this).get();
        }
        return this.capturedItemsSupplier.get();
    }

    public CapturedSupplier<EntityItem> getCapturedItemsSupplier() throws IllegalStateException {
        if (this.capturedItemsSupplier == null) {
            throw TrackingUtil.throwWithContext("Intended to capture dropped item entities!", this).get();
        }
        return this.capturedItemsSupplier;
    }

    public List<BlockSnapshot> getCapturedBlocks() throws IllegalStateException {
        if (this.blocksSupplier == null) {
            throw TrackingUtil.throwWithContext("Expected to be capturing blocks, but we're not capturing them!", this).get();
        }
        return this.blocksSupplier.get();
    }

    public CapturedSupplier<BlockSnapshot> getCapturedBlockSupplier() throws IllegalStateException {
        if (this.blocksSupplier == null) {
            throw TrackingUtil.throwWithContext("Expected to be capturing blocks, but we're not capturing them!", this).get();
        }
        return this.blocksSupplier;
    }

    public CapturedMultiMapSupplier<BlockPos, ItemDropData> getBlockDropSupplier() throws IllegalStateException {
        if (this.blockItemDropsSupplier == null) {
            throw TrackingUtil.throwWithContext("Expected to be capturing block drops!", this).get();
        }
        return this.blockItemDropsSupplier;
    }

    public CapturedMultiMapSupplier<BlockPos, EntityItem> getBlockItemDropSupplier() throws IllegalStateException {
        if (this.blockItemEntityDropsSupplier == null) {
            throw TrackingUtil.throwWithContext("Intended to track block item drops!", this).get();
        }
        return this.blockItemEntityDropsSupplier;
    }

    public CapturedMultiMapSupplier<UUID, ItemDropData> getPerEntityItemDropSupplier() throws IllegalStateException {
        if (this.entityItemDropsSupplier == null) {
            throw TrackingUtil.throwWithContext("Intended to capture entity drops!", this).get();
        }
        return this.entityItemDropsSupplier;
    }

    public CapturedMultiMapSupplier<UUID, EntityItem> getPerEntityItemEntityDropSupplier() throws IllegalStateException {
        if (this.entityItemEntityDropsSupplier == null) {
            throw TrackingUtil.throwWithContext("Intended to capture entity drops!", this).get();
        }
        return this.entityItemEntityDropsSupplier;
    }

    public CapturedSupplier<ItemDropData> getCapturedItemStackSupplier() throws IllegalStateException {
        if (this.capturedItemStackSupplier == null) {
            throw TrackingUtil.throwWithContext("Expected to be capturing ItemStack drops from entities!", this).get();
        }
        return this.capturedItemStackSupplier;
    }

    public CapturedMultiMapSupplier<BlockPos, net.minecraft.entity.Entity> getPerBlockEntitySpawnSuppplier() throws IllegalStateException {
        if (this.blockEntitySpawnSupplier == null) {
            throw TrackingUtil.throwWithContext("Intended to track block entity spawns!", this).get();
        }
        return this.blockEntitySpawnSupplier;
    }

    public CaptureBlockPos getCaptureBlockPos() throws IllegalStateException {
        if (this.captureBlockPos == null) {
            throw TrackingUtil.throwWithContext("Intended to capture a block position!", this).get();
        }
        return this.captureBlockPos;
    }

    public boolean hasCaptures() {
        if (this.blocksSupplier != null && !this.blocksSupplier.isEmpty()) {
            return true;
        }
        if (this.blockEntitySpawnSupplier != null && !this.blockEntitySpawnSupplier.isEmpty()) {
            return true;
        }
        if (this.blockItemDropsSupplier != null && !this.blockItemDropsSupplier.isEmpty()) {
            return true;
        }
        if (this.blockItemEntityDropsSupplier != null && !this.blockItemEntityDropsSupplier.isEmpty()) {
            return true;
        }
        if (this.capturedEntitiesSupplier != null && !this.capturedEntitiesSupplier.isEmpty()) {
            return true;
        }
        if (this.capturedItemsSupplier != null && !this.capturedItemsSupplier.isEmpty()) {
            return true;
        }
        if (this.capturedItemStackSupplier!= null && !this.capturedItemStackSupplier.isEmpty()) {
            return true;
        }
        if (this.entityItemDropsSupplier != null && !this.entityItemDropsSupplier.isEmpty()) {
            return true;
        }
        if (this.entityItemEntityDropsSupplier != null && !this.entityItemEntityDropsSupplier.isEmpty()) {
            return true;
        }
        if (this.source != null && this.source instanceof EntityPlayer) {
            if (((IMixinInventoryPlayer) ((EntityPlayer) this.source).inventory).getCapturedTransactions().size() > 0) {
                return true;
            }
        }

        return false;
    }

    public Optional<BlockPos> getBlockPosition() {
        return getCaptureBlockPos()
                .getPos();
    }

    public void addNotifierAndOwnerToCauseStack(CauseStackManager.StackFrame frame) {
        if (this.owner != null) {
            frame.addContext(EventContextKeys.OWNER, this.owner);
        }
        if (this.notifier != null) {
            frame.addContext(EventContextKeys.NOTIFIER, this.notifier);
        }
    }

    protected PhaseContext(IPhaseState<? extends P> state) {
        this.state = state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.isCompleted);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PhaseContext<?> other = (PhaseContext<?>) obj;
        return Objects.equals(this.isCompleted, other.isCompleted);
    }

    @Override
    public String toString() {
        return com.google.common.base.MoreObjects.toStringHelper(this)
                .add("isCompleted", this.isCompleted)
                .toString();
    }

    protected P markEmpty() {
        this.isCompleted = true;
        return (P) this;
    }

    private boolean isEmpty() {
        if (this == PhaseContext.EMPTY) {
            return true;
        }
        return false;
    }

    @Override
    public void close() { // Should never throw an exception
        if (this.isEmpty()) {
            // We aren't ever supposed to close here...
            PhaseTracker.getInstance()
                .printMessageWithCaughtException("Closing an empty Phasecontext",
                    "We should never be closing an empty phase context (or complete phase) This is likely an error from sponge.",
                    new IllegalStateException("Closing empty phase context"));
            return;
        }
        PhaseTracker.getInstance().completePhase(this.state);
        if (this.usedFrame == null && SpongeImplHooks.isMainThread()) {
            // So, this part is interesting... Since the used frame is null, that means
            // the cause stack manager still has the refernce of this context/phase, we have
            // to "pop off" the list.
            SpongeImpl.getCauseStackManager().popFrameMutator(this);
        }
        if (this.usedFrame != null) {
            this.usedFrame.iterator().forEachRemaining(Sponge.getCauseStackManager()::popCauseFrame);
            this.usedFrame.clear();
            this.usedFrame = null;
        }
    }


    public List<BlockSnapshot> getCapturedBlocksOrEmptyList() {
        return this.blocksSupplier != null ? this.blocksSupplier.orEmptyList() : Collections.emptyList();
    }

    public List<Entity> getCapturedEntitiesOrEmptyList() {
        return this.capturedEntitiesSupplier != null ? this.capturedEntitiesSupplier.orEmptyList() : Collections.emptyList();
    }

    public List<EntityItem> getCapturedItemsOrEmptyList() {
        return this.capturedItemsSupplier != null ? this.capturedItemsSupplier.orEmptyList() : Collections.emptyList();
    }

    public boolean isCapturingBlockItemDrops() {
        return this.blockItemDropsSupplier != null;
    }

    public void printTrace(PrettyPrinter printer) {
        if (SpongeImpl.getGlobalConfig().getConfig().getPhaseTracker().generateStackTracePerStateEntry()) {
            printer.add("Entrypoint:")
                .add(this.stackTrace);
        }
    }

}
