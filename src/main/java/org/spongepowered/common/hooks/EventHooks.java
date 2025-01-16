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
package org.spongepowered.common.hooks;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.tracking.PhaseTracker;

/**
 * Event hooks for when there is no specific event translation between Sponge
 * and the platform.
 */
public interface EventHooks {

    default ChangeEntityWorldEvent.Pre callChangeEntityWorldEventPre(final Entity entity, final ServerLevel toWorld) {
        final ChangeEntityWorldEvent.Pre event = SpongeEventFactory.createChangeEntityWorldEventPre(PhaseTracker.getCauseStackManager().currentCause(),
            (org.spongepowered.api.entity.Entity) entity, (org.spongepowered.api.world.server.ServerWorld) entity.getCommandSenderWorld(),
            (org.spongepowered.api.world.server.ServerWorld) toWorld, (org.spongepowered.api.world.server.ServerWorld) toWorld);
        SpongeCommon.post(event);
        return event;
    }

    default boolean callPlayerDestruction(ServerPlayer serverPlayer, DamageSource cause) {
        return false;
    }
}
