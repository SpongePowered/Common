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
package org.spongepowered.common.accessor.entity.passive;

import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.network.datasync.DataParameter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;
import java.util.UUID;

@Mixin(FoxEntity.class)
public interface FoxEntityAccessor {

    @Accessor("TRUSTED_UUID_SECONDARY") static DataParameter<Optional<UUID>> accessor$getTrustedUuidSecondary() {
        throw new IllegalStateException("Untransformed Accessor!");
    }

    @Accessor("TRUSTED_UUID_MAIN") static DataParameter<Optional<UUID>> accessor$getTrustedUuidMain() {
        throw new IllegalStateException("Untransformed Accessor!");
    }

    @Invoker("setVariantType") void accessor$setVariantType(FoxEntity.Type typeIn);
    @Invoker("setStuck") void accessor$setStuck(boolean isStuck);
    @Invoker("setSleeping") void accessor$setSleeping(boolean isSleeping);
    @Invoker("isFoxAggroed") boolean accessor$isFoxAggroed();
    @Invoker("setFoxAggroed") void accessor$setFoxAggroed(boolean aggroed);

}
