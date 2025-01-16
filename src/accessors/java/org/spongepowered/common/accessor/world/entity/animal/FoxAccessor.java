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
package org.spongepowered.common.accessor.world.entity.animal;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.common.UntransformedAccessorError;

import java.util.Optional;

@Mixin(Fox.class)
public interface FoxAccessor {

    @Accessor("DATA_TRUSTED_ID_0") static EntityDataAccessor<Optional<EntityReference<LivingEntity>>> accessor$DATA_TRUSTED_ID_0() {
        throw new UntransformedAccessorError();
    }

    @Accessor("DATA_TRUSTED_ID_1") static EntityDataAccessor<Optional<EntityReference<LivingEntity>>> accessor$DATA_TRUSTED_ID_1() {
        throw new UntransformedAccessorError();
    }

    @Invoker("setFaceplanted") void invoker$setFaceplanted(final boolean faceplanted);

    @Invoker("isDefending") boolean invoker$isDefending();

    @Invoker("setDefending") void invoker$setDefending(final boolean defending);

    @Invoker("setSleeping") void invoker$setSleeping(final boolean sleeping);

    @Invoker("setVariant") void invoker$setVariant(Fox.Variant v);

}
