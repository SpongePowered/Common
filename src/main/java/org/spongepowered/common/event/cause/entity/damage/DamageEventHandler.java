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
package org.spongepowered.common.event.cause.entity.damage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.cause.entity.damage.DamageFunction;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.event.cause.entity.damage.source.BlockDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.FallingBlockDamageSource;
import org.spongepowered.api.item.inventory.ArmorEquipable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.common.accessor.entity.LivingEntityAccessor;
import org.spongepowered.common.bridge.CreatorTrackedBridge;
import org.spongepowered.common.bridge.world.chunk.ChunkBridge;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.VecHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Predicate;

public final class DamageEventHandler {

    public static final DoubleUnaryOperator HARD_HAT_FUNCTION = damage -> -(damage - (damage * 0.75F));

    public static DoubleUnaryOperator createResistanceFunction(final int resistanceAmplifier) {
        final int base = (resistanceAmplifier + 1) * 5;
        final int modifier = 25 - base;
        return damage -> -(damage - ((damage * modifier) / 25.0F));
    }

    @SuppressWarnings("ConstantConditions")
    public static Optional<DamageFunction> createHardHatModifier(final LivingEntity entityLivingBase,
        final DamageSource damageSource
    ) {
        if ((damageSource instanceof FallingBlockDamageSource) && !entityLivingBase.getItemStackFromSlot(EquipmentSlotType.HEAD)
            .isEmpty()) {
            final DamageModifier modifier = DamageModifier.builder()
                .cause(Cause.of(
                    EventContext.empty(),
                    ((ItemStack) (Object) entityLivingBase.getItemStackFromSlot(EquipmentSlotType.HEAD)).createSnapshot()
                ))
                .type(DamageModifierTypes.HARD_HAT)
                .build();
            return Optional.of(new DamageFunction(modifier, DamageEventHandler.HARD_HAT_FUNCTION));
        }
        return Optional.empty();
    }

