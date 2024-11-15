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
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.datapack.DataPack;
import org.spongepowered.api.datapack.DataPacks;
import org.spongepowered.api.world.generation.config.noise.Noise;
import org.spongepowered.api.world.generation.config.noise.NoiseTemplate;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.AbstractDataPackEntryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.DoubleStream;

public record SpongeNoiseTemplate(ResourceKey key, NormalNoise.NoiseParameters noiseParameters, DataPack<NoiseTemplate> pack) implements NoiseTemplate {

    @Override
    public Noise noise() {
        return (Noise) (Object) this.noiseParameters;
    }

    @Override
    public int contentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        final JsonElement serialized = SpongeNoiseTemplate.encode(this, SpongeCommon.vanillaRegistryAccess());
        try {
            return DataFormats.JSON.get().read(serialized.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read deserialized Noise:\n" + serialized, e);
        }
    }

    public static JsonElement encode(final NoiseTemplate template, final RegistryAccess registryAccess) {
        return NormalNoise.NoiseParameters.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, (NormalNoise.NoiseParameters) (Object) template.noise()).getOrThrow();
    }

    public static NormalNoise.NoiseParameters decode(final JsonElement json, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return NormalNoise.NoiseParameters.DIRECT_CODEC.parse(ops, json).getOrThrow();
    }

    public static SpongeNoiseTemplate decode(final DataPack<NoiseTemplate> pack, final ResourceKey key, final JsonElement packEntry, final RegistryAccess registryAccess) {
        final NormalNoise.NoiseParameters parsed = SpongeNoiseTemplate.decode(packEntry, registryAccess);
        return new SpongeNoiseTemplate(key, parsed, pack);
    }


    public static final class BuilderImpl extends AbstractDataPackEntryBuilder<Noise, NoiseTemplate, Builder> implements Builder {

        @Nullable private Integer octave;
        @Nullable private List<Double> amplitudes;

        public BuilderImpl() {
            this.reset();
        }

        @Override
        public Builder fromValue(final Noise noise) {
            this.octave(noise.octave()).amplitudes(noise.amplitudes());
            return this;
        }

        @Override
        public Builder octave(final int octave) {
            this.octave = octave;
            return this;
        }

        @Override
        public Builder amplitudes(final double... amplitudes) {
            this.amplitudes = DoubleStream.of(amplitudes).boxed().toList();
            return this;
        }

        @Override
        public Builder amplitudes(final List<Double> amplitudes) {
            this.amplitudes = amplitudes;
            return this;
        }

        @Override
        public Builder reset() {
            this.octave = null;
            this.amplitudes = null;
            this.pack = DataPacks.NOISE;
            this.key = null;
            return this;
        }

        @Override
        public Builder fromDataPack(final DataView datapack) throws IOException {
            final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(datapack));
            return this.fromValue((Noise) (Object) SpongeNoiseTemplate.decode(json, SpongeCommon.vanillaRegistryAccess()));
        }

        @Override
        public Function<NoiseTemplate, Noise> valueExtractor() {
            return NoiseTemplate::noise;
        }

        @Override
        protected SpongeNoiseTemplate build0() {
            Objects.requireNonNull(this.octave, "octave");
            Objects.requireNonNull(this.amplitudes, "amplitudes");
            final NormalNoise.NoiseParameters params = new NormalNoise.NoiseParameters(this.octave, new ArrayList<>(this.amplitudes));
            return new SpongeNoiseTemplate(this.key, params, this.pack);
        }
    }
}
