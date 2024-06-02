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
package org.spongepowered.common.mixin.core.registries;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.launch.Launch;

import java.util.Map;
import java.util.function.Supplier;

@Mixin(BuiltInRegistries.class)
public abstract class BuiltInRegistriesMixin {

    // We hook in here if we extend existing vanilla BuiltInRegistries with our own
    // This methods should then be called during bootstrap

    @Shadow @Final private static Map<ResourceLocation, Supplier<?>> LOADERS;

    @Inject(method = "bootStrap", at = @At(value = "HEAD"))
    private static void impl$beforeCreateContents(CallbackInfo ci)
    {
        Launch.instance().lifecycle().establishEarlyGlobalRegistries();
    }

    @Inject(method = "bootStrap", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/registries/BuiltInRegistries;freeze()V"))
    private static void impl$beforeFreeze(CallbackInfo ci)
    {
        Launch.instance().lifecycle().finalizeEarlyGlobalRegistries();
    }

}
