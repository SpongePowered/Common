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
package org.spongepowered.common.mixin.core.server;

import com.google.common.collect.ImmutableSet;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.players.PlayerList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.advancement.AdvancementTree;
import org.spongepowered.api.advancement.criteria.AdvancementCriterion;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.advancement.AdvancementEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.advancements.AdvancementProgressBridge;
import org.spongepowered.common.bridge.advancements.CriterionBridge;
import org.spongepowered.common.bridge.advancements.CriterionProgressBridge;
import org.spongepowered.common.bridge.server.PlayerAdvancementsBridge;
import org.spongepowered.common.bridge.world.entity.PlatformEntityBridge;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin implements PlayerAdvancementsBridge {

    // @formatter:off
    @Shadow @Final private Map<AdvancementHolder, AdvancementProgress> progress;
    @Shadow private net.minecraft.server.level.ServerPlayer player;
    @Shadow private net.minecraft.advancements.AdvancementTree tree;
    
    // @formatter:on

    private boolean impl$wasSuccess;
    @Nullable private Component impl$message;

    @Inject(method = "startProgress", at = @At("HEAD"))
    private void impl$setAdvancementsOnStart(final AdvancementHolder advancement, final AdvancementProgress progress, final CallbackInfo ci) {
        final AdvancementProgressBridge advancementProgress = (AdvancementProgressBridge) progress;
        advancementProgress.bridge$setAdvancementId(advancement.id());
        advancementProgress.bridge$setPlayerAdvancements((PlayerAdvancements) (Object) this);
    }

    @Redirect(method = "registerListeners(Lnet/minecraft/advancements/AdvancementHolder;)V",
            at = @At(
                    value = "INVOKE",
                    ordinal = 0,
                    target = "Lnet/minecraft/advancements/CriterionProgress;isDone()Z"))
    private boolean impl$onUnregisterListenersGetProgress(final CriterionProgress progress) {
        final CriterionProgressBridge mixinCriterionProgress = (CriterionProgressBridge) progress;
        if (((PlatformEntityBridge) this.player).bridge$isFakePlayer() || !mixinCriterionProgress.bridge$isCriterionAvailable()) {
            return progress.isDone();
        }

        final AdvancementCriterion criterion = ((org.spongepowered.api.advancement.criteria.CriterionProgress) progress).criterion();
        final CriterionBridge mixinCriterion = (CriterionBridge) criterion;
        // Only remove the trigger once the goal is reached
        if (mixinCriterion.bridge$getScoreCriterion() != null) {


            return ((CriterionProgressBridge) progress).bridge$getAdvancementProgress()
                    .get(mixinCriterion.bridge$getScoreCriterion()).get().achieved();
        }
        return progress.isDone();
    }

    @Nullable
    @Redirect(method = "unregisterListeners",
            at = @At(
                    value = "INVOKE",
                    ordinal = 0,
                    target = "Lnet/minecraft/advancements/AdvancementProgress;getCriterion(Ljava/lang/String;)Lnet/minecraft/advancements/CriterionProgress;"))
    private CriterionProgress impl$updateProgressOnUnregister(final AdvancementProgress advancementProgress, final String criterion) {
        if (((PlatformEntityBridge) this.player).bridge$isFakePlayer()) {
            return advancementProgress.getCriterion(criterion);
        }

        final org.spongepowered.api.advancement.Advancement advancement = ((org.spongepowered.api.advancement.AdvancementProgress) advancementProgress).advancement();
        final AdvancementCriterion advancementCriterion = (AdvancementCriterion) (Object) ((Advancement) (Object) advancement).criteria().get(criterion);
        final CriterionBridge criterionBridge = (CriterionBridge) advancementCriterion;
        // Only remove the trigger once the goal is reached
        if (criterionBridge.bridge$getScoreCriterion() != null && !((org.spongepowered.api.advancement.AdvancementProgress) advancementProgress)
                .get(criterionBridge.bridge$getScoreCriterion()).get().achieved()) {
            return null;
        }
        return advancementProgress.getCriterion(criterion);
    }

    @Override
    public Set<AdvancementTree> bridge$getAdvancementTrees() {
        final ImmutableSet.Builder<AdvancementTree> builder = ImmutableSet.builder();
        for (final Map.Entry<AdvancementHolder, AdvancementProgress> entry : this.progress.entrySet()) {
            final org.spongepowered.api.advancement.Advancement advancement = (org.spongepowered.api.advancement.Advancement) (Object) entry.getKey().value();
            if (!advancement.parent().isPresent()) {
                final AdvancementNode node = this.tree.get(entry.getKey());
                if (node != null) {
                    builder.add((AdvancementTree) node.root());
                }
            }
        }
        return builder.build();
    }

    @Override
    public ServerPlayer bridge$getPlayer() {
        return (ServerPlayer) this.player;
    }

    @Override
    public void bridge$reloadAdvancementProgress() {
        for (final AdvancementProgress progress : this.progress.values()) {
            ((AdvancementProgressBridge) progress).bridge$updateProgressMap();
        }
    }

    @Redirect(method = "lambda$award$2",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void impl$updateTextOnGranting(final PlayerList instance, final net.minecraft.network.chat.Component $$0, final boolean $$1) {
        this.impl$message = SpongeAdventure.asAdventure($$0);
        this.impl$wasSuccess = true;
    }

    @Inject(method = "award",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void impl$setWasSuccessonGrant(final AdvancementHolder advancement, final String string, final CallbackInfoReturnable<Boolean> ci) {
        this.impl$wasSuccess = true;
    }

    @Inject(method = "award",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerAdvancements;markForVisibilityUpdate(Lnet/minecraft/advancements/AdvancementHolder;)V"))
    @SuppressWarnings("deprecation")
    private void impl$callGrantEventIfSuccessful(final AdvancementHolder advancement, final String string, final CallbackInfoReturnable<Boolean> ci) {
        if (!this.impl$wasSuccess) {
            return;
        }
        final Instant instant = Instant.now();


        final Audience channel;
        if (this.impl$message != null) {
            channel = Sponge.server().broadcastAudience();
        } else {
            channel = Audience.empty();
        }

        final AdvancementEvent.Grant event = SpongeEventFactory.createAdvancementEventGrant(
                Sponge.server().causeStackManager().currentCause(),
                channel,
                Optional.of(channel),
                this.impl$message == null ? Component.empty() : this.impl$message,
                this.impl$message == null ? Component.empty() : this.impl$message,
                (org.spongepowered.api.advancement.Advancement) (Object) advancement.value(),
                (ResourceKey) (Object) advancement.id(),
                (ServerPlayer) this.player, instant, false

        );
        SpongeCommon.post(event);
        if (!event.isMessageCancelled()) {
            event.audience().ifPresent(eventChannel -> eventChannel.sendMessage(Identity.nil(), event.message()));
        }

        this.impl$message = null;
        this.impl$wasSuccess = false;
    }

    @Inject(method = "revoke", locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/server/PlayerAdvancements;markForVisibilityUpdate(Lnet/minecraft/advancements/AdvancementHolder;)V"))
    private void impl$callRevokeEventIfSuccessful(final AdvancementHolder advancement, final String string, final CallbackInfoReturnable<Boolean> ci, boolean var0) {
        if (var0) {
            final Cause currentCause = Sponge.server().causeStackManager().currentCause();
            SpongeCommon.post(SpongeEventFactory.createAdvancementEventRevoke(currentCause, (org.spongepowered.api.advancement.Advancement) (Object) advancement.value(), (ResourceKey) (Object) advancement.id(), (ServerPlayer) this.player));
        }
    }

}
