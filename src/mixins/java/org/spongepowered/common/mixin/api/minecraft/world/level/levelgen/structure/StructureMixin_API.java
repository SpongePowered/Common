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
package org.spongepowered.common.mixin.api.minecraft.world.level.levelgen.structure;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.entity.EntityCategory;
import org.spongepowered.api.world.generation.feature.DecorationStep;
import org.spongepowered.api.world.generation.structure.StructureType;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3i;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@Mixin(Structure.class)
public abstract class StructureMixin_API implements org.spongepowered.api.world.generation.structure.Structure {

    // @formatter:off
    @Shadow public abstract net.minecraft.world.level.levelgen.structure.StructureType<?> shadow$type();
    @Shadow public abstract HolderSet<Biome> shadow$biomes();
    @Shadow public abstract GenerationStep.Decoration shadow$step();
    @Shadow public abstract Map<MobCategory, StructureSpawnOverride> shadow$spawnOverrides();
    // @formatter:on

    @Override
    public boolean place(final ServerWorld world, final Vector3i pos) {
        // see PlaceCommand#placeStructure
        final ServerLevel level = (ServerLevel) world;
        final ServerChunkCache chunkSource = level.getChunkSource();
        final var key = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).wrapAsHolder((Structure) (Object) this);
        final StructureStart start = ((Structure) (Object) this).generate(
            key,
            level.dimension(),
            level.registryAccess(),
            chunkSource.getGenerator(),
            chunkSource.getGenerator().getBiomeSource(),
            chunkSource.randomState(),
            level.getStructureManager(),
            level.getSeed(),
            new ChunkPos(VecHelper.toBlockPos(pos)),
            0,
            level,
            ignored -> true);

        if (!start.isValid()) {
            return false;
        }

        final BoundingBox bb = start.getBoundingBox();
        ChunkPos minPos = new ChunkPos(SectionPos.blockToSectionCoord(bb.minX()), SectionPos.blockToSectionCoord(bb.minZ()));
        ChunkPos maxPos = new ChunkPos(SectionPos.blockToSectionCoord(bb.maxX()), SectionPos.blockToSectionCoord(bb.maxZ()));
        if (ChunkPos.rangeClosed(minPos, maxPos).anyMatch(($$1x) -> !level.isLoaded($$1x.getWorldPosition()))) {
            return false;
        }
        ChunkPos.rangeClosed(minPos, maxPos).forEach((chunkPos) -> start.placeInChunk(level, level.structureManager(), chunkSource.getGenerator(), level.getRandom(),
                new BoundingBox(chunkPos.getMinBlockX(), level.getMinY(), chunkPos.getMinBlockZ(), chunkPos.getMaxBlockX(), level.getMaxY(), chunkPos.getMaxBlockZ()), chunkPos));
        return true;
    }

    @Override
    public boolean place(final ServerLocation location) {
        return this.place(location.world(), location.blockPosition());
    }

    @Override
    public StructureType type() {
        return (StructureType) this.shadow$type();
    }

    @Override
    public Collection<org.spongepowered.api.world.biome.Biome> allowedBiomes() {
        return this.shadow$biomes().stream().map(Holder::value).map(org.spongepowered.api.world.biome.Biome.class::cast).toList();
    }

    @Override
    public Map<EntityCategory, StructureNaturalSpawner> spawners() {
        return (Map) this.shadow$spawnOverrides();
    }

    @Override
    public DecorationStep decorationStep() {
        return (DecorationStep) (Object) this.shadow$step();
    }

    @Override
    public DataView toContainer() {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, SpongeCommon.server().registryAccess());
        final JsonElement serialized = this.api$codec().encodeStart(ops, (Structure) (Object) this).getOrThrow();
        try {
            return DataFormats.JSON.get().read(serialized.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read deserialized Structure:\n" + serialized, e);
        }
    }

    private <T extends Structure> Codec<T> api$codec() {
        final var type = (net.minecraft.world.level.levelgen.structure.StructureType<T>) this.shadow$type();
        return type.codec().codec();
    }
}
