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

import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.api.gui.window.PlayerInventoryWindow;
import org.spongepowered.api.item.inventory.Container;

import java.util.Optional;

public class SpongePlayerInventoryWindow extends AbstractSpongeWindow implements PlayerInventoryWindow {

    @Override
    public Optional<Container> getContainer() {
        return Optional.empty();// TODO Optional.of((Container) this.player.inventoryContainer);
    }

    @Override
    protected boolean show(EntityPlayerMP player) {
        // TODO Auto-generated method stub
        return false;
    }

    public static class Builder extends SpongeWindowBuilder<PlayerInventoryWindow, PlayerInventoryWindow.Builder>
            implements PlayerInventoryWindow.Builder {

        @Override
        public PlayerInventoryWindow build() {
            return new SpongePlayerInventoryWindow();
        }
    }

}
