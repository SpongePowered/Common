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
package org.spongepowered.common.mixin.api.minecraft.world.level.levelgen.feature;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.world.generation.feature.Feature;
import org.spongepowered.api.world.generation.feature.FeatureType;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3i;

import java.io.IOException;

@Mixin(ConfiguredFeature.class)
public abstract class ConfiguredFeatureMixin_API<
        F extends net.minecraft.world.level.levelgen.feature.Feature<FC>,
        FC extends FeatureConfiguration,
        APIF extends FeatureType>
        implements Feature {

    // @formatter:off
    @Shadow @Final private F feature;
    @Shadow @Final private FC config;
    @Shadow public abstract boolean shadow$place(final WorldGenLevel $$0, final ChunkGenerator $$1, final RandomSource $$2, final BlockPos $$3);
    // @formatter:on

    @Override
    public APIF type() {
        return (APIF) this.feature;
    }

    @Override
    public DataView toContainer() {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, SpongeCommon.vanillaRegistryAccess());
        final JsonElement serialized = this.feature.configuredCodec().codec().encodeStart(ops, (ConfiguredFeature<FC, net.minecraft.world.level.levelgen.feature.Feature<FC>>) (Object) this).getOrThrow();
        try {
            return DataFormats.JSON.get().read(serialized.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read deserialized Feature: " + serialized, e);
        }
    }

    @Override
    public boolean place(ServerWorld world, Vector3i pos) {
        return this.shadow$place(((WorldGenLevel) world), (ChunkGenerator) world.generator(), ((WorldGenLevel) world).getRandom(), VecHelper.toBlockPos(pos));
    }

    @Override
    public boolean place(final ServerLocation location) {
        return this.place(location.world(), location.blockPosition());
    }
}
