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
package org.spongepowered.common.mixin.tracker.world.ticks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.bridge.world.ticks.LevelTicksBridge;
import org.spongepowered.common.bridge.world.ticks.ScheduledTickBridge;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.generation.GenerationPhase;

import java.util.List;
import java.util.function.BiConsumer;

@Mixin(LevelTicks.class)
public abstract class LevelTicksMixin_Tracker {

    // @formatter:off
    @Shadow @Final private List<ScheduledTick<?>> alreadyRunThisTick;
    // @formatter:on

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "schedule(Lnet/minecraft/world/ticks/ScheduledTick;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/ticks/LevelChunkTicks;schedule(Lnet/minecraft/world/ticks/ScheduledTick;)V")
    )
    private void tracker$associatePhaseContextWithTickEntry(final LevelChunkTicks instance, final ScheduledTick<?> scheduledTick) {
        PhaseTracker.getInstance().getPhaseContext().associateScheduledTickUpdate(((LevelTicksBridge<?>) this).bridge$level(), scheduledTick);
        instance.schedule(scheduledTick);
    }

    @Redirect(method = "runCollectedTicks",
        at = @At(value = "INVOKE",
            target = "Ljava/util/function/BiConsumer;accept(Ljava/lang/Object;Ljava/lang/Object;)V"
        )
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void tracker$wrapTickConsumer(BiConsumer consumer, Object blockPos, Object ticking) {
        // Technically we can grab the latest ScheduledTick from the accepted list
        final var thisScheduledTick = this.alreadyRunThisTick.get(this.alreadyRunThisTick.size() - 1);
        if (((ScheduledTickBridge) (Object) thisScheduledTick).bridge$isPartOfWorldGeneration()) {
            try (final var context = GenerationPhase.State.DEFERRED_SCHEDULED_UPDATE.createPhaseContext(
                    PhaseTracker.SERVER)
                .source(this)
                .scheduledUpdate((BlockPos) blockPos, ticking)
            ) {
                context.buildAndSwitch();
                consumer.accept(blockPos, ticking);
            }
        } else {
            consumer.accept(blockPos, ticking);
        }
    }
}
