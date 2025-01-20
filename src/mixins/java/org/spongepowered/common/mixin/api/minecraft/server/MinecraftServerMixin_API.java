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
package org.spongepowered.common.mixin.api.minecraft.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.datafixers.DataFixer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.item.recipe.RecipeManager;
import org.spongepowered.api.map.MapStorage;
import org.spongepowered.api.profile.GameProfileManager;
import org.spongepowered.api.resource.ResourceManager;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.service.ServiceProvider;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.generation.config.WorldGenerationConfig;
import org.spongepowered.api.world.server.DataPackManager;
import org.spongepowered.api.world.storage.ChunkLayout;
import org.spongepowered.api.world.teleport.TeleportHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.SpongeServer;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.commands.CommandsBridge;
import org.spongepowered.common.bridge.server.MinecraftServerBridge;
import org.spongepowered.common.command.manager.SpongeCommandManager;
import org.spongepowered.common.command.sponge.SpongeCommand;
import org.spongepowered.common.datapack.SpongeDataPackManager;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.map.SpongeMapStorage;
import org.spongepowered.common.profile.SpongeGameProfileManager;
import org.spongepowered.common.registry.RegistryHolderLogic;
import org.spongepowered.common.registry.SpongeRegistryHolder;
import org.spongepowered.common.scheduler.ServerScheduler;
import org.spongepowered.common.user.SpongeUserManager;
import org.spongepowered.common.util.BlockDestructionIdCache;
import org.spongepowered.common.util.UsernameCache;
import org.spongepowered.common.world.server.SpongeWorldManager;
import org.spongepowered.common.world.storage.SpongeChunkLayout;
import org.spongepowered.common.world.storage.SpongePlayerDataManager;
import org.spongepowered.common.world.teleport.SpongeTeleportHelper;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@SuppressWarnings("rawtypes")
@Mixin(MinecraftServer.class)
@Implements(value = @Interface(iface = Server.class, prefix = "server$", remap = Interface.Remap.NONE))
public abstract class MinecraftServerMixin_API implements SpongeServer, SpongeRegistryHolder {

    // @formatter:off
    @Shadow @Final public long[] tickTimesNanos;
    @Shadow @Final protected WorldData worldData;

    @Shadow public abstract net.minecraft.world.item.crafting.RecipeManager shadow$getRecipeManager();
    @Shadow public abstract PlayerList shadow$getPlayerList();
    @Shadow public abstract boolean shadow$usesAuthentication();
    @Shadow public abstract String shadow$getMotd();
    @Shadow public abstract int shadow$getTickCount();
    @Shadow public abstract void shadow$halt(boolean p_71263_1_);
    @Shadow public abstract int shadow$getPlayerIdleTimeout();
    @Shadow public abstract void shadow$setPlayerIdleTimeout(int p_143006_1_);
    @Shadow public abstract boolean shadow$isHardcore();
    @Shadow public abstract boolean shadow$isPvpAllowed();
    @Shadow public abstract boolean shadow$isCommandBlockEnabled();
    @Shadow public abstract boolean shadow$isSpawningMonsters();
    @Shadow public abstract boolean shadow$isSpawningAnimals();
    @Shadow public abstract Commands shadow$getCommands();
    @Shadow public abstract PackRepository shadow$getPackRepository();
    @Shadow public abstract net.minecraft.server.packs.resources.ResourceManager shadow$getResourceManager();
    @Shadow public abstract WorldData shadow$getWorldData();
    // @formatter:on


    @Shadow @Final protected LevelStorageSource.LevelStorageAccess storageSource;

    @Shadow public abstract RegistryAccess.Frozen registryAccess();

    @Shadow public abstract boolean repliesToStatus();

    @Shadow public abstract boolean isSingleplayer();

    private Iterable<? extends Audience> audiences;
    private ServerScheduler api$scheduler;
    private SpongeTeleportHelper api$teleportHelper;
    private SpongePlayerDataManager api$playerDataHandler;
    private UsernameCache api$usernameCache;
    private Audience api$broadcastAudience;
    private ServerScoreboard api$scoreboard;
    private SpongeGameProfileManager api$profileManager;
    private MapStorage api$mapStorage;
    private RegistryHolderLogic api$registryHolder;
    private SpongeUserManager api$userManager;
    private SpongeDataPackManager api$dataPackManager;
    private final BlockDestructionIdCache api$blockDestructionIdCache = new BlockDestructionIdCache(0, AtomicInteger::decrementAndGet);

