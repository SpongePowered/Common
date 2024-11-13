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
package org.spongepowered.common.data.provider.item.stack;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.consume_effects.PlaySoundConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Platform;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.item.ItemRarity;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"unchecked", "UnstableApiUsage"})
public final class ItemStackData {

    public static final FoodProperties DEFAULT_FOOD_PROPERTIES = new FoodProperties(0, 0, false);
    public static final Consumable DEFAULT_CONSUMABLE_PROPERTIES = new Consumable(1.6F, ItemUseAnimation.EAT, null, true, List.of());

    private ItemStackData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        // TODO DataComponents.SUSPICIOUS_STEW_EFFECTS
        // TODO maybe DataComponents.ATTRIBUTE_MODIFIERS as keys?
        // TODO DataComponents.BUNDLE_CONTENTS also check for Shulker Boxes? - removing the component prevents using the bundle
        // TODO DataComponents.CONTAINER_LOOT for containers with loottable data, also for blockentity?
        // TODO DataComponents.BLOCK_ENTITY_DATA maybe expose as raw DataContainer? (id MUST have block entity type)
        // TODO DataComponents.BLOCK_STATE - actually StateProperties
        // TODO DataComponents.POT_DECORATIONS - List<ItemType>
        // TODO DataComponents.ENTITY_DATA maybe expose as raw DataContainer? (applies to spawneggs/armorstand) (id MUST have entity type)
        // TODO DataComponents.BUCKET_ENTITY_DATA maybe expose as raw DataContainer?
        // TODO DataComponents.BEES maybe expose as raw DataContainer? + [min_]ticks_in_hive
        // TODO DataComponents.TRIM + TrimMaterial + TrimPattern + showInToolTip @HideFlagsItemStackData
        // TODO DataComponents.INSTRUMENT goat horn + API type + duration + range
        // TODO DataComponents.RECIPES - for Items.KNOWLEDGE_BOOK
        // TODO DataComponents.MAX_STACK_SIZE; incompatible with MAX_DAMAGE?
        // TODO DataComponents.OMINOUS_BOTTLE_AMPLIFIER 1.21

        // TODO rework applicable potion effects to consume effects
        final var applicablePotionEffects = Keys.APPLICABLE_POTION_EFFECTS;
        ConsumeEffect newPotionEffects = new ApplyStatusEffectsConsumeEffect(List.of());
        ConsumeEffect teleportRand = new TeleportRandomlyConsumeEffect(5);
        ConsumeEffect removeStatusEffects = new RemoveStatusEffectsConsumeEffect(HolderSet.empty());
        ConsumeEffect clearStatusEffects = new ClearAllStatusEffectsConsumeEffect();
        ConsumeEffect playSoundEffect = new PlaySoundConsumeEffect(Holder.direct(null));

