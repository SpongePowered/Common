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

import org.spongepowered.api.registry.RegistryTypes;

public final class SpongeRegistries {

    public static void registerGlobalRegistries(final SpongeRegistryHolder holder) {
        VanillaRegistryLoader.load(holder);

        holder.createRegistry(RegistryTypes.ACCOUNT_DELETION_RESULT_TYPE, SpongeRegistryLoaders.accountDeletionResultType().values());
        holder.createRegistry(RegistryTypes.BAN_TYPE, SpongeRegistryLoaders.banType().values());
        holder.createRegistry(SpongeRegistryTypes.TRANSACTION_TYPE, SpongeRegistryLoaders.blockTransactionTypes().values());
        holder.createRegistry(RegistryTypes.BODY_PART, SpongeRegistryLoaders.bodyPart().values());
        holder.createRegistry(RegistryTypes.REGISTRY_KEYED_VALUE_PARAMETER, SpongeRegistryLoaders.valueParameter().values());
        holder.createRegistry(RegistryTypes.CLICK_TYPE, SpongeRegistryLoaders.clickType().values());
        holder.createRegistry(RegistryTypes.CAT_TYPE, SpongeRegistryLoaders.catType().values());
        holder.createRegistry(RegistryTypes.CLIENT_COMPLETION_KEY, SpongeRegistryLoaders.clientCompletionKey().values());
        holder.createRegistry(RegistryTypes.CLIENT_COMPLETION_TYPE, SpongeRegistryLoaders.clientCompletionType().values());
        holder.createRegistry(RegistryTypes.COMMAND_REGISTRAR_TYPE, () -> SpongeRegistryLoaders.commandRegistrarType().values(), true);
        holder.createRegistry(RegistryTypes.CURRENCY, null, true);
        holder.createRegistry(RegistryTypes.DAMAGE_TYPE, SpongeRegistryLoaders.damageType().values());
        holder.createRegistry(RegistryTypes.DAMAGE_MODIFIER_TYPE, SpongeRegistryLoaders.damageModifierType().values());
        holder.createRegistry(RegistryTypes.DISMOUNT_TYPE, SpongeRegistryLoaders.dismountType().values());
        holder.createRegistry(RegistryTypes.DISPLAY_SLOT, SpongeRegistryLoaders.displaySlot().values());
        holder.createRegistry(RegistryTypes.GOAL_EXECUTOR_TYPE, SpongeRegistryLoaders.goalExecutorType().values());
        holder.createRegistry(RegistryTypes.GOAL_TYPE, SpongeRegistryLoaders.goalType().values());
        holder.createRegistry(RegistryTypes.HORSE_COLOR, SpongeRegistryLoaders.horseColor().values());
        holder.createRegistry(RegistryTypes.HORSE_STYLE, SpongeRegistryLoaders.horseStyle().values());
        holder.createRegistry(RegistryTypes.LLAMA_TYPE, SpongeRegistryLoaders.llamaType().values());
        holder.createRegistry(RegistryTypes.MATTER_TYPE, SpongeRegistryLoaders.matterType().values());
        holder.createRegistry(RegistryTypes.MOVEMENT_TYPE, SpongeRegistryLoaders.movementType().values());
        holder.createRegistry(RegistryTypes.MUSIC_DISC, SpongeRegistryLoaders.musicDisc().values());
        holder.createRegistry(RegistryTypes.NOTE_PITCH, SpongeRegistryLoaders.notePitch().values());
        holder.createRegistry(RegistryTypes.OPERATION, SpongeRegistryLoaders.operation().values());
        holder.createRegistry(RegistryTypes.PALETTE_TYPE, SpongeRegistryLoaders.paletteType().values());
        holder.createRegistry(RegistryTypes.PARROT_TYPE, SpongeRegistryLoaders.parrotType().values());
        holder.createRegistry(RegistryTypes.PARTICLE_OPTION, SpongeRegistryLoaders.particleOption().values());
        holder.createRegistry(RegistryTypes.PLACEHOLDER_PARSER, SpongeRegistryLoaders.placeholderParser().values());
        holder.createRegistry(RegistryTypes.PORTAL_TYPE, SpongeRegistryLoaders.portalType().values());
        holder.createRegistry(RegistryTypes.QUERY_TYPE, SpongeRegistryLoaders.queryType().values());
        holder.createRegistry(RegistryTypes.RABBIT_TYPE, SpongeRegistryLoaders.rabbitType().values());
        holder.createRegistry(RegistryTypes.RESOLVE_OPERATION, SpongeRegistryLoaders.resolveOperation().values());
        holder.createRegistry(RegistryTypes.SELECTOR_TYPE, SpongeRegistryLoaders.selectorType().values());
        holder.createRegistry(RegistryTypes.SELECTOR_SORT_ALGORITHM, SpongeRegistryLoaders.selectorSortAlgorithm().values());
        holder.createRegistry(RegistryTypes.SKIN_PART, SpongeRegistryLoaders.skinPart().values());
        holder.createRegistry(RegistryTypes.SPAWN_TYPE, SpongeRegistryLoaders.spawnType().values());
        holder.createRegistry(RegistryTypes.TELEPORT_HELPER_FILTER, SpongeRegistryLoaders.teleportHelperFilter().values());
        holder.createRegistry(SpongeRegistryTypes.VALIDATION_TYPE, SpongeRegistryLoaders.validationType().values());
        holder.createRegistry(RegistryTypes.WEATHER_TYPE, SpongeRegistryLoaders.weather().values());
        holder.createRegistry(RegistryTypes.DATA_FORMAT, SpongeRegistryLoaders.dataFormat().values());
        holder.createRegistry(RegistryTypes.MAP_COLOR_TYPE, SpongeRegistryLoaders.mapColorType().values());
        holder.createRegistry(RegistryTypes.MAP_DECORATION_ORIENTATION, SpongeRegistryLoaders.mapDecorationOrientation().values());
        holder.createRegistry(RegistryTypes.MAP_DECORATION_TYPE, SpongeRegistryLoaders.mapDecorationType().values());
        holder.createRegistry(RegistryTypes.MAP_SHADE, SpongeRegistryLoaders.mapShade().values());
    }

    public static void registerServerRegistries(final SpongeRegistryHolder holder) {
    }
}
