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
package org.spongepowered.common.mixin.core.world.entity.player;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.kyori.adventure.bossbar.BossBar;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player.BedSleepingProblem;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.authlib.GameProfileHolderBridge;
import org.spongepowered.common.bridge.effect.ViewerBridge;
import org.spongepowered.common.bridge.server.level.ServerLevelBridge;
import org.spongepowered.common.bridge.server.level.ServerPlayerBridge;
import org.spongepowered.common.bridge.world.entity.player.PlayerBridge;
import org.spongepowered.common.bridge.world.food.FoodDataBridge;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.mixin.core.world.entity.LivingEntityMixin;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.ExperienceHolderUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(net.minecraft.world.entity.player.Player.class)
public abstract class PlayerMixin extends LivingEntityMixin implements PlayerBridge, GameProfileHolderBridge, ViewerBridge {

    // @formatter: off
    @Shadow @Final protected static EntityDataAccessor<Byte> DATA_PLAYER_MODE_CUSTOMISATION;
    @Shadow public int experienceLevel;
    @Shadow public int totalExperience;
    @Shadow public float experienceProgress;
    @Shadow public AbstractContainerMenu containerMenu;
    @Shadow @Final public InventoryMenu inventoryMenu;
    @Shadow @Final private GameProfile gameProfile;
    @Shadow public abstract boolean shadow$isSpectator();
    @Shadow protected abstract int shadow$getPermissionLevel();
    @Shadow public abstract int shadow$getXpNeededForNextLevel();
    @Shadow @Nullable public abstract ItemEntity shadow$drop(final ItemStack droppedItem, final boolean dropAround, final boolean traceItem);
    @Shadow public abstract FoodData shadow$getFoodData();
    @Shadow public abstract Scoreboard shadow$getScoreboard();
    @Shadow public abstract boolean shadow$isCreative();
    @Shadow public abstract String shadow$getScoreboardName();
    @Shadow public abstract void shadow$awardStat(ResourceLocation stat);
    @Shadow public abstract Inventory shadow$getInventory();
    @Shadow public Either<BedSleepingProblem, Unit> shadow$startSleepInBed(final BlockPos param0) {
        return null; // Shadowed
    }
    @Shadow protected abstract void shadow$playShoulderEntityAmbientSound(CompoundTag p_192028_1_);
    // @formatter: on

    private boolean impl$affectsSpawning = true;
    protected final boolean impl$isFake = this.bridge$isFakePlayer();

    private final Set<BossBar> impl$activeBossBars = new HashSet<>();
    private final Set<BossBar> impl$unmodifiableActiveBossBars = Collections.unmodifiableSet(this.impl$activeBossBars);

    @Override
    public boolean bridge$affectsSpawning() {
        return this.impl$affectsSpawning && !this.shadow$isSpectator() && !this.bridge$vanishState().untargetable();
    }

    @Override
    public void bridge$setAffectsSpawning(final boolean affectsSpawning) {
        this.impl$affectsSpawning = affectsSpawning;
    }

    @Override
    public GameProfile bridge$getGameProfile() {
        return this.gameProfile;
    }

    @Override
    public int bridge$getExperienceSinceLevel() {
        return this.totalExperience - ExperienceHolderUtil.xpAtLevel(this.experienceLevel);
    }

    @Override
    public void bridge$setExperienceSinceLevel(final int experience) {
        this.totalExperience = ExperienceHolderUtil.xpAtLevel(this.experienceLevel) + experience;
        this.experienceProgress = (float) experience / this.shadow$getXpNeededForNextLevel();
    }

    @Override
    public Iterable<? extends BossBar> bridge$getActiveBossBars() {
        return this.impl$unmodifiableActiveBossBars;
    }

    @Override
    public void bridge$addActiveBossBar(final BossBar bar) {
        this.impl$activeBossBars.add(bar);
    }

    @Override
    public void bridge$removeActiveBossBar(final BossBar bar) {
        this.impl$activeBossBars.remove(bar);
    }

