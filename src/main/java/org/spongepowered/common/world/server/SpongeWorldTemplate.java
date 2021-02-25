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

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.kyori.adventure.text.Component;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.datapack.DataPackType;
import org.spongepowered.api.datapack.DataPackTypes;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.registry.RegistryReference;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.WorldType;
import org.spongepowered.api.world.WorldTypes;
import org.spongepowered.api.world.biome.provider.BiomeProvider;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.generation.ChunkGenerator;
import org.spongepowered.api.world.generation.config.NoiseGeneratorConfig;
import org.spongepowered.api.world.generation.config.WorldGenerationConfig;
import org.spongepowered.api.world.server.WorldTemplate;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
import org.spongepowered.common.AbstractResourceKeyed;
import org.spongepowered.common.accessor.world.gen.DimensionGeneratorSettingsAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.ResourceKeyBridge;
import org.spongepowered.common.bridge.world.level.dimension.LevelStemBridge;
import org.spongepowered.common.bridge.world.level.storage.PrimaryLevelDataBridge;
import org.spongepowered.common.serialization.EnumCodec;
import org.spongepowered.common.serialization.MathCodecs;
import org.spongepowered.common.util.AbstractResourceKeyedBuilder;
import org.spongepowered.common.util.MissingImplementationException;
import org.spongepowered.common.server.BootstrapProperties;
import org.spongepowered.math.vector.Vector3i;

import java.util.Objects;
import java.util.Optional;

public final class SpongeWorldTemplate extends AbstractResourceKeyed implements WorldTemplate {

    @Nullable public final Component displayName;
    public final RegistryReference<WorldType> worldType;
    public final org.spongepowered.api.world.generation.ChunkGenerator generator;
    public final WorldGenerationConfig generationConfig;
    @Nullable public final SerializationBehavior serializationBehavior;
    @Nullable public final RegistryReference<GameMode> gameMode;
    @Nullable public final RegistryReference<Difficulty> difficulty;
    @Nullable public final Integer viewDistance;
    @Nullable public final Vector3i spawnPosition;
    @Nullable public final Boolean hardcore, pvp, commands;

    public final boolean loadOnStartup, performsSpawnLogic;

    private static final Codec<SpongeDataSection> SPONGE_CODEC = RecordCodecBuilder
            .create(r -> r
                    .group(
                            SpongeAdventure.STRING_CODEC.optionalFieldOf("display_name").forGetter(v -> Optional.ofNullable(v.displayName)),
                            ResourceLocation.CODEC.optionalFieldOf("game_mode").forGetter(v -> Optional.ofNullable(v.gameMode)),
                            ResourceLocation.CODEC.optionalFieldOf("difficulty").forGetter(v -> Optional.ofNullable(v.difficulty)),
                            EnumCodec.create(SerializationBehavior.class).optionalFieldOf("serialization_behavior").forGetter(v -> Optional.ofNullable(v.serializationBehavior)),
                            Codec.INT.optionalFieldOf("view_distance").forGetter(v -> Optional.ofNullable(v.viewDistance)),
                            MathCodecs.VECTOR_3i.optionalFieldOf("spawn_position").forGetter(v -> Optional.ofNullable(v.spawnPosition)),
                            Codec.BOOL.optionalFieldOf("load_on_startup").forGetter(v -> Optional.ofNullable(v.loadOnStartup)),
                            Codec.BOOL.optionalFieldOf("performs_spawn_logic").forGetter(v -> Optional.ofNullable(v.performsSpawnLogic)),
                            Codec.BOOL.optionalFieldOf("hardcore").forGetter(v -> Optional.ofNullable(v.hardcore)),
                            Codec.BOOL.optionalFieldOf("commands").forGetter(v -> Optional.ofNullable(v.commands)),
                            Codec.BOOL.optionalFieldOf("pvp").forGetter(v -> Optional.ofNullable(v.pvp))
                    )
                    // *Chuckles* I continue to be in danger...
                    .apply(r, (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11) ->
                            new SpongeDataSection(f1.orElse(null), f2.orElse(null), f3.orElse(null), f4.orElse(null),
                                    f5.orElse(null), f6.orElse(null), f7.orElse(null), f8.orElse(null), f9.orElse(null),
                                    f10.orElse(null), f11.orElse(null))
                    )
            );

