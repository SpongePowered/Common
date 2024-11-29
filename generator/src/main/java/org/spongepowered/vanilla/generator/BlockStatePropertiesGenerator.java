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
package org.spongepowered.vanilla.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.world.level.block.state.properties.WallSide;
import org.tinylog.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.lang.model.element.Modifier;

/**
 * Generates catalog classes for {@link BlockState} properties.
 */
public class BlockStatePropertiesGenerator implements Generator {

    enum PropertyType {
        ENUM(
            EnumProperty.class,
            "EnumStateProperties",
            BlockStatePropertiesGenerator.inStatePkg("EnumStateProperty")
        ),
        INTEGER(
            IntegerProperty.class,
            "IntegerStateProperties",
            BlockStatePropertiesGenerator.inStatePkg("IntegerStateProperty")
        ),
        BOOLEAN(
            BooleanProperty.class,
            "BooleanStateProperties",
            BlockStatePropertiesGenerator.inStatePkg("BooleanStateProperty")
        );

        private static final Map<Class<?>, PropertyType> knownProperties = new HashMap<>();

        private final Class<?> propertyType;
        private final String catalogClassName; // class, in CatalogedBy
        private final ClassName valueType; // X in DefaultedRegistryReference<X>

        PropertyType(final Class<?> propertyType, final String catalogClassName, final ClassName valueType) {
            this.propertyType = propertyType;
            this.catalogClassName = catalogClassName;
            this.valueType = valueType;
        }

        static PropertyType ofProperty(final Property<?> prop) {
           final PropertyType value = PropertyType.knownProperties.get(prop.getClass());
           if (value == null) {
               for (final PropertyType type : PropertyType.knownProperties.values()) {
                   if (type.propertyType.isInstance(prop)) {
                       PropertyType.knownProperties.put(prop.getClass(), type);
                       return type;
                   }
               }
           }

           return value;
        }

        static {
            for (final PropertyType type : PropertyType.values()) {
                PropertyType.knownProperties.put(type.propertyType, type);
            }
        }
    }

    static Map<Class<?>, TypeName> vanillaEnumTypeMapping = new HashMap<>();
    static {
        vanillaEnumTypeMapping.put(StructureMode.class, BlockStatePropertiesGenerator.inDataTypePkg("StructureMode"));
        vanillaEnumTypeMapping.put(PistonType.class, BlockStatePropertiesGenerator.inDataTypePkg("PistonType"));
        vanillaEnumTypeMapping.put(BambooLeaves.class, BlockStatePropertiesGenerator.inDataTypePkg("BambooLeavesType"));
        vanillaEnumTypeMapping.put(WallSide.class, BlockStatePropertiesGenerator.inDataTypePkg("WallConnectionState"));
        vanillaEnumTypeMapping.put(RailShape.class, BlockStatePropertiesGenerator.inDataTypePkg("RailDirection"));
        vanillaEnumTypeMapping.put(AttachFace.class, BlockStatePropertiesGenerator.inDataTypePkg("AttachmentSurface"));
        vanillaEnumTypeMapping.put(Tilt.class, BlockStatePropertiesGenerator.inDataTypePkg("Tilt"));
        vanillaEnumTypeMapping.put(RedstoneSide.class, BlockStatePropertiesGenerator.inDataTypePkg("WireAttachmentType"));
        vanillaEnumTypeMapping.put(ChestType.class, BlockStatePropertiesGenerator.inDataTypePkg("ChestAttachmentType"));
        vanillaEnumTypeMapping.put(SlabType.class, BlockStatePropertiesGenerator.inDataTypePkg("SlabPortion"));
        vanillaEnumTypeMapping.put(BellAttachType.class, BlockStatePropertiesGenerator.inDataTypePkg("BellAttachmentType"));
        vanillaEnumTypeMapping.put(SculkSensorPhase.class, BlockStatePropertiesGenerator.inDataTypePkg("SculkSensorState"));
        vanillaEnumTypeMapping.put(DoorHingeSide.class, BlockStatePropertiesGenerator.inDataTypePkg("DoorHinge"));
        vanillaEnumTypeMapping.put(NoteBlockInstrument.class, BlockStatePropertiesGenerator.inDataTypePkg("InstrumentType"));
        vanillaEnumTypeMapping.put(StairsShape.class, BlockStatePropertiesGenerator.inDataTypePkg("StairShape"));
        vanillaEnumTypeMapping.put(DripstoneThickness.class, BlockStatePropertiesGenerator.inDataTypePkg("DripstoneSegment"));
        vanillaEnumTypeMapping.put(FrontAndTop.class, BlockStatePropertiesGenerator.inDataTypePkg("JigsawBlockOrientation"));
        vanillaEnumTypeMapping.put(ComparatorMode.class, BlockStatePropertiesGenerator.inDataTypePkg("ComparatorMode"));
        vanillaEnumTypeMapping.put(TrialSpawnerState.class, BlockStatePropertiesGenerator.inDataTypePkg("TrialSpawnerState"));
        vanillaEnumTypeMapping.put(VaultState.class, BlockStatePropertiesGenerator.inDataTypePkg("VaultState"));

        // Custom Mapping required see StateHolderMixin_API
        final ClassName portionTypeClass = BlockStatePropertiesGenerator.inDataTypePkg("PortionType");
        vanillaEnumTypeMapping.put(Half.class, portionTypeClass);
        vanillaEnumTypeMapping.put(BedPart.class, portionTypeClass);
        vanillaEnumTypeMapping.put(DoubleBlockHalf.class, portionTypeClass);
        vanillaEnumTypeMapping.put(Direction.Axis.class, ClassName.get("org.spongepowered.api.util", "Axis"));
        vanillaEnumTypeMapping.put(Direction.class, ClassName.get("org.spongepowered.api.util", "Direction"));

    }


