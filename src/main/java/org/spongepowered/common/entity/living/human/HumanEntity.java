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
package org.spongepowered.common.entity.living.human;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.serialization.DataResult;
import net.kyori.adventure.text.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.scoreboard.TeamMember;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.network.protocol.game.ClientboundAddEntityPacketAccessor;
import org.spongepowered.common.accessor.network.protocol.game.ClientboundPlayerInfoUpdatePacketAccessor;
import org.spongepowered.common.accessor.world.entity.LivingEntityAccessor;
import org.spongepowered.common.accessor.world.entity.player.PlayerAccessor;
import org.spongepowered.common.config.SpongeGameConfigs;
import org.spongepowered.common.launch.Launch;
import org.spongepowered.common.profile.SpongeProfileProperty;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.SpongeTicks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class HumanEntity extends PathfinderMob implements TeamMember, RangedAttackMob {
    public static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("sponge", "human"));

    public static AttributeSupplier createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ATTACK_DAMAGE, 1.0d) // Player
                .add(Attributes.MOVEMENT_SPEED, (double) 0.23f) // Player (custom value)
                .add(Attributes.ATTACK_SPEED) // Player
                .add(Attributes.LUCK) // Player
                .build();
    }

    // A queue of packets waiting to send to players tracking this human
    private final Map<UUID, List<Stream<Packet<?>>>> playerPacketMap = new HashMap<>();

    private ResolvableProfile fakeProfile;
    private boolean aiDisabled = false, leftHanded = false;

    public HumanEntity(final EntityType<? extends HumanEntity> type, final Level world) {
        super(type, world);
        this.fakeProfile = new ResolvableProfile(new GameProfile(this.uuid, ""));
        this.setCanPickUpLoot(true);
        this.entityData.set(PlayerAccessor.accessor$DATA_PLAYER_MODE_CUSTOMISATION(), Constants.Sponge.Entity.Human.PLAYER_MODEL_FLAG_ALL);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder $$0) {
        // LivingEntity
        $$0.define(LivingEntityAccessor.accessor$DATA_LIVING_ENTITY_FLAGS(), (byte) 0);
        $$0.define(LivingEntityAccessor.accessor$DATA_HEALTH_ID(), 1.0F);
        $$0.define(LivingEntityAccessor.accessor$DATA_EFFECT_PARTICLES(), List.of());
        $$0.define(LivingEntityAccessor.accessor$DATA_EFFECT_AMBIENCE_ID(), Boolean.FALSE);
        $$0.define(LivingEntityAccessor.accessor$DATA_ARROW_COUNT_ID(), 0);
        $$0.define(LivingEntityAccessor.accessor$DATA_STINGER_COUNT_ID(), 0);
        $$0.define(LivingEntityAccessor.accessor$SLEEPING_POS_ID(), Optional.empty());

        // Player
        $$0.define(PlayerAccessor.accessor$DATA_PLAYER_ABSORPTION_ID(), 0.0F);
        $$0.define(PlayerAccessor.accessor$DATA_SCORE_ID(), 0);
        $$0.define(PlayerAccessor.accessor$DATA_PLAYER_MODE_CUSTOMISATION(), (byte) 0);
        $$0.define(PlayerAccessor.accessor$DATA_PLAYER_MAIN_HAND(), (byte) 1);
        $$0.define(PlayerAccessor.accessor$DATA_SHOULDER_LEFT(), new CompoundTag());
        $$0.define(PlayerAccessor.accessor$DATA_SHOULDER_RIGHT(), new CompoundTag());
    }

    @Override
    protected void registerGoals() {
        // Just some defaults, so they do something by default. Plugins can fully cancel this and make them do whatever
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Mob.class, 8.0F));
    }

    @Override
    public boolean isLeftHanded() {
        return this.leftHanded;
    }

    @Override
    public boolean isNoAi() {
        return this.aiDisabled;
    }

    @Override
    public Component teamRepresentation() {
        return Component.text(this.fakeProfile.name().orElse(""));
    }

    @Override
    public PlayerTeam getTeam() {
        return this.level().getScoreboard().getPlayersTeam(this.fakeProfile.name().orElse(""));
    }

    @Override
    public void setCustomName(final net.minecraft.network.chat.@Nullable Component name) {
        final net.minecraft.network.chat.Component customName = this.getCustomName();
        if (customName == null && name == null || customName != null && customName.equals(name)) {
            return;
        }
        super.setCustomName(name);
        this.setProfileName(name);
        if (this.isAliveAndInWorld()) {
            this.respawnOnClient();
        }
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("profile")) {
              ResolvableProfile.CODEC
                .parse(NbtOps.INSTANCE, tag.get("profile"))
                .resultOrPartial($$0x -> SpongeCommon.logger().error("Failed to load profile from player head: {}", $$0x))
                .ifPresent(profile -> this.fakeProfile = profile);

            this.setUUID(this.fakeProfile.id().get());
        }
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        final DataResult<Tag> result = ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, this.fakeProfile);
        result.ifSuccess(gameProfile -> tag.put("profile", gameProfile));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.updateSwingTime();
    }

    @Override
    public void setNoAi(final boolean disable) {
        this.aiDisabled = disable;
    }

    @Override
    public void setLeftHanded(final boolean leftHanded) {
        this.leftHanded = leftHanded;
    }

    @Override
    public void setAggressive(final boolean aggressive) {
        // NOOP, we handle the arm swing manually...
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.PLAYER_SWIM;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.PLAYER_SPLASH;
    }

    @Override
    public int getDimensionChangingDelay() {
        return 10;
    }

    @Override
    public void die(final @Nullable DamageSource cause) {
        super.die(cause);
        this.reapplyPosition();
        if (cause != null) {
            this.setDeltaMovement(-Mth.cos((this.getHurtDir() + this.getYRot()) * ((float)Math.PI / 180F)) * 0.1F, 0.1F,
                    -Mth.sin((this.getHurtDir() + this.getYRot()) * ((float)Math.PI / 180F)) * 0.1F);
        } else {
            this.setDeltaMovement(0.0D, 0.1D, 0.0D);
        }

        this.clearFire();
        this.setSharedFlag(0, false);
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public Vec3 getVehicleAttachmentPoint(final Entity $$0) {
        // TODO  Constants.Entity.Player.PLAYER_Y_OFFSET;
        return super.getVehicleAttachmentPoint($$0);
    }

    @Override
    public float getSpeed() {
        return (float) this.getAttribute(Attributes.MOVEMENT_SPEED).getValue();
    }

    @Override
    public Fallsounds getFallSounds() {
        return new Fallsounds(SoundEvents.PLAYER_SMALL_FALL, SoundEvents.PLAYER_BIG_FALL);
    }

    @Override
    public float getAbsorptionAmount() {
        return this.getEntityData().get(PlayerAccessor.accessor$DATA_PLAYER_ABSORPTION_ID());
    }

    @Override
    public void internalSetAbsorptionAmount(float amount) {
        this.getEntityData().set(PlayerAccessor.accessor$DATA_PLAYER_ABSORPTION_ID(), amount);
    }

    @Override
    protected void tickHeadTurn(final float p_110146_1_) {
        // Make the body rotation follow head rotation
        this.setYRot(this.getYHeadRot());
    }

    private void setProfileName(final net.minecraft.network.chat.@Nullable Component newName) {
        final Optional<String> optName = Optional.ofNullable(newName).map(net.minecraft.network.chat.Component::getString);
        this.fakeProfile = new ResolvableProfile(optName, this.fakeProfile.id(), this.fakeProfile.properties());
    }

    public boolean getOrLoadSkin(final UUID minecraftAccount) {
        GameProfile gameProfile = SpongeCommon.server().getProfileCache().get(minecraftAccount).orElse(null);
        if (gameProfile == null) {
            ProfileResult result =
                    SpongeCommon.server().getSessionService().fetchProfile(minecraftAccount, true);
            if (result == null) {
                return false;
            }

            gameProfile = result.profile();

            // TODO Should we put profile cache entries with UUIDs that don't have their names?

            SpongeCommon.server().getProfileCache().add(gameProfile);
        }

        this.fakeProfile.properties().replaceValues(ProfileProperty.TEXTURES, gameProfile.getProperties().get(ProfileProperty.TEXTURES));
        if (this.isAliveAndInWorld()) {
            this.respawnOnClient();
        }

        return true;
    }

    public boolean getOrLoadSkin(final String minecraftAccount) {
        Objects.requireNonNull(minecraftAccount);
        GameProfile gameProfile = SpongeCommon.server().getProfileCache().get(minecraftAccount).orElse(null);
        if (gameProfile == null) {
            return false;
        }

        if (gameProfile.getProperties().isEmpty()) {
            ProfileResult result = SpongeCommon.server().getSessionService().fetchProfile(gameProfile.getId(), true);
            if (result == null) {
                return false;
            }
            gameProfile = result.profile();
            SpongeCommon.server().getProfileCache().add(gameProfile);
        }

        this.fakeProfile.properties().clear();
        this.fakeProfile.properties().putAll(gameProfile.getProperties());
        if (this.isAliveAndInWorld()) {
            this.respawnOnClient();
        }

        return true;
    }

    public void removeFromTabListDelayed(final @Nullable ServerPlayer player, final ClientboundPlayerInfoRemovePacket removePacket) {
        final int delay = SpongeGameConfigs.getForWorld(this.level()).get().entity.human.tabListRemoveDelay;
        final Runnable removeTask = () -> this.pushPackets(player, removePacket);
        if (delay == 0) {
            removeTask.run();
        } else {
            Sponge.server().scheduler().submit(Task.builder()
                    .execute(removeTask)
                    .delay(new SpongeTicks(delay))
                    .plugin(Launch.instance().commonPlugin())
                    .build());
        }
    }

    public SpongeProfileProperty getSkinProperty() {
        final Collection<Property> properties = this.fakeProfile.properties().get(ProfileProperty.TEXTURES);
        if (properties.isEmpty()) {
            return null;
        }
        return new SpongeProfileProperty(properties.iterator().next());
    }

    public void setSkinProperty(final ProfileProperty property) {
        this.fakeProfile.properties()
                .replaceValues(
                        ProfileProperty.TEXTURES,
                        Collections.singletonList(((SpongeProfileProperty) property).asProperty()));

        if (this.isAliveAndInWorld()) {
            this.respawnOnClient();
        }
    }

    private boolean isAliveAndInWorld() {
        return this.level().getEntity(this.getId()) == this && !this.isRemoved();
    }

    private void respawnOnClient() {
        this.pushPackets(new ClientboundRemoveEntitiesPacket(this.getId()), this.createPlayerListPacket(EnumSet.allOf(ClientboundPlayerInfoUpdatePacket.Action.class)));
        this.pushPackets(this.getAddEntityPacket(new ServerEntity(
            (ServerLevel) this.level(),
            this, 1, true, packet -> {},
            (packet, list) -> {}
        )));
    }

    /**
     * Can the fake profile be removed from the tab list immediately (i.e. as
     * soon as the human has spawned).
     *
     * @return Whether it can be removed with 0 ticks delay
     */
    public boolean canRemoveFromListImmediately() {
        return !this.fakeProfile.properties().containsKey(ProfileProperty.TEXTURES);
    }

    /**
     * Called when a player stops tracking this human.
     *
     * Removes the player from the packet queue and sends them a REMOVE_PLAYER
     * tab list packet to make sure the human is not on it.
     *
     * @param player The player that has stopped tracking this human
     */
    public void untrackFrom(final ServerPlayer player) {
        this.playerPacketMap.remove(player.getUUID());
        player.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(this.uuid)));
    }

    /**
     * Creates a {@link ClientboundPlayerInfoUpdatePacket} packet with the given action.
     *
     * @param actions The action to apply on the tab list
     * @return A new tab list packet
     */
    public ClientboundPlayerInfoUpdatePacket createPlayerListPacket(final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions) {
        final ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(actions, List.of());

        ((ClientboundPlayerInfoUpdatePacketAccessor) packet).accessor$entries(List.of(
            new ClientboundPlayerInfoUpdatePacket.Entry(this.uuid, this.fakeProfile.gameProfile(), false, 0, GameType.DEFAULT_MODE, this.getDisplayName(), false, 0, null)));
        return packet;
    }

    /**
     * Push the given packets to all players tracking this human.
     *
     * @param packets All packets to send in a single tick
     */
    public void pushPackets(final Packet<?>... packets) {
        this.pushPackets(null, packets); // null = all players
    }

    /**
     * Push the given packets to the given player (who must be tracking this
     * human).
     *
     * @param player The player tracking this human
     * @param packets All packets to send in a single tick
     */
    public void pushPackets(final @Nullable ServerPlayer player, final Packet<?>... packets) {
        final List<Stream<Packet<?>>> queue;
        if (player == null) {
            queue = this.playerPacketMap.computeIfAbsent(null, k -> new ArrayList<>());
        } else {
            queue = this.playerPacketMap.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        }
        queue.add(Stream.of(packets));
    }

    /**
     * (Internal) Pops the packets off the queue for the given player.
     *
     * @param player The player to get packets for (or null for all players)
     * @return An array of packets to send in a single tick
     */
    public Stream<Packet<?>> popQueuedPackets(final @Nullable ServerPlayer player) {
        final List<Stream<Packet<?>>> queue = this.playerPacketMap.get(player == null ? null : player.getUUID());
        return queue == null || queue.isEmpty() ? Stream.empty() : queue.remove(0);
    }

    @Override
    public void performRangedAttack(final LivingEntity target, final float distanceFactor) {
        final ItemStack itemstack = this.getItemInHand(InteractionHand.OFF_HAND);
        final ItemStack weaponStack = this.getWeaponItem();
        final Arrow arrow = new Arrow(this.level(), this, itemstack.getItem() instanceof ArrowItem ? itemstack : new ItemStack(Items.ARROW), weaponStack);
        final double d0 = target.getX() - this.getX();
        final double d1 = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - arrow.getY();
        final double d2 = target.getZ() - this.getZ();
        final double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        arrow.shoot(d0, d1 + d3 * 0.20000000298023224D, d2, 1.6F, 14 - this.level().getDifficulty().getId() * 4);

        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity $$0) {
        final ClientboundAddEntityPacket packet = new ClientboundAddEntityPacket(this, $$0);
        ((ClientboundAddEntityPacketAccessor) packet).accessor$type(EntityType.PLAYER);
        return packet;
    }
}
