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
package org.spongepowered.common.world.server;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.TimeUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.CatSpawner;
import net.minecraft.world.entity.npc.WanderingTraderSpawner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.storage.CommandStorage;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.datapack.DataPack;
import org.spongepowered.api.datapack.DataPackTypes;
import org.spongepowered.api.datapack.DataPacks;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.file.DeleteFileVisitor;
import org.spongepowered.api.world.DefaultWorldKeys;
import org.spongepowered.api.world.WorldType;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.WorldManager;
import org.spongepowered.api.world.server.WorldTemplate;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.server.MinecraftServerAccessor;
import org.spongepowered.common.accessor.world.level.storage.LevelStorageSource_LevelStorageAccessAccessor;
import org.spongepowered.common.bridge.ResourceKeyBridge;
import org.spongepowered.common.bridge.core.MappedRegistryBridge;
import org.spongepowered.common.bridge.server.level.ServerLevelBridge;
import org.spongepowered.common.bridge.world.level.chunk.storage.IOWorkerBridge;
import org.spongepowered.common.bridge.world.level.dimension.LevelStemBridge;
import org.spongepowered.common.bridge.world.level.levelgen.WorldOptionsBridge;
import org.spongepowered.common.bridge.world.level.storage.PrimaryLevelDataBridge;
import org.spongepowered.common.config.SpongeGameConfigs;
import org.spongepowered.common.config.inheritable.InheritableConfigHandle;
import org.spongepowered.common.config.inheritable.WorldConfig;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.generation.GenerationPhase;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.launch.Launch;
import org.spongepowered.common.user.SpongeUserManager;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.FutureUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class SpongeWorldManager implements WorldManager {

    private final MinecraftServer server;
    private final Path defaultWorldDirectory, customWorldsDirectory;
    private final Map<net.minecraft.resources.ResourceKey<Level>, ServerLevel> worlds;

    // TODO - pending https://github.com/SpongePowered/SpongeAPI/issues/2574 - 25w02a
    private static final TicketType SPAWN_CHUNKS = TicketType.FORCED;

    public SpongeWorldManager(final MinecraftServer server) {
        this.server = server;
        this.defaultWorldDirectory = ((LevelStorageSource_LevelStorageAccessAccessor) ((MinecraftServerAccessor) this.server).accessor$storageSource()).accessor$levelDirectory().path();
        this.customWorldsDirectory = this.defaultWorldDirectory.resolve("dimensions");
        try {
            Files.createDirectories(this.customWorldsDirectory);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        this.worlds = ((MinecraftServerAccessor) this.server).accessor$levels();
    }

    @Override
    public Server server() {
        return (Server) this.server;
    }

    public Path getDefaultWorldDirectory() {
        return this.defaultWorldDirectory;
    }

    @Override
    public Optional<ServerWorld> world(final ResourceKey key) {
        return Optional.ofNullable((ServerWorld) this.worlds.get(SpongeWorldManager.createRegistryKey(Objects
                .requireNonNull(key, "key"))));
    }

    @Override
    public Path worldDirectory(final ResourceKey key) {
        Objects.requireNonNull(key, "key");
        return this.getDirectory(key);
    }

    @Override
    public Collection<ServerWorld> worlds() {
        return Collections.unmodifiableCollection((Collection<ServerWorld>) (Object) this.worlds.values());
    }

    @Override
    public List<ResourceKey> worldKeys() {
        final List<ResourceKey> worldKeys = new ArrayList<>();
        worldKeys.add(DefaultWorldKeys.DEFAULT);

        if (Files.exists(this.getDirectory(DefaultWorldKeys.THE_NETHER))) {
            worldKeys.add(DefaultWorldKeys.THE_NETHER);
        }

        if (Files.exists(this.getDirectory(DefaultWorldKeys.THE_END))) {
            worldKeys.add(DefaultWorldKeys.THE_END);
        }

        try {
            for (final Path namespacedDirectory : Files.list(this.customWorldsDirectory).toList()) {
                if (this.customWorldsDirectory.equals(namespacedDirectory)) {
                    continue;
                }

                for (final Path valueDirectory : Files.list(namespacedDirectory).toList()) {
                    if (namespacedDirectory.equals(valueDirectory)) {
                        continue;
                    }

                    worldKeys.add(ResourceKey.of(namespacedDirectory.getFileName().toString(), valueDirectory.getFileName().toString()));
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        return Collections.unmodifiableList(worldKeys);
    }

    @Override
    public boolean worldExists(final ResourceKey key) {
        final net.minecraft.resources.ResourceKey<Level> registryKey = SpongeWorldManager.createRegistryKey(Objects.requireNonNull(key, "key"));

        if (Level.OVERWORLD.equals(registryKey)) {
            return true;
        }

        if (this.worlds.get(registryKey) != null) {
            return true;
        }

        return Files.exists(this.getDirectory(key));
    }

    @Override
    public Optional<ResourceKey> worldKey(final UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return this.worlds
            .values()
            .stream()
            .filter(w -> ((ServerWorld) w).uniqueId().equals(uniqueId))
            .map(w -> (ServerWorld) w)
            .map(ServerWorld::key)
            .findAny();
    }

    @Override
    public Collection<ServerWorld> worldsOfType(final WorldType type) {
        Objects.requireNonNull(type, "type");

        return this.worlds().stream().filter(w -> w.worldType() == type).collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<ServerWorld> loadWorld(final WorldTemplate template) {
        final ResourceKey key = Objects.requireNonNull(template, "template").key();
        final net.minecraft.resources.ResourceKey<Level> registryKey = SpongeWorldManager.createRegistryKey(key);
        if (Level.OVERWORLD.equals(registryKey)) {
            return FutureUtil.completedWithException(new IllegalArgumentException("The default world cannot be told to load!"));
        }

        final ServerLevel serverWorld = this.worlds.get(registryKey);
        if (serverWorld != null) {
            return CompletableFuture.completedFuture((ServerWorld) serverWorld);
        }

        this.saveTemplate(template);

        return this.loadWorld0(registryKey, ((SpongeWorldTemplate) template).levelStem());
    }

    @Override
    public CompletableFuture<ServerWorld> loadWorld(final ResourceKey key) {
        final net.minecraft.resources.ResourceKey<Level> registryKey = SpongeWorldManager.createRegistryKey(Objects.requireNonNull(key, "key"));
        if (Level.OVERWORLD.equals(registryKey)) {
            return FutureUtil.completedWithException(new IllegalArgumentException("The default world cannot be told to load!"));
        }

        final ServerLevel world = this.worlds.get(registryKey);
        if (world != null) {
            return CompletableFuture.completedFuture((ServerWorld) world);
        }

        // First find a loaded level-stem / To load based on a datapack load using the WorldTemplate instead

        final net.minecraft.resources.ResourceKey<LevelStem> rKey = net.minecraft.resources.ResourceKey.create(Registries.LEVEL_STEM, (ResourceLocation) (Object) key);
        final LevelStem levelStem = SpongeCommon.vanillaRegistry(Registries.LEVEL_STEM).getValue(rKey);
        if (levelStem != null) {
            return this.loadWorld0(registryKey, levelStem);
        }

        // Then attempt to load from data pack
        final DataPack<WorldTemplate> pack = this.findPack(key);
        return this.loadTemplate(pack, key).thenCompose(template -> {
            if (template.isEmpty()) {
                return FutureUtil.completedWithException(new IOException(String.format("Failed to load a template for '%s'!", key)));
            }
            return this.loadWorld0(registryKey, ((SpongeWorldTemplate) template.get()).levelStem());
        });
    }

    private CompletableFuture<ServerWorld> loadWorld0(final net.minecraft.resources.ResourceKey<Level> registryKey, final LevelStem levelStem) {
        final ResourceKey worldKey = (ResourceKey) (Object) registryKey.location();
        final DimensionType dimensionType = levelStem.type().value();
        final Optional<ResourceKey> worldTypeKey = this.worldTypeKey(dimensionType);

        MinecraftServerAccessor.accessor$LOGGER().info("Loading world '{}' ({})", worldKey, worldTypeKey.map(ResourceKey::toString).orElse("inline"));

        final ChunkProgressListener chunkStatusListener = ((MinecraftServerAccessor) this.server).accessor$progressListenerFactory().create(11);
        final ServerLevel world;
        try {
            world = this.createNonDefaultLevel(registryKey, levelStem, worldKey, worldTypeKey.orElse(null), chunkStatusListener);
        } catch (final IOException e) {
            return FutureUtil.completedWithException(new RuntimeException(String.format("Failed to create level data for world '%s'!", worldKey), e));
        }

        return SpongeCommon.asyncScheduler().submit(() -> this.prepareWorld(world)).thenApply(w -> {
                    ((MinecraftServerAccessor) this.server).invoker$forceDifficulty();
                    return w;
                }).thenCompose(w -> this.postWorldLoad(world, false))
                  .thenApply(w -> (ServerWorld) w);
    }

    private LevelStorageSource.LevelStorageAccess getLevelStorageAccess(final ResourceKey worldKey) throws IOException {
        if (this.isVanillaWorld(worldKey)) {
            final String directoryName = this.getDirectoryName(worldKey);
            return LevelStorageSource.createDefault(this.defaultWorldDirectory).createAccess(directoryName);
        }
        final String name = worldKey.namespace() + File.separator + worldKey.value();
        return LevelStorageSource.createDefault(this.customWorldsDirectory).createAccess(name);
    }

    private LevelSettings createLevelSettings(final PrimaryLevelData defaultLevelData, final LevelStem levelStem, final String directoryName) {
        final LevelStemBridge levelStemBridge = (LevelStemBridge) (Object) levelStem;
        final GameType gameType = levelStemBridge.bridge$gameMode();
        final Boolean hardcore = levelStemBridge.bridge$hardcore();
        final Difficulty difficulty = levelStemBridge.bridge$difficulty();
        final Boolean allowCommands = levelStemBridge.bridge$allowCommands();
        return new LevelSettings(
                directoryName,
                gameType == null ? defaultLevelData.getGameType() : gameType,
                hardcore == null ? defaultLevelData.isHardcore() : hardcore,
                difficulty == null ? defaultLevelData.getDifficulty() : difficulty,
                allowCommands == null ? defaultLevelData.isAllowCommands() : allowCommands,
                defaultLevelData.getGameRules().copy(defaultLevelData.enabledFeatures()),
                defaultLevelData.getDataConfiguration());
    }

    @Override
    public CompletableFuture<Boolean> unloadWorld(final ResourceKey key) {
        final net.minecraft.resources.ResourceKey<Level> registryKey = SpongeWorldManager.createRegistryKey(Objects.requireNonNull(key, "key"));

        if (Level.OVERWORLD.equals(registryKey)) {
            return CompletableFuture.completedFuture(false);
        }

        final ServerLevel world = this.worlds.get(registryKey);
        if (world == null) {
            return CompletableFuture.completedFuture(false);
        }

        return this.unloadWorld((ServerWorld) world);
    }

    @Override
    public CompletableFuture<Boolean> unloadWorld(final ServerWorld world) {
        final net.minecraft.resources.ResourceKey<Level> registryKey = SpongeWorldManager.createRegistryKey(Objects.requireNonNull(world, "world").key());

        if (Level.OVERWORLD.equals(registryKey)) {
            return CompletableFuture.completedFuture(false);
        }

        if (world != this.worlds.get(registryKey)) {
            return CompletableFuture.completedFuture(false);
        }

        try {
            this.unloadWorld0((ServerLevel) world);
            return CompletableFuture.completedFuture(true);
        } catch (final IOException e) {
            return FutureUtil.completedWithException(e);
        }
    }

    @Override
    public CompletableFuture<Optional<ServerWorldProperties>> loadProperties(final ResourceKey key) {
        final net.minecraft.resources.ResourceKey<Level> registryKey = SpongeWorldManager.createRegistryKey(Objects.requireNonNull(key, "key"));

        if (this.worlds.get(registryKey) != null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        if (!this.worldExists(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        final LevelStorageSource.LevelStorageAccess storageSource;

        try {
            storageSource = this.getLevelStorageAccess(key);
        } catch (final IOException e) {
            return FutureUtil.completedWithException(e);
        }

        final WorldData levelData;
        try {
            final PrimaryLevelData defaultLevelData = (PrimaryLevelData) this.server.getWorldData();
            try {
                levelData = this.loadLevelData(this.server.registryAccess(), defaultLevelData.getDataConfiguration(), storageSource.getDataTag());
            } catch (final Exception ex) {
                return FutureUtil.completedWithException(ex);
            }
        } finally {
            try {
                storageSource.close();
            } catch (final IOException ex) {
                return FutureUtil.completedWithException(ex);
            }
        }

        if (levelData == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        final DataPack<WorldTemplate> pack = this.findPack(key);
        return this.loadTemplate(pack, key).thenCompose(template -> {
            if (template.isPresent()) {
                final LevelStem scratch = ((SpongeWorldTemplate) template.get()).levelStem();
                ((PrimaryLevelDataBridge) levelData).bridge$populateFromLevelStem(scratch);
            }

            ((ResourceKeyBridge) levelData).bridge$setKey(key);
            return CompletableFuture.completedFuture(Optional.of((ServerWorldProperties) levelData));
        });
    }

    @Override
    public CompletableFuture<Boolean> saveProperties(final ServerWorldProperties properties) {
        final net.minecraft.resources.ResourceKey<Level> registryKey = SpongeWorldManager.createRegistryKey(Objects.requireNonNull(properties, "properties").key());

        if (this.worlds.get(registryKey) != null) {
            return CompletableFuture.completedFuture(false);
        }

        try {
            this.saveLevelDat((WorldData) properties, properties.key());
        } catch (Exception ex) {
            return FutureUtil.completedWithException(ex);
        }

        // Properties doesn't have everything we need...namely the generator, load the template and set values we actually got
        final DataPack<WorldTemplate> pack = this.findPack(properties.key());
        return this.loadTemplate(pack, properties.key()).thenCompose(r -> {
            final WorldTemplate template = r.orElse(null);
            if (template != null) {
                return this.saveTemplate(WorldTemplate.builder().from(template).from(properties).build());
            }

            return CompletableFuture.completedFuture(true);
        });
    }


    private void saveLevelDat(final WorldData properties, final ResourceKey key) throws IOException {
        try (var storageSource = this.getLevelStorageAccess(key)) {
            storageSource.saveDataTag(this.server.registryAccess(), properties, null);
        }
    }

    @Override
    public CompletableFuture<Boolean> copyWorld(final ResourceKey key, final ResourceKey copyKey) {
        final net.minecraft.resources.ResourceKey<Level> registryKey = SpongeWorldManager.createRegistryKey(Objects.requireNonNull(key, "key"));
        final net.minecraft.resources.ResourceKey<Level> copyRegistryKey = SpongeWorldManager.createRegistryKey(Objects.requireNonNull(copyKey, "copyKey"));

        if (Level.OVERWORLD.equals(copyRegistryKey)) {
            return CompletableFuture.completedFuture(false);
        }

        if (!this.worldExists(key)) {
            return CompletableFuture.completedFuture(false);
        }

        if (this.worldExists(copyKey)) {
            return CompletableFuture.completedFuture(false);
        }

        final ServerLevel loadedWorld = this.worlds.get(registryKey);
        final boolean disableLevelSaving;

        if (loadedWorld != null) {
            disableLevelSaving = loadedWorld.noSave;
            loadedWorld.save(null, true, loadedWorld.noSave);
            loadedWorld.noSave = true;
        } else {
            disableLevelSaving = false;
        }

        final boolean isDefaultWorld = DefaultWorldKeys.DEFAULT.equals(key);

        return CompletableFuture.runAsync(() -> {
            final Path originalDirectory = this.getDirectory(key);
            final Path copyDirectory = this.getDirectory(copyKey);

            try {
                Files.walkFileTree(originalDirectory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                        // Silly recursion if the default world is being copied
                        if (dir.getFileName().toString().equals(Constants.Sponge.World.DIMENSIONS_DIRECTORY)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        // Silly copying of vanilla sub worlds if the default world is being copied
                        if (isDefaultWorld && SpongeWorldManager.this.isVanillaSubWorld(dir.getFileName().toString())) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        final Path relativize = originalDirectory.relativize(dir);
                        final Path directory = copyDirectory.resolve(relativize);
                        Files.createDirectories(directory);

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                        final String fileName = file.getFileName().toString();
                        // Do not copy backups (not relevant anymore)
                        if (fileName.equals(Constants.Sponge.World.LEVEL_SPONGE_DAT_OLD)) {
                            return FileVisitResult.CONTINUE;
                        }
                        if (fileName.equals(Constants.World.LEVEL_DAT_OLD)) {
                            return FileVisitResult.CONTINUE;
                        }
                        Files.copy(file, copyDirectory.resolve(originalDirectory.relativize(file)), StandardCopyOption.COPY_ATTRIBUTES,
                            StandardCopyOption.REPLACE_EXISTING);

                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (final IOException e) {
                // Bail the whole deal if we hit IO problems!
                try {
                    Files.walkFileTree(copyDirectory, DeleteFileVisitor.INSTANCE);
                } catch (final IOException ignore) {
                }

                throw new CompletionException(e);
            }

            if (loadedWorld != null) {
                loadedWorld.noSave = disableLevelSaving;
            }

            final Path configFile = this.getConfigFile(key);
            final Path copyConfigFile = this.getConfigFile(copyKey);

            try {
                Files.createDirectories(copyConfigFile.getParent());
                Files.copy(configFile, copyConfigFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException e) {
                throw new CompletionException(e);
            }
        }).thenApplyAsync($ -> {
            try {
                this.server().dataPackManager().copy(this.findPack(key), key, copyKey);
            } catch (final IOException e) {
                throw new CompletionException(e);
            }

            return true;
        }, SpongeCommon.server());
    }

    @Override
    public CompletableFuture<Boolean> moveWorld(final ResourceKey key, final ResourceKey movedKey) {
        final net.minecraft.resources.ResourceKey<Level> registryKey = SpongeWorldManager.createRegistryKey(Objects.requireNonNull(key, "key"));

        if (Level.OVERWORLD.equals(registryKey)) {
            return CompletableFuture.completedFuture(false);
        }

        if (!this.worldExists(key)) {
            return CompletableFuture.completedFuture(false);
        }

        if (this.worldExists(movedKey)) {
            return CompletableFuture.completedFuture(false);
        }

        final ServerLevel loadedWorld = this.worlds.get(registryKey);
        if (loadedWorld != null) {
            try {
                this.unloadWorld0(loadedWorld);
            } catch (final IOException e) {
                return FutureUtil.completedWithException(e);
            }
        }

        return CompletableFuture.runAsync(() -> {
            final Path originalDirectory = this.getDirectory(key);
            final Path movedDirectory = this.getDirectory(movedKey);

            try {
                Files.createDirectories(movedDirectory);
                Files.move(originalDirectory, movedDirectory, StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException e) {
                throw new CompletionException(e);
            }

            final Path configFile = this.getConfigFile(key);
            final Path movedConfigFile = this.getConfigFile(movedKey);

            try {
                Files.createDirectories(movedConfigFile.getParent());
                Files.move(configFile, movedConfigFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException e) {
                throw new CompletionException(e);
            }
        }).thenApplyAsync($ -> {
            try {
                this.server().dataPackManager().move(this.findPack(key), key, movedKey);
            } catch (final IOException e) {
                throw new CompletionException(e);
            }

            return true;
        }, SpongeCommon.server());
    }

    @Override
    public CompletableFuture<Boolean> deleteWorld(final ResourceKey key) {
        final net.minecraft.resources.ResourceKey<Level> registryKey = SpongeWorldManager.createRegistryKey(Objects.requireNonNull(key, "key"));

        if (Level.OVERWORLD.equals(registryKey)) {
            return CompletableFuture.completedFuture(false);
        }

        if (!this.worldExists(key)) {
            return CompletableFuture.completedFuture(false);
        }

        final ServerLevel loadedWorld = this.worlds.get(registryKey);
        if (loadedWorld != null) {
            final boolean disableLevelSaving = loadedWorld.noSave;
            loadedWorld.noSave = true;
            ((IOWorkerBridge) loadedWorld.getChunkSource().chunkMap.chunkScanner()).bridge$forciblyClear();
            try {
                this.unloadWorld0(loadedWorld);
            } catch (final IOException e) {
                loadedWorld.noSave = disableLevelSaving;
                return FutureUtil.completedWithException(e);
            }
        }

        return CompletableFuture.runAsync(() -> {
            final Path directory = this.getDirectory(key);
            if (Files.exists(directory)) {
                try {
                    Files.walkFileTree(directory, DeleteFileVisitor.INSTANCE);
                } catch (final IOException e) {
                    throw new CompletionException(e);
                }
            }

            final Path configFile = this.getConfigFile(key);
            try {
                Files.deleteIfExists(configFile);
            } catch (final IOException e) {
                throw new CompletionException(e);
            }
        }).thenApplyAsync($ -> {
            try {
                this.server().dataPackManager().delete(this.findPack(key), key);
            } catch (final IOException e) {
                throw new CompletionException(e);
            }

            //After vanilla has detected a new dimension from a data pack it "promotes" it
            //to the overworld's level data where the level persist even when the data pack is removed.
            //This forcible removes it from there too.
            final Registry<LevelStem> levelStemRegistry = SpongeCommon.vanillaRegistry(Registries.LEVEL_STEM);
            final net.minecraft.resources.ResourceKey<net.minecraft.world.level.dimension.LevelStem> levelStemKey = Registries.levelToLevelStem(registryKey);
            if (levelStemRegistry.containsKey(levelStemKey)) {
                ((MappedRegistryBridge<LevelStem>) levelStemRegistry).bridge$forceRemoveValue(Registries.levelToLevelStem(registryKey));
            }

            final LevelStorageSource.LevelStorageAccess storageSource = ((MinecraftServerAccessor) this.server).accessor$storageSource();
            final PrimaryLevelData levelData = (PrimaryLevelData) this.server.getWorldData();
            storageSource.saveDataTag(SpongeCommon.server().registryAccess(), levelData, null);

            return true;
        }, SpongeCommon.server());
    }

    private DataPack<WorldTemplate> findPack(ResourceKey key) {
        return this.server().dataPackManager().findPack(DataPackTypes.WORLD, key).orElse(DataPacks.WORLD);
    }

    private void unloadWorld0(final ServerLevel world) throws IOException {
        final net.minecraft.resources.ResourceKey<Level> registryKey = world.dimension();

        if (world.getPlayers(p -> true).size() != 0) {
            throw new IOException(String.format("World '%s' was told to unload but players remain.", registryKey.location()));
        }

        final Optional<ResourceKey> worldTypeKey = this.worldTypeKey(world.dimensionType());
        SpongeCommon.logger().info("Unloading world '{}' ({})", registryKey.location(), worldTypeKey.map(ResourceKey::toString).orElse("inline"));

        final UnloadWorldEvent unloadWorldEvent = SpongeEventFactory.createUnloadWorldEvent(PhaseTracker.getCauseStackManager().currentCause(), (ServerWorld) world);
        SpongeCommon.post(unloadWorldEvent);

        final BlockPos spawnPoint = world.getSharedSpawnPos();
        world.getChunkSource().removeTicketWithRadius(SpongeWorldManager.SPAWN_CHUNKS, new ChunkPos(spawnPoint), 11);

        ((PrimaryLevelDataBridge) world.getLevelData()).bridge$configAdapter().save();

        try {
            world.save(null, true, world.noSave);
            world.close();
            ((ServerLevelBridge) world).bridge$getLevelSave().close();
        } catch (final Exception ex) {
            throw new IOException(ex);
        }

        this.worlds.remove(registryKey);
    }

    public void loadLevel() {

        final boolean multiworldEnabled = this.server.isSingleplayer() || (this.server instanceof DedicatedServer ds && ds.getProperties().allowNether);
        if (!multiworldEnabled) {
            SpongeCommon.logger().warn("The option 'allow-nether' has been set to 'false' in the server.properties. "
                    + "Multi-World support has been disabled and no worlds besides the default world will be loaded.");
        }

        final ChunkProgressListener chunkStatusListener = ((MinecraftServerAccessor) this.server).accessor$progressListenerFactory().create(11);
        var registry = SpongeCommon.vanillaRegistry(Registries.LEVEL_STEM);
        for (LevelStem template : registry) {
            final ResourceKey worldKey = (ResourceKey) (Object) registry.getKey(template);
            final LevelStemBridge templateBridge = (LevelStemBridge) (Object) template;

            final boolean isDefaultWorld = DefaultWorldKeys.DEFAULT.equals(worldKey);
            if (!isDefaultWorld && !multiworldEnabled) {
                continue;
            }

            final DimensionType dimensionType = template.type().value();
            final Optional<ResourceKey> worldTypeKey = this.worldTypeKey(dimensionType);

            if (!isDefaultWorld && !templateBridge.bridge$loadOnStartup()) {
                SpongeCommon.logger().warn("World '{}' has been disabled from loading at startup. Skipping...", worldKey);
                continue;
            }
            MinecraftServerAccessor.accessor$LOGGER().info("Loading world '{}' ({})", worldKey, worldTypeKey.map(ResourceKey::toString).orElse("inline"));
            final net.minecraft.resources.ResourceKey<Level> registryKey = SpongeWorldManager.createRegistryKey(worldKey);
            if (isDefaultWorld) {
                final LevelStorageSource.LevelStorageAccess storageSource = ((MinecraftServerAccessor) this.server).accessor$storageSource();
                final PrimaryLevelData levelData = (PrimaryLevelData) this.server.getWorldData();
                ((ResourceKeyBridge) levelData).bridge$setKey(((ResourceKey) (Object) registryKey.location()));
                final List<CustomSpawner> spawners = ImmutableList.of(new PhantomSpawner(), new PatrolSpawner(), new CatSpawner(), new VillageSiege(), new WanderingTraderSpawner(levelData));

                final ServerLevel world = this.createLevel(registryKey, template, worldKey, worldTypeKey.orElse(null), storageSource, levelData, spawners, chunkStatusListener);

                // Ensure that the world border is registered.
                world.getWorldBorder().applySettings(levelData.getWorldBorder());
                this.prepareWorld(world);
            } else {
                try {
                    final ServerLevel world = this.createNonDefaultLevel(registryKey, template, worldKey, worldTypeKey.orElse(null), chunkStatusListener);
                    // Ensure that the world border is registered.
                    world.getWorldBorder().applySettings(((PrimaryLevelData) world.getLevelData()).getWorldBorder());
                    this.prepareWorld(world);
                } catch (final IOException e) {
                    throw new RuntimeException(String.format("Failed to create level data for world '%s'!", worldKey), e);
                } catch (final Exception e) {
                    throw new IllegalStateException(String.format("Failed to create level data for world '%s'!", worldKey), e);
                }
            }
        }

        ((MinecraftServerAccessor) this.server).invoker$forceDifficulty();

        for (final Map.Entry<net.minecraft.resources.ResourceKey<Level>, ServerLevel> entry : this.worlds.entrySet()) {
            try {
                this.postWorldLoad(entry.getValue(), true).get();
            } catch (final InterruptedException | ExecutionException e) {
                throw new IllegalStateException(e);
            }
        }

        ((SpongeUserManager) Sponge.server().userManager()).init();
    }

    private PrimaryLevelData getOrCreateLevelData(@Nullable final Dynamic<?> dynamicLevelData, final LevelStem levelStem, final String directoryName) {
        final PrimaryLevelData defaultLevelData = (PrimaryLevelData) this.server.getWorldData();
        if (dynamicLevelData != null) {
            try {
                @Nullable PrimaryLevelData levelData = this.loadLevelData(this.server.registryAccess(), defaultLevelData.getDataConfiguration(), dynamicLevelData);
                if (levelData != null) {
                    return levelData;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load level data from " + directoryName, e);
            }
        }

        if (this.server.isDemo()) {
            return new PrimaryLevelData(MinecraftServer.DEMO_SETTINGS, WorldOptions.DEMO_OPTIONS, SpongeWorldManager.specialWorldProperty(levelStem), Lifecycle.stable());
        }

        final LevelSettings levelSettings = this.createLevelSettings(defaultLevelData, levelStem, directoryName);
        final Long customSeed = ((LevelStemBridge) (Object) levelStem).bridge$seed();
        if (customSeed != null) {

            final WorldOptions generationSettings = ((WorldOptionsBridge) defaultLevelData.worldGenOptions()).bridge$withSeed(customSeed);
            // TODO generateStructures?
            // TODO bonusChest?
            return new PrimaryLevelData(levelSettings, generationSettings, SpongeWorldManager.specialWorldProperty(levelStem), Lifecycle.stable());
        }
        return new PrimaryLevelData(levelSettings, defaultLevelData.worldGenOptions(), SpongeWorldManager.specialWorldProperty(levelStem), Lifecycle.stable());
    }

    private PrimaryLevelData loadLevelData(final RegistryAccess.Frozen access, final WorldDataConfiguration datapackConfig, final Dynamic<?> dataTag) {
        final LevelDataAndDimensions levelData = LevelStorageSource.getLevelDataAndDimensions(dataTag, datapackConfig, access.lookupOrThrow(Registries.LEVEL_STEM), access);
        return (PrimaryLevelData) levelData.worldData();
    }

    // Do not call this for the default world, that is handled very special in loadLevel()
    private ServerLevel createNonDefaultLevel(
            final net.minecraft.resources.ResourceKey<Level> registryKey,
            final LevelStem levelStem,
            final ResourceKey worldKey,
            @Nullable final ResourceKey worldTypeKey,
            final ChunkProgressListener chunkStatusListener) throws IOException {
        final String directoryName = this.getDirectoryName(worldKey);
        final LevelStorageSource.LevelStorageAccess storageSource = this.getLevelStorageAccess(worldKey);
        Dynamic<?> dataTag;
        try {
            dataTag = storageSource.getDataTag();
        } catch (IOException e) {
            dataTag = null; // ((MinecraftServerAccessor) this.server).accessor$storageSource().getDataTag(); // Fallback to overworld level.dat
        }
        final PrimaryLevelData levelData = this.getOrCreateLevelData(dataTag, levelStem, directoryName);
        final List<CustomSpawner> spawners;
        if (levelStem.type().is(BuiltinDimensionTypes.OVERWORLD) || levelStem.type().is(BuiltinDimensionTypes.OVERWORLD_CAVES)) {
            spawners = ImmutableList.of(new PhantomSpawner(), new PatrolSpawner(), new CatSpawner(), new VillageSiege(), new WanderingTraderSpawner(levelData));
        } else {
            spawners = ImmutableList.of();
        }
        ((ResourceKeyBridge) levelData).bridge$setKey(worldKey);
        return this.createLevel(registryKey, levelStem, worldKey, worldTypeKey, storageSource, levelData, spawners, chunkStatusListener);
    }

    private ServerLevel createLevel(
            final net.minecraft.resources.ResourceKey<Level> registryKey,
            final LevelStem levelStem,
            final ResourceKey worldKey,
            @Nullable final ResourceKey worldTypeKey,
            final LevelStorageSource.LevelStorageAccess storageSource,
            final PrimaryLevelData levelData,
            final List<CustomSpawner> spawners,
            final ChunkProgressListener chunkStatusListener) {

        ((PrimaryLevelDataBridge) levelData).bridge$populateFromLevelStem(levelStem);

        final InheritableConfigHandle<WorldConfig> configAdapter = SpongeGameConfigs.createWorld(worldTypeKey, worldKey);
        ((PrimaryLevelDataBridge) levelData).bridge$configAdapter(configAdapter);

        levelData.setModdedInfo(this.server.getServerModName(), this.server.getModdedStatus().shouldReportAsModified());
        final long seed = BiomeManager.obfuscateSeed(levelData.worldGenOptions().seed());

        final Executor executor = ((MinecraftServerAccessor) this.server).accessor$executor();
        final ServerLevel world = new ServerLevel(this.server, executor, storageSource, levelData,
                registryKey, levelStem, chunkStatusListener, levelData.isDebugWorld(), seed, spawners, true, null);
        this.worlds.put(registryKey, world);

        return world;
    }

    private ServerLevel prepareWorld(final ServerLevel world) {
        final boolean isDefaultWorld = Level.OVERWORLD.equals(world.dimension());
        final PrimaryLevelData levelData = (PrimaryLevelData) world.getLevelData();
        final PrimaryLevelDataBridge levelDataBridge = (PrimaryLevelDataBridge) levelData;

        if (isDefaultWorld) {
            // Initialize scoreboard data. This will hook to the ServerScoreboard, needs to be made multi-world aware
            ((MinecraftServerAccessor) this.server).accessor$readScoreboard(world.getDataStorage());

            ((MinecraftServerAccessor) this.server).accessor$commandStorage(new CommandStorage(world.getDataStorage()));
        }

        final boolean isInitialized = levelData.isInitialized();

        final LoadWorldEvent loadWorldEvent = SpongeEventFactory.createLoadWorldEvent(PhaseTracker.getCauseStackManager().currentCause(), (ServerWorld) world, isInitialized);
        SpongeCommon.post(loadWorldEvent);
        PlatformHooks.INSTANCE.getWorldHooks().postLoadWorld(world);

        levelDataBridge.bridge$triggerViewDistanceLogic();

        world.getWorldBorder().applySettings(levelData.getWorldBorder());

        if (!isInitialized) {
            try {
                final boolean isDebugGeneration = levelData.isDebugWorld();
                final boolean hasSpawnAlready = levelDataBridge.bridge$customSpawnPosition();
                if (!hasSpawnAlready) {
                    if (isDefaultWorld || levelDataBridge.bridge$performsSpawnLogic()) {
                        try (final var state = GenerationPhase.State.TERRAIN_GENERATION.createPhaseContext(PhaseTracker.getInstance())) {
                            state.buildAndSwitch();
                            MinecraftServerAccessor.invoker$setInitialSpawn(world, levelData, levelData.worldGenOptions().generateBonusChest(), isDebugGeneration);
                        }
                    } else if (Level.END.equals(world.dimension())) {
                        levelData.setSpawn(ServerLevel.END_SPAWN_POINT, 0);
                    }
                } else if (levelData.worldGenOptions().generateBonusChest()) {
                    final BlockPos pos = levelData.getSpawnPos();
                    final ConfiguredFeature<?, ?> bonusChestFeature = SpongeCommon.vanillaRegistry(Registries.CONFIGURED_FEATURE).getValue(MiscOverworldFeatures.BONUS_CHEST);
                    bonusChestFeature.place(world, world.getChunkSource().getGenerator(), world.random, pos);
                }
                levelData.setInitialized(true);
                if (isDebugGeneration) {
                    ((MinecraftServerAccessor) this.server).invoker$setupDebugLevel(levelData);
                }
            } catch (final Throwable throwable) {
                final CrashReport crashReport = CrashReport.forThrowable(throwable, "Exception initializing world '" + world.dimension().location()  + "'");
                try {
                    world.fillReportDetails(crashReport);
                } catch (final Throwable ignore) {
                }

                throw new ReportedException(crashReport);
            }

            levelData.setInitialized(true);
        }

        // Initialize PlayerData in PlayerList, add WorldBorder listener. We change the method in PlayerList to handle per-world border
        this.server.getPlayerList().addWorldborderListener(world);

        if (levelData.getCustomBossEvents() != null) {
            ((ServerLevelBridge) world).bridge$getBossBarManager().load(levelData.getCustomBossEvents(), world.registryAccess());
        }

        return world;
    }

    private CompletableFuture<ServerLevel> postWorldLoad(final ServerLevel world, final boolean blocking) {
        final PrimaryLevelData levelData = (PrimaryLevelData) world.getLevelData();
        final PrimaryLevelDataBridge levelBridge = (PrimaryLevelDataBridge) levelData;
        if (Level.OVERWORLD.equals(world.dimension()) || levelBridge.bridge$performsSpawnLogic()) {
            final Optional<ResourceKey> worldTypeKey = this.worldTypeKey(world.dimensionType());
            MinecraftServerAccessor.accessor$LOGGER().info("Preparing start region for world '{}' ({})", world.dimension().location(),
                    worldTypeKey.map(ResourceKey::toString).orElse("inline"));
            if (blocking) {
                this.loadSpawnChunks(world);
                return CompletableFuture.completedFuture(world); // Chunk are generated
            } else {
                return this.loadSpawnChunksAsync(world); // Chunks are NOT generated yet BUT will be when the future returns
            }
        }
        return CompletableFuture.completedFuture(world); // Chunks are NOT generated AND will not generate unless prompted
    }

    private Optional<ResourceKey> worldTypeKey(final DimensionType type) {
        return Optional.ofNullable(SpongeCommon.vanillaRegistry(Registries.DIMENSION_TYPE).getKey(type)).map(ResourceKey.class::cast);
    }

    private CompletableFuture<ServerLevel> loadSpawnChunksAsync(final ServerLevel world) {

        final BlockPos spawnPoint = world.getSharedSpawnPos();
        final ChunkPos chunkPos = new ChunkPos(spawnPoint);
        final ServerChunkCache serverChunkProvider = world.getChunkSource();
//        serverChunkProvider.getLightEngine().setTaskPerBatch(500); was 5 in 1.19.4 , final 1000 in 1.20

        final int borderRadius = 11;
        final int diameter = ((borderRadius - 1) * 2) + 1;
        final int spawnChunks = diameter * diameter;

        serverChunkProvider.addTicketWithRadius(SpongeWorldManager.SPAWN_CHUNKS, chunkPos, borderRadius);
        final CompletableFuture<ServerLevel> generationFuture = new CompletableFuture<>();
        Sponge.asyncScheduler().submit(
                Task.builder().plugin(Launch.instance().platformPlugin())
                        .execute(task -> {
                            if (serverChunkProvider.getTickingGenerated() >= spawnChunks) {
                                Sponge.server().scheduler().submit(Task.builder().plugin(Launch.instance().platformPlugin()).execute(() -> generationFuture.complete(world)).build());
                                // Notify the future that we are done
                                task.cancel(); // And cancel this task
                                MinecraftServerAccessor.accessor$LOGGER().info("Done preparing start region for world '{}' ({})", world.dimension().location(),
                                        this.worldTypeKey(world.dimensionType()).map(ResourceKey::toString).orElse("inline"));
                            }
                        })
                        .interval(10, TimeUnit.MILLISECONDS)
                        .build()
        );
        return generationFuture.thenApply(v -> {
            this.updateForcedChunks(world, serverChunkProvider);
//            serverChunkProvider.getLightEngine().setTaskPerBatch(5);

            // Sponge Start - Release the chunk ticket if spawn is not set to be kept loaded...
            if (!((PrimaryLevelDataBridge) world.getLevelData()).bridge$performsSpawnLogic()) {
                serverChunkProvider.removeTicketWithRadius(SpongeWorldManager.SPAWN_CHUNKS, chunkPos, 11);
            }
            return world;
        });
    }

    private void loadSpawnChunks(final ServerLevel world) {
        final BlockPos spawnPoint = world.getSharedSpawnPos();
        final ChunkPos chunkPos = new ChunkPos(spawnPoint);
        final ChunkProgressListener chunkStatusListener = ((ServerLevelBridge) world).bridge$getChunkStatusListener();
        chunkStatusListener.updateSpawnPos(chunkPos);
        final ServerChunkCache serverChunkProvider = world.getChunkSource();
//        serverChunkProvider.getLightEngine().setTaskPerBatch(500);
        ((MinecraftServerAccessor) this.server).accessor$nextTickTimeNanos(Util.getNanos());
        serverChunkProvider.addTicketWithRadius(SpongeWorldManager.SPAWN_CHUNKS, chunkPos, 11);

        while (serverChunkProvider.getTickingGenerated() != 441) {
            ((MinecraftServerAccessor) this.server).accessor$nextTickTimeNanos(Util.getNanos() + 10L * TimeUtil.NANOSECONDS_PER_MILLISECOND);
            ((MinecraftServerAccessor) this.server).accessor$waitUntilNextTick();
        }

        ((MinecraftServerAccessor) this.server).accessor$nextTickTimeNanos(Util.getNanos() + 10L * TimeUtil.NANOSECONDS_PER_MILLISECOND);
        ((MinecraftServerAccessor) this.server).accessor$waitUntilNextTick();

        this.updateForcedChunks(world, serverChunkProvider);

        ((MinecraftServerAccessor) this.server).accessor$nextTickTimeNanos(Util.getNanos() + 10L * TimeUtil.NANOSECONDS_PER_MILLISECOND);
        ((MinecraftServerAccessor) this.server).accessor$waitUntilNextTick();
        chunkStatusListener.stop();
//        serverChunkProvider.getLightEngine().setTaskPerBatch(5);

        // Sponge Start - Release the chunk ticket if spawn is not set to be kept loaded...
        if (!((PrimaryLevelDataBridge) world.getLevelData()).bridge$performsSpawnLogic()) {
            serverChunkProvider.removeTicketWithRadius(SpongeWorldManager.SPAWN_CHUNKS, chunkPos, 11);
        }
    }

    private void updateForcedChunks(final ServerLevel world, final ServerChunkCache serverChunkProvider) {
        final @Nullable TicketStorage forcedChunksSaveData = world.getDataStorage().get(TicketStorage.factory(), "chunks");
        if (forcedChunksSaveData != null) {
            final LongIterator longIterator = forcedChunksSaveData.getForceLoadedChunks().iterator();

            while (longIterator.hasNext()) {
                final long i = longIterator.nextLong();
                final ChunkPos forceChunkPos = new ChunkPos(i);
                serverChunkProvider.updateChunkForced(forceChunkPos, true);
            }
        }
    }

    private CompletionStage<Boolean> saveTemplate(final WorldTemplate template) {
        return this.server().dataPackManager().save(template).thenApply(b -> true);
    }

    private CompletableFuture<Optional<WorldTemplate>> loadTemplate(final DataPack<WorldTemplate> pack, final ResourceKey key) {
        if (this.server().dataPackManager().exists(pack, key)) {
            return this.server().dataPackManager().load(pack, key).exceptionally(e -> {
                e.printStackTrace();
                return Optional.empty();
            });
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }

    public static net.minecraft.resources.ResourceKey<Level> createRegistryKey(final ResourceKey key) {
        return net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, (ResourceLocation) (Object) key);
    }

    private String getDirectoryName(final ResourceKey key) {
        if (DefaultWorldKeys.DEFAULT.equals(key)) {
            return "";
        }
        if (DefaultWorldKeys.THE_NETHER.equals(key)) {
            return "DIM-1";
        }
        if (DefaultWorldKeys.THE_END.equals(key)) {
            return "DIM1";
        }
        return key.value();
    }

    private Path getDirectory(final ResourceKey key) {
        if (DefaultWorldKeys.DEFAULT.equals(key)) {
            return this.defaultWorldDirectory;
        }
        if (DefaultWorldKeys.THE_NETHER.equals(key)) {
            return this.defaultWorldDirectory.resolve("DIM-1");
        }
        if (DefaultWorldKeys.THE_END.equals(key)) {
            return this.defaultWorldDirectory.resolve("DIM1");
        }
        return this.customWorldsDirectory.resolve(key.namespace()).resolve(key.value());
    }

    private boolean isVanillaWorld(final ResourceKey key) {
        return DefaultWorldKeys.DEFAULT.equals(key) || DefaultWorldKeys.THE_NETHER.equals(key) || DefaultWorldKeys.THE_END.equals(key);
    }

    private boolean isVanillaSubWorld(final String directoryName) {
        return "DIM-1".equals(directoryName) || "DIM1".equals(directoryName);
    }

    private Path getConfigFile(final ResourceKey key) {
        return SpongeCommon.spongeConfigDirectory().resolve(Launch.instance().id()).resolve("worlds").resolve(key.namespace())
                .resolve(key.value() + ".conf");
    }

    private static PrimaryLevelData.SpecialWorldProperty specialWorldProperty(final LevelStem stem) {
        //Copied from WorldDimensions#specialWorldProperty
        final ChunkGenerator $$1 = stem.generator();
        if ($$1 instanceof DebugLevelSource) {
            return PrimaryLevelData.SpecialWorldProperty.DEBUG;
        } else {
            return $$1 instanceof FlatLevelSource ? PrimaryLevelData.SpecialWorldProperty.FLAT : PrimaryLevelData.SpecialWorldProperty.NONE;
        }
    }
}