    @Inject(method = "<init>", at = @At("TAIL"))
    public void api$initializeSpongeFieldsfinal(final Thread $$0, final LevelStorageSource.LevelStorageAccess $$1, final PackRepository $$2, final WorldStem $$3, final Proxy $$4,
            final DataFixer $$5, final Services $$6, final ChunkProgressListenerFactory $$7, final CallbackInfo ci) {
        this.api$scheduler = new ServerScheduler();
        this.api$playerDataHandler = new SpongePlayerDataManager(this);
        this.api$teleportHelper = new SpongeTeleportHelper();
        this.api$mapStorage = new SpongeMapStorage();
        this.api$registryHolder = new RegistryHolderLogic($$3.dataPackResources().fullRegistries().get());
        this.api$userManager = new SpongeUserManager((MinecraftServer) (Object) this);

        this.api$dataPackManager = new SpongeDataPackManager((MinecraftServer) (Object) this, this.storageSource.getLevelPath(LevelResource.DATAPACK_DIR));
    }

    @Override
    public RecipeManager recipeManager() {
        return (RecipeManager) this.shadow$getRecipeManager();
    }

    @Override
    public DataPackManager dataPackManager() {
        return this.api$dataPackManager;
    }

    @Override
    public @NonNull Iterable<? extends Audience> audiences() {
        if (this.audiences == null) {
            this.audiences = Iterables.concat((List) this.shadow$getPlayerList().getPlayers(), Collections.singleton(Sponge.game().systemSubject()));
        }
        return this.audiences;
    }

    @Override
    public ChunkLayout chunkLayout() {
        return SpongeChunkLayout.INSTANCE;
    }

    @Override
    public Audience broadcastAudience() {
        if (this.api$broadcastAudience == null) {
            this.api$broadcastAudience = this;
        }

        return this.api$broadcastAudience;
    }

    @Override
    public void setBroadcastAudience(final Audience channel) {
        this.api$broadcastAudience = Objects.requireNonNull(channel, "channel");
    }

    @Override
    public Optional<InetSocketAddress> boundAddress() {
        return Optional.empty();
    }

    @Override
    public boolean isWhitelistEnabled() {
        return this.shadow$getPlayerList().isUsingWhitelist();
    }

    @Override
    public void setHasWhitelist(final boolean enabled) {
        this.shadow$getPlayerList().setUsingWhiteList(enabled);
    }

    @Override
    public boolean isOnlineModeEnabled() {
        return this.shadow$usesAuthentication();
    }

    @Override
    public boolean isHardcoreModeEnabled() {
        return this.shadow$isHardcore();
    }

    @Override
    public Difficulty difficulty() {
        return (Difficulty) (Object) this.worldData.getDifficulty();
    }

    @Override
    public GameMode gameMode() {
        return (GameMode) (Object) this.worldData.getGameType();
    }

    @Override
    public boolean isGameModeEnforced() {
        throw new UnsupportedOperationException("Seemingly this disappeared from being possible???");
    }

    @Override
    public boolean isPVPEnabled() {
        return this.shadow$isPvpAllowed();
    }

    @Override
    public boolean areCommandBlocksEnabled() {
        return this.shadow$isCommandBlockEnabled();
    }

    @Override
    public boolean isMonsterSpawnsEnabled() {
        return this.shadow$isSpawningMonsters();
    }

    @Override
    public boolean isAnimalSpawnsEnabled() {
        return this.shadow$isSpawningAnimals();
    }

    /**
     * See {@link SpongeWorldManager#loadLevel()}
     */
    @Override
    public boolean isMultiWorldEnabled() {
        return this.isSingleplayer() || ((Object) this instanceof DedicatedServer ds) && ds.getProperties().allowNether;
    }

    @Override
    public WorldGenerationConfig worldGenerationConfig() {
        final WorldData overworldData = this.shadow$getWorldData();

        final WorldGenSettings settings = new WorldGenSettings(overworldData.worldGenOptions(),
                new WorldDimensions(this.registryAccess().registryOrThrow(Registries.LEVEL_STEM)));

        return (WorldGenerationConfig) (Object) settings;
    }

    @Override
    public SpongeUserManager userManager() {
        return this.api$userManager;
    }

    @Override public TeleportHelper teleportHelper() {
        return this.api$teleportHelper;
    }

