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
package org.spongepowered.common.world;

import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.common.event.tracking.BlockChangeFlagManager;
import org.spongepowered.common.util.Constants;

import java.util.StringJoiner;

/**
 * A flag of sorts that determines whether a block change will perform various
 * interactions, such as notifying neighboring blocks, performing block physics
 * on placement, etc.
 */
public record SpongeBlockChangeFlag(int rawFlag) implements BlockChangeFlag {

    @Override
    public boolean updateNeighbors() {
        return (this.rawFlag & Constants.BlockChangeFlags.BLOCK_UPDATED) != 0; // 1;
    }

    @Override
    public boolean notifyClients() {
        return (this.rawFlag & Constants.BlockChangeFlags.NOTIFY_CLIENTS) != 0; // 2;
    }

    @Override
    public boolean performBlockPhysics() {
        return (this.rawFlag & Constants.BlockChangeFlags.PHYSICS_MASK) == 0; // sponge
    }

    @Override
    public boolean updateNeighboringShapes() {
        return (this.rawFlag & Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE) == 0; // 16
    }

    @Override
    public boolean updateLighting() {
        return (this.rawFlag & Constants.BlockChangeFlags.LIGHTING_UPDATES) == 0; // 128 vanilla check
    }

    @Override
    public boolean notifyPathfinding() {
        return (this.rawFlag & Constants.BlockChangeFlags.PATHFINDING_UPDATES) == 0; // sponge
    }

    @Override
    public boolean ignoreRender() {
        return (this.rawFlag & Constants.BlockChangeFlags.IGNORE_RENDER) != 0; // 4
    }

    @Override
    public boolean forceClientRerender() {
        return (this.rawFlag & Constants.BlockChangeFlags.FORCE_RE_RENDER) != 0; // 8
    }

    @Override
    public boolean movingBlocks() {
        return (this.rawFlag & Constants.BlockChangeFlags.BLOCK_MOVING) != 0; // 64
    }

    @Override
    public boolean neighborDropsAllowed() {
        return (this.rawFlag & Constants.BlockChangeFlags.NEIGHBOR_DROPS) == 0; // 32
    }

    @Override
    public boolean performBlockDestruction() {
        return (this.rawFlag & Constants.BlockChangeFlags.STRUCTURE_PLACEMENT) == 0; // 256
    }

    @Override
    public SpongeBlockChangeFlag withUpdateNeighbors(final boolean updateNeighbors) {
        if (this.updateNeighbors() == updateNeighbors) {
            return this;
        }
        return BlockChangeFlagManager.fromNativeInt(updateNeighbors
            ? this.rawFlag | Constants.BlockChangeFlags.BLOCK_UPDATED
            : this.rawFlag & ~Constants.BlockChangeFlags.BLOCK_UPDATED);
    }

    @Override
    public SpongeBlockChangeFlag withNotifyClients(final boolean notifyClients) {
        if (this.notifyClients() == notifyClients) {
            return this;
        }
        return BlockChangeFlagManager.fromNativeInt(notifyClients
            ? this.rawFlag | Constants.BlockChangeFlags.NOTIFY_CLIENTS
            : this.rawFlag & ~Constants.BlockChangeFlags.NOTIFY_CLIENTS);
    }

    @Override
    public SpongeBlockChangeFlag withPhysics(final boolean performBlockPhysics) {
        if (this.performBlockPhysics() == performBlockPhysics) {
            return this;
        }
        return BlockChangeFlagManager.fromNativeInt(performBlockPhysics
            ? this.rawFlag & ~Constants.BlockChangeFlags.PHYSICS_MASK
            : this.rawFlag | Constants.BlockChangeFlags.PHYSICS_MASK);
    }

    @Override
    public SpongeBlockChangeFlag withNotifyObservers(final boolean notifyObservers) {
        if (this.updateNeighboringShapes() == notifyObservers) {
            return this;
        }
        return BlockChangeFlagManager.fromNativeInt(notifyObservers
            ? this.rawFlag & ~Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE
            : this.rawFlag | Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE);
    }

    @Override
    public SpongeBlockChangeFlag withLightingUpdates(final boolean lighting) {
        if (this.updateLighting() == lighting) {
            return this;
        }
        return BlockChangeFlagManager.fromNativeInt(lighting
            ? this.rawFlag & ~Constants.BlockChangeFlags.LIGHTING_UPDATES
            : this.rawFlag | Constants.BlockChangeFlags.LIGHTING_UPDATES);
    }

