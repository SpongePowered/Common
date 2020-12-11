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
package org.spongepowered.vanilla.mixin.core.util.registry;

import com.mojang.serialization.Lifecycle;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.MutableRegistry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.registry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.bridge.util.registry.MutableRegistryBridge;
import org.spongepowered.common.bridge.util.registry.RegistryBridge;
import org.spongepowered.common.registry.SpongeRegistryEntry;

@Mixin(MutableRegistry.class)
public abstract class MutableRegistryMixin_Vanilla<T> extends RegistryMixin_Vanilla<T> implements MutableRegistryBridge<T> {

    // @formatter:off
    @Shadow public abstract <V extends T> V shadow$register(RegistryKey<T> p_218381_1_, V p_218381_2_, Lifecycle p_218381_3_);
    // @formatter:on

    private boolean vanilla$isDynamic = true;

    @Override
    public boolean bridge$isDynamic() {
        return this.vanilla$isDynamic;
    }

    @Override
    public void bridge$setDynamic(final boolean isDynamic) {
        this.vanilla$isDynamic = isDynamic;
    }

    @Nullable
    @Override
    public RegistryEntry<T> bridge$register(RegistryKey<T> key, T value, Lifecycle lifecycle) {
        this.shadow$register(key, value, lifecycle);

        return ((RegistryBridge<T>) this).bridge$getEntries().get(key.location());
    }

    @Inject(method = "registerMapping", at = @At("TAIL"))
    private void vanilla$cacheRegistryEntry(int p_218382_1_, RegistryKey<T> p_218382_2_, T p_218382_3_, Lifecycle p_218382_4_,
            CallbackInfoReturnable<T> cir) {

        this.bridge$getEntries().put((ResourceKey) (Object) p_218382_2_.location(),
                new SpongeRegistryEntry<>((ResourceKey) (Object) p_218382_2_.location(), p_218382_3_));
    }
}
