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
package org.spongepowered.common.util.transformation;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.event.EventContextKey;
import org.spongepowered.api.registry.RegistryHolder;
import org.spongepowered.api.registry.RegistryKey;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.mirror.Mirror;
import org.spongepowered.api.util.rotation.Rotation;
import org.spongepowered.api.util.rotation.Rotations;
import org.spongepowered.api.util.transformation.Transformation;
import org.spongepowered.api.world.biome.Biome;
import org.spongepowered.api.world.schematic.PaletteReference;
import org.spongepowered.api.world.schematic.PaletteType;
import org.spongepowered.api.world.volume.archetype.ArchetypeVolume;
import org.spongepowered.api.world.volume.stream.StreamOptions;
import org.spongepowered.api.world.volume.stream.VolumePositionTranslators;
import org.spongepowered.common.event.SpongeEventContextKeyBuilder;
import org.spongepowered.common.registry.SpongeBuilderProvider;
import org.spongepowered.common.registry.SpongeFactoryProvider;
import org.spongepowered.common.registry.SpongeRegistryKey;
import org.spongepowered.common.registry.SpongeRegistryType;
import org.spongepowered.common.test.UnitTestExtension;
import org.spongepowered.common.test.stub.StubGame;
import org.spongepowered.common.test.stub.StubKey;
import org.spongepowered.common.test.stub.StubModule;
import org.spongepowered.common.test.stub.block.StubBlock;
import org.spongepowered.common.test.stub.block.StubState;
import org.spongepowered.common.test.stub.registry.StubRegistryFactory;
import org.spongepowered.common.test.stub.registry.StubbedRegistry;
import org.spongepowered.common.test.stub.util.StubMirror;
import org.spongepowered.common.test.stub.util.StubRotations;
import org.spongepowered.common.test.stub.world.schematic.StubBlockStatePaletteType;
import org.spongepowered.common.test.stub.world.schematic.StubPaletteType;
import org.spongepowered.common.world.schematic.SpongePaletteReferenceFactory;
import org.spongepowered.common.world.volume.buffer.archetype.AbstractReferentArchetypeVolume;
import org.spongepowered.common.world.volume.buffer.archetype.SpongeArchetypeVolume;
import org.spongepowered.common.world.volume.stream.SpongeStreamOptionsBuilder;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.stream.IntStream;
import java.util.stream.Stream;

@Disabled
@SuppressWarnings("rawtypes")
@ExtendWith({ MockitoExtension.class, UnitTestExtension.class })
@MockitoSettings(strictness = Strictness.LENIENT)
public final class VolumeTransformationTest {

