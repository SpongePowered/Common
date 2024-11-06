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
package org.spongepowered.common.registry;

import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.advancement.criteria.AdvancementCriterion;
import org.spongepowered.api.advancement.criteria.AndCriterion;
import org.spongepowered.api.advancement.criteria.OrCriterion;
import org.spongepowered.api.adventure.Audiences;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.standard.VariableValueParameters;
import org.spongepowered.api.command.registrar.tree.CommandTreeNode;
import org.spongepowered.api.command.selector.Selector;
import org.spongepowered.api.data.DataManipulator;
import org.spongepowered.api.data.type.ToolRule;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.datapack.DataPackType;
import org.spongepowered.api.effect.ForwardingViewer;
import org.spongepowered.api.effect.VanishState;
import org.spongepowered.api.event.EventListenerRegistration;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackComparators;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.recipe.crafting.RecipeInput;
import org.spongepowered.api.item.recipe.smithing.ArmorTrim;
import org.spongepowered.api.network.channel.ChannelExceptionHandler;
import org.spongepowered.api.network.status.Favicon;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.api.registry.DuplicateRegistrationException;
import org.spongepowered.api.registry.FactoryProvider;
import org.spongepowered.api.registry.RegistryKey;
import org.spongepowered.api.registry.RegistryReference;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.api.registry.TypeNotFoundException;
import org.spongepowered.api.resource.ResourcePath;
import org.spongepowered.api.resource.pack.PackStatus;
import org.spongepowered.api.resource.pack.PackType;
import org.spongepowered.api.scoreboard.ScoreFormat;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlot;
import org.spongepowered.api.service.permission.NodeTree;
import org.spongepowered.api.state.BooleanStateProperty;
import org.spongepowered.api.state.EnumStateProperty;
import org.spongepowered.api.state.IntegerStateProperty;
import org.spongepowered.api.state.StateMatcher;
import org.spongepowered.api.tag.Tag;
import org.spongepowered.api.tag.TagTemplate;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.util.MinecraftDayTime;
import org.spongepowered.api.util.Range;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.util.Transform;
import org.spongepowered.api.util.blockray.RayTrace;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.DefaultWorldKeys;
import org.spongepowered.api.world.PositionSource;
import org.spongepowered.api.world.WorldTypeEffect;
import org.spongepowered.api.world.biome.AttributedBiome;
import org.spongepowered.api.world.biome.BiomeAttributes;
import org.spongepowered.api.world.biome.ambient.ParticleConfig;
import org.spongepowered.api.world.biome.ambient.SoundConfig;
import org.spongepowered.api.world.biome.provider.BiomeProvider;
import org.spongepowered.api.world.biome.provider.MultiNoiseBiomeConfig;
import org.spongepowered.api.world.biome.spawner.NaturalSpawnCost;
import org.spongepowered.api.world.biome.spawner.NaturalSpawner;
import org.spongepowered.api.world.generation.ChunkGenerator;
import org.spongepowered.api.world.generation.config.SurfaceRule;
import org.spongepowered.api.world.generation.config.flat.LayerConfig;
import org.spongepowered.api.world.generation.structure.jigsaw.JigsawPoolElement;
import org.spongepowered.api.world.portal.PortalLogic;
import org.spongepowered.api.world.schematic.PaletteReference;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerLocationCreator;
import org.spongepowered.api.world.server.WorldTemplate;
import org.spongepowered.api.world.volume.archetype.entity.EntityArchetypeEntry;
import org.spongepowered.api.world.volume.block.BlockVolumeFactory;
import org.spongepowered.api.world.weather.Weather;
import org.spongepowered.common.advancement.SpongeAdvancementCriterionFactory;
import org.spongepowered.common.advancement.criterion.SpongeAndCriterion;
import org.spongepowered.common.advancement.criterion.SpongeOrCriterion;
import org.spongepowered.common.adventure.AudiencesFactory;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.block.BlockStatePropertyImpl;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.command.SpongeCommandCompletionFactory;
import org.spongepowered.common.command.manager.SpongeCommandCauseFactory;
import org.spongepowered.common.command.parameter.SpongeParameterFactory;
import org.spongepowered.common.command.parameter.managed.factory.SpongeVariableValueParametersFactory;
import org.spongepowered.common.command.registrar.tree.builder.SpongeCommandTreeBuilderFactory;
import org.spongepowered.common.command.result.SpongeCommandResultFactory;
import org.spongepowered.common.command.selector.SpongeSelectorFactory;
import org.spongepowered.common.data.manipulator.ImmutableDataManipulatorFactory;
import org.spongepowered.common.data.manipulator.MutableDataManipulatorFactory;
import org.spongepowered.common.data.value.SpongeValueFactory;
import org.spongepowered.common.datapack.SpongeDataPackType;
import org.spongepowered.common.effect.SpongeCustomForwardingViewer;
import org.spongepowered.common.entity.effect.SpongeVanishState;
import org.spongepowered.common.event.SpongeEventListenerRegistration;
import org.spongepowered.common.event.tracking.BlockChangeFlagManager;
import org.spongepowered.common.item.SpongeItemStack;
import org.spongepowered.common.item.SpongeItemStackSnapshot;
import org.spongepowered.common.item.SpongeToolRuleFactory;
import org.spongepowered.common.item.recipe.SpongeRecipeInputFactory;
import org.spongepowered.common.item.recipe.smithing.SpongeArmorTrimFactory;
import org.spongepowered.common.item.util.SpongeItemStackComparatorFactory;
import org.spongepowered.common.network.channel.SpongeChannelExceptionHandlerFactory;
import org.spongepowered.common.network.status.SpongeFavicon;
import org.spongepowered.common.profile.SpongeGameProfile;
import org.spongepowered.common.profile.SpongeProfilePropertyFactory;
import org.spongepowered.common.resource.SpongeResourcePath;
import org.spongepowered.common.resource.pack.SpongePackStatusFactory;
import org.spongepowered.common.resource.pack.SpongePackTypeFactory;
import org.spongepowered.common.scoreboard.SpongeDisplaySlotFactory;
import org.spongepowered.common.scoreboard.SpongeScoreFormatFactory;
import org.spongepowered.common.service.server.permission.SpongeNodeTree;
import org.spongepowered.common.state.SpongeStateMatcherFactory;
import org.spongepowered.common.tag.SpongeTagFactory;
import org.spongepowered.common.tag.SpongeTagTemplateFactory;
import org.spongepowered.common.util.SpongeAABB;
import org.spongepowered.common.util.SpongeDamageSourceFactory;
import org.spongepowered.common.util.SpongeMinecraftDayTime;
import org.spongepowered.common.util.SpongeRange;
import org.spongepowered.common.util.SpongeTicks;
import org.spongepowered.common.util.SpongeTransform;
import org.spongepowered.common.util.raytrace.SpongeRayTraceFactory;
import org.spongepowered.common.world.SpongeDefaultWorldKeysFactory;
import org.spongepowered.common.world.SpongePositionSourceFactory;
import org.spongepowered.common.world.SpongeWorldTypeEffect;
import org.spongepowered.common.world.biome.SpongeAttributedBiome;
import org.spongepowered.common.world.biome.SpongeBiomeAttributesFactory;
import org.spongepowered.common.world.biome.SpongeBiomeProviderFactory;
import org.spongepowered.common.world.biome.ambient.SpongeParticleConfigFactory;
import org.spongepowered.common.world.biome.ambient.SpongeSoundConfigFactory;
import org.spongepowered.common.world.biome.provider.SpongeMultiNoiseBiomeConfig;
import org.spongepowered.common.world.biome.spawner.SpongeNaturalSpawnerCostFactory;
import org.spongepowered.common.world.biome.spawner.SpongeNaturalSpawnerFactory;
import org.spongepowered.common.world.generation.SpongeChunkGeneratorFactory;
import org.spongepowered.common.world.generation.config.flat.SpongeLayerConfigFactory;
import org.spongepowered.common.world.generation.config.noise.SpongeSurfaceRulesFactory;
import org.spongepowered.common.world.generation.structure.jigsaw.SpongeJigsawFactory;
import org.spongepowered.common.world.portal.SpongePortalLogicFactory;
import org.spongepowered.common.world.schematic.SpongePaletteReferenceFactory;
import org.spongepowered.common.world.server.SpongeServerLocation;
import org.spongepowered.common.world.server.SpongeServerLocationCreatorFactory;
import org.spongepowered.common.world.server.SpongeWorldTemplate;
import org.spongepowered.common.world.volume.archetype.entity.SpongeEntityArchetypeEntryFactory;
import org.spongepowered.common.world.volume.block.SpongeBlockVolumeFactory;
import org.spongepowered.common.world.weather.SpongeWeather;

