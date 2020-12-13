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
package org.spongepowered.common.accessor.world.server;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.block.BlockEventData;
import net.minecraft.entity.Entity;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Queue;

@Mixin(ServerWorld.class)
public interface ServerWorldAccessor {

    @Accessor("LOGGER") static Logger accessor$LOGGER() {
        throw new UnsupportedOperationException("Untransformed Accessor");
    }

    @Invoker("hasDuplicateEntity") boolean accessor$hasDuplicateEntity(Entity entity);

    @Accessor("entitiesById") Int2ObjectMap<Entity> accessor$getEntitiesById();

    @Accessor("tickingEntities") boolean accessor$isTickingEntities();

    @Accessor("entitiesToAdd") Queue<Entity> accessor$getEntitiesToAdd();

    @Invoker("removeFromChunk") void accessor$removeFromChunk(Entity entityIn);

    @Accessor("blockEventQueue") ObjectLinkedOpenHashSet<BlockEventData> accessor$getBlockEventQueue();

    @Invoker("onEntityAdded") void accessor$onEntityAdded(Entity entityIn);
}