    @Override
    public Stream<ServerPlayer> streamOnlinePlayers() {
        if (this.shadow$getPlayerList() == null || this.shadow$getPlayerList().getPlayers() == null) {
            return Stream.empty();
        }
        return (Stream) this.shadow$getPlayerList().getPlayers().stream();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Collection<ServerPlayer> onlinePlayers() {
        if (this.shadow$getPlayerList() == null || this.shadow$getPlayerList().getPlayers() == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf((List) this.shadow$getPlayerList().getPlayers());
    }

    @Override
    public Optional<ServerPlayer> player(final UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        if (this.shadow$getPlayerList() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((ServerPlayer) this.shadow$getPlayerList().getPlayer(uniqueId));
    }

    @Override
    public Optional<ServerPlayer> player(final String name) {
        if (this.shadow$getPlayerList() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((ServerPlayer) this.shadow$getPlayerList().getPlayerByName(name));
    }

    @Override
    public Component motd() {
        return LegacyComponentSerializer.legacySection().deserialize(this.shadow$getMotd());
    }

    @Override
    public int maxPlayers() {
        if (this.shadow$getPlayerList() == null) {
            return 0;
        }
        return this.shadow$getPlayerList().getMaxPlayers();
    }

    @Override
    public @NonNull Ticks runningTimeTicks() {
        return Ticks.of(this.shadow$getTickCount());
    }

    @Override
    public double ticksPerSecond() {
        // Cap at 20 TPS
        return 1000 / Math.max(50, this.averageTickTime());
    }

    @Override
    public double averageTickTime() {
        return SpongeCommand.getAverage(this.tickTimesNanos) / 1000000;
    }

    @Override
    public int targetTicksPerSecond() {
        return 20;
    }

    @Override
    public void shutdown() {
        this.shadow$halt(false);
    }

    @Override
    public void shutdown(final Component kickMessage) {
        Objects.requireNonNull(kickMessage, "kickMessage");

        for (final var player : new ArrayList<>(this.shadow$getPlayerList().getPlayers())) {
            ((ServerPlayer) player).kick(kickMessage);
        }

        this.shadow$halt(false);
    }

    @Override
    public GameProfileManager gameProfileManager() {
        if (this.api$profileManager == null) {
            this.api$profileManager = new SpongeGameProfileManager(this);
        }

        return this.api$profileManager;
    }

    @Override
    public SpongeGameProfileManager gameProfileManagerIfPresent() {
        return this.api$profileManager;
    }

    @Override
    public SpongeCommandManager commandManager() {
        return ((CommandsBridge) this.shadow$getCommands()).bridge$commandManager();
    }

    public Optional<ResourcePackRequest> server$resourcePack() {
        return Optional.ofNullable(((MinecraftServerBridge) this).bridge$getResourcePack());
    }

    @Override
    public Optional<Scoreboard> serverScoreboard() {
        if (this.api$scoreboard == null) {
            final ServerLevel world = SpongeCommon.server().overworld();
            if (world == null) {
                return Optional.empty();
            }
            this.api$scoreboard = world.getScoreboard();
        }

        return Optional.of((Scoreboard) this.api$scoreboard);
    }

    @Intrinsic
    public int server$playerIdleTimeout() {
        return this.shadow$getPlayerIdleTimeout();
    }

    @Intrinsic
    public void server$setPlayerIdleTimeout(final int timeout) {
        this.shadow$setPlayerIdleTimeout(timeout);
    }

    @Override
    public Game game() {
        return Sponge.game();
    }

    @Override
    public CauseStackManager causeStackManager() {
        return PhaseTracker.getCauseStackManager();
    }

    @Override
    public org.spongepowered.api.resource.pack.PackRepository packRepository() {
        return (org.spongepowered.api.resource.pack.PackRepository) this.shadow$getPackRepository();
    }

    @Override
    public ResourceManager resourceManager() {
        return (ResourceManager) this.shadow$getResourceManager();
    }

    @Override
    public ServerScheduler scheduler() {
        return this.api$scheduler;
    }

    @Override
    public boolean onMainThread() {
        return ((MinecraftServer) (Object) this).isSameThread();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public SpongePlayerDataManager getPlayerDataManager() {
        return this.api$playerDataHandler;
    }

    @Override
    public UsernameCache getUsernameCache() {
        if (this.api$usernameCache == null) {
            this.api$usernameCache = new UsernameCache(this);
        }

        return this.api$usernameCache;
    }

    @Override
    public BlockDestructionIdCache getBlockDestructionIdCache() {
        return this.api$blockDestructionIdCache;
    }

    @Override
    public void sendMessage(final Identity identity, final Component message, final MessageType type) {
        this.shadow$getPlayerList().broadcastSystemMessage(SpongeAdventure.asVanilla(message), false);
    }

    @Override
    public ServiceProvider.ServerScoped serviceProvider() {
        return ((MinecraftServerBridge) this).bridge$getServiceProvider();
    }

    @Override
    public MapStorage mapStorage() {
        return this.api$mapStorage;
    }

    @Override
    public RegistryHolderLogic registryHolder() {
        return this.api$registryHolder;
    }
}