import java.util.Map;
import java.util.Objects;

@Singleton
@SuppressWarnings("unchecked")
public final class SpongeFactoryProvider implements FactoryProvider {

    private final Map<Class<?>, Object> factories;

    public SpongeFactoryProvider() {
        this.factories = new Object2ObjectOpenHashMap<>();
    }

    @Override
    public <T> T provide(final Class<T> clazz) throws TypeNotFoundException {
        final Object duck = this.factories.get(clazz);
        if (duck == null) {
            throw new TypeNotFoundException(String.format("Type '%s' has no factory registered!", clazz));
        }

        return (T) duck;
    }

    public <T> SpongeFactoryProvider registerFactory(Class<T> factoryClass, T factory) {
        Objects.requireNonNull(factory, "factory");

        if (this.factories.containsKey(factoryClass)) {
            throw new DuplicateRegistrationException(String.format("Type '%s' has already been registered as a factory!", factoryClass));
        }

        this.factories.put(factoryClass, factory);
        return this;
    }

    /**
     * Order matters, be careful...
     */
    public void registerDefaultFactories() {
        this
                .registerFactory(ResourceKey.Factory.class, new SpongeResourceKeyFactory())
                .registerFactory(Audiences.Factory.class, new AudiencesFactory())
                .registerFactory(ForwardingViewer.Factory.class, new SpongeCustomForwardingViewer.FactoryImpl())
                .registerFactory(AABB.Factory.class, new SpongeAABB.FactoryImpl())
                .registerFactory(AdvancementCriterion.Factory.class, new SpongeAdvancementCriterionFactory())
                .registerFactory(CommandCause.Factory.class, new SpongeCommandCauseFactory())
                .registerFactory(CommandTreeNode.NodeFactory.class, new SpongeCommandTreeBuilderFactory())
                .registerFactory(ItemStackSnapshot.Factory.class, () -> SpongeItemStackSnapshot.EMPTY)
                .registerFactory(Parameter.Value.Factory.class, new SpongeParameterFactory())
                .registerFactory(ServerLocation.Factory.class, new SpongeServerLocation.Factory())
                .registerFactory(SpongeComponents.Factory.class, new SpongeAdventure.Factory())
                .registerFactory(Transform.Factory.class, new SpongeTransform.Factory())
                .registerFactory(VariableValueParameters.Factory.class, new SpongeVariableValueParametersFactory())
                .registerFactory(ChannelExceptionHandler.Factory.class, new SpongeChannelExceptionHandlerFactory())
                .registerFactory(Selector.Factory.class, new SpongeSelectorFactory())
                .registerFactory(Range.Factory.class, new SpongeRange.FactoryImpl())
                .registerFactory(Value.Factory.class, new SpongeValueFactory())
                .registerFactory(DataManipulator.Mutable.Factory.class, new MutableDataManipulatorFactory())
                .registerFactory(DataManipulator.Immutable.Factory.class, new ImmutableDataManipulatorFactory())
                .registerFactory(BlockChangeFlag.Factory.class, new BlockChangeFlagManager.Factory())
                .registerFactory(OrCriterion.Factory.class, new SpongeOrCriterion.Factory())
                .registerFactory(AndCriterion.Factory.class, new SpongeAndCriterion.Factory())
                .registerFactory(Ticks.Factory.class, new SpongeTicks.Factory())
                .registerFactory(MinecraftDayTime.Factory.class, new SpongeMinecraftDayTime.Factory())
                .registerFactory(GameProfile.Factory.class, new SpongeGameProfile.Factory())
                .registerFactory(ProfileProperty.Factory.class, new SpongeProfilePropertyFactory())
                .registerFactory(RayTrace.Factory.class, new SpongeRayTraceFactory())
                .registerFactory(StateMatcher.Factory.class, new SpongeStateMatcherFactory())
                .registerFactory(RegistryKey.Factory.class, new SpongeRegistryKey.FactoryImpl())
                .registerFactory(RegistryType.Factory.class, new SpongeRegistryType.FactoryImpl())
                .registerFactory(RegistryReference.Factory.class, new SpongeRegistryReference.FactoryImpl())
                .registerFactory(DataPackType.Factory.class, new SpongeDataPackType.FactoryImpl())
                .registerFactory(BlockVolumeFactory.class, new SpongeBlockVolumeFactory())
                .registerFactory(DamageSource.Factory.class, new SpongeDamageSourceFactory())
                .registerFactory(PaletteReference.Factory.class, new SpongePaletteReferenceFactory())
                .registerFactory(EntityArchetypeEntry.Factory.class, new SpongeEntityArchetypeEntryFactory())
                .registerFactory(ServerLocationCreator.Factory.class, new SpongeServerLocationCreatorFactory())
                .registerFactory(AttributedBiome.Factory.class, new SpongeAttributedBiome.FactoryImpl())
                .registerFactory(MultiNoiseBiomeConfig.Factory.class, new SpongeMultiNoiseBiomeConfig.FactoryImpl())
                .registerFactory(BiomeAttributes.Factory.class, new SpongeBiomeAttributesFactory())
                .registerFactory(BiomeProvider.Factory.class, new SpongeBiomeProviderFactory())
                .registerFactory(WorldTypeEffect.Factory.class, new SpongeWorldTypeEffect.FactoryImpl())
                .registerFactory(WorldTemplate.Factory.class, new SpongeWorldTemplate.FactoryImpl())
                .registerFactory(LayerConfig.Factory.class, new SpongeLayerConfigFactory())
                .registerFactory(SurfaceRule.Factory.class, new SpongeSurfaceRulesFactory())
                .registerFactory(ChunkGenerator.Factory.class, new SpongeChunkGeneratorFactory())
                .registerFactory(ItemStackComparators.Factory.class, new SpongeItemStackComparatorFactory())
                .registerFactory(Favicon.Factory.class, new SpongeFavicon.FactoryImpl())
                .registerFactory(CommandCompletion.Factory.class, new SpongeCommandCompletionFactory())
                .registerFactory(DisplaySlot.Factory.class, new SpongeDisplaySlotFactory())
                .registerFactory(Weather.Factory.class, new SpongeWeather.FactoryImpl())
                .registerFactory(NodeTree.Factory.class, new SpongeNodeTree.FactoryImpl())
                .registerFactory(TagTemplate.Factory.class, new SpongeTagTemplateFactory())
                .registerFactory(Tag.Factory.class, new SpongeTagFactory())
                .registerFactory(EventListenerRegistration.Factory.class, new SpongeEventListenerRegistration.FactoryImpl())
                .registerFactory(CommandResult.Factory.class, new SpongeCommandResultFactory())
                .registerFactory(ItemStack.Factory.class, new SpongeItemStack.FactoryImpl())
                .registerFactory(BlockSnapshot.Factory.class, new SpongeBlockSnapshot.FactoryImpl())
                .registerFactory(PackType.Factory.class, new SpongePackTypeFactory())
                .registerFactory(PackStatus.Factory.class, new SpongePackStatusFactory())
                .registerFactory(ResourcePath.Factory.class, new SpongeResourcePath.FactoryImpl())
                .registerFactory(VanishState.Factory.class, new SpongeVanishState.SpongeVanishStateFactory())
                .registerFactory(BooleanStateProperty.Factory.class, new BlockStatePropertyImpl.BooleanFactoryImpl())
                .registerFactory(IntegerStateProperty.Factory.class, new BlockStatePropertyImpl.IntegerFactoryImpl())
                .registerFactory(EnumStateProperty.Factory.class, new BlockStatePropertyImpl.EnumFactoryImpl())
                .registerFactory(DefaultWorldKeys.Factory.class, new SpongeDefaultWorldKeysFactory())
                .registerFactory(PositionSource.Factory.class, new SpongePositionSourceFactory())
                .registerFactory(JigsawPoolElement.Factory.class, new SpongeJigsawFactory())
                .registerFactory(ParticleConfig.Factory.class, new SpongeParticleConfigFactory())
                .registerFactory(SoundConfig.Factory.class, new SpongeSoundConfigFactory())
                .registerFactory(NaturalSpawnCost.Factory.class, new SpongeNaturalSpawnerCostFactory())
                .registerFactory(NaturalSpawner.Factory.class, new SpongeNaturalSpawnerFactory())
                .registerFactory(ScoreFormat.Factory.class, new SpongeScoreFormatFactory())
                .registerFactory(ToolRule.Factory.class, new SpongeToolRuleFactory())
                .registerFactory(PortalLogic.Factory.class, new SpongePortalLogicFactory())
                .registerFactory(RecipeInput.Factory.class, new SpongeRecipeInputFactory())
                .registerFactory(ArmorTrim.Factory.class, new SpongeArmorTrimFactory())
        ;
    }
}