    private static final Vector3i INVALID_STUB_POSITION = Vector3i.from(
        Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

    private static final Injector guice = Guice.createInjector(new StubModule());

    @SuppressWarnings("unused")
    static class DummyInjectable {
        @Inject Game game;
    }

    @BeforeAll
    static void setup() {
        VolumeTransformationTest.guice.injectMembers(new DummyInjectable());
        final StubGame game = (StubGame) Sponge.game();

        final SpongeFactoryProvider factoryProvider = game.factoryProvider();
        final SpongeBuilderProvider builderProvider = game.builderProvider();
        // Set up Rotations
        final StubbedRegistry<Rotation> rotation = new StubbedRegistry<>(
            () -> RegistryTypes.ROTATION,
            (k) -> Rotations.NONE.get()
        );

        // Set up Blocks and BlockState
        final StubbedRegistry<BlockType> blocktypes = new StubbedRegistry<>(
            () -> RegistryTypes.BLOCK_TYPE,
            StubBlock::new
        );
        // Set up biomes
        final StubbedRegistry<Biome> biomes = new StubbedRegistry<>(
            () -> RegistryTypes.BIOME,
            (key) -> Mockito.mock(Biome.class)
        );
        // Set up palettes
        final StubbedRegistry<PaletteType<?, ?>> paletteTypeRegistry = new StubbedRegistry<>(
            () -> RegistryTypes.PALETTE_TYPE,
            (key) -> new StubPaletteType<>()
        );
        factoryProvider.registerFactory(RegistryType.Factory.class, new SpongeRegistryType.FactoryImpl());
        factoryProvider.registerFactory(RegistryKey.Factory.class, new SpongeRegistryKey.FactoryImpl());
        factoryProvider.registerFactory(PaletteReference.Factory.class, new SpongePaletteReferenceFactory());
        // and finally, set up the resourcekey stuff
        factoryProvider.registerFactory(ResourceKey.Factory.class, new StubRegistryFactory());
        game.register(rotation);
        game.register(blocktypes);
        game.register(biomes);
        game.register(paletteTypeRegistry);
        final StubbedRegistry<Mirror> mirror = new StubbedRegistry<>(
            () -> RegistryTypes.MIRROR, (k) -> Mockito.mock(Mirror.class));
        StubMirror.registerDefaults(mirror);
        game.register(mirror);

        builderProvider.register(EventContextKey.Builder.class, SpongeEventContextKeyBuilder::new);
        builderProvider.register(Transformation.Builder.class, SpongeTransformationBuilder::new);
        builderProvider.register(StreamOptions.Builder.class, SpongeStreamOptionsBuilder::new);
        StubRotations.registerDefaults(rotation);

        paletteTypeRegistry.register(new StubKey("sponge", "block_state_palette"), new StubBlockStatePaletteType());
    }

    @SuppressWarnings("unused") // IDEA bug not noticing this method is used by junit to populate parameters
    private static Stream<Arguments> testTransformationsOfPositions() {
        return Stream.of(
            Arguments.of(
                Vector3i.ZERO,
                Vector3i.from(2, 2, 2),
                Vector3i.ZERO,
                Vector3i.from(1, 1, 1),
                0,
                StubRotations.NONE
            ),
            Arguments.of(
                Vector3i.ZERO,
                Vector3i.from(2, 2, 2),
                Vector3i.UNIT_X,
                Vector3i.from(1, 1, 1),
                1,
                StubRotations.CLOCKWISE_90
            ),
            Arguments.of(
                Vector3i.ZERO,
                Vector3i.from(2, 2, 2),
                Vector3i.from(-1, 1, 4),
                Vector3i.from(1, 1, 1),
                0,
                StubRotations.NONE
            ),
            Arguments.of(
                Vector3i.ZERO,
                Vector3i.from(2, 2, 2),
                Vector3i.from(-1, 1, 4),
                Vector3i.from(1, 1, 1),
                1,
                StubRotations.CLOCKWISE_90
            ),
            Arguments.of(
                Vector3i.from(1, -1, -1),
                Vector3i.from(2, 1, 0),
                Vector3i.ZERO,
                Vector3i.from(1, -1, -1),
                1,
                StubRotations.CLOCKWISE_90
            ),
            Arguments.of(
                Vector3i.from(1, -1, -1),
                Vector3i.from(2, 1, 0),
                Vector3i.ZERO,
                Vector3i.from(1, -1, -1),
                2,
                StubRotations.COUNTERCLOCKWISE_90
            ),
            Arguments.of(
                Vector3i.from(1, -1, -1),
                Vector3i.from(2, 1, 0),
                Vector3i.ZERO,
                Vector3i.from(1, -1, -1),
                4,
                StubRotations.CLOCKWISE_180
            ),
            Arguments.of(
                Vector3i.from(-4, -1, -5),
                Vector3i.from(10, 7, 9),
                Vector3i.ZERO,
                Vector3i.from(1, -3, -1),
                8,
                StubRotations.CLOCKWISE_90
            ),
            Arguments.of(
                Vector3i.from(-4, -1, -5),
                Vector3i.from(10, 7, 9),
                Vector3i.from(-8, 3, -7),
                Vector3i.from(1, -3, -1),
                8,
                StubRotations.CLOCKWISE_90
            ),
            Arguments.of(
                Vector3i.from(-11, -4, -10),
                Vector3i.from(21, 11, 17),
                Vector3i.from(-6, 2, -4),
                Vector3i.from(1, -3, -1),
                8,
                StubRotations.CLOCKWISE_90
            )
        );
    }

    private static SpongeArchetypeVolume fillVolume(final Vector3i min, final Vector3i max, final Vector3i origin) {
        final Vector3i rawMin = min.min(max);
        final Vector3i rawMax = max.max(min);
        final Vector3i size = rawMax.sub(rawMin).add(Vector3i.ONE);
        final Vector3i relativeMin = rawMin.sub(origin);
        final RegistryHolder holder = Sponge.game();
        final SpongeArchetypeVolume volume = new SpongeArchetypeVolume(relativeMin, size, holder);

        final StubbedRegistry<BlockType> blockRegistry = (StubbedRegistry<BlockType>) RegistryTypes.BLOCK_TYPE.get();
        final Vector3i volMax = volume.max().add(Vector3i.ONE);
        IntStream.range(relativeMin.x(), volMax.x()).forEach(x -> IntStream.range(relativeMin.z(), volMax.z())
            .forEach(z -> IntStream.range(relativeMin.y(), volMax.y())
                .forEach(y -> {
                    final BlockType block = blockRegistry.createEntry(
                        "minecraft", String.format("volumetest{%d, %d, %d}", x, y, z));
                    final BlockState blockState = block.defaultState();
                    volume.setBlock(x, y, z, blockState);
                })));
        return volume;
    }

    @MethodSource("testTransformationsOfPositions")
    @ParameterizedTest
    void testTransformationsOfPositions(
        final Vector3i min, final Vector3i max, final Vector3i origin, final Vector3i testForRoundTrip,
        final int rotationCount, final StubRotations wanted
    ) {
        final SpongeArchetypeVolume volume = VolumeTransformationTest.fillVolume(min, max, origin);
        final Vector3i size = volume.size();
        final Vector3i relativeMin = volume.min();

        final Vector3d center = volume.logicalCenter();

        ArchetypeVolume intermediary = volume;
        for (int i = 0; i < rotationCount; i++) {
            intermediary = intermediary.transform(Transformation.builder()
                .origin(center)
                .rotate(wanted)
                .build());
        }
        Rotation expected = Rotations.NONE.get();
        for (int i = 0; i < rotationCount; i++) {
            expected = expected.and(wanted);
        }
        final Transformation expectedTransform = Transformation.builder()
            .origin(center)
            .rotate(expected)
            .build();
        final Transformation inverse = expectedTransform.inverse();
        final ArchetypeVolume rotated = intermediary;
        if (rotationCount > 0) {
            final Vector3d preliminaryTransformed = expectedTransform.transformPosition(testForRoundTrip.toDouble());
            Vector3i unTransformed = preliminaryTransformed.round().toInt();
            for (int i = 0; i < rotationCount; i++) {
                unTransformed = ((AbstractReferentArchetypeVolume) rotated).inverseTransform(
                    unTransformed.x(), unTransformed.y(), unTransformed.z());
            }
            Assertions.assertEquals(testForRoundTrip, unTransformed);
        }
        for (int x = 0; x < size.x(); x++) {
            for (int y = 0; y < size.y(); y++) {
                for (int z = 0; z < size.z(); z++) {
                    final int relativeX = x + relativeMin.x();
                    final int relativeY = y + relativeMin.y();
                    final int relativeZ = z + relativeMin.z();
                    final Vector3d rawRelativePosition = new Vector3d(relativeX, relativeY, relativeZ);
                    final BlockState untransformedState = volume.block(relativeX, relativeY, relativeZ);
                    final Vector3i transformedPosition = expectedTransform.transformPosition(
                        rawRelativePosition).toInt();
                    final BlockState transformedState = rotated.block(
                        transformedPosition.x(), transformedPosition.y(), transformedPosition.z());
                    Assertions.assertEquals(untransformedState, transformedState, () -> String.format(
                        "Block Check Failed!\nOriginal(%d, %d, %d): %s\nTransformed(%d, %d, %d): %s\n",
                        relativeX, relativeY, relativeZ, untransformedState,
                        transformedPosition.x(), transformedPosition.y(),
                        transformedPosition.z(), transformedState
                    ));
                }
            }
        }
        if (rotationCount < 0) {
            return;
        }
        // At this point, we should have an abstract referent volume at least

        rotated.blockStateStream(rotated.min(), rotated.max(), StreamOptions.lazily())
            .forEach((rotatedRef, type, x, y, z) -> {
                final Vector3d transformedPos = new Vector3d(x, y, z);
                // We have this offset in the stream, so we have to undo it here.
                final Vector3d invertedTransformedPos = inverse
                    .transformPosition(transformedPos.add(VolumePositionTranslators.BLOCK_OFFSET))
                    .sub(VolumePositionTranslators.BLOCK_OFFSET);
                final Vector3i invertedBlockPos = invertedTransformedPos.toInt();
                final Vector3i expectedPos;
                Assertions.assertInstanceOf(StubState.class, type,
                    () -> String.format("expected state to be a stub state for pos: [%f, %f, %f] but got %s", x, y, z,
                        type
                    )
                );
                Assertions.assertNotEquals(
                    ((StubState) type).deducedPos,
                    VolumeTransformationTest.INVALID_STUB_POSITION,
                    () -> String.format("expected to have a positioned stub state: [%f, %f, %f] but got %s", x, y, z,
                        type
                    )
                );
                expectedPos = ((StubState) type).deducedPos;
                Assertions.assertEquals(expectedPos, invertedBlockPos,
                    () -> String.format(
                        "expected untransformed position %s for state %s does not match reverse transformed position: %s",
                        expectedPos, type, invertedBlockPos
                    )
                );
                final BlockState block = volume.block(expectedPos.x(), expectedPos.y(), expectedPos.z());

                Assertions.assertEquals(type, block,
                    () -> String.format(
                        "Expected deduced state to be equal from the original target volume but had a mismatch: Original target %s does not match %s",
                        block, type
                    )
                );
            });
    }

}
