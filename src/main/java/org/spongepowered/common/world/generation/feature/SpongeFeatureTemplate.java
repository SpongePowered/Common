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
package org.spongepowered.common.world.generation.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.datapack.DataPack;
import org.spongepowered.api.datapack.DataPacks;
import org.spongepowered.api.world.generation.feature.Feature;
import org.spongepowered.api.world.generation.feature.FeatureTemplate;
import org.spongepowered.api.world.generation.feature.FeatureType;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.AbstractDataPackEntryBuilder;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

public record SpongeFeatureTemplate(ResourceKey key, ConfiguredFeature<?, ?> representedFeature, DataPack<FeatureTemplate> pack) implements FeatureTemplate {

    @Override
    public int contentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        final JsonElement serialized = SpongeFeatureTemplate.encode(this, SpongeCommon.vanillaRegistryAccess());
        try {
            return DataFormats.JSON.get().read(serialized.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read deserialized Feature:\n" + serialized, e);
        }
    }

    @Override
    public org.spongepowered.api.world.generation.feature.Feature feature() {
        return (org.spongepowered.api.world.generation.feature.Feature) (Object) this.representedFeature;
    }

    public static JsonElement encode(final FeatureTemplate template, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return ConfiguredFeature.DIRECT_CODEC.encodeStart(ops, (ConfiguredFeature<?, ?>) (Object) template.feature()).getOrThrow();
    }

    public static ConfiguredFeature<?, ?> decode(final JsonElement json, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return ConfiguredFeature.DIRECT_CODEC.parse(ops, json).getOrThrow();
    }

    public static SpongeFeatureTemplate decode(final DataPack<FeatureTemplate> pack, final ResourceKey key, final JsonElement packEntry, final RegistryAccess registryAccess) {
        final ConfiguredFeature<?, ?> parsed = SpongeFeatureTemplate.decode(packEntry, registryAccess);
        return new SpongeFeatureTemplate(key, parsed, pack);
    }

    public static class BuilderImpl extends AbstractDataPackEntryBuilder<Feature, FeatureTemplate, Builder> implements Builder {

        private net.minecraft.world.level.levelgen.feature.Feature<?> type;
        private FeatureConfiguration config;

        public BuilderImpl() {
            this.reset();
        }

        @Override
        public Function<FeatureTemplate, Feature> valueExtractor() {
            return FeatureTemplate::feature;
        }

        @Override
        public Builder type(final FeatureType type) {
            this.type = (net.minecraft.world.level.levelgen.feature.Feature<?>) type;
            return this;
        }

        @Override
        public Builder reset() {
            this.key = null;
            this.type = null;
            this.config = null;
            this.pack = DataPacks.FEATURE;
            return this;
        }

        @Override
        public Builder fromValue(final org.spongepowered.api.world.generation.feature.Feature feature) {
            this.type(feature.type());
            this.config = ((ConfiguredFeature) (Object) feature).config();
            return this;
        }

        @Override
        public Builder fromDataPack(final DataView pack) throws IOException {
            final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(pack));
            final ConfiguredFeature<?, ?> feature = SpongeFeatureTemplate.decode(json, SpongeCommon.vanillaRegistryAccess());
            this.fromValue((Feature) (Object) feature);
            return this;
        }

        @Override
        protected FeatureTemplate build0() {
            Objects.requireNonNull(this.type, "config");
            Objects.requireNonNull(this.config, "config");
            final ConfiguredFeature<?, ?> feature = new ConfiguredFeature<>((net.minecraft.world.level.levelgen.feature.Feature<? super FeatureConfiguration>) this.type, this.config);
            return new SpongeFeatureTemplate(this.key, feature, this.pack);
        }
    }
}
