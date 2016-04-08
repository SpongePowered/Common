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

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.world.gen.PopulatorType;

import java.util.Arrays;
import java.util.UUID;

public class StaticMixinHelper {

    public static EntityPlayerMP packetPlayer = null;
    public static boolean processingInternalForgeEvent = false;
    // Set before firing an internal Forge BlockBreak event to handle extended blockstate
    public static IBlockState breakEventExtendedState = null;
    public static boolean convertingMapFormat = false;

    // This is only set in SpongeForge, but it removes the problem of having both SpongeForge
    // and SpongeCommon attempting to redirect ItemInWorldManager;activateBlockOrUseItem in NetHandlerPlayServer.
    public static boolean lastPlayerInteractCancelled = false;

    // For animation packet
    public static int lastAnimationPacketTick = 0;
    public static int lastSecondaryPacketTick = 0;
    public static int lastPrimaryPacketTick = 0;
    public static EntityPlayerMP lastAnimationPlayer = null;
}
