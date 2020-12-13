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

import net.kyori.adventure.text.Component;
import net.minecraft.block.StandingSignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.tileentity.SignTileEntity;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.util.DirectionUtil;
import org.spongepowered.common.util.RotationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SignData {

    private SignData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(SignTileEntity.class)
                    .create(Keys.SIGN_LINES)
                        .get(SignData::getSignLines)
                        .set(SignData::setSignLines)
                        .delete(h -> SignData.setSignLines(h, Collections.emptyList()))
                    .create(Keys.DIRECTION)
                        .get(h -> {
                            if (h.getBlockState().getBlock() instanceof StandingSignBlock) {
                                return RotationUtils.getFor(h.getBlockState().get(StandingSignBlock.ROTATION));
                            } else if (h.getBlockState().getBlock() instanceof WallSignBlock) {
                                return DirectionUtil.getFor(h.getBlockState().get(WallSignBlock.FACING));
                            }
                            return null;
                        })
                        .setAnd((h, v) -> {
                            if (h.getBlockState().getBlock() instanceof StandingSignBlock) {
                                h.getWorld().setBlockState(h.getPos(), RotationUtils.set(h.getBlockState(), v, StandingSignBlock.ROTATION));
                                return true;
                            } else if (h.getBlockState().getBlock() instanceof WallSignBlock) {
                                h.getWorld().setBlockState(h.getPos(), DirectionUtil.set(h.getBlockState(), v, WallSignBlock.FACING));
                                return true;
                            }

                            return false;
                        })
                        .supports(h -> h.getWorld() != null)
                .asMutable(ServerLocation.class)
                    .create(Keys.SIGN_LINES)
                        .get(SignData::getSignLines)
                        .set(SignData::setSignLines)
                        .delete(h -> SignData.setSignLines(h, Collections.emptyList()))
                        .supports(loc -> loc.getBlockEntity().map(b -> b instanceof SignTileEntity).orElse(false));
    }
    // @formatter:on

    private static SignTileEntity toSignTileEntity(final ServerLocation holder) {
        return (SignTileEntity) holder.getBlockEntity().get();
    }

    private static void setSignLines(final ServerLocation holder, final List<Component> value) {
        SignData.setSignLines(SignData.toSignTileEntity(holder), value);
    }

    private static void setSignLines(final SignTileEntity holder, final List<Component> value) {
        for (int i = 0; i < holder.signText.length; i++) {
            holder.signText[i] = SpongeAdventure.asVanilla(i > value.size() - 1 ? Component.empty() : value.get(i));
        }
        holder.markDirty();
        holder.getWorld().notifyBlockUpdate(holder.getPos(), holder.getBlockState(), holder.getBlockState(), 3);
    }

    private static List<Component> getSignLines(ServerLocation h) {
        return SignData.getSignLines(SignData.toSignTileEntity(h));
    }

    private static List<Component> getSignLines(SignTileEntity h) {
        final List<Component> lines = new ArrayList<>(h.signText.length);
        for (int i = 0; i < h.signText.length; i++) {
            lines.add(SpongeAdventure.asAdventure(h.signText[i]));
        }
        return lines;
    }
}
