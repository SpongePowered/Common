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
package org.spongepowered.common.event.tracking.phase;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.context.GeneralizedContext;

import java.util.List;
import java.util.stream.Collectors;

public class PlayerPhase extends TrackingPhase {

    public static final class State {

        public static final IPhaseState<GeneralizedContext> PLAYER_LOGOUT = new PlayerPhaseState();
    }

    static final class PlayerPhaseState implements IPhaseState<GeneralizedContext> {

        PlayerPhaseState() {
        }

        @Override
        public TrackingPhase getPhase() {
            return TrackingPhases.PLAYER;
        }

        @Override
        public GeneralizedContext createPhaseContext() {
            return new GeneralizedContext(this);
        }

        @Override
        public void unwind(GeneralizedContext phaseContext) {

        }
        private final String className = this.getClass().getSimpleName();

        @Override
        public String toString() {
            return this.getPhase() + "{" + this.className + "}";
        }
    }

    public static PlayerPhase getInstance() {
        return Holder.INSTANCE;
    }

    private PlayerPhase() {
    }

    private static final class Holder {

        static final PlayerPhase INSTANCE = new PlayerPhase();
    }

    @SuppressWarnings("unchecked")
    public void unwind(IPhaseState<?> state, PhaseContext<?> phaseContext) {
        // Since currently all we have is PLAYER_LOGOUT, don't care about
        // states.
        final Player player = phaseContext.getSource(Player.class)
                .orElseThrow(TrackingUtil.throwWithContext("Expected to be processing a player leaving, but we're not!", phaseContext));
        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(player);
            phaseContext.getCapturedItemsSupplier().acceptAndClearIfNotEmpty(items -> {
                SpongeCommonEventFactory.callDropItemDispense(items, phaseContext);
            });
            phaseContext.getCapturedEntitySupplier().acceptAndClearIfNotEmpty(entities -> {

            });
            phaseContext.getCapturedItemStackSupplier().acceptAndClearIfNotEmpty(items -> {
                final List<Entity> drops = items.stream()
                        .map(drop -> drop.create(EntityUtil.getMinecraftWorld(player)))
                        .map(EntityUtil::fromNative)
                        .collect(Collectors.toList());
                SpongeCommonEventFactory.callDropItemCustom(drops, phaseContext);
            });
            phaseContext.getCapturedBlockSupplier()
                    .acceptAndClearIfNotEmpty(blocks -> TrackingUtil.processBlockCaptures(blocks, state, phaseContext));
        }
    }

}
