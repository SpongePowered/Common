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
package org.spongepowered.common.mixin.core.server.level;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Either;
import io.netty.channel.Channel;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.Audiences;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.SkinPart;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.chat.ChatVisibility;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.data.ChangeDataHolderEvent;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.RotateEntityEvent;
import org.spongepowered.api.event.entity.living.player.KickPlayerEvent;
import org.spongepowered.api.event.entity.living.player.PlayerChangeClientSettingsEvent;
import org.spongepowered.api.event.entity.living.player.RespawnPlayerEvent;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.locale.Locales;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.network.ConnectionAccessor;
import org.spongepowered.common.accessor.server.level.ChunkMapAccessor;
import org.spongepowered.common.accessor.server.level.ChunkMap_TrackedEntityAccessor;
import org.spongepowered.common.accessor.server.network.ServerCommonPacketListenerImplAccessor;
import org.spongepowered.common.accessor.world.level.portal.TeleportTransitionAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.data.DataCompoundHolder;
import org.spongepowered.common.bridge.data.TransientBridge;
import org.spongepowered.common.bridge.permissions.SubjectBridge;
import org.spongepowered.common.bridge.server.ServerScoreboardBridge;
import org.spongepowered.common.bridge.server.level.ServerPlayerBridge;
import org.spongepowered.common.bridge.world.BossEventBridge;
import org.spongepowered.common.bridge.world.entity.player.PlayerBridge;
import org.spongepowered.common.data.DataUtil;
import org.spongepowered.common.data.type.SpongeSkinPart;
import org.spongepowered.common.entity.player.ClientType;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.entity.EntityPhase;
import org.spongepowered.common.event.tracking.phase.entity.TeleportContext;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.mixin.core.world.entity.player.PlayerMixin;
import org.spongepowered.common.network.packet.SpongePacketHandler;
import org.spongepowered.common.util.LocaleCache;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.border.PlayerOwnBorderListener;
import org.spongepowered.math.vector.Vector3d;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

// See also: SubjectMixin_API and SubjectMixin
@SuppressWarnings("ConstantConditions")
@Mixin(net.minecraft.server.level.ServerPlayer.class)
public abstract class ServerPlayerMixin extends PlayerMixin implements SubjectBridge, ServerPlayerBridge {

    // @formatter:off
    @Shadow public ServerGamePacketListenerImpl connection;
    @Shadow @Final public ServerPlayerGameMode gameMode;
    @Shadow @Final public MinecraftServer server;
    @Shadow private int lastRecordedExperience;
    @Shadow private boolean isChangingDimension;
    @Shadow private net.minecraft.world.phys.Vec3 enteredNetherPosition;
    @Shadow private int lastSentExp;
    @Shadow private float lastSentHealth;
    @Shadow private int lastSentFood;

    @Shadow public abstract ServerLevel shadow$serverLevel();
    @Shadow public abstract void shadow$resetStat(final Stat<?> statistic);
    @Shadow protected abstract void shadow$tellNeutralMobsThatIDied();
    @Shadow protected abstract void shadow$triggerDimensionChangeTriggers(ServerLevel serverworld);
    @Shadow public abstract void shadow$doCloseContainer();
    @Shadow public abstract boolean shadow$setGameMode(GameType param0);
    @Shadow public abstract void shadow$setCamera(@org.jetbrains.annotations.Nullable final Entity $$0);
    // @formatter:on

    private net.minecraft.network.chat.@Nullable Component impl$connectionMessage;
    private Locale impl$language = Locales.DEFAULT;
    private Scoreboard impl$scoreboard = Sponge.game().server().serverScoreboard().get();
    @Nullable private Boolean impl$keepInventory = null;
    // Used to restore original item received in a packet after canceling an event
    private int impl$viewDistance;
    private int impl$skinPartMask;
    private Set<SkinPart> impl$skinParts = ImmutableSet.of();
    private final PlayerOwnBorderListener impl$borderListener = new PlayerOwnBorderListener((net.minecraft.server.level.ServerPlayer) (Object) this);
    private boolean impl$sleepingIgnored;
    private boolean impl$noGameModeEvent;
    @Nullable private WorldBorder impl$worldBorder;
    private ServerLevel impl$respawnLevel;

    @Override
    public net.minecraft.network.chat.@Nullable Component bridge$getConnectionMessageToSend() {
        if (this.impl$connectionMessage == null) {
            return net.minecraft.network.chat.Component.literal("");
        }
        return this.impl$connectionMessage;
    }

    @Override
    public void bridge$setConnectionMessageToSend(final net.minecraft.network.chat.Component message) {
        this.impl$connectionMessage = message;
    }

    @Override
    public String bridge$getSubjectCollectionIdentifier() {
        return PermissionService.SUBJECTS_USER;
    }

