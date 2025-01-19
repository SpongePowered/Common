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
package org.spongepowered.neoforge.mixin.core.neoforge.event.world;

import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.transaction.BlockTransaction;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.neoforge.launch.bridge.event.NeoEventBridge_Neo;

import java.util.Collections;

@Mixin(value = BlockEvent.BreakEvent.class, remap = false)
public abstract class BlockEvent_BreakEventMixin_Neo extends BlockEventMixin_Neo implements NeoEventBridge_Neo<ChangeBlockEvent.All>, ICancellableEvent {

    @Override
    public void bridge$syncFrom(final ChangeBlockEvent.All event) {
        final Vector3i pos = VecHelper.toVector3i(this.shadow$getPos());
        if (event.isCancelled() ||
            event.transactions()
                .stream()
                .filter(x -> x.original().position().equals(pos))
                .anyMatch(x -> !x.isValid() || x.operation() != Operations.BREAK.get() || x.custom().isPresent())) {
            this.setCanceled(true);
        }
    }

    @Override
    public void bridge$syncTo(final ChangeBlockEvent.All event) {
        if (this.isCanceled()) {
            final Vector3i pos = VecHelper.toVector3i(this.shadow$getPos());
            event.transactions(Operations.BREAK.get())
                .filter(x -> x.original().position().equals(pos))
                .forEach(x -> x.setValid(false));
        }
    }

    @Override
    public ChangeBlockEvent.@Nullable All bridge$createSpongeEvent() {
        final LevelAccessor accessor = this.shadow$getLevel();
        if (accessor instanceof ServerWorld serverWorld) {
            final BlockTransaction transaction = new BlockTransaction(
                    SpongeBlockSnapshot.BuilderImpl
                            .pooled()
                            .world(serverWorld.key())
                            .position(VecHelper.toVector3i(this.shadow$getPos()))
                            .blockState((BlockState) this.shadow$getState())
                            .build(),
                    SpongeBlockSnapshot.BuilderImpl
                            .pooled()
                            .world(serverWorld.key())
                            .position(VecHelper.toVector3i(this.shadow$getPos()))
                            .blockState(BlockState.builder().blockType(BlockTypes.AIR.get()).build())
                            .build(),
                    Operations.BREAK.get()
            );
            return SpongeEventFactory.createChangeBlockEventAll(PhaseTracker.getCauseStackManager().currentCause(),
                    Collections.singletonList(transaction), serverWorld);
        }
        return null;
    }
}
