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
package org.spongepowered.common.event.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.world.SpongeBlockChangeFlag;

class BlockChangeFlagManagerTest {

    @BeforeAll
    public static void setupBlockChangeFlagManager() {
        BlockChangeFlagManager.fromNativeInt(0);
    }

    private static SpongeBlockChangeFlag createDefaultFlag() {
        return BlockChangeFlagManager.fromNativeInt(Constants.BlockChangeFlags.DEFAULT);
    }

    @Test
    void verifyDefaultFlag() {
        final SpongeBlockChangeFlag flag = BlockChangeFlagManagerTest.createDefaultFlag();
        assertTrue(flag.updateNeighbors()); // 1
        assertTrue(flag.notifyClients()); // 2
        assertFalse(flag.ignoreRender()); // 4
        assertFalse(flag.forceClientRerender()); // 8
        assertTrue(flag.updateNeighboringShapes()); // 16
        assertTrue(flag.neighborDropsAllowed()); // 32
        assertFalse(flag.movingBlocks()); // 64
        assertTrue(flag.updateLighting()); // 128
        assertTrue(flag.performBlockPhysics()); // Sponge added
        assertTrue(flag.notifyPathfinding()); // Sponge added
        assertTrue(flag.performBlockDestruction()); // Sponge added
    }

    @Test
    void verifyNotifyClientFlagChanges() {
        final SpongeBlockChangeFlag flag = BlockChangeFlagManagerTest.createDefaultFlag();
        final SpongeBlockChangeFlag updated = flag.withNotifyClients(false);
        assertTrue(updated.updateNeighbors()); // 1
        assertFalse(updated.notifyClients(), "Flipping withNotifyClients false is not being changed"); // 2
        assertFalse(updated.ignoreRender()); // 4
        assertFalse(updated.forceClientRerender()); // 8
        assertTrue(updated.updateNeighboringShapes()); // 16
        assertTrue(updated.neighborDropsAllowed()); // 32
        assertFalse(updated.movingBlocks()); // 64
        assertTrue(updated.updateLighting()); // 128
        assertTrue(updated.performBlockPhysics()); // Sponge added
        assertTrue(updated.notifyPathfinding()); // Sponge added
        assertTrue(updated.performBlockDestruction()); // Sponge added
    }

    @Test
    void verifyNoneFlag() {
        final SpongeBlockChangeFlag none = BlockChangeFlagManager.fromNativeInt(Constants.BlockChangeFlags.NONE);
        assertFalse(none.updateNeighbors());
        assertFalse(none.notifyClients());
        assertFalse(none.ignoreRender());
        assertFalse(none.forceClientRerender());
        assertFalse(none.updateNeighboringShapes());
        assertFalse(none.neighborDropsAllowed());
        assertFalse(none.movingBlocks());
        assertFalse(none.updateLighting());
        assertFalse(none.performBlockPhysics());
        assertFalse(none.notifyPathfinding());
        assertFalse(none.performBlockDestruction());
    }

    @Test
    void verifyRestoration() {
        final SpongeBlockChangeFlag restore = BlockChangeFlagManager.fromNativeInt(Constants.BlockChangeFlags.FORCED_RESTORE);
        assertFalse(restore.updateNeighbors());
        assertTrue(restore.notifyClients());
        assertFalse(restore.ignoreRender());
        assertTrue(restore.forceClientRerender());
        assertFalse(restore.updateNeighboringShapes());
        assertFalse(restore.neighborDropsAllowed());
        assertFalse(restore.movingBlocks());
        assertTrue(restore.updateLighting());
        assertFalse(restore.performBlockPhysics());
        assertTrue(restore.notifyPathfinding());
        assertFalse(restore.performBlockDestruction());
    }

    @Test
    void verifyEmpty() {
        final SpongeBlockChangeFlag empty = BlockChangeFlagManager.fromNativeInt(0);
        assertFalse(empty.updateNeighbors());
        assertFalse(empty.notifyClients());
        assertFalse(empty.ignoreRender());
        assertFalse(empty.forceClientRerender());
        assertTrue(empty.updateNeighboringShapes());
        assertTrue(empty.neighborDropsAllowed());
        assertFalse(empty.movingBlocks());
        assertTrue(empty.updateLighting());
        assertTrue(empty.performBlockPhysics());
        assertTrue(empty.notifyPathfinding());
        assertTrue(empty.performBlockDestruction());
    }

    @Test
    void verifyEmptyToNotifyNeighbors() {
        final SpongeBlockChangeFlag empty = BlockChangeFlagManager.fromNativeInt(0);
        final SpongeBlockChangeFlag neighbors = empty.withUpdateNeighbors(true);
        assertTrue(neighbors.updateNeighbors());
        assertFalse(neighbors.notifyClients());
        assertFalse(neighbors.ignoreRender());
        assertFalse(neighbors.forceClientRerender());
        assertTrue(neighbors.updateNeighboringShapes());
        assertTrue(neighbors.neighborDropsAllowed());
        assertFalse(neighbors.movingBlocks());
        assertTrue(neighbors.updateLighting());
        assertTrue(neighbors.performBlockPhysics());
        assertTrue(neighbors.notifyPathfinding());
        assertTrue(neighbors.performBlockDestruction());
    }