    @Override
    public String name() {
        return "block state properties";
    }

    @Override
    public void generate(final Context ctx) throws IOException {
        for (final var typeToProperty : this.computeUsedProperties().entrySet()) {
            this.writeCatalogClass(ctx, typeToProperty.getKey(), typeToProperty.getValue());
        }
    }

    private Map<Property<?>, String> vanillaProperties() {
        final Map<Property<?>, String> vanillaMap = new IdentityHashMap<>();
        for (Field field : BlockStateProperties.class.getDeclaredFields()) {
            try {
                final Object property = field.get(null);
                if (property instanceof Property<?>) {
                    vanillaMap.put(((Property<?>) property), field.getName());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return vanillaMap;
    }

    private Map<PropertyType, Map<String, Property<?>>> computeUsedProperties() {
        // get properties used in block states
        final Map<Property<?>, String> vanillaMap = this.vanillaProperties();
        final Map<PropertyType, Map<String, Property<?>>> propertyUsages = new HashMap<>();
        final Map<String, Integer> propertyCount = new HashMap<>();

        for (final Block block : BuiltInRegistries.BLOCK) {
            for (final Property<?> property : block.defaultBlockState().getProperties()) {
                final var type = PropertyType.ofProperty(property);
                if (type == null) {
                    Logger.warn("Unknown property type for state property {} in block {}", property, BuiltInRegistries.BLOCK.getKey(block));
                    continue;
                }

                String name = vanillaMap.computeIfAbsent(property, p -> {
                    // attempt to handle non-vanilla properties:
                    int cnt = propertyCount.computeIfAbsent(property.getName(), n -> 0) + 1;
                    return property.getName() + "_" + cnt;
                });
                propertyUsages.computeIfAbsent(type, $ -> new TreeMap<>()).put(name, property);
            }
        }
        return propertyUsages;
    }

    private void writeCatalogClass(final Context ctx, final PropertyType type, final Map<String, Property<?>> properties) throws IOException {
        final var clazz = Types.utilityClass(
            type.catalogClassName,
            Generator.GENERATED_FILE_JAVADOCS
        );
        clazz.addAnnotation(Types.suppressWarnings("unused"));

        properties.forEach((name, property) -> clazz.addMethod(this.makeMethod(type.valueType, name, property)));

        ctx.write("state", clazz.build());

        final var cu = ctx.compilationUnit("state", type.catalogClassName);
    }

    private MethodSpec makeMethod(final ClassName className, final String name, final Property<?> property) {
        TypeName returnTypeName = className;
        if (property instanceof EnumProperty) {
            final TypeName mappedType = vanillaEnumTypeMapping.getOrDefault(property.getValueClass(), Types.WILDCARD);
            returnTypeName = ParameterizedTypeName.get(className, mappedType);
        }

        return MethodSpec.methodBuilder("property_" + name).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(returnTypeName)
                .addCode("return $T.of($S);", className, name)
                .build();
    }

    private static ClassName inDataTypePkg(final String name) {
        return ClassName.get("org.spongepowered.api.data.type", name);
    }

    private static ClassName inStatePkg(final String name) {
        return ClassName.get("org.spongepowered.api.state", name);
    }
}
