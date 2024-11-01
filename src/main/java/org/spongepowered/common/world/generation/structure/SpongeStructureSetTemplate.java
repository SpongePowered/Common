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
package org.spongepowered.common.world.generation.structure;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.datapack.DataPack;
import org.spongepowered.api.datapack.DataPacks;
import org.spongepowered.api.world.generation.structure.Structure;
import org.spongepowered.api.world.generation.structure.StructureSetTemplate;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.AbstractDataPackEntryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record SpongeStructureSetTemplate(ResourceKey key, StructureSet representedStructureSet, DataPack<StructureSetTemplate> pack) implements StructureSetTemplate {

    @Override
    public org.spongepowered.api.world.generation.structure.StructureSet structureSet() {
        return (org.spongepowered.api.world.generation.structure.StructureSet) (Object) this.representedStructureSet;
    }

    @Override
    public int contentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        final JsonElement serialized = SpongeStructureSetTemplate.encode(this, SpongeCommon.vanillaRegistryAccess());
        try {
            return DataFormats.JSON.get().read(serialized.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read deserialized Structure:\n" + serialized, e);
        }
    }

    public static JsonElement encode(final StructureSetTemplate template, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return StructureSet.DIRECT_CODEC.encodeStart(ops, (StructureSet) (Object) template.structureSet()).getOrThrow();
    }

    public static StructureSet decode(final JsonElement json, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return StructureSet.DIRECT_CODEC.parse(ops, json).getOrThrow();
    }

    public static SpongeStructureSetTemplate decode(final DataPack<StructureSetTemplate> pack, final ResourceKey key, final JsonElement packEntry, final RegistryAccess registryAccess) {
        final StructureSet parsed = SpongeStructureSetTemplate.decode(packEntry, registryAccess);
        return new SpongeStructureSetTemplate(key, parsed, pack);
    }

    public static final class BuilderImpl extends AbstractDataPackEntryBuilder<org.spongepowered.api.world.generation.structure.StructureSet, StructureSetTemplate, Builder> implements Builder {

        @Nullable private StructurePlacement placement;
        private List<StructureSet.StructureSelectionEntry> structureSelectionEntries = new ArrayList<>();

        public BuilderImpl() {
            this.reset();
        }

        @Override
        public Function<StructureSetTemplate, org.spongepowered.api.world.generation.structure.StructureSet> valueExtractor() {
            return StructureSetTemplate::structureSet;
        }

        @Override
        public Builder fromValue(final org.spongepowered.api.world.generation.structure.StructureSet structureSet) {
            var mcSet = (StructureSet) (Object) structureSet;
            this.structureSelectionEntries = mcSet.structures();
            this.placement = mcSet.placement();
            return this;
        }

        @Override
        public Builder fromDataPack(final DataView pack) throws IOException {
            final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(pack));
            final StructureSet decoded = SpongeStructureSetTemplate.decode(json, SpongeCommon.vanillaRegistryAccess());
            this.structureSelectionEntries = decoded.structures();
            this.placement = decoded.placement();
            return this;
        }

        @Override
        public Builder placement(final org.spongepowered.api.world.generation.structure.StructurePlacement placement) {
            this.placement = (StructurePlacement) placement;
            return this;
        }

        @Override
        public Builder add(final Structure structure, final int weight) {
            final Registry<net.minecraft.world.level.levelgen.structure.Structure> registry = SpongeCommon.vanillaRegistry(Registries.STRUCTURE);
            final var mcStructure = (net.minecraft.world.level.levelgen.structure.Structure) structure;
            Holder<net.minecraft.world.level.levelgen.structure.Structure> holder;
            try {
                holder = registry.createIntrusiveHolder(mcStructure);
            } catch (Exception e) {
                holder = Holder.direct(mcStructure);
            }
            this.structureSelectionEntries.add(new StructureSet.StructureSelectionEntry(holder, weight));
            return this;
        }

        @Override
        public Builder reset() {
            super.reset();
            this.pack = DataPacks.STRUCTURE_SET;
            this.placement = null;
            this.structureSelectionEntries = new ArrayList<>();
            return this;
        }

        @Override
        protected SpongeStructureSetTemplate build0() {
            Objects.requireNonNull(this.placement, "placement");
            return new SpongeStructureSetTemplate(this.key, new StructureSet(this.structureSelectionEntries, this.placement), this.pack);
        }
    }
}
