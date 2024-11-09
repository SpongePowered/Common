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
package org.spongepowered.common.mixin.tracker.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.bridge.world.TrackedWorldBridge;
import org.spongepowered.common.event.tracking.BlockChangeFlagManager;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.effect.AddBlockLootDropsEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.ExplodeBlockEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.SpawnDestructBlocksEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.WorldBlockChangeCompleteEffect;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.WorldPipeline;
import org.spongepowered.common.util.Constants;

import java.util.function.BiConsumer;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin_Tracker {


    // TODO sounds & particles?

    @Redirect(method = "interactWithBlocks",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private void tracker$onPopResource(final Level $$0, final BlockPos $$1, final ItemStack $$2) {
        // This is built into the SpawnDestructBlocksEffect
        // Block.popResource(this.level, $$3.pos, $$3.stack);
    }


    @Redirect(method = "interactWithBlocks",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onExplosionHit(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;Ljava/util/function/BiConsumer;)V"))
    private void tracker$onExplosionHit(final BlockState instance, final ServerLevel serverLevel,
        final BlockPos blockPos, final Explosion explosion, final BiConsumer biConsumer) {
        if (explosion.getBlockInteraction() == Explosion.BlockInteraction.TRIGGER_BLOCK) {
            instance.onExplosionHit(serverLevel, blockPos, explosion, biConsumer);
            return;
        }
        // this.level.getBlockState($$2).onExplosionHit(this.level, $$2, this, ($$1x, $$2x) -> addOrAppendStack($$1, $$1x, $$2x));

        // TODO addOrAppendStack? ItemStack pre merging
        final PhaseContext<@NonNull ?> context = PhaseTracker.getInstance().getPhaseContext();
        ((TrackedWorldBridge) serverLevel).bridge$startBlockChange(blockPos, Blocks.AIR.defaultBlockState(), 3)
            .ifPresent(builder -> {
                final WorldPipeline build = builder
                    .addEffect(AddBlockLootDropsEffect.getInstance())
                    .addEffect(ExplodeBlockEffect.getInstance())
                    .addEffect(SpawnDestructBlocksEffect.getInstance())
                    .addEffect(WorldBlockChangeCompleteEffect.getInstance())
                    .build();
                build.processEffects(context, instance, Blocks.AIR.defaultBlockState(), blockPos,
                    null,
                    BlockChangeFlagManager.fromNativeInt(3),
                    Constants.World.DEFAULT_BLOCK_CHANGE_LIMIT);
            });
    }


}
