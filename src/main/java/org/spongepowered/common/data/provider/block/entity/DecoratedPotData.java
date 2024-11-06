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
package org.spongepowered.common.data.provider.block.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraft.world.level.block.entity.PotDecorations;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.common.accessor.world.level.block.entity.DecoratedPotBlockEntityAccessor;
import org.spongepowered.common.data.provider.DataProviderRegistrator;

import java.util.Optional;

public final class DecoratedPotData {

    public static void register(final DataProviderRegistrator registrator) {
        // @formatter:off
        registrator.asMutable(DecoratedPotBlockEntity.class)
            .create(Keys.POT_FRONT_DECORATION)
                .get((h) -> h.getDecorations().front().map(ItemType.class::cast).orElseGet(ItemTypes.BRICK))
                .setAnd((h, v) -> DecoratedPotData.setPotDirection(h, PotDirection.FRONT, v))
            .create(Keys.POT_BACK_DECORATION)
                .get((h) -> h.getDecorations().back().map(ItemType.class::cast).orElseGet(ItemTypes.BRICK))
                .setAnd((h, v) -> DecoratedPotData.setPotDirection(h, PotDirection.BACK, v))
            .create(Keys.POT_LEFT_DECORATION)
                .get((h) -> h.getDecorations().left().map(ItemType.class::cast).orElseGet(ItemTypes.BRICK))
                .setAnd((h, v) -> DecoratedPotData.setPotDirection(h, PotDirection.LEFT, v))
            .create(Keys.POT_RIGHT_DECORATION)
                .get((h) -> h.getDecorations().right().map(ItemType.class::cast).orElseGet(ItemTypes.BRICK))
                .setAnd((h, v) -> DecoratedPotData.setPotDirection(h, PotDirection.RIGHT, v))
            ;
        // @formatter:on
    }

    static boolean setPotDirection(DecoratedPotBlockEntity h, PotDirection direction, ItemType v) {
        if (v == Items.BRICK) {
            var current = h.getDecorations();
            var newDecorations = direction.applyToDirection(current, null);
            ((DecoratedPotBlockEntityAccessor) h).accessor$setDecorations(newDecorations);
            h.setChanged();
            return true;
        }
        if (DecoratedPotPatterns.getPatternFromItem((Item) v) == null) {
            return false;
        }
        var current = h.getDecorations();
        var newDecorations = direction.applyToDirection(current, (Item) v);
        ((DecoratedPotBlockEntityAccessor) h).accessor$setDecorations(newDecorations);
        h.setChanged();
        return true;
    }

    enum PotDirection {
        FRONT {
            @Override
            PotDecorations applyToDirection(PotDecorations existing, @Nullable Item value) {
                return new PotDecorations(
                    existing.back(),
                    existing.left(),
                    existing.right(),
                    Optional.ofNullable(value)
                );
            }
        },
        BACK {
            @Override
            PotDecorations applyToDirection(PotDecorations existing, @Nullable Item value) {
                return new PotDecorations(
                    Optional.ofNullable(value),
                    existing.left(),
                    existing.right(),
                    existing.front()
                );
            }
        },
        LEFT {
            @Override
            PotDecorations applyToDirection(PotDecorations existing, @Nullable Item value) {
                return new PotDecorations(
                    existing.back(),
                    Optional.ofNullable(value),
                    existing.right(),
                    existing.front()
                );
            }
        },
        RIGHT {
            @Override
            PotDecorations applyToDirection(PotDecorations existing, @Nullable Item value) {
                return new PotDecorations(
                    existing.back(),
                    existing.left(),
                    Optional.ofNullable(value),
                    existing.front()
                );
            }
        };

        abstract PotDecorations applyToDirection(final PotDecorations existing, final @Nullable Item value);
    }

}