    @Test
    void verifyEmptyToNotifyClients() {
        final SpongeBlockChangeFlag empty = BlockChangeFlagManager.fromNativeInt(0);
        final SpongeBlockChangeFlag clients = empty.withNotifyClients(true);
        assertFalse(clients.updateNeighbors());
        assertTrue(clients.notifyClients());
        assertFalse(clients.ignoreRender());
        assertFalse(clients.forceClientRerender());
        assertTrue(clients.updateNeighboringShapes());
        assertTrue(clients.neighborDropsAllowed());
        assertFalse(clients.movingBlocks());
        assertTrue(clients.updateLighting());
        assertTrue(clients.performBlockPhysics());
        assertTrue(clients.notifyPathfinding());
        assertTrue(clients.performBlockDestruction());
    }

    @Test
    void verifyWithNotifyObservers() {
        final SpongeBlockChangeFlag empty = BlockChangeFlagManager.fromNativeInt(0);
        final SpongeBlockChangeFlag observers = empty.withNotifyObservers(true);
        assertFalse(observers.updateNeighbors());
        assertFalse(observers.notifyClients());
        assertFalse(observers.ignoreRender());
        assertFalse(observers.forceClientRerender());
        assertTrue(observers.updateNeighboringShapes());
        assertTrue(observers.neighborDropsAllowed());
        assertFalse(observers.movingBlocks());
        assertTrue(observers.updateLighting());
        assertTrue(observers.performBlockPhysics());
        assertTrue(observers.notifyPathfinding());
        assertTrue(observers.performBlockDestruction());
    }

    @Test
    void verifyWithNoPhysics() {
        final SpongeBlockChangeFlag empty = BlockChangeFlagManager.fromNativeInt(0);
        final SpongeBlockChangeFlag noPhysics = empty.withPhysics(false);
        assertFalse(noPhysics.updateNeighbors());
        assertFalse(noPhysics.notifyClients());
        assertFalse(noPhysics.ignoreRender());
        assertFalse(noPhysics.forceClientRerender());
        assertTrue(noPhysics.updateNeighboringShapes());
        assertTrue(noPhysics.neighborDropsAllowed());
        assertFalse(noPhysics.movingBlocks());
        assertTrue(noPhysics.updateLighting());
        assertFalse(noPhysics.performBlockPhysics());
        assertTrue(noPhysics.notifyPathfinding());
        assertTrue(noPhysics.performBlockDestruction());
    }

    @Test
    void verifyWithNoLighting() {
        final SpongeBlockChangeFlag empty = BlockChangeFlagManager.fromNativeInt(0);
        final SpongeBlockChangeFlag noLighting = empty.withLightingUpdates(false);
        assertFalse(noLighting.updateNeighbors());
        assertFalse(noLighting.notifyClients());
        assertFalse(noLighting.ignoreRender());
        assertFalse(noLighting.forceClientRerender());
        assertTrue(noLighting.updateNeighboringShapes());
        assertTrue(noLighting.neighborDropsAllowed());
        assertFalse(noLighting.movingBlocks());
        assertFalse(noLighting.updateLighting());
        assertTrue(noLighting.performBlockPhysics());
        assertTrue(noLighting.notifyPathfinding());
        assertTrue(noLighting.performBlockDestruction());
    }

    @Test
    void verifyWithNoPathfinding() {
        final SpongeBlockChangeFlag empty = BlockChangeFlagManager.fromNativeInt(0);
        final SpongeBlockChangeFlag noPathfinding = empty.withPathfindingUpdates(false);
        assertFalse(noPathfinding.updateNeighbors());
        assertFalse(noPathfinding.notifyClients());
        assertFalse(noPathfinding.ignoreRender());
        assertFalse(noPathfinding.forceClientRerender());
        assertTrue(noPathfinding.updateNeighboringShapes());
        assertTrue(noPathfinding.neighborDropsAllowed());
        assertFalse(noPathfinding.movingBlocks());
        assertTrue(noPathfinding.updateLighting());
        assertTrue(noPathfinding.performBlockPhysics());
        assertFalse(noPathfinding.notifyPathfinding());
        assertTrue(noPathfinding.performBlockDestruction());
    }

    @Test
    void verifyWithPerformBlockDestruction() {
        final SpongeBlockChangeFlag empty = BlockChangeFlagManager.fromNativeInt(0);
        final SpongeBlockChangeFlag noPathfinding = empty.withPerformBlockDestruction(false);
        assertFalse(noPathfinding.updateNeighbors());
        assertFalse(noPathfinding.notifyClients());
        assertFalse(noPathfinding.ignoreRender());
        assertFalse(noPathfinding.forceClientRerender());
        assertTrue(noPathfinding.updateNeighboringShapes());
        assertTrue(noPathfinding.neighborDropsAllowed());
        assertFalse(noPathfinding.movingBlocks());
        assertTrue(noPathfinding.updateLighting());
        assertTrue(noPathfinding.performBlockPhysics());
        assertTrue(noPathfinding.notifyPathfinding());
        assertFalse(noPathfinding.performBlockDestruction());
    }

