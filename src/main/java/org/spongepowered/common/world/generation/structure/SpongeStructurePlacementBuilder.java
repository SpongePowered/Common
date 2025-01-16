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
package org.spongepowered.common.world.generation.structure;

import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement.ExclusionZone;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement.FrequencyReductionMethod;
import org.spongepowered.api.tag.Tag;
import org.spongepowered.api.world.generation.structure.StructurePlacement;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3i;

import java.util.Objects;
import java.util.Optional;

public class SpongeStructurePlacementBuilder implements StructurePlacement.Builder, StructurePlacement.Builder.RandomSpread, StructurePlacement.Builder.ConcentricRings {

    private Boolean randomSpread;

    private Vec3i locateOffset = Vec3i.ZERO;
    private int salt = 0;

    private Integer spacing;
    private Integer separation;
    private RandomSpreadType randomSpreadType = RandomSpreadType.LINEAR;

    private Integer distance;
    private Integer spread;
    private Integer count;
    private HolderSet<Biome> preferredBiomes = HolderSet.direct();

    @Override
    public StructurePlacement.Builder reset() {
        this.randomSpread = null;
        this.locateOffset = Vec3i.ZERO;
        this.spacing = null;
        this.separation = null;
        this.salt = 0;
        this.randomSpreadType = RandomSpreadType.LINEAR;
        this.distance = null;
        this.spread = null;
        this.count = null;
        this.preferredBiomes = HolderSet.direct();
        return this;
    }

    // RandomSpread

    @Override
    public RandomSpread randomSpread(final Vector3i locateOffset, final int salt) {
        this.randomSpread = true;
        this.salt = salt;
        this.locateOffset = VecHelper.toVanillaVector3i(locateOffset);
        return this;
    }

    @Override
    public RandomSpread randomSpread(final int salt) {
        this.randomSpread = true;
        this.salt = salt;
        return this;
    }

    @Override
    public RandomSpread linear() {
        this.randomSpreadType = RandomSpreadType.LINEAR;
        return this;
    }

    @Override
    public RandomSpread triangular() {
        this.randomSpreadType = RandomSpreadType.TRIANGULAR;
        return this;
    }

    @Override
    public RandomSpread spacing(final int spacing) {
        this.spacing = spacing;
        return this;
    }

    @Override
    public RandomSpread separation(final int separation) {
        this.separation = separation;
        return this;
    }

    // ConcentricRings

    @Override
    public ConcentricRings concentricRings() {
        this.randomSpread = false;
        this.salt = 0;
        return this;
    }

    @Override
    public ConcentricRings concentricRings(final Vector3i locateOffset, final int salt) {
        this.randomSpread = false;
        this.salt = salt;
        this.locateOffset = VecHelper.toVanillaVector3i(locateOffset);
        return this;
    }

    @Override
    public ConcentricRings distance(final int distance) {
        this.distance = distance;
        return this;
    }

    @Override
    public ConcentricRings spread(final int spread) {
        this.spread = spread;
        return this;
    }

    @Override
    public ConcentricRings count(final int count) {
        this.count = count;
        return this;
    }

    @Override
    public ConcentricRings preferredBiomes(final Tag<org.spongepowered.api.world.biome.Biome> preferredBiomes) {
        final Registry<Biome> registry = SpongeCommon.vanillaRegistry(Registries.BIOME);
        // TODO - Snapshot 24w34a
//        this.preferredBiomes = registry.getOrCreateTag((TagKey<Biome>) (Object) preferredBiomes);
        return this;
    }

    // Building

    @SuppressWarnings("deprecation")
    @Override
    public StructurePlacement build() {
        final FrequencyReductionMethod method = FrequencyReductionMethod.DEFAULT; // TODO expose other FrequencyReductionMethods?
        final float frequency = 1.0f; // TODO expose frequency?
        final Optional<ExclusionZone> exclusionZone = Optional.empty(); // TODO expose ExclusionZone?

        if (this.randomSpread) {
            Objects.requireNonNull(this.spacing, "spacing");
            Objects.requireNonNull(this.separation, "spacing");
            return (StructurePlacement) new RandomSpreadStructurePlacement(this.locateOffset, method, frequency, this.salt,
                    exclusionZone, this.spacing, this.separation, this.randomSpreadType);
        } else {
            Objects.requireNonNull(this.distance, "distance");
            Objects.requireNonNull(this.spread, "spread");
            Objects.requireNonNull(this.count, "count");
            return (StructurePlacement) new ConcentricRingsStructurePlacement(this.locateOffset, method, frequency, this.salt,
                    exclusionZone, this.distance, this.spread, this.count, this.preferredBiomes);
        }
    }
}
