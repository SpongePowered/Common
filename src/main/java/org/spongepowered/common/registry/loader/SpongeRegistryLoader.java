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

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.adventure.ResolveOperation;
import org.spongepowered.api.adventure.ResolveOperations;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.transaction.Operation;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.data.KeyValueMatcher;
import org.spongepowered.api.data.persistence.DataFormat;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.type.BodyPart;
import org.spongepowered.api.data.type.BodyParts;
import org.spongepowered.api.data.type.MatterType;
import org.spongepowered.api.data.type.MatterTypes;
import org.spongepowered.api.data.type.NotePitch;
import org.spongepowered.api.data.type.NotePitches;
import org.spongepowered.api.data.type.SkinPart;
import org.spongepowered.api.data.type.SkinParts;
import org.spongepowered.api.effect.particle.ParticleOption;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.entity.ai.goal.GoalExecutorType;
import org.spongepowered.api.entity.ai.goal.GoalExecutorTypes;
import org.spongepowered.api.entity.ai.goal.GoalType;
import org.spongepowered.api.entity.ai.goal.GoalTypes;
import org.spongepowered.api.entity.ai.goal.builtin.LookAtGoal;
import org.spongepowered.api.entity.ai.goal.builtin.LookRandomlyGoal;
import org.spongepowered.api.entity.ai.goal.builtin.SwimGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.AttackLivingGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.AvoidLivingGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.RandomWalkingGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.RangedAttackAgainstAgentGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.horse.RunAroundLikeCrazyGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.target.FindNearestAttackableTargetGoal;
import org.spongepowered.api.event.cause.entity.DismountType;
import org.spongepowered.api.event.cause.entity.DismountTypes;
import org.spongepowered.api.event.cause.entity.MovementType;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.cause.entity.SpawnType;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.api.event.cause.entity.damage.DamageStepType;
import org.spongepowered.api.event.cause.entity.damage.DamageStepTypes;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.menu.ClickType;
import org.spongepowered.api.item.inventory.menu.ClickTypes;
import org.spongepowered.api.item.inventory.query.QueryType;
import org.spongepowered.api.item.inventory.query.QueryTypes;
import org.spongepowered.api.map.color.MapColorType;
import org.spongepowered.api.map.color.MapColorTypes;
import org.spongepowered.api.map.color.MapShade;
import org.spongepowered.api.map.color.MapShades;
import org.spongepowered.api.map.decoration.orientation.MapDecorationOrientation;
import org.spongepowered.api.map.decoration.orientation.MapDecorationOrientations;
import org.spongepowered.api.registry.RegistryKey;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.BanType;
import org.spongepowered.api.service.ban.BanTypes;
import org.spongepowered.api.service.economy.account.AccountDeletionResultType;
import org.spongepowered.api.service.economy.account.AccountDeletionResultTypes;
import org.spongepowered.api.service.economy.transaction.TransactionType;
import org.spongepowered.api.service.economy.transaction.TransactionTypes;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.util.orientation.Orientation;
import org.spongepowered.api.util.orientation.Orientations;
import org.spongepowered.api.world.ChunkRegenerateFlag;
import org.spongepowered.api.world.ChunkRegenerateFlags;
import org.spongepowered.api.world.PositionSource;
import org.spongepowered.api.world.generation.config.flat.FlatGeneratorConfig;
import org.spongepowered.api.world.generation.config.noise.NoiseConfig;
import org.spongepowered.api.world.generation.config.noise.NoiseConfigs;
import org.spongepowered.api.world.schematic.PaletteType;
import org.spongepowered.api.world.schematic.PaletteTypes;
import org.spongepowered.api.world.weather.WeatherType;
import org.spongepowered.api.world.weather.WeatherTypes;
import org.spongepowered.common.accessor.world.level.levelgen.NoiseSettingsAccessor;
import org.spongepowered.common.adventure.SpongeResolveOperation;
import org.spongepowered.common.ban.SpongeBanType;
import org.spongepowered.common.block.BlockStateSerializerDeserializer;
import org.spongepowered.common.block.transaction.BlockOperation;
import org.spongepowered.common.data.persistence.HoconDataFormat;
import org.spongepowered.common.data.persistence.JsonDataFormat;
import org.spongepowered.common.data.persistence.NBTDataFormat;
import org.spongepowered.common.data.persistence.SNBTDataFormat;
import org.spongepowered.common.data.type.SpongeBodyPart;
import org.spongepowered.common.data.type.SpongeMatterType;
import org.spongepowered.common.data.type.SpongeNotePitch;
import org.spongepowered.common.data.type.SpongeSkinPart;
import org.spongepowered.common.economy.SpongeAccountDeletionResultType;
import org.spongepowered.common.economy.SpongeTransactionType;
import org.spongepowered.common.effect.particle.SpongeParticleOption;
import org.spongepowered.common.entity.ai.SpongeGoalExecutorType;
import org.spongepowered.common.entity.ai.goal.SpongeGoalType;
import org.spongepowered.common.event.cause.entity.SpongeDismountType;
import org.spongepowered.common.event.cause.entity.SpongeMovementType;
import org.spongepowered.common.event.cause.entity.SpongeSpawnType;
import org.spongepowered.common.event.cause.entity.SpongeSpawnTypes;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageStepType;
import org.spongepowered.common.inventory.menu.handler.SpongeClickType;
import org.spongepowered.common.inventory.query.SpongeOneParamQueryType;
import org.spongepowered.common.inventory.query.SpongeQueryTypes;
import org.spongepowered.common.inventory.query.SpongeTwoParamQueryType;
import org.spongepowered.common.inventory.query.type.GridQuery;
import org.spongepowered.common.inventory.query.type.InventoryTypeQuery;
import org.spongepowered.common.inventory.query.type.ItemStackCustomQuery;
import org.spongepowered.common.inventory.query.type.ItemStackExactQuery;
import org.spongepowered.common.inventory.query.type.ItemStackIgnoreQuantityQuery;
import org.spongepowered.common.inventory.query.type.ItemTypeQuery;
import org.spongepowered.common.inventory.query.type.KeyValueMatcherQuery;
import org.spongepowered.common.inventory.query.type.LensQuery;
import org.spongepowered.common.inventory.query.type.PlayerPrimaryHotbarFirstQuery;
import org.spongepowered.common.inventory.query.type.ReverseQuery;
import org.spongepowered.common.inventory.query.type.SlotLensQuery;
import org.spongepowered.common.inventory.query.type.TypeQuery;
import org.spongepowered.common.inventory.query.type.UnionQuery;
import org.spongepowered.common.map.color.SpongeMapColorType;
import org.spongepowered.common.map.color.SpongeMapShade;
import org.spongepowered.common.map.decoration.orientation.SpongeMapDecorationOrientation;
import org.spongepowered.common.registry.RegistryLoader;
import org.spongepowered.common.util.SpongeOrientation;
import org.spongepowered.common.world.SpongeChunkRegenerateFlag;
import org.spongepowered.common.world.schematic.SpongePaletteType;
import org.spongepowered.common.world.weather.SpongeWeatherType;
import org.spongepowered.math.vector.Vector3d;

