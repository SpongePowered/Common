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
package org.spongepowered.common.mixin.tracking.world;

import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.phase.TrackingPhases;
import org.spongepowered.common.event.tracking.phase.WorldPhase;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.world.IMixinWorldInfo;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import org.spongepowered.common.util.SpongeHooks;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

@Mixin(value = net.minecraft.world.chunk.Chunk.class, priority = 1111)
public abstract class MixinChunk_Tracker implements Chunk, IMixinChunk {

    private static final int NUM_XZ_BITS = 4;
    private static final int NUM_SHORT_Y_BITS = 8;
    private static final int NUM_INT_Y_BITS = 24;
    private static final int Y_SHIFT = NUM_XZ_BITS;
    private static final int Z_SHORT_SHIFT = Y_SHIFT + NUM_SHORT_Y_BITS;
    private static final int Z_INT_SHIFT = Y_SHIFT + NUM_INT_Y_BITS;
    private static final short XZ_MASK = 0xF;
    private static final short Y_SHORT_MASK = 0xFF;
    private static final int Y_INT_MASK = 0xFFFFFF;

    @Shadow @Final private World worldObj;
    @Shadow @Final public int xPosition;
    @Shadow @Final public int zPosition;
    @Shadow @Final private ExtendedBlockStorage[] storageArrays;
    @Shadow @Final private int[] precipitationHeightMap;
    @Shadow @Final private int[] heightMap;
    @Shadow private boolean isModified;

    public Map<Integer, PlayerTracker> trackedIntBlockPositions = Maps.newHashMap();
    public Map<Short, PlayerTracker> trackedShortBlockPositions = Maps.newHashMap();

    @Override
    public void addTrackedBlockPosition(Block block, BlockPos pos, User user, PlayerTracker.Type trackerType) {
        if (this.worldObj.isRemote) {
            return;
        } else {
            IMixinWorldServer spongeWorld = (IMixinWorldServer) this.worldObj;
            if (spongeWorld.getCauseTracker().getStack().peekState().ignoresBlockTracking()) {
                // Don't track chunk gen
                return;
            }
        }

        if (!SpongeHooks.getActiveConfig(this.worldObj).getConfig().getBlockTracking().getBlockBlacklist().contains(((BlockType)block).getId())) {
            SpongeHooks.logBlockTrack(this.worldObj, block, pos, user, true);
        } else {
            SpongeHooks.logBlockTrack(this.worldObj, block, pos, user, false);
        }

        final IMixinWorldInfo worldInfo = (IMixinWorldInfo) this.worldObj.getWorldInfo();
        final int indexForUniqueId = worldInfo.getIndexForUniqueId(user.getUniqueId());
        if (pos.getY() <= 255) {
            short blockPos = blockPosToShort(pos);
            final PlayerTracker playerTracker = this.trackedShortBlockPositions.get(blockPos);
            if (playerTracker != null) {
                if (trackerType == PlayerTracker.Type.OWNER) {
                    playerTracker.ownerIndex = indexForUniqueId;
                    playerTracker.notifierIndex = indexForUniqueId;
                } else {
                    playerTracker.notifierIndex = indexForUniqueId;
                }
            } else {
                this.trackedShortBlockPositions.put(blockPos, new PlayerTracker(indexForUniqueId, trackerType));
            }
        } else {
            int blockPos = blockPosToInt(pos);
            final PlayerTracker playerTracker = this.trackedIntBlockPositions.get(blockPos);
            if (playerTracker != null) {
                if (trackerType == PlayerTracker.Type.OWNER) {
                    playerTracker.ownerIndex = indexForUniqueId;
                } else {
                    playerTracker.notifierIndex = indexForUniqueId;
                }
            } else {
                this.trackedIntBlockPositions.put(blockPos, new PlayerTracker(indexForUniqueId, trackerType));
            }
        }
    }

    @Override
    public Map<Integer, PlayerTracker> getTrackedIntPlayerPositions() {
        return this.trackedIntBlockPositions;
    }

    @Override
    public Map<Short, PlayerTracker> getTrackedShortPlayerPositions() {
        return this.trackedShortBlockPositions;
    }

