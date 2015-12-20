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
package org.spongepowered.common.mixin.core.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.projectile.FishHook;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.interfaces.IMixinEntityFishHook;

@Mixin(ItemFishingRod.class)
public abstract class MixinItemFishingRod extends Item {

    @Override
    @Overwrite
    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        if (player.fishEntity != null) {
            // Damage and animation logic is moved inside handleHookRetraction, as the event
            // is fired there
            ((IMixinEntityFishHook) player.fishEntity).setFishingRodItemStack(itemStack);
            player.fishEntity.handleHookRetraction();
        } else {
            EntityFishHook fishHook = new EntityFishHook(world, player);
            EntitySnapshot fishHookSnapshot = ((Entity) fishHook).createSnapshot();
            // only fire event on server-side to avoid crash on client
            if (!player.worldObj.isRemote
                && SpongeImpl.postEvent(SpongeEventFactory.createFishingEventStart(SpongeImpl.getGame(), Cause.of(NamedCause.source(player)),
                fishHookSnapshot, (FishHook) fishHook))) {
                player.fishEntity = null;
                return itemStack;
            }

            world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
            if (!player.worldObj.isRemote) {
                world.spawnEntityInWorld(fishHook);
            }

            player.swingItem();
            player.triggerAchievement(StatList.objectUseStats[Item.getIdFromItem(this)]);
        }
        return itemStack;
    }

}
