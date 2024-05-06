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
package org.spongepowered.common.event.tracking.context.transaction.effect;

import net.minecraft.world.ItemInteractionResult;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.UseItemOnBlockPipeline;

public final class InteractionUseItemOnBlockEffect implements ProcessingSideEffect<UseItemOnBlockPipeline, ItemInteractionResult, InteractionAtArgs, ItemInteractionResult> {

    private static final class Holder {
        static final InteractionUseItemOnBlockEffect INSTANCE = new InteractionUseItemOnBlockEffect();
    }

    public static InteractionUseItemOnBlockEffect getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public EffectResult<ItemInteractionResult> processSideEffect(
        UseItemOnBlockPipeline pipeline, ItemInteractionResult oldState, InteractionAtArgs args
    ) {
        final var player = args.player();
        final var hand = args.hand();
        final var world = args.world();
        final var blockRaytraceResult = args.blockRaytraceResult();
        final var blockstate = args.blockstate();
        final ItemInteractionResult result = blockstate.useItemOn(args.copiedStack(), world, player, hand, blockRaytraceResult);
        pipeline.transactor().logPlayerInventoryChange(args.player(), PlayerInventoryTransaction.EventCreator.STANDARD);
        try (EffectTransactor ignored = BroadcastInventoryChangesEffect.transact(pipeline.transactor())) {
            args.player().containerMenu.broadcastChanges();
        }
        return new EffectResult<>(result, true);
    }

}
