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
package org.spongepowered.vanilla.generator;

import com.github.javaparser.utils.Log;
import com.google.common.base.CaseFormat;
import com.mojang.datafixers.util.Pair;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.WildcardTypeName;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Markings;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.scores.DisplaySlot;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * A generator that will output source code containing constants used in <em>Minecraft: Java Edition</em>.
 */
public final class GeneratorMain {

    private GeneratorMain() {
    }

    /**
     * The entry point.
     *
     * @param args arguments, expected to be {@code <output directory> }
     */
    public static void main(final String[] args) {
        Logger.info("Begining bootstrap");
        Log.setAdapter(new JavaparserLog());
        if (args.length != 2) {
            Logger.error("Invalid arguments. Usage: generator <outputDir> <licenseHeader>");
            System.exit(1);
            return;
        }

        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Bootstrap.validate();

        // Create a generator context based on arguments
        final var outputDir = Path.of(args[0]);
        final String licenseHeader;
        try(final var reader = Files.newBufferedReader(Path.of(args[1]), StandardCharsets.UTF_8)) {
            licenseHeader = reader.lines().map(line -> (" * " + line).stripTrailing()).collect(Collectors.joining("\n", "\n", "\n "));
        } catch (final IOException ex) {
            Logger.error("Failed to read license header file!", ex);
            System.exit(1);
            return;
        }

        final var dataPacks = GeneratorMain.loadVanillaDatapack();
        final var context = new Context(outputDir, dataPacks.getFirst(), dataPacks.getSecond(), licenseHeader);
        Logger.info("Generating data for Minecraft version {}", context.gameVersion());

        // Execute every generator
        boolean failed = false;
        for (final Generator generator : GeneratorMain.generators(context)) {
            try {
                Logger.info("Generating {}", generator.name());
                generator.generate(context);
            } catch (final Exception ex) {
                Logger.error(ex, "An unexpected error occurred while generating {} data", generator.name());
                failed = true;
            }
        }
        if (failed) {
            Logger.error("A failure occurred earlier in generating data. See log for details.");
            System.exit(1);
        }

        // Write modified files to disk
        context.complete();
        // Success!
        Logger.info("Successfully generated data!");
    }

