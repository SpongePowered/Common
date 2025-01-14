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
package org.spongepowered.common.mixin.inventory.event.entity.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.mixin.inventory.event.entity.LivingEntityMixin_Inventory;

import java.util.Map;

@Mixin(value = Player.class)
public abstract class PlayerMixin_Inventory extends LivingEntityMixin_Inventory {

    // @formatter:off
    @Final @Shadow public net.minecraft.world.entity.player.Inventory inventory;
    @Shadow public AbstractContainerMenu containerMenu;
    @Shadow @Final public InventoryMenu inventoryMenu;
    // @formatter:on

    protected PlayerMixin_Inventory(final EntityType<?> param0, final Level param1) {
        super(param0, param1);
    }

    // -- Override some injections


    @Override
    protected void inventory$onHandleEquipmentChanges(
        final Map<EquipmentSlot, ItemStack> map, final CallbackInfo ci
    ) {
        if (this.tickCount == 1) {
            // Ignore Equipment on player spawn/respawn
            return;
        }
        super.inventory$onHandleEquipmentChanges(map, ci);
    }

    @Inject(method = "setItemSlot", at = @At(value = "HEAD"))
    protected void impl$beforeSetItemSlot(final EquipmentSlot param0, final ItemStack param1, final CallbackInfo ci) {
    }

    @Inject(method = "setItemSlot", at = @At(value = "RETURN"))
    protected void impl$afterSetItemSlot(final EquipmentSlot param0, final ItemStack param1, final CallbackInfo ci) {
    }



    @Redirect(method = "remove", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/InventoryMenu;removed(Lnet/minecraft/world/entity/player/Player;)V"))
    protected void inventory$switchToCloseWindowState(final InventoryMenu instance, final Player $$0) {
        instance.removed($$0);
    }

    @Redirect(method = "touch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;playerTouch(Lnet/minecraft/world/entity/player/Player;)V"))
    protected void inventory$onTouch(final Entity entity, final Player player) {
        entity.playerTouch(player);
    }

}
