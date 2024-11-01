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
package org.spongepowered.common.item;


import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Block;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataContentUpdater;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.attribute.AttributeModifier;
import org.spongepowered.api.entity.attribute.type.AttributeType;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.data.DataUpdaterDelegate;
import org.spongepowered.common.data.persistence.NBTTranslator;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.Preconditions;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

public final class SpongeItemStack  {

    public static final class BuilderImpl extends AbstractDataBuilder<ItemStack> implements ItemStack.Builder {
        private ItemType type;
        private int quantity;
        private @Nullable LinkedHashMap<Key<?>, Object> keyValues;
        private DataComponentPatch components = DataComponentPatch.EMPTY;

        public BuilderImpl() {
            super(ItemStack.class, 1);
            this.reset();
        }

        @Override
        public ItemStack.Builder itemType(final ItemType itemType) {
            Objects.requireNonNull(itemType, "Item type cannot be null");
            this.type = itemType;
            return this;
        }

        @Override
        public ItemType currentItem() {
            return this.type == null ? BlockTypes.AIR.get().item().get() : this.type;
        }

        @Override
        public ItemStack.Builder quantity(final int quantity) throws IllegalArgumentException {
            Preconditions.checkArgument(quantity >= 0, "Quantity must not be smaller than 0");
            this.quantity = quantity;
            return this;
        }

        @Override
        public <V> ItemStack.Builder add(final Key<? extends Value<V>> key, final V value) throws IllegalArgumentException {
            if (this.keyValues == null) {
                this.keyValues = new LinkedHashMap<>();
            }
            this.keyValues.put(Objects.requireNonNull(key, "Key cannot be null!"), Objects.requireNonNull(value, "Value cannot be null!"));
            return this;
        }

        @Override
        public ItemStack.Builder fromItemStack(final ItemStack itemStack) {
            Objects.requireNonNull(itemStack, "Item stack cannot be null");
            // Assumes the item stack's values don't need to be validated
            this.type = itemStack.type();
            this.quantity = itemStack.quantity();
            if ((Object) itemStack instanceof net.minecraft.world.item.ItemStack mcStack) {
                this.components = mcStack.getComponentsPatch();
                //            this.itemDataSet.addAll(((CustomDataHolderBridge) itemStack).bridge$getCustomManipulators());

            } else {
                //            this.itemDataSet.addAll(itemStack.getValues());
            }
            return this;
        }

        @Override
        public ItemStack.Builder attributeModifier(final AttributeType attributeType, final AttributeModifier modifier, final EquipmentType equipmentType) {
            Objects.requireNonNull(attributeType, "AttributeType cannot be null");
            Objects.requireNonNull(modifier, "AttributeModifier cannot be null");
            Objects.requireNonNull(equipmentType, "EquipmentType cannot be null");

            var existing = this.components.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (existing == null) {
                existing = Optional.empty();
            }
            var modifiers = existing.map(ItemAttributeModifiers.class::cast).orElse(ItemAttributeModifiers.EMPTY);
            var attribute = BuiltInRegistries.ATTRIBUTE.wrapAsHolder((Attribute) attributeType);
            var slotGroup = SpongeItemStack.asEquipmentSlotGroup(equipmentType);

            modifiers = modifiers.withModifierAdded(attribute, (net.minecraft.world.entity.ai.attributes.AttributeModifier) (Object) modifier, slotGroup);

            final DataComponentPatch.Builder builder = DataComponentPatch.builder();
            this.components.entrySet().forEach(entry -> builder.set((DataComponentType) entry.getKey(), entry.getValue().orElse(null)));
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
            this.components = builder.build();

            return this;
        }

        @Override
        public ItemStack.Builder fromContainer(final DataView container) {
            final Optional<ItemStack> stack = SpongeItemStack.createItemStack(container);
            if (stack.isPresent()) {
                this.reset();
                return this.fromItemStack(stack.get());
            }
            SpongeCommon.logger().warn("Invalid ItemStack Container");
            return this;
        }

