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

import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.Ticket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.accessor.server.level.TicketAccessor;
import org.spongepowered.common.accessor.world.level.TicketStorageAccessor;
import org.spongepowered.common.bridge.world.DistanceManagerBridge;
import org.spongepowered.common.bridge.world.server.TicketBridge;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.SpongeTicks;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3i;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin implements DistanceManagerBridge {

    // @formatter:off
    @Shadow @Final TicketStorage ticketStorage;
    // @formatter:on

    @Override
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public boolean bridge$checkTicketValid(final Ticket<?> ticket) {
        // Only report the ticket is valid if it's associated with this manager.
        final var nativeTicket = ((net.minecraft.server.level.Ticket) (Object) ticket);
        final var bridgeTicket = (TicketBridge) ticket;
        final var tickets = this.ticketStorage.getTickets(bridgeTicket.bridge$chunkPosition());
        if (tickets != null && tickets.contains(nativeTicket)) {
            return nativeTicket.isTimedOut();
        }
        return false;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public Ticks bridge$timeLeft(final Ticket<?> ticket) {
        if (this.bridge$checkTicketValid(ticket)) {
            final var mcTicket = (net.minecraft.server.level.Ticket) ticket;
            if (mcTicket.getType().timeout() == 0) {
                return Ticks.zero();
            }
            final long left = ((TicketAccessor) ticket).accessor$ticksLeft();
            return new SpongeTicks(Math.max(0, left));
        }
        return Ticks.zero();
    }

    @Override
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public boolean bridge$renewTicket(final Ticket<?> ticket) {
        if (this.bridge$checkTicketValid(ticket)) {
            final var nativeTicket = (net.minecraft.server.level.Ticket) (Object) ticket;
            nativeTicket.resetTicksLeft();
            this.ticketStorage.setDirty();
            return true;
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S, T> Optional<Ticket<T>> bridge$registerTicket(
            final ServerWorld world, final org.spongepowered.api.world.server.TicketType<T> ticketType,
            final Vector3i pos, final T value, final int distanceLimit) {
        final int distance = Mth.clamp(Constants.ChunkTicket.MAX_FULL_CHUNK_DISTANCE - distanceLimit, 0, Constants.ChunkTicket.MAX_FULL_CHUNK_TICKET_LEVEL);
        final TicketType type = (TicketType) (Object) ticketType;
        final net.minecraft.server.level.Ticket ticketToRequest = new net.minecraft.server.level.Ticket(type, distance);
        this.ticketStorage.addTicket(VecHelper.toChunkPos(pos).toLong(), ticketToRequest);
        return Optional.of(((TicketBridge) (Object) ticketToRequest).bridge$retrieveAppropriateTicket());
    }

    @Override
    @SuppressWarnings({"ConstantConditions"})
    public boolean bridge$releaseTicket(final Ticket<?> ticket) {
        if (this.bridge$checkTicketValid(ticket)) {
            final var chunkPos = ((TicketBridge) ticket).bridge$chunkPosition();
            return this.ticketStorage.removeTicket(chunkPos, (net.minecraft.server.level.Ticket) ticket);
        }
        return false;
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public <T> Collection<Ticket<T>> bridge$tickets(final org.spongepowered.api.world.server.TicketType<T> ticketType) {
        return ((TicketStorageAccessor) this.ticketStorage).accessor$tickets().values().stream()
                .flatMap(x -> x.stream().filter(ticket -> ticket.getType() == (TicketType) (Object) ticketType))
                .map(x -> (Ticket<T>) (Object) x)
                .collect(Collectors.toList());
    }

}
