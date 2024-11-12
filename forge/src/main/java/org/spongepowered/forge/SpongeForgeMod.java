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
package org.spongepowered.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Client;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.common.applaunch.config.core.ConfigHandle;
import org.spongepowered.common.entity.SpongeEntityTypes;
import org.spongepowered.common.entity.living.human.HumanEntity;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.launch.Launch;
import org.spongepowered.common.launch.Lifecycle;
import org.spongepowered.common.network.channel.SpongeChannelManager;
import org.spongepowered.common.network.packet.SpongePacketHandler;
import org.spongepowered.forge.hook.ForgeChannelHooks;
import org.spongepowered.forge.hook.ForgeEventHooks;
import org.spongepowered.forge.hook.ForgeGeneralHooks;
import org.spongepowered.forge.hook.ForgeItemHooks;
import org.spongepowered.forge.hook.ForgeWorldHooks;

@Mod("spongeforge")
public final class SpongeForgeMod {

    private final Logger logger = LogManager.getLogger("spongeforge");

    public SpongeForgeMod(FMLJavaModLoadingContext ctx) {
        // WorldPersistenceHooks.addHook(SpongeLevelDataPersistence.INSTANCE); // TODO SF 1.19.4

        final IEventBus modBus = ctx.getModEventBus();

        // modBus: add all FML events with it
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::onRegister);
        modBus.addListener(this::onEntityAttributeCreationEvent);

        // annotation events, for non-FML things
        MinecraftForge.EVENT_BUS.register(this);

        // Set platform hooks as required
        PlatformHooks.INSTANCE.setEventHooks(new ForgeEventHooks());
        PlatformHooks.INSTANCE.setWorldHooks(new ForgeWorldHooks());
        PlatformHooks.INSTANCE.setGeneralHooks(new ForgeGeneralHooks());
        PlatformHooks.INSTANCE.setChannelHooks(new ForgeChannelHooks());
        PlatformHooks.INSTANCE.setItemHooks(new ForgeItemHooks());
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        final Lifecycle lifecycle = Launch.instance().lifecycle();
        lifecycle.callConstructEvent();
        lifecycle.callRegisterFactoryEvent();
        lifecycle.callRegisterBuilderEvent();
        lifecycle.callRegisterChannelEvent();
        lifecycle.establishGameServices();
        lifecycle.establishDataKeyListeners();

        SpongePacketHandler.init((SpongeChannelManager) Sponge.channelManager());

        this.logger.info("SpongeForge v{} initialized", Launch.instance().platformPlugin().metadata().version());
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        final Client minecraft = (Client) Minecraft.getInstance();
        final Lifecycle lifecycle = Launch.instance().lifecycle();
        lifecycle.establishDataProviders();
        lifecycle.callRegisterDataEvent();
        lifecycle.establishClientRegistries(minecraft);
        lifecycle.callStartingEngineEvent(minecraft);
    }

    @SubscribeEvent
    public void onServerAboutToStart(final ServerAboutToStartEvent event) {
        // Save config now that registries have been initialized
        ConfigHandle.setSaveSuppressed(false);

        final Lifecycle lifecycle = Launch.instance().lifecycle();
        lifecycle.establishServerServices();

        lifecycle.establishServerFeatures();

        lifecycle.establishServerRegistries((Server) event.getServer());
        lifecycle.callStartingEngineEvent((Server) event.getServer());
    }

    @SubscribeEvent
    public void onServerStarted(final ServerStartedEvent event) {
        final Lifecycle lifecycle = Launch.instance().lifecycle();
        lifecycle.callStartedEngineEvent((Server) event.getServer());

        lifecycle.callLoadedGameEvent();
    }

    @SubscribeEvent
    public void onServerStoppingEvent(final ServerStoppingEvent event) {
        final Lifecycle lifecycle = Launch.instance().lifecycle();
        lifecycle.callStoppingEngineEvent((Server) event.getServer());
    }

    public void onRegister(RegisterEvent event) {
        if (event.getRegistryKey() == ForgeRegistries.Keys.ENTITY_TYPES) {
            SpongeEntityTypes.HUMAN = EntityType.Builder.of(HumanEntity::new, MobCategory.MISC)
                    .noSave()
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(org.spongepowered.common.util.Constants.Entity.Player.TRACKING_RANGE)
                    .updateInterval(2)
                    .build(ResourceKey.create(ForgeRegistries.Keys.ENTITY_TYPES, ResourceLocation.parse("sponge:human")))
            ;

            event.register(ForgeRegistries.Keys.ENTITY_TYPES, helper -> helper.register(HumanEntity.KEY, SpongeEntityTypes.HUMAN));
        }
    }
    public void onEntityAttributeCreationEvent(final EntityAttributeCreationEvent event) {
        event.put(SpongeEntityTypes.HUMAN, HumanEntity.createAttributes());
    }
}