    private static Pair<RegistryAccess.Frozen, ReloadableServerResources> loadVanillaDatapack() {
        // Load resource packs, see WorldStem.load
        // and call to WorldStem.load in net.minecraft.server.Main
        // We don't currently try to load any datapacks here
        final var packRepository = ServerPacksSource.createVanillaTrustedRepository();
        MinecraftServer.configurePackRepository(packRepository, WorldDataConfiguration.DEFAULT, /* safeMode = */ false, true);
        final CloseableResourceManager resourceManager = new MultiPackResourceManager(PackType.SERVER_DATA, packRepository.openAllSelected());

        // WorldLoader.load
        final LayeredRegistryAccess<RegistryLayer> staticRegistries = RegistryLayer.createRegistryAccess();
        final LayeredRegistryAccess<RegistryLayer> withWorldgen = staticRegistries.replaceFrom(
            RegistryLayer.WORLDGEN,
            RegistryDataLoader.load(resourceManager, staticRegistries.getAccessForLoading(RegistryLayer.WORLDGEN), RegistryDataLoader.WORLDGEN_REGISTRIES)
        );
        final LayeredRegistryAccess<RegistryLayer> withDimensions = withWorldgen.replaceFrom(
            RegistryLayer.DIMENSIONS,
            RegistryDataLoader.load(resourceManager, staticRegistries.getAccessForLoading(RegistryLayer.DIMENSIONS), RegistryDataLoader.DIMENSION_REGISTRIES)
        );


        final RegistryAccess.Frozen compositeRegistries = withDimensions.getAccessForLoading(RegistryLayer.RELOADABLE);
        final var resourcesFuture = ReloadableServerResources.loadResources(
            resourceManager,
            withDimensions,
            packRepository.getRequestedFeatureFlags(),
            CommandSelection.ALL,
            2, // functionPermissionLevel
            Util.backgroundExecutor(), // prepareExecutor
            Runnable::run // applyExecutor
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                resourceManager.close();
            }
        }).thenApply(resources -> {
            resources.updateRegistryTags();
            return resources;
        });

        Logger.info("Datapack load initiated");

        final ReloadableServerResources resources;
        try {
            resources = resourcesFuture.get();
        } catch (final InterruptedException | ExecutionException ex) {
            Logger.error(ex, "Failed to load registries/datapacks");
            System.exit(1);
            throw new RuntimeException();
        }

        Logger.info("Datapack load complete");

        return Pair.of(
            compositeRegistries,
            resources
        );
    }

    private static List<Generator> generators(final Context context) {
        // Prepare a set of generators
        // We are starting out by just generating Vanilla registry-backed catalogs
        // Enum-backed (automatically-named) catalogs can be added later as necessary
        return List.of(
            new MapEntriesValidator<>(
                "world.gamerule",
                "GameRules",
                GameRules.class,
                "GAME_RULE_TYPES",
                map -> {
                    final Map<ResourceLocation, Object> out = new HashMap<>(map.size());
                    map.forEach((BiConsumer<Object, Object>) (k, v) -> {
                        var key = (GameRules.Key<?>) k;
                        out.put(ResourceLocation.fromNamespaceAndPath("sponge", CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key.getId())), v);
                    });
                    return out;
                }
            ),
            new EnumEntriesValidator<>(
                 "item",
                 "FireworkShapes",
                  FireworkExplosion.Shape.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "entity",
                 "EntityCategories",
                 MobCategory.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "data.type",
                 "BoatTypes",
                 Boat.Type.class,
                 "getSerializedName",
                 "sponge"
            ),
            new RegistryEntriesGenerator<>(
                 "data.type",
                 "ArmorMaterials",
                 "ARMOR_MATERIAL",
                 context.relativeClass("data.type", "ArmorMaterial"),
                 Registries.ARMOR_MATERIAL
            ),
            new RegistryEntriesGenerator<>(
                 "map.decoration",
                 "MapDecorationTypes",
                 "MAP_DECORATION_TYPE",
                 context.relativeClass("map.decoration", "MapDecorationType"),
                 Registries.MAP_DECORATION_TYPE
            ),
            new EnumEntriesValidator<>(
                 "data.type",
                 "BambooLeavesTypes",
                 BambooLeaves.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "data.type",
                 "DyeColors",
                 DyeColor.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "world.difficulty",
                 "Difficulties",
                 Difficulty.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "data.type",
                 "PandaGenes",
                 Panda.Gene.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "data.type",
                 "SpellTypes",
                 SpellcasterIllager.IllagerSpell.class,
                 "name",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "data.type",
                 "RabbitTypes",
                 Rabbit.Variant.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "data.type",
                 "ParrotTypes",
                 Parrot.Variant.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "data.type",
                 "MooshroomTypes",
                 MushroomCow.MushroomType.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "data.type",
                 "FoxTypes",
                 Fox.Type.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "data.type",
                 "LlamaTypes",
                 Llama.Variant.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "data.type",
                 "HorseColors",
                 Variant.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "data.type",
                 "HorseStyles",
                 Markings.class,
                 "name",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "data.type",
                 "InstrumentTypes",
                 NoteBlockInstrument.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "item",
                 "ItemRarities",
                 Rarity.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "advancement",
                 "AdvancementTypes",
                 AdvancementType.class,
                 "getSerializedName",
                 "sponge"
            ),
            new EnumEntriesValidator<>(
                 "data.type",
                 "TropicalFishShapes",
                 TropicalFish.Pattern.class,
                 "getSerializedName",
                 "sponge"
            ),
            new RegistryEntriesGenerator<>(
                "data.type",
                "ArtTypes",
                "ART_TYPE",
                context.relativeClass("data.type", "ArtType"),
                Registries.PAINTING_VARIANT,
                a -> true, RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                "entity.attribute.type",
                "AttributeTypes",
                "ATTRIBUTE_TYPE",
                context.relativeClass("entity.attribute.type", "RangedAttributeType"),
                Registries.ATTRIBUTE,
                a -> true, null,
                context.relativeClass("entity.attribute.type", "AttributeType")
            ),
            new RegistryEntriesGenerator<>(
                "world.biome",
                "Biomes",
                "BIOME",
                context.relativeClass("world.biome", "Biome"),
                Registries.BIOME
            ),
            new RegistryEntriesGenerator<>(
                "world.generation.carver",
                "CarverTypes",
                "CARVER_TYPE",
                context.relativeClass("world.generation.carver", "CarverType"),
                Registries.CARVER
            ),
            new RegistryEntriesGenerator<>(
                "world.generation.carver",
                "Carvers",
                "CARVER",
                context.relativeClass("world.generation.carver", "Carver"),
                Registries.CONFIGURED_CARVER,
                a -> true, RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                "world.generation.config.noise",
                "NoiseGeneratorConfigs",
                "NOISE_GENERATOR_CONFIG",
                context.relativeClass("world.generation.config.noise", "NoiseGeneratorConfig"),
                Registries.NOISE_SETTINGS,
                a -> true, RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                "world.generation.config.flat",
                "FlatGeneratorConfigs",
                "FLAT_GENERATOR_CONFIG",
                context.relativeClass("world.generation.config.flat", "FlatGeneratorConfig"),
                Registries.FLAT_LEVEL_GENERATOR_PRESET,
                a -> true, RegistryScope.GAME
            ),
            new RegistryEntriesGenerator<>(
                "world.generation.config.noise",
                "Noises",
                "NOISE",
                context.relativeClass("world.generation.config.noise", "Noise"),
                Registries.NOISE,
                a -> true, RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                "world.generation.config.noise",
                "DensityFunctions",
                "DENSITY_FUNCTION",
                context.relativeClass("world.generation.config.noise", "DensityFunction"),
                Registries.DENSITY_FUNCTION,
                a -> true, RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                "block",
                "BlockTypes",
                "BLOCK_TYPE",
                context.relativeClass("block", "BlockType"),
                Registries.BLOCK
            ),
            new RegistryEntriesGenerator<>(
                "block.entity",
                "BlockEntityTypes",
                "BLOCK_ENTITY_TYPE",
                context.relativeClass("block.entity", "BlockEntityType"),
                Registries.BLOCK_ENTITY_TYPE
            ),
            new RegistryEntriesValidator<>(
                "world.chunk",
                "ChunkStates",
                Registries.CHUNK_STATUS
            ),
            /*new RegistryEntriesValidator<>( // todo: has special ordering
                "item.inventory",
                "ContainerTypes",
                Registry.MENU_REGISTRY
            ),*/
            new RegistryEntriesValidator<>(
                "item.enchantment",
                "EnchantmentTypes",
                Registries.ENCHANTMENT
            ),
            new RegistryEntriesValidator<>(
                "entity",
                "EntityTypes",
                Registries.ENTITY_TYPE,
                $ -> true,
                Set.of(ResourceLocation.fromNamespaceAndPath("sponge", "human")) // Sponge's Human type is an extra addition
            ),
            new RegistryEntriesGenerator<>(
                "fluid",
                "FluidTypes",
                "FLUID_TYPE",
                context.relativeClass("fluid", "FluidType"),
                Registries.FLUID
            ),
            new RegistryEntriesGenerator<>(
                "item",
                "ItemTypes",
                "ITEM_TYPE",
                context.relativeClass("item", "ItemType"),
                Registries.ITEM
            ),
            new RegistryEntriesGenerator<>(
                "effect.particle",
                "ParticleTypes",
                "PARTICLE_TYPE",
                context.relativeClass("effect.particle", "ParticleType"),
                Registries.PARTICLE_TYPE
            ),
            new RegistryEntriesGenerator<>(
                "item.potion",
                "PotionTypes",
                "POTION_TYPE",
                context.relativeClass("item.potion", "PotionType"),
                Registries.POTION
            ),
            new RegistryEntriesGenerator<>(
                "effect.potion",
                "PotionEffectTypes",
                "POTION_EFFECT_TYPE",
                context.relativeClass("effect.potion", "PotionEffectType"),
                Registries.MOB_EFFECT
            ),
            new RegistryEntriesGenerator<>(
                "data.type",
                "ProfessionTypes",
                "PROFESSION_TYPE",
                context.relativeClass("data.type", "ProfessionType"),
                Registries.VILLAGER_PROFESSION
            ),
            new RegistryEntriesGenerator<>(
                "data.type",
                "BannerPatternShapes",
                "BANNER_PATTERN_SHAPE",
                context.relativeClass("data.type", "BannerPatternShape"),
                Registries.BANNER_PATTERN,
                $ -> true,
                RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                "data.type",
                "CatTypes",
                "CAT_TYPE",
                context.relativeClass("data.type", "CatType"),
                Registries.CAT_VARIANT
            ),
            new RegistryEntriesGenerator<>(
                "data.type",
                "FrogTypes",
                "FROG_TYPE",
                context.relativeClass("data.type", "FrogType"),
                Registries.FROG_VARIANT
            ),
            new RegistryEntriesGenerator<>(
                "effect.sound.music",
                "MusicDiscs",
                "MUSIC_DISC",
                context.relativeClass("effect.sound.music", "MusicDisc"),
                Registries.JUKEBOX_SONG,
                $ -> true,
                RegistryScope.SERVER
            ),
            new RegistryEntriesValidator<>(
                "item.recipe",
                "RecipeTypes",
                Registries.RECIPE_TYPE
            ),
            new RegistryEntriesGenerator<>(
                "effect.sound",
                "SoundTypes",
                "SOUND_TYPE",
                context.relativeClass("effect.sound", "SoundType"),
                Registries.SOUND_EVENT
            ),
            new RegistryEntriesGenerator<>(
                "statistic",
                "Statistics",
                "STATISTIC",
                context.relativeClass("statistic", "Statistic"),
                Registries.CUSTOM_STAT
            ),
            new RegistryEntriesValidator<>(
                "statistic",
                "StatisticCategories",
                Registries.STAT_TYPE
            ),
            new RegistryEntriesGenerator<>(
                "world.generation.structure",
                "Structures",
                "STRUCTURE",
                context.relativeClass("world.generation.structure", "Structure"),
                Registries.STRUCTURE,
                $ -> true, RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                "world.generation.structure",
                "StructureTypes",
                "STRUCTURE_TYPE",
                context.relativeClass("world.generation.structure", "StructureType"),
                Registries.STRUCTURE_TYPE
            ),
            new RegistryEntriesGenerator<>(
                "world.generation.structure",
                "StructureSets",
                "STRUCTURE_SET",
                context.relativeClass("world.generation.structure", "StructureSet"),
                Registries.STRUCTURE_SET,
                $ -> true, RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                "world.generation.structure.jigsaw",
                "JigsawPools",
                "JIGSAW_POOL",
                context.relativeClass("world.generation.structure.jigsaw", "JigsawPool"),
                Registries.TEMPLATE_POOL,
                $ -> true, RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                "world.generation.structure.jigsaw",
                "ProcessorLists",
                "PROCESSOR_LIST",
                context.relativeClass("world.generation.structure.jigsaw", "ProcessorList"),
                Registries.PROCESSOR_LIST,
                $ -> true, RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                "world.generation.structure.jigsaw",
                "ProcessorTypes",
                "PROCESSOR_TYPE",
                context.relativeClass("world.generation.structure.jigsaw", "ProcessorType"),
                Registries.STRUCTURE_PROCESSOR,
                $ -> true, RegistryScope.GAME
            ),
            new RegistryEntriesGenerator<>(
                "advancement.criteria.trigger",
                "Triggers",
                "TRIGGER",
                ParameterizedTypeName.get(context.relativeClass("advancement.criteria.trigger", "Trigger"), WildcardTypeName.subtypeOf(Object.class)),
                Registries.TRIGGER_TYPE,
                $ -> true,
                RegistryScope.GAME
            ),
            new RegistryEntriesGenerator<>(
                "item.recipe.smithing",
                "TrimMaterials",
                "TRIM_MATERIAL",
                context.relativeClass("item.recipe.smithing", "TrimMaterial"),
                Registries.TRIM_MATERIAL,
                $ ->true,
                RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                "item.recipe.smithing",
                "TrimPatterns",
                "TRIM_PATTERN",
                context.relativeClass("item.recipe.smithing", "TrimPattern"),
                Registries.TRIM_PATTERN,
                $ -> true,
                RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                "data.type",
                "VillagerTypes",
                "VILLAGER_TYPE",
                context.relativeClass("data.type", "VillagerType"),
                Registries.VILLAGER_TYPE
            ),
            new RegistryEntriesGenerator<>(
                "world",
                "WorldTypes",
                "WORLD_TYPE",
                context.relativeClass("world", "WorldType"),
                Registries.DIMENSION_TYPE,
                $ -> true,
                RegistryScope.SERVER
            ),
            new BlockStateDataProviderGenerator(),
            new BlockStatePropertiesGenerator(),
            new BlockStatePropertyKeysGenerator(),
            new RegistryEntriesGenerator<>(
                    "world.generation.feature",
                    "PlacedFeatures",
                    "PLACED_FEATURE",
                    context.relativeClass("world.generation.feature", "PlacedFeature"),
                    Registries.PLACED_FEATURE,
                    $ -> true, RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                    "world.generation.feature",
                    "Features",
                    "FEATURE",
                    context.relativeClass("world.generation.feature", "Feature"),
                    Registries.CONFIGURED_FEATURE,
                    $ -> true, RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                    "world.generation.feature",
                    "FeatureTypes",
                    "FEATURE_TYPE",
                    context.relativeClass("world.generation.feature", "FeatureType"),
                    Registries.FEATURE,
                    $ -> true, RegistryScope.SERVER
            ),
            new RegistryEntriesGenerator<>(
                    "world.generation.feature",
                    "PlacementModifierTypes",
                    "PLACEMENT_MODIFIER",
                    context.relativeClass("world.generation.feature", "PlacementModifierType"),
                    Registries.PLACEMENT_MODIFIER_TYPE,
                    $ -> true, RegistryScope.SERVER
            ),
            new TagGenerator(
                    "BLOCK_TYPE",
                    Registries.BLOCK,
                    context.relativeClass("block", "BlockType"),
                    "tag",
                    "BlockTypeTags"
            ),
            new TagGenerator(
                    "BIOME",
                    Registries.BIOME,
                    context.relativeClass("world.biome", "Biome"),
                    "tag",
                    "BiomeTags"
            ),
            new TagGenerator(
                    "ITEM_TYPE",
                    Registries.ITEM,
                    context.relativeClass("item", "ItemType"),
                    "tag",
                    "ItemTypeTags"
            ),
            new TagGenerator(
                    "ENTITY_TYPE",
                    Registries.ENTITY_TYPE,
                    ParameterizedTypeName.get(context.relativeClass("entity", "EntityType"), WildcardTypeName.subtypeOf(Object.class)),
                    "tag",
                    "EntityTypeTags"
            ),
            new TagGenerator(
                    "FLUID_TYPE",
                    Registries.FLUID,
                    context.relativeClass("fluid", "FluidType"),
                    "tag",
                    "FluidTypeTags"
            ),
            new TagGenerator(
                    "ENCHANTMENT_TYPE",
                    Registries.ENCHANTMENT,
                    context.relativeClass("item.enchantment", "EnchantmentType"),
                    "tag",
                    "EnchantmenTypeTags"
            ),
            new RegistryEntriesGenerator<>(
                    "event.cause.entity.damage",
                    "DamageTypes",
                    "DAMAGE_TYPE",
                    context.relativeClass("event.cause.entity.damage", "DamageType"),
                    Registries.DAMAGE_TYPE,
                    a -> true, RegistryScope.SERVER
            ),
            new TagGenerator(
                    "DAMAGE_TYPE",
                    Registries.DAMAGE_TYPE,
                    context.relativeClass("event.cause.entity.damage", "DamageType"),
                    "tag",
                    "DamageTypeTags"
            ),
            new EnumEntriesValidator<>(
                    "event.cause.entity.damage",
                    "DamageEffects",
                    DamageEffects.class,
                    "getSerializedName",
                    "sponge"
            ),
            new EnumEntriesValidator<>(
                    "event.cause.entity.damage",
                    "DamageScalings",
                    DamageScaling.class,
                    "getSerializedName",
                    "sponge"
            ),
            new EnumEntriesValidator<>(
                    "entity.display",
                    "ItemDisplayTypes",
                    ItemDisplayContext.class,
                    "getSerializedName",
                    "sponge"
            ),
            new EnumEntriesValidator<>(
                    "data.type",
                    "PushReactions",
                    PushReaction.class,
                    "name",
                    "sponge"
            ),
            new EnumEntriesValidator<>(
                    "item.inventory.equipment",
                    "EquipmentGroups",
                    EquipmentSlot.Type.class,
                    "name",
                    "sponge"
            ),
            new EnumEntriesValidator<>(
                    "data.type",
                    "DripstoneSegments",
                    DripstoneThickness.class,
                    "getSerializedName",
                    "sponge"
            ),
            new EnumEntriesValidator<>(
                    "data.type",
                    "ItemTiers",
                    Tiers.class,
                    "name",
                    "sponge"
            ),
            new EnumEntriesValidator<>(
                    "entity.display",
                    "BillboardTypes",
                    Display.BillboardConstraints.class,
                    "getSerializedName",
                    "sponge"
            ),
            new EnumEntriesValidator<>(
                    "data.type",
                    "Tilts",
                    Tilt.class,
                    "getSerializedName",
                    "sponge"
            ),
            new EnumEntriesValidator<>(
                    "scoreboard.displayslot",
                    "DisplaySlots",
                    DisplaySlot.class,
                    "getSerializedName",
                    "sponge"
            ),
            new EnumEntriesValidator<>(
                    "data.type",
                    "SculkSensorStates",
                    SculkSensorPhase.class,
                    "getSerializedName",
                    "sponge"
            ),
            new EnumEntriesValidator<>(
                    "data.type",
                    "TrialSpawnerStates",
                    TrialSpawnerState.class,
                    "getSerializedName",
                    "sponge"
            ),
            new EnumEntriesValidator<>(
                    "item.inventory.equipment",
                    "EquipmentTypes",
                    EquipmentSlot.class,
                    "getSerializedName",
                    "sponge"
            ),
            new EnumEntriesValidator<>(
                    "entity.display",
                    "TextAlignments",
                    Display.TextDisplay.Align.class,
                    "getSerializedName",
                    "sponge"
            )
        );
    }
}
