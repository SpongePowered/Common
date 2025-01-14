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


import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.common.applaunch.config.core.SpongeConfigs;
import org.spongepowered.common.event.tracking.context.transaction.TransactionalCaptureSupplier;
import org.spongepowered.common.util.MemoizedSupplier;
import org.spongepowered.common.util.Preconditions;
import org.spongepowered.common.util.PrettyPrinter;

import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Similar to {@link Cause} except it can be built continuously and retains no
 * real side effects. Strictly speaking this object exists to avoid confusion
 * between what is suggested to be a {@link Cause} for an {@link Event} versus
 * the context of which a {@link IPhaseState} is being completed with.
 */
@SuppressWarnings("unchecked")
@DefaultQualifier(NonNull.class)
public class PhaseContext<P extends PhaseContext<P>> implements PhaseStateProxy<P>, AutoCloseable {

    @MonotonicNonNull private static Supplier<PhaseContext<@NonNull ?>> EMPTY = MemoizedSupplier.memoize(() -> new EmptyContext(new PhaseTracker()).markEmpty());
    protected final PhaseTracker createdTracker;
    @MonotonicNonNull private TransactionalCaptureSupplier transactor;

    /**
     * Default flagged empty PhaseContext that can be used for stubbing in corner cases.
     */
    public static PhaseContext<@NonNull ?> empty() {
        return PhaseContext.EMPTY.get();
    }

    protected final IPhaseState<P> state; // Only temporary to verify the state creation with constructors
    protected boolean isCompleted = false;
    // Only used in hard debugging instances.
    private @Nullable StackTraceElement[] stackTrace;

    // General
    @Nullable protected UUID creator;
    @Nullable protected UUID notifier;
    private boolean allowsBlockEvents = true; // Defaults to allow block events
    private boolean allowsEntityEvents = true;
    private boolean allowsBulkBlockCaptures = true; // Defaults to allow block captures
    private boolean allowsBulkEntityCaptures = true;
    @Nullable Deque<CauseStackManager.StackFrame> usedFrame;

    private @Nullable Object source;

    public P source(final Object owner) {
        Preconditions.checkState(!this.isCompleted, "Cannot add a new object to the context if it's already marked as completed!");
        this.source = owner;
        return (P) this;
    }

    public P creator(final Supplier<Optional<UUID>> supplier) {
        supplier.get().ifPresent(this::creator);
        return (P) this;
    }

    public P creator(final UUID owner) {
        this.checkNotCompleted();
        if (this.creator != null) {
            throw new IllegalStateException("Owner for this phase context is already set!");
        }
        this.creator = Objects.requireNonNull(owner, "Owner cannot be null!");
        return (P) this;
    }

    public P notifier(final Supplier<Optional<UUID>> supplier) {
        supplier.get().ifPresent(this::notifier);
        return (P) this;
    }

    public P notifier(final UUID notifier) {
        this.checkNotCompleted();
        if (this.notifier != null) {
            throw new IllegalStateException("Notifier for this phase context is already set!");
        }
        this.notifier = Objects.requireNonNull(notifier, "Notifier cannot be null!");
        return (P) this;
    }

    public P setBulkBlockCaptures(final boolean captures) {
        this.allowsBulkBlockCaptures = captures;
        return (P) this;
    }

    public boolean allowsBulkBlockCaptures() {
        return this.allowsBulkBlockCaptures;
    }

    public P setBlockEvents(final boolean events) {
        this.allowsBlockEvents = events;
        return (P) this;
    }

    public boolean allowsBlockEvents() {
        return this.allowsBlockEvents;
    }

    protected P setEntitySpawnEvents(final boolean b) {
        this.allowsEntityEvents = b;
        return (P) this;
    }

    public boolean allowsEntityEvents() {
        return this.allowsEntityEvents;
    }

    protected P setBulkEntityCaptures(final boolean b) {
        this.allowsBulkEntityCaptures = b;
        return (P) this;
    }

    public boolean allowsBulkEntityCaptures() {
        return this.allowsBulkEntityCaptures;
    }

    public P buildAndSwitch() {
        this.isCompleted = true;
        if (SpongeConfigs.getCommon().get().phaseTracker.generateStackTracePerPhase) {
            this.stackTrace = new Exception("Debug Trace").getStackTrace();
        }
        this.createdTracker.switchToPhase(this.state, this);
        return (P) this;
    }

    public boolean isComplete() {
        return this.isCompleted;
    }

    public PrettyPrinter printCustom(final PrettyPrinter printer, final int indent) {
        final String s = String.format("%1$" + indent + "s", "");
        if (this.stackTrace != null) {
            printer.add(s + "StackTrace On Entry")
                .add(this.stackTrace);
        }
        if (this.creator != null) {
            printer.add(s + "- %s: %s", "Owner", this.creator);
        }
        if (this.source != null) {
            printer.add(s + "- %s: %s", "Source", this.source);
        }
        if (this.transactor != null && !this.transactor.isEmpty()) {
            printer.add(s + "- %s: %s", "CapturedTransactions", this.transactor);
        }
        return printer;
    }


