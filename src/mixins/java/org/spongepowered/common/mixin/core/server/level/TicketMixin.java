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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.common.bridge.world.server.TicketBridge;

@Mixin(Ticket.class)
public abstract class TicketMixin implements TicketBridge {

    @Unique
    private long impl$chunkPosition;
    @Unique
    private Ticket impl$parent;

    @Override
    public long bridge$chunkPosition() {
        return this.impl$chunkPosition;
    }

    @Override
    public void bridge$setChunkPosition(final long chunkPos) {
        this.impl$chunkPosition = chunkPos;
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public <T> org.spongepowered.api.world.server.Ticket<T> bridge$retrieveAppropriateTicket() {
        if (this.impl$parent != null) {
            return (org.spongepowered.api.world.server.Ticket<T>) (Object) this.impl$parent;
        }
        return (org.spongepowered.api.world.server.Ticket<T>) this;
    }

    @Override
    public void bridge$setParentTicket(final Ticket parentTicket) {
        this.impl$parent = parentTicket;
    }

}
