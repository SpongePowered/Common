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
package org.spongepowered.common.data.provider.block.state;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.InstrumentType;
import org.spongepowered.api.data.type.MatterTypes;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.common.accessor.world.level.block.BaseFireBlockAccessor;
import org.spongepowered.common.accessor.world.level.block.state.BlockBehaviourAccessor;
import org.spongepowered.common.accessor.world.level.block.state.BlockBehaviour_PropertiesAccessor;
import org.spongepowered.common.bridge.block.BlockBridge;
import org.spongepowered.common.bridge.block.DyeColorBlockBridge;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.util.Constants;

import java.util.Collections;

public final class BlockData {

    private BlockData() {
    }

    // @formatter:off
    @SuppressWarnings("deprecation") public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asImmutable(Block.class)
                    .create(Keys.BLAST_RESISTANCE)
                        .get(h -> (double) ((BlockBehaviour_PropertiesAccessor) ((BlockBehaviourAccessor) h).accessor$properties()).accessor$explosionResistance())
                    .create(Keys.DYE_COLOR)
                        .get(h -> ((DyeColorBlockBridge) h).bridge$getDyeColor().orElse(null))
                        .supports(h -> h instanceof DyeColorBlockBridge)
                    .create(Keys.DESTROY_SPEED)
                        .get(block -> (double) ((BlockBehaviour_PropertiesAccessor) ((BlockBehaviourAccessor) block).accessor$properties()).accessor$destroyTime())
                    .create(Keys.HELD_ITEM)
                        .get(h -> {
                            final Item item = h.asItem();
                            if (item instanceof BlockItem) {
                                return (ItemType) item;
                            }
                            return null;
                        })
                        .supports(h -> h.asItem() instanceof BlockItem)
                    .create(Keys.IS_GRAVITY_AFFECTED)
                        .get(h -> h instanceof FallingBlock)
                    .create(Keys.IS_UNBREAKABLE)
                        .get(h -> (double) ((BlockBehaviour_PropertiesAccessor) ((BlockBehaviourAccessor) h).accessor$properties()).accessor$destroyTime() < 0)
                    .create(Keys.IS_SURROGATE_BLOCK)
                        .get(h -> ((BlockBridge) h).bridge$isDummy())
                .asImmutable(BlockState.class)
                    .create(Keys.CONNECTED_DIRECTIONS)
                        .get(h -> {
                            if (h.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
                                return null;
                            }
                            return Collections.singleton(Constants.DirectionFunctions.getFor(ChestBlock.getConnectedDirection(h)));
                        })
                        .supports(h -> h.hasProperty(ChestBlock.TYPE))
                    .create(Keys.IS_PASSABLE)
                        .get(h -> !h.blocksMotion())
                    .create(Keys.IS_FLAMMABLE)
                        .get(((BaseFireBlockAccessor) Blocks.FIRE)::invoker$canBurn)
                    .create(Keys.IS_SOLID)
                        .get(BlockBehaviour.BlockStateBase::isSolid)
                    .create(Keys.IS_REPLACEABLE)
                        .get(BlockBehaviour.BlockStateBase::canBeReplaced)
                    .create(Keys.LIGHT_EMISSION)
                        .get(BlockState::getLightEmission)
                    .create(Keys.MATTER_TYPE)
                        .get(h -> {
                            if (h.getBlock() instanceof LiquidBlock) {
                                return MatterTypes.LIQUID.get();
                            } else if (h.isAir()) {
                                return MatterTypes.GAS.get();
                            } else {
                                return MatterTypes.SOLID.get();
                            }
                        })
                    .create(Keys.BURNABLE)
                        .get(BlockBehaviour.BlockStateBase::ignitedByLava)
                    // TODO state above
                    .create(Keys.REPRESENTED_INSTRUMENT)
                        .get(h -> (InstrumentType) (Object) h.instrument());
    }
    // @formatter:on
}