import java.util.function.Function;

@SuppressWarnings("unchecked")
public final class SpongeRegistryLoader {


    // @formatter:off

    public static RegistryLoader<AccountDeletionResultType> accountDeletionResultType() {
        return RegistryLoader.of(l -> l .mapping(SpongeAccountDeletionResultType::new, m -> m.add(
                AccountDeletionResultTypes.ABSENT,
                AccountDeletionResultTypes.FAILED,
                AccountDeletionResultTypes.SUCCESS,
                AccountDeletionResultTypes.UNDELETABLE
        )));
    }

    public static RegistryLoader<BanType> banType() {
        return RegistryLoader.of(l -> {
            l.add(BanTypes.IP, k -> new SpongeBanType(Ban.IP.class));
            l.add(BanTypes.PROFILE, k -> new SpongeBanType(Ban.Profile.class));
        });
    }

    public static RegistryLoader<BodyPart> bodyPart() {
        return RegistryLoader.of(l -> l.mapping(SpongeBodyPart::new, m -> m.add(
                BodyParts.CHEST,
                BodyParts.HEAD,
                BodyParts.LEFT_ARM,
                BodyParts.LEFT_LEG,
                BodyParts.RIGHT_ARM,
                BodyParts.RIGHT_LEG
        )));
    }

