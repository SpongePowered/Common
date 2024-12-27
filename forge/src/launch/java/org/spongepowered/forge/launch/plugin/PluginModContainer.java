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
package org.spongepowered.forge.launch.plugin;

import com.google.inject.Injector;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.EventBusErrorMessage;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.IEventListener;
import net.minecraftforge.fml.Logging;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingException;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.config.IConfigEvent;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.fml.javafmlmod.AutomaticEventSubscriber;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.common.inject.plugin.PluginGuice;
import org.spongepowered.common.launch.Launch;
import org.spongepowered.forge.launch.event.ForgeEventManager;
import org.spongepowered.plugin.metadata.model.PluginDependency;

import java.util.concurrent.CountDownLatch;

// Spongified FMLModContainer
public final class PluginModContainer extends ModContainer {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ModFileScanData scanResults;
    private final IEventBus eventBus;
    private Object modInstance;
    private final Class<?> modClass;
    private final CountDownLatch initializationLock;

    public PluginModContainer(IModInfo info, String className, ModFileScanData modFileScanResults, ModuleLayer gameLayer) {
        super(info);
        LOGGER.debug(Logging.LOADING, "Creating PluginModContainer instance for {}", className);
        this.scanResults = modFileScanResults;
        this.activityMap.put(ModLoadingStage.CONSTRUCT, this::constructPlugin);
        this.eventBus = BusBuilder.builder().setExceptionHandler(this::onEventFailed).setTrackPhases(false).markerType(IModBusEvent.class).useModLauncher().build();
        this.contextExtension = () -> null;
        this.initializationLock = new CountDownLatch(1);

        try {
            Module module = gameLayer.findModule(info.getOwningFile().moduleName()).orElseThrow();
            modClass = Class.forName(module, className);
            LOGGER.trace(Logging.LOADING, "Loaded modclass {} with {}", modClass.getName(), modClass.getClassLoader());
        } catch (Throwable e) {
            LOGGER.error(Logging.LOADING, "Failed to load class {}", className, e);
            throw new ModLoadingException(info, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmodclass", e);
        }
    }

    @Override
    public void dispatchConfigEvent(IConfigEvent event) {
        this.eventBus.post(event.self());
    }

    private void onEventFailed(IEventBus iEventBus, Event event, IEventListener[] iEventListeners, int i, Throwable throwable) {
        LOGGER.error(new EventBusErrorMessage(event, i, iEventListeners, throwable));
    }

    private void constructPlugin() {
        try {
            LOGGER.trace(Logging.LOADING, "Loading plugin instance {} of type {}", getModId(), this.modClass.getName());

            final ForgePluginContainer pluginContainer = ForgePluginContainer.of(this);
            pluginContainer.metadata().dependencies().stream()
                .filter(d -> d.loadOrder() == PluginDependency.LoadOrder.AFTER)
                .flatMap(d -> ModList.get().getModContainerById(d.id()).stream())
                .filter(m -> m instanceof PluginModContainer)
                .forEach(m -> ((PluginModContainer) m).waitForInitialization());

            ModList.get().forEachModInOrder(m -> {
                if (m instanceof PluginModContainer p && ForgePluginContainer.of(m).metadata().dependencies().stream()
                    .anyMatch(d -> d.id().equals(this.getModId()) && d.loadOrder() == PluginDependency.LoadOrder.BEFORE)) {
                    p.waitForInitialization();
                }
            });

            final Injector pluginInjector = PluginGuice.create(pluginContainer, this.modClass, Launch.instance().lifecycle().platformInjector());
            this.modInstance = pluginInjector.getInstance(this.modClass);
            pluginContainer.setInjector(pluginInjector);
            ((ForgeEventManager) MinecraftForge.EVENT_BUS).registerListeners(pluginContainer, this.modInstance);

            LOGGER.trace(Logging.LOADING, "Loaded plugin instance {} of type {}", getModId(), this.modClass.getName());

            this.initializationLock.countDown();
        } catch (Throwable e) {
            LOGGER.error(Logging.LOADING, "Failed to create plugin instance. PluginID: {}, class {}", getModId(), this.modClass.getName(), e);
            throw new ModLoadingException(this.modInfo, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmod", e, this.modClass);
        }

        try {
            LOGGER.trace(Logging.LOADING, "Injecting Automatic event subscribers for {}", getModId());
            AutomaticEventSubscriber.inject(this, this.scanResults, this.modClass.getClassLoader());
            LOGGER.trace(Logging.LOADING, "Completed Automatic event subscribers for {}", getModId());
        } catch (Throwable e) {
            LOGGER.error(Logging.LOADING, "Failed to register automatic subscribers. PluginID: {}, class {}", getModId(), this.modClass.getName(), e);
            throw new ModLoadingException(this.modInfo, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmod", e, this.modClass);
        }
    }

    @Override
    public boolean matches(Object mod) {
        return mod == this.modInstance;
    }

    @Override
    public Object getMod() {
        return this.modInstance;
    }

    @Override
    protected <T extends Event & IModBusEvent> void acceptEvent(final T e) {
        try {
            LOGGER.trace(Logging.LOADING, "Firing event for modid {} : {}", this.getModId(), e);
            this.eventBus.post(e);
            LOGGER.trace(Logging.LOADING, "Fired event for modid {} : {}", this.getModId(), e);
        } catch (Throwable t) {
            LOGGER.error(Logging.LOADING, "Caught exception during event {} dispatch for modid {}", e, this.getModId(), t);
            throw new ModLoadingException(this.modInfo, this.modLoadingStage, "fml.modloading.errorduringevent", t);
        }
    }

    private void waitForInitialization() {
        try {
            this.initializationLock.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
