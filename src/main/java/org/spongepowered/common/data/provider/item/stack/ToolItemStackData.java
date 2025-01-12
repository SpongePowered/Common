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
package org.spongepowered.common.data.provider.item.stack;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.ToolRule;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.data.provider.DataProviderRegistrator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ToolItemStackData {

    private ToolItemStackData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(ItemStack.class)
                    .create(Keys.EFFICIENCY)
                        .get(h -> {
                            final @Nullable Tool tool = h.get(DataComponents.TOOL);
                            if (tool != null) {
                                return (double) tool.defaultMiningSpeed();
                            }
                            return null;
                        })
                        .set((h, v) -> {
                            final @Nullable Tool tool = h.get(DataComponents.TOOL);
                            if (tool != null) {
                                h.set(DataComponents.TOOL, new Tool(tool.rules(), v.floatValue(), tool.damagePerBlock(), tool.canDestroyBlocksInCreative()));
                                return;
                            }
                            h.set(DataComponents.TOOL, new Tool(List.of(), v.floatValue(), 1, true));
                        })
                        .delete(h -> h.remove(DataComponents.TOOL))
                .create(Keys.TOOL_DAMAGE_PER_BLOCK)
                    .get(h -> {
                        final @Nullable Tool tool = h.get(DataComponents.TOOL);
                        if (tool != null) {
                            return tool.damagePerBlock();
                        }
                        return null;
                    })
                    .set((h, v) -> {
                        final @Nullable Tool tool = h.get(DataComponents.TOOL);
                        if (tool != null) {
                            h.set(DataComponents.TOOL, new Tool(tool.rules(), tool.defaultMiningSpeed(), v, tool.canDestroyBlocksInCreative()));
                            return;
                        }
                        h.set(DataComponents.TOOL, new Tool(List.of(), 1f, v, true));
                    })
                .create(Keys.TOOL_RULES)
                    .get(h -> {
                        final @Nullable Tool tool = h.get(DataComponents.TOOL);
                        if (tool == null) {
                            return null;
                        }
                        return tool.rules().stream().map(ToolRule.class::cast).toList();
                    })
                    .set((h, v) -> {
                        var mcValue = v.stream().map(Tool.Rule.class::cast).toList();
                        final @Nullable Tool tool = h.get(DataComponents.TOOL);
                        if (tool != null) {
                            h.set(DataComponents.TOOL, new Tool(mcValue, tool.defaultMiningSpeed(), tool.damagePerBlock(), tool.canDestroyBlocksInCreative()));
                            return;
                        }
                        h.set(DataComponents.TOOL, new Tool(mcValue, 1f, 1, true));
                    })
                    .delete(h -> {
                        final @Nullable Tool tool = h.get(DataComponents.TOOL);
                        if (tool != null) {
                            h.set(DataComponents.TOOL, new Tool(List.of(), tool.defaultMiningSpeed(), tool.damagePerBlock(), tool.canDestroyBlocksInCreative()));
                        }
                    })
                .create(Keys.CAN_HARVEST)
                    .get(h -> {
                        final Registry<Block> blockRegistry = SpongeCommon.vanillaRegistry(Registries.BLOCK);
                        final Tool tool = h.get(DataComponents.TOOL);
                        if (tool != null) {
                            return tool.rules().stream().map(Tool.Rule::blocks)
                                    .flatMap(HolderSet::stream)
                                    .map(Holder::value)
                                    .map(BlockType.class::cast)
                                    .collect(Collectors.toSet());
                        }

                        final Set<BlockType> blockTypes = blockRegistry.stream()
                                .filter(b -> h.isCorrectToolForDrops(b.defaultBlockState()))
                                .map(BlockType.class::cast)
                                .collect(Collectors.toUnmodifiableSet());
                        return blockTypes.isEmpty() ? null : blockTypes;
                    })
        ;
    }
    // @formatter:on
}