    public static RegistryLoader<ChunkRegenerateFlag> chunkRegenerateFlag() {
        return RegistryLoader.of(l -> {
            l.add(ChunkRegenerateFlags.NONE, k -> new SpongeChunkRegenerateFlag(false, false));
            l.add(ChunkRegenerateFlags.CREATE, k -> new SpongeChunkRegenerateFlag(true, false));
            l.add(ChunkRegenerateFlags.ENTITIES, k -> new SpongeChunkRegenerateFlag(false, true));
            l.add(ChunkRegenerateFlags.ALL, k -> new SpongeChunkRegenerateFlag(true, true));
        });
    }

    public static RegistryLoader<ClickType<?>> clickType() {
        return RegistryLoader.of(l -> l.mapping(SpongeClickType::new, m -> m.add(
                ClickTypes.CLICK_LEFT,
                ClickTypes.CLICK_LEFT_OUTSIDE,
                ClickTypes.CLICK_MIDDLE,
                ClickTypes.CLICK_RIGHT,
                ClickTypes.CLICK_RIGHT_OUTSIDE,
                ClickTypes.DOUBLE_CLICK,
                ClickTypes.DRAG_END,
                ClickTypes.DRAG_LEFT_ADD,
                ClickTypes.DRAG_MIDDLE_ADD,
                ClickTypes.DRAG_RIGHT_ADD,
                ClickTypes.DRAG_START,
                ClickTypes.KEY_SWAP,
                ClickTypes.KEY_THROW_ALL,
                ClickTypes.KEY_THROW_ONE,
                ClickTypes.SHIFT_CLICK_LEFT,
                ClickTypes.SHIFT_CLICK_RIGHT
        )));
    }

    public static RegistryLoader<DamageStepType> damageStepType() {
        return RegistryLoader.of(l -> l.mapping(SpongeDamageStepType::new, m -> m.add(
                DamageStepTypes.ABSORPTION,
                DamageStepTypes.ARMOR,
                DamageStepTypes.ARMOR_ENCHANTMENT,
                DamageStepTypes.BASE_COOLDOWN,
                DamageStepTypes.CRITICAL_HIT,
                DamageStepTypes.DEFENSIVE_POTION_EFFECT,
                DamageStepTypes.ENCHANTMENT_COOLDOWN,
                DamageStepTypes.FREEZING_BONUS,
                DamageStepTypes.HARD_HAT,
                DamageStepTypes.MAGIC,
                DamageStepTypes.NEGATIVE_POTION_EFFECT,
                DamageStepTypes.OFFENSIVE_POTION_EFFECT,
                DamageStepTypes.SHIELD,
                DamageStepTypes.SWEEPING,
                DamageStepTypes.WEAPON_BONUS,
                DamageStepTypes.WEAPON_ENCHANTMENT
        )));
    }


    public static RegistryLoader<DismountType> dismountType() {
        return RegistryLoader.of(l -> l.mapping(SpongeDismountType::new, m -> m.add(
                DismountTypes.DEATH,
                DismountTypes.DERAIL,
                DismountTypes.PLAYER
        )));
    }

    public static RegistryLoader<GoalExecutorType> goalExecutorType() {
        return RegistryLoader.of(l -> l.mapping(SpongeGoalExecutorType::new, m -> m.add(
                    GoalExecutorTypes.NORMAL,
                    GoalExecutorTypes.TARGET
        )));
    }

    public static RegistryLoader<GoalType> goalType() {
        return RegistryLoader.of(l -> {
            // See GoalTypeProvider
            l.add(GoalTypes.ATTACK_LIVING, k -> new SpongeGoalType(AttackLivingGoal.class));
            l.add(GoalTypes.AVOID_LIVING, k -> new SpongeGoalType(AvoidLivingGoal.class));
            l.add(GoalTypes.FIND_NEAREST_ATTACKABLE, k -> new SpongeGoalType(FindNearestAttackableTargetGoal.class));
            l.add(GoalTypes.LOOK_AT, k -> new SpongeGoalType(LookAtGoal.class));
            l.add(GoalTypes.LOOK_RANDOMLY, k -> new SpongeGoalType(LookRandomlyGoal.class));
            l.add(GoalTypes.RANDOM_WALKING, k -> new SpongeGoalType(RandomWalkingGoal.class));
            l.add(GoalTypes.RANGED_ATTACK_AGAINST_AGENT, k -> new SpongeGoalType(RangedAttackAgainstAgentGoal.class));
            l.add(GoalTypes.RUN_AROUND_LIKE_CRAZY, k -> new SpongeGoalType(RunAroundLikeCrazyGoal.class));
            l.add(GoalTypes.SWIM, k -> new SpongeGoalType(SwimGoal.class));
        });
    }