    @Override
    public Optional<User> getBlockOwner(BlockPos pos) {
        if (this.trackedIntBlockPositions.get(blockPosToInt(pos)) != null) {
            PlayerTracker tracker = this.trackedIntBlockPositions.get(blockPosToInt(pos));
            Optional<UUID> uuid = (((IMixinWorldInfo) this.worldObj.getWorldInfo()).getUniqueIdForIndex(tracker.ownerIndex));
            if (uuid.isPresent()) {
                return getUserFromId(uuid.get());
            }
        } else if (this.trackedShortBlockPositions.get(blockPosToShort(pos)) != null) {
            PlayerTracker tracker = this.trackedShortBlockPositions.get(blockPosToShort(pos));
            Optional<UUID> uuid = (((IMixinWorldInfo) this.worldObj.getWorldInfo()).getUniqueIdForIndex(tracker.ownerIndex));
            if (uuid.isPresent()) {
                return getUserFromId(uuid.get());
            }
        }

        return Optional.empty();
    }

    private Optional<User> getUserFromId(UUID uuid) {
        // get player if online
        EntityPlayer player = this.worldObj.getPlayerEntityByUUID(uuid);
        if (player != null) {
            return Optional.of((User) player);
        }
        // player is not online, get user from storage if one exists
        return SpongeImpl.getGame().getServiceManager().provide(UserStorageService.class).get().get(uuid);
    }

    @Override
    public Optional<User> getBlockNotifier(BlockPos pos) {
        if (this.trackedIntBlockPositions.get(blockPosToInt(pos)) != null) {
            PlayerTracker tracker = this.trackedIntBlockPositions.get(blockPosToInt(pos));
            Optional<UUID> uuid = (((IMixinWorldInfo) this.worldObj.getWorldInfo()).getUniqueIdForIndex(tracker.notifierIndex));
            if (uuid.isPresent()) {
                return getUserFromId(uuid.get());
            }
        } else if (this.trackedShortBlockPositions.get(blockPosToShort(pos)) != null) {
            PlayerTracker tracker = this.trackedShortBlockPositions.get(blockPosToShort(pos));
            Optional<UUID> uuid = (((IMixinWorldInfo) this.worldObj.getWorldInfo()).getUniqueIdForIndex(tracker.notifierIndex));
            if (uuid.isPresent()) {
                return getUserFromId(uuid.get());
            }
        }

        return Optional.empty();
    }

    // Special setter used by API
    @Override
    public void setBlockNotifier(BlockPos pos, @Nullable UUID uuid) {
        if (pos.getY() <= 255) {
            short blockPos = blockPosToShort(pos);
            if (this.trackedShortBlockPositions.get(blockPos) != null) {
                this.trackedShortBlockPositions.get(blockPos).notifierIndex = uuid == null ? -1 :
                                                                              ((IMixinWorldInfo) this.worldObj.getWorldInfo()).getIndexForUniqueId(uuid);
            } else {
                this.trackedShortBlockPositions.put(blockPos,
                        new PlayerTracker(uuid == null ? -1 : ((IMixinWorldInfo) this.worldObj.getWorldInfo()).getIndexForUniqueId(uuid),
                                PlayerTracker.Type.NOTIFIER));
            }
        } else {
            int blockPos = blockPosToInt(pos);
            if (this.trackedIntBlockPositions.get(blockPos) != null) {
                this.trackedIntBlockPositions.get(blockPos).notifierIndex = uuid == null ? -1 :
                                                                            ((IMixinWorldInfo) this.worldObj.getWorldInfo()).getIndexForUniqueId(uuid);
            } else {
                this.trackedIntBlockPositions.put(blockPos,
                        new PlayerTracker(uuid == null ? -1 : ((IMixinWorldInfo) this.worldObj.getWorldInfo()).getIndexForUniqueId(uuid),
                                PlayerTracker.Type.NOTIFIER));
            }
        }
    }