    @Override
    public Tristate bridge$permDefault(final String permission) {
        return Tristate.FALSE;
    }

    @Override
    protected final boolean impl$setLocation(final boolean isChangeOfWorld, final ServerLevel level, final Vector3d pos) {
        if (this.shadow$isRemoved()) {
            return false;
        }
        final var thisPlayer = ((net.minecraft.server.level.ServerPlayer) (Object) this);

        final ChunkPos chunkPos = new ChunkPos(VecHelper.toBlockPos(pos));
        level.getChunkSource().addRegionTicket(TicketType.FORCED, chunkPos, 1, chunkPos);

        thisPlayer.stopRiding();
        if (thisPlayer.isSleeping()) {
            thisPlayer.stopSleepInBed(true, true);
        }

        if (!isChangeOfWorld) {
            this.connection.teleport(new PositionMoveRotation(VecHelper.toVanillaVector3d(pos), Vec3.ZERO, this.shadow$getYRot(), this.shadow$getXRot()), new HashSet<>());
            this.connection.resetPosition();
        } else {
            this.bridge$changeDimension(new TeleportTransition(level, VecHelper.toVanillaVector3d(pos), thisPlayer.getKnownMovement(),
                    this.shadow$getYRot(), this.shadow$getXRot(), TeleportTransition.DO_NOTHING));
        }
        return true;
    }

    @Override
    public void bridge$refreshExp() {
        this.lastRecordedExperience = -1;
    }

    @Override
    public boolean bridge$kick(final Component message) {
        final Component messageToSend;
        if (ShouldFire.KICK_PLAYER_EVENT) {
            final KickPlayerEvent kickEvent = SpongeEventFactory.createKickPlayerEvent(PhaseTracker.getCauseStackManager().currentCause(),
                message,
                message,
                (ServerPlayer) this
                );
            if (Sponge.eventManager().post(kickEvent)) {
                return false;
            }
            messageToSend = kickEvent.message();
        } else {
            messageToSend = message;
        }
        final net.minecraft.network.chat.Component component = SpongeAdventure.asVanilla(messageToSend);
        this.connection.disconnect(component);
        return true;
    }

    @Override
    public Locale bridge$getLanguage() {
        return this.impl$language;
    }

    @Override
    public void bridge$setLanguage(final Locale language) {
        this.impl$language = language;

        // Update locale on Channel, used for sending localized messages
        if (this.connection != null) {
            final Channel channel = ((ConnectionAccessor) ((ServerCommonPacketListenerImplAccessor) this.connection).accessor$connection()).accessor$channel();
            channel.attr(SpongeAdventure.CHANNEL_LOCALE).set(language);

            this.containerMenu.broadcastFullState();
        }
    }

    @Override
    public void bridge$initScoreboard() {
        ((ServerScoreboardBridge) this.shadow$getScoreboard()).bridge$addPlayer((net.minecraft.server.level.ServerPlayer) (Object) this, true);
    }

    @Override
    public void bridge$removeScoreboardOnRespawn() {
        ((ServerScoreboardBridge) ((ServerPlayer) this).scoreboard()).bridge$removePlayer((net.minecraft.server.level.ServerPlayer) (Object) this, false);
    }

    @Override
    public void bridge$setScoreboardOnRespawn(final Scoreboard scoreboard) {
        this.impl$scoreboard = scoreboard;
        ((ServerScoreboardBridge) ((ServerPlayer) this).scoreboard()).bridge$addPlayer((net.minecraft.server.level.ServerPlayer) (Object) this, false);
    }

    @Override
    public Scoreboard bridge$getScoreboard() {
        return this.impl$scoreboard;
    }

    @Override
    public void bridge$replaceScoreboard(@org.checkerframework.checker.nullness.qual.Nullable Scoreboard scoreboard) {
        if (scoreboard == null) {
            scoreboard = Sponge.game().server().serverScoreboard()
                    .orElseThrow(() -> new IllegalStateException("Server does not have a valid scoreboard"));
        }
        this.impl$scoreboard = scoreboard;
    }

    @Override
    public boolean bridge$keepInventory() {
        if (this.impl$keepInventory == null) {
            return this.shadow$serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        }
        return this.impl$keepInventory;
    }

    @Override
    public int bridge$getExperiencePointsOnDeath(final LivingEntity entity, final ServerLevel $$0, final Entity $$1) {
        if (this.impl$keepInventory != null && this.impl$keepInventory) {
            return 0;
        }
        return super.bridge$getExperiencePointsOnDeath(entity, $$0, $$1);
    }

    @Override
    public int bridge$getViewDistance() {
        return this.impl$viewDistance;
    }

