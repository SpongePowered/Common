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
package org.spongepowered.common.mixin.inventory.impl.server.level;

import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.inventory.adapter.impl.slots.SlotAdapter;
import org.spongepowered.common.item.util.ItemStackUtil;

@Mixin(targets = "net/minecraft/server/level/ServerPlayer$1")
public abstract class ServerPlayer_Mixin_Inventory {

    // @formatter:off
    @Shadow @Final ServerPlayer this$0;
    // @formatter:on

    @Inject(method = "sendInitialData", at = @At("RETURN"))
    private void inventory$sendOffhand(final AbstractContainerMenu containerMenu, final NonNullList<ItemStack> $$1, final ItemStack $$2, final int[] $$3, final CallbackInfo ci) {
        if (containerMenu == this.this$0.inventoryMenu) {
            return;
        }

        final org.spongepowered.api.item.inventory.Slot offhand
                = ((org.spongepowered.api.entity.living.player.server.ServerPlayer) this.this$0).inventory().offhand();
        this.this$0.connection.send(
                new ClientboundSetPlayerInventoryPacket(((SlotAdapter) offhand).getOrdinal(), ItemStackUtil.toNative(offhand.peek())));
    }
}
