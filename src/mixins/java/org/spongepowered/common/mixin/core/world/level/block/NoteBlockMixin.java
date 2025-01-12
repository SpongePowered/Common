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
package org.spongepowered.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.InstrumentType;
import org.spongepowered.api.data.type.NotePitch;
import org.spongepowered.api.event.sound.PlaySoundEvent;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.mixin.core.block.BlockMixin;

@Mixin(NoteBlock.class)
public abstract class NoteBlockMixin extends BlockMixin {

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "triggerEvent",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/level/Level;playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V"
            ),
            cancellable = true)
    // TODO mixin earlier to cancel tuning particles
    private void impl$throwNoteBlockSoundEvent(BlockState state, Level worldIn, BlockPos pos, int id, int param, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (!ShouldFire.PLAY_SOUND_EVENT_NOTE_BLOCK) {
            return;
        }

        //No noteblock sounds if the block above it isn't air
        if (!worldIn.getBlockState(pos.above()).is(Blocks.AIR)) {
            return;
        }

        //InstrumentProperty doesn't return what we wan't for the noteblock directly, so we have to check the block under it.
        InstrumentType instrumentType = ((ServerWorld) worldIn).block(pos.getX(), pos.getY() - 1, pos.getZ()).get(Keys.REPRESENTED_INSTRUMENT).orElse(null);
        if (instrumentType == null) {
            return;
        }

        float pitch = (float) Math.pow(2.0D, (double) (param - 12) / 12.0D);

        final MappedRegistry<NotePitch> registry =
                (MappedRegistry<NotePitch>) (Object) Sponge.game().registry(RegistryTypes.NOTE_PITCH);
        // TODO byStateAbove
        final PlaySoundEvent.NoteBlock event = SpongeCommonEventFactory.callPlaySoundNoteBlockEvent(
                PhaseTracker.getCauseStackManager().currentCause(), (ServerWorld) worldIn, pos,
                state.instrument().getSoundEvent().value(), instrumentType, registry.byId(param), pitch);
        if (event.isCancelled()) {
            callbackInfo.setReturnValue(true);
        }
    }
}
