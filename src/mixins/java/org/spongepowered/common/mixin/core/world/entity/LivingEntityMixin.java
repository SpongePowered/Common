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
package org.spongepowered.common.mixin.core.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.action.SleepingEvent;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.item.inventory.UseItemStackEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.data.VanishableBridge;
import org.spongepowered.common.bridge.world.entity.LivingEntityBridge;
import org.spongepowered.common.bridge.world.entity.player.PlayerBridge;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.entity.living.human.HumanEntity;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.SpongeTicks;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3d;

import java.util.Optional;

@SuppressWarnings("ConstantConditions")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends EntityMixin implements LivingEntityBridge {

    // @formatter:off
    @Shadow protected int useItemRemaining;
    @Shadow protected boolean dead;
    @Shadow protected ItemStack useItem;
    @Shadow @Nullable private DamageSource lastDamageSource;
    @Shadow private long lastDamageStamp;

    @Shadow public abstract AttributeInstance shadow$getAttribute(Holder<Attribute> attribute);
    @Shadow public abstract void shadow$setItemInHand(InteractionHand hand, @Nullable ItemStack stack);
    @Shadow public abstract void shadow$stopUsingItem();
    @Shadow public abstract int shadow$getUseItemRemainingTicks();
    @Shadow public abstract float shadow$getHealth();
    @Shadow public abstract CombatTracker shadow$getCombatTracker();
    @Shadow public void shadow$kill(ServerLevel level) { }
    @Shadow public abstract InteractionHand shadow$getUsedItemHand();
    @Shadow public abstract Optional<BlockPos> shadow$getSleepingPos();
    @Shadow protected abstract void shadow$spawnItemParticles(ItemStack stack, int count);
    @Shadow public abstract ItemStack shadow$getItemInHand(InteractionHand hand);
    @Shadow protected abstract void shadow$dropEquipment(ServerLevel level);
    @Shadow protected abstract void shadow$dropAllDeathLoot(ServerLevel level, DamageSource damageSourceIn);
    @Shadow @Nullable public abstract LivingEntity shadow$getKillCredit();
    @Shadow protected abstract void shadow$createWitherRose(@Nullable LivingEntity p_226298_1_);
    @Shadow public abstract float shadow$getMaxHealth();
    @Shadow public abstract AttributeMap shadow$getAttributes();
    @Shadow public abstract void shadow$clearSleepingPos();
    @Shadow public abstract void shadow$setHealth(final float $$0);

    // @formatter:on

    @Nullable private ItemStack impl$activeItemStackCopy;
    @Nullable private Vector3d impl$preTeleportPosition;
    private int impl$deathEventsPosted;


    /**
     * Due to cancelling death events, "healing" the entity is the only way to cancel the death, but we still
     * want to reset the death event counter. This is the simplest way to get it working with forge mods who
     * do not have access to Sponge's API.
     */
    @Inject(method = "setHealth",
        at = @At("HEAD"))
    private void impl$resetDeathEventCounter(final float health, final CallbackInfo info) {
        if (this.shadow$getHealth() <= 0 && health > 0) {
            this.impl$deathEventsPosted = 0;
        }
    }

    @Redirect(method = "dropExperience",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getExperienceReward(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)I"))
    protected int impl$exposeGetExperienceForDeath(final LivingEntity instance, final ServerLevel $$0, final Entity $$1) {
        return this.bridge$getExperiencePointsOnDeath(instance, $$0, $$1);
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void impl$throwDestructEntityDeath(final DamageSource cause, final CallbackInfo ci) {
        final boolean throwEvent = !((LevelBridge) this.shadow$level()).bridge$isFake() && Sponge.isServerAvailable() && PhaseTracker.getWorldInstance().onSidedThread();
        if (!this.dead) { // isDead should be set later on in this method so we aren't re-throwing the events.
            if (throwEvent && this.impl$deathEventsPosted <= Constants.Sponge.MAX_DEATH_EVENTS_BEFORE_GIVING_UP) {
                // ignore because some moron is not resetting the entity.
                this.impl$deathEventsPosted++;
                if (SpongeCommonEventFactory.callDestructEntityEventDeath((LivingEntity) (Object) this, cause).isCancelled()) {
                    ci.cancel();
                    this.shadow$setHealth(this.shadow$getMaxHealth());
                }
            }
        } else {
            this.impl$deathEventsPosted = 0;
        }
    }

    @Inject(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;broadcastEntityEvent(Lnet/minecraft/world/entity/Entity;B)V"),
            cancellable = true)
    private void impl$doNotSendStateForHumans(final DamageSource cause, final CallbackInfo ci) {
        if (((LivingEntity) (Object) this) instanceof HumanEntity) {
            ci.cancel();
        }
    }

    @Redirect(method = "dropAllDeathLoot",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;dropEquipment(Lnet/minecraft/server/level/ServerLevel;)V"
            )
    )
    private void tracker$dropInventory(final LivingEntity thisEntity, final ServerLevel level) {
        if (thisEntity instanceof PlayerBridge && ((PlayerBridge) thisEntity).bridge$keepInventory()) {
            return;
        }
        this.shadow$dropEquipment(level);
    }

    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void impl$pushEntitiesIfNotVanished(final CallbackInfo ci) {
        if (this.bridge$vanishState().ignoresCollisions()) {
            ci.cancel();
        }
    }



    @Inject(method = "randomTeleport", at = @At("HEAD"))
    private void impl$snapshotPositionBeforeVanillaTeleportLogic(final double x, final double y, final double z, final boolean changeState,
                                                                 final CallbackInfoReturnable<Boolean> cir) {
        this.impl$preTeleportPosition = new Vector3d(this.shadow$getX(), this.shadow$getY(), this.shadow$getZ());
    }

    @Inject(method = "randomTeleport", at = @At(value = "RETURN", ordinal = 0, shift = At.Shift.BY, by = 2), cancellable = true)
    private void impl$callMoveEntityEventForTeleport(final double x, final double y, final double z, final boolean changeState,
                                                     final CallbackInfoReturnable<Boolean> cir) {
        if (!ShouldFire.MOVE_ENTITY_EVENT) {
            return;
        }

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(this);

            // ENTITY_TELEPORT is our fallback context
            if (!frame.currentContext().containsKey(EventContextKeys.MOVEMENT_TYPE)) {
                frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.ENTITY_TELEPORT);
            }

            final MoveEntityEvent event = SpongeEventFactory.createMoveEntityEvent(frame.currentCause(),
                    (org.spongepowered.api.entity.Entity) this, this.impl$preTeleportPosition, new Vector3d(this.shadow$getX(), this.shadow$getY(),
                            this.shadow$getZ()),
                    new Vector3d(x, y, z));

            if (SpongeCommon.post(event)) {
                this.shadow$teleportTo(this.impl$preTeleportPosition.x(), this.impl$preTeleportPosition.y(),
                        this.impl$preTeleportPosition.z());
                cir.setReturnValue(false);
                return;
            }

            this.shadow$teleportTo(event.destinationPosition().x(), event.destinationPosition().y(),
                    event.destinationPosition().z());
        }
    }

    /**
     * @author gabizou - January 4th, 2016
     * @reason This allows invisiblity to ignore entity collisions.
     */
    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void impl$ifVanishedDoNotPick(final CallbackInfoReturnable<Boolean> cir) {
        if (this.bridge$vanishState().ignoresCollisions()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "checkFallDamage",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
    private <T extends ParticleOptions> int impl$vanishSpawnParticleForFallState(
            final ServerLevel serverLevel, final T options, final double xCoord, final double yCoord, final double zCoord, final int numberOfParticles,
            final double xOffset,
            final double yOffset,
            final double zOffset,
            final double particleSpeed
    ) {
        if (this.bridge$vanishState().createsParticles()) {
            return serverLevel.sendParticles(options, xCoord, yCoord, zCoord, numberOfParticles, xOffset, yOffset, zOffset, particleSpeed);
        }
        return 0;
    }

    @Inject(method = "onEquippedItemBroken", at = @At("HEAD"), cancellable = true)
    private void impl$vanishDoesNotBroadcastBreakEvents(final Item $$0, final EquipmentSlot $$1, final CallbackInfo ci) {
        if (this.bridge$vanishState().invisible()) {
            ci.cancel();
        }
    }

    @Inject(method = "updatingUsingItem",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;stopUsingItem()V"))
    protected void impl$updateHealthForUseFinish(final CallbackInfo ci) {
    }

    // Data delegated methods

    // Start implementation of UseItemstackEvent

    @Inject(method = "startUsingItem",
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/LivingEntity;useItem:Lnet/minecraft/world/item/ItemStack;"))
    private void impl$onSetActiveItemStack(final InteractionHand hand, final CallbackInfo ci, final ItemStack stack) {
        if (this.shadow$level().isClientSide) {
            return;
        }

        final UseItemStackEvent.Start event;
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(stack);
            final HandType handType = (HandType) (Object) hand;
            this.impl$addSelfToFrame(frame, snapshot, handType);
            final Ticks useDuration = SpongeTicks.ticksOrInfinite(stack.getUseDuration((LivingEntity) (Object) this));
            event = SpongeEventFactory.createUseItemStackEventStart(PhaseTracker.getCauseStackManager().currentCause(),
                useDuration, useDuration, snapshot);
        }

        if (SpongeCommon.post(event)) {
            ci.cancel();
        } else {
            this.useItemRemaining = SpongeTicks.toSaturatedIntOrInfinite(event.remainingDuration());
        }
    }

    @Redirect(method = "startUsingItem",
        at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/LivingEntity;useItemRemaining:I"))
    private void impl$getItemDuration(final LivingEntity this$0, final int count) {
        if (this.shadow$level().isClientSide) {
            this.useItemRemaining = count;
        }
        // If we're on the server, do nothing, since we already set this field on onSetActiveItemStack
    }

    // A helper method for firing UseItemStackEvent sub-events
    // This ensures that the cause and context for these events
    // always have OWNER and NOTIFIER set (if possible),
    // as well as USED_ITEM and USED_HAND
    private void impl$addSelfToFrame(final CauseStackManager.StackFrame frame, final ItemStackSnapshot snapshot, final HandType hand) {
        frame.addContext(EventContextKeys.USED_HAND, hand);
        this.impl$addSelfToFrame(frame, snapshot);
    }

    private void impl$addSelfToFrame(final CauseStackManager.StackFrame frame, final ItemStackSnapshot snapshot) {
        frame.pushCause(this);
        frame.addContext(EventContextKeys.USED_ITEM, snapshot);
        if ((Object) this instanceof ServerPlayer spongePlayer)  {
            frame.addContext(EventContextKeys.CREATOR, spongePlayer.getUUID());
            frame.addContext(EventContextKeys.NOTIFIER, spongePlayer.getUUID());
        }
    }

    @Redirect(method = "updateUsingItem",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getUseItemRemainingTicks()I",
            ordinal = 0))
    private int impl$onGetRemainingItemDuration(final LivingEntity self) {
        if (this.shadow$level().isClientSide) {
            return self.getUseItemRemainingTicks();
        }

        final UseItemStackEvent.Tick event;
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(this.useItem);
            final HandType handType = (HandType) (Object) this.shadow$getUsedItemHand();
            this.impl$addSelfToFrame(frame, snapshot, handType);
            final Ticks useItemRemainingTicks = SpongeTicks.ticksOrInfinite(this.useItemRemaining);
            event = SpongeEventFactory.createUseItemStackEventTick(PhaseTracker.getCauseStackManager().currentCause(),
                useItemRemainingTicks, useItemRemainingTicks, snapshot);
            SpongeCommon.post(event);
        }
        // Because the item usage will only finish if useItemRemaining == 0 and decrements it first, it should be >= 1
        this.useItemRemaining = event.remainingDuration().isInfinite()
                ? Constants.TickConversions.INFINITE_TICKS
                : Math.max(SpongeTicks.toSaturatedIntOrInfinite(event.remainingDuration()), 1);

        if (event.isCancelled()) {
            // Get prepared for some cool hacks: We're within the condition for updateItemUse
            // So if we don't want it to call the method we just pass a value that makes the
            // condition evaluate to false, so an integer >= 25
            return 26;
        }

        return this.shadow$getUseItemRemainingTicks();
    }

    @Inject(method = "updateUsingItem",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;useItemRemaining:I", opcode = Opcodes.PUTFIELD), cancellable = true)
    private void impl$dontReduceInfiniteRemainingItemDuration(final CallbackInfo ci) {
        if (this.useItemRemaining == Constants.TickConversions.INFINITE_TICKS) {
            ci.cancel();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "completeUsingItem",
        cancellable = true,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private void impl$onUpdateItemUse(final CallbackInfo ci) {
        if (this.shadow$level().isClientSide) {
            return;
        }


        final UseItemStackEvent.Finish event;
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(this.useItem);
            final HandType handType = (HandType) (Object) this.shadow$getUsedItemHand();
            this.impl$addSelfToFrame(frame, snapshot, handType);
            final Ticks useItemRemainingTicks = SpongeTicks.ticksOrInfinite(this.useItemRemaining);
            event = SpongeEventFactory.createUseItemStackEventFinish(PhaseTracker.getCauseStackManager().currentCause(),
                    useItemRemainingTicks, useItemRemainingTicks, snapshot);
        }
        SpongeCommon.post(event);
        if (event.remainingDuration().isInfinite() || event.remainingDuration().ticks() > 0) {
            this.useItemRemaining = SpongeTicks.toSaturatedIntOrInfinite(event.remainingDuration());
            ci.cancel();
        } else if (event.isCancelled()) {
            this.shadow$stopUsingItem();
            ci.cancel();
        } else {
            this.impl$activeItemStackCopy = this.useItem.copy();
        }
    }

    @Redirect(method = "completeUsingItem",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"))
    private void impl$onSetHeldItem(final LivingEntity self, final InteractionHand hand, final ItemStack stack) {
        if (this.shadow$level().isClientSide) {
            self.setItemInHand(hand, stack);
            return;
        }

        // Unforunately, ItemFood calls ItemStack#shrink in Item#onItemUseFinish.
        // To ensure that we provide the original ItemStack in the event,
        // we make a copy of in our onUpdateItemUse redirect
        // If the event or transaction is cancelled, we make sure to explicitly
        // set the copy back in the player's hand, since it may have been already
        // modified if an ItemFood is being used.

        final ItemStackSnapshot activeItemStackSnapshot = ItemStackUtil.snapshotOf(this.impl$activeItemStackCopy == null ? ItemStack.EMPTY : this.impl$activeItemStackCopy);


        final UseItemStackEvent.Replace event;
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(stack == null ? ItemStack.EMPTY : stack);
            final HandType handType = (HandType) (Object) hand;
            this.impl$addSelfToFrame(frame, activeItemStackSnapshot, handType);
            final Ticks useItemRemainingTicks = SpongeTicks.ticksOrInfinite(this.useItemRemaining);
            event = SpongeEventFactory.createUseItemStackEventReplace(PhaseTracker.getCauseStackManager().currentCause(),
                    useItemRemainingTicks, useItemRemainingTicks, activeItemStackSnapshot,
                new Transaction<>(ItemStackUtil.snapshotOf(this.impl$activeItemStackCopy), snapshot));
        }

        if (SpongeCommon.post(event)) {
            this.shadow$setItemInHand(hand, this.impl$activeItemStackCopy.copy());
            return;
        }

        if (!event.itemStackResult().isValid()) {
            this.shadow$setItemInHand(hand, this.impl$activeItemStackCopy.copy());
            return;
        }

        this.shadow$setItemInHand(hand, ItemStackUtil.fromSnapshotToNative(event.itemStackResult().finalReplacement()));
    }

    @SuppressWarnings("ConstantConditions")
    @Redirect(method = "releaseUsingItem",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;releaseUsing(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)V"))
    // stopActiveHand
    private void impl$onStopPlayerUsing(final ItemStack stack, final net.minecraft.world.level.Level world, final LivingEntity self, final int duration) {
        if (this.shadow$level().isClientSide) {
            stack.releaseUsing(world, self, duration);
            return;
        }
        final PhaseTracker phaseTracker = PhaseTracker.getWorldInstance((ServerLevel) world);
        try (final CauseStackManager.StackFrame frame = phaseTracker.pushCauseFrame()) {
            final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(stack);
            final HandType handType = (HandType) (Object) this.shadow$getUsedItemHand();
            this.impl$addSelfToFrame(frame, snapshot, handType);
            final Ticks ticksDuration = SpongeTicks.ticksOrInfinite(duration);
            if (!SpongeCommon.post(SpongeEventFactory.createUseItemStackEventStop(phaseTracker.currentCause(),
                ticksDuration, ticksDuration, snapshot))) {
                stack.releaseUsing(world, self, duration);
                if (self instanceof ServerPlayer) {
                    // Log Change and capture SlotTransactions
                    phaseTracker.getPhaseContext().getTransactor().logPlayerInventoryChange(((ServerPlayer) self), PlayerInventoryTransaction.EventCreator.STANDARD);
                    ((ServerPlayer) self).inventoryMenu.broadcastChanges();
                }
            }
        }
    }

    @Inject(method = "stopUsingItem",
        at = @At("HEAD"))
    private void impl$onResetActiveHand(final CallbackInfo ci) {
        if (this.shadow$level().isClientSide) {
            return;
        }

        // If we finished using an item, impl$activeItemStackCopy will be non-null
        // However, if a player stopped using an item early, impl$activeItemStackCopy will not be set
        final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(this.impl$activeItemStackCopy != null ? this.impl$activeItemStackCopy : this.useItem);

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            this.impl$addSelfToFrame(frame, snapshot);
            final Ticks useItemRemainingTicks = SpongeTicks.ticksOrInfinite(this.useItemRemaining);
            SpongeCommon.post(SpongeEventFactory.createUseItemStackEventReset(PhaseTracker.getCauseStackManager().currentCause(),
                    useItemRemainingTicks, useItemRemainingTicks, snapshot));
        }
        this.impl$activeItemStackCopy = null;
    }

    // End implementation of UseItemStackEvent

    @Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
    private void impl$makeVanishable(final CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof VanishableBridge
                && ((VanishableBridge) this).bridge$vanishState().untargetable()) {
            // Sponge: Take into account untargetability from vanishing
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canBeSeenByAnyone", at = @At("HEAD"), cancellable = true)
    private void impl$ifVanishedCantBeSeenByAnyone(final CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof VanishableBridge
            && ((VanishableBridge) this).bridge$vanishState().untargetable()) {
            // Sponge: Take into account untargetability from vanishing
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "stopSleeping", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;clearSleepingPos()V"))
    private void impl$callFinishSleepingEvent(final CallbackInfo ci) {
        if (this.shadow$level().isClientSide) {
            return;
        }

        final Optional<BlockPos> sleepingPos = this.shadow$getSleepingPos();
        if (!sleepingPos.isPresent()) {
            return;
        }
        final BlockSnapshot snapshot = ((ServerWorld) this.shadow$level()).createSnapshot(sleepingPos.get().getX(), sleepingPos.get().getY(), sleepingPos.get().getZ());
        final Cause currentCause = Sponge.server().causeStackManager().currentCause();
        final ServerLocation loc = ServerLocation.of((ServerWorld) this.shadow$level(), VecHelper.toVector3d(this.shadow$position()));
        final Vector3d rot = ((Living) this).rotation();
        final SleepingEvent.Finish event = SpongeEventFactory.createSleepingEventFinish(currentCause, loc, loc, rot, rot, snapshot, (Living) this);
        Sponge.eventManager().post(event);
        this.shadow$clearSleepingPos();
        if (event.toLocation().world() != this.shadow$level()) {
            throw new UnsupportedOperationException("World change is not supported here.");
        }
        this.shadow$setPos(event.toLocation().x(), event.toLocation().y(), event.toLocation().z());
        ((Living) this).setRotation(event.toRotation());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;recheckStatus()V", shift = At.Shift.AFTER))
    private void impl$clearLastDamageSource(final CallbackInfo ci) {
        //Fix for MC-270896 - Players leak the last entity they took damage from
        if (this.lastDamageSource != null && this.shadow$level().getGameTime() - this.lastDamageStamp > 40L) {
            this.lastDamageSource = null;
        }
    }

}
