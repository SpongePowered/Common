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
package org.spongepowered.common.mixin.core.world;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.world.Dimension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.config.SpongeConfig;
import org.spongepowered.common.interfaces.world.IMixinDimensionType;
import org.spongepowered.common.world.DimensionManager;

@Mixin(DimensionType.class)
@Implements(value = @Interface(iface = org.spongepowered.api.world.DimensionType.class, prefix = "dimensionType$"))
public abstract class MixinDimensionType implements IMixinDimensionType {

    @Shadow @Final private Class <? extends WorldProvider> clazz;
    @Shadow public abstract String getName();

    private String sanitizedId;
    private SpongeConfig<SpongeConfig.DimensionConfig> config;
    private volatile Context context;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onConstruct(String enumName, int ordinal, int idIn, String nameIn, String suffixIn, Class <? extends WorldProvider > clazzIn,
            CallbackInfo ci) {
        this.sanitizedId = this.getName().toLowerCase().replace(" ", "_").replaceAll("[^A-Za-z0-9_]", "");
        this.config = new SpongeConfig<>(SpongeConfig.Type.DIMENSION, SpongeImpl.getSpongeConfigDir().resolve("worlds").resolve(dimensionType$getId
                ()).resolve("dimension.conf"), SpongeImpl.ECOSYSTEM_ID);
        this.context = new Context(Context.DIMENSION_KEY, this.sanitizedId);
    }

    public String dimensionType$getId() {
        return this.sanitizedId;
    }

    @Intrinsic
    public String dimensionType$getName() {
        return this.getName();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Dimension> dimensionType$getDimensionClass() {
        return (Class<? extends Dimension>) this.clazz;
    }

    @Override
    public SpongeConfig<SpongeConfig.DimensionConfig> getDimensionConfig() {
        return this.config;
    }

    @Override
    public Context getContext() {
        return this.context;
    }

    /**
     * @author Zidane - March 30th, 2016
     * @reason This method generally checks dimension type ids (-1 | 0 | 1) in Vanilla. I change this assumption to dimension
     * instance ids. Since the DimensionManager tracks dimension instance ids by dimension type ids and Vanilla keeps
     * their ids 1:1, this is a safe change that ensures a mixup can't happen.
     */
    @Overwrite
    public static DimensionType getById(int dimensionId) {
        return DimensionManager.getDimensionType(dimensionId).orElseThrow(() -> new IllegalArgumentException("Invalid dimension id " + dimensionId));
    }
}