    @Override
    public Set<SkinPart> bridge$getSkinParts() {
        final int mask = this.shadow$getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION);
        if (this.impl$skinPartMask != mask) {
            this.impl$skinParts = Sponge.game().registry(RegistryTypes.SKIN_PART).stream()
                    .map(part -> (SpongeSkinPart) part)
                    .filter(part -> part.test(mask))
                    .collect(ImmutableSet.toImmutableSet());
            this.impl$skinPartMask = mask;
        }

        return this.impl$skinParts;
    }

    @Override
    public void bridge$setSkinParts(final Set<SkinPart> skinParts) {
        int mask = 0;
        for (final SkinPart part : skinParts) {
            mask |= ((SpongeSkinPart) part).getMask();
        }

        this.shadow$getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) mask);
        this.impl$skinParts = ImmutableSet.copyOf(skinParts);
        this.impl$skinPartMask = mask;
    }

    @Override
    public boolean bridge$sleepingIgnored() {
        return this.impl$sleepingIgnored;
    }

    @Override
    public void bridge$setSleepingIgnored(final boolean sleepingIgnored) {
        this.impl$sleepingIgnored = sleepingIgnored;
    }

    @Override
    public void bridge$sendSpongePacketToViewer(final org.spongepowered.api.network.channel.packet.Packet packet) {
        if (this.impl$isFake) {
            return;
        }
        final ClientType clientType = this.bridge$getClientType();
        if (clientType == ClientType.SPONGE_VANILLA || clientType == ClientType.SPONGE_FORGE) {
            SpongePacketHandler.getChannel().sendTo((ServerPlayer) this, packet);
        }
    }

    @Override
    public void bridge$sendToViewer(final Packet<ClientGamePacketListener> packet) {
        if (this.impl$isFake) {
            return;
        }
        this.connection.send(packet);
    }

    /*
    @Inject(method = "markPlayerActive()V", at = @At("HEAD"))
    private void impl$onPlayerActive(final CallbackInfo ci) {
        ((ServerPlayNetHandlerBridge) this.connection).bridge$resendLatestResourcePackRequest();
    }
*/

    /**
     * @author faithcaio - 2024-05-30 - MC 1.21
     * @reason Redirect all teleports through {@link #bridge$changeDimension} to fire our move/rotate/teleport events
     */
    @Overwrite
    public boolean teleportTo(final ServerLevel world,
                              final double x,
                              final double y,
                              final double z,
                              Set<Relative> relative,
                              final float yaw,
                              final float pitch,
                              final boolean setCamera) {
        if (setCamera) {
            this.shadow$setCamera((net.minecraft.server.level.ServerPlayer) (Object) this);
        }
        final boolean hasMovementContext = PhaseTracker.getCauseStackManager().currentContext().containsKey(EventContextKeys.MOVEMENT_TYPE);
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            if (!hasMovementContext) {
                frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.PLUGIN);
            }

            return this.bridge$changeDimension(new TeleportTransition(world, new Vec3(x, y, z), Vec3.ZERO, yaw, pitch, relative,
                    e -> world.getChunkSource().addRegionTicket(TicketType.FORCED, e.chunkPosition(), 1, e.chunkPosition()))) != null;
        }
    }

    /**
     * This is effectively an overwrite of changeDimension and teleportTo.
     * Handles {@link MoveEntityEvent} -> {@link RotateEntityEvent} for in dimension teleport
     * Handles {@link ChangeEntityWorldEvent.Reposition} -> rotate -> {@link ChangeEntityWorldEvent.Post}
     * {@link ChangeEntityWorldEvent.Pre} is handled at call sites. TODO only for known portals?
     * For known portals {@link ChangeEntityWorldEvent.Reposition} is handled before at {@link Entity#handlePortal()}
     *
     * @return The {@link Entity} that is either this one, or replaces this one
     */
    @Override
    public net.minecraft.server.level.@Nullable ServerPlayer bridge$changeDimension(final TeleportTransition originalTransition) {
        if (this.shadow$isRemoved()) {
            return null;
        }

        final var thisPlayer = (net.minecraft.server.level.ServerPlayer) (Object) this;

        if (originalTransition.missingRespawnBlock()) { // Player only code
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
        }

        final var originalNewLevel = originalTransition.newLevel();
        final var oldLevel = this.shadow$serverLevel();

        // SpongeStart
        final var transition = this.impl$fireDimensionTransitionEvents(originalTransition, thisPlayer);
        if (transition == null) {
            return null;
        }
        final var newLevel = transition.newLevel();
        // Sponge End

        if (newLevel.dimension() == oldLevel.dimension()) { // actually no dimension change
             this.connection.teleport(transition.position().x, transition.position().y, transition.position().z, transition.yRot(), transition.xRot());
             this.connection.resetPosition();
            transition.postTeleportTransition().onTransition(thisPlayer);
            // TODO setYHeadRot after would rotate event result
            return thisPlayer;
        }
        this.isChangingDimension = true;
        LevelData lvlData = newLevel.getLevelData();
        this.connection.send(new ClientboundRespawnPacket(thisPlayer.createCommonSpawnInfo(newLevel), ClientboundRespawnPacket.KEEP_ALL_DATA));
        this.connection.send(new ClientboundChangeDifficultyPacket(lvlData.getDifficulty(), lvlData.isDifficultyLocked()));
        PlayerList playerList = this.server.getPlayerList();
        playerList.sendPlayerPermissionLevel(thisPlayer);
        oldLevel.removePlayerImmediately(thisPlayer, Entity.RemovalReason.CHANGED_DIMENSION);
        this.shadow$unsetRemoved();

        final ProfilerFiller filler = Profiler.get();
        filler.push("moving");
        if (oldLevel.dimension() == Level.OVERWORLD && newLevel.dimension() == Level.NETHER) {
            this.enteredNetherPosition = thisPlayer.position();
        }
        filler.pop();

        filler.push("placing");
        thisPlayer.setServerLevel(newLevel);
        this.connection.teleport(transition.position().x, transition.position().y, transition.position().z, transition.yRot(), transition.xRot());
        this.connection.resetPosition();
        newLevel.addDuringTeleport(thisPlayer);
        filler.pop();

        this.shadow$triggerDimensionChangeTriggers(oldLevel); // TODO old sponge EntityUtil#performPostChangePlayerWorldLogic this was only done when using a portal
        this.connection.send(new ClientboundPlayerAbilitiesPacket(thisPlayer.getAbilities()));
        playerList.sendLevelInfo(thisPlayer, newLevel);
        playerList.sendAllPlayerInfo(thisPlayer);
        playerList.sendActivePlayerEffects(thisPlayer);
        transition.postTeleportTransition().onTransition(thisPlayer);
        // TODO old sponge EntityUtil#performPostChangePlayerWorldLogic called bridge$getBossBarManager().onPlayerDisconnect(player); on both worlds
        // TODO old sponge EntityUtil#performPostChangePlayerWorldLogic closed player.closeContainer(); when open
        this.lastSentExp = -1;
        this.lastSentHealth = -1.0F;
        this.lastSentFood = -1;
        // Sponge Start TODO cause/context like in impl$fireDimensionTransitionEvents
        Sponge.eventManager().post(
                SpongeEventFactory.createChangeEntityWorldEventPost(
                        PhaseTracker.getCauseStackManager().currentCause(),
                        (org.spongepowered.api.entity.Entity) this,
                        (ServerWorld) oldLevel,
                        (ServerWorld) newLevel,
                        (ServerWorld) originalNewLevel
                )
        );
        // Sponge End
        return thisPlayer;
    }

    @Nullable
    private TeleportTransition impl$fireDimensionTransitionEvents(final TeleportTransition originalTransition,
            final net.minecraft.server.level.ServerPlayer thisPlayer) {
        var transition = originalTransition;
        var isDimensionChange = transition.newLevel() != thisPlayer.serverLevel();

        if (!this.impl$moveEventsFired) {
            final PhaseTracker phaseTracker = PhaseTracker.getWorldInstance(thisPlayer.serverLevel());
            final var contextToSwitchTo = EntityPhase.State.PORTAL_DIMENSION_CHANGE.createPhaseContext(phaseTracker).worldChange()
                    .player();
            final boolean hasMovementContext = phaseTracker.currentContext().containsKey(EventContextKeys.MOVEMENT_TYPE);
            try (final TeleportContext context = contextToSwitchTo.buildAndSwitch();
                    final CauseStackManager.StackFrame frame = phaseTracker.pushCauseFrame()) {
                frame.pushCause(thisPlayer);
                if (!hasMovementContext) {
                    // TODO we should be able to detect normal plugin code though
                    // add an unknown movement type?
                    frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.PLUGIN);
                }

                final var originalDest = VecHelper.toVector3d(transition.position());
                final @Nullable Vector3d newDest;

                if (isDimensionChange) {
                    if (transition.newLevel() != thisPlayer.level()) {
                        final ChangeEntityWorldEvent.Pre preEvent = PlatformHooks.INSTANCE.getEventHooks().callChangeEntityWorldEventPre(thisPlayer, transition.newLevel());
                        if (SpongeCommon.post(preEvent)) {
                            return null;
                        }
                        if (preEvent.destinationWorld() != preEvent.originalDestinationWorld()) {
                            transition = new TeleportTransition((ServerLevel) preEvent.destinationWorld(),
                                    transition.position(),
                                    transition.deltaMovement(),
                                    transition.yRot(), transition.xRot(),
                                    transition.missingRespawnBlock(),
                                    transition.asPassenger(),
                                    EnumSet.noneOf(Relative.class),
                                    transition.postTeleportTransition());
                        }
                    }

                    final var reposition = this.bridge$fireRepositionEvent((ServerWorld) thisPlayer.serverLevel(), (ServerWorld) transition.newLevel(), originalDest);
                    if (reposition.isCancelled()) {
                        return null; // we did not move yet so just return
                    }
                    newDest = reposition.destinationPosition();
                } else {
                    if (ShouldFire.MOVE_ENTITY_EVENT) { // TODO move into impl$fireMoveEvent?
                        newDest = this.impl$fireMoveEvent(phaseTracker, originalDest);
                        if (newDest == null) {
                            return null;
                        }
                    } else {
                        newDest = originalDest;
                    }
                }
                if (newDest != originalDest) {
                    // if changed override the DimensionTransition
                    transition = new TeleportTransition(transition.newLevel(),
                            VecHelper.toVanillaVector3d(newDest),
                            transition.deltaMovement(),
                            transition.yRot(), transition.xRot(),
                            transition.missingRespawnBlock(),
                            transition.asPassenger(),
                        EnumSet.noneOf(Relative.class),
                            transition.postTeleportTransition());
                }

                final Vector3d toRot = new Vector3d(transition.xRot(), transition.yRot(), 0);
                final Vector3d fromRot = new Vector3d(thisPlayer.getXRot(), thisPlayer.getYRot(), 0);
                // TODO this skips with fuzzy rotation change check. Do we want this?
                var newToRot = SpongeCommonEventFactory.callRotateEvent((org.spongepowered.api.entity.Entity) thisPlayer, fromRot, toRot);
                if (newToRot == null) {
                    newToRot = fromRot; // Cancelled Rotate - Reset to original rotation
                }
                if (toRot != newToRot) {
                    transition = new TeleportTransition(transition.newLevel(),
                            transition.position(),
                            transition.deltaMovement(),
                            (float) newToRot.y(), (float) newToRot.x(),
                            transition.missingRespawnBlock(),
                            transition.asPassenger(),
                            EnumSet.noneOf(Relative.class),
                            transition.postTeleportTransition());
                }

            }

        }

        thisPlayer.setCamera(thisPlayer);
        thisPlayer.stopRiding();
        if (thisPlayer.isSleeping()) {
            thisPlayer.stopSleepInBed(true, true);
        }

        return transition;
    }

    @Redirect(
            method = {"openMenu", "openHorseInventory"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;closeContainer()V"
            )
    )
    private void impl$closePreviousContainer(final net.minecraft.server.level.ServerPlayer self) {
        this.shadow$doCloseContainer();
    }

    /**
     * @author blood - May 12th, 2016
     * @author gabizou - June 3rd, 2016
     * @author gabizou - February 22nd, 2020 - Minecraft 1.14.3
     * @reason SpongeForge requires an overwrite so we do it here instead. This handles player death events.
     */
    @Overwrite
    public void die(final DamageSource cause) {
        // Sponge start - Call Destruct Death Event
        if (PlatformHooks.INSTANCE.getEventHooks().callPlayerDestruction((net.minecraft.server.level.ServerPlayer) (Object) this, cause)) {
            return;
        }
        final DestructEntityEvent.Death event = SpongeCommonEventFactory.callDestructEntityEventDeath((net.minecraft.server.level.ServerPlayer) (Object) this, cause,
                Audiences.server());
        if (event.isCancelled()) {
            return;
        }
        // Sponge end

        final var level = this.shadow$serverLevel();
        final boolean flag = level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES) && !event.isMessageCancelled();
        if (flag) {
            final net.minecraft.network.chat.Component component = this.shadow$getCombatTracker().getDeathMessage();
            final ClientboundPlayerCombatKillPacket packet = new ClientboundPlayerCombatKillPacket(this.shadow$getId(), component);
            this.connection.send(packet, PacketSendListener.exceptionallySend(() -> {
                final String s = component.getString(256);
                final net.minecraft.network.chat.Component itextcomponent1 = net.minecraft.network.chat.Component.translatable("death.attack.message_too_long", net.minecraft.network.chat.Component.literal(s).withStyle(ChatFormatting.YELLOW));
                final net.minecraft.network.chat.Component itextcomponent2 = net.minecraft.network.chat.Component.translatable("death.attack.even_more_magic", this.shadow$getDisplayName())
                        .withStyle((p_212357_1_) -> p_212357_1_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, itextcomponent1)));
                return new ClientboundPlayerCombatKillPacket(this.shadow$getId(), itextcomponent2);
            }));
            final Team team = this.shadow$getTeam();
            if (team != null && team.getDeathMessageVisibility() != Team.Visibility.ALWAYS) {
                if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OTHER_TEAMS) {
                    this.server.getPlayerList().broadcastSystemToTeam(
                            (net.minecraft.server.level.ServerPlayer) (Object) this, component);
                } else if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OWN_TEAM) {
                    this.server.getPlayerList().broadcastSystemToAllExceptTeam(
                            (net.minecraft.server.level.ServerPlayer) (Object) this, component);
                }
            } else {
                final Component message = event.message();
                // Sponge start - use the event audience
                if (message != Component.empty()) {
                    event.audience().ifPresent(eventChannel -> eventChannel.sendMessage(Identity.nil(), message));
                }
                // Sponge end
                // this.server.getPlayerList().sendMessage(itextcomponent);
            }
        } else {
            this.connection.send(
                    new ClientboundPlayerCombatKillPacket(this.shadow$getId(), net.minecraft.network.chat.Component.empty()));
        }

        this.shadow$removeEntitiesOnShoulder();
        if (level.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            this.shadow$tellNeutralMobsThatIDied();
        }

        // Sponge Start - update the keep inventory flag for dropping inventory
        // during the death update ticks
        this.impl$keepInventory = event.keepInventory();

        if (!this.shadow$isSpectator() && level instanceof ServerLevel sLevel) {
            this.shadow$dropAllDeathLoot(sLevel, cause);
        }
        // Sponge End

        this.shadow$getScoreboard().forAllObjectives(
                ObjectiveCriteria.DEATH_COUNT, (net.minecraft.server.level.ServerPlayer) (Object) this, sa -> sa.set(sa.get() + 1));
        final LivingEntity livingentity = this.shadow$getKillCredit();
        if (livingentity != null) {
            this.shadow$awardStat(Stats.ENTITY_KILLED_BY.get(livingentity.getType()));
            livingentity.awardKillScore((net.minecraft.server.level.ServerPlayer) (Object) this, cause);
            this.shadow$createWitherRose(livingentity);
        }

        level.broadcastEntityEvent((net.minecraft.server.level.ServerPlayer) (Object) this, (byte) 3);
        this.shadow$awardStat(Stats.DEATHS);
        this.shadow$resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
        this.shadow$resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        this.shadow$clearFire();
        this.shadow$setSharedFlag(0, false);
        this.shadow$getCombatTracker().recheckStatus();
    }

    @Redirect(method = "restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean tracker$useKeepFromBridge(final GameRules gameRules, final GameRules.Key<?> key,
            final net.minecraft.server.level.ServerPlayer corpse, final boolean keepEverything) {
        final boolean keep = ((PlayerBridge) corpse).bridge$keepInventory(); // Override Keep Inventory GameRule?
        if (!keep) {
            // Copy corpse inventory to respawned player
            this.shadow$getInventory().replaceWith(corpse.getInventory());
            // Clear corpse so that mods do not copy from it again
            corpse.getInventory().clearContent();
        }
        return keep;
    }

    @Inject(method = "restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V", at = @At("HEAD"))
    private void impl$copyDataOnRespawn(final net.minecraft.server.level.ServerPlayer oldPlayer, final boolean respawnFromEnd, final CallbackInfo ci) {
        // Copy Sponge data
        if (oldPlayer instanceof DataCompoundHolder) {
            final DataCompoundHolder oldEntity = (DataCompoundHolder) oldPlayer;
            DataUtil.syncDataToTag(oldEntity);
            final CompoundTag compound = oldEntity.data$getCompound();
            ((DataCompoundHolder) this).data$setCompound(compound);
            DataUtil.syncTagToData(this);
        }

        this.impl$language = ((ServerPlayerBridge) oldPlayer).bridge$getLanguage();
        this.impl$viewDistance = ((ServerPlayerBridge) oldPlayer).bridge$getViewDistance();

        // Update boss bars
        SpongeAdventure.forEachBossBar(bar -> ((BossEventBridge) bar).bridge$replacePlayer(oldPlayer, (net.minecraft.server.level.ServerPlayer) (Object) this));

        ((ServerPlayerBridge) oldPlayer).bridge$removeScoreboardOnRespawn();
        ((ServerPlayerBridge) this).bridge$setScoreboardOnRespawn(((ServerPlayer) oldPlayer).scoreboard());
    }

    @SuppressWarnings({"ConstantConditions", "UnstableApiUsage"})
    @Inject(method = "updateOptions", at = @At("HEAD"))
    private void impl$handleClientSettings(final ClientInformation info, final CallbackInfo ci) {
        if (!ShouldFire.PLAYER_CHANGE_CLIENT_SETTINGS_EVENT) {
            return;
        }

        final Locale newLocale = LocaleCache.getLocale(info.language());

        final ImmutableSet<SkinPart> skinParts = Sponge.game().registry(RegistryTypes.SKIN_PART).stream()
                .map(part -> (SpongeSkinPart) part)
                .filter(part -> part.test(info.modelCustomisation()))
                .collect(ImmutableSet.toImmutableSet());
        final int viewDistance = info.viewDistance();

        // Post before the player values are updated
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            final ChatVisibility visibility = (ChatVisibility) (Object) info.chatVisibility();
            final PlayerChangeClientSettingsEvent event = SpongeEventFactory.createPlayerChangeClientSettingsEvent(
                    frame.currentCause(),
                    visibility,
                    skinParts,
                    newLocale,
                    (ServerPlayer) this,
                    info.chatColors(),
                    viewDistance);
            SpongeCommon.post(event);
        }
    }

    @Inject(method = "updateOptions", at = @At("TAIL"))
    private void impl$updateTrackedClientSettings(final ClientInformation info, final CallbackInfo ci) {
        final Locale newLocale = LocaleCache.getLocale(info.language());

        // Update the fields we track ourselves
        this.impl$viewDistance = info.viewDistance();
        this.bridge$setLanguage(newLocale);
        this.impl$language = newLocale;
    }

    @Override
    public PlayerOwnBorderListener bridge$getWorldBorderListener() {
        return this.impl$borderListener;
    }

    @Inject(method = "sendSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",
            cancellable = true, at = @At("HEAD"))
    public void sendMessage(final net.minecraft.network.chat.Component $$0, final boolean $$1, final CallbackInfo ci) {
        if (this.impl$isFake) {
            // Don't bother sending messages to fake players
            ci.cancel();
        }
    }

    @Inject(method = "sendChatMessage", cancellable = true, at = @At("HEAD"))
    public void sendMessage(final OutgoingChatMessage $$0, final boolean $$1, final ChatType.Bound $$2, final CallbackInfo ci) {
        if (this.impl$isFake) {
            // Don't bother sending messages to fake players
            ci.cancel();
        }
    }

    @Override
    public net.minecraft.world.scores.Scoreboard shadow$getScoreboard() {
        return (net.minecraft.world.scores.Scoreboard) this.impl$scoreboard;
    }

    @Override
    protected void impl$onRightClickEntity(
        final Entity entityToInteractOn, final InteractionHand hand, final CallbackInfoReturnable<InteractionResult> cir
    ) {
        final ItemStack itemInHand = this.shadow$getItemInHand(hand);
        final InteractEntityEvent.Secondary event = SpongeCommonEventFactory.callInteractEntityEventSecondary((net.minecraft.server.level.ServerPlayer) (Object) this,
                itemInHand, entityToInteractOn, hand, null);
        if (event.isCancelled()) {
            this.containerMenu.sendAllDataToRemote();
            if (itemInHand.getItem() == Items.LEAD && entityToInteractOn instanceof Mob) {
                this.connection.send(new ClientboundSetEntityLinkPacket(entityToInteractOn, ((Mob) entityToInteractOn).getLeashHolder()));
            } else if (itemInHand.getItem() == Items.WATER_BUCKET && entityToInteractOn instanceof AbstractFish) {
                final ChunkMap_TrackedEntityAccessor trackerAccessor = ((ChunkMapAccessor) ((ServerWorld) this.shadow$level()).chunkManager()).accessor$entityMap().get(entityToInteractOn.getId());
                if (trackerAccessor != null) {
                    trackerAccessor.accessor$getServerEntity().sendPairingData((net.minecraft.server.level.ServerPlayer) (Object) this, this.connection::send);
                }
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Override
    public PlayerTeam shadow$getTeam() {
        return ((net.minecraft.world.scores.Scoreboard) this.impl$scoreboard).getPlayersTeam(this.shadow$getScoreboardName());
    }

    @Inject(method = "startSleepInBed", at = @At(value = "RETURN"), cancellable = true)
    private void impl$onReturnSleep(final BlockPos param0, final CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        final Either<Player.BedSleepingProblem, Unit> returnValue = cir.getReturnValue();
        if (returnValue.left().isPresent()) {
            switch (returnValue.left().get()) {

                case NOT_POSSIBLE_HERE:
                case TOO_FAR_AWAY:
                case NOT_POSSIBLE_NOW:
                case OBSTRUCTED:
                case NOT_SAFE:
                    final Cause currentCause = Sponge.server().causeStackManager().currentCause();
                    final BlockSnapshot snapshot = ((ServerWorld) this.shadow$level()).createSnapshot(param0.getX(), param0.getY(), param0.getZ());
                    if (Sponge.eventManager().post(SpongeEventFactory.createSleepingEventFailed(currentCause, snapshot, (Living) this))) {
                        final Either<Player.BedSleepingProblem, Unit> var5 = super.shadow$startSleepInBed(param0).ifRight((param0x) -> {
                            this.shadow$awardStat(Stats.SLEEP_IN_BED);
                            CriteriaTriggers.SLEPT_IN_BED.trigger((net.minecraft.server.level.ServerPlayer) (Object) this);
                        });
                        ((ServerLevel) this.shadow$level()).updateSleepingPlayerList();
                        cir.setReturnValue(var5);
                    }
                    break;
                case OTHER_PROBLEM: // ignore
                    break;
            }
        }
    }

    @Override
    protected void impl$updateHealthForUseFinish(final CallbackInfo ci) {
        this.bridge$refreshScaledHealth();
    }

    @ModifyVariable(method = "setGameMode", at = @At(value = "HEAD"), argsOnly = true)
    private GameType impl$setGameMode(final GameType value) {
        if (!ShouldFire.CHANGE_DATA_HOLDER_EVENT_VALUE_CHANGE || Objects.equals(this.gameMode.getGameModeForPlayer(), value) || this.impl$noGameModeEvent) {
            return value;
        }

        final DataTransactionResult transaction = DataTransactionResult.builder()
                .replace(Value.immutableOf(Keys.GAME_MODE, (GameMode) (Object) this.gameMode.getGameModeForPlayer()))
                .success(Value.immutableOf(Keys.GAME_MODE, (GameMode) (Object) value))
                .result(DataTransactionResult.Type.SUCCESS)
                .build();

        final ChangeDataHolderEvent.ValueChange
                event =
                SpongeEventFactory.createChangeDataHolderEventValueChange(PhaseTracker.getCauseStackManager().currentCause(), transaction, (DataHolder.Mutable) this);

        Sponge.eventManager().post(event);

        if (event.isCancelled()) {
            return this.gameMode.getGameModeForPlayer();
        }

        return (GameType) (Object) event.endResult().successfulValue(Keys.GAME_MODE)
                .map(Value::get)
                .orElse((GameMode) (Object) value);
    }

    @Override
    public void bridge$setGameModeNoEvent(final GameType gameType) {
        try {
            this.impl$noGameModeEvent = true;
            this.shadow$setGameMode(gameType);
        } finally {
            this.impl$noGameModeEvent = false;
        }
    }

    @Override
    public @Nullable WorldBorder bridge$getWorldBorder() {
        return this.impl$worldBorder;
    }

    @Override
    public void bridge$replaceWorldBorder(final @Nullable WorldBorder border) {
        this.impl$worldBorder = border;
    }

    @Override
    public boolean bridge$isTransient() {
        return this.impl$transient;
    }

    /**
     * @author Zidane
     * @reason Have PVP check if the world allows it or not
     * @return True if PVP allowed
     */
    @Overwrite
    private boolean isPvpAllowed() {
        return ((ServerWorld) this.shadow$serverLevel()).properties().pvp();
    }

    /**
     * @author gabizou - 1.21.2-pre1
     * @reason We route all teleportation through sponge's bridge for handling events
     */
    @Overwrite
    public net.minecraft.server.level.@Nullable ServerPlayer teleport(final TeleportTransition transition) {
        return this.bridge$changeDimension(transition);
    }

    @Redirect(method = "findRespawnPositionAndUseSpawnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getRespawnDimension()Lnet/minecraft/resources/ResourceKey;"))
    private ResourceKey<Level> impl$callRespawnPlayerSelectWorld(final net.minecraft.server.level.ServerPlayer player) {
        var playerRespawnDestination = this.server.getLevel(player.getRespawnDimension());
        if (playerRespawnDestination == null) {
            SpongeCommon.logger().warn("The player '{}' respawn location was located in a world that isn't loaded or doesn't exist. This is not safe so "
                + "the player will be moved to the spawn of the default world.", player.getGameProfile().getName());
            playerRespawnDestination = player.server.overworld();
        }

        final RespawnPlayerEvent.SelectWorld event = SpongeEventFactory.createRespawnPlayerEventSelectWorld(PhaseTracker.getCauseStackManager().currentCause(),
                (ServerWorld) playerRespawnDestination, (ServerWorld) player.serverLevel(), (ServerWorld) playerRespawnDestination, (ServerPlayer) player);
        SpongeCommon.post(event);

        this.impl$respawnLevel = (ServerLevel) event.destinationWorld();
        return ((ServerLevel) event.destinationWorld()).dimension();
    }

    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("RETURN"))
    private void impl$onFindRespawnPositionAndUseSpawnBlock(final CallbackInfoReturnable<TeleportTransition> cir) {
        ((TeleportTransitionAccessor) (Object) cir.getReturnValue()).accessor$newLevel(this.impl$respawnLevel);
        this.impl$respawnLevel = null;
    }

    @Redirect(method = "saveParentVehicle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hasExactlyOnePlayerPassenger()Z"))
    private boolean impl$skipUnserializableRootVehicle(final Entity instance) {
        return instance.hasExactlyOnePlayerPassenger() && !((TransientBridge) instance).bridge$isTransient();
    }
}