    @Redirect(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
        )
    )
    private List<Entity> impl$ignoreOtherEntitiesWhenUncollideable(final Level level, final Entity entity, final AABB aabb) {
        if (this.bridge$vanishState().ignoresCollisions()) {
            return Collections.emptyList();
        }
        return level.getEntities(entity, aabb);
    }

    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSleeping()Z"))
    private boolean impl$postSleepingEvent(final net.minecraft.world.entity.player.Player self) {
        if (self.isSleeping()) {
            if (!((LevelBridge) this.shadow$level()).bridge$isFake()) {
                final CauseStackManager csm = PhaseTracker.getCauseStackManager();
                csm.pushCause(this);
                final BlockPos bedLocation = this.shadow$getSleepingPos().get();
                final BlockSnapshot snapshot = ((ServerWorld) this.shadow$level()).createSnapshot(bedLocation.getX(), bedLocation.getY(), bedLocation.getZ());
                SpongeCommon.post(SpongeEventFactory.createSleepingEventTick(csm.currentCause(), snapshot, (Living) this));
                csm.popCause();
            }
            return true;
        }
        return false;
    }

    @Redirect(method = "getDisplayName", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getName()Lnet/minecraft/network/chat/Component;"))
    private Component impl$useCustomNameIfSet(final net.minecraft.world.entity.player.Player playerEntity) {
        if (playerEntity instanceof ServerPlayer) {
            if (playerEntity.hasCustomName()) {
                return playerEntity.getCustomName();
            }

            return playerEntity.getName();
        }

        return playerEntity.getName();
    }

    @Redirect(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;playShoulderEntityAmbientSound(Lnet/minecraft/nbt/CompoundTag;)V"
        )
    )
    private void impl$ignoreShoulderSoundsWhileVanished(final net.minecraft.world.entity.player.Player thisPlayer, final CompoundTag tag) {
        if (!this.bridge$vanishState().createsSounds()) {
            return;
        }
        this.shadow$playShoulderEntityAmbientSound(tag);
    }

    @Redirect(
        method = {
            "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V",
            "giveExperienceLevels(I)V",
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"
        )
    )
    private void impl$ignoreExperienceLevelSoundsWhileVanished(final Level world,
        final net.minecraft.world.entity.player.Player player, final double x, final double y, final double z,
        final SoundEvent sound, final SoundSource category, final float volume, final float pitch
    ) {
        if (!this.bridge$vanishState().createsSounds()) {
            return;
        }
        this.shadow$level().playSound(player, x, y, z, sound, category, volume, pitch);
    }

    @Redirect(method = "canUseGameMasterBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getPermissionLevel()I"))
    private int impl$checkPermissionForCommandBlock(final net.minecraft.world.entity.player.Player playerEntity) {
        if (this instanceof Subject) {
            return ((Subject) this).hasPermission(Constants.Permissions.COMMAND_BLOCK_PERMISSION) ? Constants.Permissions.COMMAND_BLOCK_LEVEL : 0;
        } else {
            return this.shadow$getPermissionLevel();
        }
    }

    /**
     * @author gabizou - September 4th, 2018
     * @author i509VCB - February 17th, 2020 - 1.14.4
     * @reason Bucket placement and other placements can be "detected"
     * for pre change events prior to them actually processing their logic,
     * this in effect can prevent item duplication issues when the block
     * changes are cancelled, but inventory is already modified. It would
     * be considered that during interaction packets, inventory is monitored,
     * however, sometimes that isn't enough.
     * @return Check if the player is a fake player, if it is, then just do
     *  the same return, otherwise, throw an event first and then return if the
     *  event is cancelled, or the stack.canPlaceOn
     */
    @Redirect(method = "mayUseItemAt",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;canPlaceOnBlockInAdventureMode(Lnet/minecraft/world/level/block/state/pattern/BlockInWorld;)Z"))
    private boolean impl$callChangeBlockPre(final ItemStack stack, final BlockInWorld cachedBlockInfo) {
        // Lazy evaluation, if the stack isn't placeable anyways, might as well not
        // call the logic.
        if (!stack.canPlaceOnBlockInAdventureMode(cachedBlockInfo)) {
            return false;
        }
        // If we're going to throw an event, then do it.
        // Just sanity checks, if the player is not in a managed world, then don't bother either.
        // some fake players may exist in pseudo worlds as well, which means we don't want to
        // process on them since the world is not a valid world to plugins.
        if (this.shadow$level() instanceof LevelBridge && !((LevelBridge) this.shadow$level()).bridge$isFake() && ShouldFire.CHANGE_BLOCK_EVENT_PRE) {
            // Note that this can potentially cause phase contexts to auto populate frames
            // we shouldn't rely so much on them, but sometimes the extra information is provided
            // through this method.
            try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
                // Go ahead and add the item stack in use, just in the event the current phase contexts don't provide
                // that information.
                frame.addContext(EventContextKeys.USED_ITEM, ItemStackUtil.snapshotOf(stack));
                // Then go ahead and call the event and return if it was cancelled
                // if it was cancelled, then there should be no changes needed to roll back
                return !SpongeCommonEventFactory.callChangeBlockEventPre((ServerLevelBridge) this.shadow$level(), cachedBlockInfo.getPos(), this).isCancelled();
            }
        }
        // Otherwise, if all else is ignored, or we're not throwing events, we're just going to return the
        // default value: true.
        return true;
    }

    @Inject(method = "getFireImmuneTicks", at = @At(value = "HEAD"), cancellable = true)
    private void impl$useCustomFireImmuneTicks(final CallbackInfoReturnable<Integer> ci) {
        if (this.impl$hasCustomFireImmuneTicks) {
            ci.setReturnValue((int) this.impl$fireImmuneTicks);
        }
    }

    @Inject(method = "interactOn", at = @At(value = "HEAD"), cancellable = true)
    protected void impl$onRightClickEntity(final Entity entityToInteractOn, final InteractionHand hand,
        final CallbackInfoReturnable<InteractionResult> cir) {
    }

    @Override
    public boolean impl$canCallIgniteEntityEvent() {
        return super.impl$canCallIgniteEntityEvent() && !this.shadow$isSpectator() && !this.shadow$isCreative();
    }

    @Inject(method = "canHarmPlayer", at = @At("HEAD"), cancellable = true)
    private void impl$onCanHarmPlayer(final net.minecraft.world.entity.player.Player other, final CallbackInfoReturnable<Boolean> cir) {
        if (!(other instanceof org.spongepowered.api.entity.living.player.server.ServerPlayer)) {
            return;
        }

        //Check whatever the other player can hit this player
        //Fixes per player scoreboards which could have different team set up for each player
        final Team otherTeam = other.getTeam();
        final Team thisTeam = ((net.minecraft.world.scores.Scoreboard) ((ServerPlayerBridge) other).bridge$getScoreboard()).getPlayersTeam(this.shadow$getScoreboardName());

        cir.setReturnValue(otherTeam == null || !otherTeam.isAlliedTo(thisTeam) || otherTeam.isAllowFriendlyFire());
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void impl$foodData(final CallbackInfo ci) {
        ((FoodDataBridge) this.shadow$getFoodData()).bridge$setPlayer((net.minecraft.world.entity.player.Player) (Object) this);
    }
}
