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
package org.spongepowered.common.world.generation.structure.jigsaw;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.datapack.DataPack;
import org.spongepowered.api.datapack.DataPacks;
import org.spongepowered.api.world.generation.structure.jigsaw.Processor;
import org.spongepowered.api.world.generation.structure.jigsaw.ProcessorList;
import org.spongepowered.api.world.generation.structure.jigsaw.ProcessorListTemplate;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.AbstractDataPackEntryBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record SpongeProcessorListTemplate(ResourceKey key, StructureProcessorList representedProcessors, DataPack<ProcessorListTemplate> pack) implements ProcessorListTemplate {

    @Override
    public ProcessorList processorList() {
        return (ProcessorList) this.representedProcessors;
    }

    @Override
    public int contentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        final JsonElement serialized = SpongeProcessorListTemplate.encode(this, SpongeCommon.vanillaRegistryAccess());
        try {
            return DataFormats.JSON.get().read(serialized.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read deserialized ProcessorList:\n" + serialized, e);
        }
    }

    public static JsonElement encode(final ProcessorListTemplate template, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return StructureProcessorType.LIST_OBJECT_CODEC.encodeStart(ops, ((StructureProcessorList) template.processorList())).getOrThrow();
    }

    public static StructureProcessorList decode(final JsonElement json, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return StructureProcessorType.LIST_OBJECT_CODEC.parse(ops, json).getOrThrow();
    }

    public static SpongeProcessorListTemplate decode(final DataPack<ProcessorListTemplate> pack, final ResourceKey key, final JsonElement json, final RegistryAccess registryAccess) {
        final StructureProcessorList parsed = SpongeProcessorListTemplate.decode(json, registryAccess);
        return new SpongeProcessorListTemplate(key, parsed, pack);
    }

    public static final class BuilderImpl extends AbstractDataPackEntryBuilder<ProcessorList, ProcessorListTemplate, Builder> implements Builder {

        @Nullable private StructureProcessorList processorList;

        public BuilderImpl() {
            this.reset();
        }

        @Override
        public Function<ProcessorListTemplate, ProcessorList> valueExtractor() {
            return value -> (ProcessorList) ((SpongeProcessorListTemplate) value).representedProcessors;
        }

        @Override
        public Builder fromValues(final List<Processor> processorList) {
            this.processorList = new StructureProcessorList((List) processorList);
            return this;
        }

        @Override
        public Builder fromValue(final ProcessorList processorList) {
            this.processorList = (StructureProcessorList) processorList;
            return this;
        }

        @Override
        public Builder reset() {
            this.pack = DataPacks.PROCESSOR_LIST;
            this.key = null;
            this.processorList = null;
            return this;
        }

        @Override
        public Builder fromDataPack(final DataView datapack) throws IOException {
            final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(datapack));
            final StructureProcessorList decoded = SpongeProcessorListTemplate.decode(json, SpongeCommon.vanillaRegistryAccess());
            this.fromValue((ProcessorList) decoded);
            return this;
        }

        @Override
        protected SpongeProcessorListTemplate build0() {
            Objects.requireNonNull(this.processorList, "processorList");
            return new SpongeProcessorListTemplate(this.key, this.processorList, this.pack);
        }
    }
}
