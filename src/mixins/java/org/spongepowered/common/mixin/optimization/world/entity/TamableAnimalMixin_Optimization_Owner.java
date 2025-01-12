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
package org.spongepowered.common.mixin.optimization.world.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.common.mixin.core.world.entity.AgableMobMixin;

import java.util.Optional;

@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin_Optimization_Owner extends AgableMobMixin {

    @Shadow @Final protected static EntityDataAccessor<Optional<EntityReference<LivingEntity>>> DATA_OWNERUUID_ID;
    @Nullable @Unique private EntityReference<LivingEntity> cachedOwner$OwnerId;

    /**
     * @author gabizou - July 26th, 2016
     * @reason Uses the cached owner id to save constant lookups from the data watcher
     *
     * @return The owner id string
     */
    @Nullable
    @Overwrite
    public EntityReference<LivingEntity> getOwnerReference() {
        if (this.cachedOwner$OwnerId == null) {
            this.cachedOwner$OwnerId = this.entityData.get(TamableAnimalMixin_Optimization_Owner.DATA_OWNERUUID_ID).orElse(null);
        }
        return this.cachedOwner$OwnerId;
    }

    /**
     * @author gabizou - January 12th, 2025
     * @reason stores the cached owner id to save constant lookups from the data watcher
     *
     * @param entity The entity to set as the owner
     */
    @Overwrite
    public void setOwner(final @javax.annotation.Nullable LivingEntity entity) {
        final var reference = Optional.ofNullable(entity).map(EntityReference::new);
        this.cachedOwner$OwnerId = reference.orElse(null);
        this.entityData.set(DATA_OWNERUUID_ID, reference);
    }

    /**
     * @author gabizou - July 26th, 2016
     * @reason stores the cached owner id
     *
     * @param ownerUuid The owner id to set
     */
    @Overwrite
    public void setOwnerReference(@Nullable final EntityReference<LivingEntity> ownerUuid) {
        this.cachedOwner$OwnerId = ownerUuid;
        this.entityData.set(TamableAnimalMixin_Optimization_Owner.DATA_OWNERUUID_ID, Optional.ofNullable(ownerUuid));
    }

}
