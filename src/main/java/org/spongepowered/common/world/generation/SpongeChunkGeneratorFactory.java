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
package org.spongepowered.common.world.generation;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.world.biome.provider.BiomeProvider;
import org.spongepowered.api.world.generation.ChunkGenerator;
import org.spongepowered.api.world.generation.ConfigurableChunkGenerator;
import org.spongepowered.api.world.generation.config.flat.FlatGeneratorConfig;
import org.spongepowered.api.world.generation.config.noise.NoiseGeneratorConfig;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.world.server.SpongeWorldTemplate;

import java.io.IOException;
import java.util.Objects;

@SuppressWarnings("unchecked")
public final class SpongeChunkGeneratorFactory implements ChunkGenerator.Factory {

    @Override
    public ConfigurableChunkGenerator<FlatGeneratorConfig> flat(final FlatGeneratorConfig config) {
        return (ConfigurableChunkGenerator<FlatGeneratorConfig>) new FlatLevelSource((FlatLevelGeneratorSettings) config);
    }

    private ConfigurableChunkGenerator<NoiseGeneratorConfig> noiseBasedChunkGenerator(final BiomeSource biomeSource, final Holder<NoiseGeneratorSettings> noiseGeneratorSettings) {
        return (ConfigurableChunkGenerator<NoiseGeneratorConfig>) (Object) new NoiseBasedChunkGenerator(biomeSource, noiseGeneratorSettings);
    }

    @Override
    public ConfigurableChunkGenerator<NoiseGeneratorConfig> noise(final BiomeProvider provider, final NoiseGeneratorConfig config) {
        var biomeSource = (BiomeSource) Objects.requireNonNull(provider, "provider");
        var noiseGeneratorSettings = (NoiseGeneratorSettings) (Object) Objects.requireNonNull(config, "config");
        return this.noiseBasedChunkGenerator(biomeSource, Holder.direct(noiseGeneratorSettings));
    }

    @Override
    public ConfigurableChunkGenerator<NoiseGeneratorConfig> overworld() {
        var noiseGeneratorSettingsRegistry = SpongeCommon.vanillaRegistry(Registries.NOISE_SETTINGS);
        final var registry = SpongeCommon.vanillaRegistry(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        final var holder = registry.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
        final var biomeSource = MultiNoiseBiomeSource.createFromPreset(holder);
        return this.noiseBasedChunkGenerator(biomeSource, noiseGeneratorSettingsRegistry.getOrThrow(NoiseGeneratorSettings.OVERWORLD));
    }

    @Override
    public ConfigurableChunkGenerator<NoiseGeneratorConfig> theNether() {
        var noiseGeneratorSettingsRegistry = SpongeCommon.vanillaRegistry(Registries.NOISE_SETTINGS);
        final var registry = SpongeCommon.vanillaRegistry(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        final var holder = registry.getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER);
        final var biomeSource = MultiNoiseBiomeSource.createFromPreset(holder);
        return this.noiseBasedChunkGenerator(biomeSource, noiseGeneratorSettingsRegistry.getOrThrow(NoiseGeneratorSettings.NETHER));
    }

    @Override
    public ConfigurableChunkGenerator<NoiseGeneratorConfig> theEnd() {
        var biomeRegistry = SpongeCommon.vanillaRegistry(Registries.BIOME);
        var noiseGeneratorSettingsRegistry = SpongeCommon.vanillaRegistry(Registries.NOISE_SETTINGS);
        var biomeSource = TheEndBiomeSource.create(biomeRegistry);
        return this.noiseBasedChunkGenerator(biomeSource, noiseGeneratorSettingsRegistry.getOrThrow(NoiseGeneratorSettings.END));
    }

    @Override
    public ChunkGenerator fromDataPack(DataView pack) throws IOException {
        final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(pack));
        return (ChunkGenerator) SpongeWorldTemplate.decodeStem(json, SpongeCommon.vanillaRegistryAccess()).generator();
    }
}
