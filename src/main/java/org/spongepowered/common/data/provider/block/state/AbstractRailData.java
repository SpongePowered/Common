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

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.state.IProperty;
import net.minecraft.state.properties.RailShape;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.RailDirection;
import org.spongepowered.common.data.provider.DataProviderRegistrator;

public final class AbstractRailData {

    private AbstractRailData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asImmutable(BlockState.class)
                    .create(Keys.RAIL_DIRECTION)
                        .get(h -> (RailDirection) (Object) h.get(((AbstractRailBlock) h.getBlock()).getShapeProperty()))
                        .set((h, v) -> {
                            final RailShape shape = (RailShape) (Object) v;
                            final IProperty<RailShape> property = ((AbstractRailBlock) h.getBlock()).getShapeProperty();
                            if (!property.getAllowedValues().contains(shape)) {
                                return h;
                            }
                            return h.with(property, shape);
                        })
                        .supports(h -> h.getBlock() instanceof AbstractRailBlock);
    }
    // @formatter:on
}