    public static final Codec<LevelStem> DIRECT_CODEC = RecordCodecBuilder
            .create(r ->  r
                    .group(
                            SpongeWorldTypeTemplate.CODEC.fieldOf("type").forGetter(LevelStem::typeSupplier),
                            net.minecraft.world.level.chunk.ChunkGenerator.CODEC.fieldOf("generator").forGetter(LevelStem::generator),
                            SpongeWorldTemplate.SPONGE_CODEC.optionalFieldOf("#sponge").forGetter(v -> {
                                final LevelStemBridge levelStemBridge = (LevelStemBridge) (Object) v;
                                return Optional.of(new SpongeDataSection(levelStemBridge.bridge$displayName().orElse(null),
                                    levelStemBridge.bridge$gameMode().orElse(null), levelStemBridge.bridge$difficulty().orElse(null),
                                    levelStemBridge.bridge$serializationBehavior().orElse(null),
                                    levelStemBridge.bridge$viewDistance().orElse(null), levelStemBridge.bridge$spawnPosition().orElse(null),
                                    levelStemBridge.bridge$loadOnStartup(), levelStemBridge.bridge$performsSpawnLogic(),
                                    levelStemBridge.bridge$hardcore().orElse(null), levelStemBridge.bridge$commands().orElse(null),
                                    levelStemBridge.bridge$pvp().orElse(null)));
                            })
                    )
                    .apply(r, r
                        .stable((f1, f2, f3) ->
                            {
                                final LevelStem template = new LevelStem(f1, f2);
                                f3.ifPresent(((LevelStemBridge) (Object) template)::bridge$populateFromData);
                                return template;
                            }
                        )
                    )
            );

    protected SpongeWorldTemplate(final BuilderImpl builder) {
        super(builder.key);
        this.displayName = builder.displayName;
        this.worldType = builder.worldType;
        this.generator = builder.generator;
        this.generationConfig = builder.generationConfig;
        this.gameMode = builder.gameMode;
        this.difficulty = builder.difficulty;
        this.serializationBehavior = builder.serializationBehavior;
        this.viewDistance = builder.viewDistance;
        this.spawnPosition = builder.spawnPosition;
        this.loadOnStartup = builder.loadOnStartup;
        this.performsSpawnLogic = builder.performsSpawnLogic;
        this.hardcore = builder.hardcore;
        this.commands = builder.commands;
        this.pvp = builder.pvp;
    }

    public SpongeWorldTemplate(final ServerLevel world) {
        super((ResourceKey) (Object) world.dimension().location());
        final ServerWorldProperties levelData = (ServerWorldProperties) world.getLevelData();
        final PrimaryLevelDataBridge levelBridge = (PrimaryLevelDataBridge) world.getLevelData();

        this.displayName = levelBridge.bridge$displayName().orElse(null);
        this.worldType = ((WorldType) world.dimensionType()).asDefaultedReference(RegistryTypes.WORLD_TYPE);
        this.generator = (ChunkGenerator) world.getChunkSource().getGenerator();
        this.generationConfig = WorldGenerationConfig.Mutable.builder().from(levelData.worldGenerationConfig()).build();
        this.gameMode = levelBridge.bridge$customGameType() ? levelData.gameMode().asDefaultedReference(RegistryTypes.GAME_MODE) : null;
        this.difficulty = levelBridge.bridge$customDifficulty() ? levelData.difficulty().asDefaultedReference(RegistryTypes.DIFFICULTY) : null;
        this.serializationBehavior = levelBridge.bridge$serializationBehavior().orElse(null);
        this.viewDistance = levelBridge.bridge$viewDistance().orElse(null);
        this.spawnPosition = levelBridge.bridge$customSpawnPosition() ? levelData.spawnPosition() : null;
        this.loadOnStartup = levelData.loadOnStartup();
        this.performsSpawnLogic = levelData.performsSpawnLogic();
        this.hardcore = levelData.hardcore();
        this.commands = levelData.commands();
        this.pvp = levelData.pvp();
    }

