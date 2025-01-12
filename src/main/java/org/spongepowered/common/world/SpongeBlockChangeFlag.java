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
public final class SpongeBlockChangeFlag implements BlockChangeFlag {

    private final boolean updateNeighbors;
    private final boolean notifyClients;
    private final boolean performBlockPhysics;
    private final boolean updateNeighborShapes;
    private final boolean ignoreRender;
    private final boolean forceReRender;
    private final boolean blockMoving;
    private final boolean lighting;
    private final boolean pathfindingUpdates;
    private final boolean neighborDrops;
    private final boolean structurePlacement;
    private final int rawFlag;

    public SpongeBlockChangeFlag(final int flag) {
        this.updateNeighbors = (flag & Constants.BlockChangeFlags.BLOCK_UPDATED) != 0; // 1
        this.notifyClients = (flag & Constants.BlockChangeFlags.NOTIFY_CLIENTS) != 0; // 2
        this.ignoreRender = (flag & Constants.BlockChangeFlags.IGNORE_RENDER) != 0; // 4
        this.forceReRender = (flag & Constants.BlockChangeFlags.FORCE_RE_RENDER) != 0; // 8
        this.updateNeighborShapes = (flag & Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE) == 0; // 16
        this.neighborDrops = (flag & Constants.BlockChangeFlags.NEIGHBOR_DROPS) == 0; // 32
        this.blockMoving = (flag & Constants.BlockChangeFlags.BLOCK_MOVING) != 0; // 64
        this.lighting = (flag & Constants.BlockChangeFlags.LIGHTING_UPDATES) == 0; // 128 vanilla check
        this.structurePlacement = (flag & Constants.BlockChangeFlags.STRUCTURE_PLACEMENT) == 0; // 256 vanilla check
        this.performBlockPhysics = (flag & Constants.BlockChangeFlags.PHYSICS_MASK) == 0; // sponge
        this.pathfindingUpdates = (flag & Constants.BlockChangeFlags.PATHFINDING_UPDATES) == 0; // sponge
        this.rawFlag = flag;
    }

    @Override
    public boolean updateNeighbors() {
        return this.updateNeighbors;
    }

    @Override
    public boolean notifyClients() {
        return this.notifyClients;
    }

    @Override
    public boolean performBlockPhysics() {
        return this.performBlockPhysics;
    }

    @Override
    public boolean updateNeighboringShapes() {
        return this.updateNeighborShapes;
    }

    @Override
    public boolean updateLighting() {
        return this.lighting;
    }

    @Override
    public boolean notifyPathfinding() {
        return this.pathfindingUpdates;
    }

    @Override
    public boolean ignoreRender() {
        return this.ignoreRender;
    }

    @Override
    public boolean structurePlacement() {
        return this.structurePlacement;
    }

    @Override
    public boolean forceClientRerender() {
        return this.forceReRender;
    }

    @Override
    public boolean movingBlocks() {
        return this.blockMoving;
    }

    @Override
    public boolean neighborDropsAllowed() {
        return this.neighborDrops;
    }

