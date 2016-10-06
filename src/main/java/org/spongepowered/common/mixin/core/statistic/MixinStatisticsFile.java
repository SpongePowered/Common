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
package org.spongepowered.common.mixin.core.statistic;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatisticsManagerServer;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.achievement.GrantAchievementEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.statistic.achievement.Achievement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.text.translation.SpongeTranslation;

import java.util.Optional;

@Mixin(StatisticsManagerServer.class)
public abstract class MixinStatisticsFile extends MixinStatFileWriter {

    private static final Translation defaultAchievementTranslation = new SpongeTranslation("chat.type.achievement");

    @Shadow private MinecraftServer mcServer;

    @Inject(method = "unlockAchievement", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isAnnouncingPlayerAchievements()Z",
            ordinal = 0, shift = Shift.BEFORE) , locals = LocalCapture.CAPTURE_FAILEXCEPTION, cancellable = true)
    private void setStatistic(EntityPlayer player, StatBase statistic, int newValue, CallbackInfo ci, int oldValue) {
        final Text message = Text.of(defaultAchievementTranslation, player.getDisplayName(), statistic.getStatName());
        final MessageChannel targetSink;
        if (this.mcServer.isAnnouncingPlayerAchievements()) {
            targetSink = MessageChannel.TO_ALL;
        } else {
            targetSink = MessageChannel.TO_NONE;
        }
        final MessageEvent.MessageFormatter formatter = new  MessageEvent.MessageFormatter(message);
        final GrantAchievementEvent event = SpongeEventFactory.createGrantAchievementEventTargetPlayer(Cause.of(NamedCause.of(NamedCause.SOURCE,player)), targetSink,
                Optional.of(targetSink), (Achievement) statistic, formatter, (Player) player, false);
        SpongeImpl.getGame().getEventManager().post(event);
        if (event.isCancelled()) {
            super.unlockAchievement(player, statistic, oldValue); // Reset to old
        } else {
            final Optional<MessageChannel> optChannel = event.getChannel();
            if (optChannel.isPresent()) {
                final MessageChannel channel = optChannel.get();
                channel.send(event.getMessage());
            }
        }
        ci.cancel();
    }

}
