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
package org.spongepowered.common.util;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.entity.BlockEntityType;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.world.server.ServerWorldAccessor;
import org.spongepowered.common.bridge.TrackableBridge;
import org.spongepowered.common.bridge.activation.ActivationCapabilityBridge;
import org.spongepowered.common.bridge.block.BlockBridge;
import org.spongepowered.common.bridge.entitycollision.CollisionCapabilityBridge;
import org.spongepowered.common.bridge.world.ServerWorldBridge;
import org.spongepowered.common.bridge.world.storage.WorldInfoBridge;
import org.spongepowered.common.config.SpongeConfig;
import org.spongepowered.applaunch.config.inheritable.WorldConfig;
import org.spongepowered.common.world.teleport.ConfigTeleportHelperFilter;

import java.util.stream.Collectors;

public class SpongeHooks {

    public static void refreshActiveConfigs() {
        for (final BlockType blockType : Sponge.getRegistry().getCatalogRegistry().getAllOf(BlockType.class).collect(Collectors.toList())) {
            if (blockType instanceof CollisionCapabilityBridge) {
                ((CollisionCapabilityBridge) blockType).collision$requiresCollisionsCacheRefresh(true);
            }
            if (blockType instanceof TrackableBridge) {
                ((BlockBridge) blockType).bridge$initializeTrackerState();
            }
        }
        for (final BlockEntityType blockEntityType : TileEntityTypeRegistryModule.getInstance().getAll()) {
            ((SpongeTileEntityType) blockEntityType).initializeTrackerState();
        }
        for (final EntityType<?> entityType : EntityTypeRegistryModule.getInstance().getAll()) {
            ((SpongeEntityType) entityType).initializeTrackerState();
        }

        for (final org.spongepowered.api.world.server.ServerWorld apiWorld : SpongeCommon.getWorldManager().getWorlds()) {
            final ServerWorld world = (ServerWorld) apiWorld;
            final SpongeConfig<WorldConfig> configAdapter = ((WorldInfoBridge) world.getWorldInfo()).bridge$getConfigAdapter();
            // Reload before updating world config cache
            configAdapter.load();
            ((ServerWorldBridge) world).bridge$updateConfigCache();
            for (final Entity entity : ((ServerWorldAccessor) world).accessor$getEntitiesById().values()) {
                if (entity instanceof ActivationCapabilityBridge) {
                    ((ActivationCapabilityBridge) entity).activation$requiresActivationCacheRefresh(true);
                }
                if (entity instanceof CollisionCapabilityBridge) {
                    ((CollisionCapabilityBridge) entity).collision$requiresCollisionsCacheRefresh(true);
                }
                if (entity instanceof TrackableBridge) {
                    ((TrackableBridge) entity).bridge$refreshTrackerStates();
                }
            }
            for (final TileEntity tileEntity : world.loadedTileEntityList) {
                if (tileEntity instanceof ActivationCapabilityBridge) {
                    ((ActivationCapabilityBridge) tileEntity).activation$requiresActivationCacheRefresh(true);
                }
                if (tileEntity instanceof TrackableBridge) {
                    ((TrackableBridge) tileEntity).bridge$refreshTrackerStates();
                }
            }
        }
        ConfigTeleportHelperFilter.invalidateCache();
    }
}
