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

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.accessor.world.entity.LivingEntityAccessor;
import org.spongepowered.common.bridge.data.VanishableBridge;
import org.spongepowered.common.bridge.server.level.ServerPlayerBridge;
import org.spongepowered.common.entity.living.human.HumanEntity;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin {

    @Shadow @Final private Entity entity;
    @Shadow @Final @Mutable private Consumer<Packet<?>> broadcast;

    /**
     * @author gabizou
     * @reason Because the packets for *most* all entity updates are handled
     * through this consumer tick, basically all the players tracking the
     * {@link #entity} can be updated within the {@code net.minecraft.world.server.ChunkManager.EntityTracker}
     * which maintains which players are being updated for all "tick" updates. The problem
     * with this is that we don't really care about which updates are sent, but rather
     * whether the update packets are sent at all (ideally so that hack clients thinking
     * they can sniff for invisible entities or players is possible). Likewise, what this
     * involves doing is setting up several safeguards against the common accessors for
     * any and all players tracking our tracked entity by filtering the consumer first,
     * then as a fail safe, the EntityTracker mixin. Meanwhile, all other states are updated
     * just fine.
     *
     * @param serverLevel The world
     * @param entity The entity being tracked
     * @param trackingRange The update frequency
     * @param trackMovementDeltas Whether velocity updates are sent
     * @param broadcaster The consumer (a method handle for EntityTracker#sendToAllTracking)
     * @param ci The callback info
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void impl$wrapConsumer(final ServerLevel serverLevel, final Entity entity, final int trackingRange,
                                   final boolean trackMovementDeltas, final Consumer<Packet<?>> broadcaster,
                                   final BiConsumer<?, ?> filter, final CallbackInfo ci) {
        this.broadcast = (packet)  -> {
            if (this.entity instanceof VanishableBridge bridge) {
                if (!bridge.bridge$vanishState().invisible()) {
                    broadcaster.accept(packet);
                }
            }
        };
    }

    @Inject(method = "removePairing", at = @At("RETURN"))
    private void impl$removeHumanFromPlayerClient(final ServerPlayer viewer, final CallbackInfo ci) {
        if (this.entity instanceof HumanEntity) {
            ((HumanEntity) this.entity).untrackFrom(viewer);
        }
    }

    @Inject(method = "sendDirtyEntityData", at = @At("HEAD"))
    public void impl$sendHumanMetadata(final CallbackInfo ci) {
        if (!(this.entity instanceof final HumanEntity human)) {
            return;
        }
        final Stream<Packet<?>> packets = human.popQueuedPackets(null);
        packets.forEach(this.broadcast);
        // Note that this will further call in ChunkManager_EntityTrackerMixin
        // for any player specific packets to send.
    }

    @ModifyArg(method = "sendDirtyEntityData", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundUpdateAttributesPacket;<init>(ILjava/util/Collection;)V"))
    private Collection<AttributeInstance> impl$injectScaledHealth(final Collection<AttributeInstance> set) {
        if (this.entity instanceof ServerPlayer) {
            if (((ServerPlayerBridge) this.entity).bridge$isHealthScaled()) {
                ((ServerPlayerBridge) this.entity).bridge$injectScaledHealth(set);
            }
        }
        return set;
    }

    @Redirect(method = "sendDirtyEntityData", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData;packDirty()Ljava/util/List;"))
    private List<SynchedEntityData.DataValue<?>> impl$createSpoofedPacket(final SynchedEntityData entityData) {
        if (!(this.entity instanceof ServerPlayerBridge && ((ServerPlayerBridge) this.entity).bridge$isHealthScaled())) {
            return entityData.packDirty();
        }

        final float scaledHealth = ((ServerPlayerBridge) this.entity).bridge$getInternalScaledHealth();
        final Float actualHealth = entityData.get(LivingEntityAccessor.accessor$DATA_HEALTH_ID());
        entityData.set(LivingEntityAccessor.accessor$DATA_HEALTH_ID(), scaledHealth);
        final List<SynchedEntityData.DataValue<?>> packed = entityData.packDirty();
        entityData.set(LivingEntityAccessor.accessor$DATA_HEALTH_ID(), actualHealth);
        return packed;
    }

}
