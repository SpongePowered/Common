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
package org.spongepowered.common.mixin.core.world.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.projectile.FishingBobber;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.tracking.PhaseTracker;

@Mixin(FishingRodItem.class)
public abstract class FishingRodItemMixin {

    @Nullable private FishingHook impl$fishHook;

    @Inject(method = "use", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/entity/projectile/FishingHook;retrieve(Lnet/minecraft/world/item/ItemStack;)I"), cancellable = true)
    private void cancelHookRetraction(Level world, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.fishing != null) {
            cir.setReturnValue(InteractionResult.SUCCESS); // Event was cancelled
        }
    }

    @Inject(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V", ordinal = 1),
            cancellable = true)
    private void onThrowEvent(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide) {
            // Only fire event on server-side to avoid crash on client
            return;
        }
        ItemStack itemstack = player.getItemInHand(hand);

        if (level instanceof ServerLevel serverLevel) {
            int $$6 = (int)(EnchantmentHelper.getFishingTimeReduction(serverLevel, itemstack, player) * 20.0F);
            int $$7 = EnchantmentHelper.getFishingLuckBonus(serverLevel, itemstack, player);
            FishingHook fishHook = new FishingHook(player, level, $$7, $$6);

            PhaseTracker.getCauseStackManager().pushCause(player);
            if (SpongeCommon.post(SpongeEventFactory.createFishingEventStart(PhaseTracker.getCauseStackManager().currentCause(), (FishingBobber) fishHook))) {
                fishHook.remove(Entity.RemovalReason.DISCARDED); // Bye
                cir.setReturnValue(InteractionResult.SUCCESS);
            } else {
                this.impl$fishHook = fishHook;
            }
            PhaseTracker.getCauseStackManager().popCause();
        }
    }

    @Redirect(method = "use", at = @At(value = "NEW", target = "net/minecraft/world/entity/projectile/FishingHook"))
    private FishingHook onNewEntityFishHook(final Player $$0, final Level $$1, final int $$2, final int $$3) {
        // Use the fish hook we created for the event
        FishingHook fishHook = this.impl$fishHook;
        this.impl$fishHook = null;
        return fishHook;
    }

}
