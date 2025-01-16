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
package org.spongepowered.common.registry.loader;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.commands.synchronization.brigadier.DoubleArgumentInfo;
import net.minecraft.commands.synchronization.brigadier.FloatArgumentInfo;
import net.minecraft.commands.synchronization.brigadier.IntegerArgumentInfo;
import net.minecraft.commands.synchronization.brigadier.LongArgumentInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.command.parameter.managed.ValueParameter;
import org.spongepowered.api.command.parameter.managed.clientcompletion.ClientCompletionType;
import org.spongepowered.api.command.parameter.managed.clientcompletion.ClientCompletionTypes;
import org.spongepowered.api.command.parameter.managed.operator.Operator;
import org.spongepowered.api.command.parameter.managed.operator.Operators;
import org.spongepowered.api.command.parameter.managed.standard.ResourceKeyedValueParameters;
import org.spongepowered.api.command.registrar.CommandRegistrarType;
import org.spongepowered.api.command.registrar.tree.CommandCompletionProvider;
import org.spongepowered.api.command.registrar.tree.CommandCompletionProviders;
import org.spongepowered.api.command.registrar.tree.CommandTreeNodeType;
import org.spongepowered.api.command.registrar.tree.CommandTreeNodeTypes;
import org.spongepowered.api.command.selector.SelectorSortAlgorithm;
import org.spongepowered.api.command.selector.SelectorSortAlgorithms;
import org.spongepowered.api.command.selector.SelectorType;
import org.spongepowered.api.command.selector.SelectorTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.common.accessor.commands.arguments.DimensionArgumentAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.command.brigadier.argument.ClientNativeArgumentParser;
import org.spongepowered.common.command.parameter.managed.clientcompletion.SpongeClientCompletionType;
import org.spongepowered.common.command.parameter.managed.operator.SpongeAdditionOperator;
import org.spongepowered.common.command.parameter.managed.operator.SpongeDivisionOperator;
import org.spongepowered.common.command.parameter.managed.operator.SpongeMaxOperator;
import org.spongepowered.common.command.parameter.managed.operator.SpongeMinOperator;
import org.spongepowered.common.command.parameter.managed.operator.SpongeModulusOperator;
import org.spongepowered.common.command.parameter.managed.operator.SpongeMultiplicationOperator;
import org.spongepowered.common.command.parameter.managed.operator.SpongeOperator;
import org.spongepowered.common.command.parameter.managed.operator.SpongeSubtractionOperator;
import org.spongepowered.common.command.parameter.managed.standard.SpongeBigDecimalValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.SpongeBigIntegerValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.SpongeColorValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.SpongeDataContainerValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.SpongeDateTimeValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.SpongeDurationValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.SpongeGameProfileValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.SpongeIPAddressValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.SpongeNoneValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.SpongeOperatorValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.SpongePluginContainerValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.SpongeServerLocationValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.SpongeTargetBlockValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.SpongeTargetEntityValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.SpongeUserValueParameter;
import org.spongepowered.common.command.registrar.BrigadierCommandRegistrar;
import org.spongepowered.common.command.registrar.SpongeCommandRegistrarTypes;
import org.spongepowered.common.command.registrar.SpongeParameterizedCommandRegistrar;
import org.spongepowered.common.command.registrar.SpongeRawCommandRegistrar;
import org.spongepowered.common.command.registrar.tree.key.SpongeAmountCommandTreeNodeType;
import org.spongepowered.common.command.registrar.tree.key.SpongeBasicCommandTreeNodeType;
import org.spongepowered.common.command.registrar.tree.key.SpongeEntityCommandTreeNodeType;
import org.spongepowered.common.command.registrar.tree.key.SpongeRangeCommandTreeNodeType;
import org.spongepowered.common.command.registrar.tree.key.SpongeStringCommandTreeNodeType;
import org.spongepowered.common.command.selector.SpongeSelectorSortAlgorithm;
import org.spongepowered.common.command.selector.SpongeSelectorType;
import org.spongepowered.common.item.SpongeItemStackSnapshot;
import org.spongepowered.common.registry.RegistryLoader;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector3d;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CommandRegistryLoader {

    @SuppressWarnings("ConstantConditions")
    public static RegistryLoader<ValueParameter<?>> valueParameter(final CommandBuildContext cbCtx) {
        return RegistryLoader.of(l -> {
            l.add(ResourceKeyedValueParameters.BIG_DECIMAL, SpongeBigDecimalValueParameter::new);
            l.add(ResourceKeyedValueParameters.BIG_INTEGER, SpongeBigIntegerValueParameter::new);
            l.add(ResourceKeyedValueParameters.BLOCK_STATE, k -> ClientNativeArgumentParser.createConverter(k, BlockStateArgument.block(cbCtx), (reader, cause, state) -> (BlockState) state.getState()));
            l.add(ResourceKeyedValueParameters.BOOLEAN, k -> ClientNativeArgumentParser.createIdentity(k, BoolArgumentType.bool()));
            l.add(ResourceKeyedValueParameters.COLOR, SpongeColorValueParameter::new);
            l.add(ResourceKeyedValueParameters.DATA_CONTAINER, SpongeDataContainerValueParameter::new);
            l.add(ResourceKeyedValueParameters.DATE_TIME, SpongeDateTimeValueParameter::new);
            l.add(ResourceKeyedValueParameters.DOUBLE, k -> ClientNativeArgumentParser.createIdentity(k, DoubleArgumentType.doubleArg()));
            l.add(ResourceKeyedValueParameters.DURATION, SpongeDurationValueParameter::new);
            l.add(ResourceKeyedValueParameters.ENTITY, k -> ClientNativeArgumentParser.createConverter(k, EntityArgument.entity(), (reader, cause, selector) -> (Entity) selector.findSingleEntity((CommandSourceStack) cause)));
            l.add(ResourceKeyedValueParameters.GAME_PROFILE, SpongeGameProfileValueParameter::new);
            l.add(ResourceKeyedValueParameters.INTEGER, k -> ClientNativeArgumentParser.createIdentity(k, IntegerArgumentType.integer()));
            l.add(ResourceKeyedValueParameters.IP, SpongeIPAddressValueParameter::new);
            l.add(ResourceKeyedValueParameters.ITEM_STACK_SNAPSHOT, k -> ClientNativeArgumentParser.createConverter(k, ItemArgument.item(cbCtx), (reader, cause, converter) -> new SpongeItemStackSnapshot((ItemStack) (Object) converter.createItemStack(1, true))));
            l.add(ResourceKeyedValueParameters.LOCATION, SpongeServerLocationValueParameter::new);
            l.add(ResourceKeyedValueParameters.LONG, k -> ClientNativeArgumentParser.createIdentity(k, LongArgumentType.longArg()));
            l.add(ResourceKeyedValueParameters.MANY_ENTITIES, k -> ClientNativeArgumentParser.createConverter(k, EntityArgument.entities(), (reader, cause, selector) -> selector.findEntities((CommandSourceStack) cause).stream().map(x -> (Entity) x).collect(
                    Collectors.toList())));
            l.add(ResourceKeyedValueParameters.MANY_GAME_PROFILES, k -> ClientNativeArgumentParser.createConverter(k, GameProfileArgument.gameProfile(), (reader, cause, converter) -> converter.getNames((CommandSourceStack) cause)));
            l.add(ResourceKeyedValueParameters.MANY_PLAYERS, k -> ClientNativeArgumentParser.createConverter(k, EntityArgument.players(), (reader, cause, selector) -> selector.findPlayers((CommandSourceStack) cause)));
            l.add(ResourceKeyedValueParameters.NONE, SpongeNoneValueParameter::new);
            l.add(ResourceKeyedValueParameters.OPERATOR, SpongeOperatorValueParameter::new);
            l.add(ResourceKeyedValueParameters.PLAYER, k -> ClientNativeArgumentParser.createConverter(k, EntityArgument.player(), (reader, cause, selector) -> (Player) selector.findSinglePlayer((CommandSourceStack) cause)));
            l.add(ResourceKeyedValueParameters.PLUGIN, SpongePluginContainerValueParameter::new);
            l.add(ResourceKeyedValueParameters.REMAINING_JOINED_STRINGS, k -> ClientNativeArgumentParser.createIdentity(k, StringArgumentType.greedyString()));
            l.add(ResourceKeyedValueParameters.RESOURCE_KEY, k -> ClientNativeArgumentParser.createConverter(k, ResourceLocationArgument.id(), (reader, cause, resourceLocation) -> (ResourceKey) (Object) resourceLocation));
            l.add(ResourceKeyedValueParameters.ROTATION, k -> ClientNativeArgumentParser.createConverter(k, RotationArgument.rotation(), (reader, cause, coords) -> {
                final Vec2 rotation = coords.getRotation((CommandSourceStack) cause);
                return new Vector3d(rotation.x, rotation.y, 0);
            }));
            l.add(ResourceKeyedValueParameters.STRING, k -> ClientNativeArgumentParser.createIdentity(k, StringArgumentType.string()));
            l.add(ResourceKeyedValueParameters.TARGET_BLOCK, SpongeTargetBlockValueParameter::new);
            l.add(ResourceKeyedValueParameters.TARGET_ENTITY, k -> new SpongeTargetEntityValueParameter(k, false));
            l.add(ResourceKeyedValueParameters.TARGET_PLAYER, k -> new SpongeTargetEntityValueParameter(k, true));
            l.add(ResourceKeyedValueParameters.TEXT_FORMATTING_CODE, k -> ClientNativeArgumentParser.createConverter(k, StringArgumentType.string(), (reader, cause, result) -> LegacyComponentSerializer.legacyAmpersand().deserialize(result)));
            l.add(ResourceKeyedValueParameters.TEXT_FORMATTING_CODE_ALL, k -> ClientNativeArgumentParser.createConverter(k, StringArgumentType.greedyString(), (reader, cause, result) -> LegacyComponentSerializer.legacyAmpersand().deserialize(result)));
            l.add(ResourceKeyedValueParameters.TEXT_JSON, k -> ClientNativeArgumentParser.createConverter(k, ComponentArgument.textComponent(cbCtx), (reader, cause, result) -> SpongeAdventure.asAdventure(result)));
            l.add(ResourceKeyedValueParameters.TEXT_JSON_ALL, k -> ClientNativeArgumentParser.createConverter(k, StringArgumentType.greedyString(), (reader, cause, result) -> GsonComponentSerializer.gson().deserialize(result)));
            l.add(ResourceKeyedValueParameters.URL, k -> ClientNativeArgumentParser.createConverter(k, StringArgumentType.string(),
                    (reader, cause, input) -> {
                        try {
                            return new URI(input).toURL();
                        } catch (final MalformedURLException | URISyntaxException ex) {
                            throw new SimpleCommandExceptionType(net.minecraft.network.chat.Component.literal("Could not parse " + input + " as a URL"))
                                    .createWithContext(reader);
                        }
                    })
            );
            l.add(ResourceKeyedValueParameters.USER, SpongeUserValueParameter::new);
            l.add(ResourceKeyedValueParameters.UUID, k -> ClientNativeArgumentParser.createIdentity(k, UuidArgument.uuid()));
            l.add(ResourceKeyedValueParameters.VECTOR2D, k -> ClientNativeArgumentParser.createConverter(k, Vec2Argument.vec2(),
                    (reader, cause, result) -> {
                        final net.minecraft.world.phys.Vec3 r = result.getPosition((CommandSourceStack) cause);
                        return new Vector2d(r.x, r.z);
                    })
            );
            l.add(ResourceKeyedValueParameters.VECTOR3D, k -> ClientNativeArgumentParser.createConverter(k, Vec3Argument.vec3(false),
                    (reader, cause, result) -> VecHelper.toVector3d(result.getPosition((CommandSourceStack) cause))));
            l.add(ResourceKeyedValueParameters.WORLD, k -> ClientNativeArgumentParser.createConverter(k,
                    DimensionArgument.dimension(),
                    (reader, cause, result) -> Sponge.server().worldManager().world((ResourceKey) (Object) result)
                            .orElseThrow(() -> DimensionArgumentAccessor.accessor$ERROR_INVALID_VALUE().createWithContext(reader, result))
            ));
        });
    }

    public static RegistryLoader<ClientCompletionType> clientCompletionType() {
        return RegistryLoader.of(l -> {
            // TODO vanilla has way more of those ; maybe generate based on ArgumentTypeInfos
            l.add(ClientCompletionTypes.DECIMAL_NUMBER, k -> new SpongeClientCompletionType(DoubleArgumentType.doubleArg()));
            l.add(ClientCompletionTypes.SNBT, k -> new SpongeClientCompletionType(CompoundTagArgument.compoundTag()));
            l.add(ClientCompletionTypes.NONE, k -> SpongeClientCompletionType.NONE);
            l.add(ClientCompletionTypes.RESOURCE_KEY, k -> new SpongeClientCompletionType(ResourceLocationArgument.id()));
            l.add(ClientCompletionTypes.STRING, k -> new SpongeClientCompletionType(StringArgumentType.string()));
            l.add(ClientCompletionTypes.WHOLE_NUMBER, k -> new SpongeClientCompletionType(LongArgumentType.longArg()));
        });
    }

    public static RegistryLoader<CommandCompletionProvider> clientSuggestionProvider() {
        return RegistryLoader.of(l -> {
            // TODO based on SuggestionProviders.PROVIDERS_BY_NAME instead
//            l.add(CommandCompletionProviders.ALL_RECIPES, k -> (CommandCompletionProvider) SuggestionProviders.ALL_RECIPES);
            l.add(CommandCompletionProviders.AVAILABLE_SOUNDS, k -> (CommandCompletionProvider) SuggestionProviders.AVAILABLE_SOUNDS);
            l.add(CommandCompletionProviders.SUMMONABLE_ENTITIES, k -> (CommandCompletionProvider) SuggestionProviders.SUMMONABLE_ENTITIES);
        });
    }


    public static RegistryLoader<CommandTreeNodeType<?>> clientCompletionKey(final CommandBuildContext cbCtx) {
        // TODO check ArgumentTypeInfos
        final Function<ResourceKey, ArgumentType<?>> fn = key -> argumentTypeFromKey(key, cbCtx);
        return RegistryLoader.of(l -> {
            l.add(CommandTreeNodeTypes.BOOL, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.FLOAT, k -> SpongeRangeCommandTreeNodeType.createFrom(k, new FloatArgumentInfo()));
            l.add(CommandTreeNodeTypes.DOUBLE, k -> SpongeRangeCommandTreeNodeType.createFrom(k, new DoubleArgumentInfo()));
            l.add(CommandTreeNodeTypes.INTEGER, k -> SpongeRangeCommandTreeNodeType.createFrom(k, new IntegerArgumentInfo()));
            l.add(CommandTreeNodeTypes.LONG, k -> SpongeRangeCommandTreeNodeType.createFrom(k, new LongArgumentInfo()));
            l.add(CommandTreeNodeTypes.STRING, SpongeStringCommandTreeNodeType::new);
            l.add(CommandTreeNodeTypes.ENTITY, SpongeEntityCommandTreeNodeType::new);
            l.add(CommandTreeNodeTypes.GAME_PROFILE, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.BLOCK_POS, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.COLUMN_POS, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.VEC3, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.VEC2, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.BLOCK_STATE, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.BLOCK_PREDICATE, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.ITEM_STACK, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.ITEM_PREDICATE, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.COLOR, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.COMPONENT, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.MESSAGE, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.NBT_COMPOUND_TAG, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.NBT_TAG, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.NBT_PATH, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.OBJECTIVE, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.OBJECTIVE_CRITERIA, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.OPERATION, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.PARTICLE, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.ANGLE, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.ROTATION, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.SCOREBOARD_SLOT, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.SCORE_HOLDER, k -> new SpongeAmountCommandTreeNodeType(k, ScoreHolderArgument.scoreHolder(), ScoreHolderArgument.scoreHolders()));
            l.add(CommandTreeNodeTypes.SWIZZLE, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.TEAM, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.ITEM_SLOT, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.RESOURCE_LOCATION, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.FUNCTION, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.ENTITY_ANCHOR, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.INT_RANGE, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.FLOAT_RANGE, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.DIMENSION, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            //  TODO API          l.add(CommandTreeNodeTypes.GAMEMODE, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.TIME, k -> new SpongeBasicCommandTreeNodeType(k, TimeArgument.time()));
            // TODO API           l.add(CommandTreeNodeTypes.RESOURCE_OR_TAG, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            // TODO API           l.add(CommandTreeNodeTypes.RESOURCE, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            // TODO API           l.add(CommandTreeNodeTypes.RESOURCE_KEY, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            // TODO API           l.add(CommandTreeNodeTypes.TEMPLATE_MIRROR, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            // TODO API           l.add(CommandTreeNodeTypes.TEMPLATE_ROTATION, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
            l.add(CommandTreeNodeTypes.UUID, k -> new SpongeBasicCommandTreeNodeType(k, fn.apply(k)));
        });
    }


    // Dynamic

    public static RegistryLoader<CommandRegistrarType<?>> commandRegistrarType() {
        // TODO dynamic
        return RegistryLoader.of(l -> {
            l.add(SpongeCommandRegistrarTypes.BRIGADIER, () -> BrigadierCommandRegistrar.TYPE);
            l.add(SpongeCommandRegistrarTypes.MANAGED, () -> SpongeParameterizedCommandRegistrar.TYPE);
            l.add(SpongeCommandRegistrarTypes.RAW, () -> SpongeRawCommandRegistrar.TYPE);
        });
    }

    // Helper

    static ArgumentType<?> argumentTypeFromKey(ResourceKey key, CommandBuildContext ctx) {
        final ArgumentTypeInfo<?,?> argumentTypeInfo = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getValue((ResourceLocation) (Object) key);
        if (argumentTypeInfo instanceof SingletonArgumentInfo<?> s) {
            return s.unpack(null).instantiate(ctx);
        }
        throw new IllegalArgumentException(key.asString());
    }

    public static RegistryLoader<Operator> operator() {
        return RegistryLoader.of(l -> {
            l.add(Operators.ADDITION, SpongeAdditionOperator::new);
            l.add(Operators.ASSIGN, () -> new SpongeOperator("="));
            l.add(Operators.DIVISION, SpongeDivisionOperator::new);
            l.add(Operators.MAX, SpongeMinOperator::new);
            l.add(Operators.MIN, SpongeMaxOperator::new);
            l.add(Operators.MODULUS, SpongeModulusOperator::new);
            l.add(Operators.MULTIPLICATION, SpongeMultiplicationOperator::new);
            l.add(Operators.SUBTRACTION, SpongeSubtractionOperator::new);
            l.add(Operators.SWAP, () -> new SpongeOperator("><"));
        });
    }

    public static RegistryLoader<SelectorSortAlgorithm> selectorSortAlgorithm() {
        return RegistryLoader.of(l -> {
            l.add(SelectorSortAlgorithms.ORDER_ARBITRARY, k -> new SpongeSelectorSortAlgorithm(EntitySelector.ORDER_ARBITRARY));
            l.add(SelectorSortAlgorithms.ORDER_FURTHEST, k -> new SpongeSelectorSortAlgorithm(EntitySelectorParser.ORDER_FURTHEST));
            l.add(SelectorSortAlgorithms.ORDER_NEAREST, k -> new SpongeSelectorSortAlgorithm(EntitySelectorParser.ORDER_NEAREST));
            l.add(SelectorSortAlgorithms.ORDER_RANDOM, k -> new SpongeSelectorSortAlgorithm(EntitySelectorParser.ORDER_RANDOM));
        });
    }

    public static RegistryLoader<SelectorType> selectorType() {
        return RegistryLoader.of(l -> {
            l.add(SelectorTypes.ALL_ENTITIES, k -> new SpongeSelectorType("@e"));
            l.add(SelectorTypes.ALL_PLAYERS, k -> new SpongeSelectorType("@a"));
            l.add(SelectorTypes.NEAREST_PLAYER, k -> new SpongeSelectorType("@p"));
            l.add(SelectorTypes.RANDOM_PLAYER, k -> new SpongeSelectorType("@r"));
            l.add(SelectorTypes.SOURCE, k -> new SpongeSelectorType("@s"));
        });
    }
}