        registrator
                .asMutable(ItemStack.class)
                    .create(Keys.BURN_TIME)
                        .get(h -> SpongeCommon.server().fuelValues().burnDuration(h))
                    .create(Keys.CONTAINER_ITEM)
                        .get(h -> (ItemType) h.getItem().getCraftingRemainder().getItem())
                    .create(Keys.DISPLAY_NAME)
                        .get(h -> SpongeAdventure.asAdventure(h.getDisplayName()))
                    .create(Keys.CUSTOM_MODEL_DATA)
                        .get(h -> h.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.DEFAULT).value())
                        .set((h, v) -> h.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(v)))
                        .delete(h -> h.remove(DataComponents.CUSTOM_MODEL_DATA))
                    .create(Keys.CUSTOM_NAME)
                        .get(h -> {
                            if (h.has(DataComponents.CUSTOM_NAME)) {
                                return SpongeAdventure.asAdventure(h.getHoverName());
                            }
                            if (h.getItem() == Items.WRITTEN_BOOK) {
                                // When no custom name is set on a written book fallback to its title
                                // The custom name has a higher priority than the title so no setter is needed.
                                var bookContent = h.get(DataComponents.WRITTEN_BOOK_CONTENT);
                                if (bookContent != null) {
                                    final String rawTitle = bookContent.title().raw();
                                    if (!StringUtil.isBlank(rawTitle)) {
                                        return LegacyComponentSerializer.legacySection().deserialize(rawTitle);
                                    }
                                }
                            }
                            return null;
                        })
                        .set((h, v) -> h.set(DataComponents.CUSTOM_NAME, SpongeAdventure.asVanillaMutable(v)))
                        .delete(h -> h.remove(DataComponents.CUSTOM_NAME))
                    .create(Keys.IS_UNBREAKABLE)
                        .get(h -> h.has(DataComponents.UNBREAKABLE))
                        .set(ItemStackData::setIsUnbrekable)
                        .delete(h -> ItemStackData.setIsUnbrekable(h, false))
                    .create(Keys.LORE)
                        .get(h -> {
                            final List<Component> lines = h.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines();
                            if (lines.isEmpty()) {
                                return null;
                            }
                            return lines.stream().map(SpongeAdventure::asAdventure).toList();
                        })
                        .set((h, v) -> {
                            if (v.isEmpty()) {
                                h.remove(DataComponents.LORE);
                                return;
                            }
                            h.set(DataComponents.LORE, new ItemLore(v.stream().map(SpongeAdventure::asVanillaMutable).map(Component.class::cast).toList()));
                        })
                        .delete(h -> h.remove(DataComponents.LORE))
                    .create(Keys.MAX_DURABILITY)
                        .get(h -> h.getMaxDamage() != 0 ? h.getMaxDamage() : null)
                        .setAnd((h, v) -> {
                            if (v <= 0) {
                                return false;
                            }

                            h.set(DataComponents.MAX_DAMAGE, v);
                            return true;
                        })
                        .supports(h -> h.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) == 1)
                    .create(Keys.MAX_STACK_SIZE)
                        .get(ItemStack::getMaxStackSize)
                        .setAnd((h, v) -> {
                            if (v <= 0 || v > 99) {
                                return false;
                            }

                            h.set(DataComponents.MAX_STACK_SIZE, v);
                            return true;
                        })
                        .supports(h -> !h.has(DataComponents.MAX_DAMAGE))
                    .create(Keys.MODEL)
                        .get(stack -> (ResourceKey) (Object) stack.get(DataComponents.ITEM_MODEL))
                        .set((stack, model) -> stack.set(DataComponents.ITEM_MODEL, (ResourceLocation) (Object) model))
                    .create(Keys.ITEM_DURABILITY)
                        .get(stack -> stack.getMaxDamage() - stack.getDamageValue())
                        .set((stack, durability) -> stack.setDamageValue(stack.getMaxDamage() - durability))
                        .supports(h -> h.getMaxDamage() != 0)
                    .create(Keys.ITEM_RARITY)
                        .get(stack -> (ItemRarity) (Object) stack.getRarity())
                        .set((h, v) -> h.set(DataComponents.RARITY, (Rarity) (Object) v))
                    .create(Keys.REPLENISHED_FOOD)
                        .get(h -> {
                            final var food = h.get(DataComponents.FOOD);
                            return food == null ? null : food.nutrition();
                        })
                        .set((h, v) -> h.update(DataComponents.FOOD, DEFAULT_FOOD_PROPERTIES,
                                fp -> new FoodProperties(v, fp.saturation(), fp.canAlwaysEat())))
                    .create(Keys.REPLENISHED_SATURATION)
                        .get(h -> {
                            final var food = h.get(DataComponents.FOOD);
                            return food == null ? null : (double) food.saturation();
                            })
                        .set((h, v) -> h.update(DataComponents.FOOD, DEFAULT_FOOD_PROPERTIES,
                                fp -> new FoodProperties(fp.nutrition(), v.floatValue(), fp.canAlwaysEat())))
                    .create(Keys.CAN_ALWAYS_EAT)
                        .get(h -> {
                            final var food = h.get(DataComponents.FOOD);
                            return food == null ? null : food.canAlwaysEat();
                        })
                        .set((h, v) -> h.update(DataComponents.FOOD, DEFAULT_FOOD_PROPERTIES,
                                fp -> new FoodProperties(fp.nutrition(), fp.saturation(), v)))
                    .create(Keys.EATING_TIME)
                        .get(h -> {
                            final var consumable = h.get(DataComponents.CONSUMABLE);
                            return consumable == null ? null : Ticks.of(consumable.consumeTicks());
                        })
                        .set((h, v) -> h.update(DataComponents.CONSUMABLE, DEFAULT_CONSUMABLE_PROPERTIES,
                                c -> new Consumable(v.ticks() / 20f, c.animation(), c.sound(), c.hasConsumeParticles(), c.onConsumeEffects())))
                    .create(Keys.FOOD_CONVERTS_TO)
                        .get(h -> {
                            final var remainder = h.get(DataComponents.USE_REMAINDER);
                            return remainder == null ? null : ItemStackUtil.fromNative(remainder.convertInto());
                        })
                        .set((h, v) -> h.set(DataComponents.USE_REMAINDER, new UseRemainder(ItemStackUtil.toNative(v))))
                        .delete(h -> h.remove(DataComponents.USE_REMAINDER))
                    .create(Keys.REPAIR_COST)
                        .get(h -> h.getOrDefault(DataComponents.REPAIR_COST, 0))
                        .set((stack, cost) -> stack.set(DataComponents.REPAIR_COST, cost))
                        .delete(stack -> stack.remove(DataComponents.REPAIR_COST))
                    .create(Keys.ENCHANTMENT_GLINT_OVERRIDE)
                        .get(h -> h.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
                        .set((stack, value) -> stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, value))
                        .delete(stack -> stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
                    .create(Keys.CHARGED_PROJECTILES)
                        .get(h -> ItemStackUtil.snapshotOf(h.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).getItems()))
                        .set((stack, value) -> stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(ItemStackUtil.fromSnapshotToNativeList(value))))
                        .delete(stack -> stack.remove(DataComponents.CHARGED_PROJECTILES))
                    .create(Keys.INTANGIBLE_PROJECTILE)
                        .get(h -> h.get(DataComponents.INTANGIBLE_PROJECTILE) != null)
                        .set((stack, value) -> {
                            if (value) {
                                stack.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
                            } else {
                                stack.remove(DataComponents.INTANGIBLE_PROJECTILE);
                            }
                        })
                        .delete(stack -> stack.remove(DataComponents.INTANGIBLE_PROJECTILE))
                    .create(Keys.INVENTORY)
                        .get(h -> ItemStackData.inventoryFromItemContainerContents(h.get(DataComponents.CONTAINER)))
                        .set((h, value) -> {
                            final List<ItemStack> items = value.slots().stream().map(Slot::peek).map(ItemStackUtil::toNative).toList();
                            h.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
                        })
                        .delete(stack -> stack.remove(DataComponents.CONTAINER))
                    .create(Keys.FIRE_RESISTANT)
                        .get(h -> {
                            final @Nullable DamageResistant resist = h.get(DataComponents.DAMAGE_RESISTANT);
                            if (resist == null) {
                                return false;
                            }
                            return resist.types().location().equals(DamageTypes.IN_FIRE.location());
                        })
                        .set((h, value) -> {
                            if (value) {
                                h.applyComponents(DataComponentPatch.builder()
                                    .set(DataComponents.DAMAGE_RESISTANT, new DamageResistant(DamageTypeTags.IS_FIRE))
                                    .build());
                            } else {
                                h.remove(DataComponents.DAMAGE_RESISTANT);
                            }
                        })
                        .delete(stack -> stack.remove(DataComponents.DAMAGE_RESISTANT))
                    .create(Keys.ITEM_NAME)
                        .get(h -> {
                            final @Nullable Component component = h.get(DataComponents.ITEM_NAME);
                            if (component == null) {
                                return null;
                            }
                            return SpongeAdventure.asAdventure(component);
                        })
                        .set((h, value) -> h.set(DataComponents.ITEM_NAME, SpongeAdventure.asVanillaMutable(value)))
                        .delete(stack -> stack.remove(DataComponents.ITEM_NAME))
                    .create(Keys.COOLDOWN_GROUP)
                        .get(h -> {
                            final @Nullable UseCooldown cooldown = h.get(DataComponents.USE_COOLDOWN);
                            if (cooldown == null) {
                                return null;
                            }
                            return (ResourceKey) (Object) cooldown.cooldownGroup().orElse(null);
                        })
                        .set((h, value) -> {
                            if (value == null) {
                                h.remove(DataComponents.USE_COOLDOWN);
                                return;
                            }
                            h.set(DataComponents.USE_COOLDOWN, new UseCooldown(1, Optional.of((ResourceLocation) (Object) value)));
                        })
                        .deleteAndGet(ItemStackData::deleteAndTransactUseCooldown)
                    .create(Keys.COOLDOWN)
                        .get(h -> {
                            final @Nullable UseCooldown cooldown = h.get(DataComponents.USE_COOLDOWN);
                            if (cooldown == null) {
                                return null;
                            }
                            return Ticks.of(cooldown.ticks());
                        })
                        .setAndGet((h, value) -> {
                            if (value == null) {
                                return ItemStackData.deleteAndTransactUseCooldown(h);
                            }
                            final var existing = h.get(DataComponents.USE_COOLDOWN);
                            var builder = DataTransactionResult.builder()
                                .success(Value.immutableOf(Keys.COOLDOWN, value));
                            if (existing != null) {
                                h.set(DataComponents.USE_COOLDOWN, new UseCooldown(value.ticks(), existing.cooldownGroup()));
                                builder.replace(existing.cooldownGroup().map(ResourceKey.class::cast)
                                    .map(group ->
                                        List.of(
                                            Value.immutableOf(Keys.COOLDOWN, Ticks.of(existing.ticks())),
                                            Value.immutableOf(Keys.COOLDOWN_GROUP, group)
                                        )
                                    )
                                    .orElseGet(() -> List.of(
                                        Value.immutableOf(Keys.COOLDOWN, Ticks.of(existing.ticks()))
                                    )));
                            } else {
                                h.set(DataComponents.USE_COOLDOWN, new UseCooldown(value.ticks(), Optional.empty()));
                            }
                            return builder.build();
                        })
                        .deleteAndGet(ItemStackData::deleteAndTransactUseCooldown)
        ;
    }
    // @formatter:on

    private static Inventory inventoryFromItemContainerContents(final ItemContainerContents contents) {
        if (contents == null) {
            return null;
        }
        var slots = contents.stream().map(ItemStackUtil::cloneDefensive).toList();
        if (slots.isEmpty()) {
            return null;
        }
        final Inventory inventory = Inventory.builder().slots(slots.size()).completeStructure()
                .plugin(SpongeCommon.game().platform().container(Platform.Component.IMPLEMENTATION))
                .build();
        slots.forEach(inventory::offer);
        return inventory;
    }

    private static void setIsUnbrekable(final ItemStack stack, final Boolean value) {
        if (value) {
            stack.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
        } else {
            stack.remove(DataComponents.UNBREAKABLE);
        }
    }

    private static DataTransactionResult deleteAndTransactUseCooldown(final ItemStack stack) {
        final @Nullable UseCooldown cooldown = stack.remove(DataComponents.USE_COOLDOWN);
        if (cooldown == null) {
            return DataTransactionResult.successNoData();
        }
        return cooldown.cooldownGroup()
            .map(group -> DataTransactionResult.successRemove(List.of(
                Value.immutableOf(Keys.COOLDOWN, Ticks.of(cooldown.ticks())),
                Value.immutableOf(Keys.COOLDOWN_GROUP, (ResourceKey) (Object) group)
            ))).orElseGet(() -> DataTransactionResult.successRemove(List.of(
                Value.immutableOf(Keys.COOLDOWN, Ticks.of(cooldown.ticks()))
            )));
    }

}
