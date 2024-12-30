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

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.api.registry.RegistryHolder;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.common.registry.loader.CommandRegistryLoader;
import org.spongepowered.common.registry.loader.DynamicSpongeRegistryLoader;
import org.spongepowered.common.registry.loader.SpongeCommonRegistryLoader;
import org.spongepowered.common.registry.loader.SpongeRegistryLoader;
import org.spongepowered.common.registry.loader.VanillaRegistryLoader;

public final class SpongeRegistries {

    // During Bootstrap
    public static void registerEarlyGlobalRegistries(final SpongeRegistryHolder holder) {
        // Vanilla
        VanillaRegistryLoader.load(holder);

        // Internal
        holder.createFrozenRegistry(SpongeRegistryTypes.TRACKER_TRANSACTION_TYPE, SpongeCommonRegistryLoader.blockTransactionTypes());
        holder.createFrozenRegistry(SpongeRegistryTypes.VALIDATION_TYPE, SpongeCommonRegistryLoader.validationType());

        // Commands
        holder.createFrozenRegistry(RegistryTypes.CLIENT_COMPLETION_TYPE, CommandRegistryLoader.clientCompletionType());
        holder.createFrozenRegistry(RegistryTypes.COMMAND_COMPLETION_PROVIDER, CommandRegistryLoader.clientSuggestionProvider());
        holder.createFrozenRegistry(RegistryTypes.OPERATOR, CommandRegistryLoader.operator());
        holder.createFrozenRegistry(RegistryTypes.SELECTOR_TYPE, CommandRegistryLoader.selectorType());
        holder.createFrozenRegistry(RegistryTypes.SELECTOR_SORT_ALGORITHM, CommandRegistryLoader.selectorSortAlgorithm());

        // other
        holder.createFrozenRegistry(RegistryTypes.ACCOUNT_DELETION_RESULT_TYPE, SpongeRegistryLoader.accountDeletionResultType());
        holder.createFrozenRegistry(RegistryTypes.BAN_TYPE, SpongeRegistryLoader.banType());
        holder.createFrozenRegistry(RegistryTypes.BODY_PART, SpongeRegistryLoader.bodyPart());
        holder.createFrozenRegistry(RegistryTypes.CLICK_TYPE, SpongeRegistryLoader.clickType());
        holder.createFrozenRegistry(RegistryTypes.CHUNK_REGENERATE_FLAG, SpongeRegistryLoader.chunkRegenerateFlag());
        holder.createFrozenRegistry(RegistryTypes.DAMAGE_STEP_TYPE, SpongeRegistryLoader.damageStepType());
        holder.createFrozenRegistry(RegistryTypes.DISMOUNT_TYPE, SpongeRegistryLoader.dismountType());
        holder.createFrozenRegistry(RegistryTypes.GOAL_EXECUTOR_TYPE, SpongeRegistryLoader.goalExecutorType());
        holder.createFrozenRegistry(RegistryTypes.GOAL_TYPE, SpongeRegistryLoader.goalType());
        holder.createFrozenRegistry(RegistryTypes.MATTER_TYPE, SpongeRegistryLoader.matterType());
        holder.createFrozenRegistry(RegistryTypes.MOVEMENT_TYPE, SpongeRegistryLoader.movementType());
        holder.createFrozenRegistry(RegistryTypes.NOTE_PITCH, SpongeRegistryLoader.notePitch());
        holder.createFrozenRegistry(RegistryTypes.OPERATION, SpongeRegistryLoader.operation());
        holder.createFrozenRegistry(RegistryTypes.ORIENTATION, SpongeRegistryLoader.orientation());
        holder.createFrozenRegistry(RegistryTypes.PALETTE_TYPE, SpongeRegistryLoader.paletteType());
        holder.createFrozenRegistry(RegistryTypes.PARTICLE_OPTION, SpongeRegistryLoader.particleOption());
        holder.createFrozenRegistry(RegistryTypes.QUERY_TYPE, SpongeRegistryLoader.queryType());
        holder.createFrozenRegistry(RegistryTypes.RESOLVE_OPERATION, SpongeRegistryLoader.resolveOperation());
        holder.createFrozenRegistry(RegistryTypes.SKIN_PART, SpongeRegistryLoader.skinPart());
        holder.createFrozenRegistry(RegistryTypes.SPAWN_TYPE, SpongeRegistryLoader.spawnType());
        holder.createFrozenRegistry(RegistryTypes.TRANSACTION_TYPE, SpongeRegistryLoader.transactionType());
        holder.createFrozenRegistry(RegistryTypes.WEATHER_TYPE, SpongeRegistryLoader.weather());
        holder.createFrozenRegistry(RegistryTypes.DATA_FORMAT, SpongeRegistryLoader.dataFormat());
        holder.createFrozenRegistry(RegistryTypes.MAP_COLOR_TYPE, SpongeRegistryLoader.mapColorType());
        holder.createFrozenRegistry(RegistryTypes.MAP_DECORATION_ORIENTATION, SpongeRegistryLoader.mapDecorationOrientation());
        holder.createFrozenRegistry(RegistryTypes.MAP_SHADE, SpongeRegistryLoader.mapShade());
        holder.createFrozenRegistry(RegistryTypes.NOISE_CONFIG, SpongeRegistryLoader.noiseConfig());

        SpongeRegistries.registerEarlyDynamicRegistries(holder);
    }

    private static void registerEarlyDynamicRegistries(final SpongeRegistryHolder holder) {
        holder.createRegistry(RegistryTypes.CURRENCY, (RegistryLoader<Currency>) null, true);
        holder.createRegistry(RegistryTypes.COMMAND_REGISTRAR_TYPE, CommandRegistryLoader.commandRegistrarType(), true);
        holder.createRegistry(RegistryTypes.PLACEHOLDER_PARSER, DynamicSpongeRegistryLoader.placeholderParser(), true);
        holder.createRegistry(RegistryTypes.TELEPORT_HELPER_FILTER, DynamicSpongeRegistryLoader.teleportHelperFilter(), true);
    }


    public static void registerGlobalRegistriesDimensionLayer(final SpongeRegistryHolder holder, final RegistryAccess.Frozen registryAccess, final FeatureFlagSet featureFlags) {
        if (holder.findRegistry(RegistryTypes.COMMAND_TREE_NODE_TYPE).isPresent()) {
            return; // Already done
        }
        final RegistryAccess.ImmutableRegistryAccess builtInRegistryAccess = new RegistryAccess.ImmutableRegistryAccess(BuiltInRegistries.REGISTRY.stream().toList());
        final CommandBuildContext cbCtx = CommandBuildContext.simple(builtInRegistryAccess, featureFlags);
        holder.createOrReplaceFrozenRegistry(RegistryTypes.COMMAND_TREE_NODE_TYPE, CommandRegistryLoader.clientCompletionKey(cbCtx));
        holder.createOrReplaceFrozenRegistry(RegistryTypes.REGISTRY_KEYED_VALUE_PARAMETER, CommandRegistryLoader.valueParameter(cbCtx));

        holder.createOrReplaceFrozenRegistry(RegistryTypes.FLAT_GENERATOR_CONFIG, SpongeRegistryLoader.flatGeneratorConfig(registryAccess));
    }

    public static void registerServerRegistries(final RegistryHolder holder) {
    }

}