        @Override
        public ItemStack.Builder fromSnapshot(final ItemStackSnapshot snapshot) {
            Objects.requireNonNull(snapshot, "The snapshot was null!");
            this.itemType(snapshot.type());
            this.quantity(snapshot.quantity());

            for (Value.Immutable<?> value : snapshot.getValues()) {
                this.add(value);
            }

            if (snapshot instanceof SpongeItemStackSnapshot) {

                this.components = ((SpongeItemStackSnapshot) snapshot).getComponentsPatch();
            }

            return this;
        }

        @Override
        public ItemStack.Builder fromBlockSnapshot(final BlockSnapshot blockSnapshot) {
            Objects.requireNonNull(blockSnapshot, "The snapshot was null!");
            this.reset();
            final BlockType blockType = blockSnapshot.state().type();
            final ResourceLocation blockTypeKey = SpongeCommon.vanillaRegistry(Registries.BLOCK).getKey((Block) blockType);
            final Optional<ItemType> itemType = blockType.item();
            this.itemType(itemType.orElseThrow(() -> new IllegalArgumentException("ItemType not found for block type: " + blockTypeKey)));
            this.quantity(1);
            if (blockSnapshot instanceof SpongeBlockSnapshot) {
                ((SpongeBlockSnapshot) blockSnapshot).getCompound().ifPresent(compoundTag -> {
                    this.components = DataComponentPatch.builder().set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(compoundTag)).build();
                });

                // todo probably needs more testing, but this'll do donkey...
            } else { // TODO handle through the API specifically handling the rest of the data stuff
                //            blockSnapshot.getContainers().forEach(this::itemData);
            }
            return this;
        }

        /**
         * Sets the data to recreate a {@link BlockState} in a held {@link ItemStack}
         * state.
         *
         * @param blockState The block state to use
         * @return This builder, for chaining
         */
        @Override
        public ItemStack.Builder fromBlockState(final BlockState blockState) {
            Objects.requireNonNull(blockState, "blockState");
            final BlockType blockType = blockState.type();
            final ResourceLocation blockTypeKey = SpongeCommon.vanillaRegistry(Registries.BLOCK).getKey((Block) blockType);
            this.itemType(blockType.item().orElseThrow(() -> new IllegalArgumentException("Missing valid ItemType for BlockType: " + blockTypeKey)));
            blockState.getValues().forEach(this::add);
            return this;
        }

        @Override
        public ItemStack.Builder from(final ItemStack value) {
            return this.fromItemStack(value);
        }

        @Override
        protected Optional<ItemStack> buildContent(final DataView container) throws InvalidDataException {
            return SpongeItemStack.createItemStack(container);
        }

        @Override
        public ItemStack.Builder reset() {
            this.type = null;
            this.quantity = 1;
            this.components = DataComponentPatch.EMPTY;
            return this;
        }

