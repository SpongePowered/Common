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
package org.spongepowered.common.mixin.core.world.entity.monster;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.bridge.world.entity.GrieferBridge;

@Mixin(targets = "net/minecraft/world/entity/monster/Silverfish$SilverfishMergeWithStoneGoal")
public abstract class Silverfish_SilverfishMergeWithStoneGoalMixin extends RandomStrollGoal {

    public Silverfish_SilverfishMergeWithStoneGoalMixin(PathfinderMob creatureIn, double speedIn) { // Ignored
        super(creatureIn, speedIn);
    }

    @Redirect(method = "canUse()Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/InfestedBlock;isCompatibleHostBlock(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean impl$onCanGrief(final BlockState blockState) {
        return InfestedBlock.isCompatibleHostBlock(blockState) && ((GrieferBridge) this.mob).bridge$canGrief();
    }
}