    public static RegistryLoader<MatterType> matterType() {
        return RegistryLoader.of(l -> l.mapping(SpongeMatterType::new, m -> m.add(
                MatterTypes.GAS,
                MatterTypes.LIQUID,
                MatterTypes.SOLID
        )));
    }

    public static RegistryLoader<MovementType> movementType() {
        return RegistryLoader.of(l -> l.mapping(SpongeMovementType::new, m -> m.add(
                MovementTypes.CHORUS_FRUIT,
                MovementTypes.COMMAND,
                MovementTypes.END_GATEWAY,
                MovementTypes.ENDER_PEARL,
                MovementTypes.ENTITY_TELEPORT,
                MovementTypes.NATURAL,
                MovementTypes.PLUGIN,
                MovementTypes.PORTAL
        )));
    }

    public static RegistryLoader<NotePitch> notePitch() {
        return RegistryLoader.of(l -> {
            l.addWithId(0, NotePitches.F_SHARP0, SpongeNotePitch::new);
            l.addWithId(1, NotePitches.G0, SpongeNotePitch::new);
            l.addWithId(2, NotePitches.G_SHARP0, SpongeNotePitch::new);
            l.addWithId(3, NotePitches.A1, SpongeNotePitch::new);
            l.addWithId(4, NotePitches.A_SHARP1, SpongeNotePitch::new);
            l.addWithId(5, NotePitches.B1, SpongeNotePitch::new);
            l.addWithId(6, NotePitches.C1, SpongeNotePitch::new);
            l.addWithId(7, NotePitches.C_SHARP1, SpongeNotePitch::new);
            l.addWithId(8, NotePitches.D1, SpongeNotePitch::new);
            l.addWithId(9, NotePitches.D_SHARP1, SpongeNotePitch::new);
            l.addWithId(10, NotePitches.E1, SpongeNotePitch::new);
            l.addWithId(11, NotePitches.F1, SpongeNotePitch::new);
            l.addWithId(12, NotePitches.F_SHARP1, SpongeNotePitch::new);
            l.addWithId(13, NotePitches.G1, SpongeNotePitch::new);
            l.addWithId(14, NotePitches.G_SHARP1, SpongeNotePitch::new);
            l.addWithId(15, NotePitches.A2, SpongeNotePitch::new);
            l.addWithId(16, NotePitches.A_SHARP2, SpongeNotePitch::new);
            l.addWithId(17, NotePitches.B2, SpongeNotePitch::new);
            l.addWithId(18, NotePitches.C2, SpongeNotePitch::new);
            l.addWithId(19, NotePitches.C_SHARP2, SpongeNotePitch::new);
            l.addWithId(20, NotePitches.D2, SpongeNotePitch::new);
            l.addWithId(21, NotePitches.D_SHARP2, SpongeNotePitch::new);
            l.addWithId(22, NotePitches.E2, SpongeNotePitch::new);
            l.addWithId(23, NotePitches.F2, SpongeNotePitch::new);
            l.addWithId(24, NotePitches.F_SHARP2, SpongeNotePitch::new);
        });
    }

    public static RegistryLoader<Operation> operation() {
        return RegistryLoader.of(l -> l.mapping(BlockOperation::new, m -> m.add(
                Operations.BREAK,
                Operations.DECAY,
                Operations.GROWTH,
                Operations.LIQUID_DECAY,
                Operations.LIQUID_SPREAD,
                Operations.MODIFY,
                Operations.PLACE
        )));
    }

    public static RegistryLoader<Orientation> orientation() {
        return RegistryLoader.of(l -> {
            l.add(Orientations.TOP, k -> new SpongeOrientation(0));
            l.add(Orientations.TOP_RIGHT, k -> new SpongeOrientation(45));
            l.add(Orientations.RIGHT, k -> new SpongeOrientation(90));
            l.add(Orientations.BOTTOM_RIGHT, k -> new SpongeOrientation(135));
            l.add(Orientations.BOTTOM, k -> new SpongeOrientation(180));
            l.add(Orientations.BOTTOM_LEFT, k -> new SpongeOrientation(225));
            l.add(Orientations.LEFT, k -> new SpongeOrientation(270));
            l.add(Orientations.TOP_LEFT, k -> new SpongeOrientation(315));
        });
    }

