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
package org.spongepowered.common.mixin.core.world;

import static com.google.common.base.Preconditions.checkArgument;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.typesafe.config.ConfigRenderOptions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEventData;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketSpawnGlobalEntity;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.ScheduledBlockUpdate;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.translator.ConfigurateTranslator;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.cause.entity.spawn.WeatherSpawnCause;
import org.spongepowered.api.event.entity.ConstructEntityEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.GeneratorType;
import org.spongepowered.api.world.GeneratorTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.WorldCreationSettings;
import org.spongepowered.api.world.gen.BiomeGenerator;
import org.spongepowered.api.world.gen.WorldGenerator;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.config.SpongeConfig;
import org.spongepowered.common.data.util.DataQueries;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.EventConsumer;
import org.spongepowered.common.event.InternalNamedCauses;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.CauseTracker;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseData;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.phase.PluginPhase;
import org.spongepowered.common.event.tracking.phase.TrackingPhases;
import org.spongepowered.common.event.tracking.phase.function.EntityListConsumer;
import org.spongepowered.common.interfaces.IMixinBlockUpdate;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.world.IMixinWorldInfo;
import org.spongepowered.common.interfaces.world.IMixinWorldProvider;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import org.spongepowered.common.interfaces.world.gen.IMixinChunkProviderServer;
import org.spongepowered.common.interfaces.world.gen.IPopulatorProvider;
import org.spongepowered.common.registry.provider.DirectionFacingProvider;
import org.spongepowered.common.util.SpongeHooks;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.DimensionManager;
import org.spongepowered.common.world.border.PlayerBorderListener;
import org.spongepowered.common.world.gen.SpongeChunkGenerator;
import org.spongepowered.common.world.gen.SpongeGenerationPopulator;
import org.spongepowered.common.world.gen.SpongeWorldGenerator;
import org.spongepowered.common.world.gen.WorldGenConstants;
import org.spongepowered.common.world.type.SpongeWorldType;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer extends MixinWorld implements IMixinWorldServer {

    private static final Vector3i BLOCK_MIN = new Vector3i(-30000000, 0, -30000000);
    private static final Vector3i BLOCK_MAX = new Vector3i(30000000, 256, 30000000).sub(1, 1, 1);
    private static final String BLOCK_UPDATE_TICK =
            "Lnet/minecraft/block/Block;updateTick(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V";
    private static final String
            BLOCK_RANDOM_TICK =
            "Lnet/minecraft/block/Block;randomTick(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V";

    private final CauseTracker causeTracker = new CauseTracker((WorldServer) (Object) this);
    private Map<BlockPos, User> trackedBlockEvents = Maps.newHashMap();
    private final Map<net.minecraft.entity.Entity, Vector3d> rotationUpdates = new HashMap<>();
    private SpongeChunkGenerator spongegen;
    private Integer dimensionId = null;
    private SpongeConfig<?> activeConfig;

    @Shadow @Final private MinecraftServer mcServer;
    @Shadow @Final private Set<NextTickListEntry> pendingTickListEntriesHashSet;
    @Shadow @Final private TreeSet<NextTickListEntry> pendingTickListEntriesTreeSet;
    @Shadow public abstract boolean fireBlockEvent(BlockEventData event);
    @Shadow @Nullable public abstract net.minecraft.entity.Entity getEntityFromUuid(UUID uuid);

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onConstructed(MinecraftServer server, ISaveHandler saveHandlerIn, WorldInfo info, int dimensionId, Profiler profilerIn,
            CallbackInfo ci) {
        ((World) (Object) this).getWorldBorder().addListener(new PlayerBorderListener(this.getMinecraftServer(), dimensionId));
    }

    @Inject(method = "createSpawnPosition(Lnet/minecraft/world/WorldSettings;)V", at = @At("HEAD"), cancellable = true)
    public void onCreateSpawnPosition(WorldSettings settings, CallbackInfo ci) {
        GeneratorType generatorType = (GeneratorType) settings.getTerrainType();
        if (generatorType != null && generatorType.equals(GeneratorTypes.THE_END)) {
            this.worldInfo.setSpawn(new BlockPos(55, 60, 0));
            ci.cancel();
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    public void beforeInit(CallbackInfoReturnable<World> cir) {
        super.init(); // Call the super (vanilla doesn't do this)
        updateWorldGenerator();
    }


    @Override
    public boolean isProcessingExplosion() {
        return this.processingExplosion;
    }

    @Override
    public SpongeConfig<SpongeConfig.WorldConfig> getWorldConfig() {
        return ((IMixinWorldInfo) this.worldInfo).getWorldConfig();
    }


    @Override
    public SpongeConfig<?> getActiveConfig() {
        return this.activeConfig;
    }

    @Override
    public void setActiveConfig(SpongeConfig<?> config) {
        this.activeConfig = config;
    }

    @Override
    public int getDimensionId() {
        return dimensionId;
    }

    @Override
    public boolean isLoaded() {
        return DimensionManager.getWorldByDimensionId(getDimensionId()).isPresent();
    }

    @Override
    public void setDimensionId(int dimensionId) {
        if (this.dimensionId != null) {
            throw new RuntimeException("Attempt was made to re-set dimension id on world!");
        }

        this.dimensionId = dimensionId;
    }

    @Override
    public void updateWorldGenerator() {

        // Get the default generator for the world type
        DataContainer generatorSettings = this.getProperties().getGeneratorSettings();

        SpongeWorldGenerator newGenerator = createWorldGenerator(generatorSettings);
        // If the base generator is an IChunkProvider which implements
        // IPopulatorProvider we request that it add its populators not covered
        // by the base generation populator
        if (newGenerator.getBaseGenerationPopulator() instanceof IChunkGenerator) {
            // We check here to ensure that the IPopulatorProvider is one of our mixed in ones and not
            // from a mod chunk provider extending a provider that we mixed into
            if (WorldGenConstants.isValid((IChunkGenerator) newGenerator.getBaseGenerationPopulator(), IPopulatorProvider.class)) {
                ((IPopulatorProvider) newGenerator.getBaseGenerationPopulator()).addPopulators(newGenerator);
            }
        } else if (newGenerator.getBaseGenerationPopulator() instanceof IPopulatorProvider) {
            // If its not a chunk provider but is a populator provider then we call it as well
            ((IPopulatorProvider) newGenerator.getBaseGenerationPopulator()).addPopulators(newGenerator);
        }

        // Re-apply all world generator modifiers
        WorldCreationSettings creationSettings = this.getCreationSettings();

        for (WorldGeneratorModifier modifier : this.getProperties().getGeneratorModifiers()) {
            modifier.modifyWorldGenerator(creationSettings, generatorSettings, newGenerator);
        }

        this.spongegen = createChunkGenerator(newGenerator);
        this.spongegen.setGenerationPopulators(newGenerator.getGenerationPopulators());
        this.spongegen.setPopulators(newGenerator.getPopulators());
        this.spongegen.setBiomeOverrides(newGenerator.getBiomeSettings());

        ChunkProviderServer chunkProviderServer = (ChunkProviderServer) this.getChunkProvider();
        chunkProviderServer.chunkGenerator = this.spongegen;
    }

    @Override
    public SpongeChunkGenerator createChunkGenerator(SpongeWorldGenerator newGenerator) {
        return new SpongeChunkGenerator((net.minecraft.world.World) (Object) this, newGenerator.getBaseGenerationPopulator(),
                newGenerator.getBiomeGenerator());
    }

    @Override
    public SpongeWorldGenerator createWorldGenerator(DataContainer settings) {
        // Minecraft uses a string for world generator settings
        // This string can be a JSON string, or be a string of a custom format

        // Try to convert to custom format
        Optional<String> optCustomSettings = settings.getString(DataQueries.WORLD_CUSTOM_SETTINGS);
        if (optCustomSettings.isPresent()) {
            return this.createWorldGenerator(optCustomSettings.get());
        }

        final StringWriter writer = new StringWriter();
        try {
            HoconConfigurationLoader.builder().setRenderOptions(ConfigRenderOptions.concise().setJson(true))
                    .setSink(() -> new BufferedWriter(writer)).build().save(ConfigurateTranslator.instance().translateData(settings));
        } catch (Exception e) {
            SpongeImpl.getLogger().warn("Failed to convert settings from [{}] for GeneratorType [{}] used by World [{}].", settings,
                    ((net.minecraft.world.World) (Object) this).getWorldType(), this, e);
        }

        return this.createWorldGenerator(writer.toString());
    }

    @Override
    public SpongeWorldGenerator createWorldGenerator(String settings) {
        final WorldServer worldServer = (WorldServer) (Object) this;
        final WorldType worldType = worldServer.getWorldType();
        final IChunkGenerator chunkGenerator;
        final BiomeProvider biomeProvider;
        if (worldType instanceof SpongeWorldType) {
            chunkGenerator = ((SpongeWorldType) worldType).getChunkGenerator(worldServer, settings);
            biomeProvider = ((SpongeWorldType) worldType).getBiomeProvider(worldServer);
        } else {
            final WorldProvider worldProvider = worldServer.provider;
            ((IMixinWorldProvider) worldProvider).setGeneratorSettings(settings);
            chunkGenerator = worldProvider.createChunkGenerator();
            biomeProvider = worldServer.provider.biomeProvider;
        }
        return new SpongeWorldGenerator(worldServer, (BiomeGenerator) biomeProvider, SpongeGenerationPopulator.of(worldServer, chunkGenerator));
    }

    @Override
    public WorldGenerator getWorldGenerator() {
        return this.spongegen;
    }

    @Override
    public CauseTracker getCauseTracker() {
        return this.causeTracker;
    }

    @Redirect(method = "updateBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;randomTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"))
    public void onUpdateBlocks(Block block, net.minecraft.world.World worldIn, BlockPos pos, IBlockState state, Random rand) {
        final CauseTracker causeTracker = this.getCauseTracker();
        final PhaseData currentTuple = causeTracker.getStack().peek();
        final IPhaseState phaseState = currentTuple.getState();
        if (phaseState.getPhase().alreadyCapturingBlockTicks(phaseState, currentTuple.getContext())) {
            block.randomTick(worldIn, pos, state, rand);
            return;
        }

        TrackingUtil.randomTickBlock(causeTracker, block, pos, state, rand);
    }

    @Redirect(method = "updateBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;isRainingAt(Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean onLightningCheck(WorldServer world, BlockPos blockPos) {
        if (world.isRainingAt(blockPos)) {
            Transform<org.spongepowered.api.world.World> transform = new Transform<>(this, VecHelper.toVector3d(blockPos).toDouble());
            SpawnCause cause = WeatherSpawnCause.builder().weather(this.getWeather()).type(SpawnTypes.WEATHER).build();
            ConstructEntityEvent.Pre event = SpongeEventFactory.createConstructEntityEventPre(Cause.of(NamedCause.source(cause)),
                    EntityTypes.LIGHTNING, transform);
            SpongeImpl.postEvent(event);
            return !event.isCancelled();
        }
        return false;
    }

    @Redirect(method = "updateBlockTick", at = @At(value = "INVOKE", target="Lnet/minecraft/block/Block;updateTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"))
    public void onUpdateBlockTick(Block block, net.minecraft.world.World worldIn, BlockPos pos, IBlockState state, Random rand) {
        this.onUpdateTick(block, worldIn, pos, state, rand);
    }

    @Redirect(method = "tickUpdates", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;updateTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"))
    public void onUpdateTick(Block block, net.minecraft.world.World worldIn, BlockPos pos, IBlockState state, Random rand) {
        final CauseTracker causeTracker = this.getCauseTracker();
        final PhaseData currentTuple = causeTracker.getStack().peek();
        final IPhaseState phaseState = currentTuple.getState();
        if (phaseState.getPhase().alreadyCapturingBlockTicks(phaseState, currentTuple.getContext())) {
            block.updateTick(worldIn, pos, state, rand);
            return;
        }
        TrackingUtil.updateTickBlock(causeTracker, block, pos, state, rand);
    }

    @Inject(method = "addBlockEvent", at = @At(value = "HEAD"))
    public void onAddBlockEvent(BlockPos pos, Block blockIn, int eventID, int eventParam, CallbackInfo ci) {
        final CauseTracker causeTracker = this.getCauseTracker();
        final PhaseData currentPhase = causeTracker.getStack().peek();
        final IPhaseState phaseState = currentPhase.getState();
        final PhaseContext context = currentPhase.getContext();
        if (phaseState.getPhase().ignoresBlockEvent(phaseState)) {
            return;
        }

        if (context.firstNamed(NamedCause.SOURCE, User.class).isPresent()) {
            // Add player to block event position
            markBlockEventUserNotification(pos, blockIn, pos);
        } else {
            Stream.<Supplier<Optional<BlockPos>>>of(
                    () -> context.firstNamed(NamedCause.SOURCE, BlockSnapshot.class).map(ticking -> VecHelper.toBlockPos(ticking.getPosition())),
                    () -> context.firstNamed(NamedCause.SOURCE, net.minecraft.tileentity.TileEntity.class).map(net.minecraft.tileentity.TileEntity::getPos))
                    // We use suppliers because we can lazy evaluate: i.e. if there's a ticking block, we won't query for a ticking tile entity
                    .map(Supplier::get)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .ifPresent(sourcePos -> markBlockEventUserNotification(pos, blockIn, sourcePos));
        }
    }

    private void markBlockEventUserNotification(BlockPos pos, Block blockIn, BlockPos sourcePos) {
        if (isBlockLoaded(sourcePos)) {
            IMixinChunk spongeChunk = (IMixinChunk) getChunkFromBlockCoords(sourcePos);
            Stream.<Supplier<Optional<User>>>of(() -> spongeChunk.getBlockOwner(sourcePos), () -> spongeChunk.getBlockNotifier(pos))
                    .map(Supplier::get)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .ifPresent(tracked -> {
                        this.trackedBlockEvents.put(pos, tracked);
                        spongeChunk.addTrackedBlockPosition(blockIn, pos, tracked, PlayerTracker.Type.NOTIFIER);
                    });
        }
    }

    // special handling for Pistons since they use their own event system
    @Redirect(method = "sendQueuedBlockEvents", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/WorldServer;fireBlockEvent(Lnet/minecraft/block/BlockEventData;)Z"))
    public boolean onFireBlockEvent(net.minecraft.world.WorldServer worldIn, BlockEventData event) {
        final CauseTracker causeTracker = this.getCauseTracker();
        final IPhaseState phaseState = causeTracker.getStack().peekState();
        if (phaseState.getPhase().ignoresBlockEvent(phaseState)) {
            return fireBlockEvent(event);
        }
        return TrackingUtil.fireMinecraftBlockEvent(causeTracker, worldIn, event, this.trackedBlockEvents);
    }

    @Redirect(method = "sendQueuedBlockEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/DimensionType;getId()I"))
    private int onGetDimensionIdForBlockEvents(DimensionType dimensionType) {
        return this.getDimensionId();
    }

    @Override
    public Collection<ScheduledBlockUpdate> getScheduledUpdates(int x, int y, int z) {
        BlockPos position = new BlockPos(x, y, z);
        ImmutableList.Builder<ScheduledBlockUpdate> builder = ImmutableList.builder();
        for (NextTickListEntry sbu : this.pendingTickListEntriesTreeSet) {
            if (sbu.position.equals(position)) {
                builder.add((ScheduledBlockUpdate) sbu);
            }
        }
        return builder.build();
    }

    @Nullable
    private NextTickListEntry tmpScheduledObj;

    @Redirect(method = "updateBlockTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/NextTickListEntry;setPriority(I)V"))
    private void onUpdateScheduledBlock(NextTickListEntry sbu, int priority) {
        this.onCreateScheduledBlockUpdate(sbu, priority);
    }

    @Redirect(method = "scheduleBlockUpdate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/NextTickListEntry;setPriority(I)V"))
    private void onCreateScheduledBlockUpdate(NextTickListEntry sbu, int priority) {
        final CauseTracker causeTracker = this.getCauseTracker();
        final IPhaseState phaseState = causeTracker.getStack().peekState();

        if (phaseState.getPhase().ignoresScheduledUpdates(phaseState)) {
            this.tmpScheduledObj = sbu;
            return;
        }

        sbu.setPriority(priority);
        ((IMixinBlockUpdate) sbu).setWorld((WorldServer) (Object) this);
        if (!((net.minecraft.world.World)(Object) this).isBlockLoaded(sbu.position)) {
            this.tmpScheduledObj = sbu;
            return;
        }

        final PhaseContext context = causeTracker.getStack().peekContext();
        if (context != null) {
            Optional<BlockSnapshot> currentTickingBlock = context.firstNamed(NamedCause.SOURCE, BlockSnapshot.class);

            // Pistons, Beacons, Notes, Comparators etc. schedule block updates so we must track these positions
            if (currentTickingBlock.isPresent()) {
                BlockPos pos = VecHelper.toBlockPos(currentTickingBlock.get().getPosition());
                SpongeHooks.tryToTrackBlock((net.minecraft.world.World) (Object) this, currentTickingBlock.get(), pos, sbu.getBlock(),
                        sbu.position, PlayerTracker.Type.NOTIFIER);
            }

        }

        this.tmpScheduledObj = sbu;
    }

    @Override
    public ScheduledBlockUpdate addScheduledUpdate(int x, int y, int z, int priority, int ticks) {
        BlockPos pos = new BlockPos(x, y, z);
        this.scheduleBlockUpdate(pos, getBlockState(pos).getBlock(), ticks, priority);
        ScheduledBlockUpdate sbu = (ScheduledBlockUpdate) this.tmpScheduledObj;
        this.tmpScheduledObj = null;
        return sbu;
    }

    @Override
    public void removeScheduledUpdate(int x, int y, int z, ScheduledBlockUpdate update) {
        // Note: Ignores position argument
        this.pendingTickListEntriesHashSet.remove(update);
        this.pendingTickListEntriesTreeSet.remove(update);
    }

    @Redirect(method = "updateAllPlayersSleepingFlag()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayer;isSpectator()Z"))
    public boolean isSpectatorOrIgnored(EntityPlayer entityPlayer) {
        // spectators are excluded from the sleep tally in vanilla
        // this redirect expands that check to include sleep-ignored players as well
        boolean ignore = entityPlayer instanceof Player && ((Player)entityPlayer).isSleepingIgnored();
        return ignore || entityPlayer.isSpectator();
    }

    @Redirect(method = "areAllPlayersAsleep()Z", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayer;isPlayerFullyAsleep()Z"))
    public boolean isPlayerFullyAsleep(EntityPlayer entityPlayer) {
        // if isPlayerFullyAsleep() returns false areAllPlayerAsleep() breaks its loop and returns false
        // this redirect forces it to return true if the player is sleep-ignored even if they're not sleeping
        boolean ignore = entityPlayer instanceof Player && ((Player)entityPlayer).isSleepingIgnored();
        return ignore || entityPlayer.isPlayerFullyAsleep();
    }

    @Redirect(method = "areAllPlayersAsleep()Z", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayer;isSpectator()Z"))
    public boolean isSpectatorAndNotIgnored(EntityPlayer entityPlayer) {
        // if a player is marked as a spectator areAllPlayersAsleep() breaks its loop and returns false
        // this redirect forces it to return false if a player is sleep-ignored even if they're a spectator
        boolean ignore = entityPlayer instanceof Player && ((Player)entityPlayer).isSleepingIgnored();
        return !ignore && entityPlayer.isSpectator();
    }

    @Override
    public Optional<Entity> getEntity(UUID uuid) {
        return Optional.ofNullable((Entity) this.getEntityFromUuid(uuid));
    }


    @Override
    public void setBlock(int x, int y, int z, BlockState block, boolean notifyNeighbors) {
        checkBlockBounds(x, y, z);
        setBlockState(new BlockPos(x, y, z), (IBlockState) block, notifyNeighbors ? 3 : 2);
    }

    @Override
    public void setBlock(int x, int y, int z, BlockState blockState, boolean notifyNeighbors, Cause cause) {
        checkArgument(cause != null, "Cause cannot be null!");
        checkArgument(cause.root() instanceof PluginContainer, "PluginContainer must be at the ROOT of a cause!");
        final CauseTracker causeTracker = this.getCauseTracker();
        checkBlockBounds(x, y, z);
        final PhaseContext context = PhaseContext.start()
                .add(NamedCause.of(InternalNamedCauses.General.PLUGIN_CAUSE, cause))
                .addCaptures()
                .add(NamedCause.source(cause.root()));
        for (Map.Entry<String, Object> entry : cause.getNamedCauses().entrySet()) {
            context.add(NamedCause.of(entry.getKey(), entry.getValue()));
        }
        context.complete();
        causeTracker.switchToPhase(TrackingPhases.PLUGIN, PluginPhase.State.BLOCK_WORKER, context);
        setBlockState(new BlockPos(x, y, z), (IBlockState) blockState, notifyNeighbors ? 3 : 2);
        causeTracker.completePhase();
    }

    private void checkBlockBounds(int x, int y, int z) {
        if (!containsBlock(x, y, z)) {
            throw new PositionOutOfBoundsException(new Vector3i(x, y, z), BLOCK_MIN, BLOCK_MAX);
        }
    }

    @Override
    public boolean spawnEntities(Iterable<? extends Entity> entities, Cause cause) {
        checkArgument(cause != null, "Cause cannot be null!");
        checkArgument(cause.root() instanceof PluginContainer, "PluginContainer must be at the ROOT of a cause!");
        List<Entity> entitiesToSpawn = new ArrayList<>();
        entities.forEach(entitiesToSpawn::add);
        return !EventConsumer.event(SpongeEventFactory.createSpawnEntityEventCustom(cause, entitiesToSpawn, this))
                .nonCancelled(event -> EntityListConsumer.FORCE_SPAWN.apply(event.getEntities(), this.getCauseTracker()))
                .process()
                .isCancelled();
    }

    /**
     * @author gabizou - April 24th, 2016
     * @reason Needs to redirect the dimension id for the packet being sent to players
     * so that the dimension is correctly adjusted
     *
     * @param id The world provider's dimension id
     * @return True if the spawn was successful and the effect is played.
     */
    @Redirect(method = "addWeatherEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/DimensionType;getId()I"))
    public int getDimensionIdForWeatherEffect(DimensionType id) {
        return this.getDimensionId();
    }

    // ------------------------- Start Cause Tracking overrides of Minecraft World methods ----------

    /**
     * @author gabizou March 11, 2016
     *
     * The train of thought for how spawning is handled:
     * 1) This method is called in implementation
     * 2) handleVanillaSpawnEntity is called to associate various contextual SpawnCauses
     * 3) {@link CauseTracker#spawnEntity(Entity)} is called to check if the entity is to
     *    be "collected" or "captured" in the current {@link PhaseContext} of the current phase
     * 4) If the entity is forced or is captured, {@code true} is returned, otherwise, the entity is
     *    passed along normal spawning handling.
     */
    @Override
    public boolean spawnEntityInWorld(net.minecraft.entity.Entity entity) {
        return getCauseTracker().spawnEntity(EntityUtil.fromNative(entity));
    }


    /**
     * @author gabizou, March 12th, 2016
     *
     * Move this into WorldServer as we should not be modifying the client world.
     *
     * Purpose: Rewritten to support capturing blocks
     */
    @Override
    public boolean setBlockState(BlockPos pos, IBlockState newState, int flags) {
        if (!this.isValid(pos)) {
            return false;
        } else if (!this.isRemote && this.worldInfo.getTerrainType() == WorldType.DEBUG_WORLD) {
            return false;
        } else {
            // Sponge - reroute to the CauseTracker
            return this.getCauseTracker().setBlockState(pos, newState, flags);
        }
    }


    /**
     * @author gabizou - March 12th, 2016
     *
     * Technically an overwrite to properly track on *server* worlds.
     */
    @Override
    public void forceBlockUpdateTick(Block blockType, BlockPos pos, Random random) {
        this.scheduledUpdatesAreImmediate = true;
        // Sponge start - Cause tracking
        final PhaseData peek = this.causeTracker.getStack().peek();
        if (this.isRemote || peek.getState().getPhase().ignoresBlockUpdateTick(peek)) {
            blockType.updateTick((World) (Object) this, pos, this.getBlockState(pos), random);
            return;
        }
        TrackingUtil.updateTickBlock(this.causeTracker, blockType, pos, this.getBlockState(pos), random);
        // Sponge end
        this.scheduledUpdatesAreImmediate = false;
    }

    /**
     * @author gabizou - March 12th, 2016
     *
     * Technically an overwrite to properly track on *server* worlds.
     */
    @Override
    public void updateComparatorOutputLevel(BlockPos pos, Block blockIn) {
        SpongeImplHooks.updateComparatorOutputLevel((net.minecraft.world.World) (Object) this, pos, blockIn);
    }

    /**
     * @author gabizou - March 12th, 2016
     *
     * Technically an overwrite to properly track on *server* worlds.
     */
    @Override
    public void notifyBlockOfStateChange(BlockPos pos, Block blockIn) {
        this.getCauseTracker().notifyBlockOfStateChange(pos, blockIn, null);
    }

    /**
     * @author gabizou - March 12th, 2016
     *
     * Technically an overwrite to properly track on *server* worlds.
     */
    @Override
    public void notifyNeighborsOfStateExcept(BlockPos pos, Block blockType, EnumFacing skipSide) {
        if (!isValid(pos)) {
            return;
        }

        EnumSet<EnumFacing> directions = EnumSet.allOf(EnumFacing.class);
        directions.remove(skipSide);

        final CauseTracker causeTracker = this.getCauseTracker();

        EventConsumer.event(SpongeCommonEventFactory.callNotifyNeighborEvent(this, pos, directions))
                .nonCancelled(event -> {
                    for (EnumFacing facing : EnumFacing.values()) {
                        final Direction direction = DirectionFacingProvider.getInstance().getKey(facing).get();
                        if (event.getNeighbors().keySet().contains(direction)) {
                            causeTracker.notifyBlockOfStateChange(pos.offset(facing), blockType, pos);
                        }
                    }
                })
                .process();

    }

    /**
     * @author gabizou - March 12th, 2016
     *
     * Technically an overwrite to properly track on *server* worlds.
     */
    @Override
    public void notifyNeighborsOfStateChange(BlockPos pos, Block blockType) {
        if (!isValid(pos)) {
            return;
        }

        final CauseTracker causeTracker = this.getCauseTracker();

        EventConsumer.event(SpongeCommonEventFactory.callNotifyNeighborEvent(this, pos, EnumSet.allOf(EnumFacing.class)))
                .nonCancelled(event -> {
                    for (EnumFacing facing : EnumFacing.values()) {
                        final Direction direction = DirectionFacingProvider.getInstance().getKey(facing).get();
                        if (event.getNeighbors().keySet().contains(direction)) {
                            causeTracker.notifyBlockOfStateChange(pos.offset(facing), blockType, pos);
                        }
                    }
                })
                .process();
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void onUpdateWeatherEffect(net.minecraft.entity.Entity entityIn) {
        final CauseTracker causeTracker = this.getCauseTracker();
        final IPhaseState state = causeTracker.getStack().peekState();
        if (state.getPhase().alreadyCapturingEntityTicks(state)) {
            entityIn.onUpdate();
            return;
        }
        TrackingUtil.tickEntity(causeTracker, entityIn);
        updateRotation(entityIn);
    }

    @Override
    protected void onUpdateTileEntities(ITickable tile) {
        final CauseTracker causeTracker = this.getCauseTracker();
        final IPhaseState state = causeTracker.getStack().peekState();
        if (state.getPhase().alreadyCapturingTileTicks(state)) {
            tile.update();
            return;
        }

        TrackingUtil.tickTileEntity(causeTracker, tile);
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void onCallEntityUpdate(net.minecraft.entity.Entity entity) {
        final CauseTracker causeTracker = this.getCauseTracker();
        final IPhaseState state = causeTracker.getStack().peekState();
        if (state.getPhase().alreadyCapturingEntityTicks(state)) {
            entity.onUpdate();
            return;
        }

        TrackingUtil.tickEntity(causeTracker, entity);
        updateRotation(entity);
    }

    // ------------------------ End of Cause Tracking ------------------------------------

    // IMixinWorld method
    @Override
    public void markAndNotifyNeighbors(BlockPos pos, @Nullable net.minecraft.world.chunk.Chunk chunk, IBlockState oldState, IBlockState newState, int flags) {
        if ((flags & 2) != 0 && (!this.isRemote || (flags & 4) == 0) && (chunk == null || chunk.isPopulated())) {
            this.notifyBlockUpdate(pos, oldState, newState, flags);
        }

        if (!this.isRemote && (flags & 1) != 0) {
            this.notifyNeighborsRespectDebug(pos, newState.getBlock());

            if (newState.getBlock().hasComparatorInputOverride(newState)) {
                this.updateComparatorOutputLevel(pos, newState.getBlock());
            }
        }
    }

    // IMixinWorld method
    @Override
    public void addEntityRotationUpdate(net.minecraft.entity.Entity entity, Vector3d rotation) {
        this.rotationUpdates.put(entity, rotation);
    }

    // IMixinWorld method
    @Override
    public void updateRotation(net.minecraft.entity.Entity entityIn) {
        Vector3d rotationUpdate = this.rotationUpdates.get(entityIn);
        if (rotationUpdate != null) {
            entityIn.rotationPitch = (float) rotationUpdate.getX();
            entityIn.rotationYaw = (float) rotationUpdate.getY();
        }
        this.rotationUpdates.remove(entityIn);
    }


    @Override
    public void onSpongeEntityAdded(net.minecraft.entity.Entity entity) {
        this.onEntityAdded(entity);
    }

    @Override
    public void onSpongeEntityRemoved(net.minecraft.entity.Entity entity) {
        this.onEntityRemoved(entity);
    }

    @Override
    public boolean spawnEntity(Entity entity, Cause cause) {
        final CauseTracker causeTracker = this.getCauseTracker();
        final IPhaseState state = causeTracker.getStack().peekState();
        if (!state.getPhase().alreadyCapturingEntitySpawns(state)) {
            causeTracker.switchToPhase(TrackingPhases.PLUGIN, PluginPhase.State.CUSTOM_SPAWN, PhaseContext.start()
                .add(NamedCause.source(cause))
                .addCaptures()
                .complete());
            causeTracker.spawnEntityWithCause(entity, cause);
            causeTracker.completePhase();
            return true;
        }
        return causeTracker.spawnEntityWithCause(entity, cause);
    }

    @Override
    public boolean forceSpawnEntity(Entity entity) {
        final net.minecraft.entity.Entity minecraftEntity = (net.minecraft.entity.Entity) entity;
        final int x = minecraftEntity.getPosition().getX();
        final int z = minecraftEntity.getPosition().getZ();
        return forceSpawnEntity(minecraftEntity, x >> 4, z >> 4);
    }

    private boolean forceSpawnEntity(net.minecraft.entity.Entity entity, int chunkX, int chunkZ) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer entityplayer = (EntityPlayer) entity;
            this.playerEntities.add(entityplayer);
            this.updateAllPlayersSleepingFlag();
        }

        if (entity instanceof EntityLightningBolt) {
            this.addWeatherEffect(entity);
            return true;
        }

        this.getChunkFromChunkCoords(chunkX, chunkZ).addEntity(entity);
        this.loadedEntityList.add(entity);
        this.onSpongeEntityAdded(entity);
        return true;
    }

    @Override
    public SpongeBlockSnapshot createSpongeBlockSnapshot(IBlockState state, IBlockState extended, BlockPos pos, int updateFlag) {
        this.builder.reset();
        Location<org.spongepowered.api.world.World> location = new Location<>(this, VecHelper.toVector3i(pos));
        this.builder.blockState((BlockState) state)
                .extendedState((BlockState) extended)
                .worldId(location.getExtent().getUniqueId())
                .position(location.getBlockPosition());
        Optional<UUID> creator = getCreator(pos.getX(), pos.getY(), pos.getZ());
        Optional<UUID> notifier = getNotifier(pos.getX(), pos.getY(), pos.getZ());
        if (creator.isPresent()) {
            this.builder.creator(creator.get());
        }
        if (notifier.isPresent()) {
            this.builder.notifier(notifier.get());
        }
        if (state.getBlock() instanceof ITileEntityProvider) {
            net.minecraft.tileentity.TileEntity te = getTileEntity(pos);
            if (te != null) {
                TileEntity tile = (TileEntity) te;
                for (DataManipulator<?, ?> manipulator : tile.getContainers()) {
                    this.builder.add(manipulator);
                }
                NBTTagCompound nbt = new NBTTagCompound();
                te.writeToNBT(nbt);
                this.builder.unsafeNbt(nbt);
            }
        }
        return new SpongeBlockSnapshot(this.builder, updateFlag);
    }


    @Inject(method = "newExplosion", at = @At(value = "HEAD"))
    public void onExplosionHead(net.minecraft.entity.Entity entityIn, double x, double y, double z, float strength, boolean isFlaming, boolean isSmoking, CallbackInfoReturnable<net.minecraft.world.Explosion> cir) {
        this.processingExplosion = true;
    }

    @Inject(method = "newExplosion", at = @At(value = "RETURN"))
    public void onExplosionReturn(net.minecraft.entity.Entity entityIn, double x, double y, double z, float strength, boolean isFlaming, boolean isSmoking, CallbackInfoReturnable<net.minecraft.world.Explosion> cir) {
        this.processingExplosion = false;
    }

    /**
     * @author amaranth - April 25th, 2016
     * @reason Avoid 25 chunk map lookups per entity per tick by using neighbor pointers
     *
     * @param xStart X block start coordinate
     * @param yStart Y block start coordinate
     * @param zStart Z block start coordinate
     * @param xEnd X block end coordinate
     * @param yEnd Y block end coordinate
     * @param zEnd Z block end coordinate
     * @param allowEmpty Whether empty chunks should be accepted
     * @return If the chunks for the area are loaded
     */
    @Override
    public boolean isAreaLoaded(int xStart, int yStart, int zStart, int xEnd, int yEnd, int zEnd, boolean allowEmpty) {
        if (yEnd < 0 || yStart > 255) {
            return false;
        }

        xStart = xStart >> 4;
        zStart = zStart >> 4;
        xEnd = xEnd >> 4;
        zEnd = zEnd >> 4;

        Chunk base = (Chunk) ((IMixinChunkProviderServer) this.getChunkProvider()).getChunkIfLoaded(xStart, zStart);
        if (base == null) {
            return false;
        }

        for (int i = xStart; i <= xEnd; i++) {
            Optional<Chunk> column = base.getNeighbor(Direction.EAST);
            if (!column.isPresent()) {
                return false;
            }

            Chunk unwrapped = column.get();
            for (int j = zStart; j <= zEnd; j++) {
                Optional<Chunk> row = unwrapped.getNeighbor(Direction.SOUTH);
                if (!row.isPresent()) {
                    return false;
                }

                // This is redundant
                if (!allowEmpty && ((net.minecraft.world.chunk.Chunk) row.get()).isEmpty()) {
                    return false;
                }
            }
        }

        return true;

    }
}
