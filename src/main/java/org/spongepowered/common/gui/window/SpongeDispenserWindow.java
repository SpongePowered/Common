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
package org.spongepowered.common.gui.window;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntityDispenser;
import org.spongepowered.api.block.tileentity.carrier.Dispenser;
import org.spongepowered.api.gui.window.DispenserWindow;

public class SpongeDispenserWindow extends AbstractSpongeTileContainerWindow<TileEntityDispenser, Dispenser> implements DispenserWindow {

    public SpongeDispenserWindow() {
        super(TileEntityDispenser.class);
    }

    @Override
    protected TileEntityDispenser createVirtualTile() {
        return new TileEntityDispenser() {

            @Override
            public boolean isUseableByPlayer(EntityPlayer player) {
                return SpongeDispenserWindow.this.players.contains(player);
            }
        };
    }

    public static class Builder extends SpongeWindowBuilder<DispenserWindow, DispenserWindow.Builder> implements DispenserWindow.Builder {

        @Override
        public DispenserWindow build() {
            return new SpongeDispenserWindow();
        }
    }

}
