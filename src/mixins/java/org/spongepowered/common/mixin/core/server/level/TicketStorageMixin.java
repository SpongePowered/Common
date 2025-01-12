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
package org.spongepowered.common.mixin.core.server.level;

import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.bridge.world.server.TicketBridge;

@Mixin(TicketStorage.class)
public class TicketStorageMixin {

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "addTicket(JLnet/minecraft/server/level/Ticket;)Z", at = @At("HEAD"))
    private void impl$addChunkPosToTicket(final long chunkPos, final net.minecraft.server.level.Ticket ticket, final CallbackInfoReturnable<Boolean> ci) {
        ((TicketBridge) (Object) ticket).bridge$setChunkPosition(chunkPos);
    }

    @SuppressWarnings("ConstantConditions")
    @ModifyVariable(method = "addTicket(JLnet/minecraft/server/level/Ticket;)Z",
        at = @At(value = "LOAD"),
        slice = @Slice(
            from = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/level/TicketStorage;isTicketSameTypeAndLevel(Lnet/minecraft/server/level/Ticket;Lnet/minecraft/server/level/Ticket;)Z"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/Ticket;resetTicksLeft()V")
        ),
        index = 6
    )
    private Ticket impl$associateTicketWithParent(final Ticket storedTicket, final long pos, final Ticket originalTicket) {
        // We do this because we want to return the original ticket that will actually be operated on, but avoid a
        // potentially costly search on an array - because addTicket doesn't return the ticket that is actually
        // in the manager.
        if (storedTicket != originalTicket) {
            ((TicketBridge) (Object) originalTicket).bridge$setParentTicket(storedTicket);
        }
        return storedTicket;
    }

}