    // Special setter used by API
    @Override
    public void setBlockCreator(BlockPos pos, @Nullable UUID uuid) {
        if (pos.getY() <= 255) {
            short blockPos = blockPosToShort(pos);
            if (this.trackedShortBlockPositions.get(blockPos) != null) {
                this.trackedShortBlockPositions.get(blockPos).ownerIndex = uuid == null ? -1 : ((IMixinWorldInfo) this.worldObj.getWorldInfo())
                        .getIndexForUniqueId(uuid);
            } else {
                this.trackedShortBlockPositions.put(blockPos, new PlayerTracker(uuid == null ? -1 : ((IMixinWorldInfo) this.worldObj.getWorldInfo())
                        .getIndexForUniqueId(uuid), PlayerTracker.Type.OWNER));
            }
        } else {
            int blockPos = blockPosToInt(pos);
            if (this.trackedIntBlockPositions.get(blockPos) != null) {
                this.trackedIntBlockPositions.get(blockPos).ownerIndex = uuid == null ? -1 : ((IMixinWorldInfo) this.worldObj.getWorldInfo())
                        .getIndexForUniqueId(uuid);
            } else {
                this.trackedIntBlockPositions.put(blockPos, new PlayerTracker(uuid == null ? -1 : ((IMixinWorldInfo) this.worldObj.getWorldInfo())
                        .getIndexForUniqueId(uuid), PlayerTracker.Type.OWNER));
            }
        }
    }

    @Override
    public void setTrackedIntPlayerPositions(Map<Integer, PlayerTracker> trackedPositions) {
        this.trackedIntBlockPositions = trackedPositions;
    }

    @Override
    public void setTrackedShortPlayerPositions(Map<Short, PlayerTracker> trackedPositions) {
        this.trackedShortBlockPositions = trackedPositions;
    }

    /**
     * Modifies bits in an integer.
     *
     * @param num Integer to modify
     * @param data Bits of data to add
     * @param which Index of nibble to start at
     * @param bitsToReplace The number of bits to replace starting from nibble index
     * @return The modified integer
     */
    private int setNibble(int num, int data, int which, int bitsToReplace) {
        return (num & ~(bitsToReplace << (which * 4)) | (data << (which * 4)));
    }

    @Inject(method = "onChunkLoad", at = @At("HEAD"))
    private void startChunkLoad(CallbackInfo callbackInfo) {
        if (!this.worldObj.isRemote) {
            ((IMixinWorldServer) this.worldObj).getCauseTracker().switchToPhase(TrackingPhases.WORLD, WorldPhase.State.CHUNK_LOADING, PhaseContext.start()
                .add(NamedCause.source(this))
                .addCaptures()
                .complete());
        }
    }

    @Inject(method = "onChunkLoad", at = @At("RETURN"))
    private void endChunkLoad(CallbackInfo callbackInfo) {
        if (!this.worldObj.isRemote) {
            ((IMixinWorldServer) this.worldObj).getCauseTracker().completePhase();
        }
    }

    /**
     * Serialize this BlockPos into a short value
     */
    private short blockPosToShort(BlockPos pos) {
        short serialized = (short) setNibble(0, pos.getX() & XZ_MASK, 0, NUM_XZ_BITS);
        serialized = (short) setNibble(serialized, pos.getY() & Y_SHORT_MASK, 1, NUM_SHORT_Y_BITS);
        serialized = (short) setNibble(serialized, pos.getZ() & XZ_MASK, 3, NUM_XZ_BITS);
        return serialized;
    }

    /**
     * Create a BlockPos from a serialized chunk position
     */
    private BlockPos blockPosFromShort(short serialized) {
        int x = this.xPosition * 16 + (serialized & XZ_MASK);
        int y = (serialized >> Y_SHIFT) & Y_SHORT_MASK;
        int z = this.zPosition * 16 + ((serialized >> Z_SHORT_SHIFT) & XZ_MASK);
        return new BlockPos(x, y, z);
    }

    /**
     * Serialize this BlockPos into an int value
     */
    private int blockPosToInt(BlockPos pos) {
        int serialized = setNibble(0, pos.getX() & XZ_MASK, 0, NUM_XZ_BITS);
        serialized = setNibble(serialized, pos.getY() & Y_INT_MASK, 1, NUM_INT_Y_BITS);
        serialized = setNibble(serialized, pos.getZ() & XZ_MASK, 7, NUM_XZ_BITS);
        return serialized;
    }

    /**
     * Create a BlockPos from a serialized chunk position
     */
    private BlockPos blockPosFromInt(int serialized) {
        int x = this.xPosition * 16 + (serialized & XZ_MASK);
        int y = (serialized >> Y_SHIFT) & Y_INT_MASK;
        int z = this.zPosition * 16 + ((serialized >> Z_INT_SHIFT) & XZ_MASK);
        return new BlockPos(x, y, z);
    }

}
