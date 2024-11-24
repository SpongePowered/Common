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
package org.spongepowered.common.inventory.custom;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import org.apache.commons.lang3.Validate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.ContainerType;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackLike;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.api.registry.DefaultedRegistryReference;
import org.spongepowered.common.inventory.lens.Lens;
import org.spongepowered.common.inventory.lens.LensCreator;
import org.spongepowered.common.inventory.lens.impl.DefaultEmptyLens;
import org.spongepowered.common.inventory.lens.impl.DefaultIndexedLens;
import org.spongepowered.common.inventory.lens.impl.LensRegistrar;
import org.spongepowered.common.inventory.lens.impl.comp.GridInventoryLens;
import org.spongepowered.common.inventory.lens.impl.minecraft.BrewingStandInventoryLens;
import org.spongepowered.common.inventory.lens.impl.minecraft.FurnaceInventoryLens;
import org.spongepowered.common.inventory.lens.impl.slot.SlotLensProvider;
import org.spongepowered.common.inventory.lens.slots.SlotLens;
import org.spongepowered.math.vector.Vector2i;
import org.spongepowered.plugin.PluginContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class SpongeViewableInventoryBuilder implements ViewableInventory.Builder, ViewableInventory.Builder.DummyStep, ViewableInventory.Builder.EndStep {
    private ContainerType type;

    private Map<Integer, Slot> slotDefinitions;
    private Slot lastSlot;

    private PluginContainer plugin;
    private Carrier carrier;
    private UUID identity;

    private List<Inventory> finalInventories;
    private Lens finalLens;
    private SlotLensProvider finalProvider;
    private ContainerTypeInfo info;

    @Override
    public BuildingStep type(ContainerType type) {
        Validate.isTrue(SpongeViewableInventoryBuilder.containerTypeInfo().containsKey(type), "Container Type cannot be used for this: " + type);
        this.type = type;
        this.slotDefinitions = new HashMap<>();
        this.info = SpongeViewableInventoryBuilder.containerTypeInfo().get(type);
        return this;
    }

    // Helpers

    private int posToIndex(Vector2i pos) {
        return this.posToIndex(pos.x(), pos.y());
    }

    private int posToIndex(int x, int y) {
        Validate.isTrue(x <= this.info.width, "Target inventory is too small: " + this.info.width + " < " + x);
        Validate.isTrue(y <= this.info.height, "Target inventory is too small: " + this.info.height + " < " + y);
        return y * this.info.width + x;
    }

    private Vector2i indexToPos(int offset) {
        Validate.isTrue(offset <= this.info.width * this.info.height, "Target inventory is too small: " + this.info.width * this.info.height + " < " + offset);
        int x = offset / this.info.height;
        int y = offset % this.info.width;
        return new Vector2i(x, y);
    }

    private Slot newDummySlot() {
        Container dummyInv = new net.minecraft.world.SimpleContainer(1);
        return ((Inventory) dummyInv).slot(0).get();
    }

    // Slot definition Impl:
    public BuildingStep slotsAtIndizes(List<Slot> source, List<Integer> at) {
        Validate.isTrue(source.size() == at.size(), "Source and index list sizes differ");
        this.lastSlot = null;
        for (int i = 0; i < at.size(); i++) {
            Slot slot = source.get(i);
            Integer index = at.get(i);
            this.slotDefinitions.put(index, slot);
            this.lastSlot = slot;
        }
        return this;
    }

    // complex redirects - (source/index list generation)
    public BuildingStep slotsAtPositions(List<Slot> source, List<Vector2i> at) {
        return this.slotsAtIndizes(source, at.stream().map(this::posToIndex).collect(Collectors.toList()));
    }

    public DummyStep fillDummy() {
        Slot slot = this.newDummySlot();
        List<Integer> indizes = IntStream.range(0, this.info.size).boxed().filter(idx -> !this.slotDefinitions.containsKey(idx)).collect(Collectors.toList());
        List<Slot> source = Stream.generate(() -> slot).limit(indizes.size()).collect(Collectors.toList());
        this.slotsAtIndizes(source, indizes);
        return this;
    }

    public DummyStep dummySlots(int count, int offset) {
        Slot slot = this.newDummySlot();
        List<Slot> source = Stream.generate(() -> slot).limit(count).collect(Collectors.toList());
        this.slots(source, offset);
        return this;
    }

    public BuildingStep slots(List<Slot> source, int offset) {
        List<Integer> indizes = IntStream.range(offset, offset + source.size()).boxed().collect(Collectors.toList());
        return this.slotsAtIndizes(source, indizes);
    }

    public DummyStep dummyGrid(Vector2i size, Vector2i offset) {
        Slot slot = this.newDummySlot();
        List<Slot> source = Stream.generate(() -> slot).limit(size.x() * size.y()).collect(Collectors.toList());
        this.grid(source, size, offset);
        return this;
    }

    public BuildingStep grid(List<Slot> source, Vector2i size, Vector2i offset) {
        int xMin = offset.x();
        int yMin = offset.y();
        int xMax = xMin + size.x() - 1;
        int yMax = yMin + size.y() - 1;

        List<Integer> indizes = new ArrayList<>();
        for (int y = yMin; y <= yMax; y++) {
            for (int x = xMin; x <= xMax; x++) {
                indizes.add(this.posToIndex(x, y));
            }
        }
        return this.slotsAtIndizes(source, indizes);
    }

    // simple redirects

    public DummyStep dummySlots(int count, Vector2i offset) {
        return this.dummySlots(count, this.posToIndex(offset));
    }

    public BuildingStep slots(List<Slot> source, Vector2i offset) {
        return this.slots(source, this.posToIndex(offset));
    }

    public DummyStep dummyGrid(Vector2i size, int offset) {
        return this.dummyGrid(size, this.indexToPos(offset));
    }

    @Override
    public BuildingStep grid(List<Slot> source, Vector2i size, int offset) {
        return this.grid(source, size, this.indexToPos(offset));
    }
    // dummy
    @Override
    public BuildingStep item(ItemStackLike item) {
        if (this.lastSlot != null) {
            this.lastSlot.set(item);
        }
        return this;
    }

    @Override
    public EndStep plugin(final PluginContainer plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        return this;
    }

    public EndStep identity(UUID uuid) {
        this.identity = uuid;
        return this;
    }
    @Override
    public EndStep carrier(Carrier carrier) {
        this.carrier = carrier;
        return this;
    }
    // Build

    @Override
    public EndStep completeStructure() {
        if (!this.slotDefinitions.isEmpty()) {
            this.fillDummy();
//            this.finalInventories = this.slotDefinitions.values().stream().map(Inventory::parent).distinct().collect(Collectors.toList());
// TODO custom slot provider with reduces inventories
//            CustomSlotProvider slotProvider = new CustomSlotProvider();
//            for (Map.Entry<Integer, Slot> entry : this.slotDefinitions.entrySet()) {
//                Slot slot = entry.getValue();
//                int idx = slot.get(Keys.SLOT_INDEX).get();
//                int offset = 0;
//                for (int i = 0; i < this.finalInventories.indexOf(slot.parent()); i++) {
//                    offset += this.finalInventories.get(i).freeCapacity();
//                }
//                slotProvider.add(new BasicSlotLens(idx + offset));
//            }
//            this.finalProvider = slotProvider;

            this.finalInventories = this.slotDefinitions.values().stream().map(Inventory.class::cast).collect(Collectors.toList());
        }

        this.finalProvider = new LensRegistrar.BasicSlotLensProvider(this.info.size);
        this.finalLens = SpongeViewableInventoryBuilder.containerTypeInfo().get(this.type).lensCreator.createLens(this.finalProvider);
        return this;
    }

    @Override
    public ViewableInventory.Custom build() {
        if (this.plugin == null) {
            throw new IllegalStateException("Plugin has not been set on this builder!");
        }

        if (this.slotDefinitions.isEmpty() && this.info.size != 0) {
            Inventory inventory = Inventory.builder().slots(this.info.size).completeStructure().plugin(this.plugin).build();
            this.finalInventories = Arrays.asList(inventory);
        }

        final ViewableCustomInventory inventory = new ViewableCustomInventory(this.plugin, this.type, SpongeViewableInventoryBuilder.containerTypeInfo()
                .get(this.type), this.info.size, this.finalLens, this.finalProvider, this.finalInventories, this.identity, this.carrier);
        if (this.slotDefinitions.isEmpty()) {
            inventory.vanilla();
        }
        return ((ViewableInventory.Custom) inventory);
    }

    public ViewableInventory.Builder reset() {
        this.plugin = null;
        this.type = null;
        this.info = null;

        this.slotDefinitions = null;
        this.lastSlot = null;

        this.carrier = null;
        this.identity = null;

        this.finalInventories = null;
        this.finalLens = null;
        this.finalProvider = null;
        return this;
    }

    public static class CustomSlotProvider implements SlotLensProvider {

        private List<SlotLens> lenses = new ArrayList<>();

        public void add(SlotLens toAdd) {
            this.lenses.add(toAdd);
        }

        @Override
        public SlotLens getSlotLens(int index) {
            return this.lenses.get(index);
        }
    }

    private static Map<ContainerType, ContainerTypeInfo> containerTypeInfo;

    public static void register(final DefaultedRegistryReference<ContainerType> ref, final ContainerTypeInfo info) {
        SpongeViewableInventoryBuilder.containerTypeInfo.put(ref.get(), info);
    }


    public static Map<ContainerType, ContainerTypeInfo> containerTypeInfo() {
        if (SpongeViewableInventoryBuilder.containerTypeInfo != null) {
            return SpongeViewableInventoryBuilder.containerTypeInfo;
        }
        SpongeViewableInventoryBuilder.containerTypeInfo = new HashMap<>();
        SpongeViewableInventoryBuilder.register(ContainerTypes.GENERIC_3X3,
                ContainerTypeInfo.ofGrid(3, 3, (id, i, p, vi) -> new DispenserMenu(id, i, vi)));
        SpongeViewableInventoryBuilder.register(ContainerTypes.GENERIC_9X1,
                ContainerTypeInfo.ofGrid(9, 1, (id, i, p, vi) -> new ChestMenu(MenuType.GENERIC_9x1, id, i, vi, 1)));
        SpongeViewableInventoryBuilder.register(ContainerTypes.GENERIC_9X2,
                ContainerTypeInfo.ofGrid(9, 2, (id, i, p, vi) -> new ChestMenu(MenuType.GENERIC_9x2, id, i, vi, 2)));
        SpongeViewableInventoryBuilder.register(ContainerTypes.GENERIC_9X3,
                ContainerTypeInfo.ofGrid(9, 3, (id, i, p, vi) -> ChestMenu.threeRows(id, i, vi)));
        SpongeViewableInventoryBuilder.register(ContainerTypes.GENERIC_9X4,
                ContainerTypeInfo.ofGrid(9, 4, (id, i, p, vi) -> new ChestMenu(MenuType.GENERIC_9x4, id, i, vi, 4)));
        SpongeViewableInventoryBuilder.register(ContainerTypes.GENERIC_9X5,
                ContainerTypeInfo.ofGrid(9, 5, (id, i, p, vi) -> new ChestMenu(MenuType.GENERIC_9x5, id, i, vi, 5)));
        SpongeViewableInventoryBuilder.register(ContainerTypes.GENERIC_9X6,
                ContainerTypeInfo.ofGrid(9, 6, (id, i, p, vi) -> ChestMenu.sixRows(id, i, vi)));
        SpongeViewableInventoryBuilder.register(ContainerTypes.HOPPER,
                ContainerTypeInfo.ofGrid(5, 1, (id, i, p, vi) -> new HopperMenu(id, i, vi)));
        SpongeViewableInventoryBuilder.register(ContainerTypes.SHULKER_BOX, // Container prevents ShulkerBoxes in Shulkerboxes
                ContainerTypeInfo.ofGrid(9, 3, (id, i, p, vi) -> new ShulkerBoxMenu(id, i, vi)));

        // With IntArray data - data is synced with Container - but not ticked as the TileEntity do that normally
        SpongeViewableInventoryBuilder.register(ContainerTypes.BLAST_FURNACE,
                ContainerTypeInfo.of(FurnaceInventoryLens::new, 3,4, (id, i, p, vi) -> new BlastFurnaceMenu(id, i, vi, vi.getData())));
        SpongeViewableInventoryBuilder.register(ContainerTypes.BREWING_STAND,
                ContainerTypeInfo.of(BrewingStandInventoryLens::new, 5,2, (id, i, p, vi) -> new BrewingStandMenu(id, i, vi, vi.getData())));
        SpongeViewableInventoryBuilder.register(ContainerTypes.FURNACE,
                ContainerTypeInfo.of(FurnaceInventoryLens::new, 3,4, (id, i, p, vi) -> new FurnaceMenu(id, i, vi, vi.getData())));
        SpongeViewableInventoryBuilder.register(ContainerTypes.LECTERN,
                ContainerTypeInfo.of(1, 1, (id, i, p, vi) -> new LecternMenu(id, vi, vi.getData())));
        SpongeViewableInventoryBuilder.register(ContainerTypes.SMOKER,
                ContainerTypeInfo.of(3, 4, (id, i, p, vi) -> new SmokerMenu(id, i, vi, vi.getData())));

        // Containers with internal Inventory
        // TODO how to handle internal Container inventories?
        SpongeViewableInventoryBuilder.register(ContainerTypes.ANVIL, // 3 internal slots
                ContainerTypeInfo.of(0, (id, i, p, vi) -> new AnvilMenu(id, i, SpongeViewableInventoryBuilder.toPos(p))));
        SpongeViewableInventoryBuilder.register(ContainerTypes.BEACON, // 1 internal slot
                ContainerTypeInfo.of( 3, (id, i, p, vi) -> new BeaconMenu(id, i, vi.getData(), SpongeViewableInventoryBuilder.toPos(p))));
        SpongeViewableInventoryBuilder.register(ContainerTypes.CARTOGRAPHY_TABLE,  // 2 internal slots
                ContainerTypeInfo.of( 0, (id, i, p, vi) -> new CartographyTableMenu(id, i, SpongeViewableInventoryBuilder.toPos(p))));
        SpongeViewableInventoryBuilder.register(ContainerTypes.CRAFTING, // 3x3+1 10 internal slots
                ContainerTypeInfo.of( 0, (id, i, p, vi) -> new CraftingMenu(id, i, SpongeViewableInventoryBuilder.toPos(p))));
        SpongeViewableInventoryBuilder.register(ContainerTypes.ENCHANTMENT, // 3 internal slot
                ContainerTypeInfo.of( 0, (id, i, p, vi) -> new EnchantmentMenu(id, i, SpongeViewableInventoryBuilder.toPos(p))));
        SpongeViewableInventoryBuilder.register(ContainerTypes.GRINDSTONE, // 2 internal slot
                ContainerTypeInfo.of( 0, (id, i, p, vi) -> new GrindstoneMenu(id, i, SpongeViewableInventoryBuilder.toPos(p))));
        SpongeViewableInventoryBuilder.register(ContainerTypes.LOOM, // 3 internal slot
                ContainerTypeInfo.of( 0, (id, i, p, vi) -> new LoomMenu(id, i, SpongeViewableInventoryBuilder.toPos(p))));
        SpongeViewableInventoryBuilder.register(ContainerTypes.STONECUTTER, // 1 internal slot
                ContainerTypeInfo.of( 0, (id, i, p, vi) -> new StonecutterMenu(id, i, SpongeViewableInventoryBuilder.toPos(p))));

        SpongeViewableInventoryBuilder.register(ContainerTypes.MERCHANT, ContainerTypeInfo.of(sp -> new DefaultEmptyLens(), 0, 0,
                (id, i, p, vi) -> {
                    final Villager merchant = new Villager(EntityType.VILLAGER, p.level());
                    merchant.setPos(p.position());
                    merchant.setVillagerData(merchant.getVillagerData().setLevel(5));
                    return new MerchantMenu(id, i, vi.applyTradeOffers(merchant));
                }));

        // Containers that need additional Info to construct

        // TODO ContainerTypes.HORSE
        // horse is used for distance to player
        // checking HorseArmor Item in Slot
        // chested State and capacity (hasChest/getInventoryColumns) to add more Slots
        int chestColumns = 0;
        AbstractHorse horse = null;
        ContainerTypeInfo.of(0, 0,
                (id, i, p, vi) -> new HorseInventoryMenu(id, i, vi, horse, chestColumns));

        return SpongeViewableInventoryBuilder.containerTypeInfo;
    }

    private static ContainerLevelAccess toPos(Player p) {
        return ContainerLevelAccess.create(p.level(), p.blockPosition());
    }

    @FunctionalInterface
    public interface CustomInventoryContainerProvider {
        @Nullable AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player player, ViewableCustomInventory customInv);
    }

    public static class ContainerTypeInfo {
        public final LensCreator lensCreator;
        public final Supplier<SimpleContainerData> dataProvider;
        public final CustomInventoryContainerProvider containerProvider;

        public final int width;
        public final int height;
        public final int size;

        public ContainerTypeInfo(LensCreator lensCreator, Supplier<SimpleContainerData> dataProvider,
                CustomInventoryContainerProvider containerProvider, int width, int height, int size) {
            this.lensCreator = lensCreator;
            this.dataProvider = dataProvider;
            this.containerProvider = containerProvider;
            this.width = width;
            this.height = height;
            this.size = size;
        }

        public static ContainerTypeInfo of(LensCreator lensCreator, int size, int dataSize, CustomInventoryContainerProvider provider) {
            return new ContainerTypeInfo(lensCreator, () -> new SimpleContainerData(dataSize), provider, 0, 0, size);
        }

        public static ContainerTypeInfo of(int dataSize, CustomInventoryContainerProvider provider) {
            return new ContainerTypeInfo(sp -> new DefaultEmptyLens(), () -> new SimpleContainerData(dataSize), provider, 0, 0, 0);
        }

        public static ContainerTypeInfo of(int size, int dataSize, CustomInventoryContainerProvider provider) {
            return new ContainerTypeInfo(sp -> new DefaultIndexedLens(0, size, sp), () -> new SimpleContainerData(dataSize), provider, 0, 0, size);
        }

        public static ContainerTypeInfo ofGrid(int width, int height, CustomInventoryContainerProvider provider) {
            return new ContainerTypeInfo(sp -> new GridInventoryLens(0, width, height, sp), () -> null, provider, width, height, width * height);
        }

        public static ContainerTypeInfo ofGrid(int width, int height, int size, CustomInventoryContainerProvider provider) {
            return new ContainerTypeInfo(sp -> new GridInventoryLens(0, width, height, sp), () -> null, provider, width, height, size);
        }
    }

}