    public SpongeWorldTemplate(final LevelStem template) {
        super(((ResourceKeyBridge) (Object) template).bridge$getKey());
        final LevelStemBridge templateBridge = (LevelStemBridge) (Object) template;
        this.displayName = templateBridge.bridge$displayName().orElse(null);
        this.worldType = ((WorldType) template.type()).asDefaultedReference(RegistryTypes.WORLD_TYPE);
        this.generator = (ChunkGenerator) template.generator();
        this.generationConfig = WorldGenerationConfig.Mutable.builder().from((WorldGenerationConfig.Mutable) BootstrapProperties.dimensionGeneratorSettings).build();
        this.gameMode = templateBridge.bridge$gameMode().isPresent() ? RegistryTypes.GAME_MODE.referenced((ResourceKey) (Object) templateBridge.bridge$gameMode().get()) : null;
        this.difficulty = templateBridge.bridge$difficulty().isPresent() ? RegistryTypes.DIFFICULTY.referenced((ResourceKey) (Object) templateBridge.bridge$difficulty().get()) : null;
        this.serializationBehavior = templateBridge.bridge$serializationBehavior().orElse(null);
        this.viewDistance = templateBridge.bridge$viewDistance().orElse(null);
        this.spawnPosition = templateBridge.bridge$spawnPosition().orElse(null);
        this.loadOnStartup = templateBridge.bridge$loadOnStartup();
        this.performsSpawnLogic = templateBridge.bridge$performsSpawnLogic();
        this.hardcore = templateBridge.bridge$hardcore().orElse(null);
        this.commands = templateBridge.bridge$commands().orElse(null);
        this.pvp = templateBridge.bridge$pvp().orElse(null);
    }

    @Override
    public DataPackType type() {
        return DataPackTypes.WORLD;
    }

    @Override
    public Optional<Component> displayName() {
        return Optional.ofNullable(this.displayName);
    }

    @Override
    public RegistryReference<WorldType> worldType() {
        return this.worldType;
    }

    @Override
    public org.spongepowered.api.world.generation.ChunkGenerator generator() {
        return this.generator;
    }

    @Override
    public WorldGenerationConfig generationConfig() {
        return this.generationConfig;
    }

    @Override
    public Optional<RegistryReference<GameMode>> gameMode() {
        return Optional.ofNullable(this.gameMode);
    }

    @Override
    public Optional<RegistryReference<Difficulty>> difficulty() {
        return Optional.ofNullable(this.difficulty);
    }

    @Override
    public Optional<SerializationBehavior> serializationBehavior() {
        return Optional.ofNullable(this.serializationBehavior);
    }

    @Override
    public boolean loadOnStartup() {
        return this.loadOnStartup;
    }

    @Override
    public boolean performsSpawnLogic() {
        return this.performsSpawnLogic;
    }

    @Override
    public Optional<Boolean> hardcore() {
        return Optional.ofNullable(this.hardcore);
    }

    @Override
    public Optional<Boolean> commands() {
        return Optional.ofNullable(this.commands);
    }

    @Override
    public Optional<Boolean> pvp() {
        return Optional.ofNullable(this.pvp);
    }

    @Override
    public Optional<Integer> viewDistance() {
        return Optional.ofNullable(this.viewDistance);
    }

    @Override
    public Optional<Vector3i> spawnPosition() {
        return Optional.ofNullable(this.spawnPosition);
    }

