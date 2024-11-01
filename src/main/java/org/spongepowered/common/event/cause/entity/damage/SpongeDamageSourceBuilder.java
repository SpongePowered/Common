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
package org.spongepowered.common.event.cause.entity.damage;


import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.cause.entity.damage.DamageType;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.world.damagesource.DamageSourceBridge;
import org.spongepowered.common.util.Preconditions;

public class SpongeDamageSourceBuilder implements DamageSource.Builder {

    private Holder<net.minecraft.world.damagesource.DamageType> damageType;
    private net.minecraft.world.entity.Entity directEntity;
    private net.minecraft.world.entity.Entity causingEntity;
    private ServerLocation location;
    private BlockSnapshot blockSnapshot;


    @SuppressWarnings("ConstantConditions")
    @Override
    public DamageSource build() throws IllegalStateException {
        Preconditions.checkState(this.damageType != null, "DamageType was null!");
        if (this.location != null) {
            Preconditions.checkState(this.blockSnapshot != null, "BlockSnapshot is null");
        }
        if (this.blockSnapshot != null) {
            Preconditions.checkState(this.location != null, "ServerLocation is null");
        }

        final var source = new net.minecraft.world.damagesource.DamageSource(this.damageType, directEntity, causingEntity);
        ((DamageSourceBridge) source).bridge$setBlock(this.location, this.blockSnapshot);
        return (DamageSource) source;
    }

    @Override
    public DamageSource.Builder type(final DamageType damageType) {
        final var registry = SpongeCommon.vanillaRegistry(Registries.DAMAGE_TYPE);
        this.damageType = registry.wrapAsHolder((net.minecraft.world.damagesource.DamageType) (Object) damageType);
        return this;
    }

    @Override
    public SpongeDamageSourceBuilder entity(final Entity entity) {
        this.directEntity = (net.minecraft.world.entity.Entity) entity;
        return this;
    }

    @Override
    public SpongeDamageSourceBuilder indirectEntity(final Entity proxy) {
        this.causingEntity = (net.minecraft.world.entity.Entity) proxy;
        return this;
    }

    @Override
    public DamageSource.Builder block(final ServerLocation location) {
        this.location = location;
        return this;
    }

    @Override
    public DamageSource.Builder block(final BlockSnapshot blockSnapshot) {
        this.blockSnapshot = blockSnapshot;
        return this;
    }

    @Override
    public SpongeDamageSourceBuilder from(final DamageSource value) {
        this.type(value.type());
        value.source().ifPresent(this::entity);
        value.indirectSource().ifPresent(this::indirectEntity);
        value.blockSnapshot().ifPresent(this::block);
        value.location().ifPresent(this::block);
        return this;
    }
}