    public static RegistryLoader<PaletteType<?, ?>> paletteType() {
        return RegistryLoader.of(l -> {
            l.add(PaletteTypes.BIOME_PALETTE, k -> new SpongePaletteType<>(
                    (string, registry) -> registry.findValue(ResourceKey.resolve(string)),
                    (registry, biome) -> registry.valueKey(biome).toString()
            ));
            l.add(PaletteTypes.BLOCK_STATE_PALETTE, k -> new SpongePaletteType<>(
                    (string, registry) -> BlockStateSerializerDeserializer.deserialize(string),
                    (registry, blockState) -> BlockStateSerializerDeserializer.serialize(blockState)
            ));
            l.add(PaletteTypes.BLOCK_ENTITY_PALETTE, k -> new SpongePaletteType<>(
                (string, registry) -> registry.findValue(ResourceKey.resolve(string)),
                (registry, blockEntityType) -> registry.valueKey(blockEntityType).toString()
            ));
        });
    }

    public static RegistryLoader<ParticleOption<?>> particleOption() {
        return RegistryLoader.of(l -> {
            l.add(ParticleOptions.BLOCK_STATE, k -> new SpongeParticleOption<>(BlockState.class));
            l.add(ParticleOptions.COLOR, k -> new SpongeParticleOption<>(Color.class));
            l.add(ParticleOptions.DELAY, k -> new SpongeParticleOption<>(Double.class));
            l.add(ParticleOptions.DESTINATION, k -> new SpongeParticleOption<>(PositionSource.class));
            l.add(ParticleOptions.ITEM_STACK_SNAPSHOT, k -> new SpongeParticleOption<>(ItemStackSnapshot.class,
                    v -> v.isEmpty() ? new IllegalArgumentException("ItemStackSnapshot must not be empty") : null));
            l.add(ParticleOptions.OFFSET, k -> new SpongeParticleOption<>(Vector3d.class));
            l.add(ParticleOptions.OPACITY, k -> new SpongeParticleOption<>(Double.class,
                    v -> v < 0 || v > 1 ? new IllegalArgumentException("Opacity must be between 0 and 1") : null));
            l.add(ParticleOptions.QUANTITY, k -> new SpongeParticleOption<>(Integer.class,
                    v -> v < 1 ? new IllegalArgumentException("Quantity must be at least one") : null));
            l.add(ParticleOptions.ROLL, k -> new SpongeParticleOption<>(Double.class));
            l.add(ParticleOptions.SCALE, k -> new SpongeParticleOption<>(Double.class,
                    v -> v < 0 ? new IllegalArgumentException("Scale must not be negative") : null));
            l.add(ParticleOptions.TO_COLOR, k -> new SpongeParticleOption<>(Color.class));
            l.add(ParticleOptions.TRAVEL_TIME, k -> new SpongeParticleOption<>(Ticks.class));
            l.add(ParticleOptions.VELOCITY, k -> new SpongeParticleOption<>(Vector3d.class));
        });
    }

    public static RegistryLoader<QueryType> queryType() {
        return RegistryLoader.of(l -> {
            l.add(QueryTypes.GRID, k -> new SpongeTwoParamQueryType<>(GridQuery::new));
            l.add(QueryTypes.INVENTORY_TYPE, k -> new SpongeOneParamQueryType<>(InventoryTypeQuery::new));
            l.add(QueryTypes.ITEM_STACK_CUSTOM, k -> new SpongeOneParamQueryType<>(ItemStackCustomQuery::new));
            l.add(QueryTypes.ITEM_STACK_EXACT, k -> new SpongeOneParamQueryType<>(ItemStackExactQuery::new));
            l.add(QueryTypes.ITEM_STACK_IGNORE_QUANTITY, k -> new SpongeOneParamQueryType<>(ItemStackIgnoreQuantityQuery::new));
            l.add(QueryTypes.ITEM_TYPE, k -> new SpongeOneParamQueryType<>(ItemTypeQuery::new));
            l.add(QueryTypes.KEY_VALUE, k -> new SpongeOneParamQueryType<KeyValueMatcher<?>>(KeyValueMatcherQuery::new));
            l.add(SpongeQueryTypes.LENS, k -> new SpongeOneParamQueryType<>(LensQuery::new));
            l.add(QueryTypes.PLAYER_PRIMARY_HOTBAR_FIRST, PlayerPrimaryHotbarFirstQuery::new);
            l.add(QueryTypes.REVERSE, ReverseQuery::new);
            l.add(SpongeQueryTypes.SLOT_LENS, k -> new SpongeOneParamQueryType<>(SlotLensQuery::new));
            l.add(QueryTypes.TYPE, k -> new SpongeOneParamQueryType<>(TypeQuery::new));
            l.add(SpongeQueryTypes.UNION, k -> new SpongeOneParamQueryType<>(UnionQuery::new));
        });
    }