    public boolean notAllCapturesProcessed() {
        // we can safely pop the frame here since this is only called when we're checking for processing
        return false;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getSource(final Class<T> sourceClass) {
        if (this.source == null) {
            return Optional.empty();
        }
        if (sourceClass.isInstance(this.source)) {
            return Optional.of((T) this.source);
        }
        return Optional.empty();
    }

    public @Nullable Object getSource() {
        return this.source;
    }

    public <T> T requireSource(final Class<T> targetClass) {
        return this.getSource(targetClass)
            .orElseThrow(TrackingUtil.throwWithContext("Expected to be ticking over at a location!", this));
    }

    public Optional<UUID> getCreator() {
        return Optional.ofNullable(this.creator);
    }

    /**
     * Applies the owner to the provided {@link Consumer} if available.
     *
     * @param consumer The consumer consuming the owner that isn't null
     * @return True if the consumer was called
     */
    public boolean applyOwnerIfAvailable(final Consumer<? super UUID> consumer) {
        if (this.creator != null) {
            consumer.accept(this.creator);
            return true;
        }
        return false;
    }

    public Optional<UUID> getNotifier() {
        return Optional.ofNullable(this.notifier);
    }

    /**
     * Applies the notifier to the provided {@link Consumer} if available.
     *
     * @param consumer The consumer consuming the notifier that isn't null
     * @return True if the consumer was called
     */
    public boolean applyNotifierIfAvailable(final Consumer<? super UUID> consumer) {
        if (this.notifier != null) {
            consumer.accept(this.notifier);
            return true;
        }
        return false;
    }

    public TransactionalCaptureSupplier getTransactor() {
        if (this.transactor == null) {
            this.transactor = new TransactionalCaptureSupplier(this);
        }
        return this.transactor;
    }

    public boolean hasCaptures() {
        if (this.transactor != null && !this.transactor.isEmpty()) {
            return true;
        }

        return false;
    }

    public void addCreatorAndNotifierToCauseStack(final CauseStackManager.StackFrame frame) {
        if (this.creator != null) {
            frame.addContext(EventContextKeys.CREATOR, this.creator);
        }
        if (this.notifier != null) {
            frame.addContext(EventContextKeys.NOTIFIER, this.notifier);
        }
    }

    protected PhaseContext(final IPhaseState<P> state, final PhaseTracker tracker) {
        this.state = state;
        this.createdTracker = Objects.requireNonNull(tracker, "Null PhaseTracker!");
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.isCompleted);
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final PhaseContext<?> other = (PhaseContext<?>) obj;
        return Objects.equals(this.isCompleted, other.isCompleted);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PhaseContext.class.getSimpleName() + "[", "]")
                .add("isCompleted=" + this.isCompleted)
                .toString();
    }

    protected P markEmpty() {
        this.isCompleted = true;
        return (P) this;
    }

    public boolean isEmpty() {
        return this == PhaseContext.EMPTY.get();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void close() { // Should never throw an exception
        if (this.isEmpty()) {
            // We aren't ever supposed to close here...
            PhasePrinter
                .printMessageWithCaughtException(this.createdTracker.stack, "Closing an empty Phasecontext",
                    "We should never be closing an empty phase context (or complete phase) This is likely an error from sponge.",
                    this.state, this, new IllegalStateException("Closing empty phase context"));
            return;
        }
        this.createdTracker.completePhase(this);
        if (this.usedFrame != null) {
            this.usedFrame.iterator().forEachRemaining(this.createdTracker::popCauseFrame);
            this.usedFrame = null;
        } else if (this.shouldProvideModifiers()) {
            // So, this part is interesting... Since the used frame is null, that means
            // the cause stack manager still has the reference of this context/phase, we have
            // to "pop off" the list.
            this.createdTracker.popFrameMutator(this);
        }
        this.reset();
        this.isCompleted = false;
        if (this.state instanceof PooledPhaseState) {
            ((PooledPhaseState) this.state).releaseContextFromPool(this);
        }
    }

    protected void reset() {
        this.source = null;
        this.stackTrace = null;
        this.creator = null;
        this.notifier = null;
        if (this.transactor != null) {
            this.transactor.reset();
        }
    }

    public void printTrace(final PrettyPrinter printer) {
        if (SpongeConfigs.getCommon().get().phaseTracker.generateStackTracePerPhase) {
            printer.add("Entrypoint:")
                .add(this.stackTrace);
        }
    }

    public @Nullable UUID getActiveUserUUID() {
        if (this.notifier != null) {
            return this.notifier;
        }
        if (this.creator != null) {
            return this.creator;
        }
        if (this.source != null && this.source instanceof ServerPlayer) {
            return ((ServerPlayer) this.source).uniqueId();
        }
        return null;
    }

    public boolean isClientSide() {
        return false;
    }

    public Optional<ServerLocation> containerLocation() {
        return Optional.empty();
    }

    protected boolean isRunaway(final PhaseContext<?> phaseContext) {
        return phaseContext.getClass() == this.getClass();
    }

    protected P defensiveCopy(final PhaseTracker tracker) {
        final P newCopy = this.state instanceof PooledPhaseState ? ((PooledPhaseState<P>) this.state).createNewContext(tracker) :  this.state.createPhaseContext(tracker);
        newCopy.source(this.source);
        // tODO - blah

        return newCopy;
    }

    private void checkNotCompleted() {
        if (this.isCompleted) {
            throw new IllegalStateException("Cannot add a new object to the context if it's already marked as completed!");
        }
    }

    @Override
    public IPhaseState<P> getState() {
        return this.state;
    }

    @Override
    public P asContext() {
        return (P) this;
    }
}
