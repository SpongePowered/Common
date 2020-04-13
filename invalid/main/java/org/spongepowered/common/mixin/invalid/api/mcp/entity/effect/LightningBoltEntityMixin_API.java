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
package org.spongepowered.common.mixin.invalid.api.mcp.entity.effect;

import com.google.common.collect.Lists;
import net.minecraft.entity.effect.LightningBoltEntity;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.weather.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.mixin.api.mcp.entity.EntityMixin_API;

import java.util.List;
import java.util.Set;

@Mixin(LightningBoltEntity.class)
public abstract class LightningBoltEntityMixin_API extends EntityMixin_API implements LightningBolt {

    private final List<Entity> api$struckEntities = Lists.newArrayList();
    private final List<Transaction<BlockSnapshot>> api$struckBlocks = Lists.newArrayList();
    private boolean api$effect = false;

    @Override
    public boolean isEffect() {
        return this.api$effect;
    }

    @Override
    public void setEffect(boolean effect) {
        this.api$effect = effect;
        if (effect) {
            this.api$struckBlocks.clear();
            this.api$struckEntities.clear();
        }
    }

    @Override
    protected Set<Value.Immutable<?>> api$getVanillaValues() {
        final Set<Value.Immutable<?>> values = super.api$getVanillaValues();

        values.add(this.isEffectOnly().asImmutable());
        values.add(this.expirationDelay().asImmutable());

        return values;
    }

}
