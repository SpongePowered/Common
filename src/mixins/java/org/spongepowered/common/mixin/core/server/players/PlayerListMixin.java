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
package org.spongepowered.common.mixin.core.server.players;

import io.netty.channel.local.LocalAddress;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.Audiences;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.RotateEntityEvent;
import org.spongepowered.api.event.entity.living.player.RespawnPlayerEvent;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.event.message.SystemMessageEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.SpongeServer;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.client.server.IntegratedPlayerListBridge;
import org.spongepowered.common.bridge.data.TransientBridge;
import org.spongepowered.common.bridge.data.VanishableBridge;
import org.spongepowered.common.bridge.network.ConnectionBridge;
import org.spongepowered.common.bridge.server.ServerScoreboardBridge;
import org.spongepowered.common.bridge.server.level.ServerLevelBridge;
import org.spongepowered.common.bridge.server.level.ServerPlayerBridge;
import org.spongepowered.common.bridge.server.players.PlayerListBridge;
import org.spongepowered.common.bridge.world.level.storage.PrimaryLevelDataBridge;
import org.spongepowered.common.entity.player.LoginPermissions;
import org.spongepowered.common.entity.player.SpongeUserView;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.context.transaction.TransactionalCaptureSupplier;
import org.spongepowered.common.event.tracking.context.transaction.effect.BroadcastInventoryChangesEffect;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;
import org.spongepowered.common.profile.SpongeGameProfile;
import org.spongepowered.common.server.PerWorldBorderListener;
import org.spongepowered.common.service.server.ban.SpongeIPBanList;
import org.spongepowered.common.service.server.ban.SpongeUserBanList;
import org.spongepowered.common.service.server.whitelist.SpongeUserWhiteList;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.NetworkUtil;
import org.spongepowered.math.vector.Vector3d;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin implements PlayerListBridge {

    // @formatter:off
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private static SimpleDateFormat BAN_DATE_FORMAT;
    @Shadow @Final private MinecraftServer server;
    @Shadow @Final @Mutable private UserBanList bans;
    @Shadow @Final @Mutable private IpBanList ipBans;
    @Shadow @Final @Mutable private UserWhiteList whitelist;
    @Shadow @Final private List<net.minecraft.server.level.ServerPlayer> players;
    @Shadow @Final protected int maxPlayers;
    @Shadow @Final private Map<UUID, net.minecraft.server.level.ServerPlayer> playersByUUID;
    @Shadow @Final private PlayerDataStorage playerIo;

    @Shadow public abstract MinecraftServer shadow$getServer();
    @Shadow public abstract Optional<CompoundTag> shadow$load(final net.minecraft.server.level.ServerPlayer $$0);
    @Shadow public abstract boolean shadow$canBypassPlayerLimit(com.mojang.authlib.GameProfile param0);
    @Shadow protected abstract boolean shadow$verifyChatTrusted(final PlayerChatMessage $$0);
    @Shadow protected abstract void shadow$broadcastChatMessage(final PlayerChatMessage $$0, final Predicate<net.minecraft.server.level.ServerPlayer> $$1,
        final net.minecraft.server.level.@Nullable ServerPlayer $$2, final ChatType.Bound $$4);
    // @formatter:on

    private boolean impl$isRespawnWithPosition = false;
    private boolean impl$isDuringSystemMessageEvent = false;

    private ResourceKey<Level> impl$originalRespawnDestination = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void impl$setSpongeLists(final CallbackInfo callbackInfo) {
        this.bans = new SpongeUserBanList(PlayerList.USERBANLIST_FILE);
        this.ipBans = new SpongeIPBanList(PlayerList.IPBANLIST_FILE);
        this.whitelist = new SpongeUserWhiteList(PlayerList.WHITELIST_FILE);
    }

    @Override
    public CompletableFuture<net.minecraft.network.chat.Component> bridge$canPlayerLogin(final SocketAddress param0, final com.mojang.authlib.GameProfile param1) {
        if (this instanceof IntegratedPlayerListBridge) {
            return ((IntegratedPlayerListBridge) this).bridge$canPlayerLoginClient(param0, param1);
        }
        return this.impl$canPlayerLoginServer(param0, param1);
    }

    protected final CompletableFuture<net.minecraft.network.chat.Component> impl$canPlayerLoginServer(final SocketAddress param0, final com.mojang.authlib.GameProfile param1) {
        final SpongeGameProfile profile = SpongeGameProfile.basicOf(param1);

        return Sponge.server().serviceProvider().banService().find(profile).thenCompose(profileBanOpt -> {
            if (profileBanOpt.isPresent()) {
                final Ban.Profile var0 = profileBanOpt.get();
                final MutableComponent var1 = net.minecraft.network.chat.Component.translatable("multiplayer.disconnect.banned.reason", var0.reason().orElse(Component.empty()));
                if (var0.expirationDate().isPresent()) {
                    Date date = Date.from(var0.expirationDate().get());
                    var1.append(net.minecraft.network.chat.Component.translatable("multiplayer.disconnect.banned.expiration", BAN_DATE_FORMAT.format(date)));
                }
                return CompletableFuture.completedFuture(var1);
            }

            if (param0 instanceof LocalAddress) { // don't bother looking up IP bans on local address
                return CompletableFuture.completedFuture(null);
            }

            final InetAddress address;
            try {
                address = InetAddress.getByName(NetworkUtil.getHostString(param0));
            } catch (final UnknownHostException ex) {
                return CompletableFuture.completedFuture(net.minecraft.network.chat.Component.literal(ex.getMessage())); // no
            }
            return Sponge.server().serviceProvider().banService().find(address).thenCompose(ipBanOpt -> {
                if (ipBanOpt.isPresent()) {
                    final Ban.IP var2 = ipBanOpt.get();
                    final MutableComponent var3 = net.minecraft.network.chat.Component.translatable("multiplayer.disconnect.banned_ip.reason", var2.reason().orElse(Component.empty()));
                    if (var2.expirationDate().isPresent()) {
                        Date date = Date.from(var2.expirationDate().get());
                        var3.append(net.minecraft.network.chat.Component.translatable("multiplayer.disconnect.banned_ip.expiration", BAN_DATE_FORMAT.format(date)));
                    }
                    return CompletableFuture.completedFuture(var3);
                }

                return CompletableFuture.supplyAsync(() -> Sponge.server().isWhitelistEnabled(), SpongeCommon.server())
                    .thenCompose(whitelistEnabled -> {
                        if (!whitelistEnabled) {
                            return CompletableFuture.completedFuture(null);
                        }
                        final PermissionService permissionService = Sponge.server().serviceProvider().permissionService();
                        return permissionService.userSubjects().loadSubject(param1.getId().toString()).handle((subject, ex) -> {
                            if (ex == null) {
                                return subject.hasPermission(LoginPermissions.BYPASS_WHITELIST_PERMISSION);
                            }
                            return permissionService.defaults().hasPermission(LoginPermissions.BYPASS_WHITELIST_PERMISSION);
                        }).thenCompose(whitelistBypass -> {
                            if (whitelistBypass) {
                                return CompletableFuture.completedFuture(null);
                            }
                            return Sponge.server().serviceProvider().whitelistService().isWhitelisted(profile).<net.minecraft.network.chat.Component>thenApply(whitelisted -> {
                                if (!whitelisted) {
                                    return net.minecraft.network.chat.Component.translatable("multiplayer.disconnect.not_whitelisted");
                                }
                                return null;
                            });
                        });
                    });
            });
        }).thenApplyAsync(component -> {
            if (component != null) {
                return component;
            }
            if (this.players.size() >= this.maxPlayers && !this.shadow$canBypassPlayerLimit(param1)) {
                return net.minecraft.network.chat.Component.translatable("multiplayer.disconnect.server_full");
            }
            return null;
        }, SpongeCommon.server());
    }

    @Redirect(method = "placeNewPlayer",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;load(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/Optional;"
        )
    )
    private Optional<CompoundTag> impl$setPlayerDataForNewPlayers(final PlayerList playerList, final net.minecraft.server.level.ServerPlayer playerIn) {
        final Optional<CompoundTag> compound = this.shadow$load(playerIn);
        if (compound.isEmpty()) {
            final Instant now = Instant.now();
            ((ServerPlayer) playerIn).offer(Keys.FIRST_DATE_JOINED, now);
            ((ServerPlayer) playerIn).offer(Keys.LAST_DATE_PLAYED, now);
        }
        return compound;
    }

    @Redirect(method = "placeNewPlayer",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"
        )
    )
    private net.minecraft.server.level.ServerLevel impl$onInitPlayer_getWorld(final MinecraftServer minecraftServer,
        final ResourceKey<Level> dimension, final Connection networkManager, final net.minecraft.server.level.ServerPlayer mcPlayer
    ) {
        final net.minecraft.network.chat.@Nullable Component kickReason = ((ConnectionBridge) networkManager).bridge$getKickReason();
        final Component disconnectMessage;
        if (kickReason != null) {
            disconnectMessage = SpongeAdventure.asAdventure(kickReason);
        } else {
            disconnectMessage = Component.text("You are not allowed to log in to this server.");
        }

        net.minecraft.server.level.ServerLevel mcWorld = minecraftServer.getLevel(dimension);

        if (mcWorld == null) {
            SpongeCommon.logger().warn("The player '{}' was located in a world that isn't loaded or doesn't exist. This is not safe so "
                            + "the player will be moved to the spawn of the default world.", mcPlayer.getGameProfile().getName());
            mcWorld = minecraftServer.overworld();
            final BlockPos spawnPoint = mcWorld.getSharedSpawnPos();
            mcPlayer.setPos(spawnPoint.getX() + 0.5, spawnPoint.getY() + 0.5, spawnPoint.getZ() + 0.5);
        }

        mcPlayer.setServerLevel(mcWorld);

        final ServerPlayer player = (ServerPlayer) mcPlayer;
        final ServerLocation location = player.serverLocation();
        final Vector3d rotation = player.rotation();
        // player.connection() cannot be used here, because it's still be null at this point
        final ServerSideConnection connection = (ServerSideConnection) ((ConnectionBridge) networkManager).bridge$getEngineConnection();

        // The user is not yet in the player list, so we need to make special provision.
        final User user = SpongeUserView.createLoginEventUser(player);

        final Cause cause = Cause.of(EventContext.empty(), connection, user);
        final ServerSideConnectionEvent.Login event = SpongeEventFactory.createServerSideConnectionEventLogin(cause, disconnectMessage,
                disconnectMessage, location, location, rotation, rotation, connection, SpongeGameProfile.of(mcPlayer.getGameProfile()), user);
        if (kickReason != null) {
            event.setCancelled(true);
        }
        if (SpongeCommon.post(event)) {
            this.impl$disconnectClient(networkManager, event.message(), player.profile());
            return null;
        }

        final ServerLocation toLocation = event.toLocation();
        final Vector3d toRotation = event.toRotation();
        mcPlayer.absMoveTo(toLocation.x(), toLocation.y(), toLocation.z(),
                (float) toRotation.y(), (float) toRotation.x());
        return (net.minecraft.server.level.ServerLevel) toLocation.world();
    }

    @Inject(method = "placeNewPlayer",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;",
            shift = At.Shift.AFTER
        )
    )
    private void impl$onInitPlayer_BeforeSetWorld(final Connection $$0, final net.minecraft.server.level.ServerPlayer $$1, final CommonListenerCookie $$2, final CallbackInfo ci) {
        if (!$$0.isConnected()) {
            ci.cancel();
        }
    }

    @Redirect(method = "placeNewPlayer",
        at = @At(value = "INVOKE",
            target = "Lorg/slf4j/Logger;info(Ljava/lang/String;[Ljava/lang/Object;)V",
            remap = false
        )
    )
    private void impl$onInitPlayer_printPlayerWorldInJoinFeedback(final org.slf4j.Logger logger, final String s, final Object[] objects,
            final Connection conn, final net.minecraft.server.level.ServerPlayer player) {
        logger.info("{}[{}] logged in to world '{}' with entity id {} at ({}, {}, {})", player.getName().getString(), player,
                ((ServerWorld)player.serverLevel()).key(), player.getId(), player.getX(), player.getY(), player.getZ());
    }

    @Redirect(method = "placeNewPlayer",
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/server/MinecraftServer;invalidateStatus()V"),
            to = @At(
                value = "FIELD",
                opcode = Opcodes.GETSTATIC,
                target = "Lnet/minecraft/ChatFormatting;YELLOW:Lnet/minecraft/ChatFormatting;"
            )
        ),
        at = @At(
            value = "INVOKE",
            remap = false,
            target = "Ljava/lang/String;equalsIgnoreCase(Ljava/lang/String;)Z"
        )
    )
    private boolean impl$onInitPlayer_dontClassSpongeNameAsModified(final String currentName, final String originalName) {
        if (originalName.equals(Constants.GameProfile.DUMMY_NAME)) {
            return true;
        }
        return currentName.equalsIgnoreCase(originalName);
    }

    @Redirect(method = "placeNewPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
        )
    )
    private void impl$onInitPlayer_delaySendMessage(final PlayerList instance, final net.minecraft.network.chat.Component message,
            final boolean $$1, final Connection manager, final net.minecraft.server.level.ServerPlayer playerIn
    ) {
        // Don't send here, will be done later. We cache the expected message.
        ((ServerPlayerBridge) playerIn).bridge$setConnectionMessageToSend(message);
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/players/PlayerList;viewDistance:I"))
    private int impl$usePerWorldViewDistance(final PlayerList instance, final Connection $$0, final net.minecraft.server.level.ServerPlayer $$1, final CommonListenerCookie $$2) {
        return ((PrimaryLevelDataBridge) $$1.serverLevel().getLevelData()).bridge$viewDistance().orElse( instance.getViewDistance());
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getCustomBossEvents()Lnet/minecraft/server/bossevents/CustomBossEvents;"))
    private CustomBossEvents impl$getPerWorldBossBarManager(
            final MinecraftServer minecraftServer, final Connection netManager, final net.minecraft.server.level.ServerPlayer playerIn) {
        return ((ServerLevelBridge) playerIn.serverLevel()).bridge$getBossBarManager();
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;updateEntireScoreboard(Lnet/minecraft/server/ServerScoreboard;Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void impl$sendScoreboard(final PlayerList playerList, final ServerScoreboard scoreboardIn, final net.minecraft.server.level.ServerPlayer playerIn) {
        ((ServerPlayerBridge)playerIn).bridge$initScoreboard();
    }

    @Redirect(
        method = "placeNewPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private void impl$onlySendSelfAddPlayerOnVanished(final PlayerList playerList, final Packet<?> addPlayer,
        final Connection playerConnection, final net.minecraft.server.level.ServerPlayer serverPlayer
    ) {
        if (((VanishableBridge) serverPlayer).bridge$vanishState().invisible()) {
            serverPlayer.connection.send(addPlayer);
            return;
        }
        playerList.broadcastAll(addPlayer);
    }

    @Redirect(
        method = "placeNewPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket;createPlayerInitializing(Ljava/util/Collection;)Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket;",
            ordinal = 0
        )
    )
    private ClientboundPlayerInfoUpdatePacket impl$onlySendAddPlayerForUnvanishedPlayers(final Collection<net.minecraft.server.level.ServerPlayer> $$0) {
        // Effectively, don't notify new players of vanished players
        final List<net.minecraft.server.level.ServerPlayer> vanishFiltered = $$0.stream().filter(p -> !((VanishableBridge) p).bridge$vanishState().invisible()).toList();
        return ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(vanishFiltered);
    }

    @Inject(method = "placeNewPlayer", at = @At(value = "RETURN"))
    private void impl$onInitPlayer_join(final Connection networkManager, final net.minecraft.server.level.ServerPlayer mcPlayer, final CommonListenerCookie $$2, final CallbackInfo ci) {
        final ServerPlayer player = (ServerPlayer) mcPlayer;
        final ServerSideConnection connection = player.connection();
        final Cause cause = Cause.of(EventContext.empty(), connection, player);
        final Audience audience = Audiences.onlinePlayers();
        final Component joinComponent = SpongeAdventure.asAdventure(((ServerPlayerBridge) mcPlayer).bridge$getConnectionMessageToSend());

        final ServerSideConnectionEvent.Join event = SpongeEventFactory.createServerSideConnectionEventJoin(cause, audience,
                Optional.of(audience), joinComponent, joinComponent, connection, player, SpongeGameProfile.of(mcPlayer.getGameProfile()), false);
        SpongeCommon.post(event);
        if (!event.isMessageCancelled()) {
            event.audience().ifPresent(audience1 -> audience1.sendMessage(Identity.nil(), event.message()));
        }

        ((ServerPlayerBridge) mcPlayer).bridge$setConnectionMessageToSend(null);

        final PhaseTracker phaseTracker = PhaseTracker.getWorldInstance(mcPlayer.serverLevel());
        final PhaseContext<?> context = phaseTracker.getPhaseContext();
        phaseTracker.pushCause(event);
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        transactor.logPlayerInventoryChange(mcPlayer, PlayerInventoryTransaction.EventCreator.STANDARD);
        try (final EffectTransactor ignored = BroadcastInventoryChangesEffect.transact(transactor)) {
            mcPlayer.inventoryMenu.broadcastChanges(); // in case plugins modified it
        }
    }

    @Redirect(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getCustomBossEvents()Lnet/minecraft/server/bossevents/CustomBossEvents;"))
    private CustomBossEvents impl$getPerWorldBossBarManager(final MinecraftServer minecraftServer, final net.minecraft.server.level.ServerPlayer playerIn) {
        return ((ServerLevelBridge) playerIn.serverLevel()).bridge$getBossBarManager();
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void impl$RemovePlayerReferenceFromScoreboard(final net.minecraft.server.level.ServerPlayer player, final CallbackInfo ci) {
        ((ServerScoreboardBridge) ((ServerPlayer) player).scoreboard()).bridge$removePlayer(player, false);
        SpongeAdventure.forEachBossBar(bar -> bar.removePlayer(player));
    }

    @Redirect(method = "addWorldborderListener",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/border/WorldBorder;addListener(Lnet/minecraft/world/level/border/BorderChangeListener;)V"
        )
    )
    private void impl$usePerWorldBorderListener(final WorldBorder worldBorder, final BorderChangeListener listener, final ServerLevel serverWorld) {
        worldBorder.addListener(new PerWorldBorderListener(serverWorld));
    }

    @Redirect(method = "load",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;load(Lnet/minecraft/nbt/CompoundTag;)V"
        )
    )
    private void impl$setSpongePlayerDataForSinglePlayer(final net.minecraft.server.level.ServerPlayer entity, final CompoundTag compound) {
        entity.load(compound);

        if (((ServerPlayer) entity).get(Keys.FIRST_DATE_JOINED).isEmpty()) {
            ((SpongeServer) this.shadow$getServer()).getPlayerDataManager().readLegacyPlayerData((ServerPlayer) entity, compound, null);
        }

        ((ServerPlayer) entity).offer(Keys.LAST_DATE_PLAYED, Instant.now());
    }

    @Inject(method = "respawn",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;)Lnet/minecraft/world/level/portal/TeleportTransition;"
        )
    )
    private void impl$flagIfRespawnPositionIsGameMechanic(final net.minecraft.server.level.ServerPlayer $$0, final boolean $$1,
            final Entity.RemovalReason $$2, final CallbackInfoReturnable<net.minecraft.server.level.ServerPlayer> cir) {
        this.impl$isRespawnWithPosition = $$0.getRespawnPosition() != null;
    }

    @Inject(method = "respawn", at = @At(value = "HEAD"))
    private void impl$callRespawnPlayerSelectWorld(final net.minecraft.server.level.ServerPlayer player, final boolean $$1,
            final Entity.RemovalReason $$2, final CallbackInfoReturnable<net.minecraft.server.level.ServerPlayer> cir) {
        this.impl$originalRespawnDestination = player.getRespawnDimension();
    }

    @Redirect(
        method = "respawn",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getX()D"),
        slice = @Slice(
            from = @At(value = "NEW", target = "(Lnet/minecraft/network/protocol/game/CommonPlayerSpawnInfo;B)Lnet/minecraft/network/protocol/game/ClientboundRespawnPacket;"),
            to = @At(value = "NEW", target = "(Lnet/minecraft/core/BlockPos;F)Lnet/minecraft/network/protocol/game/ClientboundSetDefaultSpawnPositionPacket;")
        )
    )
    private double impl$callRespawnPlayerRecreateEvent(final net.minecraft.server.level.ServerPlayer newPlayer,
            final net.minecraft.server.level.ServerPlayer player, final boolean keepAllPlayerData) {
        final ServerPlayer originalPlayer = (ServerPlayer) player;
        final ServerPlayer recreatedPlayer = (ServerPlayer) newPlayer;

        final Vector3d originalPosition = originalPlayer.position();
        final Vector3d destinationPosition = recreatedPlayer.position();
        final ServerWorld originalWorld = originalPlayer.world();
        final ServerWorld destinationWorld = recreatedPlayer.world();

        final RespawnPlayerEvent.Recreate event = SpongeEventFactory.createRespawnPlayerEventRecreate(PhaseTracker.getCauseStackManager().currentCause(),
                destinationPosition, originalWorld, originalPosition, destinationWorld,
                (ServerWorld) this.server.getLevel(this.impl$originalRespawnDestination),
                destinationPosition, originalPlayer, recreatedPlayer, this.impl$isRespawnWithPosition, !keepAllPlayerData);
        SpongeCommon.post(event);

        this.impl$isRespawnWithPosition = false;
        newPlayer.setPos(event.destinationPosition().x(), event.destinationPosition().y(), event.destinationPosition().z());

        if (ShouldFire.ROTATE_ENTITY_EVENT) {
            try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
                frame.pushCause(event);

                final RotateEntityEvent rotateEvent = SpongeEventFactory.createRotateEntityEvent(
                    frame.currentCause(), recreatedPlayer, originalPlayer.rotation(), recreatedPlayer.rotation());
                if (SpongeCommon.post(rotateEvent)) {
                    newPlayer.setXRot(player.getXRot());
                    newPlayer.setYRot(player.getYRot());
                } else {
                    newPlayer.setXRot((float) rotateEvent.toRotation().x());
                    newPlayer.setYRot((float) rotateEvent.toRotation().y());
                }
            }
        }

        return newPlayer.getX();
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    private void impl$callRespawnPlayerPostEvent(final net.minecraft.server.level.ServerPlayer player, final boolean $$1, final Entity.RemovalReason $$2,
            final CallbackInfoReturnable<net.minecraft.server.level.ServerPlayer> cir) {
        final ServerPlayer recreatedPlayer = (ServerPlayer) cir.getReturnValue();
        final ServerWorld originalWorld = (ServerWorld) player.serverLevel();

        final RespawnPlayerEvent.Post event = SpongeEventFactory.createRespawnPlayerEventPost(PhaseTracker.getCauseStackManager().currentCause(),
                recreatedPlayer.world(), originalWorld, (ServerWorld) this.server.getLevel(this.impl$originalRespawnDestination), recreatedPlayer);
        SpongeCommon.post(event);

        this.impl$originalRespawnDestination = null;
    }

    @Redirect(method = "sendLevelInfo", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;overworld()Lnet/minecraft/server/level/ServerLevel;"))
    private ServerLevel impl$usePerWorldWorldBorder(final MinecraftServer minecraftServer, final net.minecraft.server.level.ServerPlayer playerIn,
            final ServerLevel worldIn) {
        return worldIn;
    }

    private void impl$disconnectClient(final Connection netManager, final Component disconnectMessage, final @Nullable GameProfile profile) {
        final net.minecraft.network.chat.Component reason = SpongeAdventure.asVanilla(disconnectMessage);

        try {
            PlayerListMixin.LOGGER.info("Disconnecting " + (profile != null ? profile.toString() + " (" + netManager.getRemoteAddress().toString() + ")" :
                    netManager.getRemoteAddress() + ": " + reason.getString()));
            netManager.send(new ClientboundDisconnectPacket(reason));
            netManager.disconnect(reason);
        } catch (final Exception exception) {
            PlayerListMixin.LOGGER.error("Error whilst disconnecting player", exception);
        }
    }

    @Inject(method = "saveAll()V", at = @At("RETURN"))
    private void impl$saveDirtyUsersOnSaveAll(final CallbackInfo ci) {
        ((SpongeServer) SpongeCommon.server()).userManager().saveDirtyUsers();
    }

    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Ljava/util/function/Function;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void impl$onBroadcastSystemMessage(final net.minecraft.network.chat.Component $$0,
            final Function<net.minecraft.server.level.ServerPlayer, net.minecraft.network.chat.Component> $$1, final boolean $$2, final CallbackInfo ci) {
        if (this.impl$isDuringSystemMessageEvent) {
            return;
        }
        ci.cancel();

        final Audience originalAudience = (Audience) this.server;
        final Component originalMessage = SpongeAdventure.asAdventure($$0);
        final SystemMessageEvent event = SpongeEventFactory.createSystemMessageEvent(Sponge.server().causeStackManager().currentCause(),
                    originalAudience, Optional.of(originalAudience), originalMessage, originalMessage);
        if (SpongeCommon.post(event)) {
            ci.cancel();
            return;
        }

        this.impl$isDuringSystemMessageEvent = true;
        event.audience().ifPresent(audience -> audience.sendMessage(event.message()));
        this.impl$isDuringSystemMessageEvent = false;
    }

    @Redirect(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V"))
    private void impl$onBroadcastChatMessage1(final PlayerList instance, final PlayerChatMessage $$0,
            final Predicate<net.minecraft.server.level.ServerPlayer> $$1, final net.minecraft.server.level.ServerPlayer $$2,
            final ChatType.Bound $$4) {
        this.impl$onBroadcastChatMessage($$0, $$1, $$2, $$4);
    }

    @Redirect(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V"))
    private void impl$onBroadcastChatMessage2(final PlayerList instance, final PlayerChatMessage $$0,
            final Predicate<net.minecraft.server.level.ServerPlayer> $$1, final net.minecraft.server.level.ServerPlayer $$2,
            final ChatType.Bound $$4) {
        this.impl$onBroadcastChatMessage($$0, $$1, $$2, $$4);
    }

    private void impl$onBroadcastChatMessage(final PlayerChatMessage $$0, final Predicate<net.minecraft.server.level.ServerPlayer> $$1,
            final net.minecraft.server.level.ServerPlayer $$2, final ChatType.Bound $$4) {

        final boolean isTrusted = this.shadow$verifyChatTrusted($$0);

        final Component content = SpongeAdventure.asAdventure($$0.decoratedContent());
        final Component sender = SpongeAdventure.asAdventure($$4.name());
        final Component target = $$4.targetName() == null ? null : SpongeAdventure.asAdventure($$4.targetName());
        final Registry<ChatType> chatTypeRegistry = SpongeCommon.vanillaRegistry(Registries.CHAT_TYPE);
        final var chatType = RegistryTypes.CHAT_TYPE.defaultReferenced((org.spongepowered.api.ResourceKey) (Object) chatTypeRegistry.getKey($$4.chatType().value()));

        final Predicate<net.minecraft.server.level.ServerPlayer> filter;
        ChatType.Bound boundChatType;

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getServerInstanceExplicitly().pushCauseFrame()) {
            if ($$2 != null) {
                frame.pushCause($$2);
            }

            final PlayerChatEvent.Submit event = SpongeEventFactory.createPlayerChatEventSubmit(frame.currentCause(), content, content, chatType,
                    Optional.empty(), Optional.ofNullable((ServerPlayer) $$2), sender, Optional.ofNullable(target), isTrusted);
            if (SpongeCommon.post(event)) {
                return; // Do nothing when canceled or audience removed
            }
            boundChatType = ChatType.bind(ResourceKey.create(Registries.CHAT_TYPE, (ResourceLocation) (Object) event.chatType().location()),
                    this.server.registryAccess(), SpongeAdventure.asVanilla(event.sender()));
            boundChatType = event.target().map(SpongeAdventure::asVanilla).map(boundChatType::withTargetName).orElse(boundChatType);

            filter = event.filter().map(f -> $$1.and((Predicate) f)).orElse($$1);
            if (event.message() != event.originalMessage()) {
                final net.minecraft.network.chat.Component customMessage = SpongeAdventure.asVanilla(event.message());
                // It works!
                // If the message is changed, the player will be shown the standard message change notification for such situations.
                // The value of `isTrusted` will always be `true` when `online-mode=true`.
                // If `online-mode=false`, then `isTrusted` will also have a value of `false`.
                // When `online-mode=false`, the player is not shown a notification when the server changes the message.
                var modifiedMessage =  new PlayerChatMessage($$0.link(), $$0.signature(), $$0.signedBody(), customMessage, $$0.filterMask());
                this.shadow$broadcastChatMessage(modifiedMessage, filter, $$2, boundChatType);
                return;
            }
        }

        this.shadow$broadcastChatMessage($$0, filter, $$2, boundChatType);
    }

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void impl$onSave(final net.minecraft.server.level.ServerPlayer player, final CallbackInfo ci) {
        if (((TransientBridge) player).bridge$isTransient()) {
            ci.cancel();
        }
    }

    @Redirect(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hasExactlyOnePlayerPassenger()Z"))
    private boolean impl$skipUnserializableRootVehicle(final Entity instance) {
        //Intentionally only checking for the entity type and
        //not for the TRANSIENT key. This ensures that arbitrary
        //entities are removed with the player but players are ignored.
        return instance.hasExactlyOnePlayerPassenger() && instance.getType().canSerialize();
    }
}