    @Override
    public SpongeBlockChangeFlag withPathfindingUpdates(final boolean pathfindingUpdates) {
        if (this.notifyPathfinding() == pathfindingUpdates) {
            return this;
        }
        return BlockChangeFlagManager.fromNativeInt(pathfindingUpdates
            ? this.rawFlag & ~Constants.BlockChangeFlags.PATHFINDING_UPDATES
            : this.rawFlag | Constants.BlockChangeFlags.PATHFINDING_UPDATES);
    }

    @Override
    public SpongeBlockChangeFlag withNeighborDropsAllowed(boolean dropsAllowed) {
        if (this.neighborDropsAllowed() == dropsAllowed) {
            return this;
        }
        return BlockChangeFlagManager.fromNativeInt(dropsAllowed
            ? this.rawFlag & ~Constants.BlockChangeFlags.NEIGHBOR_DROPS
            : this.rawFlag | Constants.BlockChangeFlags.NEIGHBOR_DROPS);
    }

    @Override
    public SpongeBlockChangeFlag withBlocksMoving(boolean moving) {
        if (this.movingBlocks() == moving) {
            return this;
        }
        return BlockChangeFlagManager.fromNativeInt(moving
            ? this.rawFlag | Constants.BlockChangeFlags.BLOCK_MOVING
            : this.rawFlag & ~Constants.BlockChangeFlags.BLOCK_MOVING);
    }

    @Override
    public SpongeBlockChangeFlag withIgnoreRender(boolean ignoreRender) {
        if (this.ignoreRender() == ignoreRender) {
            return this;
        }
        return BlockChangeFlagManager.fromNativeInt(ignoreRender
            ? this.rawFlag | Constants.BlockChangeFlags.IGNORE_RENDER
            : this.rawFlag & ~Constants.BlockChangeFlags.IGNORE_RENDER);
    }

    @Override
    public SpongeBlockChangeFlag withForcedReRender(boolean forcedReRender) {
        if (this.forceClientRerender() == forcedReRender) {
            return this;
        }
        return BlockChangeFlagManager.fromNativeInt(forcedReRender
            ? this.rawFlag | Constants.BlockChangeFlags.FORCE_RE_RENDER
            : this.rawFlag & ~Constants.BlockChangeFlags.FORCE_RE_RENDER);
    }

    @Override
    public SpongeBlockChangeFlag withPerformBlockDestruction(boolean performBlockDestruction) {
        if (this.performBlockDestruction() == performBlockDestruction) {
            return this;
        }
        return BlockChangeFlagManager.fromNativeInt(performBlockDestruction
            ? this.rawFlag & ~Constants.BlockChangeFlags.STRUCTURE_PLACEMENT
            : this.rawFlag | Constants.BlockChangeFlags.STRUCTURE_PLACEMENT);
    }

    @Override
    public SpongeBlockChangeFlag inverse() {
        return BlockChangeFlagManager.fromNativeInt((~this.rawFlag) & Constants.BlockChangeFlags.MASK);
    }

    @Override
    public SpongeBlockChangeFlag andFlag(final BlockChangeFlag flag) {
        final SpongeBlockChangeFlag o = (SpongeBlockChangeFlag) flag;
        return BlockChangeFlagManager.fromNativeInt(this.rawFlag | o.rawFlag);
    }

    @Override
    public SpongeBlockChangeFlag andNotFlag(final BlockChangeFlag flag) {
        final SpongeBlockChangeFlag o = (SpongeBlockChangeFlag) flag;
        return BlockChangeFlagManager.fromNativeInt(this.rawFlag & ~o.rawFlag);
    }

    public int getRawFlag() {
        return this.rawFlag;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SpongeBlockChangeFlag.class.getSimpleName() + "[", "]")
                .add("rawFlag=" + this.rawFlag)
                .add("notifyNeighbors=" + this.updateNeighbors())
                .add("notifyClients=" + this.notifyClients())
                .add("performBlockPhysics=" + this.performBlockPhysics())
                .add("updateNeighboringShapes=" + this.updateNeighboringShapes())
                .add("updateLighting=" + this.updateLighting())
                .add("notifyPathfinding=" + this.notifyPathfinding())
                .add("ignoreRender=" + this.ignoreRender())
                .add("forceClientRerender=" + this.forceClientRerender())
                .add("movingBlocks=" + this.movingBlocks())
                .add("neighborDropsAllowed=" + this.neighborDropsAllowed())
                .add("performBlockDestruction=" + this.performBlockDestruction())
                .toString();
    }

    public SpongeBlockChangeFlag asNestedNeighborUpdates() {
        return BlockChangeFlagManager.fromNativeInt(this.rawFlag & ~(Constants.BlockChangeFlags.BLOCK_UPDATED | Constants.BlockChangeFlags.NEIGHBOR_DROPS));
    }
}
