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
package org.spongepowered.common.world.schematic;

import com.mojang.datafixers.DataFixer;
import io.leangen.geantyref.TypeToken;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.entity.BlockEntityArchetype;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataContentUpdater;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataTranslator;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.entity.EntityArchetype;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.registry.Registry;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.biome.Biome;
import org.spongepowered.api.world.schematic.Palette;
import org.spongepowered.api.world.schematic.PaletteTypes;
import org.spongepowered.api.world.schematic.Schematic;
import org.spongepowered.api.world.volume.archetype.entity.EntityArchetypeEntry;
import org.spongepowered.api.world.volume.biome.BiomeVolume;
import org.spongepowered.api.world.volume.block.BlockVolume;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.block.BlockStateSerializerDeserializer;
import org.spongepowered.common.block.entity.SpongeBlockEntityArchetypeBuilder;
import org.spongepowered.common.data.persistence.NBTTranslator;
import org.spongepowered.common.data.persistence.schematic.SchematicUpdater1_to_2;
import org.spongepowered.common.data.persistence.schematic.SchematicUpdater2_to_3;
import org.spongepowered.common.entity.SpongeEntityArchetypeBuilder;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.world.volume.VolumeStreamUtils;
import org.spongepowered.common.world.volume.buffer.archetype.SpongeArchetypeVolume;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SchematicTranslator implements DataTranslator<Schematic> {

    private static final SchematicTranslator INSTANCE = new SchematicTranslator();
    private static final TypeToken<Schematic> TYPE_TOKEN = TypeToken.get(Schematic.class);

    private static final ConcurrentSkipListSet<String> MISSING_MOD_IDS = new ConcurrentSkipListSet<>();

    private static final DataContentUpdater V1_TO_2 = new SchematicUpdater1_to_2();
    private static final DataContentUpdater V2_TO_3 = new SchematicUpdater2_to_3();

    @Nullable private static DataFixer VANILLA_FIXER;

    public static SchematicTranslator get() {
        return SchematicTranslator.INSTANCE;
    }

    private SchematicTranslator() {

    }

    @Override
    public TypeToken<Schematic> token() {
        return SchematicTranslator.TYPE_TOKEN;
    }

    @Override
    public Schematic translate(final DataView unprocessed) throws InvalidDataException {
        if (SchematicTranslator.VANILLA_FIXER == null) {
            SchematicTranslator.VANILLA_FIXER = SpongeCommon.server().getFixerUpper();
        }
        final DataView schematicView = unprocessed.getView(Constants.Sponge.Schematic.SCHEMATIC)
            .orElse(unprocessed);
        final int version = schematicView.getInt(Constants.Sponge.Schematic.VERSION).get();

        if (version > Constants.Sponge.Schematic.CURRENT_VERSION) {
            throw new InvalidDataException(
                String.format("Unknown schematic version %d (current version is %d)", version,
                    Constants.Sponge.Schematic.CURRENT_VERSION
                ));
        } else if (version == 1) {
            SchematicTranslator.V2_TO_3.update(SchematicTranslator.V1_TO_2.update(schematicView));
        } else if (version == 2) {
            SchematicTranslator.V2_TO_3.update(schematicView);
        }
        final int dataVersion = schematicView.getInt(Constants.Sponge.Schematic.DATA_VERSION).get();
        // DataFixer will be able to upgrade entity and tile entity data if and only if we're running a valid server and
        // the data version is outdated.
        final boolean needsFixers = dataVersion < SharedConstants.getCurrentVersion().getDataVersion().getVersion() && SchematicTranslator.VANILLA_FIXER != null;


        final DataView updatedView;
        if (needsFixers) {
            final CompoundTag compound = NBTTranslator.INSTANCE.translate(schematicView);
            final CompoundTag updated = DataFixTypes.CHUNK.updateToCurrentVersion(SchematicTranslator.VANILLA_FIXER, compound, dataVersion);
            updatedView = NBTTranslator.INSTANCE.translate(updated);
        } else {
            updatedView = schematicView;
        }

        final SpongeSchematicBuilder builder = new SpongeSchematicBuilder();
        final Optional<DataView> metadataView = updatedView.getView(Constants.Sponge.Schematic.METADATA);
        metadataView.ifPresent(metadata -> {
            metadata.getView(DataQuery.of(".")).ifPresent(data -> {
                for (final DataQuery key : data.keys(false)) {
                    if (!metadata.contains(key)) {
                        metadata.set(key, data.get(key).get());
                    }
                }
            });
            final String schematicName = metadata.getString(Constants.Sponge.Schematic.NAME).orElse("unknown");
            metadata.getStringList(Constants.Sponge.Schematic.REQUIRED_MODS).ifPresent(mods -> {
                for (final String modId : mods) {
                    if (!Sponge.pluginManager().plugin(modId).isPresent()) {
                        if (SchematicTranslator.MISSING_MOD_IDS.add(modId)) {
                            SpongeCommon.logger().warn(
                                "When attempting to load the Schematic: {} there is a missing modid {} some blocks/tiles/entities may not load correctly.",
                                schematicName, modId
                            );
                        }
                    }
                }
            });
            final DataContainer meta = DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED);
            for (final DataQuery key : metadata.keys(false)) {
                meta.set(key, metadata.get(key).get());
            }
            builder.metadata(meta);
        });

        final int width = updatedView.getShort(Constants.Sponge.Schematic.WIDTH)
            .orElseThrow(() -> new InvalidDataException("Missing value for: " + Constants.Sponge.Schematic.WIDTH));
        final int height = updatedView.getShort(Constants.Sponge.Schematic.HEIGHT)
            .orElseThrow(() -> new InvalidDataException("Missing value for: " + Constants.Sponge.Schematic.HEIGHT));
        final int length = updatedView.getShort(Constants.Sponge.Schematic.LENGTH)
            .orElseThrow(() -> new InvalidDataException("Missing value for: " + Constants.Sponge.Schematic.LENGTH));
        if (width <= 0
            || height <= 0
            || length <= 0) {
            throw new InvalidDataException(String.format(
                "Schematic is larger than maximum allowable size (found: (%d, %d, %d) max: (%d, %<d, %<d)",
                width, height, length, Constants.Sponge.Schematic.MAX_SIZE
            ));
        }

        final int[] offsetArray = (int[]) updatedView.get(Constants.Sponge.Schematic.OFFSET).orElse(new int[3]);
        if (offsetArray.length != 3) {
            throw new InvalidDataException("Schematic offset was not of length 3");
        }

        final Vector3i offset = new Vector3i(offsetArray[0], offsetArray[1], offsetArray[2]);
        final SpongeArchetypeVolume archetypeVolume = new SpongeArchetypeVolume(
            offset, new Vector3i(width, height, length),
            Sponge.server()
        );

        updatedView.getView(Constants.Sponge.Schematic.BLOCK_CONTAINER)
            .ifPresent(
                blocks -> SchematicTranslator.deserializeBlockContainer(blocks, archetypeVolume, width, length,
                    offset, needsFixers
                ));

        updatedView.getView(Constants.Sponge.Schematic.BIOME_CONTAINER)
            .ifPresent(
                biomes -> SchematicTranslator.deserializeBiomeContainer(biomes, archetypeVolume, width, length,
                    offset
                ));



        updatedView.getViewList(Constants.Sponge.Schematic.ENTITIES)
            .map(List::stream)
            .orElse(Stream.of())
            .filter(entity -> entity.contains(
                Constants.Sponge.Schematic.ENTITIES_POS,
                Constants.Sponge.Schematic.ENTITIES_ID
            ))
            .map(SchematicTranslator.deserializeEntityArchetype())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(archetypeVolume::addEntity);

        builder.volume(archetypeVolume);
        return builder.build();
    }

    private static Function<DataView, Optional<EntityArchetypeEntry>> deserializeEntityArchetype() {
        return view -> {
            final String typeId = view.getString(Constants.Sponge.Schematic.ENTITIES_ID).get();
            final ResourceKey key = ResourceKey.resolve(typeId);
            final Optional<EntityType<@NonNull ?>> entityType = Sponge.game().registry(
                RegistryTypes.ENTITY_TYPE).findValue(key);
            return entityType.map(type -> {
                final List<Double> pos = view.getDoubleList(Constants.Sponge.Schematic.ENTITIES_POS)
                    .orElseThrow(() -> new IllegalStateException(
                        "Schematic not abiding by format, all Entities must have an x y z pos"));
                final EntityArchetype.Builder builder = SpongeEntityArchetypeBuilder.pooled()
                    .type(type);
                view.getView(Constants.Sponge.Schematic.BLOCKENTITY_DATA).ifPresent(builder::entityData);
                final Vector3d entityPosition = new Vector3d(pos.get(0), pos.get(1), pos.get(2));
                return EntityArchetypeEntry.of(builder.build(), entityPosition);
            });
        };
    }

    @NonNull
    private static Consumer<DataView> deserializeBlockEntities(
        final Vector3i offset, final SpongeArchetypeVolume archetypeVolume,
        final boolean needsFixers
    ) {
        return blockEntityData -> {
            final int[] pos = (int[]) blockEntityData.get(Constants.Sponge.Schematic.BLOCKENTITY_POS)
                .orElseThrow(() -> new IllegalStateException(
                    "Schematic not abiding by format, all BlockEntities must have an x y z pos"));
            blockEntityData.getString(Constants.Sponge.Schematic.BLOCKENTITY_ID)
                .map(ResourceKey::resolve)
                .map(key -> Sponge.game().registry(RegistryTypes.BLOCK_ENTITY_TYPE).findValue(key))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .ifPresent(type -> {
                    final int x = pos[0] + offset.x();
                    final int y = pos[1] + offset.y();
                    final int z = pos[2] + offset.z();
                    final BlockEntityArchetype.Builder builder = SpongeBlockEntityArchetypeBuilder.pooled()
                        .state(archetypeVolume.block(x, y, z))
                        .blockEntity(type);
                    blockEntityData.getView(Constants.Sponge.Schematic.BLOCKENTITY_DATA)
                        .ifPresent(builder::blockEntityData);

                    archetypeVolume.addBlockEntity(x, y, z, builder.build());
                });
        };
    }

    private static void deserializeBlockContainer(
        final DataView view,
        final SpongeArchetypeVolume archetypeVolume,
        final int width,
        final int length,
        final Vector3i offset,
        final boolean needsFixers
    ) {
        final MutableBimapPalette<BlockState, BlockType> palette;
        final DataView paletteMap = view.getView(Constants.Sponge.Schematic.BLOCK_PALETTE)
            .orElseThrow(() -> new InvalidDataException("Missing BlockPalette as required by Schematic Specification"));
        final Set<DataQuery> paletteKeys = paletteMap.keys(false);
        // If we had a default palette_max we don't want to allocate all
        // that space for nothing so we use a sensible default instead
        palette = new MutableBimapPalette<>(
            PaletteTypes.BLOCK_STATE_PALETTE.get(),
            Sponge.game().registry(RegistryTypes.BLOCK_TYPE),
            RegistryTypes.BLOCK_TYPE,
            paletteKeys.size()
        );
        for (final DataQuery key : paletteKeys) {
            final BlockState state = BlockStateSerializerDeserializer.deserialize(key.parts().get(0))
                .orElseGet(() -> BlockTypes.BEDROCK.get().defaultState());
            palette.assign(state, paletteMap.getInt(key)
                .orElseThrow(() -> new IllegalStateException("Somehow got a missing biome in the palette map for schematic"))
            );
        }

        final byte[] blockData = (byte[]) view.get(Constants.Sponge.Schematic.BLOCK_DATA)
            .orElseThrow(() -> new InvalidDataException("Missing BlockData for Schematic"));
        SchematicTranslator.readByteArrayData(
            width, (width * length), offset, new CachingPalette.MutableImpl<>(palette), blockData, archetypeVolume,
            BlockVolume.Modifiable::setBlock
        );
        view.getViewList(Constants.Sponge.Schematic.BLOCKENTITY_CONTAINER)
            .ifPresent(tileData ->
                tileData.forEach(
                    SchematicTranslator.deserializeBlockEntities(offset, archetypeVolume, needsFixers))
            );
    }

    private static void deserializeBiomeContainer(
        final DataView view,
        final SpongeArchetypeVolume archetypeVolume,
        final int width,
        final int length,
        final Vector3i offset
    ) {
        final MutableBimapPalette<Biome, Biome> biomePalette;
        final DataView biomeMap = view.getView(Constants.Sponge.Schematic.BIOME_PALETTE)
            .orElseThrow(() -> new InvalidDataException("Missing BiomePalette as required by the schematic spec"));

        final Set<DataQuery> biomeKeys = biomeMap.keys(false);
        final Registry<Biome> biomeRegistry = VolumeStreamUtils.nativeToSpongeRegistry(SpongeCommon.vanillaRegistry(Registries.BIOME));
        biomePalette = new MutableBimapPalette<>(
            PaletteTypes.BIOME_PALETTE.get(),
            biomeRegistry,
            RegistryTypes.BIOME,
            biomeKeys.size()
        );

        for (final DataQuery biomeKey : biomeKeys) {
            final ResourceKey key = ResourceKey.resolve(biomeKey.parts().get(0));
            final Biome biome = biomeRegistry.findValue(key).get();
            biomePalette.assign(biome, biomeMap.getInt(biomeKey).get());
        }
        final byte[] biomeData = (byte[]) view.get(Constants.Sponge.Schematic.BIOME_DATA)
            .orElseThrow(() -> new InvalidDataException("Missing BlockData for Schematic"));
        SchematicTranslator.readByteArrayData(
            width, (width * length), offset, biomePalette, biomeData, archetypeVolume,
            BiomeVolume.Modifiable::setBiome
        );
    }


    static interface PostSetter<V, T> {

        void apply(V volume, int x, int y, int z, T type);
    }

    private static <Buffer, Type, ParentType> void readByteArrayData(
        final int width,
        final int i1,
        final Vector3i offset,
        final Palette<Type, ParentType> palette,
        final byte[] data,
        final Buffer buffer,
        final PostSetter<Buffer, Type> setter
    ) {
        int index = 0;
        int i = 0;
        int value = 0;
        int varint_length = 0;
        while (i < data.length) {
            value = 0;
            varint_length = 0;

            while (true) {
                value |= (data[i] & 127) << (varint_length++ * 7);
                if (varint_length > 5) {
                    throw new RuntimeException("VarInt too big (probably corrupted data)");
                }
                if ((data[i] & 128) != 128) {
                    i++;
                    break;
                }
                i++;
            }
            // index = (y * length + z) * width + x
            final int y = index / i1;
            final int z = (index % i1) / width;
            final int x = (index % i1) % width;
            final Type state = palette.get(value, Sponge.game()).get();
            setter.apply(buffer, x + offset.x(), y + offset.y(), z + offset.z(), state);

            index++;
        }
    }

    @Override
    public DataContainer translate(final Schematic schematic) throws InvalidDataException {
        final DataContainer data = DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED);
        final DataView view = data.createView(Constants.Sponge.Schematic.SCHEMATIC);
        this.addTo(schematic, view);
        return data;
    }

    @Override
    public DataView addTo(final Schematic schematic, final DataView data) {
        final int xMin = schematic.min().x();
        final int yMin = schematic.min().y();
        final int zMin = schematic.min().z();
        final int width = schematic.size().x();
        final int height = schematic.size().y();
        final int length = schematic.size().z();
        if (width > Constants.Sponge.Schematic.MAX_SIZE || height > Constants.Sponge.Schematic.MAX_SIZE || length > Constants.Sponge.Schematic.MAX_SIZE) {
            throw new IllegalArgumentException(String.format(
                "Schematic is larger than maximum allowable size (found: (%d, %d, %d) max: (%d, %<d, %<d)", width,
                height, length, Constants.Sponge.Schematic.MAX_SIZE
            ));
        }
        data.set(Constants.Sponge.Schematic.WIDTH, (short) width);
        data.set(Constants.Sponge.Schematic.HEIGHT, (short) height);
        data.set(Constants.Sponge.Schematic.LENGTH, (short) length);

        data.set(Constants.Sponge.Schematic.VERSION, Constants.Sponge.Schematic.CURRENT_VERSION);
        data.set(Constants.Sponge.Schematic.DATA_VERSION, SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        for (final DataQuery metaKey : schematic.metadata().keys(false)) {
            data.set(Constants.Sponge.Schematic.METADATA.then(metaKey), schematic.metadata().get(metaKey).get());
        }
        final Set<String> requiredMods = new HashSet<>();

        final int[] offset = new int[]{xMin, yMin, zMin};
        data.set(Constants.Sponge.Schematic.OFFSET, offset);

        // Check if we have blocks to store
        if (schematic.blockPalette().highestId() != 0) {
            final DataView blockData = data.createView(Constants.Sponge.Schematic.BLOCK_CONTAINER);
            final Palette.Mutable<BlockState, BlockType> palette = schematic.blockPalette().asMutable(
                Sponge.server());
            try (final ByteArrayOutputStream buffer = new ByteArrayOutputStream(width * height * length)) {
                for (int y = 0; y < height; y++) {
                    final int y0 = yMin + y;
                    for (int z = 0; z < length; z++) {
                        final int z0 = zMin + z;
                        for (int x = 0; x < width; x++) {
                            final int x0 = xMin + x;
                            final BlockState state = schematic.block(x0, y0, z0);
                            SchematicTranslator.writeIdToBuffer(buffer, palette.orAssign(state));
                        }
                    }
                }

                blockData.set(Constants.Sponge.Schematic.BLOCK_DATA, buffer.toByteArray());
            } catch (final IOException e) {
                // should never reach here
            }


            final Registry<BlockType> blockRegistry = VolumeStreamUtils.nativeToSpongeRegistry(
                    SpongeCommon.vanillaRegistry(Registries.BLOCK));

            SchematicTranslator.writePaletteToView(
                blockData, palette, blockRegistry, Constants.Sponge.Schematic.BLOCK_PALETTE, BlockState::type,
                requiredMods
            );

            final List<DataView> blockEntities = schematic.blockEntityArchetypes().entrySet().stream().map(entry -> {
                final DataContainer container = DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED);
                final Vector3i pos = entry.getKey();
                final BlockEntityArchetype archetype = entry.getValue();
                final DataContainer entityData = archetype.blockEntityData();
                final int[] apos = new int[]{pos.x() - xMin, pos.y() - yMin, pos.z() - zMin};
                container.set(Constants.Sponge.Schematic.BLOCKENTITY_POS, apos);
                container.set(Constants.Sponge.Schematic.BLOCKENTITY_DATA, entityData);
                final ResourceKey key = archetype.blockEntityType().key(RegistryTypes.BLOCK_ENTITY_TYPE);
                container.set(Constants.Sponge.Schematic.ENTITIES_ID, key.asString());
                final String namespace = key.namespace();
                if (!ResourceKey.MINECRAFT_NAMESPACE.equals(namespace)) {
                    requiredMods.add(namespace);
                }
                return container;
            }).collect(Collectors.toList());

            blockData.set(Constants.Sponge.Schematic.BLOCKENTITY_CONTAINER, blockEntities);

        }

        if (schematic.biomePalette().highestId() != 0) {
            final DataView biomeContainer = data.createView(Constants.Sponge.Schematic.BIOME_CONTAINER);
            final Palette.Mutable<Biome, Biome> biomePalette = schematic.biomePalette().asMutable(
                Sponge.game());

            try (final ByteArrayOutputStream buffer = new ByteArrayOutputStream(width * height * length)) {
                for (int y = 0; y < height; y++) {
                    final int y0 = yMin + y;
                    for (int z = 0; z < length; z++) {
                        final int z0 = zMin + z;
                        for (int x = 0; x < width; x++) {
                            final int x0 = xMin + x;
                            final Biome state = schematic.biome(x0, y0, z0);
                            SchematicTranslator.writeIdToBuffer(buffer, biomePalette.orAssign(state));
                        }

                    }
                }

                biomeContainer.set(Constants.Sponge.Schematic.BIOME_DATA, buffer.toByteArray());
            } catch (final IOException e) {
                // Should never reach here.
            }

            final Registry<Biome> biomeRegistry = VolumeStreamUtils.nativeToSpongeRegistry(SpongeCommon.vanillaRegistry(Registries.BIOME));

            SchematicTranslator.writePaletteToView(
                biomeContainer, biomePalette, biomeRegistry, Constants.Sponge.Schematic.BIOME_PALETTE,
                Function.identity(), requiredMods
            );
        }

        final List<DataView> entities = schematic.entityArchetypesByPosition().stream().map(entry -> {
            final DataContainer container = DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED);

            final List<Double> entityPosition = new ArrayList<>();
            entityPosition.add(entry.position().x());
            entityPosition.add(entry.position().y());
            entityPosition.add(entry.position().z());
            container.set(Constants.Sponge.Schematic.ENTITIES_POS, entityPosition);
            final ResourceKey key = entry.archetype().type().key(RegistryTypes.ENTITY_TYPE);
            if (!ResourceKey.MINECRAFT_NAMESPACE.equals(key.namespace())) {
                requiredMods.add(key.namespace());
            }
            container.set(Constants.Sponge.Schematic.ENTITIES_ID, key.toString());
            final DataContainer entityData = entry.archetype().entityData();
            container.set(Constants.Sponge.Schematic.BLOCKENTITY_DATA, entityData);
            return container;
        }).collect(Collectors.toList());

        data.set(Constants.Sponge.Schematic.ENTITIES, entities);

        if (!requiredMods.isEmpty()) {
            data.set(Constants.Sponge.Schematic.METADATA.then(Constants.Sponge.Schematic.REQUIRED_MODS), requiredMods);
        }

        return data;
    }

    private static <T, P> void writePaletteToView(
        final DataView view,
        final Palette.Mutable<T, P> palette,
        final Registry<P> parentRegistryType,
        final DataQuery paletteQuery,
        final Function<T, P> parentGetter,
        final Set<String> requiredMods
    ) {
        palette.streamWithIds().forEach(entry -> {
            // getOrAssign to skip the optional, it will never assign
            final String stringified = palette.type().stringifier().apply(
                parentRegistryType,
                entry.getKey()
            );
            view.set(paletteQuery.then(stringified), entry.getValue());
            final ResourceKey blockKey = parentRegistryType
                .findValueKey(parentGetter.apply(entry.getKey()))
                .orElseThrow(() -> new IllegalStateException(
                    "Somehow have a BlockState that is not registered in the global BlockType registry"));
            if (!ResourceKey.MINECRAFT_NAMESPACE.equals(blockKey.namespace())) {
                requiredMods.add(blockKey.namespace());
            }
        });
    }

    public static void writeIdToBuffer(final ByteArrayOutputStream buffer, final int orAssign) {
        int id = orAssign;

        while ((id & -128) != 0) {
            buffer.write(id & 127 | 128);
            id >>>= 7;
        }
        buffer.write(id);
    }

}