    @Override
    public int getContentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        throw new MissingImplementationException("SpongeWorldTemplate", "toContainer");
    }

    public LevelStem asDimension() {
        final LevelStem scratch =
                new LevelStem(() -> BootstrapProperties.registries.dimensionTypes().get((ResourceLocation) (Object) this.worldType.location()),
                        (net.minecraft.world.level.chunk.ChunkGenerator) this.generator);
        ((LevelStemBridge) (Object) scratch).bridge$setFromSettings(false);
        ((LevelStemBridge) (Object) scratch).bridge$populateFromTemplate(this);
        return scratch;
    }

    public static final class SpongeDataSection {

        @Nullable public final Component displayName;
        @Nullable public final ResourceLocation gameMode;
        @Nullable public final ResourceLocation difficulty;
        @Nullable public final SerializationBehavior serializationBehavior;
        @Nullable public final Integer viewDistance;
        @Nullable public final Vector3i spawnPosition;
        @Nullable public final Boolean loadOnStartup, performsSpawnLogic, hardcore, commands, pvp;

        public SpongeDataSection(@Nullable final Component displayName, @Nullable final ResourceLocation gameMode,
            @Nullable final ResourceLocation difficulty, @Nullable final SerializationBehavior serializationBehavior,
            @Nullable final Integer viewDistance, @Nullable final Vector3i spawnPosition, @Nullable final Boolean loadOnStartup,
            @Nullable final Boolean performsSpawnLogic, @Nullable final Boolean hardcore, @Nullable final Boolean commands,
            @Nullable final Boolean pvp)
        {
            this.displayName = displayName;
            this.gameMode = gameMode;
            this.difficulty = difficulty;
            this.serializationBehavior = serializationBehavior;
            this.viewDistance = viewDistance;
            this.spawnPosition = spawnPosition;
            this.loadOnStartup = loadOnStartup;
            this.performsSpawnLogic = performsSpawnLogic;
            this.hardcore = hardcore;
            this.commands = commands;
            this.pvp = pvp;
        }
    }

    public static final class BuilderImpl extends AbstractResourceKeyedBuilder<WorldTemplate, WorldTemplate.Builder> implements WorldTemplate.Builder {

        @Nullable protected Component displayName;
        @Nullable protected RegistryReference<WorldType> worldType;
        @Nullable protected ChunkGenerator generator;
        @Nullable protected WorldGenerationConfig generationConfig;
        @Nullable protected RegistryReference<GameMode> gameMode;
        @Nullable protected RegistryReference<Difficulty> difficulty;
        @Nullable protected SerializationBehavior serializationBehavior;
        @Nullable protected Integer viewDistance;
        @Nullable protected Vector3i spawnPosition;
        @Nullable protected Boolean hardcore, pvp, commands;

        protected boolean loadOnStartup, performsSpawnLogic;

        @Override
        public Builder displayName(@Nullable final Component displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        public Builder worldType(final RegistryReference<WorldType> worldType) {
            this.worldType = Objects.requireNonNull(worldType, "worldType");
            return this;
        }

        @Override
        public Builder generator(final ChunkGenerator generator) {
            this.generator = Objects.requireNonNull(generator, "generator");
            return this;
        }

        @Override
        public Builder generationConfig(final WorldGenerationConfig generationConfig) {
            this.generationConfig = Objects.requireNonNull(generationConfig, "generationConfig");
            return this;
        }

        @Override
        public Builder gameMode(final RegistryReference<GameMode> gameMode) {
            this.gameMode = gameMode;
            return this;
        }

        @Override
        public Builder difficulty(final RegistryReference<Difficulty> difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        @Override
        public Builder serializationBehavior(@Nullable final SerializationBehavior serializationBehavior) {
            this.serializationBehavior = serializationBehavior;
            return this;
        }

        @Override
        public Builder loadOnStartup(final boolean loadOnStartup) {
            this.loadOnStartup = loadOnStartup;
            return this;
        }

        @Override
        public Builder performsSpawnLogic(final boolean performsSpawnLogic) {
            this.performsSpawnLogic = performsSpawnLogic;
            return this;
        }

        @Override
        public Builder hardcore(@Nullable final Boolean hardcore) {
            this.hardcore = hardcore;
            return this;
        }

        @Override
        public Builder commands(@Nullable Boolean commands) {
            this.commands = commands;
            return this;
        }

        @Override
        public Builder pvp(@Nullable final Boolean pvp) {
            this.pvp = pvp;
            return this;
        }

        @Override
        public Builder viewDistance(@Nullable final Integer distance) {
            this.viewDistance = distance;
            return this;
        }

        @Override
        public Builder spawnPosition(@Nullable Vector3i position) {
            this.spawnPosition = position;
            return this;
        }

        @Override
        public Builder reset() {
            super.reset();
            this.displayName = null;
            this.worldType = WorldTypes.OVERWORLD;
            this.generator = ChunkGenerator.overworld();
            final WorldGenSettings generationSettings = BootstrapProperties.dimensionGeneratorSettings;
            this.generationConfig = (WorldGenerationConfig) DimensionGeneratorSettingsAccessor.invoker$new(generationSettings.seed(),
                    generationSettings.generateFeatures(), generationSettings.generateBonusChest(),
                    new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.stable()),
                    ((DimensionGeneratorSettingsAccessor) generationSettings).accessor$legacyCustomOptions());
            this.gameMode = null;
            this.difficulty = null;
            this.serializationBehavior = null;
            this.viewDistance = null;
            this.spawnPosition = null;
            this.loadOnStartup = true;
            this.performsSpawnLogic = false;
            this.hardcore = null;
            this.commands = null;
            this.pvp = null;
            return this;
        }

        @Override
        public Builder from(final WorldTemplate template) {
            this.key = Objects.requireNonNull(template, "template").getKey();
            this.displayName = template.displayName().orElse(null);
            this.worldType = template.worldType();
            this.generator = template.generator();
            final WorldGenSettings generationSettings = (WorldGenSettings) template.generationConfig();
            this.generationConfig = (WorldGenerationConfig) DimensionGeneratorSettingsAccessor.invoker$new(generationSettings.seed(),
                    generationSettings.generateFeatures(), generationSettings.generateBonusChest(),
                    new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.stable()),
                    ((DimensionGeneratorSettingsAccessor) generationSettings).accessor$legacyCustomOptions());
            this.gameMode = template.gameMode().orElse(null);
            this.difficulty = template.difficulty().orElse(null);
            this.serializationBehavior = template.serializationBehavior().orElse(null);
            this.viewDistance = template.viewDistance().orElse(null);
            this.spawnPosition = template.spawnPosition().orElse(null);
            this.loadOnStartup = template.loadOnStartup();
            this.performsSpawnLogic = template.performsSpawnLogic();
            this.hardcore = template.hardcore().orElse(null);
            this.commands = template.commands().orElse(null);
            this.pvp = template.pvp().orElse(null);
            return this;
        }

        @Override
        protected WorldTemplate build0() {
            return new SpongeWorldTemplate(this);
        }
    }

    public static final class FactoryImpl implements WorldTemplate.Factory {

        @Override
        public WorldTemplate overworld() {
            return new BuilderImpl()
                    .reset()
                    .key(ResourceKey.minecraft("overworld"))
                    .worldType(WorldTypes.OVERWORLD)
                    .generator(ChunkGenerator.overworld())
                    .performsSpawnLogic(true)
                    .build();
        }

        @Override
        public WorldTemplate overworldCaves() {
            return new BuilderImpl()
                    .reset()
                    .key(ResourceKey.minecraft("overworld_caves"))
                    .worldType(WorldTypes.OVERWORLD_CAVES)
                    .generator(ChunkGenerator.noise(BiomeProvider.overworld(), NoiseGeneratorConfig.caves()))
                    .performsSpawnLogic(true)
                    .build();
        }

        @Override
        public WorldTemplate theNether() {
            return new BuilderImpl()
                    .reset()
                    .key(ResourceKey.minecraft("the_nether"))
                    .worldType(WorldTypes.THE_NETHER)
                    .generator(ChunkGenerator.theNether())
                    .build();
        }

        @Override
        public WorldTemplate theEnd() {
            return new BuilderImpl()
                    .reset()
                    .key(ResourceKey.minecraft("the_end"))
                    .worldType(WorldTypes.THE_END)
                    .generator(ChunkGenerator.theEnd())
                    .build();
        }
    }
}