        @SuppressWarnings({"unchecked", "rawtypes", "ConstantConditions"})
        @Override
        public ItemStack build() throws IllegalStateException {
            Preconditions.checkState(this.type != null, "Item type has not been set");

            if (this.type == null || this.quantity <= 0) {
                // If either type is none(air) or quantity is 0 return the vanilla EMPTY item
                return ((ItemStack) (Object) net.minecraft.world.item.ItemStack.EMPTY);
            }

            final net.minecraft.world.item.ItemStack mcStack = new net.minecraft.world.item.ItemStack((Item) this.type, this.quantity);
            mcStack.applyComponents(this.components);
            final ItemStack stack = (ItemStack) (Object) mcStack;

            if (this.keyValues != null) {
                this.keyValues.forEach((key, value) -> stack.offer((Key) key, value));
            }
            return stack;
        }
    }

    public static final class FactoryImpl implements ItemStack.Factory {

        @Override
        public ItemStack empty() {
            return (ItemStack) (Object) net.minecraft.world.item.ItemStack.EMPTY;
        }
    }

    @NotNull
    public static DataContainer getDataContainer(final net.minecraft.world.item.ItemStack mcStack) {
        final ResourceLocation key = BuiltInRegistries.ITEM.getKey(mcStack.getItem());
        final DataContainer container = DataContainer.createNew()
                .set(Queries.CONTENT_VERSION, ((ItemStack) (Object) mcStack).contentVersion())
                .set(Constants.ItemStack.TYPE, key)
                .set(Constants.ItemStack.COUNT, mcStack.getCount());
        // Cleanup Old Custom Data
        SpongeItemStack.cleanupOldCustomData(mcStack);
        // Serialize all DataComponents...

        var ops = RegistryOps.create(NbtOps.INSTANCE, SpongeCommon.vanillaRegistryAccess());
        var componentsTag = DataComponentPatch.CODEC.encodeStart(ops, mcStack.getComponentsPatch());
        var components = NBTTranslator.INSTANCE.translate((CompoundTag) componentsTag.getOrThrow());
        container.set(Constants.ItemStack.COMPONENTS, components);
        return container;
    }

    @NotNull
    public static EquipmentSlotGroup asEquipmentSlotGroup(final EquipmentType equipmentType) {
        // TODO expose EquipmentSlotGroup?
        return switch (((EquipmentSlot) (Object) equipmentType)) {
            case MAINHAND -> EquipmentSlotGroup.MAINHAND;
            case OFFHAND -> EquipmentSlotGroup.OFFHAND;
            case FEET -> EquipmentSlotGroup.FEET;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case HEAD -> EquipmentSlotGroup.HEAD;
            case BODY -> EquipmentSlotGroup.BODY;
        };
    }


    private static void cleanupOldCustomData(final net.minecraft.world.item.ItemStack stack) {
        final CompoundTag unsafe = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        unsafe.remove(Constants.Sponge.Data.V2.SPONGE_DATA); // and its V2.CUSTOM_MANIPULATOR_TAG_LIST
        Constants.NBT.filterSpongeCustomData(unsafe); // FORGE_DATA > V2.SPONGE_DATA also removes it if empty
    }

    // TODO updater for old (maybe V2?) Constants.Sponge.DATA_MANIPULATORS
    public static final ImmutableList<DataContentUpdater> STACK_UPDATERS = ImmutableList.of(
                    ItemStackSnapshotDuplicateManipulatorUpdater.INSTANCE,
                    ItemStackDataComponentsUpdater.INSTANCE);

    @NotNull
    public static Optional<ItemStack> createItemStack(final DataView container) {
        Objects.requireNonNull(container);

        // Update Data if needed
        final Integer version = container.getInt(Queries.CONTENT_VERSION).orElse(1);
        ImmutableList.Builder<DataContentUpdater> builder = ImmutableList.builder();
        int lastUpdaterVersion = version;
        for (final DataContentUpdater updater : STACK_UPDATERS) {
            if (lastUpdaterVersion == updater.inputVersion()) {
                lastUpdaterVersion = updater.outputVersion();
                builder.add(updater);
            }
        }

        final DataUpdaterDelegate delegate = new DataUpdaterDelegate(builder.build(), version, Constants.ItemStack.Data.CURRENT_VERSION);
        final DataView updatedContainer = delegate.update(container);

        // Check if we have valid updated stack
        if (!updatedContainer.contains(Constants.ItemStack.TYPE, Constants.ItemStack.COUNT)) {
            return Optional.empty();
        }

        final int count = updatedContainer.getInt(Constants.ItemStack.COUNT).get();

        final ItemType itemType = updatedContainer.getRegistryValue(Constants.ItemStack.TYPE, RegistryTypes.ITEM_TYPE)
                .orElseThrow(() -> new IllegalStateException("Unable to find item with id: "));
        final var mcStack = new net.minecraft.world.item.ItemStack((Item) itemType, count);
        if (!mcStack.isEmpty()) { // ignore components when the stack is empty anyways
            // Read and apply components from container to mc stack
            mcStack.applyComponents(SpongeItemStack.patchFromData(updatedContainer));
        }
        return Optional.of((ItemStack) (Object) mcStack);
    }

    public static DataComponentPatch patchFromData(final DataView container) {
        return container.getView(Constants.ItemStack.COMPONENTS).map(NBTTranslator.INSTANCE::translate).flatMap(compound -> {
            var dynamic = new Dynamic<>(RegistryOps.create(NbtOps.INSTANCE, SpongeCommon.vanillaRegistryAccess()), compound);
            return DataComponentPatch.CODEC.decode(dynamic).result().map(Pair::getFirst);
        }).orElse(DataComponentPatch.EMPTY);
    }
}