    public static RegistryLoader<ResolveOperation> resolveOperation() {
        return RegistryLoader.of(l -> {
            l.add(ResolveOperations.CONTEXTUAL_COMPONENTS, SpongeResolveOperation::newContextualComponents);
            l.add(ResolveOperations.CUSTOM_TRANSLATIONS, SpongeResolveOperation::newCustomTranslations);
        });
    }

    public static RegistryLoader<SkinPart> skinPart() {
        return RegistryLoader.of(l -> {
            l.add(SkinParts.CAPE, k -> new SpongeSkinPart("cape"));
            l.add(SkinParts.HAT, k -> new SpongeSkinPart("hat"));
            l.add(SkinParts.JACKET, k -> new SpongeSkinPart("jacket"));
            l.add(SkinParts.LEFT_PANTS_LEG, k -> new SpongeSkinPart("left_pants_leg"));
            l.add(SkinParts.LEFT_SLEEVE, k -> new SpongeSkinPart("left_sleeve"));
            l.add(SkinParts.RIGHT_PANTS_LEG, k -> new SpongeSkinPart("right_pants_leg"));
            l.add(SkinParts.RIGHT_SLEEVE, k -> new SpongeSkinPart("right_sleeve"));
        });
    }

    public static RegistryLoader<SpawnType> spawnType() {
        return RegistryLoader.<SpawnType>of(l -> {
            l.add(SpongeSpawnTypes.FORCED, k -> new SpongeSpawnType().setForced());
        }).mapping(SpongeSpawnType::new, m -> m.add(
                SpawnTypes.BLOCK_SPAWNING,
                SpawnTypes.BREEDING,
                SpawnTypes.CHUNK_LOAD,
                SpawnTypes.CUSTOM,
                SpawnTypes.DISPENSE,
                SpawnTypes.DROPPED_ITEM,
                SpongeSpawnTypes.ENTITY_DEATH,
                SpawnTypes.EXPERIENCE,
                SpawnTypes.FALLING_BLOCK,
                SpongeSpawnTypes.FORCED,
                SpawnTypes.MOB_SPAWNER,
                SpawnTypes.PASSIVE,
                SpawnTypes.PLACEMENT,
                SpawnTypes.PLUGIN,
                SpawnTypes.PROJECTILE,
                SpawnTypes.SPAWN_EGG,
                SpawnTypes.STRUCTURE,
                SpawnTypes.TNT_IGNITE,
                SpawnTypes.WEATHER,
                SpawnTypes.WORLD_SPAWNER
        ));
    }

    public static RegistryLoader<TransactionType> transactionType() {
        return RegistryLoader.of(l -> l.mapping(SpongeTransactionType::new, m -> {
                m.add(TransactionTypes.DEPOSIT);
                m.add(TransactionTypes.TRANSFER);
                m.add(TransactionTypes.WITHDRAW);
            }));
    }

    public static RegistryLoader<WeatherType> weather() {
        return RegistryLoader.of(l -> l.mapping(SpongeWeatherType::new, m -> m.add(
                WeatherTypes.CLEAR,
                WeatherTypes.RAIN,
                WeatherTypes.THUNDER
        )));
    }

    public static RegistryLoader<DataFormat> dataFormat() {
        return RegistryLoader.of(l -> {
            l.add(DataFormats.JSON, k -> new JsonDataFormat());
            l.add(DataFormats.HOCON, k -> new HoconDataFormat());
            l.add(DataFormats.SNBT, k -> new SNBTDataFormat());
            l.add(DataFormats.NBT, k -> new NBTDataFormat());
        });
    }

