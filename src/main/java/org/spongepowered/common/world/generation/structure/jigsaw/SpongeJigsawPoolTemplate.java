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
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.datapack.DataPack;
import org.spongepowered.api.datapack.DataPacks;
import org.spongepowered.api.registry.RegistryReference;
import org.spongepowered.api.world.generation.structure.jigsaw.JigsawPool;
import org.spongepowered.api.world.generation.structure.jigsaw.JigsawPoolElement;
import org.spongepowered.api.world.generation.structure.jigsaw.JigsawPoolTemplate;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.world.level.levelgen.structure.pools.StructureTemplatePoolAccessor;
import org.spongepowered.common.util.AbstractDataPackEntryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record SpongeJigsawPoolTemplate(ResourceKey key, StructureTemplatePool representedJigsawPool, DataPack<JigsawPoolTemplate> pack) implements JigsawPoolTemplate {

    @Override
    public JigsawPool jigsawPool() {
        return (JigsawPool) this.representedJigsawPool;
    }
    @Override
    public int contentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        final JsonElement serialized = SpongeJigsawPoolTemplate.encode(this, SpongeCommon.vanillaRegistryAccess());
        try {
            return DataFormats.JSON.get().read(serialized.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read deserialized Structure:\n" + serialized, e);
        }
    }

    public static JsonElement encode(final JigsawPoolTemplate template, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return StructureTemplatePool.DIRECT_CODEC.encodeStart(ops, (StructureTemplatePool) template.jigsawPool()).getOrThrow();
    }

    public static StructureTemplatePool decode(final JsonElement json, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return StructureTemplatePool.DIRECT_CODEC.parse(ops, json).getOrThrow();
    }

    public static SpongeJigsawPoolTemplate decode(final DataPack<JigsawPoolTemplate> pack, final ResourceKey key, final JsonElement packEntry, final RegistryAccess registryAccess) {
        final StructureTemplatePool parsed = SpongeJigsawPoolTemplate.decode(packEntry, registryAccess);
        return new SpongeJigsawPoolTemplate(key, parsed, pack);
    }

    public static final class BuilderImpl extends AbstractDataPackEntryBuilder<JigsawPool, JigsawPoolTemplate, Builder> implements Builder {

        private Holder<StructureTemplatePool> fallback;
        private List<Pair<StructurePoolElement, Integer>> templates;

        public BuilderImpl() {
            this.reset();
        }

        @Override
        public Function<JigsawPoolTemplate, JigsawPool> valueExtractor() {
            return JigsawPoolTemplate::jigsawPool;
        }

        @Override
        public Builder fromValue(final JigsawPool StructureTemplatePool) {
            var mcPool = (StructureTemplatePool) StructureTemplatePool;
            this.fallback = mcPool.getFallback();
            this.templates = new ArrayList<>(((StructureTemplatePoolAccessor) mcPool).accessor$rawTemplates());
            return this;
        }

        @Override
        public Builder fromDataPack(final DataView pack) throws IOException {
            final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(pack));
            final StructureTemplatePool decoded = SpongeJigsawPoolTemplate.decode(json, SpongeCommon.vanillaRegistryAccess());
            return this.fromValue((JigsawPool) decoded);
        }

        @Override
        public Builder add(final JigsawPoolElement element, final int weight) {
            this.templates.add(Pair.of((StructurePoolElement) element, weight));
            return this;
        }

        @Override
        public Builder name(final ResourceKey name) {
// TODO            this.name = (ResourceLocation) (Object) name;
            return this;
        }

        @Override
        public Builder fallback(final RegistryReference<JigsawPool> fallback) {
// TODO            this.fallback = (ResourceLocation) (Object) fallback.location();
            return this;
        }

        @Override
        public Builder fallback(final JigsawPoolTemplate fallback) {
// TODO            this.fallback = (ResourceLocation) (Object) fallback.key();
            return this;
        }

        @Override
        public Builder reset() {
            super.reset();
            this.pack = DataPacks.JIGSAW_POOL;
            // TODO this.name = null;
            // TODO this.fallback = new ResourceLocation("empty");
            this.templates = new ArrayList<>();
            return this;
        }

        @Override
        protected SpongeJigsawPoolTemplate build0() {
            // TODO Objects.requireNonNull(this.name, "name");
            return new SpongeJigsawPoolTemplate(this.key, new StructureTemplatePool(this.fallback, this.templates), this.pack);
        }
    }
}
