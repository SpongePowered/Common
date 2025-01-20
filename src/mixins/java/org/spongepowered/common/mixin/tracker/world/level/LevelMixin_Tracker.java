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
package org.spongepowered.common.mixin.tracker.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.PhaseTracker;

import java.util.List;
import java.util.function.Predicate;

@Mixin(Level.class)
public abstract class LevelMixin_Tracker implements LevelBridge, LevelAccessor {

    // @formatter:off
    @Shadow @Final public RandomSource random;

    @Shadow public abstract LevelChunk shadow$getChunkAt(BlockPos pos);
    @Override
    @Shadow public boolean setBlock(final BlockPos pos, final BlockState state, final int flags, final int limit) { throw new IllegalStateException("Untransformed shadow!"); }
    @Shadow public void shadow$removeBlockEntity(final BlockPos pos) { } // shadowed
    @Shadow @Nullable public abstract BlockEntity shadow$getBlockEntity(BlockPos pos);
    @Shadow public void shadow$setBlockEntity(final BlockEntity tileEntity) { } // Shadowed
    @Shadow public abstract BlockState shadow$getBlockState(BlockPos pos);
    @Shadow public abstract boolean shadow$isDebug();
    @Override
    @Shadow public boolean destroyBlock(final BlockPos p_241212_1_, final boolean p_241212_2_, @Nullable final Entity p_241212_3_, final int p_241212_4_) { throw new IllegalStateException("Untransformed shadow!"); }
    @Shadow public abstract FluidState shadow$getFluidState(BlockPos p_204610_1_);
    // @formatter:on


    @Inject(method = {
        "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
        "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
    }, at = @At("RETURN"))
    private void tracker$ThrowCollisionEvent(final @Coerce Object entityIn, final AABB aabb, final Predicate<?> filter, final CallbackInfoReturnable<List<Entity>> cir) {
        if (this.bridge$isFake() || !PhaseTracker.getInstance().getPhaseContext().allowsEntityCollisionEvents()) {
            return;
        }
        final List<Entity> ret = cir.getReturnValue();
        if (ret == null || ret.isEmpty()) {
            return;
        }

        if (!ShouldFire.COLLIDE_ENTITY_EVENT) {
            return;
        }

        final @org.checkerframework.checker.nullness.qual.Nullable Entity entity = entityIn instanceof Entity ? ((Entity) entityIn) : null;
        if (SpongeCommonEventFactory.callCollideEntityEvent(entity, ret).isCancelled()) {
            ret.clear();
        }
    }

}