    public static RegistryLoader<MapColorType> mapColorType() {
        final Function<MapColor, Integer> colorId = mc -> mc.id;
        return RegistryLoader.of(l -> {
            l.addWithId(MapColor.NONE, MapColorTypes.NONE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.GRASS, MapColorTypes.GRASS, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.SAND, MapColorTypes.SAND, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.WOOL, MapColorTypes.WOOL, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.FIRE, MapColorTypes.FIRE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.ICE, MapColorTypes.ICE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.METAL, MapColorTypes.METAL, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.PLANT, MapColorTypes.PLANT, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.SNOW, MapColorTypes.SNOW, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.CLAY, MapColorTypes.CLAY, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.DIRT, MapColorTypes.DIRT, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.STONE, MapColorTypes.STONE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.WATER, MapColorTypes.WATER, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.WOOD, MapColorTypes.WOOD, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.QUARTZ, MapColorTypes.QUARTZ, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_ORANGE, MapColorTypes.COLOR_ORANGE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_MAGENTA, MapColorTypes.COLOR_MAGENTA, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_LIGHT_BLUE, MapColorTypes.COLOR_LIGHT_BLUE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_YELLOW, MapColorTypes.COLOR_YELLOW, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_LIGHT_GREEN, MapColorTypes.COLOR_LIGHT_GREEN, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_PINK, MapColorTypes.COLOR_PINK, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_GRAY, MapColorTypes.COLOR_GRAY, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_LIGHT_GRAY, MapColorTypes.COLOR_LIGHT_GRAY, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_CYAN, MapColorTypes.COLOR_CYAN, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_PURPLE, MapColorTypes.COLOR_PURPLE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_BLUE, MapColorTypes.COLOR_BLUE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_BROWN, MapColorTypes.COLOR_BROWN, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_GREEN, MapColorTypes.COLOR_GREEN, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_RED, MapColorTypes.COLOR_RED, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.COLOR_BLACK, MapColorTypes.COLOR_BLACK, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.GOLD, MapColorTypes.GOLD, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.DIAMOND, MapColorTypes.DIAMOND, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.LAPIS, MapColorTypes.LAPIS_LAZULI, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.EMERALD, MapColorTypes.EMERALD, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.PODZOL, MapColorTypes.PODZOL, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.NETHER, MapColorTypes.NETHER, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_WHITE, MapColorTypes.TERRACOTTA_WHITE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_ORANGE, MapColorTypes.TERRACOTTA_ORANGE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_MAGENTA, MapColorTypes.TERRACOTTA_MAGENTA, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_LIGHT_BLUE, MapColorTypes.TERRACOTTA_LIGHT_BLUE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_YELLOW, MapColorTypes.TERRACOTTA_YELLOW, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_LIGHT_GREEN, MapColorTypes.TERRACOTTA_LIGHT_GREEN, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_PINK, MapColorTypes.TERRACOTTA_PINK, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_GRAY, MapColorTypes.TERRACOTTA_GRAY, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_LIGHT_GRAY, MapColorTypes.TERRACOTTA_LIGHT_GRAY, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_CYAN, MapColorTypes.TERRACOTTA_CYAN, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_PURPLE, MapColorTypes.TERRACOTTA_PURPLE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_BLUE, MapColorTypes.TERRACOTTA_BLUE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_BROWN, MapColorTypes.TERRACOTTA_BROWN, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_GREEN, MapColorTypes.TERRACOTTA_GREEN, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_RED, MapColorTypes.TERRACOTTA_RED, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.TERRACOTTA_BLACK, MapColorTypes.TERRACOTTA_BLACK, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.CRIMSON_NYLIUM, MapColorTypes.CRIMSON_NYLIUM, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.CRIMSON_STEM, MapColorTypes.CRIMSON_STEM, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.CRIMSON_HYPHAE, MapColorTypes.CRIMSON_HYPHAE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.WARPED_NYLIUM, MapColorTypes.WARPED_NYLIUM, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.WARPED_STEM, MapColorTypes.WARPED_STEM, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.WARPED_HYPHAE, MapColorTypes.WARPED_HYPHAE, SpongeMapColorType::new, colorId);
            l.addWithId(MapColor.WARPED_WART_BLOCK, MapColorTypes.WARPED_WART_BLOCK, SpongeMapColorType::new, colorId);
        });
    }

