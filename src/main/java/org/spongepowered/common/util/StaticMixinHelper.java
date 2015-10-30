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

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.common.world.gen.SpongePopulatorType;

public class StaticMixinHelper {

    public static EntityPlayerMP processingPlayer = null;
    public static Packet processingPacket = null;
    public static boolean processingInternalForgeEvent = false;
    public static SpongePopulatorType populator = null;
    public static ItemStack lastPlayerItem = null;
    @SuppressWarnings("rawtypes")
    public static Class lastPopulatorClass = null;
    public static boolean isFlowerGen = false;
    public static ItemStackSnapshot lastCursor = null;
    public static Container lastOpenContainer = null;
    public static IInventory lastOpenInventory = null;

    @SuppressWarnings({"deprecation", "rawtypes"})
    public static Class getCallerClass(int level) {
        return sun.reflect.Reflection.getCallerClass(level);
    }
}
