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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
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
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

/**
 * Generates catalog classes for {@link BlockState} properties.
 */
public class BlockStatePropertyKeysGenerator implements Generator {



    enum PropertyType {
        ENUM(
                EnumProperty.class,
                p -> vanillaEnumTypeMapping.getOrDefault(p.getValueClass(), Types.WILDCARD)
        ),
        INTEGER(
                IntegerProperty.class,
                p -> ClassName.get(Integer.class)
        ),
        BOOLEAN(
                BooleanProperty.class,
                p -> ClassName.get(Boolean.class)
        );

        private static final Map<Class<?>, BlockStatePropertyKeysGenerator.PropertyType> knownProperties = new HashMap<>();

        private final Class<?> propertyType;
        private final Function<Property<?>, TypeName> valueTypeFunction; // X in Key<Value<X>>

        PropertyType(final Class<?> propertyType, final Function<Property<?>, TypeName> valueTypeFunction) {
            this.propertyType = propertyType;
            this.valueTypeFunction = valueTypeFunction;
        }

        static BlockStatePropertyKeysGenerator.PropertyType ofProperty(final Property<?> prop) {
            final BlockStatePropertyKeysGenerator.PropertyType value = BlockStatePropertyKeysGenerator.PropertyType.knownProperties.get(prop.getClass());
            if (value == null) {
                for (final BlockStatePropertyKeysGenerator.PropertyType type : BlockStatePropertyKeysGenerator.PropertyType.knownProperties.values()) {
                    if (type.propertyType.isInstance(prop)) {
                        BlockStatePropertyKeysGenerator.PropertyType.knownProperties.put(prop.getClass(), type);
                        return type;
                    }
                }
            }

            return value;
        }

        static {
            for (final BlockStatePropertyKeysGenerator.PropertyType type : BlockStatePropertyKeysGenerator.PropertyType.values()) {
                BlockStatePropertyKeysGenerator.PropertyType.knownProperties.put(type.propertyType, type);
            }
        }
    }

    static Map<Class<?>, TypeName> vanillaEnumTypeMapping = new HashMap<>();
    static {
        vanillaEnumTypeMapping.put(StructureMode.class, BlockStatePropertyKeysGenerator.inDataTypePkg("StructureMode"));
        vanillaEnumTypeMapping.put(PistonType.class, BlockStatePropertyKeysGenerator.inDataTypePkg("PistonType"));
        vanillaEnumTypeMapping.put(BambooLeaves.class, BlockStatePropertyKeysGenerator.inDataTypePkg("BambooLeavesType"));
        vanillaEnumTypeMapping.put(WallSide.class, BlockStatePropertyKeysGenerator.inDataTypePkg("WallConnectionState"));
        vanillaEnumTypeMapping.put(RailShape.class, BlockStatePropertyKeysGenerator.inDataTypePkg("RailDirection"));
        vanillaEnumTypeMapping.put(AttachFace.class, BlockStatePropertyKeysGenerator.inDataTypePkg("AttachmentSurface"));
        vanillaEnumTypeMapping.put(Tilt.class, BlockStatePropertyKeysGenerator.inDataTypePkg("Tilt"));
        vanillaEnumTypeMapping.put(RedstoneSide.class, BlockStatePropertyKeysGenerator.inDataTypePkg("WireAttachmentType"));
        vanillaEnumTypeMapping.put(ChestType.class, BlockStatePropertyKeysGenerator.inDataTypePkg("ChestAttachmentType"));
        vanillaEnumTypeMapping.put(SlabType.class, BlockStatePropertyKeysGenerator.inDataTypePkg("SlabPortion"));
        vanillaEnumTypeMapping.put(BellAttachType.class, BlockStatePropertyKeysGenerator.inDataTypePkg("BellAttachmentType"));
        vanillaEnumTypeMapping.put(SculkSensorPhase.class, BlockStatePropertyKeysGenerator.inDataTypePkg("SculkSensorState"));
        vanillaEnumTypeMapping.put(DoorHingeSide.class, BlockStatePropertyKeysGenerator.inDataTypePkg("DoorHinge"));
        vanillaEnumTypeMapping.put(NoteBlockInstrument.class, BlockStatePropertyKeysGenerator.inDataTypePkg("InstrumentType"));
        vanillaEnumTypeMapping.put(StairsShape.class, BlockStatePropertyKeysGenerator.inDataTypePkg("StairShape"));
        vanillaEnumTypeMapping.put(DripstoneThickness.class, BlockStatePropertyKeysGenerator.inDataTypePkg("DripstoneSegment"));
        vanillaEnumTypeMapping.put(FrontAndTop.class, BlockStatePropertyKeysGenerator.inDataTypePkg("JigsawBlockOrientation"));
        vanillaEnumTypeMapping.put(ComparatorMode.class, BlockStatePropertyKeysGenerator.inDataTypePkg("ComparatorMode"));
        vanillaEnumTypeMapping.put(TrialSpawnerState.class, BlockStatePropertyKeysGenerator.inDataTypePkg("TrialSpawnerState"));
        vanillaEnumTypeMapping.put(VaultState.class, BlockStatePropertyKeysGenerator.inDataTypePkg("VaultState"));

        // Custom Mapping required see StateHolderMixin_API
        final ClassName portionTypeClass = BlockStatePropertyKeysGenerator.inDataTypePkg("PortionType");
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
        final var clazz = Types.utilityClass("BlockStateKeys", Generator.GENERATED_FILE_JAVADOCS);
        clazz.addAnnotation(Types.suppressWarnings("unused"));

        final MethodSpec factoryMethod = this.factoryMethod();
        for (final var propertyEntry : this.vanillaProperties().entrySet()) {
            final var type = BlockStatePropertyKeysGenerator.PropertyType.ofProperty(propertyEntry.getValue());
            if (type == null) {
                Logger.warn("Unknown property type for state property {}", propertyEntry.getValue());
                continue;
            }
            clazz.addField(this.makeKeyField(type, propertyEntry.getKey(), propertyEntry.getValue(), factoryMethod));
        }

        clazz.addMethod(factoryMethod);

        ctx.write("data", clazz.build());

        final var cu = ctx.compilationUnit("data", "BlockStateKeys");
    }

