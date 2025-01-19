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
package org.spongepowered.forge.launch.bridge.event;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.event.Event;

public interface ForgeEventBridge_Forge<E extends Event> {

    /**
     * Syncs the Sponge event to this Forge event
     *
     * <p>Note that a Sponge event might service multiple Forge events, so any
     * syncing should be limited to the remit of this event only (i.e., if the
     * event fires for multiple positions, the sync should only consider the
     * positions this event is concerned with)</p>
     *
     * @param event The Sponge event
     */
    void bridge$syncFrom(E event);

    /**
     * Syncs the Forge event to this Sponge event
     *
     * <p>Note that a Sponge event might service multiple Forge events, so any
     * syncing should be limited to the remit of this event only (that is, this
     * should only affect the Sponge event as far as the remit of this event goes,
     * most likely not cancelling events but invalidating transactions.)</p>
     *
     * @param event The Sponge event
     */
    void bridge$syncTo(E event);

    /**
     * Creates a Sponge event from this Forge event
     */
    @Nullable E bridge$createSpongeEvent();

}
