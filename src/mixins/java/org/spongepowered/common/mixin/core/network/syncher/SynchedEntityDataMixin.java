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
package org.spongepowered.common.mixin.core.network.syncher;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.ObjectUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.data.ChangeDataHolderEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.network.syncher.EntityDataAccessorBridge;
import org.spongepowered.common.bridge.world.entity.EntityBridge;
import org.spongepowered.common.data.datasync.DataParameterConverter;
import org.spongepowered.common.event.tracking.PhaseTracker;

import java.util.Optional;

@Mixin(SynchedEntityData.class)
public abstract class SynchedEntityDataMixin {

    //@formatter:off
    @Shadow @Final private SyncedDataHolder entity;

    @Shadow protected abstract <T> SynchedEntityData.DataItem<T> getItem(final EntityDataAccessor<T> key);
    //@formatter:on

    @SuppressWarnings({"unchecked", "rawtypes"})
    @WrapOperation(
        method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData;getItem(Lnet/minecraft/network/syncher/EntityDataAccessor;)Lnet/minecraft/network/syncher/SynchedEntityData$DataItem;")
    )
    public <T> SynchedEntityData.DataItem<T> impl$callTransactionOnGet(
        final SynchedEntityData instance, final EntityDataAccessor<T> accessor,
        final Operation<SynchedEntityData.DataItem<T>> original,
        final EntityDataAccessor<T> second, final T newValue, final boolean isServer,
        @Share("data-event") LocalRef<ChangeDataHolderEvent.ValueChange> eventHolder,
        @Share("data-converter") LocalRef<DataParameterConverter<T>> converterRef
    ) {
        final SynchedEntityData.DataItem<T> dataentry = original.call(instance, accessor);

        // Sponge Start - set up the current value, so we don't have to retrieve it multiple times
        final T currentValue = dataentry.getValue();
        final T incomingValue = newValue;
        if (isServer || ObjectUtils.notEqual(newValue, currentValue)) {
            if (this.entity == null) {
                return dataentry;
            }

            final var thisEntity = (Entity) this.entity;

            if (thisEntity.level() == null || thisEntity.level().isClientSide || ((EntityBridge) this.entity).bridge$isConstructing()) {
                return dataentry;
            }
            final Optional<DataParameterConverter<T>> converter = ((EntityDataAccessorBridge) (Object) accessor).bridge$getDataConverter();
            // At this point it is changing
            if (converter.isPresent()) {
                converterRef.set(converter.get());
                // Ok, we have a key ready to use the converter
                final Optional<DataTransactionResult> optional = converter.get().createTransaction(thisEntity, currentValue, incomingValue);
                if (optional.isEmpty()) {
                    return dataentry;
                }
                // Only need to make a transaction if there are actual changes necessary.
                final DataTransactionResult transaction = optional.get();
                final ChangeDataHolderEvent.ValueChange
                    event =
                    SpongeEventFactory.createChangeDataHolderEventValueChange(PhaseTracker.getCauseStackManager().currentCause(), transaction,
                        (DataHolder.Mutable) this.entity);
                Sponge.eventManager().post(event);
                eventHolder.set(event);
                if (event.isCancelled()) {
                    //If the event is cancelled, well, don't change the underlying value.
                    return dataentry;
                }

            }
        }
        return dataentry;
    }

    @Inject(method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData$DataItem;setValue(Ljava/lang/Object;)V"),
        cancellable = true
    )
    private <T> void impl$useChangeHolderEvent(
        final EntityDataAccessor<T> accessor, final T incoming, final boolean isServer, final CallbackInfo ci,
        @Share("data-event") LocalRef<ChangeDataHolderEvent.ValueChange> eventHolder
    ) {
        if (eventHolder.get() == null) {
            return;
        }
        final var event = eventHolder.get();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @WrapOperation(
        method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData$DataItem;setValue(Ljava/lang/Object;)V")
    )
    private <T> void impl$useEventValue(
        final SynchedEntityData.DataItem<T> instance,
        final T $$0, final Operation<Void> original, final EntityDataAccessor<T> key, final T incoming,
        final boolean isServer,
        final @Share("data-event") LocalRef<ChangeDataHolderEvent.ValueChange> eventHolder,
        final @Share("data-converter") LocalRef<DataParameterConverter<T>> converterRef
    ) {
        if (eventHolder.get() == null) {
            original.call(instance, $$0);
            return;
        }
        final var event = eventHolder.get();
        final var converter = converterRef.get();
        final var newValue = converter.getValueFromEvent(instance.getValue(), event.endResult());

        original.call(instance, newValue);
    }

}
