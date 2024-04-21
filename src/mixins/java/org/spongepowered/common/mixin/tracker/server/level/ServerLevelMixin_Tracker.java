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
package org.spongepowered.common.mixin.tracker.server.level;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.entity.explosive.Explosive;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.bridge.TrackableBridge;
import org.spongepowered.common.bridge.explosives.ExplosiveBridge;
import org.spongepowered.common.bridge.server.level.ServerLevelBridge;
import org.spongepowered.common.bridge.world.TrackedWorldBridge;
import org.spongepowered.common.bridge.world.entity.GrieferBridge;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.bridge.world.level.TrackableBlockEventDataBridge;
import org.spongepowered.common.bridge.world.level.block.state.BlockStateBridge;
import org.spongepowered.common.bridge.world.level.chunk.LevelChunkBridge;
import org.spongepowered.common.bridge.world.level.chunk.TrackedLevelChunkBridge;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.BlockChangeFlagManager;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.context.transaction.GameTransaction;
import org.spongepowered.common.event.tracking.context.transaction.block.ChangeBlock;
import org.spongepowered.common.event.tracking.context.transaction.block.RemoveBlockEntity;
import org.spongepowered.common.event.tracking.context.transaction.effect.CheckBlockPostPlacementIsSameEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.EffectResult;
import org.spongepowered.common.event.tracking.context.transaction.effect.NotifyClientEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.NotifyNeighborSideEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.PerformBlockDropsFromDestruction;
import org.spongepowered.common.event.tracking.context.transaction.effect.RemoveTileEntityFromChunkEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.SetAndRegisterBlockEntityToLevelChunk;
import org.spongepowered.common.event.tracking.context.transaction.effect.UpdateConnectingBlocksEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.UpdateLightSideEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.UpdateWorldRendererEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.WorldBlockChangeCompleteEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.WorldDestroyBlockLevelEffect;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.ChunkPipeline;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.PipelineCursor;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.TileEntityPipeline;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.UseBlockPipeline;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.UseItemOnBlockPipeline;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.UseItemPipeline;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.WorldPipeline;
import org.spongepowered.common.event.tracking.phase.general.GeneralPhase;
import org.spongepowered.common.event.tracking.phase.tick.TickPhase;
import org.spongepowered.common.mixin.tracker.world.level.LevelMixin_Tracker;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.SpongeBlockChangeFlag;
import org.spongepowered.common.world.server.SpongeLocatableBlockBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin_Tracker extends LevelMixin_Tracker implements TrackedWorldBridge {


    @Redirect(
            // This normally would target this.entityTickList.forEach((var2x) ->
            // but we don't have lambda syntax support yet.
            method = "lambda$tick$2",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;guardEntityTick(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/Entity;)V")
    )
    private void tracker$wrapNormalEntityTick(final ServerLevel level, final Consumer<Entity> entityUpdateConsumer,
        final Entity entity
    ) {
        final PhaseContext<@NonNull ?> currentState = PhaseTracker.getWorldInstance((ServerLevel) (Object) this).getPhaseContext();
        TrackingUtil.tickEntity(entityUpdateConsumer, entity);
    }

    @Override
    protected void tracker$wrapBlockEntityTick(final TickingBlockEntity blockEntity) {
        TrackingUtil.tickTileEntity(this, blockEntity);
    }


    /**
     * For PhaseTracking, we need to wrap around the
     * {@link BlockState#tick(ServerLevel, BlockPos, RandomSource)} method, and the ScheduledTickList uses a lambda method
     * to {@code ServerWorld#tickBlock(NextTickListEntry)}, so it's either we customize the ScheduledTickList
     * or we wrap in this method here.
     *
     * @param blockState The block state being ticked
     * @param worldIn The world (this world)
     * @param posIn The position of the block
     * @param randomIn The world random
     * @author gabizou - January 11th, 2020 - Minecraft 1.14.3
     */
    @Redirect(method = "tickBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void tracker$wrapBlockTick(final BlockState blockState, final ServerLevel worldIn, final BlockPos posIn, final RandomSource randomIn) {
        TrackingUtil.updateTickBlock(this, blockState, posIn, randomIn);
    }

    @Redirect(method = "tickFluid(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/Fluid;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void tracker$wrapFluidTick(final FluidState fluidState, final ServerLevel level, final BlockPos pos, final BlockState blockState) {
        TrackingUtil.updateTickFluid(this, fluidState, pos, blockState);
    }

    /**
     * For PhaseTracking, we need to wrap around the
     * {@link BlockState#tick(ServerLevel, BlockPos, RandomSource)} method, and the ScheduledTickList uses a lambda method
     * to {@code ServerWorld#tickBlock(NextTickListEntry)}, so it's either we customize the ScheduledTickList
     * or we wrap in this method here.
     *
     * @author gabizou - January 11th, 2020 - Minecraft 1.14.3
     */
    @Redirect(method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void tracker$wrapBlockRandomTick(final BlockState blockState, final ServerLevel worldIn, final BlockPos posIn, final RandomSource randomIn) {
        TrackingUtil.randomTickBlock(this, blockState, posIn, this.random);
    }

    @Redirect(method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"
        )
    )
    private void tracker$wrapFluidRandomTick(final FluidState fluidState, final ServerLevel worldIn, final BlockPos pos, final RandomSource random) {
        TrackingUtil.randomTickFluid(this, fluidState, pos, this.random);
    }

    @Inject(
        method = "tickChunk",
        at = @At(
            value = "INVOKE_STRING",
            target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V",
            args = "ldc=thunder"
        )
    )
    private void tracker$startWeatherTickPhase(final LevelChunk param0, final int param1, final CallbackInfo ci) {
        TickPhase.Tick.WEATHER.createPhaseContext(PhaseTracker.getWorldInstance((ServerLevel) (Object) this))
            .buildAndSwitch();
    }

    @Inject(
        method = "tickChunk",
        at = @At(
            value = "INVOKE_STRING",
            target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
            args = "ldc=tickBlocks"
        )
    )
    private void tracker$closeWeatherTickPhase(final LevelChunk param0, final int param1, final CallbackInfo ci) {
        final PhaseContext<@NonNull ?> context = PhaseTracker.getWorldInstance((ServerLevel) (Object) this).getPhaseContext();
        if (context.getState() != TickPhase.Tick.WEATHER) {
            throw new IllegalStateException("Expected to be in a Weather ticking state, but we aren't.");
        }
        context.close();
    }

    @Redirect(method = "doBlockEvent(Lnet/minecraft/world/level/BlockEventData;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;triggerEvent(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;II)Z"))
    private boolean tracker$wrapBlockStateEventReceived(final BlockState recievingState, final net.minecraft.world.level.Level thisWorld, final BlockPos targetPos, final int eventId, final int flag, final BlockEventData data) {
        return TrackingUtil.fireMinecraftBlockEvent((ServerLevel) (Object) this, data, recievingState);
    }

    @Redirect(
        method = "blockEvent(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;add(Ljava/lang/Object;)Z",
            remap = false
        )
    )
    private boolean tracker$associatePhaseContextDataWithBlockEvent(
        final ObjectLinkedOpenHashSet<BlockEventData> list, final Object data,
        final BlockPos pos, final Block blockIn, final int eventID, final int eventParam
    ) {
        final PhaseContext<@NonNull ?> currentContext = PhaseTracker.getWorldInstance((ServerLevel) (Object) this).getPhaseContext();
        final BlockEventData blockEventData = (BlockEventData) data;
        final TrackableBlockEventDataBridge blockEvent = (TrackableBlockEventDataBridge) (Object) blockEventData;
        // Short circuit phase states who do not track during block events
        if (currentContext.ignoresBlockEvent()) {
            return list.add(blockEventData);
        }

        final BlockState state = this.shadow$getBlockState(pos);
        if (((TrackableBridge) blockIn).bridge$allowsBlockEventCreation()) {
            blockEvent.bridge$setSourceUserUUID(currentContext.getActiveUserUUID());
            if (((BlockStateBridge) state).bridge$hasTileEntity()) {
                blockEvent.bridge$setTileEntity((BlockEntity) this.shadow$getBlockEntity(pos));
            }
            if (blockEvent.bridge$getTileEntity() == null) {
                final LocatableBlock locatable = new SpongeLocatableBlockBuilder()
                    .world((org.spongepowered.api.world.server.ServerWorld) this)
                    .position(pos.getX(), pos.getY(), pos.getZ())
                    .state((org.spongepowered.api.block.BlockState) state)
                    .build();
                blockEvent.bridge$setTickingLocatable(locatable);
            }
        }

        // Short circuit any additional handling. We've associated enough with the BlockEvent to
        // allow tracking to take place for other/future phases
        if (!((TrackableBridge) blockIn).bridge$allowsBlockEventCreation()) {
            return list.add((BlockEventData) data);
        }
        // In pursuant with our block updates management, we chose to
        // effectively allow the block event get added to the list, but
        // we log the transaction so that we can call the change block event
        // pre, and if needed, undo the add to the list.
        currentContext.appendNotifierToBlockEvent(this, pos, blockEvent);
        // We fire a Pre event to make sure our captures do not get stuck in a loop.
        // This is very common with pistons as they add block events while blocks are being notified.
        if (ShouldFire.CHANGE_BLOCK_EVENT_PRE) {
            if (blockIn instanceof PistonBaseBlock) {
                // We only fire pre events for pistons
                if (SpongeCommonEventFactory.handlePistonEvent(this, pos, state, eventID)) {
                    return false;
                }
            } else {
                if (SpongeCommonEventFactory.callChangeBlockEventPre((ServerLevelBridge) this, pos).isCancelled()) {
                    return false;
                }
            }
        }
        currentContext.getTransactor().logBlockEvent(state, this, pos, blockEvent);

        return list.add(blockEventData);
    }

    @Redirect(method = "explode", at = @At(value = "NEW",
        target = "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;Lnet/minecraft/world/phys/Vec3;FZLnet/minecraft/world/level/Explosion$BlockInteraction;)Lnet/minecraft/world/level/ServerExplosion;"))
    private ServerExplosion tracker$onExplode(final ServerLevel $$0, final Entity entity, final DamageSource $$2,
        final ExplosionDamageCalculator $$3, final Vec3 $$4, final float radius, final boolean $$6,
        final Explosion.BlockInteraction blockInteraction) {
        final var cannotGrief = entity instanceof GrieferBridge grieferBridge && !grieferBridge.bridge$canGrief();
        var finalRadius = radius;
        if (entity instanceof ExplosiveBridge bridge) {
            finalRadius = bridge.bridge$getExplosionRadius().orElse(finalRadius);
        }

        return new ServerExplosion($$0, entity, $$2, $$3, $$4, finalRadius, $$6, cannotGrief ? Explosion.BlockInteraction.KEEP : blockInteraction);
    }

    private org.spongepowered.api.world.explosion.Explosion tracker$apiExplosion;

    @Redirect(method = "explode", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/ServerExplosion;explode()V"))
    private void tracker$onExplode(final ServerExplosion instance) {
        var mcExplosion = instance;
        final var entity = instance.getDirectSourceEntity();
        // TODO entity can be null, what is our API explosive then?
        // TODO API explosive can be smth. that is not an entity?
        final var thisWorld = (ServerWorld) this;

        final var explosionBuilder = org.spongepowered.api.world.explosion.Explosion.builder().from((org.spongepowered.api.world.explosion.Explosion) mcExplosion);

        if (!(entity instanceof final Explosive apiExplosive)) {
            mcExplosion.explode();
            this.tracker$apiExplosion = explosionBuilder.build();
            return;
        }
        final PhaseTracker phaseTracker = PhaseTracker.getWorldInstance((ServerLevel) (Object) this);
        final var detonateEvent = SpongeEventFactory.createDetonateExplosiveEvent(phaseTracker.currentCause(),
                explosionBuilder, apiExplosive, (org.spongepowered.api.world.explosion.Explosion) instance);
        if (Sponge.eventManager().post(detonateEvent)) {
            this.tracker$cancelExplosionEffects(entity);
            return;
        }

        // Build the explosion from event
        var apiExplosion = detonateEvent.explosionBuilder().build();
        if (apiExplosion.radius() <= 0) {
            this.tracker$cancelExplosionEffects(entity);
            return;
        }

        if (ShouldFire.EXPLOSION_EVENT_PRE) {
            // Set up the pre event
            final ExplosionEvent.Pre event =
                SpongeEventFactory.createExplosionEventPre(phaseTracker.currentCause(), apiExplosion, thisWorld);
            if (SpongeCommon.post(event)) {
                this.tracker$cancelExplosionEffects(entity);
                return;
            }

            try {
                // Since we already have the API created implementation Explosion, let's use it.
                mcExplosion = (ServerExplosion) event.explosion();
                apiExplosion = event.explosion();
            } catch (final ClassCastException e) {
                new org.spongepowered.asm.util.PrettyPrinter(60).add("Explosion not compatible with this implementation").centre().hr()
                    .add("An explosion that was expected to be used for this implementation does not originate from this implementation.")
                    .trace();
                mcExplosion = (ServerExplosion) detonateEvent.explosionBuilder().build();
            }
        }

        try (final PhaseContext<@NonNull ?> ctx = GeneralPhase.State.EXPLOSION.createPhaseContext(phaseTracker).explosion(mcExplosion)
            .source(((Optional) apiExplosion.sourceExplosive()).orElse(this))) {
            ctx.buildAndSwitch();

            mcExplosion.explode();
        }

        this.tracker$apiExplosion = apiExplosion;
    }

    @Inject(method = "explode", cancellable = true, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/ServerExplosion;explode()V", shift = At.Shift.AFTER))
    private void tracker$onCancelled(final CallbackInfo ci) {
        if (this.tracker$apiExplosion == null) {
            ci.cancel();
        }
    }

    /**
     * See {@link net.minecraft.client.multiplayer.ClientPacketListener#handleExplosion} for client side handling
     */
    @Redirect(method = "explode", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void tracker$onClientboundExplodePacket(final ServerGamePacketListenerImpl instance, final Packet packet) {
        final var originalPacket = (ClientboundExplodePacket) packet;
        ((ServerLevelBridge) this).bridge$handleExplosionPacket(instance, this.tracker$apiExplosion, originalPacket);
    }

    @Inject(method = "explode", at = @At(value = "RETURN"))
    private void tracker$afterExplodeCleanup(final Entity $$0, final DamageSource $$1,
        final ExplosionDamageCalculator $$2, final double $$3, final double $$4, final double $$5, final float $$6,
        final boolean $$7, final Level.ExplosionInteraction $$8, final ParticleOptions $$9, final ParticleOptions $$10,
        final Holder<SoundEvent> $$11, final CallbackInfo ci) {
        this.tracker$apiExplosion = null;
    }

    private void tracker$cancelExplosionEffects(final Entity entity) {
        // TODO cancel effects
        if (entity instanceof ExplosiveBridge explosiveBridge) {
            explosiveBridge.bridge$cancelExplosion();
        }
    }

    @Override
    public Optional<WorldPipeline.Builder> bridge$startBlockChange(final BlockPos pos, final BlockState newState, final int flags) {
        if (((ServerLevel) (Object) this).isOutsideBuildHeight(pos)) {
            return Optional.empty();
        } else if (this.shadow$isDebug()) { // isClientSide is always false since this is WorldServer
            return Optional.empty();
        }
        // Sponge Start - Sanity check against the PhaseTracker for instances
        if (this.bridge$isFake()) {
            return Optional.empty();
        }
        final PhaseTracker instance = PhaseTracker.getWorldInstance((ServerLevel) (Object) this);
        if (!instance.onSidedThread()) {
            throw new UnsupportedOperationException("Cannot perform a tracked Block Change on a ServerWorld while not on the main thread!");
        }
        final SpongeBlockChangeFlag spongeFlag = BlockChangeFlagManager.fromNativeInt(flags);

        final LevelChunk chunk = this.shadow$getChunkAt(pos);
        if (chunk.isEmpty()) {
            return Optional.empty();
        }
        final net.minecraft.world.level.block.state.BlockState currentState = chunk.getBlockState(pos);


        return Optional.of(this.bridge$makePipeline(pos, currentState, newState, chunk, spongeFlag, Constants.World.DEFAULT_BLOCK_CHANGE_LIMIT));
    }

    private WorldPipeline.Builder bridge$makePipeline(
        final BlockPos pos,
        final BlockState currentState,
        final BlockState newState,
        final LevelChunk chunk,
        final SpongeBlockChangeFlag spongeFlag,
        final int limit
    ) {
        final TrackedLevelChunkBridge mixinChunk = (TrackedLevelChunkBridge) chunk;

        // Then build and use the BlockPipeline
        final ChunkPipeline chunkPipeline = mixinChunk.bridge$createChunkPipeline(pos, newState, currentState, spongeFlag, limit);
        final WorldPipeline.Builder worldPipelineBuilder = WorldPipeline.builder(chunkPipeline);
        worldPipelineBuilder.addEffect((pipeline, oldState, args) -> {
            if (oldState == null) {
                return EffectResult.nullReturn();
            }
            return EffectResult.nullPass();
        })
            .addEffect(UpdateLightSideEffect.getInstance())
            .addEffect(CheckBlockPostPlacementIsSameEffect.getInstance())
            .addEffect(UpdateWorldRendererEffect.getInstance())
            .addEffect(NotifyClientEffect.getInstance())
            .addEffect(NotifyNeighborSideEffect.getInstance())
            .addEffect(UpdateConnectingBlocksEffect.getInstance());
        return worldPipelineBuilder;
    }

    /**
     * @author gabizou, March 12th, 2016
     * <p>
     * Move this into WorldServer as we should not be modifying the client world.
     * <p>
     * Purpose: Rewritten to support capturing blocks
     */
    @Override
    public boolean setBlock(final BlockPos pos, final net.minecraft.world.level.block.state.BlockState newState, final int flags, final int limit) {
        if (((ServerLevel) (Object) this).isOutsideBuildHeight(pos)) {
            return false;
        } else if (this.shadow$isDebug()) { // isClientSide is always false since this is WorldServer
            return false;
        }
        // Sponge Start - Sanity check against the PhaseTracker for instances
        if (this.bridge$isFake()) {
            return super.setBlock(pos, newState, flags, limit);
        }
        final PhaseTracker instance = PhaseTracker.getWorldInstance((ServerLevel) (Object) this);
        if (!instance.onSidedThread()) {
            throw new UnsupportedOperationException("Cannot perform a tracked Block Change on a ServerWorld while not on the main thread!");
        }
        final SpongeBlockChangeFlag spongeFlag = BlockChangeFlagManager.fromNativeInt(flags);

        final LevelChunk chunk = this.shadow$getChunkAt(pos);
        if (chunk.isEmpty()) {
            return false;
        }
        final net.minecraft.world.level.block.state.BlockState currentState = chunk.getBlockState(pos);
        // Check if the transaction would be rendered redundant, if so, follow minecraft's normal
        // change of "don't do anything if the block is the same".
        if (currentState == newState) {
            return false;
        }
        final WorldPipeline pipeline = this.bridge$makePipeline(pos, currentState, newState, chunk, spongeFlag, limit)
            .addEffect(WorldBlockChangeCompleteEffect.getInstance())
            .build();

        return pipeline.processEffects(instance.getPhaseContext(), currentState, newState, pos, null, spongeFlag, limit);
    }

    @Override
    public boolean destroyBlock(final BlockPos pos, final boolean doDrops, @Nullable final Entity p_241212_3_, final int limit) {
        final BlockState currentState = this.shadow$getBlockState(pos);
        if (currentState.isAir()) {
            return false;
        } else {
            // Sponge Start - Sanity check against the PhaseTracker for instances
            if (this.bridge$isFake()) {
                return super.destroyBlock(pos, doDrops, p_241212_3_, limit);
            }
            final PhaseTracker instance = PhaseTracker.getWorldInstance((ServerLevel) (Object) this);
            if (!instance.onSidedThread()) {
                throw new UnsupportedOperationException("Cannot perform a tracked Block Change on a ServerWorld while not on the main thread!");
            }
            final FluidState fluidstate = this.shadow$getFluidState(pos);
            final BlockState emptyBlock = fluidstate.createLegacyBlock();
            final SpongeBlockChangeFlag spongeFlag = BlockChangeFlagManager.fromNativeInt(3);

            final LevelChunk chunk = this.shadow$getChunkAt(pos);
            if (chunk.isEmpty()) {
                return false;
            }
            final WorldPipeline.Builder pipelineBuilder = this.bridge$makePipeline(pos, currentState, emptyBlock, chunk, spongeFlag, limit)
                .addEffect(WorldDestroyBlockLevelEffect.getInstance());

            if (doDrops) {
                pipelineBuilder.addEffect(PerformBlockDropsFromDestruction.getInstance());
            }

            final WorldPipeline pipeline = pipelineBuilder
                .addEffect(WorldBlockChangeCompleteEffect.getInstance())
                .build();

            return pipeline.processEffects(instance.getPhaseContext(), currentState, emptyBlock, pos, p_241212_3_, spongeFlag, limit);
        }
    }

    @Override
    public SpongeBlockSnapshot bridge$createSnapshot(final net.minecraft.world.level.block.state.BlockState state, final BlockPos pos,
        final BlockChangeFlag updateFlag
    ) {
        final SpongeBlockSnapshot.BuilderImpl builder = SpongeBlockSnapshot.BuilderImpl.pooled();
        builder.reset();
        builder.blockState(state)
            .world((ServerLevel) (Object) this)
            .position(VecHelper.toVector3i(pos));
        final LevelChunk chunk = this.shadow$getChunkAt(pos);
        if (chunk == null) {
            return builder.flag(updateFlag).build();
        }
        final Optional<UUID> creator = ((LevelChunkBridge) chunk).bridge$getBlockCreatorUUID(pos);
        final Optional<UUID> notifier = ((LevelChunkBridge) chunk).bridge$getBlockNotifierUUID(pos);
        creator.ifPresent(builder::creator);
        notifier.ifPresent(builder::notifier);
        final boolean hasTileEntity = ((BlockStateBridge) state).bridge$hasTileEntity();
        final net.minecraft.world.level.block.entity.BlockEntity tileEntity = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
        if (hasTileEntity || tileEntity != null) {
            // We MUST only check to see if a TE exists to avoid creating a new one.
            if (tileEntity != null) {
                // TODO - custom data.
                final CompoundTag nbt = new CompoundTag();
                // Some mods like OpenComputers assert if attempting to save robot while moving
                try {
                    tileEntity.saveWithFullMetadata(tileEntity.getLevel().registryAccess());
                    builder.addUnsafeCompound(nbt);
                } catch (final Throwable t) {
                    // ignore
                }
            }
        }
        builder.flag(updateFlag);
        return builder.build();
    }

    @Override
    public UseItemOnBlockPipeline bridge$startInteractionUseOnChange(net.minecraft.world.level.Level worldIn, ServerPlayer playerIn, InteractionHand handIn, BlockHitResult blockRaytraceResultIn, BlockState blockstate, ItemStack copiedStack) {
        if (this.shadow$isDebug()) { // isClientSide is always false since this is WorldServer
            return null;
        }
        if (this.bridge$isFake()) {
            return null;
        }

        final var instance = PhaseTracker.getInstance();
        if (instance.getSidedThread() != PhaseTracker.SERVER.getSidedThread() && instance != PhaseTracker.SERVER) {
            throw new UnsupportedOperationException("Cannot perform a tracked Block Change on a ServerWorld while not on the main thread!");
        }
        final var pipeline = new UseItemOnBlockPipeline(
            (ServerLevel) worldIn,
            playerIn,
            handIn,
            blockRaytraceResultIn,
            blockstate,
            copiedStack,
            instance.getPhaseContext().getTransactor()
        );
        return pipeline;
    }

    @Override
    public UseBlockPipeline bridge$startInteractionChange(net.minecraft.world.level.Level worldIn, ServerPlayer playerIn, InteractionHand handIn, BlockHitResult blockRaytraceResultIn, BlockState blockstate, ItemStack copiedStack) {
        if (this.shadow$isDebug()) { // isClientSide is always false since this is WorldServer
            return null;
        }
        if (this.bridge$isFake()) {
            return null;
        }

        final var instance = PhaseTracker.getInstance();
        if (instance.getSidedThread() != PhaseTracker.SERVER.getSidedThread() && instance != PhaseTracker.SERVER) {
            throw new UnsupportedOperationException("Cannot perform a tracked Block Change on a ServerWorld while not on the main thread!");
        }
        final var pipeline = new UseBlockPipeline(
            (ServerLevel) worldIn,
            playerIn,
            handIn,
            blockRaytraceResultIn,
            blockstate,
            copiedStack,
            instance.getPhaseContext().getTransactor()
        );
        return pipeline;
    }
    @Override
    public UseItemPipeline bridge$startItemInteractionChange(net.minecraft.world.level.Level worldIn, ServerPlayer playerIn, InteractionHand handIn, ItemStack copiedStack, BlockHitResult blockRaytraceResult, boolean creative) {
        if (this.shadow$isDebug()) { // isClientSide is always false since this is WorldServer
            return null;
        }
        if (this.bridge$isFake()) {
            return null;
        }

        final var instance = PhaseTracker.getInstance();
        if (instance.getSidedThread() != PhaseTracker.SERVER.getSidedThread() && instance != PhaseTracker.SERVER) {
            throw new UnsupportedOperationException("Cannot perform a tracked Block Change on a ServerWorld while not on the main thread!");
        }
        return new UseItemPipeline(
            (ServerLevel) worldIn,
            playerIn,
            handIn,
            copiedStack,
            blockRaytraceResult,
            creative,
            instance.getPhaseContext().getTransactor()
        );
    }

    /**
     * Technically an overwrite, but because this is simply an override, we can
     * effectively do as we need to, which is determine if we are performing
     * block transactional recording, go ahead and record transactions.
     * <p>In the event that we are performing transaction recording, the following
     * must be true:
     * <ul>
     *     <li>This world instance is managed and verified by Sponge</li>
     *     <li>This world must {@link LevelBridge#bridge$isFake()} return {@code false}</li>
     *     <li>The {@link PhaseTracker#SERVER}'s {@link PhaseTracker#getSidedThread()} must be {@code ==} {@link Thread#currentThread()}</li
     *     <li>The current {@link IPhaseState} must be allowing to record transactions with an applicable {@link org.spongepowered.common.event.tracking.context.transaction.TransactionalCaptureSupplier}</li>
     * </ul>
     * After which, we may be able to appropriately associate the {@link net.minecraft.world.level.block.entity.BlockEntity}
     * being removed with either an existing {@link ChangeBlock},
     * or generate a new {@link RemoveBlockEntity} transaction
     * that would otherwise be able to associate with either the current {@link IPhaseState} or a parent {@link GameTransaction}
     * if this call is the result of a {@link org.spongepowered.common.event.tracking.context.transaction.effect.ProcessingSideEffect}..
     *
     * @param pos The position of the tile entity to remove
     * @author gabizou - July 31st, 2020 - Minecraft 1.14.3
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void shadow$removeBlockEntity(final BlockPos pos) {
        final BlockPos immutable = pos.immutable();
        final net.minecraft.world.level.block.entity.BlockEntity tileentity = this.shadow$getBlockEntity(immutable);
        if (tileentity == null) {
            return;
        }
        final PhaseTracker phaseTracker = PhaseTracker.getWorldInstance((ServerLevel) (Object) this);
        if (this.bridge$isFake() || !phaseTracker.onSidedThread()) {
            // If we're fake or not on the server thread, well, we could effectively call
            // out whoever is trying to remove tile entities asynchronously....
            super.shadow$removeBlockEntity(immutable);
            return;
        }
        // Otherwise, let's go on and check if we're recording transactions,
        // and if so, log the tile entity removal (may associate with an existing transaction,
        // or create a new transaction.
        final PhaseContext<@NonNull ?> current = phaseTracker.getPhaseContext();
        if (current.getTransactor().logTileRemoval(tileentity, () -> (ServerLevel) (Object) this)) {
            final TileEntityPipeline pipeline = TileEntityPipeline.kickOff((ServerLevel) (Object) this, immutable)
                .addEffect(RemoveTileEntityFromChunkEffect.getInstance())
                .build();
            pipeline.processEffects(current, new PipelineCursor(tileentity.getBlockState(), immutable, tileentity, (Entity) null, Constants.World.DEFAULT_BLOCK_CHANGE_LIMIT));
            return;
        }
        super.shadow$removeBlockEntity(immutable);
    }

    @SuppressWarnings({"RedundantCast", "ConstantConditions"})
    @Override
    public void shadow$setBlockEntity(final net.minecraft.world.level.block.entity.BlockEntity proposed) {
        final BlockPos immutable = proposed.getBlockPos().immutable();
        final PhaseTracker phaseTracker = PhaseTracker.getWorldInstance((ServerLevel) (Object) this);
        if (this.bridge$isFake() || !phaseTracker.onSidedThread()) {
            // If we're fake or not on the server thread, well, we could effectively call
            // out whoever is trying to remove tile entities asynchronously....
            super.shadow$setBlockEntity(proposed);
            return;
        }
        if (proposed != null) {
            if (proposed.getLevel() != (ServerLevel) (Object) this) {
                proposed.setLevel((ServerLevel) (Object) this);
            }
        }
        // Otherwise, let's go on and check if we're recording transactions,
        // and if so, log the tile entity removal (may associate with an existing transaction,
        // or create a new transaction.
        final PhaseContext<@NonNull ?> current = phaseTracker.getPhaseContext();
        if (current.doesBlockEventTracking()) {
            final net.minecraft.world.level.block.entity.@Nullable BlockEntity existing = this.shadow$getChunkAt(immutable).getBlockEntity(immutable);
            if (current.getTransactor().logTileReplacement(immutable, existing, proposed, () -> (ServerLevel) (Object) this)) {
                final TileEntityPipeline pipeline = TileEntityPipeline.kickOff((ServerLevel) (Object) this, immutable)
                    .addEffect(SetAndRegisterBlockEntityToLevelChunk.getInstance())
                    .build();
                pipeline.processEffects(current, new PipelineCursor(proposed.getBlockState(), immutable, proposed, (Entity) null, Constants.World.DEFAULT_BLOCK_CHANGE_LIMIT));
                return;
            }
        }

        super.shadow$setBlockEntity(proposed);
    }

    @Inject(
        method = "addEntity(Lnet/minecraft/world/entity/Entity;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;addNewEntity(Lnet/minecraft/world/level/entity/EntityAccess;)Z"
        ),
        cancellable = true
    )
    private void tracker$throwPreEventAndRecord(final Entity entityIn, final CallbackInfoReturnable<Boolean> cir) {
        if (this.bridge$isFake()) {
            return;
        }
        final PhaseTracker tracker = PhaseTracker.getWorldInstance((ServerLevel) (Object) this);
        if (!tracker.onSidedThread()) {
            // TODO - async entity spawn logging
            return;
        }
        final PhaseContext<@NonNull ?> current = tracker.getPhaseContext();
        if (!current.doesAllowEntitySpawns()) {
            cir.setReturnValue(false);
            return;
        }

        try (final CauseStackManager.StackFrame frame = tracker.pushCauseFrame()) {
            final List<org.spongepowered.api.entity.Entity> entities = new ArrayList<>();
            entities.add((org.spongepowered.api.entity.Entity) entityIn);

            frame.addContext(EventContextKeys.SPAWN_TYPE, current.getSpawnTypeForTransaction(entityIn));
            final SpawnEntityEvent.Pre pre = SpongeEventFactory.createSpawnEntityEventPre(frame.currentCause(), entities);
            Sponge.eventManager().post(pre);

            if (pre.isCancelled() || entities.isEmpty()) {
                cir.setReturnValue(false);
                return;
            }
        }

        if (current.allowsBulkEntityCaptures()) {
            current.getTransactor().logEntitySpawn(current, this, entityIn);
        }
    }

}
