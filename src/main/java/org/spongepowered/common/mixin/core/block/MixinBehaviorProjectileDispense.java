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
package org.spongepowered.common.mixin.core.block;

import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.BehaviorProjectileDispense;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.IProjectile;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.entity.projectile.LaunchProjectileEvent;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeImpl;

import java.util.ArrayList;
import java.util.List;

@NonnullByDefault
@Mixin(BehaviorProjectileDispense.class)
public class MixinBehaviorProjectileDispense extends BehaviorDefaultDispenseItem {

    private boolean shouldNotSpawn = false;

    @Inject(method = "dispenseStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION, cancellable = true)
    public void onspawnEntity(IBlockSource source, ItemStack stack, CallbackInfoReturnable<ItemStack> cir, EnumFacing enumfacing, IProjectile iprojectile) {
        TileEntity tileEntity = source.getBlockTileEntity();
        if (tileEntity instanceof ProjectileSource) {
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                frame.addContext(EventContextKeys.PROJECTILE_SOURCE, (ProjectileSource) tileEntity);
                frame.addContext(EventContextKeys.THROWER, (ProjectileSource) tileEntity);
                ((Projectile) iprojectile).setShooter((ProjectileSource) tileEntity);
                List<Projectile> projectiles = new ArrayList<>();
                projectiles.add((Projectile) iprojectile);
                LaunchProjectileEvent event =
                        SpongeEventFactory.createLaunchProjectileEvent(Sponge.getCauseStackManager().getCurrentCause(), projectiles);
                if (SpongeImpl.postEvent(event)) {
                    cir.setReturnValue(stack);
                }

                if (!projectiles.remove(iprojectile)) {
                    this.shouldNotSpawn = true;
                }

                event.getEntities().forEach(entity -> entity.getWorld().spawnEntity(entity));
            }
        }
    }

    @Redirect(method = "dispenseStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    public boolean redirectSpawn(World world, net.minecraft.entity.Entity entityIn) {
        if (this.shouldNotSpawn) {
            this.shouldNotSpawn = false;
            return false;
        }

        return world.spawnEntity(entityIn);
    }
}