    public static Optional<DamageFunction> createArmorModifiers(final LivingEntity entityLivingBase,
        final DamageSource damageSource
    ) {
        if (!damageSource.isUnblockable()) {
            final int totalArmorValue = entityLivingBase.getTotalArmorValue();
            final float totalArmor = (float) totalArmorValue;
            final IAttributeInstance attribute = entityLivingBase.getAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);
            final double armorToughness = attribute.getValue();
            final float f = 2.0F + (float) armorToughness / 4.0F;

            final DoubleUnaryOperator function = incomingDamage -> {
                final float f1 = MathHelper.clamp(totalArmor - (float) incomingDamage / f, totalArmor * 0.2F, 20.0F);
                return -(incomingDamage - (incomingDamage * (1.0F - f1 / 25.0F)));
            };
            final Cause.Builder builder = Cause.builder();
            final EventContext.Builder contextBuilder = EventContext.builder();
            if (entityLivingBase instanceof ArmorEquipable) {
                // TODO - Add the event context keys for these armor pieces
                final ItemStackSnapshot helmet = ((ArmorEquipable) entityLivingBase).getHead().createSnapshot();
                final ItemStackSnapshot chest = ((ArmorEquipable) entityLivingBase).getChest().createSnapshot();
                final ItemStackSnapshot legs = ((ArmorEquipable) entityLivingBase).getLegs().createSnapshot();
                final ItemStackSnapshot feet = ((ArmorEquipable) entityLivingBase).getFeet().createSnapshot();
            }
            final DamageFunction armorModifier = DamageFunction.of(DamageModifier.builder()
                .cause(Cause.of(EventContext.empty(), attribute, entityLivingBase))
                .type(DamageModifierTypes.ARMOR)
                .build(), function);
            return Optional.of(armorModifier);
        }
        return Optional.empty();
    }

    public static Optional<DamageFunction> createResistanceModifier(final LivingEntity entityLivingBase, final DamageSource damageSource) {
        if (!damageSource.isDamageAbsolute() && entityLivingBase.isPotionActive(Effects.RESISTANCE) && damageSource != DamageSource.OUT_OF_WORLD) {
            final PotionEffect effect = ((PotionEffect) entityLivingBase.getActivePotionEffect(Effects.RESISTANCE));
            return Optional.of(new DamageFunction(DamageModifier.builder()
                .cause(Cause.of(EventContext.empty(), effect))
                .type(DamageModifierTypes.DEFENSIVE_POTION_EFFECT)
                .build(), DamageEventHandler.createResistanceFunction(effect.getAmplifier())));
        }
        return Optional.empty();
    }

    private static double enchantmentDamageTracked;

    public static Optional<List<DamageFunction>> createEnchantmentModifiers(
        final LivingEntity living,
        final DamageSource damageSource
    ) {
        if (!damageSource.isDamageAbsolute()) {
            final Iterable<net.minecraft.item.ItemStack> inventory = living.getArmorInventoryList();
            if (EnchantmentHelper.getEnchantmentModifierDamage(inventory, damageSource) <= 0) {
                return Optional.empty();
            }
            final List<DamageFunction> modifiers = new ArrayList<>();
            boolean first = true;
            int totalModifier = 0;
            for (final net.minecraft.item.ItemStack itemStack : inventory) {
                if (itemStack.isEmpty()) {
                    continue;
                }
                final Multimap<Enchantment, Short> enchantments = LinkedHashMultimap.create();
                final ListNBT enchantmentList = itemStack.getEnchantmentTagList();
                if (enchantmentList.isEmpty()) {
                    continue;
                }

                for (int i = 0; i < enchantmentList.size(); ++i) {
                    final short enchantmentId = enchantmentList.getCompound(i).getShort(Constants.Item.ITEM_ENCHANTMENT_ID);
                    final short level = enchantmentList.getCompound(i).getShort(Constants.Item.ITEM_ENCHANTMENT_LEVEL);

                    final Enchantment enchantment = Registry.ENCHANTMENT.getByValue(enchantmentId);
                    if (enchantment != null) {
                        // Ok, we have an enchantment!
                        final int temp = enchantment.calcModifierDamage(level, damageSource);
                        if (temp != 0) {
                            enchantments.put(enchantment, level);
                        }
                    }
                }
                final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(itemStack);

                for (final Map.Entry<Enchantment, Collection<Short>> enchantment : enchantments.asMap().entrySet()) {
                    final DamageObject object = new DamageObject();
                    int modifierTemp = 0;
                    for (final short level : enchantment.getValue()) {
                        modifierTemp += enchantment.getKey().calcModifierDamage(level, damageSource);
                    }
                    final int modifier = modifierTemp;
                    object.previousDamage = totalModifier;
                    if (object.previousDamage > 25) {
                        object.previousDamage = 25;
                    }
                    totalModifier += modifier;
                    object.augment = first;
                    object.ratio = modifier;
                    final DoubleUnaryOperator enchantmentFunction = damageIn -> {
                        if (object.augment) {
                            DamageEventHandler.enchantmentDamageTracked = damageIn;
                        }
                        if (damageIn <= 0) {
                            return 0D;
                        }
                        final double actualDamage = DamageEventHandler.enchantmentDamageTracked;
                        if (object.previousDamage > 25) {
                            return 0D;
                        }
                        double modifierDamage = actualDamage;
                        final double magicModifier;
                        if (modifier > 0 && modifier <= 20) {
                            final int j = 25 - modifier;
                            magicModifier = modifierDamage * j;
                            modifierDamage = magicModifier / 25.0F;
                        }
                        return -Math.max(actualDamage - modifierDamage, 0.0D);
                    };
                    if (first) {
                        first = false;
                    }

                    final DamageModifier enchantmentModifier = DamageModifier.builder()
                        .cause(Cause.of(EventContext.empty(), enchantment, snapshot, living))
                        .type(DamageModifierTypes.ARMOR_ENCHANTMENT)
                        .build();
                    modifiers.add(new DamageFunction(enchantmentModifier, enchantmentFunction));
                }
                if (!modifiers.isEmpty()) {
                    return Optional.of(modifiers);
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<DamageFunction> createAbsorptionModifier(final LivingEntity entityLivingBase) {
        final float absorptionAmount = entityLivingBase.getAbsorptionAmount();
        if (absorptionAmount > 0) {
            final DoubleUnaryOperator function = damage ->
                -(Math.max(damage - Math.max(damage - absorptionAmount, 0.0F), 0.0F));
            final DamageModifier modifier = DamageModifier.builder()
                .cause(Cause.of(EventContext.empty(), entityLivingBase))
                .type(DamageModifierTypes.ABSORPTION)
                .build();
            return Optional.of(new DamageFunction(modifier, function));
        }
        return Optional.empty();
    }

    public static ServerLocation findFirstMatchingBlock(final Entity entity, final AxisAlignedBB bb, final Predicate<BlockState> predicate
    ) {
        final int i = MathHelper.floor(bb.minX);
        final int j = MathHelper.floor(bb.maxX + 1.0D);
        final int k = MathHelper.floor(bb.minY);
        final int l = MathHelper.floor(bb.maxY + 1.0D);
        final int i1 = MathHelper.floor(bb.minZ);
        final int j1 = MathHelper.floor(bb.maxZ + 1.0D);
        final AbstractChunkProvider spongeChunkProvider = entity.world.getChunkProvider();
        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    final BlockPos blockPos = new BlockPos(k1, l1, i2);
                    final Chunk chunk = spongeChunkProvider.getChunk(
                        blockPos.getX() >> 4,
                        blockPos.getZ() >> 4,
                        false
                    );
                    if (chunk == null || chunk.isEmpty()) {
                        continue;
                    }
                    if (predicate.test(chunk.getBlockState(blockPos))) {
                        return ServerLocation.of((ServerWorld) entity.world, k1, l1, i2);
                    }
                }
            }
        }

        // Entity is source of fire
        return ((org.spongepowered.api.entity.Entity) entity).getServerLocation();
    }

    /**
     * This applies various contexts based on the type of {@link DamageSource}, whether
     * it's provided by sponge or vanilla. This is not stack neutral, which is why it requires
     * a {@link CauseStackManager.StackFrame} reference to push onto the stack.
     *
     * @param damageSource
     * @param frame
     */
    public static void generateCauseFor(final DamageSource damageSource, final CauseStackManager.StackFrame frame) {
        if (damageSource instanceof IndirectEntityDamageSource) {
            final net.minecraft.entity.Entity source = damageSource.getTrueSource();
            if (!(source instanceof PlayerEntity) && source instanceof CreatorTrackedBridge) {
                final CreatorTrackedBridge creatorBridge = (CreatorTrackedBridge) source;
                creatorBridge.tracked$getCreatorReference().ifPresent(creator -> frame.addContext(EventContextKeys.CREATOR, creator));
                creatorBridge.tracked$getNotifierReference().ifPresent(notifier -> frame.addContext(EventContextKeys.NOTIFIER, notifier));
            }
        } else if (damageSource instanceof EntityDamageSource) {
            final net.minecraft.entity.Entity source = damageSource.getTrueSource();
            if (!(source instanceof PlayerEntity) && source instanceof CreatorTrackedBridge) {
                final CreatorTrackedBridge creatorBridge = (CreatorTrackedBridge) source;
                creatorBridge.tracked$getCreatorReference().ifPresent(creator -> frame.addContext(EventContextKeys.CREATOR, creator));
                creatorBridge.tracked$getNotifierReference().ifPresent(notifier -> frame.addContext(EventContextKeys.NOTIFIER, notifier));
            }
        } else if (damageSource instanceof BlockDamageSource) {
            final ServerLocation location = ((BlockDamageSource) damageSource).getLocation();
            final BlockPos blockPos = VecHelper.toBlockPos(location);
            final ChunkBridge mixinChunk = (ChunkBridge) ((net.minecraft.world.World) location.getWorld()).getChunkAt(blockPos);
            mixinChunk.bridge$getBlockCreator(blockPos).ifPresent(creator -> frame.addContext(EventContextKeys.CREATOR, creator));
            mixinChunk.bridge$getBlockNotifier(blockPos).ifPresent(notifier -> frame.addContext(EventContextKeys.NOTIFIER, notifier));
        }
        frame.pushCause(damageSource);
    }

    public static List<DamageFunction> createAttackEnchantmentFunction(
        final net.minecraft.item.ItemStack heldItem, final CreatureAttribute creatureAttribute, final float attackStrength
    ) {
        final Multimap<Enchantment, Integer> enchantments = LinkedHashMultimap.create();
        final List<DamageFunction> damageModifierFunctions = new ArrayList<>();
        if (!heldItem.isEmpty()) {
            final ListNBT nbttaglist = heldItem.getEnchantmentTagList();
            if (nbttaglist.isEmpty()) {
                return ImmutableList.of();
            }

            for (int i = 0; i < nbttaglist.size(); ++i) {
                final int j = nbttaglist.getCompound(i).getShort("id");
                final int enchantmentLevel = nbttaglist.getCompound(i).getShort("lvl");

                final Enchantment enchantment = Registry.ENCHANTMENT.getByValue(j);
                if (enchantment != null) {
                    enchantments.put(enchantment, enchantmentLevel);
                }
            }
            if (enchantments.isEmpty()) {
                return ImmutableList.of();
            }
            final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(heldItem);

            for (final Map.Entry<Enchantment, Collection<Integer>> enchantment : enchantments.asMap().entrySet()) {
                final DamageModifier enchantmentModifier = DamageModifier.builder()
                    .type(DamageModifierTypes.WEAPON_ENCHANTMENT)
                    .cause(Cause.of(EventContext.empty(), snapshot, enchantment))
                    .build();
                final DoubleUnaryOperator enchantmentFunction = (damage) -> {
                    double totalDamage = 0;
                    for (final int level : enchantment.getValue()) {
                        totalDamage += (double) enchantment.getKey().calcDamageByCreature(level, creatureAttribute) * attackStrength;
                    }
                    return totalDamage;
                };
                damageModifierFunctions.add(new DamageFunction(enchantmentModifier, enchantmentFunction));
            }
        }

        return damageModifierFunctions;
    }

    public static DamageFunction provideCriticalAttackTuple(final PlayerEntity player) {
        final DamageModifier modifier = DamageModifier.builder()
            .cause(Cause.of(EventContext.empty(), player))
            .type(DamageModifierTypes.CRITICAL_HIT)
            .build();
        final DoubleUnaryOperator function = (damage) -> damage * .5F;
        return new DamageFunction(modifier, function);
    }

    public static DamageFunction provideCooldownAttackStrengthFunction(final PlayerEntity player,
        final float attackStrength
    ) {
        final DamageModifier modifier = DamageModifier.builder()
            .cause(Cause.of(EventContext.empty(), player))
            .type(DamageModifierTypes.ATTACK_COOLDOWN)
            .build();
        // The formula is as follows:
        // Since damage needs to be "multiplied", this needs to basically add negative damage but re-add the "reduced" damage.
        final DoubleUnaryOperator function = (damage) -> -damage + (damage * (0.2F + attackStrength * attackStrength * 0.8F));
        return new DamageFunction(modifier, function);
    }

    @SuppressWarnings("ConstantConditions")
    public static Optional<DamageFunction> createShieldFunction(final LivingEntity entity, final DamageSource source, final float amount) {
        if (entity.isActiveItemStackBlocking() && amount > 0.0 && ((LivingEntityAccessor) entity).accessor$canBlockDamageSource(source)) {
            final DamageModifier modifier = DamageModifier.builder()
                .cause(Cause.of(EventContext.empty(), entity, ((ItemStack) (Object) entity.getActiveItemStack()).createSnapshot()))
                .type(DamageModifierTypes.SHIELD)
                .build();
            return Optional.of(new DamageFunction(modifier, (damage) -> -damage));
        }
        return Optional.empty();
    }
}
