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
package org.spongepowered.common.world.server;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.TicketType;
import org.spongepowered.common.util.Constants;

import java.util.Comparator;
import java.util.Objects;

@SuppressWarnings("unchecked")
public final class SpongeTicketTypeBuilder<T> implements TicketType.Builder<T> {

    private @MonotonicNonNull String name;
    private @Nullable Comparator<T> comparator;
    private @MonotonicNonNull Ticks lifetime;

    @Override
    public TicketType.Builder<T> reset() {
        this.name = null;
        this.comparator = null;
        this.lifetime = null;
        return this;
    }

    @Override
    public TicketType.Builder<T> name(final String name) {
        this.name = Objects.requireNonNull(name, "Name cannot null");
        return this;
    }

    @Override
    public TicketType.Builder<T> comparator(final @Nullable Comparator<T> comparator) {
        this.comparator = comparator;
        return this;
    }

    @Override
    public TicketType.Builder<T> lifetime(final Ticks lifetime) {
        Objects.requireNonNull(lifetime, "Lifetime cannot be null");
        if (!lifetime.isInfinite() && lifetime.ticks() <= 0) {
            throw new IllegalArgumentException("The lifetime is required to be a positive integer");
        }
        this.lifetime = lifetime;
        return this;
    }

    @Override
    public TicketType<T> build() {
        Objects.requireNonNull(this.name, "Name cannot null");
        Objects.requireNonNull(this.lifetime, "Lifetime cannot be null");
        if (this.comparator == null) {
            this.comparator = (v1, v2) -> 0;
        }
        final var timeout = this.lifetime.isInfinite() ? Constants.ChunkTicket.INFINITE_TIMEOUT : this.lifetime.ticks();
        return (TicketType<T>) (Object) new net.minecraft.server.level.TicketType(
            timeout,
            false, // TODO - Do we determine if a ticket type should be stored?
            net.minecraft.server.level.TicketType.TicketUse.LOADING_AND_SIMULATION);
    }
}