    private FieldSpec makeKeyField(final PropertyType propertyType, final String name, final Property<?> property, final MethodSpec factoryMethod) {
        final TypeName valueType = propertyType.valueTypeFunction.apply(property);
        final ParameterizedTypeName keyType = ParameterizedTypeName.get(inDataPkg("Key"), ParameterizedTypeName.get(inDataValuePkg("Value"), valueType));
        return FieldSpec.builder(keyType, name, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L.$N($L, $T.class)", "BlockStateKeys",
                        factoryMethod,
                        Types.resourceKey(Types.NAMESPACE_MINECRAFT, "property/" + property.getName()),
                        valueType)
                .build();
    }

    private MethodSpec factoryMethod() {
        final ParameterSpec rKeyParam = ParameterSpec.builder(Types.RESOURCE_KEY, "resourceKey", Modifier.FINAL).build();
        final TypeVariableName genericType = TypeVariableName.get("T");
        final ParameterSpec typeParam = ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Class.class), genericType), "type", Modifier.FINAL).build();
        return MethodSpec.methodBuilder("key").addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(inDataPkg("Key"), ParameterizedTypeName.get(inDataValuePkg("Value"), genericType)))
                .addParameter(rKeyParam).addParameter(typeParam)
                .addCode("return $T.builder().key($N).elementType($N).build();", inDataPkg("Key"), rKeyParam, typeParam)
                .addTypeVariable(genericType)
                .build();
    }

    private static ClassName inDataTypePkg(final String name) {
        return ClassName.get("org.spongepowered.api.data.type", name);
    }

    private static ClassName inDataPkg(final String name) {
        return ClassName.get("org.spongepowered.api.data", name);
    }

    private static ClassName inDataValuePkg(final String name) {
        return ClassName.get("org.spongepowered.api.data.value", name);
    }

    // Utility to get Vanilla Data:

    private Map<String, Property<?>> vanillaProperties() {
        final Map<String, Property<?>> vanillaMap = new TreeMap<>();
        for (Field field : BlockStateProperties.class.getDeclaredFields()) {
            try {
                final Object property = field.get(null);
                if (property instanceof Property<?>) {
                    vanillaMap.put(field.getName(), ((Property<?>) property));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return vanillaMap;
    }

}
