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
package org.spongepowered.common.world.generation.carver;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.datapack.DataPack;
import org.spongepowered.api.datapack.DataPacks;
import org.spongepowered.api.world.generation.carver.Carver;
import org.spongepowered.api.world.generation.carver.CarverTemplate;
import org.spongepowered.api.world.generation.carver.CarverType;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.AbstractDataPackEntryBuilder;

import java.io.IOException;
import java.util.function.Function;

public record SpongeCarverTemplate(ResourceKey key, ConfiguredWorldCarver<?> representedCarver, DataPack<CarverTemplate> pack) implements CarverTemplate {

    @Override
    public int contentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        final JsonElement serialized = SpongeCarverTemplate.encode(this, SpongeCommon.vanillaRegistryAccess());
        try {
            return DataFormats.JSON.get().read(serialized.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read deserialized Carver:\n" + serialized, e);
        }
    }

    @Override
    public Carver carver() {
        return (Carver) (Object) this.representedCarver;
    }

    public static JsonElement encode(final Codec<ConfiguredWorldCarver<?>> codec, final ConfiguredWorldCarver<?> carver, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return codec.encodeStart(ops, carver).getOrThrow();
    }

    public static JsonElement encode(final CarverTemplate template, final RegistryAccess registryAccess) {
        return SpongeCarverTemplate.encode(ConfiguredWorldCarver.DIRECT_CODEC, (ConfiguredWorldCarver<?>) (Object) template.carver(), registryAccess);
    }

    public static ConfiguredWorldCarver<?> decode(final Codec<ConfiguredWorldCarver<?>> codec, final JsonElement json, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return codec.parse(ops, json).getOrThrow();
    }

    public static CarverTemplate decode(final DataPack<CarverTemplate> pack, final ResourceKey key, final JsonElement packEntry, final RegistryAccess registryAccess) {
        final ConfiguredWorldCarver<?> parsed = SpongeCarverTemplate.decode(ConfiguredWorldCarver.DIRECT_CODEC, packEntry, registryAccess);
        return new SpongeCarverTemplate(key, parsed, pack);
    }

    public static class BuilderImpl extends AbstractDataPackEntryBuilder<Carver, CarverTemplate, Builder> implements Builder {

        @Nullable private WorldCarver<?> type;
        @Nullable private CarverConfiguration config;

        @Override
        public Function<CarverTemplate, Carver> valueExtractor() {
            return CarverTemplate::carver;
        }

        @Override
        public Builder reset() {
            this.type = null;
            this.config = null;
            this.key = null;
            this.pack = DataPacks.CARVER;
            return this;
        }

        @Override
        public Builder type(final CarverType type) {
            this.type = (WorldCarver<?>) type;
            return this;
        }

        @Override
        public Builder fromValue(final Carver carver) {
            this.type(carver.type());
            this.config = ((ConfiguredWorldCarver<?>) (Object) carver).config();
            return this;
        }

        @Override
        public Builder fromDataPack(final DataView pack) throws IOException {
            final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(pack));
            final ConfiguredWorldCarver<?> carver = SpongeCarverTemplate.decode(ConfiguredWorldCarver.DIRECT_CODEC, json, SpongeCommon.vanillaRegistryAccess());
            this.fromValue((Carver) (Object) carver);
            return this;
        }

        @Override
        protected CarverTemplate build0() {
            ConfiguredWorldCarver<?> carver = new ConfiguredWorldCarver<>((WorldCarver<? super CarverConfiguration>) this.type, this.config);
            return new SpongeCarverTemplate(this.key, carver, this.pack);
        }
    }
}
