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
package org.spongepowered.common.mixin.core.server.dedicated;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.DataFixer;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.server.players.GameProfileCacheBridge;
import org.spongepowered.common.datapack.SpongeDataPackManager;
import org.spongepowered.common.launch.Launch;
import org.spongepowered.common.mixin.core.server.MinecraftServerMixin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin extends MinecraftServerMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void impl$setServerOnGame(Thread $$0, LevelStorageSource.LevelStorageAccess $$1, PackRepository $$2, WorldStem $$3,
            DedicatedServerSettings $$4, DataFixer $$5, Services $$6, ChunkProgressListenerFactory $$7, CallbackInfo ci) {
        $$4.getProperties().serverResourcePackInfo.ifPresent(packInfo -> {
            try {
                this.impl$resourcePack = ResourcePackRequest.resourcePackRequest()
                        .packs(ResourcePackInfo.resourcePackInfo(packInfo.id(), new URI(packInfo.url()), packInfo.hash()))
                        .required(packInfo.isRequired())
                        .prompt(SpongeAdventure.asAdventure(Optional.ofNullable(packInfo.prompt())))
                        .build();
            } catch (final URISyntaxException e) {
                e.printStackTrace();
            }
        });
        SpongeCommon.game().setServer(this);
        $$6.profileCache().load();

        ((SpongeDataPackManager) this.dataPackManager()).init();
    }

    @Override
    protected void loadLevel() {
        // TODO replacement? this.shadow$detectBundledResources();
        this.worldManager().loadLevel();
    }

    @Override
    public boolean bridge$performAutosaveChecks() {
        return this.shadow$isRunning();
    }

    @WrapOperation(method = "initServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/GameProfileCache;save()V"))
    private void onSave(final GameProfileCache cache, final Operation<Void> original) {
        ((GameProfileCacheBridge) cache).bridge$setCanSave(true);
        original.call(cache);
        ((GameProfileCacheBridge) cache).bridge$setCanSave(false);
    }

    @Inject(method = "stopServer", at = @At("TAIL"))
    private void impl$callStoppedGame(final CallbackInfo ci) {
        Launch.instance().lifecycle().callStoppedGameEvent();
    }
}
