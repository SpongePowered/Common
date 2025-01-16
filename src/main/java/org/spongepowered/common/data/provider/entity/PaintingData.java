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
package org.spongepowered.common.data.provider.entity;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.ArtType;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.server.level.ChunkMapAccessor;
import org.spongepowered.common.accessor.server.level.ChunkMap_TrackedEntityAccessor;
import org.spongepowered.common.accessor.world.entity.decoration.HangingEntityAccessor;
import org.spongepowered.common.accessor.world.entity.decoration.PaintingAccessor;
import org.spongepowered.common.data.provider.DataProviderRegistrator;

public final class PaintingData {

    private PaintingData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(Painting.class)
                    .create(Keys.ART_TYPE)
                        .get(h -> (ArtType) (Object) h.getVariant().value())
                        .setAnd((h, v) -> {
                            if (!h.level().isClientSide) {
                                final Holder<PaintingVariant> oldArt = h.getVariant();
                                var newArt = SpongeCommon.server().registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT).wrapAsHolder((PaintingVariant) (Object) v);
                                ((PaintingAccessor) h).invoker$setVariant(newArt);
                                ((HangingEntityAccessor) h).invoker$setDirection(h.getDirection());
                                if (!h.survives()) {
                                    ((PaintingAccessor) h).invoker$setVariant(oldArt);
                                    ((HangingEntityAccessor) h).invoker$setDirection(h.getDirection());
                                    return false;
                                }

                                final ChunkMapAccessor chunkManager = (ChunkMapAccessor) ((ServerLevel) h.level()).getChunkSource().chunkMap;
                                final ChunkMap_TrackedEntityAccessor paintingTracker = chunkManager.accessor$entityMap().get(h.getId());
                                if (paintingTracker == null) {
                                    return true;
                                }

                                for (final ServerPlayerConnection playerConnection : paintingTracker.accessor$seenBy().toArray(new ServerPlayerConnection[0])) {
                                    final ServerPlayer player = playerConnection.getPlayer();
                                    paintingTracker.accessor$removePlayer(player);
                                    paintingTracker.accessor$updatePlayer(player);
                                }
                                return true;
                            }
                            return true;
                        });
    }
    // @formatter:on
}