    public static RegistryLoader<MapDecorationOrientation> mapDecorationOrientation() {
        return RegistryLoader.of(l -> {
            l.addWithId(0, MapDecorationOrientations.SOUTH, k -> new SpongeMapDecorationOrientation(0));
            l.addWithId(1, MapDecorationOrientations.SOUTH_SOUTHWEST, k -> new SpongeMapDecorationOrientation(1));
            l.addWithId(2, MapDecorationOrientations.SOUTHWEST, k -> new SpongeMapDecorationOrientation(2));
            l.addWithId(3, MapDecorationOrientations.WEST_SOUTHWEST, k -> new SpongeMapDecorationOrientation(3));
            l.addWithId(4, MapDecorationOrientations.WEST, k -> new SpongeMapDecorationOrientation(4));
            l.addWithId(5, MapDecorationOrientations.WEST_NORTHWEST, k -> new SpongeMapDecorationOrientation(5));
            l.addWithId(6, MapDecorationOrientations.NORTHWEST, k -> new SpongeMapDecorationOrientation(6));
            l.addWithId(7, MapDecorationOrientations.NORTH_NORTHWEST, k -> new SpongeMapDecorationOrientation(7));
            l.addWithId(8, MapDecorationOrientations.NORTH, k -> new SpongeMapDecorationOrientation(8));
            l.addWithId(9, MapDecorationOrientations.NORTH_NORTHEAST, k -> new SpongeMapDecorationOrientation(9));
            l.addWithId(10, MapDecorationOrientations.NORTHEAST, k -> new SpongeMapDecorationOrientation(10));
            l.addWithId(11, MapDecorationOrientations.EAST_NORTHEAST, k -> new SpongeMapDecorationOrientation(11));
            l.addWithId(12, MapDecorationOrientations.EAST, k -> new SpongeMapDecorationOrientation(12));
            l.addWithId(13, MapDecorationOrientations.EAST_SOUTHEAST, k -> new SpongeMapDecorationOrientation(13));
            l.addWithId(14, MapDecorationOrientations.SOUTHEAST, k -> new SpongeMapDecorationOrientation(14));
            l.addWithId(15, MapDecorationOrientations.SOUTH_SOUTHEAST, k -> new SpongeMapDecorationOrientation(15));
        });
    }

    public static RegistryLoader<MapShade> mapShade() {
        return RegistryLoader.of(l -> {
            l.addWithId(0, MapShades.BASE, k -> new SpongeMapShade(0, 180));
            l.addWithId(1, MapShades.DARK, k -> new SpongeMapShade(1, 220));
            l.addWithId(2, MapShades.DARKER, k -> new SpongeMapShade(2, 255));
            l.addWithId(3, MapShades.DARKEST, k -> new SpongeMapShade(3, 135));
        });
    }

    public static RegistryLoader<NoiseConfig> noiseConfig() {
        return RegistryLoader.of(l -> {
            l.add(NoiseConfigs.OVERWORLD, k -> (NoiseConfig) (Object) NoiseSettingsAccessor.accessor$OVERWORLD_NOISE_SETTINGS());
            l.add(NoiseConfigs.NETHER, k -> (NoiseConfig) (Object) NoiseSettingsAccessor.accessor$NETHER_NOISE_SETTINGS());
            l.add(NoiseConfigs.END, k -> (NoiseConfig) (Object) NoiseSettingsAccessor.accessor$END_NOISE_SETTINGS());
            l.add(NoiseConfigs.CAVES, k -> (NoiseConfig) (Object) NoiseSettingsAccessor.accessor$CAVES_NOISE_SETTINGS());
            l.add(NoiseConfigs.FLOATING_ISLANDS, k -> (NoiseConfig) (Object) NoiseSettingsAccessor.accessor$FLOATING_ISLANDS_NOISE_SETTINGS());
        });
    }

    public static RegistryLoader<FlatGeneratorConfig> flatGeneratorConfig(RegistryAccess registryAccess) {
        final Registry<FlatLevelGeneratorPreset> registry = registryAccess.lookupOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET);
        return RegistryLoader.of(l -> {
            for (final var entry : registry.entrySet()) {
                l.add(RegistryKey.of(RegistryTypes.FLAT_GENERATOR_CONFIG, (ResourceKey) (Object) entry.getKey().location()), () -> (FlatGeneratorConfig) entry.getValue().settings());
            }
        });
    }

    // @formatter:on
}
