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
package org.spongepowered.forge.launch.event;

import com.google.inject.Singleton;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.GenericEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.IEventBusInvokeDispatcher;
import net.minecraftforge.eventbus.api.IEventListener;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.event.manager.RegisteredListener;
import org.spongepowered.common.event.manager.SpongeEventManager;
import org.spongepowered.forge.launch.bridge.event.ForgeEventBridge_Forge;
import org.spongepowered.forge.launch.bridge.event.SpongeEventBridge_Forge;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Singleton
public final class ForgeEventManager extends SpongeEventManager implements IEventBus {

    private final IEventBus wrappedEventBus;

    public ForgeEventManager(final IEventBus eventBus) {
        this.wrappedEventBus = eventBus;
    }

    // IEventBus

    @Override
    public void register(final Object target) {
        this.wrappedEventBus.register(target);
    }

    @Override
    public <T extends Event> void addListener(final Consumer<T> consumer) {
        this.wrappedEventBus.addListener(consumer);
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final Consumer<T> consumer) {
        this.wrappedEventBus.addListener(priority, consumer);
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final boolean receiveCancelled, final Consumer<T> consumer) {
        this.wrappedEventBus.addListener(priority, receiveCancelled, consumer);
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final boolean receiveCancelled, final Class<T> eventType, final Consumer<T> consumer) {
        this.wrappedEventBus.addListener(priority, receiveCancelled, eventType, consumer);
    }

    @Override
    public <T extends GenericEvent<? extends F>, F> void addGenericListener(final Class<F> genericClassFilter, final Consumer<T> consumer) {
        this.wrappedEventBus.addGenericListener(genericClassFilter, consumer);
    }

    @Override
    public <T extends GenericEvent<? extends F>, F> void addGenericListener(final Class<F> genericClassFilter, final EventPriority priority,
            final Consumer<T> consumer) {
        this.wrappedEventBus.addGenericListener(genericClassFilter, priority, consumer);
    }

    @Override
    public <T extends GenericEvent<? extends F>, F> void addGenericListener(final Class<F> genericClassFilter, final EventPriority priority,
            final boolean receiveCancelled, final Consumer<T> consumer) {
        this.wrappedEventBus.addGenericListener(genericClassFilter, priority, receiveCancelled, consumer);
    }

    @Override
    public <T extends GenericEvent<? extends F>, F> void addGenericListener(final Class<F> genericClassFilter, final EventPriority priority,
            final boolean receiveCancelled, final Class<T> eventType, final Consumer<T> consumer) {
        this.wrappedEventBus.addGenericListener(genericClassFilter, priority, receiveCancelled, eventType, consumer);
    }

    @Override
    public void unregister(final Object object) {
        this.wrappedEventBus.unregister(object);
    }

    @Override
    public boolean post(final Event event) {
        return this.post(event, IEventListener::invoke);
    }

    @Override
    public boolean post(final Event event, final IEventBusInvokeDispatcher wrapper) {
        if (event instanceof ForgeEventBridge_Forge<?> forgeEvent) {
            // intercept!
            final org.spongepowered.api.event.@Nullable Event spongeEvent = forgeEvent.bridge$createSpongeEvent();
            if (spongeEvent != null) {
                return this.postDualBus(spongeEvent, Collections.singleton(event), wrapper);
            }
        }
        // Do as Forge does - SpongeVanilla has no role to play here.
        return this.wrappedEventBus.post(event, wrapper);
    }

    @Override
    public void shutdown() {
        this.wrappedEventBus.shutdown();
    }

    @Override
    public void start() {
        this.wrappedEventBus.start();
    }

    // EventManager

    @Override
    public boolean post(final org.spongepowered.api.event.Event event) {
        final SpongeEventBridge_Forge eventBridge = ((SpongeEventBridge_Forge) event);
        final @Nullable Collection<? extends Event> forgeEvents = eventBridge.bridge$createForgeEvents();
        if (forgeEvents == null || forgeEvents.isEmpty()) {
            // Do as SpongeVanilla does - Forge has no role to play here.
            return super.post(event);
        }
        return this.postDualBus(event, forgeEvents, eventBridge.bridge$eventDispatcher());
    }

    // Implementation

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean postDualBus(
        final org.spongepowered.api.event.Event spongeEvent, final Collection<? extends Event> forgeEvents,
        final IEventBusInvokeDispatcher dispatcher) {
        try (final NoExceptionClosable ignored = this.preparePost(spongeEvent)) {
            final RegisteredListener.Cache listeners = this.getHandlerCache(spongeEvent);
            final List<RegisteredListener<?>> beforeModifications = listeners.beforeModifications();
            if (!beforeModifications.isEmpty()) {
                // First, we fire the Sponge beforeModifications on the Sponge event
                this.post(spongeEvent, beforeModifications);

                // Then we sync to the Forge events
                for (final Event forgeEvent : forgeEvents) {
                    ((ForgeEventBridge_Forge) forgeEvent).bridge$syncFrom(spongeEvent);
                }
            }
            // Then, we fire all our Forge events
            for (final Event forgeEvent : forgeEvents) {
                this.wrappedEventBus.post(forgeEvent, dispatcher);
                // We must sync back the event's changes, if there are any.
                // For complex events, this will be a partial sync.
                ((ForgeEventBridge_Forge) forgeEvent).bridge$syncTo(spongeEvent);
            }

            // and now we do our standard event listener stuff.
            return this.post(spongeEvent, listeners.afterModifications());
        }
    }
}
