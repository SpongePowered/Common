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
package org.spongepowered.common.inject;

import com.google.inject.PrivateModule;
import com.google.inject.binder.AnnotatedBindingBuilder;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.MinecraftVersion;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.AssetManager;
import org.spongepowered.api.command.manager.CommandManager;
import org.spongepowered.api.config.ConfigManager;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.registry.GameRegistry;
import org.spongepowered.api.service.ServiceProvider;
import org.spongepowered.api.service.whitelist.WhitelistService;
import org.spongepowered.api.sql.SqlManager;
import org.spongepowered.api.util.metric.MetricsConfigManager;
import org.spongepowered.api.world.TeleportHelper;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.SpongeGame;
import org.spongepowered.common.SpongePlatform;
import org.spongepowered.common.asset.SpongeAssetManager;
import org.spongepowered.common.command.SpongeCommandManager;
import org.spongepowered.common.config.SpongeConfigManager;
import org.spongepowered.common.data.SpongeDataManager;
import org.spongepowered.common.event.SpongeCauseStackManager;
import org.spongepowered.common.event.SpongeEventManager;
import org.spongepowered.common.launch.Launcher;
import org.spongepowered.common.registry.SpongeGameRegistry;
import org.spongepowered.common.service.SpongeServiceProvider;
import org.spongepowered.common.service.ban.SpongeBanService;
import org.spongepowered.common.service.permission.SpongePermissionService;
import org.spongepowered.common.service.user.SpongeUserStorageService;
import org.spongepowered.common.service.whitelist.SpongeWhitelistService;
import org.spongepowered.common.sql.SpongeSqlManager;
import org.spongepowered.common.util.metric.SpongeMetricsConfigManager;
import org.spongepowered.common.world.teleport.SpongeTeleportHelper;

import javax.annotation.OverridingMethodsMustInvokeSuper;

public final class SpongeCommonModule extends PrivateModule {

    @Override
    @OverridingMethodsMustInvokeSuper
    protected void configure() {
        this.bind(Logger.class).toInstance(SpongeCommon.getLogger());
        //noinspection UninstantiableBinding
        this.bindAndExpose(Game.class).to(SpongeGame.class);
        this.bindAndExpose(Platform.class).to(SpongePlatform.class);
        this.bindAndExpose(MinecraftVersion.class).toInstance(SpongeCommon.MINECRAFT_VERSION);
        this.bindAndExpose(AssetManager.class).to(SpongeAssetManager.class);
        this.bindAndExpose(EventManager.class).to(SpongeEventManager.class);
        this.bindAndExpose(PluginManager.class).toInstance(Launcher.getInstance().getPluginManager());
        this.bindAndExpose(GameRegistry.class).to(SpongeGameRegistry.class);
        this.bindAndExpose(TeleportHelper.class).to(SpongeTeleportHelper.class);
        this.bindAndExpose(DataManager.class).to(SpongeDataManager.class);
        this.bindAndExpose(ConfigManager.class).to(SpongeConfigManager.class);
        this.bindAndExpose(CauseStackManager.class).to(SpongeCauseStackManager.class);
        this.bindAndExpose(MetricsConfigManager.class).to(SpongeMetricsConfigManager.class);
        this.bindAndExpose(SqlManager.class).to(SpongeSqlManager.class);
        this.bindAndExpose(ServiceProvider.class).to(SpongeServiceProvider.class);
        this.bindAndExpose(CommandManager.class).to(SpongeCommandManager.class);

        // Default services that might need to be injected
        this.bind(SpongeBanService.class);
        // TODO: Pagination Service
        this.bind(SpongePermissionService.class);
        this.bind(SpongeUserStorageService.class);
        this.bind(SpongeWhitelistService.class);

        this.requestStaticInjection(SpongeCommon.class);
        this.requestStaticInjection(Sponge.class);
    }

    protected <T> AnnotatedBindingBuilder<T> bindAndExpose(final Class<T> type) {
        this.expose(type);
        return this.bind(type);
    }
}
