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
package org.spongepowered.common.mixin.core.world.entity.animal.horse;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.world.entity.animal.horse.AbstractHorseBridge;
import org.spongepowered.common.mixin.core.world.entity.AgableMobMixin;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin extends AgableMobMixin implements AbstractHorseBridge {

    // @formatter:off
    @Shadow protected SimpleContainer inventory;
    // @formatter:on

    @Override
    public boolean bridge$isSaddled() {
        return this.shadow$isSaddled();
    }

    @Override
    public void bridge$setSaddled(boolean saddled) {
        if (!this.shadow$isSaddled() && saddled) {
            this.shadow$setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        } else if (this.shadow$isSaddled() && !saddled){
            this.inventory.setItem(0, ItemStack.EMPTY);
        }
    }
}
