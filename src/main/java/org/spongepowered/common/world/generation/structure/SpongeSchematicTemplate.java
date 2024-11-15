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

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.datapack.DataPack;
import org.spongepowered.api.datapack.DataPacks;
import org.spongepowered.api.world.generation.structure.SchematicTemplate;
import org.spongepowered.api.world.schematic.Schematic;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.data.persistence.NBTTranslator;
import org.spongepowered.common.util.AbstractResourceKeyedBuilder;

import java.util.Objects;

public record SpongeSchematicTemplate(ResourceKey key, StructureTemplate nbtStructure, DataPack<SchematicTemplate> pack) implements SchematicTemplate {

    @Override
    public Schematic schematic() {
        return (Schematic) this.nbtStructure;
    }

    @Override
    public int contentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        final CompoundTag serialized = SpongeSchematicTemplate.encode(this, SpongeCommon.vanillaRegistryAccess());
        return NBTTranslator.INSTANCE.translateFrom(serialized);
    }

    public static CompoundTag encode(final SchematicTemplate template, final RegistryAccess registryAccess) {
        return ((SpongeSchematicTemplate) template).nbtStructure.save(new CompoundTag());
    }

    public static StructureTemplate decode(final CompoundTag nbt, final RegistryAccess registryAccess) {
        return SpongeCommon.server().getStructureManager().readStructure(nbt);
    }

    public static SpongeSchematicTemplate decode(final DataPack<SchematicTemplate> pack, final ResourceKey key, final CompoundTag packEntry, final RegistryAccess registryAccess) {
        final StructureTemplate parsed = SpongeSchematicTemplate.decode(packEntry, registryAccess);
        return new SpongeSchematicTemplate(key, parsed, pack);
    }

    public static final class BuilderImpl extends AbstractResourceKeyedBuilder<SchematicTemplate, Builder> implements Builder {

        private DataPack<SchematicTemplate> pack = DataPacks.SCHEMATIC;
        @Nullable private StructureTemplate nbtStructure;

        public BuilderImpl() {
            this.reset();
        }

        @Override
        public Builder from(final SchematicTemplate value) {
            this.nbtStructure = ((SpongeSchematicTemplate) value).nbtStructure;
            return this;
        }

        @Override
        public Builder reset() {
            this.pack = DataPacks.SCHEMATIC;
            this.nbtStructure = null;
            return this;
        }

        @Override
        public Builder pack(final DataPack<SchematicTemplate> pack) {
            this.pack = pack;
            return this;
        }

        @Override
        protected SpongeSchematicTemplate build0() {
            Objects.requireNonNull(this.nbtStructure, "structure");
            return new SpongeSchematicTemplate(this.key, this.nbtStructure, this.pack);
        }
    }
}
