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
package org.spongepowered.common.world.generation.config.noise;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.datapack.DataPack;
import org.spongepowered.api.datapack.DataPacks;
import org.spongepowered.api.world.biome.BiomeAttributes;
import org.spongepowered.api.world.generation.config.SurfaceRule;
import org.spongepowered.api.world.generation.config.noise.NoiseConfig;
import org.spongepowered.api.world.generation.config.noise.NoiseConfigs;
import org.spongepowered.api.world.generation.config.noise.NoiseGeneratorConfig;
import org.spongepowered.api.world.generation.config.noise.NoiseGeneratorConfigTemplate;
import org.spongepowered.api.world.generation.config.noise.NoiseGeneratorConfigs;
import org.spongepowered.api.world.generation.config.noise.NoiseRouter;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.AbstractDataPackEntryBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record SpongeNoiseGeneratorConfigTemplate(ResourceKey key, NoiseGeneratorSettings representedSettings, DataPack<NoiseGeneratorConfigTemplate> pack) implements NoiseGeneratorConfigTemplate {

    @Override
    public NoiseGeneratorConfig config() {
        return (NoiseGeneratorConfig) (Object) this.representedSettings;
    }

    @Override
    public int contentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        final JsonElement serialized = SpongeNoiseGeneratorConfigTemplate.encode(this, SpongeCommon.vanillaRegistryAccess());
        try {
            return DataFormats.JSON.get().read(serialized.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read deserialized NoiseGeneratorConfig:\n" + serialized, e);
        }
    }

    public static JsonElement encode(final NoiseGeneratorConfigTemplate template, final RegistryAccess registryAccess) {
        return NoiseGeneratorSettings.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, (NoiseGeneratorSettings) (Object) template.config()).getOrThrow();
    }

    public static NoiseGeneratorSettings decode(final JsonElement json, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return NoiseGeneratorSettings.DIRECT_CODEC.parse(ops, json).getOrThrow();
    }

    public static NoiseGeneratorConfigTemplate decode(final DataPack<NoiseGeneratorConfigTemplate> pack, final ResourceKey key, final JsonElement packEntry, final RegistryAccess registryAccess) {
        final NoiseGeneratorSettings parsed = SpongeNoiseGeneratorConfigTemplate.decode(packEntry, registryAccess);
        return new SpongeNoiseGeneratorConfigTemplate(key, parsed, pack);
    }


    public static final class BuilderImpl extends AbstractDataPackEntryBuilder<NoiseGeneratorConfig, NoiseGeneratorConfigTemplate, Builder> implements Builder {

        public NoiseConfig noiseConfig;
        public BlockState defaultBlock, defaultFluid;
        public int seaLevel;
        public boolean aquifers, oreVeins, legacyRandomSource, disableMobGeneration;
        public SurfaceRule surfaceRule;
        private NoiseRouter router;
        private List<BiomeAttributes> spawnTargets;

        public BuilderImpl() {
            this.reset();
        }

        @Override
        public Function<NoiseGeneratorConfigTemplate, NoiseGeneratorConfig> valueExtractor() {
            return NoiseGeneratorConfigTemplate::config;
        }

        @Override
        public Builder noiseConfig(final NoiseConfig config) {
            this.noiseConfig = Objects.requireNonNull(config, "config");
            return this;
        }

        @Override
        public Builder defaultBlock(final BlockState block) {
            this.defaultBlock = Objects.requireNonNull(block, "block");
            return this;
        }

        @Override
        public Builder defaultFluid(final BlockState fluid) {
            this.defaultFluid = Objects.requireNonNull(fluid, "fluid");
            return this;
        }

        @Override
        public Builder surfaceRule(SurfaceRule rule) {
            this.surfaceRule = rule;
            return this;
        }


        @Override
        public Builder seaLevel(final int y) {
            this.seaLevel = y;
            return this;
        }

        @Override
        public Builder mobGeneration(boolean mobGeneration) {
            this.disableMobGeneration = !mobGeneration;
            return this;
        }

        @Override
        public Builder aquifers(final boolean enableAquifers) {
            this.aquifers = enableAquifers;
            return this;
        }

        @Override
        public Builder oreVeins(final boolean enableOreVeins) {
            this.oreVeins = enableOreVeins;
            return this;
        }

        @Override
        public Builder randomSource(boolean useLegacyRandomSource) {
            this.legacyRandomSource = useLegacyRandomSource;
            return this;
        }

        @Override
        public Builder noiseRouter(final NoiseRouter router) {
            this.router = router;
            return this;
        }

        @Override
        public Builder spawnTargets(final List<BiomeAttributes> spawnTargets) {
            this.spawnTargets = spawnTargets;
            return this;
        }

        @Override
        public Builder reset() {
            this.noiseConfig = NoiseConfigs.OVERWORLD.get();
            this.defaultBlock = BlockTypes.STONE.get().defaultState();
            this.defaultFluid = BlockTypes.WATER.get().defaultState();
            this.surfaceRule = SurfaceRule.overworld();
            this.seaLevel = 63;
            this.aquifers = false;
            this.oreVeins = false;
            this.legacyRandomSource = false;
            this.router = NoiseGeneratorConfigs.OVERWORLD.get().noiseRouter();
            this.spawnTargets = (List) new OverworldBiomeBuilder().spawnTarget();;
            this.pack = DataPacks.NOISE_GENERATOR_CONFIG;
            this.key = null;
            return this;
        }

        @Override
        public Builder fromValue(final NoiseGeneratorConfig value) {
            this.noiseConfig = value.noiseConfig();
            this.defaultBlock = value.defaultBlock();
            this.defaultFluid = value.defaultFluid();
            this.surfaceRule = value.surfaceRule();
            this.seaLevel = value.seaLevel();
            this.aquifers = value.aquifers();
            this.legacyRandomSource = value.legacyRandomSource();
            return this;
        }

        @Override
        public Builder fromDataPack(final DataView datapack) throws IOException {
            final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(datapack));
            final NoiseGeneratorSettings decoded = SpongeNoiseGeneratorConfigTemplate.decode(json, SpongeCommon.vanillaRegistryAccess());
            return this.fromValue((NoiseGeneratorConfig) (Object) decoded);
        }

        @Override
        protected NoiseGeneratorConfigTemplate build0() {
            final NoiseGeneratorSettings settings = new NoiseGeneratorSettings(
                    (net.minecraft.world.level.levelgen.NoiseSettings) (Object) this.noiseConfig,
                    (net.minecraft.world.level.block.state.BlockState) this.defaultBlock,
                    (net.minecraft.world.level.block.state.BlockState) this.defaultFluid,
                    (net.minecraft.world.level.levelgen.NoiseRouter) (Object) this.router,
                    (net.minecraft.world.level.levelgen.SurfaceRules.RuleSource) this.surfaceRule,
                    (List) this.spawnTargets,
                    this.seaLevel,
                    this.disableMobGeneration,
                    this.aquifers,
                    this.oreVeins,
                    this.legacyRandomSource
            );
            return new SpongeNoiseGeneratorConfigTemplate(this.key, settings, this.pack);
        }
    }
}