    @Test
    void verifyEmptyToInverse() {
        final SpongeBlockChangeFlag empty = BlockChangeFlagManager.fromNativeInt(0);
        final SpongeBlockChangeFlag inverse = empty.inverse();
        assertTrue(inverse.updateNeighbors());
        assertTrue(inverse.notifyClients());
        assertTrue(inverse.ignoreRender());
        assertTrue(inverse.forceClientRerender());
        assertFalse(inverse.updateNeighboringShapes());
        assertFalse(inverse.neighborDropsAllowed());
        assertTrue(inverse.movingBlocks());
        assertFalse(inverse.updateLighting());
        assertFalse(inverse.performBlockPhysics());
        assertFalse(inverse.notifyPathfinding());
        assertFalse(inverse.performBlockDestruction());
    }

    /**
     * This is a test that Mojang is doing a bitwise and'ing of a flag
     * with -34 if the flag if flag.updateNeighboringShapes() is true
     * Basically put:
     *  <pre>{@code if ((var3 & 16) == 0 && var4 > 0) {
     *     int var9 = var3 & -34; <---- this code right here is where we want to verify
     *     var7.updateIndirectNeighbourShapes(this, var1, var9, var4 - 1);
     *     var2.updateNeighbourShapes(this, var1, var9, var4 - 1);
     *     var2.updateIndirectNeighbourShapes(this, var1, var9, var4 - 1);
     * }}</pre>
     * This is used by {@link org.spongepowered.common.event.tracking.context.transaction.effect.UpdateConnectingBlocksEffect}
     * with {@link BlockChangeFlag#updateNeighboringShapes()}
     */
    @Test
    void verifyNestedNeighborPhysics() {
        final SpongeBlockChangeFlag flag = BlockChangeFlagManagerTest.createDefaultFlag();
        assertTrue(flag.updateNeighboringShapes()); // 16
        assertTrue(flag.rawFlag() != 0);
        assertEquals((flag.rawFlag() & 32), 0);
        assertTrue(flag.neighborDropsAllowed());
        final SpongeBlockChangeFlag nestedNeighbor = flag.asNestedNeighborUpdates();
        assertFalse(nestedNeighbor.updateNeighbors());
        assertTrue(nestedNeighbor.notifyClients());
        assertFalse(nestedNeighbor.ignoreRender());
        assertFalse(nestedNeighbor.forceClientRerender());
        assertTrue(nestedNeighbor.updateNeighboringShapes());
        assertTrue(nestedNeighbor.neighborDropsAllowed());
        assertFalse(nestedNeighbor.movingBlocks());
        assertTrue(nestedNeighbor.updateLighting());
        assertTrue(nestedNeighbor.performBlockPhysics());
        assertTrue(nestedNeighbor.notifyPathfinding());
        assertTrue(nestedNeighbor.performBlockDestruction());
        assertEquals(3 & -34, nestedNeighbor.rawFlag());

        final int overloadedFlag =
            Constants.BlockChangeFlags.BLOCK_UPDATED
            | Constants.BlockChangeFlags.NOTIFY_CLIENTS
            | Constants.BlockChangeFlags.IGNORE_RENDER
            | Constants.BlockChangeFlags.FORCE_RE_RENDER
            | Constants.BlockChangeFlags.NEIGHBOR_DROPS
            | Constants.BlockChangeFlags.BLOCK_MOVING
            | Constants.BlockChangeFlags.LIGHTING_UPDATES
            | Constants.BlockChangeFlags.PHYSICS_MASK
            | Constants.BlockChangeFlags.PATHFINDING_UPDATES;
        final SpongeBlockChangeFlag otherFlag = BlockChangeFlagManager.fromNativeInt(overloadedFlag);
        assertTrue(otherFlag.updateNeighboringShapes()); // 16
        assertTrue(otherFlag.rawFlag() != 0);
        assertFalse(otherFlag.neighborDropsAllowed());
        final SpongeBlockChangeFlag nestedOther = otherFlag.asNestedNeighborUpdates();
        assertFalse(nestedOther.updateNeighbors()); // 1
        assertTrue(nestedOther.notifyClients());    // 2
        assertTrue(nestedOther.ignoreRender());   // 4
        assertTrue(nestedOther.forceClientRerender()); // 8
        assertTrue(nestedOther.updateNeighboringShapes()); // 16
        assertTrue(nestedOther.neighborDropsAllowed());    // 32
        assertTrue(nestedOther.movingBlocks());
        assertFalse(nestedOther.updateLighting());
        assertFalse(nestedOther.performBlockPhysics());
        assertFalse(nestedOther.notifyPathfinding());
        assertTrue(nestedOther.performBlockDestruction());

        // Finally, verify that with mojang's flag logic, we're still abiding by it.
        assertEquals(overloadedFlag & -34, nestedOther.rawFlag());


    }


}