    @Override
    public SpongeBlockChangeFlag withUpdateNeighbors(final boolean updateNeighbors) {
        if (this.updateNeighbors == updateNeighbors) {
            return this;
        }
        final int maskedFlag =
            (updateNeighbors ? Constants.BlockChangeFlags.BLOCK_UPDATED : 0)
                | (this.notifyClients ? Constants.BlockChangeFlags.NOTIFY_CLIENTS : 0)
                | (this.ignoreRender ? Constants.BlockChangeFlags.IGNORE_RENDER : 0)
                | (this.forceReRender ? Constants.BlockChangeFlags.FORCE_RE_RENDER : 0)
                | (this.updateNeighborShapes ? 0 : Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE)
                | (this.neighborDrops ? 0 : Constants.BlockChangeFlags.NEIGHBOR_DROPS)
                | (this.blockMoving ? Constants.BlockChangeFlags.BLOCK_MOVING : 0)
                | (this.performBlockPhysics ? 0 : Constants.BlockChangeFlags.PHYSICS_MASK)
                | (this.lighting ? 0 : Constants.BlockChangeFlags.LIGHTING_UPDATES)
                | (this.pathfindingUpdates ? 0 : Constants.BlockChangeFlags.PATHFINDING_UPDATES);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);
    }

    @Override
    public SpongeBlockChangeFlag withNotifyClients(final boolean notifyClients) {
        if (this.notifyClients == notifyClients) {
            return this;
        }
        final int maskedFlag =
                (this.updateNeighbors ? Constants.BlockChangeFlags.BLOCK_UPDATED : 0)
                        | (notifyClients ? Constants.BlockChangeFlags.NOTIFY_CLIENTS : 0)
                        | (this.ignoreRender ? Constants.BlockChangeFlags.IGNORE_RENDER : 0)
                        | (this.forceReRender ? Constants.BlockChangeFlags.FORCE_RE_RENDER : 0)
                        | (this.updateNeighborShapes ? 0 : Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE)
                        | (this.neighborDrops ? 0 : Constants.BlockChangeFlags.NEIGHBOR_DROPS)
                        | (this.blockMoving ? Constants.BlockChangeFlags.BLOCK_MOVING : 0)
                        | (this.performBlockPhysics ? 0 : Constants.BlockChangeFlags.PHYSICS_MASK)
                        | (this.lighting ? 0 : Constants.BlockChangeFlags.LIGHTING_UPDATES)
                        | (this.pathfindingUpdates ? 0 : Constants.BlockChangeFlags.PATHFINDING_UPDATES);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);
    }

    @Override
    public SpongeBlockChangeFlag withPhysics(final boolean performBlockPhysics) {
        if (this.performBlockPhysics == performBlockPhysics) {
            return this;
        }
        final int maskedFlag =
            (this.updateNeighbors ? Constants.BlockChangeFlags.BLOCK_UPDATED : 0)
                | (this.notifyClients ? Constants.BlockChangeFlags.NOTIFY_CLIENTS : 0)
                | (this.ignoreRender ? Constants.BlockChangeFlags.IGNORE_RENDER : 0)
                | (this.forceReRender ? Constants.BlockChangeFlags.FORCE_RE_RENDER : 0)
                | (this.updateNeighborShapes ? 0 : Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE)
                | (this.neighborDrops ? 0 : Constants.BlockChangeFlags.NEIGHBOR_DROPS)
                | (this.blockMoving ? Constants.BlockChangeFlags.BLOCK_MOVING : 0)
                | (performBlockPhysics ? 0 : Constants.BlockChangeFlags.PHYSICS_MASK)
                | (this.lighting ? 0 : Constants.BlockChangeFlags.LIGHTING_UPDATES)
                | (this.pathfindingUpdates ? 0 : Constants.BlockChangeFlags.PATHFINDING_UPDATES);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);
    }

    @Override
    public SpongeBlockChangeFlag withNotifyObservers(final boolean notifyObservers) {
        if (this.updateNeighborShapes == notifyObservers) {
            return this;
        }
        final int maskedFlag =
            (this.updateNeighbors ? Constants.BlockChangeFlags.BLOCK_UPDATED : 0)
                | (this.notifyClients ? Constants.BlockChangeFlags.NOTIFY_CLIENTS : 0)
                | (this.ignoreRender ? Constants.BlockChangeFlags.IGNORE_RENDER : 0)
                | (this.forceReRender ? Constants.BlockChangeFlags.FORCE_RE_RENDER : 0)
                | (notifyObservers ? 0 : Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE)
                | (this.neighborDrops ? 0 : Constants.BlockChangeFlags.NEIGHBOR_DROPS)
                | (this.blockMoving ? Constants.BlockChangeFlags.BLOCK_MOVING : 0)
                | (this.performBlockPhysics ? 0 : Constants.BlockChangeFlags.PHYSICS_MASK)
                | (this.lighting ? 0 : Constants.BlockChangeFlags.LIGHTING_UPDATES)
                | (this.pathfindingUpdates ? 0 : Constants.BlockChangeFlags.PATHFINDING_UPDATES);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);

    }

    @Override
    public SpongeBlockChangeFlag withLightingUpdates(final boolean lighting) {
        if (this.lighting == lighting) {
            return this;
        }
        final int maskedFlag =
            (this.updateNeighbors ? Constants.BlockChangeFlags.BLOCK_UPDATED : 0)
                | (this.notifyClients ? Constants.BlockChangeFlags.NOTIFY_CLIENTS : 0)
                | (this.ignoreRender ? Constants.BlockChangeFlags.IGNORE_RENDER : 0)
                | (this.forceReRender ? Constants.BlockChangeFlags.FORCE_RE_RENDER : 0)
                | (this.updateNeighborShapes ? 0 : Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE)
                | (this.neighborDrops ? 0 : Constants.BlockChangeFlags.NEIGHBOR_DROPS)
                | (this.blockMoving ? Constants.BlockChangeFlags.BLOCK_MOVING : 0)
                | (this.performBlockPhysics ? 0 : Constants.BlockChangeFlags.PHYSICS_MASK)
                | (lighting ? 0 : Constants.BlockChangeFlags.LIGHTING_UPDATES)
                | (this.pathfindingUpdates ? 0 : Constants.BlockChangeFlags.PATHFINDING_UPDATES);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);
    }

    @Override
    public SpongeBlockChangeFlag withPathfindingUpdates(final boolean pathfindingUpdates) {
        if (this.pathfindingUpdates == pathfindingUpdates) {
            return this;
        }
        final int maskedFlag =
            (this.updateNeighbors ? Constants.BlockChangeFlags.BLOCK_UPDATED : 0)
                | (this.notifyClients ? Constants.BlockChangeFlags.NOTIFY_CLIENTS : 0)
                | (this.ignoreRender ? Constants.BlockChangeFlags.IGNORE_RENDER : 0)
                | (this.forceReRender ? Constants.BlockChangeFlags.FORCE_RE_RENDER : 0)
                | (this.updateNeighborShapes ? 0 : Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE)
                | (this.neighborDrops ? 0 : Constants.BlockChangeFlags.NEIGHBOR_DROPS)
                | (this.blockMoving ? Constants.BlockChangeFlags.BLOCK_MOVING : 0)
                | (this.performBlockPhysics ? 0 : Constants.BlockChangeFlags.PHYSICS_MASK)
                | (this.lighting ? 0 : Constants.BlockChangeFlags.LIGHTING_UPDATES)
                | (pathfindingUpdates ? 0 : Constants.BlockChangeFlags.PATHFINDING_UPDATES);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);
    }

    @Override
    public SpongeBlockChangeFlag inverse() {
        final int maskedFlag =
            (this.updateNeighbors ? 0 : Constants.BlockChangeFlags.BLOCK_UPDATED)
                | (this.notifyClients ? 0 : Constants.BlockChangeFlags.NOTIFY_CLIENTS)
                | (this.ignoreRender ? 0 : Constants.BlockChangeFlags.IGNORE_RENDER)
                | (this.forceReRender ? 0 : Constants.BlockChangeFlags.FORCE_RE_RENDER)
                | (this.updateNeighborShapes ? Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE : 0)
                | (this.neighborDrops ? Constants.BlockChangeFlags.NEIGHBOR_DROPS : 0)
                | (this.blockMoving ? 0 : Constants.BlockChangeFlags.BLOCK_MOVING)
                | (this.performBlockPhysics ? Constants.BlockChangeFlags.PHYSICS_MASK : 0)
                | (this.lighting ? Constants.BlockChangeFlags.LIGHTING_UPDATES : 0)
                | (this.pathfindingUpdates ? Constants.BlockChangeFlags.PATHFINDING_UPDATES : 0);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);
    }

    @Override
    public SpongeBlockChangeFlag andFlag(final BlockChangeFlag flag) {
        final SpongeBlockChangeFlag o = (SpongeBlockChangeFlag) flag;
        final int maskedFlag =
            (this.updateNeighbors || o.updateNeighbors ? Constants.BlockChangeFlags.BLOCK_UPDATED : 0)
                | (this.notifyClients || o.notifyClients ? Constants.BlockChangeFlags.NOTIFY_CLIENTS : 0)
                | (this.ignoreRender || o.ignoreRender ? Constants.BlockChangeFlags.IGNORE_RENDER : 0)
                | (this.forceReRender || o.forceReRender ? Constants.BlockChangeFlags.FORCE_RE_RENDER : 0)
                | (this.updateNeighborShapes || o.updateNeighborShapes ? 0 : Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE)
                | (this.neighborDrops || o.neighborDrops ? 0 : Constants.BlockChangeFlags.NEIGHBOR_DROPS)
                | (this.blockMoving || o.blockMoving ? Constants.BlockChangeFlags.BLOCK_MOVING : 0)
                | (this.performBlockPhysics || o.performBlockPhysics ? 0 : Constants.BlockChangeFlags.PHYSICS_MASK)
                | (this.lighting || o.lighting ? 0 : Constants.BlockChangeFlags.LIGHTING_UPDATES)
                | (this.pathfindingUpdates || o.pathfindingUpdates ? 0 : Constants.BlockChangeFlags.PATHFINDING_UPDATES);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);
    }

    @Override
    public SpongeBlockChangeFlag andNotFlag(final BlockChangeFlag flag) {
        final SpongeBlockChangeFlag o = (SpongeBlockChangeFlag) flag;
        final int maskedFlag =
            (this.updateNeighbors && !o.updateNeighbors ? Constants.BlockChangeFlags.BLOCK_UPDATED : 0)
                | (this.notifyClients && !o.notifyClients ? Constants.BlockChangeFlags.NOTIFY_CLIENTS : 0)
                | (this.ignoreRender && !o.ignoreRender ? Constants.BlockChangeFlags.IGNORE_RENDER : 0)
                | (this.forceReRender && !o.forceReRender ? Constants.BlockChangeFlags.FORCE_RE_RENDER : 0)
                | (this.updateNeighborShapes && !o.updateNeighborShapes ? 0 : Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE)
                | (this.neighborDrops && !o.neighborDrops ? Constants.BlockChangeFlags.NEIGHBOR_DROPS : 0)
                | (this.blockMoving && !o.blockMoving ? Constants.BlockChangeFlags.BLOCK_MOVING : 0)
                | (this.performBlockPhysics && !o.performBlockPhysics ? 0 : Constants.BlockChangeFlags.PHYSICS_MASK)
                | (this.lighting && !o.lighting ? 0 : Constants.BlockChangeFlags.LIGHTING_UPDATES)
                | (this.pathfindingUpdates && !o.pathfindingUpdates ? 0 : Constants.BlockChangeFlags.PATHFINDING_UPDATES);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);
    }

    @Override
    public BlockChangeFlag withNeighborDropsAllowed(boolean dropsAllowed) {
        if (this.neighborDrops == dropsAllowed) {
            return this;
        }
        final int maskedFlag =
            (this.updateNeighbors ? Constants.BlockChangeFlags.BLOCK_UPDATED : 0)
                | (this.notifyClients ? Constants.BlockChangeFlags.NOTIFY_CLIENTS : 0)
                | (this.ignoreRender ? Constants.BlockChangeFlags.IGNORE_RENDER : 0)
                | (this.forceReRender ? Constants.BlockChangeFlags.FORCE_RE_RENDER : 0)
                | (this.updateNeighborShapes ? 0 : Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE)
                | (dropsAllowed ? 0 : Constants.BlockChangeFlags.NEIGHBOR_DROPS)
                | (this.blockMoving ? Constants.BlockChangeFlags.BLOCK_MOVING : 0)
                | (this.performBlockPhysics ? 0 : Constants.BlockChangeFlags.PHYSICS_MASK)
                | (this.lighting ? 0 : Constants.BlockChangeFlags.LIGHTING_UPDATES)
                | (this.pathfindingUpdates ? 0 : Constants.BlockChangeFlags.PATHFINDING_UPDATES);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);
    }

    @Override
    public BlockChangeFlag withBlocksMoving(boolean moving) {
        if (this.blockMoving == moving) {
            return this;
        }
        final int maskedFlag =
            (this.updateNeighbors ? Constants.BlockChangeFlags.BLOCK_UPDATED : 0)
                | (this.notifyClients ? Constants.BlockChangeFlags.NOTIFY_CLIENTS : 0)
                | (this.ignoreRender ? Constants.BlockChangeFlags.IGNORE_RENDER : 0)
                | (this.forceReRender ? Constants.BlockChangeFlags.FORCE_RE_RENDER : 0)
                | (this.updateNeighborShapes ? 0 : Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE)
                | (this.neighborDrops ? 0 : Constants.BlockChangeFlags.NEIGHBOR_DROPS)
                | (moving ? Constants.BlockChangeFlags.BLOCK_MOVING : 0)
                | (this.performBlockPhysics ? 0 : Constants.BlockChangeFlags.PHYSICS_MASK)
                | (this.lighting ? 0 : Constants.BlockChangeFlags.LIGHTING_UPDATES)
                | (this.pathfindingUpdates ? 0 : Constants.BlockChangeFlags.PATHFINDING_UPDATES);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);
    }

    @Override
    public BlockChangeFlag withIgnoreRender(boolean ignoreRender) {
        if (this.ignoreRender == ignoreRender) {
            return this;
        }
        final int maskedFlag =
            (this.updateNeighbors ? Constants.BlockChangeFlags.BLOCK_UPDATED : 0)
                | (this.notifyClients ? Constants.BlockChangeFlags.NOTIFY_CLIENTS : 0)
                | (ignoreRender ? Constants.BlockChangeFlags.IGNORE_RENDER : 0)
                | (this.forceReRender ? Constants.BlockChangeFlags.FORCE_RE_RENDER : 0)
                | (this.updateNeighborShapes ? 0 : Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE)
                | (this.neighborDrops ? 0 : Constants.BlockChangeFlags.NEIGHBOR_DROPS)
                | (this.blockMoving ? Constants.BlockChangeFlags.BLOCK_MOVING : 0)
                | (this.performBlockPhysics ? 0 : Constants.BlockChangeFlags.PHYSICS_MASK)
                | (this.lighting ? 0 : Constants.BlockChangeFlags.LIGHTING_UPDATES)
                | (this.pathfindingUpdates ? 0 : Constants.BlockChangeFlags.PATHFINDING_UPDATES);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);
    }

    @Override
    public BlockChangeFlag withForcedReRender(boolean forcedReRender) {
        if (this.forceReRender == ignoreRender) {
            return this;
        }
        final int maskedFlag =
            (this.updateNeighbors ? Constants.BlockChangeFlags.BLOCK_UPDATED : 0)
                | (this.notifyClients ? Constants.BlockChangeFlags.NOTIFY_CLIENTS : 0)
                | (this.ignoreRender ? Constants.BlockChangeFlags.IGNORE_RENDER : 0)
                | (forcedReRender ? Constants.BlockChangeFlags.FORCE_RE_RENDER : 0)
                | (this.updateNeighborShapes ? 0 : Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE)
                | (this.neighborDrops ? 0 : Constants.BlockChangeFlags.NEIGHBOR_DROPS)
                | (this.blockMoving ? Constants.BlockChangeFlags.BLOCK_MOVING : 0)
                | (this.performBlockPhysics ? 0 : Constants.BlockChangeFlags.PHYSICS_MASK)
                | (this.lighting ? 0 : Constants.BlockChangeFlags.LIGHTING_UPDATES)
                | (this.pathfindingUpdates ? 0 : Constants.BlockChangeFlags.PATHFINDING_UPDATES);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);
    }

    @Override
    public BlockChangeFlag withStructurePlacement(boolean structurePlacement) {
        if (this.structurePlacement == structurePlacement) {
            return this;
        }
        final int maskedFlag =
            (this.updateNeighbors ? Constants.BlockChangeFlags.BLOCK_UPDATED : 0)
            | (this.notifyClients ? Constants.BlockChangeFlags.NOTIFY_CLIENTS : 0)
            | (this.ignoreRender ? Constants.BlockChangeFlags.IGNORE_RENDER : 0)
            | (this.forceReRender ? Constants.BlockChangeFlags.FORCE_RE_RENDER : 0)
            | (this.updateNeighborShapes ? 0 : Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE)
            | (this.neighborDrops ? 0 : Constants.BlockChangeFlags.NEIGHBOR_DROPS)
            | (this.blockMoving ? Constants.BlockChangeFlags.BLOCK_MOVING : 0)
            | (this.performBlockPhysics ? 0 : Constants.BlockChangeFlags.PHYSICS_MASK)
            | (this.lighting ? 0 : Constants.BlockChangeFlags.LIGHTING_UPDATES)
            | (this.pathfindingUpdates ? 0 : Constants.BlockChangeFlags.PATHFINDING_UPDATES)
            | (structurePlacement ? Constants.BlockChangeFlags.STRUCTURE_PLACEMENT : 0);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);
    }

    public int getRawFlag() {
        return this.rawFlag;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SpongeBlockChangeFlag.class.getSimpleName() + "[", "]")
                .add("rawFlag=" + this.rawFlag)
                .add("notifyNeighbors=" + this.updateNeighbors)
                .add("notifyClients=" + this.notifyClients)
                .add("performBlockPhysics=" + this.performBlockPhysics)
                .add("updateNeighboringShapes=" + this.updateNeighborShapes)
                .add("ignoreRender=" + this.ignoreRender)
                .add("forceReRender=" + this.forceReRender)
                .toString();
    }

    public SpongeBlockChangeFlag asNestedNeighborUpdates() {
        final int maskedFlag = (this.notifyClients ? Constants.BlockChangeFlags.NOTIFY_CLIENTS : 0)
                | (this.ignoreRender ? Constants.BlockChangeFlags.IGNORE_RENDER : 0)
                | (this.forceReRender ? Constants.BlockChangeFlags.FORCE_RE_RENDER : 0)
                | (this.updateNeighborShapes ? 0 : Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE)
                | (this.blockMoving ? Constants.BlockChangeFlags.BLOCK_MOVING : 0)
                | (this.performBlockPhysics ? 0 : Constants.BlockChangeFlags.PHYSICS_MASK)
                | (this.lighting ? 0 : Constants.BlockChangeFlags.LIGHTING_UPDATES)
                | (this.pathfindingUpdates ? 0 : Constants.BlockChangeFlags.PATHFINDING_UPDATES);
        return BlockChangeFlagManager.fromNativeInt(maskedFlag);
    }
}
