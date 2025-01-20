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
package org.spongepowered.common.mixin.core.server;

import com.google.inject.Injector;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.world.UnloadWorldEvent;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectProxy;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.SpongeServer;
import org.spongepowered.common.applaunch.config.core.SpongeConfigs;
import org.spongepowered.common.bridge.commands.CommandSourceBridge;
import org.spongepowered.common.bridge.commands.CommandSourceProviderBridge;
import org.spongepowered.common.bridge.network.chat.SpongeChatDecorator;
import org.spongepowered.common.bridge.server.MinecraftServerBridge;
import org.spongepowered.common.bridge.server.level.ServerLevelBridge;
import org.spongepowered.common.bridge.server.players.GameProfileCacheBridge;
import org.spongepowered.common.bridge.world.level.storage.PrimaryLevelDataBridge;
import org.spongepowered.common.config.inheritable.InheritableConfigHandle;
import org.spongepowered.common.config.inheritable.WorldConfig;
import org.spongepowered.common.datapack.SpongeDataPackManager;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.registry.SpongeRegistryHolder;
import org.spongepowered.common.service.server.SpongeServerScopedServiceProvider;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements SpongeServer, MinecraftServerBridge, CommandSourceProviderBridge, SubjectProxy,
    CommandSourceBridge {

    // @formatter:off
    @Shadow @Final private Map<ResourceKey<Level>, ServerLevel> levels;
    @Shadow @Final private static Logger LOGGER;
    @Shadow private int tickCount;
    @Shadow @Final protected LevelStorageSource.LevelStorageAccess storageSource;
    @Shadow @Final private Thread serverThread;

    @Shadow public abstract CommandSourceStack shadow$createCommandSourceStack();
    @Shadow public abstract Iterable<ServerLevel> shadow$getAllLevels();
    @Shadow public abstract boolean shadow$isDedicatedServer();
    @Shadow public abstract boolean shadow$isRunning();
    @Shadow public abstract PlayerList shadow$getPlayerList();
    @Shadow public abstract PackRepository shadow$getPackRepository();
    @Shadow public abstract RegistryAccess.Frozen shadow$registryAccess();
    @Shadow public abstract GameProfileCache shadow$getProfileCache();
    @Shadow public abstract CompletableFuture<Void> shadow$reloadResources(final Collection<String> $$0);
    @Shadow public abstract WorldData shadow$getWorldData();
    @Shadow protected abstract void loadLevel(); // has overrides!
    @Shadow public abstract boolean shadow$haveTime();
    @Shadow public abstract ReloadableServerRegistries.Holder shadow$reloadableRegistries();
    @Shadow private volatile boolean isSaving;
    // @formatter:on

    private final ChatDecorator impl$spongeDecorator = new SpongeChatDecorator();
    private @Nullable SpongeServerScopedServiceProvider impl$serviceProvider;
    protected @Nullable ResourcePackRequest impl$resourcePack;
    private final BlockableEventLoop<Runnable> impl$spongeMainThreadExecutor = new BlockableEventLoop<>("Sponge") {

        //Used to schedule internal Sponge tasks to the main thread
        //that could be joined on the main thread. Avoiding using the
        //MinecraftServer Executor to prevent changes in timings.

        @Override
        protected @NonNull Runnable wrapRunnable(@NonNull Runnable runnable) {
            return runnable;
        }

        @Override
        protected boolean shouldRun(@NonNull Runnable runnable) {
            return MinecraftServerMixin.this.shadow$haveTime();
        }

        @Override
        protected @NonNull Thread getRunningThread() {
            return MinecraftServerMixin.this.serverThread;
        }
    };

    @Override
    public Subject subject() {
        return SpongeCommon.game().systemSubject();
    }

    @Inject(method = "spin", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private static void impl$setThreadOnServerPhaseTracker(final Function<Thread, MinecraftServer> p_240784_0_,
                                                           final CallbackInfoReturnable<MinecraftServerMixin> cir,
                                                           final AtomicReference<MinecraftServer> atomicReference,
                                                           final Thread thread) {
        try {
            PhaseTracker.SERVER.setThread(thread);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException("Could not initialize the server PhaseTracker!");
        }
    }

    @Override
    public ResourcePackRequest bridge$getResourcePack() {
        return this.impl$resourcePack;
    }

    @Inject(method = "tickServer", at = @At(value = "HEAD"))
    private void impl$onServerTickStart(final CallbackInfo ci) {
        this.scheduler().tick();
    }

    @Override
    public CommandSourceStack bridge$getCommandSource(final Cause cause) {
        return this.shadow$createCommandSourceStack();
    }

    // The Audience of the Server is actually a Forwarding Audience - so any message sent to
    // the server will be sent to everyone connected. We therefore need to make sure we send
    // things to the right place. We consider anything done by the server as being done by the
    // system subject
    @Override
    public void bridge$addToCauseStack(final CauseStackManager.StackFrame frame) {
        frame.pushCause(Sponge.systemSubject());
    }

    /**
     * @author Zidane
     * @reason Apply our branding
     */
    @DontObfuscate
    @Overwrite
    public String getServerModName() {
        return "sponge";
    }

    @Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;saveAllChunks(ZZZ)Z"))
    private void impl$callUnloadWorldEvents(final CallbackInfo ci) {
        for(final ServerLevel level : this.shadow$getAllLevels()) {
            final UnloadWorldEvent unloadWorldEvent = SpongeEventFactory.createUnloadWorldEvent(PhaseTracker.getCauseStackManager().currentCause(), (ServerWorld) level);
            SpongeCommon.post(unloadWorldEvent);
        }
    }

    @Inject(method = "stopServer", at = @At(value = "TAIL"))
    private void impl$closeLevelSaveForOtherWorlds(final CallbackInfo ci) {
        for (final Map.Entry<ResourceKey<Level>, ServerLevel> entry : this.levels.entrySet()) {
            if (entry.getKey() == Level.OVERWORLD) {
                continue;
            }

            final LevelStorageSource.LevelStorageAccess levelSave = ((ServerLevelBridge) entry.getValue()).bridge$getLevelSave();
            try {
                levelSave.close();
            } catch (final IOException e) {
                MinecraftServerMixin.LOGGER.error("Failed to unlock level {}", levelSave.getLevelId(), e);
            }
        }
    }

    /**
     * Render localized/formatted chat components
     *
     * @param input original component
     */
    @Inject(method = "sendSystemMessage", at = @At("HEAD"), cancellable = true)
    private void impl$useTranslatingLogger(final Component input, final CallbackInfo ci) {
        MinecraftServerMixin.LOGGER.info(input.getString());
        ci.cancel();
    }

    @ModifyConstant(method = "tickServer", constant = @Constant(intValue = 0, ordinal = 0, expandZeroConditions = Constant.Condition.LESS_THAN_OR_EQUAL_TO_ZERO),
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;ticksUntilAutosave:I"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;computeNextAutosaveInterval()I")))
    private int getSaveTickInterval(final int zero) {
        if (!this.shadow$isDedicatedServer()) {
            return zero;
        } else if (!this.shadow$isRunning()) {
            // Don't autosave while server is stopping
            return Integer.MIN_VALUE;
        }

        final int autoPlayerSaveInterval = SpongeConfigs.getCommon().get().world.playerAutoSaveInterval;
        if (autoPlayerSaveInterval > 0 && (this.tickCount % autoPlayerSaveInterval == 0)) {
            this.isSaving = true;
            this.shadow$getPlayerList().saveAll();
            this.isSaving = false;
        }

        this.isSaving = true;
        this.saveAllChunks(true, false, false);
        this.isSaving = false;

        // force check to fail as we handle everything above
        return Integer.MIN_VALUE;
    }

    /**
     * @author Zidane - November, 24th 2020 - Minecraft 1.15
     * @reason To allow per-world auto-save tick intervals or disable auto-saving entirely
     */
    @Overwrite
    public boolean saveAllChunks(final boolean suppressLog, final boolean flush, final boolean isForced) {
        boolean result = false;

        for (final ServerLevel world : this.shadow$getAllLevels()) {
            // Sponge start - use our own config
            final SerializationBehavior serializationBehavior = ((PrimaryLevelDataBridge) world.getLevelData()).bridge$serializationBehavior().orElse(SerializationBehavior.AUTOMATIC);
            final InheritableConfigHandle<WorldConfig> configAdapter = ((PrimaryLevelDataBridge) world.getLevelData()).bridge$configAdapter();
            final boolean log = configAdapter.get().world.logAutoSave;

            // If the server isn't running or we hit Vanilla's save interval or this was triggered
            // by a command, save our configs
            if (!this.shadow$isRunning() || this.tickCount % 6000 == 0 || isForced) {
                ((PrimaryLevelDataBridge) world.getLevelData()).bridge$configAdapter().save();
            }

            final boolean canSaveAtAll = serializationBehavior != SerializationBehavior.NONE;

            // This world is set to not save of any time, no reason to check the auto-save/etc, skip it
            if (!canSaveAtAll) {
                continue;
            }

            // Only run auto-save skipping if the server is still running and the save is not forced
            if (this.bridge$performAutosaveChecks() && !isForced) {
                final int autoSaveInterval = configAdapter.get().world.autoSaveInterval;

                // Do not process properties or chunks if the world is not set to do so unless the server is shutting down
                if (autoSaveInterval <= 0 || serializationBehavior != SerializationBehavior.AUTOMATIC) {
                    continue;
                }

                // Now check the interval vs the tick counter and skip it
                if (this.tickCount % autoSaveInterval != 0) {
                    continue;
                }
            }
            // Sponge end

            if (log) {
                LOGGER.info("Saving chunks for level '{}'/{}", world, world.dimension().location());
            }

            world.save(null, flush, world.noSave && !isForced);
            result = true;
        }

        // Sponge start - We do per-world WorldInfo/WorldBorders/BossBars
//        ServerLevel var2 = this.overworld();
//        ServerLevelData var3 = this.worldData.overworldData();
//        var3.setWorldBorder(var2.getWorldBorder().createSettings());
//        this.worldData.setCustomBossEvents(this.getCustomBossEvents().save(this.registryAccess()));
//        this.storageSource.saveDataTag(this.registryHolder, this.worldData, this.shadow$getPlayerList().getSingleplayerData());
        // Sponge end

        // Sponge start
        // Save the usercache.json file every 10 minutes or if forced to
        if (isForced || this.tickCount % 6000 == 0) {
            // We want to save the username cache json, as we normally bypass it.
            final GameProfileCache profileCache = this.shadow$getProfileCache();
            ((GameProfileCacheBridge) profileCache).bridge$setCanSave(true);
            profileCache.save();
            ((GameProfileCacheBridge) profileCache).bridge$setCanSave(false);
        }
        // Sponge end

        if (flush) {
            for (final ServerLevel world : this.shadow$getAllLevels()) {
                // Sponge start - use our own config
                final InheritableConfigHandle<WorldConfig> configAdapter = ((PrimaryLevelDataBridge) world.getLevelData()).bridge$configAdapter();
                final boolean log = configAdapter.get().world.logAutoSave;
                // Sponge end

                if (log) {
                    LOGGER.info("ThreadedAnvilChunkStorage ({}): All chunks are saved", world.getChunkSource().chunkMap.getStorageName());
                }
            }

            LOGGER.info("ThreadedAnvilChunkStorage: All dimensions are saved");
        }

        return result;
    }

    /**
     * @author Zidane
     * @reason Set the difficulty without marking as custom
     */
    @Overwrite
    public void setDifficulty(final Difficulty difficulty, final boolean forceDifficulty) {
        for (final ServerLevel world : this.shadow$getAllLevels()) {
            this.bridge$setDifficulty(world, difficulty, forceDifficulty);
        }
    }

    @Override
    public void bridge$setDifficulty(final ServerLevel world, final Difficulty newDifficulty, final boolean forceDifficulty) {
        if (world.getLevelData().isDifficultyLocked() && !forceDifficulty) {
            return;
        }

        if (forceDifficulty && world.getLevelData() instanceof PrimaryLevelDataBridge bridge && bridge.bridge$isVanilla()) {
            // Don't allow vanilla forcing the difficulty at launch set ours if we have a custom one
            if (!bridge.bridge$customDifficulty()) {
                bridge.bridge$forceSetDifficulty(newDifficulty);
            }
        } else {
            ((PrimaryLevelData) world.getLevelData()).setDifficulty(newDifficulty);
        }
    }

    @Override
    public void bridge$initServices(final Game game, final Injector injector) {
        if (this.impl$serviceProvider == null) {
            this.impl$serviceProvider = new SpongeServerScopedServiceProvider(this, game, injector);
            this.impl$serviceProvider.init();
        }
    }

    @Override
    public SpongeServerScopedServiceProvider bridge$getServiceProvider() {
        return this.impl$serviceProvider;
    }

    @Inject(method = "reloadResources", at = @At(value = "HEAD"))
    public void impl$reloadResources(final Collection<String> datapacksToLoad, final CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        final List<String> reloadablePacks = ((SpongeDataPackManager) this.dataPackManager()).registerPacks();
        datapacksToLoad.addAll(reloadablePacks);
        this.shadow$getPackRepository().reload();
    }

    // Targets lambda where ReloadableResources is set to server because at
    // this point reloaded resources become "valid" in terms of registries
    @Inject(method = "lambda$reloadResources$29", at = @At(value = "TAIL"))
    private void impl$handleRegistriesReload(final Collection<String> $$0x, final @Coerce Object reloadableResources, final CallbackInfo ci) {
        ((SpongeRegistryHolder) this).registryHolder().reload(this.shadow$reloadableRegistries().get());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Inject(method = "getChatDecorator", at = @At("RETURN"), cancellable = true)
    private void impl$redirectChatDecorator(final CallbackInfoReturnable<ChatDecorator> cir) {
        if (cir.getReturnValue() == ChatDecorator.PLAIN) {
            cir.setReturnValue(this.impl$spongeDecorator);
        }
    }

    @Override
    public BlockableEventLoop<Runnable> bridge$spongeMainThreadExecutor() {
        return this.impl$spongeMainThreadExecutor;
    }

    @Inject(method = "pollTaskInternal", at = @At("HEAD"), cancellable = true)
    private void impl$pollSpongeTasks(final CallbackInfoReturnable<Boolean> cir) {
        //Pool our tasks first to try to have small impact on timings
        if (this.impl$spongeMainThreadExecutor.pollTask()) {
            cir.setReturnValue(true);
        }
    }
}
