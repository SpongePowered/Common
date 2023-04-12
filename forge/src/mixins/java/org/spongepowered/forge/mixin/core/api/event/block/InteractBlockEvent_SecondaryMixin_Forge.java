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
package org.spongepowered.forge.mixin.core.api.event.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.forge.hook.ForgeEventHooks;
import org.spongepowered.forge.launch.bridge.event.SpongeEventBridge_Forge;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Mixin(InteractBlockEvent.Secondary.class)
public interface InteractBlockEvent_SecondaryMixin_Forge extends SpongeEventBridge_Forge {

    @Override
    default @Nullable Collection<? extends Event> bridge$createForgeEvents() {
        final InteractBlockEvent.Secondary thisEvent = (InteractBlockEvent.Secondary) (Object) this;

        final Optional<BlockHitResult> blockHitResult = thisEvent.context().get(ForgeEventHooks.BLOCK_HIT_RESULT);
        if (!blockHitResult.isPresent()) {
            return null;
        }
        final Optional<Player> playerOpt = thisEvent.cause().first(Player.class);
        if (!playerOpt.isPresent()) {
            return null;
        }
        final BlockHitResult hitResult = blockHitResult.get();
        final BlockPos blockPos = VecHelper.toBlockPos(thisEvent.block().position());
        final InteractionHand hand = (InteractionHand) (Object) thisEvent.context().require(EventContextKeys.USED_HAND);

        final PlayerInteractEvent.RightClickBlock forgeEvent = new PlayerInteractEvent.RightClickBlock(
                playerOpt.get(),
                hand,
                blockPos,
                hitResult
        );
        return Collections.singleton(forgeEvent);
    }
}
