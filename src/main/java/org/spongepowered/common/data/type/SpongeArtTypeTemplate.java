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
package org.spongepowered.common.data.type;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.type.ArtType;
import org.spongepowered.api.data.type.ArtTypeTemplate;
import org.spongepowered.api.datapack.DataPack;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.AbstractDataPackEntryBuilder;
import org.spongepowered.common.util.Preconditions;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public record SpongeArtTypeTemplate(ResourceKey key, PaintingVariant representedType, DataPack<ArtTypeTemplate> pack) implements ArtTypeTemplate {

    @Override
    public ArtType type() {
        return (ArtType) (Object) this.representedType;
    }

    @Override
    public int contentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        final JsonElement serialized = SpongeArtTypeTemplate.encode(this, SpongeCommon.vanillaRegistryAccess());
        try {
            return DataFormats.JSON.get().read(serialized.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read deserialized DamageType:\n" + serialized, e);
        }
    }

    public static JsonElement encode(final ArtTypeTemplate template, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return PaintingVariant.DIRECT_CODEC.encodeStart(ops, (PaintingVariant) (Object) template.type()).getOrThrow();
    }

    public static PaintingVariant decode(final JsonElement json, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return PaintingVariant.DIRECT_CODEC.parse(ops, json).getOrThrow();
    }

    public static SpongeArtTypeTemplate decode(final DataPack<ArtTypeTemplate> pack, final ResourceKey key, final JsonElement packEntry, final RegistryAccess registryAccess) {
        final var parsed = SpongeArtTypeTemplate.decode(packEntry, registryAccess);
        return new SpongeArtTypeTemplate(key, parsed, pack);
    }

    public static final class BuilderImpl extends AbstractDataPackEntryBuilder<ArtType, ArtTypeTemplate, Builder> implements Builder {

        private int width;
        private int height;
        private ResourceLocation assetId;

        @Override
        public Builder dimensions(final int width, final int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        @Override
        public Builder asset(final ResourceKey assetId) {
            this.assetId = (ResourceLocation) (Object) assetId;
            return this;
        }

        @Override
        public Builder fromValue(final ArtType value) {
            if ((Object) value instanceof PaintingVariant variant) {
                this.width = variant.width();
                this.height = variant.height();
                this.assetId = variant.assetId();
            }
            return this;
        }

        @Override
        public Builder fromDataPack(final DataView dataView) throws IOException {
            final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(dataView));
            final var damageType = SpongeArtTypeTemplate.decode(json, SpongeCommon.vanillaRegistryAccess());
            this.fromValue((ArtType) (Object) damageType);
            return this;
        }

        @Override
        public Function<ArtTypeTemplate, ArtType> valueExtractor() {
            return ArtTypeTemplate::type;
        }

        @Override
        protected ArtTypeTemplate build0() {
            Preconditions.checkArgument(this.width >= 0, "width must set");
            Preconditions.checkArgument(this.height >= 0, "height must set");
            Objects.requireNonNull(this.assetId, "assetId");
            return new SpongeArtTypeTemplate(this.key, new PaintingVariant(this.width, this.height, this.assetId, Optional.empty(), Optional.empty()), this.pack);
        }
    }
}
