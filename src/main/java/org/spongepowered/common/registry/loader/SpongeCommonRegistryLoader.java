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
package org.spongepowered.common.registry.loader;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.common.data.nbt.validation.SpongeValidationType;
import org.spongepowered.common.data.nbt.validation.ValidationType;
import org.spongepowered.common.data.nbt.validation.ValidationTypes;
import org.spongepowered.common.event.tracking.context.transaction.type.BlockTransactionType;
import org.spongepowered.common.event.tracking.context.transaction.type.NoOpTransactionType;
import org.spongepowered.common.event.tracking.context.transaction.type.TransactionType;
import org.spongepowered.common.event.tracking.context.transaction.type.TransactionTypes;
import org.spongepowered.common.registry.RegistryLoader;

import java.util.Locale;

public class SpongeCommonRegistryLoader {

    public static RegistryLoader<TransactionType<@NonNull ?>> blockTransactionTypes() {
        return RegistryLoader.of(l -> {
            l.add(TransactionTypes.BLOCK, k -> new BlockTransactionType());
            l.add(TransactionTypes.CHANGE_INVENTORY_EVENT, k -> new NoOpTransactionType<>(false, k.value().toUpperCase(Locale.ROOT)));
            l.add(TransactionTypes.CLICK_CONTAINER_EVENT, k -> new NoOpTransactionType<>(false, k.value().toUpperCase(Locale.ROOT)));
            l.add(TransactionTypes.ENTITY_DEATH_DROPS, k -> new NoOpTransactionType<>(false, k.value().toUpperCase(Locale.ROOT)));
            l.add(TransactionTypes.INTERACT_CONTAINER_EVENT, k -> new NoOpTransactionType<>(false, k.value().toUpperCase(Locale.ROOT)));
            l.add(TransactionTypes.NEIGHBOR_NOTIFICATION, k -> new NoOpTransactionType<>(false, k.value().toUpperCase(Locale.ROOT)));
            l.add(TransactionTypes.SCHEDULE_BLOCK_UPDATE, k -> new NoOpTransactionType<>(false, k.value().toUpperCase(Locale.ROOT)));
            l.add(TransactionTypes.SLOT_CHANGE, k -> new NoOpTransactionType<>(false, k.value().toUpperCase(Locale.ROOT)));
            l.add(TransactionTypes.SPAWN_ENTITY, k -> new NoOpTransactionType<>(false, k.value().toUpperCase(Locale.ROOT)));
        });
    }

    public static RegistryLoader<ValidationType> validationType() {
        return RegistryLoader.of(l -> l.mapping(SpongeValidationType::new, m -> m.add(
                ValidationTypes.BLOCK_ENTITY,
                ValidationTypes.ENTITY
        )));
    }

}
