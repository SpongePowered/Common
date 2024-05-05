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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
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
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.data.type.MatterType;
import org.spongepowered.api.data.type.MatterTypes;
import org.spongepowered.api.data.type.NotePitch;
import org.spongepowered.api.data.type.NotePitches;
import org.spongepowered.api.data.type.SkinPart;
import org.spongepowered.api.data.type.SkinParts;
import org.spongepowered.api.effect.particle.ParticleOption;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.potion.PotionEffectType;
import org.spongepowered.api.effect.sound.music.MusicDisc;
import org.spongepowered.api.effect.sound.music.MusicDiscs;
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
import org.spongepowered.api.event.cause.entity.damage.DamageModifierType;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.menu.ClickType;
import org.spongepowered.api.item.inventory.menu.ClickTypes;
import org.spongepowered.api.item.inventory.query.QueryType;
import org.spongepowered.api.item.inventory.query.QueryTypes;
import org.spongepowered.api.map.color.MapColorType;
import org.spongepowered.api.map.color.MapColorTypes;
import org.spongepowered.api.map.color.MapShade;
import org.spongepowered.api.map.color.MapShades;
import org.spongepowered.api.map.decoration.MapDecorationType;
import org.spongepowered.api.map.decoration.MapDecorationTypes;
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
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.util.orientation.Orientation;
import org.spongepowered.api.util.orientation.Orientations;
import org.spongepowered.api.world.ChunkRegenerateFlag;
import org.spongepowered.api.world.ChunkRegenerateFlags;
import org.spongepowered.api.world.generation.config.flat.FlatGeneratorConfig;
import org.spongepowered.api.world.generation.config.noise.NoiseConfig;
import org.spongepowered.api.world.generation.config.noise.NoiseConfigs;
import org.spongepowered.api.world.portal.PortalType;
import org.spongepowered.api.world.portal.PortalTypes;
import org.spongepowered.api.world.schematic.PaletteType;
import org.spongepowered.api.world.schematic.PaletteTypes;
import org.spongepowered.api.world.server.TicketType;
import org.spongepowered.api.world.server.TicketTypes;
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
import org.spongepowered.common.effect.record.SpongeMusicDisc;
import org.spongepowered.common.entity.ai.SpongeGoalExecutorType;
import org.spongepowered.common.entity.ai.goal.SpongeGoalType;
import org.spongepowered.common.event.cause.entity.SpongeDismountType;
import org.spongepowered.common.event.cause.entity.SpongeMovementType;
import org.spongepowered.common.event.cause.entity.SpongeSpawnType;
import org.spongepowered.common.event.cause.entity.SpongeSpawnTypes;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageModifierType;
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
import org.spongepowered.common.map.decoration.SpongeMapDecorationBannerType;
import org.spongepowered.common.map.decoration.SpongeMapDecorationType;
import org.spongepowered.common.map.decoration.orientation.SpongeMapDecorationOrientation;
import org.spongepowered.common.registry.RegistryLoader;
import org.spongepowered.common.util.SpongeOrientation;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.SpongeChunkRegenerateFlag;
import org.spongepowered.common.world.portal.EndPortalType;
import org.spongepowered.common.world.portal.NetherPortalType;
import org.spongepowered.common.world.portal.UnknownPortalType;
import org.spongepowered.common.world.schematic.SpongePaletteType;
import org.spongepowered.common.world.server.SpongeTicketType;
import org.spongepowered.common.world.weather.SpongeWeatherType;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.Comparator;
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

    public static RegistryLoader<DamageModifierType> damageModifierType() {
        return RegistryLoader.of(l -> l.mapping(SpongeDamageModifierType::new, m -> m.add(
                DamageModifierTypes.ABSORPTION,
                DamageModifierTypes.ARMOR,
                DamageModifierTypes.ARMOR_ENCHANTMENT,
                DamageModifierTypes.ATTACK_COOLDOWN,
                DamageModifierTypes.CRITICAL_HIT,
                DamageModifierTypes.DEFENSIVE_POTION_EFFECT,
                DamageModifierTypes.DIFFICULTY,
                DamageModifierTypes.HARD_HAT,
                DamageModifierTypes.MAGIC,
                DamageModifierTypes.NEGATIVE_POTION_EFFECT,
                DamageModifierTypes.OFFENSIVE_POTION_EFFECT,
                DamageModifierTypes.SHIELD,
                DamageModifierTypes.SWEEPING,
                DamageModifierTypes.WEAPON_ENCHANTMENT
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

    public static RegistryLoader<MusicDisc> musicDisc() {
        return RegistryLoader.of(l -> {
            // TODO ItemTags.MUSIC_DISCS to check for completion
            l.add(MusicDiscs.BLOCKS, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_BLOCKS));
            l.add(MusicDiscs.CAT, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_CAT));
            l.add(MusicDiscs.CHIRP, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_CHIRP));
            l.add(MusicDiscs.FAR, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_FAR));
            l.add(MusicDiscs.MALL, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_MALL));
            l.add(MusicDiscs.MELLOHI, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_MELLOHI));
            l.add(MusicDiscs.MUSIC_DISC_5, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_5));
            l.add(MusicDiscs.MUSIC_DISC_11, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_11));
            l.add(MusicDiscs.MUSIC_DISC_13, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_13));
            l.add(MusicDiscs.OTHERSIDE, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_OTHERSIDE));
            l.add(MusicDiscs.PIGSTEP, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_PIGSTEP));
            l.add(MusicDiscs.STAL, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_STAL));
            l.add(MusicDiscs.STRAD, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_STRAD));
            l.add(MusicDiscs.WAIT, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_WAIT));
            l.add(MusicDiscs.WARD, k -> new SpongeMusicDisc((RecordItem) Items.MUSIC_DISC_WARD));
        });
    }

    public static RegistryLoader<NotePitch> notePitch() {
        return RegistryLoader.of(l -> {
            l.add(0, NotePitches.F_SHARP0, SpongeNotePitch::new);
            l.add(1, NotePitches.G0, SpongeNotePitch::new);
            l.add(2, NotePitches.G_SHARP0, SpongeNotePitch::new);
            l.add(3, NotePitches.A1, SpongeNotePitch::new);
            l.add(4, NotePitches.A_SHARP1, SpongeNotePitch::new);
            l.add(5, NotePitches.B1, SpongeNotePitch::new);
            l.add(6, NotePitches.C1, SpongeNotePitch::new);
            l.add(7, NotePitches.C_SHARP1, SpongeNotePitch::new);
            l.add(8, NotePitches.D1, SpongeNotePitch::new);
            l.add(9, NotePitches.D_SHARP1, SpongeNotePitch::new);
            l.add(10, NotePitches.E1, SpongeNotePitch::new);
            l.add(11, NotePitches.F1, SpongeNotePitch::new);
            l.add(12, NotePitches.F_SHARP1, SpongeNotePitch::new);
            l.add(13, NotePitches.G1, SpongeNotePitch::new);
            l.add(14, NotePitches.G_SHARP1, SpongeNotePitch::new);
            l.add(15, NotePitches.A2, SpongeNotePitch::new);
            l.add(16, NotePitches.A_SHARP2, SpongeNotePitch::new);
            l.add(17, NotePitches.B2, SpongeNotePitch::new);
            l.add(18, NotePitches.C2, SpongeNotePitch::new);
            l.add(19, NotePitches.C_SHARP2, SpongeNotePitch::new);
            l.add(20, NotePitches.D2, SpongeNotePitch::new);
            l.add(21, NotePitches.D_SHARP2, SpongeNotePitch::new);
            l.add(22, NotePitches.E2, SpongeNotePitch::new);
            l.add(23, NotePitches.F2, SpongeNotePitch::new);
            l.add(24, NotePitches.F_SHARP2, SpongeNotePitch::new);
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
            l.add(ParticleOptions.DIRECTION, k -> new SpongeParticleOption<>(Direction.class));
            l.add(ParticleOptions.ITEM_STACK_SNAPSHOT, k -> new SpongeParticleOption<>(ItemStackSnapshot.class));
            l.add(ParticleOptions.OFFSET, k -> new SpongeParticleOption<>(Vector3d.class));
            l.add(ParticleOptions.POTION_EFFECT_TYPE, k -> new SpongeParticleOption<>(PotionEffectType.class));
            l.add(ParticleOptions.QUANTITY, k -> new SpongeParticleOption<>(Integer.class, v -> v < 1 ? new IllegalArgumentException("Quantity must be at least one") : null));
            l.add(ParticleOptions.ROLL, k -> new SpongeParticleOption<>(Double.class));
            l.add(ParticleOptions.SCALE, k -> new SpongeParticleOption<>(Double.class, v -> v < 0 ? new IllegalArgumentException("Scale must not be negative") : null));
            l.add(ParticleOptions.TO_COLOR, k -> new SpongeParticleOption<>(Color.class));
            l.add(ParticleOptions.TRAVEL_TIME, k -> new SpongeParticleOption<>(Ticks.class));
            l.add(ParticleOptions.VELOCITY, k -> new SpongeParticleOption<>(Vector3d.class));
        });
    }

    public static RegistryLoader<PortalType> portalType() {
        return RegistryLoader.of(l -> {
            l.add(PortalTypes.END, EndPortalType::new);
            l.add(PortalTypes.NETHER, NetherPortalType::new);
            l.add(PortalTypes.UNKNOWN, UnknownPortalType::new);
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

    public static RegistryLoader<TicketType<?>> ticketType() {
        return RegistryLoader.of(l -> {
            l.add(TicketTypes.STANDARD, k -> new SpongeTicketType<Vector3i>("standard", Comparator.comparingLong(x -> VecHelper.toChunkPos(x).toLong()), 1));
            l.add(TicketTypes.PORTAL, k -> (TicketType<?>) net.minecraft.server.level.TicketType.PORTAL);
            l.add(TicketTypes.POST_TELEPORT, k -> (TicketType<?>) net.minecraft.server.level.TicketType.POST_TELEPORT);
        });
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
            l.add(MapColor.NONE, MapColorTypes.NONE, SpongeMapColorType::new, colorId);
            l.add(MapColor.GRASS, MapColorTypes.GRASS, SpongeMapColorType::new, colorId);
            l.add(MapColor.SAND, MapColorTypes.SAND, SpongeMapColorType::new, colorId);
            l.add(MapColor.WOOL, MapColorTypes.WOOL, SpongeMapColorType::new, colorId);
            l.add(MapColor.FIRE, MapColorTypes.FIRE, SpongeMapColorType::new, colorId);
            l.add(MapColor.ICE, MapColorTypes.ICE, SpongeMapColorType::new, colorId);
            l.add(MapColor.METAL, MapColorTypes.METAL, SpongeMapColorType::new, colorId);
            l.add(MapColor.PLANT, MapColorTypes.PLANT, SpongeMapColorType::new, colorId);
            l.add(MapColor.SNOW, MapColorTypes.SNOW, SpongeMapColorType::new, colorId);
            l.add(MapColor.CLAY, MapColorTypes.CLAY, SpongeMapColorType::new, colorId);
            l.add(MapColor.DIRT, MapColorTypes.DIRT, SpongeMapColorType::new, colorId);
            l.add(MapColor.STONE, MapColorTypes.STONE, SpongeMapColorType::new, colorId);
            l.add(MapColor.WATER, MapColorTypes.WATER, SpongeMapColorType::new, colorId);
            l.add(MapColor.WOOD, MapColorTypes.WOOD, SpongeMapColorType::new, colorId);
            l.add(MapColor.QUARTZ, MapColorTypes.QUARTZ, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_ORANGE, MapColorTypes.COLOR_ORANGE, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_MAGENTA, MapColorTypes.COLOR_MAGENTA, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_LIGHT_BLUE, MapColorTypes.COLOR_LIGHT_BLUE, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_YELLOW, MapColorTypes.COLOR_YELLOW, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_LIGHT_GREEN, MapColorTypes.COLOR_LIGHT_GREEN, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_PINK, MapColorTypes.COLOR_PINK, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_GRAY, MapColorTypes.COLOR_GRAY, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_LIGHT_GRAY, MapColorTypes.COLOR_LIGHT_GRAY, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_CYAN, MapColorTypes.COLOR_CYAN, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_PURPLE, MapColorTypes.COLOR_PURPLE, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_BLUE, MapColorTypes.COLOR_BLUE, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_BROWN, MapColorTypes.COLOR_BROWN, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_GREEN, MapColorTypes.COLOR_GREEN, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_RED, MapColorTypes.COLOR_RED, SpongeMapColorType::new, colorId);
            l.add(MapColor.COLOR_BLACK, MapColorTypes.COLOR_BLACK, SpongeMapColorType::new, colorId);
            l.add(MapColor.GOLD, MapColorTypes.GOLD, SpongeMapColorType::new, colorId);
            l.add(MapColor.DIAMOND, MapColorTypes.DIAMOND, SpongeMapColorType::new, colorId);
            l.add(MapColor.LAPIS, MapColorTypes.LAPIS_LAZULI, SpongeMapColorType::new, colorId);
            l.add(MapColor.EMERALD, MapColorTypes.EMERALD, SpongeMapColorType::new, colorId);
            l.add(MapColor.PODZOL, MapColorTypes.PODZOL, SpongeMapColorType::new, colorId);
            l.add(MapColor.NETHER, MapColorTypes.NETHER, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_WHITE, MapColorTypes.TERRACOTTA_WHITE, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_ORANGE, MapColorTypes.TERRACOTTA_ORANGE, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_MAGENTA, MapColorTypes.TERRACOTTA_MAGENTA, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_LIGHT_BLUE, MapColorTypes.TERRACOTTA_LIGHT_BLUE, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_YELLOW, MapColorTypes.TERRACOTTA_YELLOW, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_LIGHT_GREEN, MapColorTypes.TERRACOTTA_LIGHT_GREEN, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_PINK, MapColorTypes.TERRACOTTA_PINK, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_GRAY, MapColorTypes.TERRACOTTA_GRAY, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_LIGHT_GRAY, MapColorTypes.TERRACOTTA_LIGHT_GRAY, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_CYAN, MapColorTypes.TERRACOTTA_CYAN, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_PURPLE, MapColorTypes.TERRACOTTA_PURPLE, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_BLUE, MapColorTypes.TERRACOTTA_BLUE, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_BROWN, MapColorTypes.TERRACOTTA_BROWN, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_GREEN, MapColorTypes.TERRACOTTA_GREEN, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_RED, MapColorTypes.TERRACOTTA_RED, SpongeMapColorType::new, colorId);
            l.add(MapColor.TERRACOTTA_BLACK, MapColorTypes.TERRACOTTA_BLACK, SpongeMapColorType::new, colorId);
            l.add(MapColor.CRIMSON_NYLIUM, MapColorTypes.CRIMSON_NYLIUM, SpongeMapColorType::new, colorId);
            l.add(MapColor.CRIMSON_STEM, MapColorTypes.CRIMSON_STEM, SpongeMapColorType::new, colorId);
            l.add(MapColor.CRIMSON_HYPHAE, MapColorTypes.CRIMSON_HYPHAE, SpongeMapColorType::new, colorId);
            l.add(MapColor.WARPED_NYLIUM, MapColorTypes.WARPED_NYLIUM, SpongeMapColorType::new, colorId);
            l.add(MapColor.WARPED_STEM, MapColorTypes.WARPED_STEM, SpongeMapColorType::new, colorId);
            l.add(MapColor.WARPED_HYPHAE, MapColorTypes.WARPED_HYPHAE, SpongeMapColorType::new, colorId);
            l.add(MapColor.WARPED_WART_BLOCK, MapColorTypes.WARPED_WART_BLOCK, SpongeMapColorType::new, colorId);
        });
    }

    public static RegistryLoader<MapDecorationOrientation> mapDecorationOrientation() {
        return RegistryLoader.of(l -> {
            l.add(0, MapDecorationOrientations.SOUTH, k -> new SpongeMapDecorationOrientation(0));
            l.add(1, MapDecorationOrientations.SOUTH_SOUTHWEST, k -> new SpongeMapDecorationOrientation(1));
            l.add(2, MapDecorationOrientations.SOUTHWEST, k -> new SpongeMapDecorationOrientation(2));
            l.add(3, MapDecorationOrientations.WEST_SOUTHWEST, k -> new SpongeMapDecorationOrientation(3));
            l.add(4, MapDecorationOrientations.WEST, k -> new SpongeMapDecorationOrientation(4));
            l.add(5, MapDecorationOrientations.WEST_NORTHWEST, k -> new SpongeMapDecorationOrientation(5));
            l.add(6, MapDecorationOrientations.NORTHWEST, k -> new SpongeMapDecorationOrientation(6));
            l.add(7, MapDecorationOrientations.NORTH_NORTHWEST, k -> new SpongeMapDecorationOrientation(7));
            l.add(8, MapDecorationOrientations.NORTH, k -> new SpongeMapDecorationOrientation(8));
            l.add(9, MapDecorationOrientations.NORTH_NORTHEAST, k -> new SpongeMapDecorationOrientation(9));
            l.add(10, MapDecorationOrientations.NORTHEAST, k -> new SpongeMapDecorationOrientation(10));
            l.add(11, MapDecorationOrientations.EAST_NORTHEAST, k -> new SpongeMapDecorationOrientation(11));
            l.add(12, MapDecorationOrientations.EAST, k -> new SpongeMapDecorationOrientation(12));
            l.add(13, MapDecorationOrientations.EAST_SOUTHEAST, k -> new SpongeMapDecorationOrientation(13));
            l.add(14, MapDecorationOrientations.SOUTHEAST, k -> new SpongeMapDecorationOrientation(14));
            l.add(15, MapDecorationOrientations.SOUTH_SOUTHEAST, k -> new SpongeMapDecorationOrientation(15));
        });
    }

    public static RegistryLoader<MapDecorationType> mapDecorationType() {
        return RegistryLoader.of(l -> {
            l.add(MapDecorationTypes.PLAYER_MARKER, k -> new SpongeMapDecorationType(MapDecoration.Type.PLAYER));
            l.add(MapDecorationTypes.GREEN_MARKER, k -> new SpongeMapDecorationType(MapDecoration.Type.FRAME));
            l.add(MapDecorationTypes.RED_MARKER, k -> new SpongeMapDecorationType(MapDecoration.Type.RED_MARKER));
            l.add(MapDecorationTypes.BLUE_MARKER, k -> new SpongeMapDecorationType(MapDecoration.Type.BLUE_MARKER));
            l.add(MapDecorationTypes.TARGET_X, k -> new SpongeMapDecorationType(MapDecoration.Type.TARGET_X));
            l.add(MapDecorationTypes.TARGET_POINT, k -> new SpongeMapDecorationType(MapDecoration.Type.TARGET_POINT));
            l.add(MapDecorationTypes.PLAYER_OFF_MAP, k -> new SpongeMapDecorationType(MapDecoration.Type.PLAYER_OFF_MAP));
            l.add(MapDecorationTypes.PLAYER_OFF_LIMITS, k -> new SpongeMapDecorationType(MapDecoration.Type.PLAYER_OFF_LIMITS));
            l.add(MapDecorationTypes.MANSION, k -> new SpongeMapDecorationType(MapDecoration.Type.MANSION));
            l.add(MapDecorationTypes.MONUMENT, k -> new SpongeMapDecorationType(MapDecoration.Type.MONUMENT));
            // banners
            l.add(MapDecorationTypes.BANNER_WHITE, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_WHITE, DyeColors.WHITE));
            l.add(MapDecorationTypes.BANNER_ORANGE, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_ORANGE, DyeColors.ORANGE));
            l.add(MapDecorationTypes.BANNER_MAGENTA, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_MAGENTA, DyeColors.MAGENTA));
            l.add(MapDecorationTypes.BANNER_LIGHT_BLUE, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_LIGHT_BLUE, DyeColors.LIGHT_BLUE));
            l.add(MapDecorationTypes.BANNER_YELLOW, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_YELLOW, DyeColors.YELLOW));
            l.add(MapDecorationTypes.BANNER_LIME, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_LIME, DyeColors.LIME));
            l.add(MapDecorationTypes.BANNER_PINK, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_PINK, DyeColors.PINK));
            l.add(MapDecorationTypes.BANNER_GRAY, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_GRAY, DyeColors.GRAY));
            l.add(MapDecorationTypes.BANNER_LIGHT_GRAY, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_LIGHT_GRAY, DyeColors.LIGHT_GRAY));
            l.add(MapDecorationTypes.BANNER_CYAN, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_CYAN, DyeColors.CYAN));
            l.add(MapDecorationTypes.BANNER_PURPLE, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_PURPLE, DyeColors.PURPLE));
            l.add(MapDecorationTypes.BANNER_BLUE, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_BLUE, DyeColors.BLUE));
            l.add(MapDecorationTypes.BANNER_BROWN, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_BROWN, DyeColors.BROWN));
            l.add(MapDecorationTypes.BANNER_GREEN, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_GREEN, DyeColors.GREEN));
            l.add(MapDecorationTypes.BANNER_RED, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_RED, DyeColors.RED));
            l.add(MapDecorationTypes.BANNER_BLACK, k -> new SpongeMapDecorationBannerType(MapDecoration.Type.BANNER_BLACK, DyeColors.BLACK));
            l.add(MapDecorationTypes.RED_X, k -> new SpongeMapDecorationType(MapDecoration.Type.RED_X));
        });
    }

    public static RegistryLoader<MapShade> mapShade() {
        return RegistryLoader.of(l -> {
            l.add(0, MapShades.BASE, k -> new SpongeMapShade(0, 180));
            l.add(1, MapShades.DARK, k -> new SpongeMapShade(1, 220));
            l.add(2, MapShades.DARKER, k -> new SpongeMapShade(2, 255));
            l.add(3, MapShades.DARKEST, k -> new SpongeMapShade(3, 135));
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
        final Registry<FlatLevelGeneratorPreset> registry = registryAccess.registryOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET);
        return RegistryLoader.of(l -> {
            for (final var entry : registry.entrySet()) {
                l.add(RegistryKey.of(RegistryTypes.FLAT_GENERATOR_CONFIG, (ResourceKey) (Object) entry.getKey().location()), () -> (FlatGeneratorConfig) entry.getValue().settings());
            }
        });
    }

    // @formatter:on
}
