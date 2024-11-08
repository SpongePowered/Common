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
package org.spongepowered.common.registry.loader;

import com.google.common.base.CaseFormat;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Markings;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.ticks.TickPriority;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.type.ItemTier;
import org.spongepowered.api.data.type.ItemTiers;
import org.spongepowered.api.item.FireworkShape;
import org.spongepowered.api.item.FireworkShapes;
import org.spongepowered.api.registry.Registry;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.scoreboard.criteria.Criteria;
import org.spongepowered.api.scoreboard.criteria.Criterion;
import org.spongepowered.common.accessor.world.entity.boss.enderdragon.phases.EnderDragonPhaseAccessor;
import org.spongepowered.common.accessor.world.level.GameRulesAccessor;
import org.spongepowered.common.registry.RegistryLoader;
import org.spongepowered.common.registry.SpongeRegistryHolder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public final class VanillaRegistryLoader {
    private final SpongeRegistryHolder holder;

    public static void load(final SpongeRegistryHolder holder) {
        final VanillaRegistryLoader loader = new VanillaRegistryLoader(holder);
        loader.loadEnumRegistries();
        loader.loadInstanceRegistries();
    }

    private VanillaRegistryLoader(final SpongeRegistryHolder holder) {
        this.holder = holder;
    }

    private void loadInstanceRegistries() {
        this.holder.createRegistry(RegistryTypes.CRITERION, VanillaRegistryLoader.criterion());
        this.manualOrAutomaticName(RegistryTypes.DRAGON_PHASE_TYPE, EnderDragonPhaseAccessor.accessor$PHASES(), map -> {
            map.put(EnderDragonPhase.HOLDING_PATTERN, "holding_pattern");
            map.put(EnderDragonPhase.STRAFE_PLAYER, "strafe_player");
            map.put(EnderDragonPhase.LANDING_APPROACH, "landing_approach");
            map.put(EnderDragonPhase.LANDING, "landing");
            map.put(EnderDragonPhase.TAKEOFF, "takeoff");
            map.put(EnderDragonPhase.SITTING_FLAMING, "sitting_flaming");
            map.put(EnderDragonPhase.SITTING_SCANNING, "sitting_scanning");
            map.put(EnderDragonPhase.SITTING_ATTACKING, "sitting_attacking");
            map.put(EnderDragonPhase.CHARGING_PLAYER, "charging_player");
            map.put(EnderDragonPhase.DYING, "dying");
            map.put(EnderDragonPhase.HOVERING, "hover");
        }, phase -> CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, ((EnderDragonPhaseAccessor) phase).accessor$name()));
        this.holder.createRegistry(RegistryTypes.FIREWORK_SHAPE, VanillaRegistryLoader.fireworkShape());
//        final var materials = new HashMap<ArmorMaterial, String>();
//        materials.put(ArmorMaterials.LEATHER, ArmorMaterials.LEATHER.modelId().toString());
//        materials.put(ArmorMaterials.CHAIN, ArmorMaterials.CHAIN.modelId().toString());
//        materials.put(ArmorMaterials.IRON, ArmorMaterials.IRON.modelId().toString());
//        materials.put(ArmorMaterials.GOLD, ArmorMaterials.GOLD.modelId().toString());
//        materials.put(ArmorMaterials.DIAMOND, ArmorMaterials.DIAMOND.modelId().toString());
//        materials.put(ArmorMaterials.TURTLE_SCUTE, ResourceKey.minecraft("turtle").toString());
//        materials.put(ArmorMaterials.NETHERITE, ArmorMaterials.NETHERITE.modelId().toString());
//        materials.put(ArmorMaterials.ARMADILLO_SCUTE, ArmorMaterials.ARMADILLO_SCUTE.modelId().toString());
//
//        this.naming(RegistryTypes.ARMOR_MATERIAL, materials.keySet().toArray(new ArmorMaterial[]{}), materials);
        this.knownName(RegistryTypes.GAME_RULE, GameRulesAccessor.accessor$GAME_RULE_TYPES().keySet(), rule -> CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, rule.getId()));
        this.holder.createRegistry(RegistryTypes.ITEM_TIER, VanillaRegistryLoader.itemTier());
    }

    private void loadEnumRegistries() {
        this.automaticName(RegistryTypes.ATTACHMENT_SURFACE, AttachFace.values());
        this.automaticName(RegistryTypes.BAMBOO_LEAVES_TYPE, BambooLeaves.values());
        this.automaticName(RegistryTypes.BELL_ATTACHMENT_TYPE, BellAttachType.values());
        this.manualName(RegistryTypes.ATTRIBUTE_OPERATION, AttributeModifier.Operation.values(), map -> {
            // names come from net.minecraft.world.level.storage.loot.functions.SetAttributesFunction.Modifier#operationFromString
            map.put(AttributeModifier.Operation.ADD_VALUE, "addition");
            map.put(AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "multiply_base");
            map.put(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, "multiply_total");
        });
        this.automaticName(RegistryTypes.CHEST_ATTACHMENT_TYPE, ChestType.values());
        this.automaticName(RegistryTypes.COLLISION_RULE, Team.CollisionRule.values());
        this.automaticName(RegistryTypes.COMPARATOR_MODE, ComparatorMode.values());
        this.automaticName(RegistryTypes.DIFFICULTY, Difficulty.values());
        this.automaticName(RegistryTypes.DYE_COLOR, DyeColor.values());
        this.automaticName(RegistryTypes.DOOR_HINGE, DoorHingeSide.values());
        this.automaticName(RegistryTypes.DRIPSTONE_SEGMENT, DripstoneThickness.values());
        this.automaticName(RegistryTypes.EQUIPMENT_GROUP, EquipmentSlot.Type.values());
        this.automaticName(RegistryTypes.EQUIPMENT_TYPE, EquipmentSlot.values());
        this.automaticName(RegistryTypes.FOX_TYPE, Fox.Variant.values());
        this.automaticName(RegistryTypes.GAME_MODE, GameType.values());
        this.automaticName(RegistryTypes.HAND_PREFERENCE, HumanoidArm.values());
        this.automaticName(RegistryTypes.HAND_TYPE, InteractionHand.values());
        this.automaticName(RegistryTypes.INSTRUMENT_TYPE, NoteBlockInstrument.values());
        this.automaticName(RegistryTypes.ITEM_RARITY, Rarity.values());
        this.automaticName(RegistryTypes.JIGSAW_BLOCK_ORIENTATION, FrontAndTop.values());
        this.automaticName(RegistryTypes.MOOSHROOM_TYPE, MushroomCow.Variant.values());
        this.automaticName(RegistryTypes.OBJECTIVE_DISPLAY_MODE, ObjectiveCriteria.RenderType.values());
        this.automaticName(RegistryTypes.PANDA_GENE, Panda.Gene.values());
        this.automaticName(RegistryTypes.PHANTOM_PHASE, Phantom.AttackPhase.values());
        this.automaticName(RegistryTypes.PICKUP_RULE, AbstractArrow.Pickup.values());
        this.automaticName(RegistryTypes.MIRROR, Mirror.values());
        this.automaticName(RegistryTypes.CHAT_VISIBILITY, ChatVisiblity.values());
        this.automaticName(RegistryTypes.PISTON_TYPE, PistonType.values());
        this.automaticName(RegistryTypes.PORTION_TYPE, Half.values());
        this.automaticName(RegistryTypes.RAID_STATUS, Raid.RaidStatus.values());
        this.automaticName(RegistryTypes.ROTATION, Rotation.values());
        this.automaticName(RegistryTypes.RAIL_DIRECTION, RailShape.values());
        this.automaticName(RegistryTypes.SCULK_SENSOR_STATE, SculkSensorPhase.values());
        this.automaticName(RegistryTypes.SLAB_PORTION, SlabType.values());
        this.automaticName(RegistryTypes.SPELL_TYPE, SpellcasterIllager.IllagerSpell.values());
        this.automaticName(RegistryTypes.STAIR_SHAPE, StairsShape.values());
        this.automaticName(RegistryTypes.STRUCTURE_MODE, StructureMode.values());
        this.automaticName(RegistryTypes.TILT, Tilt.values());
        this.automaticName(RegistryTypes.TASK_PRIORITY, TickPriority.values());
        this.automaticName(RegistryTypes.VISIBILITY, Team.Visibility.values());
        this.automaticName(RegistryTypes.WIRE_ATTACHMENT_TYPE, RedstoneSide.values());
        this.automaticName(RegistryTypes.ADVANCEMENT_TYPE, AdvancementType.values());
        this.automaticName(RegistryTypes.TROPICAL_FISH_SHAPE, TropicalFish.Pattern.values());
        this.automaticName(RegistryTypes.HEIGHT_TYPE, Heightmap.Types.values());
        this.automaticName(RegistryTypes.ENTITY_CATEGORY, MobCategory.values());
        this.automaticName(RegistryTypes.WALL_CONNECTION_STATE, WallSide.values());
        this.automaticName(RegistryTypes.GRASS_COLOR_MODIFIER, BiomeSpecialEffects.GrassColorModifier.values());
        this.automaticName(RegistryTypes.PRECIPITATION, Biome.Precipitation.values());
        this.automaticName(RegistryTypes.TEMPERATURE_MODIFIER, Biome.TemperatureModifier.values());
        this.automaticName(RegistryTypes.DECORATION_STEP, GenerationStep.Decoration.values());
        this.automaticName(RegistryTypes.PARROT_TYPE, Parrot.Variant.values());
        this.automaticName(RegistryTypes.RABBIT_TYPE, Rabbit.Variant.values());
        this.automaticName(RegistryTypes.LLAMA_TYPE, Llama.Variant.values());
        this.automaticName(RegistryTypes.HORSE_COLOR, Variant.values());
        this.automaticName(RegistryTypes.HORSE_STYLE, Markings.values());
        this.automaticName(RegistryTypes.DAMAGE_SCALING, DamageScaling.values());
        this.automaticName(RegistryTypes.DAMAGE_EFFECT, DamageEffects.values());
        this.automaticName(RegistryTypes.ITEM_DISPLAY_TYPE, ItemDisplayContext.values());
        this.automaticName(RegistryTypes.BILLBOARD_TYPE, Display.BillboardConstraints.values());
        this.automaticName(RegistryTypes.TEXT_ALIGNMENT, Display.TextDisplay.Align.values());
        this.automaticName(RegistryTypes.LIGHT_TYPE, LightLayer.values());
        this.automaticName(RegistryTypes.DISPLAY_SLOT, DisplaySlot.values());
        this.automaticName(RegistryTypes.PUSH_REACTION, PushReaction.values());
        this.automaticName(RegistryTypes.TRIAL_SPAWNER_STATE, TrialSpawnerState.values());
        this.automaticName(RegistryTypes.VAULT_STATE, VaultState.values());
        this.automaticName(RegistryTypes.EXPLOSION_BLOCK_INTERACTION, Explosion.BlockInteraction.values());
    }

    private static RegistryLoader<Criterion> criterion() {
        return RegistryLoader.of(l -> {
            l.add(Criteria.AIR, k -> (Criterion) ObjectiveCriteria.AIR);
            l.add(Criteria.ARMOR, k -> (Criterion) ObjectiveCriteria.ARMOR);
            l.add(Criteria.DEATH_COUNT, k -> (Criterion) ObjectiveCriteria.DEATH_COUNT);
            l.add(Criteria.DUMMY, k -> (Criterion) ObjectiveCriteria.DUMMY);
            l.add(Criteria.EXPERIENCE, k -> (Criterion) ObjectiveCriteria.EXPERIENCE);
            l.add(Criteria.FOOD, k -> (Criterion) ObjectiveCriteria.FOOD);
            l.add(Criteria.HEALTH, k -> (Criterion) ObjectiveCriteria.HEALTH);
            l.add(Criteria.LEVEL, k -> (Criterion) ObjectiveCriteria.LEVEL);
            l.add(Criteria.PLAYER_KILL_COUNT, k -> (Criterion) ObjectiveCriteria.KILL_COUNT_PLAYERS);
            l.add(Criteria.TOTAL_KILL_COUNT, k -> (Criterion) ObjectiveCriteria.KILL_COUNT_ALL);
            l.add(Criteria.TRIGGER, k -> (Criterion) ObjectiveCriteria.TRIGGER);
        });
    }

    private static RegistryLoader<FireworkShape> fireworkShape() {
        return RegistryLoader.of(l -> {
            l.addWithId(FireworkExplosion.Shape.BURST.getId(), FireworkShapes.BURST, () -> (FireworkShape) (Object) FireworkExplosion.Shape.BURST);
            l.addWithId(FireworkExplosion.Shape.CREEPER.getId(), FireworkShapes.CREEPER, () -> (FireworkShape) (Object) FireworkExplosion.Shape.CREEPER);
            l.addWithId(FireworkExplosion.Shape.LARGE_BALL.getId(), FireworkShapes.LARGE_BALL, () -> (FireworkShape) (Object) FireworkExplosion.Shape.LARGE_BALL);
            l.addWithId(FireworkExplosion.Shape.SMALL_BALL.getId(), FireworkShapes.SMALL_BALL, () -> (FireworkShape) (Object) FireworkExplosion.Shape.SMALL_BALL);
            l.addWithId(FireworkExplosion.Shape.STAR.getId(), FireworkShapes.STAR, () -> (FireworkShape) (Object) FireworkExplosion.Shape.STAR);
        });
    }

    private static RegistryLoader<ItemTier> itemTier() {
        return RegistryLoader.of(l -> {
            l.add(ItemTiers.DIAMOND, k -> (ItemTier) (Object) ToolMaterial.DIAMOND);
            l.add(ItemTiers.GOLD, k -> (ItemTier) (Object) ToolMaterial.GOLD);
            l.add(ItemTiers.IRON, k -> (ItemTier) (Object) ToolMaterial.IRON);
            l.add(ItemTiers.NETHERITE, k -> (ItemTier) (Object) ToolMaterial.NETHERITE);
            l.add(ItemTiers.STONE, k -> (ItemTier) (Object) ToolMaterial.STONE);
            l.add(ItemTiers.WOOD, k -> (ItemTier) (Object) ToolMaterial.WOOD);
        });
    }

    // The following methods are named for clarity above.

    @SuppressWarnings("UnusedReturnValue")
    private <A, I extends Enum<I>> Registry<A> automaticName(final RegistryType<A> type, final I[] values) {
        final Function<I, String> keyFunction = i -> i instanceof final StringRepresentable s ? s.getSerializedName() : i.name();
        return this.naming(type, values, keyFunction.andThen(i -> i.toLowerCase(Locale.ROOT)));
    }

    @SuppressWarnings("UnusedReturnValue")
    private <A, I> Registry<A> knownName(final RegistryType<A> type, final Collection<I> values, final Function<I, String> name) {
        final Map<I, String> map = new HashMap<>();
        for (final I value : values) {
            map.put(value, name.apply(value));
        }
        return this.naming(type, values.size(), map);
    }

    @SuppressWarnings("UnusedReturnValue")
    private <A, I extends Enum<I>> Registry<A> manualName(final RegistryType<A> type, final I[] values, final Consumer<Manual<A, I>> byName) {
        final Map<I, String> map = new HashMap<>(values.length);
        byName.accept(map::put);
        return this.naming(type, values, map);
    }

    @SuppressWarnings("UnusedReturnValue")
    private <A, I> Registry<A> manualOrAutomaticName(final RegistryType<A> type, final I[] values, final Consumer<Manual<A, I>> byName, final Function<I, String> autoName) {
        final Map<I, String> map = new HashMap<>(values.length);
        byName.accept(map::put);
        for (final I value : values) {
            map.computeIfAbsent(value, autoName);
        }
        return this.naming(type, values, map);
    }

    @SuppressWarnings("UnusedReturnValue")
    private <A, I extends Enum<I>> Registry<A> naming(final RegistryType<A> type, final I[] values, final Function<I, String> name) {
        final Map<I, String> map = new HashMap<>();
        for (final I value : values) {
            map.put(value, name.apply(value));
        }
        return this.naming(type, values, map);
    }

    @SuppressWarnings("UnusedReturnValue")
    private <A, I> Registry<A> naming(final RegistryType<A> type, final I[] values, final Map<I, String> byName) {
        return this.naming(type, values.length, byName);
    }

    @SuppressWarnings("UnusedReturnValue")
    private <A, I> Registry<A> naming(final RegistryType<A> type, final int values, final Map<I, String> byName) {
        if (values != byName.size()) {
            throw new IllegalStateException(type.location() + " in " + type.root() + " is has value mismatch: " + values + " / " + byName.size());
        }
        final Registry<A> registry = this.holder.createRegistry(type, () -> {
            final Map<ResourceKey, A> map = new HashMap<>();
            for (final Map.Entry<I, String> value : byName.entrySet()) {
                final String rawId = value.getValue();
                // To address Vanilla shortcomings, some mods will manually prefix their modid onto values they put into Vanilla registry-like
                // registrars. We need to account for that possibility
                if (rawId.contains(":")) {
                    map.put((ResourceKey) (Object) ResourceLocation.parse(rawId), (A) value.getKey());
                } else {
                    map.put(ResourceKey.sponge(rawId), (A) value.getKey());
                }
            }
            return map;
        }, false);
        if (registry instanceof MappedRegistry<?> toFreeze) {
            toFreeze.freeze();
        }
        return registry;
    }

    @SuppressWarnings("unused")
    private interface Manual<A, I> {
        void put(final I value, final String key);
    }
}
