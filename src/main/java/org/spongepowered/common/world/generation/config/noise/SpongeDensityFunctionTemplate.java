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
import net.minecraft.world.level.levelgen.DensityFunction;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.datapack.DataPack;
import org.spongepowered.api.datapack.DataPacks;
import org.spongepowered.api.world.generation.config.noise.DensityFunctionTemplate;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.AbstractDataPackEntryBuilder;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

public record SpongeDensityFunctionTemplate(ResourceKey key, DensityFunction representedFunction, DataPack<DensityFunctionTemplate> pack) implements DensityFunctionTemplate {

    @Override
    public org.spongepowered.api.world.generation.config.noise.DensityFunction densityFunction() {
        return (org.spongepowered.api.world.generation.config.noise.DensityFunction) this.representedFunction;
    }

    @Override
    public int contentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        final JsonElement serialized = SpongeDensityFunctionTemplate.encode(this, SpongeCommon.vanillaRegistryAccess());
        try {
            return DataFormats.JSON.get().read(serialized.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read deserialized Density Function:\n" + serialized, e);
        }
    }

    public static JsonElement encode(final DensityFunctionTemplate template, final RegistryAccess registryAccess) {
        return DensityFunction.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, (DensityFunction) template.densityFunction()).getOrThrow();
    }

    public static DensityFunction decode(final JsonElement json, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return DensityFunction.DIRECT_CODEC.parse(ops, json).getOrThrow();
    }

    public static SpongeDensityFunctionTemplate decode(final DataPack<DensityFunctionTemplate> pack, final ResourceKey key, final JsonElement packEntry, final RegistryAccess registryAccess) {
        final DensityFunction parsed = SpongeDensityFunctionTemplate.decode(packEntry, registryAccess);
        return new SpongeDensityFunctionTemplate(key, parsed, pack);
    }


    public static final class BuilderImpl extends AbstractDataPackEntryBuilder<org.spongepowered.api.world.generation.config.noise.DensityFunction, DensityFunctionTemplate, Builder> implements Builder {

        @Nullable private DensityFunction densityFunction;

        public BuilderImpl() {
            this.reset();
        }

        @Override
        public Function<DensityFunctionTemplate, org.spongepowered.api.world.generation.config.noise.DensityFunction> valueExtractor() {
            return DensityFunctionTemplate::densityFunction;
        }

        @Override
        public Builder fromValue(final org.spongepowered.api.world.generation.config.noise.DensityFunction densityFunction) {
            this.densityFunction = (DensityFunction) densityFunction;
            return this;
        }

        @Override
        public Builder fromDataPack(final DataView pack) throws IOException {
            final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(pack));
            this.densityFunction = SpongeDensityFunctionTemplate.decode(json, SpongeCommon.vanillaRegistryAccess());
            return this;
        }

        @Override
        public Builder reset() {
            this.pack = DataPacks.DENSITY_FUNCTION;
            this.key = null;
            this.densityFunction = null;
            return this;
        }

        @Override
        protected SpongeDensityFunctionTemplate build0() {
            Objects.requireNonNull(this.densityFunction, "densityFunction");
            return new SpongeDensityFunctionTemplate(this.key, this.densityFunction, this.pack);
        }
    }
}
