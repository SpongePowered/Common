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
package org.spongepowered.neoforge.launch.event;

import com.google.inject.Singleton;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.event.manager.RegisteredListener;
import org.spongepowered.common.event.manager.SpongeEventManager;
import org.spongepowered.neoforge.launch.bridge.event.NeoEventBridge_Neo;
import org.spongepowered.neoforge.launch.bridge.event.SpongeEventBridge_Neo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Singleton
public final class NeoEventManager extends SpongeEventManager implements IEventBus {

    private final IEventBus wrappedEventBus;

    public NeoEventManager(final IEventBus eventBus) {
        this.wrappedEventBus = eventBus;
    }

    // IEventBus

    @Override
    public void register(final Object target) {
        this.wrappedEventBus.register(target);
    }

    @Override
    public <T extends Event> void addListener(Consumer<T> consumer) {
        this.wrappedEventBus.addListener(consumer);
    }

    @Override
    public <T extends Event> void addListener(Class<T> eventType, Consumer<T> consumer) {
        this.wrappedEventBus.addListener(eventType, consumer);
    }

    @Override
    public <T extends Event> void addListener(EventPriority priority, Consumer<T> consumer) {
        this.wrappedEventBus.addListener(priority, consumer);
    }

    @Override
    public <T extends Event> void addListener(EventPriority priority, Class<T> eventType, Consumer<T> consumer) {
        this.wrappedEventBus.addListener(priority, eventType, consumer);
    }

    @Override
    public <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Consumer<T> consumer) {
        this.wrappedEventBus.addListener(priority, receiveCanceled, consumer);
    }

    @Override
    public <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Class<T> eventType, Consumer<T> consumer) {
        this.wrappedEventBus.addListener(priority, receiveCanceled, eventType, consumer);
    }

    @Override
    public <T extends Event> void addListener(boolean receiveCanceled, Consumer<T> consumer) {
        this.wrappedEventBus.addListener(receiveCanceled, consumer);
    }

    @Override
    public <T extends Event> void addListener(boolean receiveCanceled, Class<T> eventType, Consumer<T> consumer) {
        this.wrappedEventBus.addListener(receiveCanceled, eventType, consumer);
    }

    @Override
    public void unregister(final Object object) {
        this.wrappedEventBus.unregister(object);
    }

    @Override
    public <T extends Event> T post(T event) {
        if (event instanceof NeoEventBridge_Neo bridgeEvent) {
            // intercept!
            final org.spongepowered.api.event.@Nullable Event spongeEvent = bridgeEvent.bridge$createSpongeEvent();
            if (spongeEvent != null) {
                this.postDualBus(spongeEvent, Collections.singleton(event));
                return event;
            }
        }
        // Do as Forge does - SpongeVanilla has no role to play here.
        this.wrappedEventBus.post(event);
        return event;
    }

    @Override
    public <T extends Event> T post(EventPriority phase, T event) {
        throw new IllegalStateException("This bus does not allow calling phase-specific post.");
    }

    @Override
    public void start() {
        this.wrappedEventBus.start();
    }

    // EventManager

    @Override
    public boolean post(final org.spongepowered.api.event.Event event) {
        final SpongeEventBridge_Neo eventBridge = ((SpongeEventBridge_Neo) event);
        final @Nullable Collection<? extends Event> forgeEvents = eventBridge.bridge$createForgeEvents();
        if (forgeEvents == null || forgeEvents.isEmpty()) {
            // Do as SpongeVanilla does - Forge has no role to play here.
            return super.post(event);
        }
        return this.postDualBus(event, forgeEvents);
    }

    // Implementation

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean postDualBus(final org.spongepowered.api.event.Event spongeEvent, final Collection<? extends Event> forgeEvents) {
        try (final NoExceptionClosable ignored = this.preparePost(spongeEvent)) {
            final RegisteredListener.Cache listeners = this.getHandlerCache(spongeEvent);
            final List<RegisteredListener<?>> beforeModifications = listeners.beforeModifications();
            if (!beforeModifications.isEmpty()) {
                // First, we fire the Sponge beforeModifications on the Sponge event
                this.post(spongeEvent, beforeModifications);

                // Then we sync to the Forge events
                for (final Event forgeEvent : forgeEvents) {
                    ((NeoEventBridge_Neo) forgeEvent).bridge$syncFrom(spongeEvent);
                }
            }
            // Then, we fire all our Forge events
            for (final Event forgeEvent : forgeEvents) {
                this.wrappedEventBus.post(forgeEvent);
                // We must sync back the event's changes, if there are any.
                // For complex events, this will be a partial sync.
                ((NeoEventBridge_Neo) forgeEvent).bridge$syncTo(spongeEvent);
            }

            // and now we do our standard event listener stuff.
            return this.post(spongeEvent, listeners.afterModifications());
        }
    }
}
