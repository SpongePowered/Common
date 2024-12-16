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
package org.spongepowered.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.entity.damage.DamageFunction;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierType;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.registry.DefaultedRegistryReference;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.CreatorTrackedBridge;
import org.spongepowered.common.bridge.world.damagesource.DamageSourceBridge;
import org.spongepowered.common.bridge.world.level.chunk.LevelChunkBridge;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageSources;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Predicate;

public final class DamageEventUtil {

    private DamageEventUtil() {
    }


    @SuppressWarnings("ConstantConditions")
    public static DamageFunction createHardHatModifier(final ItemStack headItem, final float multiplier) {
        final var snapshot = ItemStackUtil.snapshotOf(headItem);
        final var modifier = DamageEventUtil.buildDamageReductionModifier(DamageModifierTypes.HARD_HAT, snapshot);
        return new DamageFunction(modifier, damage -> damage * multiplier);
    }

    /**
     * LivingEntity#getDamageAfterArmorAbsorb
     */
    public static DamageFunction createArmorModifiers(final LivingEntity living, final DamageSource damageSource) {
        final DoubleUnaryOperator function = dmg -> CombatRules.getDamageAfterAbsorb(living, (float) dmg,
                damageSource, living.getArmorValue(), (float) living.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        final var modifier = DamageEventUtil.buildDamageReductionModifierWithFrame(DamageModifierTypes.ARMOR, living, Attributes.ARMOR_TOUGHNESS);

        return DamageFunction.of(modifier, function);
    }

    /**
     * LivingEntity#getDamageAfterMagicAbsorb
     */
    public static DamageFunction createResistanceModifier(final LivingEntity living) {
        final var effect = living.getEffect(MobEffects.DAMAGE_RESISTANCE);
        var modifier = DamageEventUtil.buildDamageReductionModifier(DamageModifierTypes.DEFENSIVE_POTION_EFFECT, effect);
        return new DamageFunction(modifier, DamageEventUtil.createResistanceFunction(living));
    }

    public static DoubleUnaryOperator createResistanceFunction(final LivingEntity living) {
        final var effect = living.getEffect(MobEffects.DAMAGE_RESISTANCE);
        final int base = effect == null ? 0 : (effect.getAmplifier() + 1) * 5;
        final int modifier = 25 - base;
        return damage -> Math.max(((damage * modifier) / 25.0F), 0.0f);
    }


    /**
     * LivingEntity#getDamageAfterMagicAbsorb
     */
    public static DamageFunction createEnchantmentModifiers(final LivingEntity living, final float damageProtection) {
        final DoubleUnaryOperator func = damage -> CombatRules.getDamageAfterMagicAbsorb((float) damage, damageProtection);
        final var modifier = DamageEventUtil.buildDamageReductionModifierWithFrame(DamageModifierTypes.ARMOR_ENCHANTMENT, living);
        return new DamageFunction(modifier, func);
    }

    public static DamageFunction createAbsorptionModifier(final LivingEntity living, final float absorptionAmount) {
        final var modifier = DamageEventUtil.buildDamageReductionModifier(DamageModifierTypes.ABSORPTION, living);
        return new DamageFunction(modifier, damage -> Math.max(damage - absorptionAmount, 0.0F));
    }

    public static ServerLocation findFirstMatchingBlock(final Entity entity, final AABB bb, final Predicate<BlockState> predicate) {
        final int i = Mth.floor(bb.minX);
        final int j = Mth.floor(bb.maxX + 1.0D);
        final int k = Mth.floor(bb.minY);
        final int l = Mth.floor(bb.maxY + 1.0D);
        final int i1 = Mth.floor(bb.minZ);
        final int j1 = Mth.floor(bb.maxZ + 1.0D);
        final ChunkSource chunkSource = entity.level().getChunkSource();
        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    final BlockPos blockPos = new BlockPos(k1, l1, i2);
                    final LevelChunk chunk = chunkSource.getChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4, false);
                    if (chunk == null || chunk.isEmpty()) {
                        continue;
                    }
                    if (predicate.test(chunk.getBlockState(blockPos))) {
                        return ServerLocation.of((ServerWorld) entity.level(), k1, l1, i2);
                    }
                }
            }
        }

        // Entity is source of fire
        return ((org.spongepowered.api.entity.Entity) entity).serverLocation();
    }

    /**
     * This applies various contexts based on the type of {@link DamageSource}, whether
     * it's provided by sponge or vanilla. This is not stack neutral, which is why it requires
     * a {@link CauseStackManager.StackFrame} reference to push onto the stack.
     */
    public static void generateCauseFor(final DamageSource damageSource, final CauseStackManager.StackFrame frame) {
        if (damageSource.getDirectEntity() instanceof org.spongepowered.api.entity.Entity entity) {
            if (!(entity instanceof Player) && entity instanceof CreatorTrackedBridge creatorBridge) {
                creatorBridge.tracker$getCreatorUUID().ifPresent(creator -> frame.addContext(EventContextKeys.CREATOR, creator));
                creatorBridge.tracker$getNotifierUUID().ifPresent(notifier -> frame.addContext(EventContextKeys.NOTIFIER, notifier));
            }
        } else if (((DamageSourceBridge) damageSource).bridge$blockLocation() != null) {
            final ServerLocation location = ((DamageSourceBridge) damageSource).bridge$blockLocation();
            final BlockPos blockPos = VecHelper.toBlockPos(location);
            final LevelChunkBridge chunkBridge = (LevelChunkBridge) ((net.minecraft.world.level.Level) location.world()).getChunkAt(blockPos);
            chunkBridge.bridge$getBlockCreatorUUID(blockPos).ifPresent(creator -> frame.addContext(EventContextKeys.CREATOR, creator));
            chunkBridge.bridge$getBlockNotifierUUID(blockPos).ifPresent(notifier -> frame.addContext(EventContextKeys.NOTIFIER, notifier));
        }
        frame.pushCause(damageSource);
    }

    /**
     * Mirrors {@link EnchantmentHelper#modifyDamage}
     */
    public static List<DamageFunction> createAttackEnchantmentFunction(final ItemStack weapon, final Entity entity, final DamageSource damageSource) {
        final var enchantments = weapon.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        final var snapshot = ItemStackUtil.snapshotOf(weapon);

        return enchantments.entrySet().stream().map(entry -> {
            final var enchantment = entry.getKey().value();
            final int level = entry.getIntValue();

            final var modifier = DamageEventUtil.buildAttackEnchantmentModifier(DamageModifierTypes.WEAPON_ENCHANTMENT, snapshot, enchantment);

            return new DamageFunction(modifier, damage ->
                    DamageEventUtil.enchantmentDamageFunction(weapon, entity, damageSource, damage, enchantment, level));
        }).toList();
    }

    public static DamageFunction provideSeparateEnchantmentFromBaseDamageFunction(final float baseDamage, final ItemStack weapon) {
        final var modifier = DamageEventUtil.buildAttackEnchantmentModifier(DamageModifierTypes.WEAPON_ENCHANTMENT, ItemStackUtil.snapshotOf(weapon));
        return new DamageFunction(modifier, damage -> damage - baseDamage);
    }

    public static DamageFunction provideCooldownEnchantmentStrengthFunction(final ItemStack weapon, final float attackStrength) {
        final var snapshot = ItemStackUtil.snapshotOf(weapon);
        final var modifier = DamageEventUtil.buildAttackEnchantmentModifier(DamageModifierTypes.ATTACK_STRENGTH, snapshot);
        return new DamageFunction(modifier, damage -> damage * attackStrength);
    }

    private static double enchantmentDamageFunction(final ItemStack weapon, final Entity entity,
            final DamageSource damageSource, final double damage, final Enchantment enchantment, final int level) {
        var totalDamage = new MutableFloat(damage);
        enchantment.modifyDamage((ServerLevel) entity.level(), level, weapon, entity, damageSource, totalDamage);
        return totalDamage.doubleValue();
    }

    public static DamageFunction provideCriticalAttackFunction(final Player player, double criticalModifier) {
        final var modifier = DamageEventUtil.buildAttackDamageModifier(DamageModifierTypes.CRITICAL_HIT, player);
        final DoubleUnaryOperator function = (damage) -> damage * criticalModifier;
        return new DamageFunction(modifier, function);
    }

    public static DamageFunction provideCooldownAttackStrengthFunction(final Player player, final float attackStrength) {
        final var modifier = DamageEventUtil.buildAttackDamageModifier(DamageModifierTypes.ATTACK_COOLDOWN, player);
        final DoubleUnaryOperator function = (damage) -> damage * (0.2F + attackStrength * attackStrength * 0.8F);
        return new DamageFunction(modifier, function);
    }

    public static DamageFunction provideWeaponAttackDamageBonusFunction(final Entity targetEntity, final ItemStack weapon, final DamageSource damageSource) {
        final var modifier = DamageEventUtil.buildAttackDamageModifier(DamageModifierTypes.WEAPON_BONUS, targetEntity);
        final DoubleUnaryOperator function = (damage) -> damage + weapon.getItem().getAttackDamageBonus(targetEntity, (float) damage, damageSource);
        return new DamageFunction(modifier, function);
    }

    public static DamageFunction provideSweepingDamageRatioFunction(final ItemStack held, final Player player, final double attackDamage) {
        final var modifier = DamageEventUtil.buildAttackEnchantmentModifier(DamageModifierTypes.SWEEPING, ItemStackUtil.snapshotOf(held));
        return DamageFunction.of(modifier, damage -> damage + player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * attackDamage);
    }

    @SuppressWarnings("ConstantConditions")
    public static DamageFunction createShieldFunction(final LivingEntity entity) {
        final var snapshot = ItemStackUtil.snapshotOf(entity.getUseItem());
        final var modifier = DamageEventUtil.buildDamageReductionModifier(DamageModifierTypes.SHIELD, entity, snapshot);
        return new DamageFunction(modifier, (damage) -> 0);
    }

    public static DamageFunction createFreezingBonus(final LivingEntity entity, final DamageSource damageSource, float multiplier) {
        final var modifier = DamageEventUtil.buildDamageReductionModifier(DamageModifierTypes.FREEZING_BONUS, damageSource, entity);
        return new DamageFunction(modifier, (damage) -> damage * multiplier);
    }

    private static DamageModifier buildDamageReductionModifierWithFrame(final DefaultedRegistryReference<DamageModifierType> modifierType, Object... causes) {
        try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
            for (final Object cause : causes) {
                frame.pushCause(cause);
            }
            return DamageModifier.builder().damageReductionGroup()
                    .cause(frame.currentCause()).type(modifierType).build();
        }
    }

    private static DamageModifier buildAttackDamageModifier(final DefaultedRegistryReference<DamageModifierType> modifierType, Object... causes) {
        return DamageModifier.builder().attackDamageGroup()
                .cause(Cause.of(EventContext.empty(), Arrays.asList(causes))).type(modifierType).build();
    }

    private static DamageModifier buildAttackEnchantmentModifier(final DefaultedRegistryReference<DamageModifierType> modifierType, Object... causes) {
        return DamageModifier.builder().attackEnchantmentGroup()
                .cause(Cause.of(EventContext.empty(), Arrays.asList(causes))).type(modifierType).build();
    }

    private static DamageModifier buildDamageReductionModifier(final DefaultedRegistryReference<DamageModifierType> modifierType, Object... causes) {
        return DamageModifier.builder().damageReductionGroup()
                .cause(Cause.of(EventContext.empty(), Arrays.asList(causes))).type(modifierType).build();
    }

    public static AttackEntityEvent callPlayerAttackEntityEvent(final Attack<Player> attack, final float knockbackModifier) {
        final boolean isMainthread = !attack.sourceEntity().level().isClientSide;
        if (isMainthread) {
            PhaseTracker.getInstance().pushCause(attack.dmgSource());
        }
        final var currentCause = isMainthread
                ? PhaseTracker.getInstance().currentCause()
                : Cause.of(EventContext.empty(), attack.dmgSource());
        final var event = attack.postEvent(knockbackModifier, currentCause);
        if (isMainthread) {
            PhaseTracker.getInstance().popCause();
        }
        return event;
    }

    /**
     * {@link Mob#doHurtTarget}
     */
    public static AttackEntityEvent callMobAttackEvent(final Attack<Mob> attack, final float knockbackModifier) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(attack.dmgSource());
            return attack.postEvent(knockbackModifier, frame.currentCause());
        }
    }

    /**
     * For {@link Entity#hurt} overrides without super call:
     * {@link net.minecraft.world.entity.decoration.BlockAttachedEntity#hurt}
     * {@link net.minecraft.world.entity.vehicle.VehicleEntity#hurt}
     * {@link net.minecraft.world.entity.decoration.ItemFrame#hurt}
     * {@link net.minecraft.world.entity.vehicle.MinecartTNT#hurt}
     * {@link net.minecraft.world.entity.boss.enderdragon.EndCrystal#hurt}
     * {@link net.minecraft.world.entity.projectile.ShulkerBullet#hurt}
     * {@link net.minecraft.world.entity.ExperienceOrb#hurt}
     * {@link net.minecraft.world.entity.item.ItemEntity#hurt}
     */
    public static AttackEntityEvent callOtherAttackEvent(
            final Entity targetEntity,
            final DamageSource damageSource,
            final double damage) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(damageSource);
            final AttackEntityEvent event = SpongeEventFactory.createAttackEntityEvent(frame.currentCause(), (org.spongepowered.api.entity.Entity) targetEntity, new ArrayList<>(), 0, damage);
            SpongeCommon.post(event);
            return event;
        }
    }

    public static DamageEventResult callLivingDamageEntityEvent(final Hurt hurt, final ActuallyHurt actuallyHurt) {

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            DamageEventUtil.generateCauseFor(actuallyHurt.dmgSource(), frame);

            final List<DamageFunction> originalFunctions = new ArrayList<>();
            originalFunctions.addAll(hurt.functions());
            originalFunctions.addAll(actuallyHurt.functions());
            final var event = SpongeEventFactory.createDamageEntityEvent(frame.currentCause(),
                    (org.spongepowered.api.entity.Entity) actuallyHurt.entity(),
                    originalFunctions,
                    actuallyHurt.baseDamage());

            if (actuallyHurt.dmgSource() != SpongeDamageSources.IGNORED) { // Basically, don't throw an event if it's our own damage source
                SpongeCommon.post(event);
            }

            return new DamageEventResult(event,
                    actuallyHurt.dmgSource(),
                    DamageEventUtil.findDamageBefore(event, DamageModifierTypes.SHIELD),
                    DamageEventUtil.findDamageDifference(event, DamageModifierTypes.SHIELD),
                    DamageEventUtil.findDamageBefore(event, DamageModifierTypes.HARD_HAT),
                    DamageEventUtil.findDamageBefore(event, DamageModifierTypes.ARMOR),
                    DamageEventUtil.findDamageDifference(event, DamageModifierTypes.DEFENSIVE_POTION_EFFECT), // TODO Math.max(0, resisted)?
                    DamageEventUtil.findDamageDifference(event, DamageModifierTypes.ABSORPTION)
            );
        }

    }

    private static Optional<Float> findDamageDifference(DamageEntityEvent event, DefaultedRegistryReference<DamageModifierType> type) {
        return DamageEventUtil.findModifier(event, type).map(event::damage).map(tuple -> tuple.first() - tuple.second()).map(Double::floatValue);
    }


    private static Optional<Float> findDamageBefore(DamageEntityEvent event, DefaultedRegistryReference<DamageModifierType> type) {
        return DamageEventUtil.findModifier(event, type).map(event::damage).map(Tuple::first).map(Double::floatValue);
    }

    private static Optional<DamageModifier> findModifier(DamageEntityEvent event, DefaultedRegistryReference<DamageModifierType> type) {
        return event.originalFunctions().stream()
                .map(DamageFunction::modifier)
                .filter(mod -> type.get().equals(mod.type()))
                .findFirst();
    }


    public static DamageEntityEvent callSimpleDamageEntityEvent(final DamageSource source, final Entity targetEntity, final double amount) {
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            DamageEventUtil.generateCauseFor(source, frame);
            final var event = SpongeEventFactory.createDamageEntityEvent(frame.currentCause(), (org.spongepowered.api.entity.Entity) targetEntity, new ArrayList<>(), amount);
            SpongeCommon.post(event);
            return event;
        }
    }




    public record DamageEventResult(DamageEntityEvent event,
                                    DamageSource source,
                                    Optional<Float> damageToShield,
                                    Optional<Float> damageBlockedByShield,
                                    Optional<Float> damageToHelmet,
                                    Optional<Float> damageToArmor,
                                    Optional<Float> damageResisted,
                                    Optional<Float> damageAbsorbed
    ) {

    }


    public record Hurt(DamageSource dmgSource, List<DamageFunction> functions) {

    }

    public record ActuallyHurt(LivingEntity entity,
                               List<DamageFunction> functions,
                               DamageSource dmgSource,
                               float baseDamage) {

    }

    public record Attack<T>(T sourceEntity,
                            Entity target,
                            ItemStack weapon,
                            DamageSource dmgSource,
                            float strengthScale,
                            float baseDamage,
                            List<DamageFunction> functions) {

        private AttackEntityEvent postEvent(final float knockbackModifier, final Cause cause) {
            final var event = SpongeEventFactory.createAttackEntityEvent(
                    cause,
                    (org.spongepowered.api.entity.Entity) this.target,
                    this.functions,
                    knockbackModifier,
                    this.baseDamage);
            SpongeCommon.post(event);
            return event;
        }

    }
}
