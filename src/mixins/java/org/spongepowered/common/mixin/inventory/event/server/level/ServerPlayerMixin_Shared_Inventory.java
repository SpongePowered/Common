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
package org.spongepowered.common.mixin.inventory.event.server.level;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.bridge.world.inventory.container.ContainerBridge;
import org.spongepowered.common.event.inventory.InventoryEventFactory;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.mixin.inventory.event.entity.player.PlayerMixin_Inventory;

import java.util.OptionalInt;

// Forge and Vanilla
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin_Shared_Inventory extends PlayerMixin_Inventory {

    // @formatter:off
    @Shadow public abstract ServerLevel shadow$serverLevel();
    // @formatter:on

    @Nullable private Object inventory$menuProvider;

    protected ServerPlayerMixin_Shared_Inventory(final EntityType<?> param0, final Level param1) {
        super(param0, param1);
    }

    @Inject(
        method = "openMenu",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/server/level/ServerPlayer;containerMenu:Lnet/minecraft/world/inventory/AbstractContainerMenu;",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.AFTER
        )
    )
    private void impl$afterOpenMenu(final CallbackInfoReturnable<OptionalInt> cir) {
        PhaseTracker.getWorldInstance(this.shadow$serverLevel()).getPhaseContext().getTransactor().logContainerSet((ServerPlayer) (Object) this);
    }

    @Inject(method = "openMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;initMenu(Lnet/minecraft/world/inventory/AbstractContainerMenu;)V"))
    private void impl$onOpenMenu(final MenuProvider menuProvider, final CallbackInfoReturnable<OptionalInt> cir) {
        this.inventory$menuProvider = menuProvider;
    }

    @Inject(method = "openMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;closeContainer()V", shift = At.Shift.AFTER), cancellable = true)
    private void impl$openMenuCloseCancelled(final CallbackInfoReturnable<OptionalInt> cir) {
        if (this.containerMenu != this.inventoryMenu) {
            cir.setReturnValue(OptionalInt.empty());
        }
    }

    @WrapOperation(
        method = "openMenu",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/MenuProvider;createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;"
        )
    )
    private AbstractContainerMenu impl$transactMenuCreationWithEffect(
        final MenuProvider menuProvider, final int containerCounter, final net.minecraft.world.entity.player.Inventory inventory,
        final Player player, final Operation<AbstractContainerMenu> original, final @Cancellable CallbackInfoReturnable<OptionalInt> cir
    ) {
        final PhaseContext<?> context = PhaseTracker.getWorldInstance(this.shadow$serverLevel()).getPhaseContext();
        try (final EffectTransactor ignored = context.getTransactor().logOpenInventory((ServerPlayer) (Object) this)) {
            final AbstractContainerMenu menu = original.call(menuProvider, containerCounter, inventory, player);
            context.containerLocation().ifPresent(((ContainerBridge) menu)::bridge$setOpenLocation);
            if (!InventoryEventFactory.callInteractContainerOpenEvent((ServerPlayer) (Object) this, menu)) {
                cir.setReturnValue(OptionalInt.empty());
            }
            return menu;
        }
    }
}
