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

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.world.SpongeBlockChangeFlag;

public final class BlockChangeFlagManager {

    private final Int2ObjectMap<SpongeBlockChangeFlag> maskedFlags = new Int2ObjectLinkedOpenHashMap<>(1 << 9);
    private static final BlockChangeFlagManager INSTANCE = new BlockChangeFlagManager();
    private static final SpongeBlockChangeFlag PHYSICS_OBSERVER = new SpongeBlockChangeFlag(Constants.BlockChangeFlags.NOTIFY_CLIENTS);
    private static final SpongeBlockChangeFlag DEFAULT = new SpongeBlockChangeFlag(Constants.BlockChangeFlags.DEFAULT);

    public static BlockChangeFlagManager getInstance() {
        return BlockChangeFlagManager.INSTANCE;
    }

    public static SpongeBlockChangeFlag fromNativeInt(final int flag) {
        if (flag == Constants.BlockChangeFlags.DEFAULT) {
            return BlockChangeFlagManager.DEFAULT;
        }
        if (flag == Constants.BlockChangeFlags.NOTIFY_CLIENTS) {
            return BlockChangeFlagManager.PHYSICS_OBSERVER;
        }
        final BlockChangeFlagManager instance = BlockChangeFlagManager.getInstance();
        return instance.maskedFlags.computeIfAbsent(flag, SpongeBlockChangeFlag::new);
    }

    public static SpongeBlockChangeFlag andNotifyClients(final BlockChangeFlag flag) {
        final int rawFlag = ((SpongeBlockChangeFlag) flag).rawFlag();
        if ((rawFlag & Constants.BlockChangeFlags.NOTIFY_CLIENTS) != 0){
            return (SpongeBlockChangeFlag) flag; // We don't need to rerun the flag
        }
        return BlockChangeFlagManager.fromNativeInt(rawFlag & ~Constants.BlockChangeFlags.NOTIFY_CLIENTS);
    }

    private BlockChangeFlagManager() {
        // basically run through all possible permutations to our custom flags
        for (int i = 0; i < (1 << 9 + 1); i++) {
            final SpongeBlockChangeFlag flag = new SpongeBlockChangeFlag(i);
            this.maskedFlags.put(flag.rawFlag(), flag);
        }
    }

    public static final class Factory implements BlockChangeFlag.Factory {

        private @Nullable BlockChangeFlag none;

        @Override
        public BlockChangeFlag none() {
            if (this.none == null) {
                this.none = BlockChangeFlagManager.fromNativeInt(Constants.BlockChangeFlags.NONE);
            }
            return this.none;
        }

        public Factory() {}
    }
}
