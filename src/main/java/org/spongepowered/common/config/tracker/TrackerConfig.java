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
package org.spongepowered.common.config.tracker;

import ninja.leaping.configurate.objectmapping.Setting;
import org.spongepowered.common.config.Config;
import org.spongepowered.common.config.ConfigHandle;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TrackerConfig implements Config {

    public static final String FILE_NAME = "tracker.conf";

    /**
     * Used to determine whether block changes will be "captured" until
     * a {@link IPhaseState#unwind(PhaseContext)} is called. Specifically
     * used for certain entities and block entities where we can expect
     * multiple block changes to take place without negatively affecting
     * expected mechanics, functionality, or performance.
     *
     */
    public static final String BLOCK_BULK_CAPTURE = "block-bulk-capture";
    public static final String ENTITY_BULK_CAPTURE = "entity-bulk-capture";
    /**
     * Used to determine whether block changes will throw an event. This is
     * second priority to {@link #BLOCK_BULK_CAPTURE} as bulk captures take
     * priority. Likewise, some phase states are already strictly performing
     * singular events regardless of whether bulk captures are enabled or not.
     */
    public static final String BLOCK_EVENT_CREATION = "block-event-creation";
    public static final String ENTITY_EVENT_CREATION = "entity-block-creation";

    @Setting("block")
    private BlockTrackerCategory blockTracker = new BlockTrackerCategory();

    @Setting("entity")
    private EntityTrackerCategory entityTracker = new EntityTrackerCategory();

    @Setting("blockentity")
    private BlockEntityTrackerCategory blockEntityTracker = new BlockEntityTrackerCategory();

    public BlockTrackerCategory getBlockTracker() {
        return this.blockTracker;
    }

    public EntityTrackerCategory getEntityTracker() {
        return this.entityTracker;
    }

    public BlockEntityTrackerCategory getBlockEntityTracker() {
        return this.blockEntityTracker;
    }
}
