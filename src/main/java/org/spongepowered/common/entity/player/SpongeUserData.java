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
package org.spongepowered.common.entity.player;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataSerializable;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.VanishState;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackLike;
import org.spongepowered.api.item.inventory.entity.UserInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.util.RespawnLocation;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.SpongeServer;
import org.spongepowered.common.accessor.server.MinecraftServerAccessor;
import org.spongepowered.common.bridge.authlib.GameProfileHolderBridge;
import org.spongepowered.common.bridge.data.DataCompoundHolder;
import org.spongepowered.common.bridge.data.SpongeDataHolderBridge;
import org.spongepowered.common.bridge.data.TransientBridge;
import org.spongepowered.common.bridge.data.VanishableBridge;
import org.spongepowered.common.bridge.permissions.SubjectBridge;
import org.spongepowered.common.bridge.world.entity.player.BedLocationHolderBridge;
import org.spongepowered.common.data.DataUtil;
import org.spongepowered.common.data.holder.SpongeMutableDataHolder;
import org.spongepowered.common.data.provider.nbt.NBTDataType;
import org.spongepowered.common.data.provider.nbt.NBTDataTypes;
import org.spongepowered.common.profile.SpongeGameProfile;
import org.spongepowered.common.service.server.permission.BridgeSubject;
import org.spongepowered.common.service.server.permission.SubjectHelper;
import org.spongepowered.common.user.SpongeUserManager;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.FileUtil;
import org.spongepowered.common.util.MissingImplementationException;
import org.spongepowered.math.vector.Vector3d;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Also see SpongeDataHolderMixin
 *
 * TODO: Ideally, this should not implement User, but does so to support DataHolders... for now
 *  Our Data system should treat Players and Users the same as they are effectively the same
 *  store - just one represents a player that is online, the other, a player that is offline.
 *  There should be no difference between the two.
 */
public final class SpongeUserData implements Identifiable, DataSerializable, BedLocationHolderBridge, SpongeMutableDataHolder,
        DataCompoundHolder, VanishableBridge, GameProfileHolderBridge, User, BridgeSubject, TransientBridge {

    private final Map<ResourceKey, RespawnLocation> spawnLocations = Maps.newHashMap();

    private ResourceKey worldKey = (ResourceKey) (Object) Level.OVERWORLD.location();
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean invulnerable;
    private boolean isVanished;
    private boolean isInvisible;
    private VanishState vanishState;
    private boolean vanishIgnoresCollision;
    private boolean vanishPreventsTargeting;
    private final GameProfile profile;
    private boolean isTransient;

    private @Nullable SpongeUserInventory inventory; // lazy load when accessing inventory
    private @Nullable PlayerEnderChestContainer enderChest; // lazy load when accessing inventory
    private CompoundTag compound;
    private boolean isConstructing;

    public static SpongeUserData create(final GameProfile profile) throws IOException {
        final ServerLevel world = SpongeCommon.server().overworld();
        if (world == null) {
            SpongeCommon.logger().warn("Overworld not initialized, cannot create users!");
            throw new IllegalStateException("Overworld not initialized, cannot create users!");
        }

        final LevelStorageSource.LevelStorageAccess storageSource = ((MinecraftServerAccessor) Sponge.server()).accessor$storageSource();
        final Path p = storageSource.getLevelPath(LevelResource.PLAYER_DATA_DIR).resolve(profile.getId().toString() + ".dat");
        if (!Files.exists(p)) {
            return new SpongeUserData(profile, new CompoundTag());
        }

        try {
            final CompoundTag compound;
            try (final InputStream in = Files.newInputStream(p)) {
                compound = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            }
            // See PlayerDataAccess - keep this line up to date.
            final int version = compound.contains("DataVersion", 3) ? compound.getInt("DataVersion") : -1;
            DataFixTypes.PLAYER.updateToCurrentVersion(DataFixers.getDataFixer(), compound, version);
            return new SpongeUserData(profile, compound);
        } catch (final IOException e) {
            SpongeCommon.logger().warn("Unable to load corrupt user file '{}'!",
                    p.relativize(Paths.get("")).toString(), e);
            FileUtil.copyCorruptedFile(p);
            throw e;
        }
    }

    private SpongeUserData(final GameProfile profile, final CompoundTag tag) {
        this.profile = profile;
        this.compound = tag;
        this.readCompound(this.compound);
        SubjectHelper.applySubject((SubjectBridge) (Object) this, PermissionService.SUBJECTS_USER);
    }

    public User asUser() {
        return ((SpongeServer) SpongeCommon.server()).userManager().asUser(this);
    }

    private void reset() {
        this.spawnLocations.clear();
    }

    @Override
    public List<DataHolder> impl$delegateDataHolder() {
        return Collections.singletonList(this.getDataHolder(true));
    }

    public DataHolder.Mutable getDataHolder(final boolean markDirty) {
        if (this.isOnline()) {
            return this.player().get();
        }
        if (markDirty) {
            this.markDirty();
        }
        return this;
    }

    private UserInventory loadInventory() {
        if (this.inventory == null) {
            this.inventory = new SpongeUserInventory(this);
            final ListTag listNBT = this.compound.getList(Constants.Entity.Player.INVENTORY, 10);
            this.inventory.readList(listNBT);
            this.inventory.currentItem = this.compound.getInt(Constants.Entity.Player.SELECTED_ITEM_SLOT);
        }
        return (UserInventory) this.inventory;
    }

    private SpongeUserData loadEnderInventory() {
        if (this.enderChest == null) {
            this.enderChest = new SpongeUserInventoryEnderchest(this);
            if (this.compound.contains(Constants.Entity.Player.ENDERCHEST_INVENTORY, 9)) {
                final ListTag nbttaglist1 = this.compound.getList(Constants.Entity.Player.ENDERCHEST_INVENTORY, 10);
                this.enderChest.fromTag(nbttaglist1, SpongeCommon.vanillaRegistryAccess());
            }
        }
        return this;
    }

    public void readCompound(final CompoundTag compound) {
        this.reset();
        this.compound = compound;

        if (!compound.contains(Constants.Sponge.World.WORLD_KEY)) {
            this.worldKey = ResourceKey.resolve(compound.getString(Constants.Sponge.World.WORLD_KEY));
        }
        final ListTag position = compound.getList(Constants.Entity.ENTITY_POSITION, Constants.NBT.TAG_DOUBLE);
        final ListTag rotation = compound.getList(Constants.Entity.ENTITY_ROTATION, Constants.NBT.TAG_FLOAT);
        this.x = position.getDouble(0);
        this.y = position.getDouble(1);
        this.z = position.getDouble(2);
        this.yaw = rotation.getFloat(0);
        this.pitch = rotation.getFloat(1);

        this.isConstructing = true;
        DataUtil.syncTagToData(this);
        this.isConstructing = false;
    }

    public void writeCompound(final CompoundTag compound) {

        compound.putString(Constants.Sponge.World.WORLD_KEY, this.worldKey.formatted());

        this.loadInventory();
        this.loadEnderInventory();

        compound.put(Constants.Entity.Player.INVENTORY, this.inventory.writeList(new ListTag()));
        compound.put(Constants.Entity.Player.ENDERCHEST_INVENTORY, this.enderChest.createTag(SpongeCommon.vanillaRegistryAccess()));
        compound.putInt(Constants.Entity.Player.SELECTED_ITEM_SLOT, this.inventory.currentItem);

        compound.put(Constants.Entity.ENTITY_POSITION, Constants.NBT.newDoubleNBTList(this.x, this.y, this.z));
        compound.put(Constants.Entity.ENTITY_ROTATION, Constants.NBT.newFloatNBTList(this.yaw, this.pitch));

        if (DataUtil.syncDataToTag(this)) {
            compound.merge(this.data$getCompound());
        }
    }

    @Override
    public UUID uniqueId() {
        return this.profile.getId();
    }

    @Override
    public String name() {
        return this.profile.getName();
    }

    @Override
    public int contentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        // TODO More data
        return DataContainer.createNew()
            .set(Queries.CONTENT_VERSION, this.contentVersion())
            .set(Constants.Entity.Player.UUID, this.profile.getId())
            .set(Constants.Entity.Player.NAME, this.profile.getName())
            .set(Constants.Entity.Player.SPAWNS, this.spawnLocations);
    }

    @Override
    public boolean canEquip(final EquipmentType type) {
        return true;
    }

    @Override
    public boolean canEquip(final EquipmentType type, final @Nullable ItemStackLike equipment) {
        return true;
    }

    @Override
    public Optional<ItemStack> equipped(final EquipmentType type) {
        throw new MissingImplementationException("SpongeUser", "equipped");
    }

    @Override
    public boolean equip(final EquipmentType type, final @Nullable ItemStackLike equipment) {
        if (this.canEquip(type, equipment)) {
            this.loadInventory();
            this.setEquippedItem(type, equipment);
            return true;
        }
        return false;
    }

    @Override
    public UserInventory inventory() {
        return this.loadInventory();
    }

    @Override
    public EquipmentInventory equipment() {
        return this.inventory().equipment();
    }

    @Override
    public ItemStack itemInHand(final HandType handType) {
        if (handType == HandTypes.MAIN_HAND.get()) {
            this.equipped(EquipmentTypes.MAINHAND.get()).orElseThrow(IllegalStateException::new);
        } else if (handType == HandTypes.OFF_HAND.get()) {
            this.equipped(EquipmentTypes.OFFHAND.get()).orElseThrow(IllegalStateException::new);
        }
        throw new IllegalArgumentException("Invalid hand " + handType);
    }

    @Override
    public ItemStack head() {
        return this.equipped(EquipmentTypes.HEAD.get()).orElseThrow(IllegalStateException::new);
    }

    @Override
    public void setHead(final ItemStackLike helmet) {
        this.equip(EquipmentTypes.HEAD.get(), helmet);
    }

    @Override
    public ItemStack chest() {
        return this.equipped(EquipmentTypes.CHEST.get()).orElseThrow(IllegalStateException::new);
    }

    @Override
    public void setChest(final ItemStackLike chestplate) {
        this.equip(EquipmentTypes.CHEST.get(), chestplate);
    }

    @Override
    public ItemStack legs() {
        return this.equipped(EquipmentTypes.LEGS.get()).orElseThrow(IllegalStateException::new);
    }

    @Override
    public void setLegs(final ItemStackLike leggings) {
        this.equip(EquipmentTypes.LEGS.get(), leggings);
    }

    @Override
    public ItemStack feet() {
        return this.equipped(EquipmentTypes.FEET.get()).orElseThrow(IllegalStateException::new);
    }

    @Override
    public void setFeet(final ItemStackLike boots) {
        this.equip(EquipmentTypes.FEET.get(), boots);
    }

    @Override
    public void setItemInHand(final HandType handType, final @Nullable ItemStackLike itemInHand) {
        if (handType == HandTypes.MAIN_HAND.get()) {
            this.setEquippedItem(EquipmentTypes.MAINHAND, itemInHand);
        } else if (handType == HandTypes.OFF_HAND.get()) {
            this.setEquippedItem(EquipmentTypes.OFFHAND, itemInHand);
        } else {
            throw new IllegalArgumentException("Invalid hand " + handType);
        }
    }

    @Override
    public Map<ResourceKey, RespawnLocation> bridge$getBedlocations() {
        final Optional<ServerPlayer> player = this.player();
        return player
            .map(value -> ((BedLocationHolderBridge) value).bridge$getBedlocations())
            .orElse(this.spawnLocations);
    }

    @Override
    public boolean bridge$setBedLocations(final Map<ResourceKey, RespawnLocation> value) {
        final Optional<ServerPlayer> player = this.player();
        if (player.isPresent()) {
            return ((BedLocationHolderBridge) player.get()).bridge$setBedLocations(value);
        }
        this.spawnLocations.clear();
        this.spawnLocations.putAll(value);
        if (value.isEmpty()) {
            ((SpongeDataHolderBridge) (Object) this).bridge$remove(Keys.RESPAWN_LOCATIONS);
        } else {
            ((SpongeDataHolderBridge) (Object) this).bridge$offer(Keys.RESPAWN_LOCATIONS, value);
        }
        this.markDirty();
        return true;
    }

    @Override
    public ImmutableMap<ResourceKey, RespawnLocation> bridge$removeAllBeds() {
        final Optional<ServerPlayer> player = this.player();
        if (player.isPresent()) {
            return ((BedLocationHolderBridge) player.get()).bridge$removeAllBeds();
        }
        final ImmutableMap<ResourceKey, RespawnLocation> locations = ImmutableMap.copyOf(this.spawnLocations);
        this.spawnLocations.clear();
        ((SpongeDataHolderBridge) (Object) this).bridge$remove(Keys.RESPAWN_LOCATIONS);
        this.markDirty();
        return locations;
    }

    public void markDirty() {
        if (this.isConstructing) {
            return;
        }
        ((SpongeServer) SpongeCommon.server()).userManager().markDirty(this);
    }

    public void save() throws IOException {
        synchronized (this) {
            final SpongeUserManager userManager = ((SpongeServer) SpongeCommon.server()).userManager();
            final LevelStorageSource.LevelStorageAccess storageSource = ((MinecraftServerAccessor) Sponge.server()).accessor$storageSource();
            final Path p = storageSource.getLevelPath(LevelResource.PLAYER_DATA_DIR).resolve(this.uniqueId() + ".dat");
            this.writeCompound(this.compound);
            try (final OutputStream out = Files.newOutputStream(p)) {
                NbtIo.writeCompressed(this.compound, out);
                userManager.unmarkDirty(this);
            } catch (final IOException e) {
                // We log the message here because the error may be swallowed by a completable future.
                SpongeCommon.logger().warn("Failed to save user file [{}]!", p, e);
                throw e;
            }
        }
    }

    // Helpers for Equipment:

    private void setEquippedItem(final Supplier<? extends EquipmentType> type, final @Nullable ItemStackLike item) {
        this.setEquippedItem(type.get(), item);
    }

    private void setEquippedItem(final EquipmentType type, final @Nullable ItemStackLike item) {
        throw new MissingImplementationException("SpongeUser", "setEquippedItem");
    }

    @Override
    public org.spongepowered.api.profile.GameProfile profile() {
        return SpongeGameProfile.of(this.profile);
    }

    @Override
    public boolean isOnline() {
        return this.player().isPresent();
    }

    @Override
    public Optional<ServerPlayer> player() {
        return Optional.ofNullable((ServerPlayer) SpongeCommon.server().getPlayerList().getPlayer(this.profile.getId()));
    }

    @Override
    public Vector3d position() {
        return this.player()
                .map(Player::position)
                .orElseGet(() -> new Vector3d(this.x, this.y, this.z));
    }

    @Override
    public ResourceKey worldKey() {
        final Optional<ServerPlayer> player = this.player();
        return player.map(serverPlayer -> serverPlayer.world().key()).orElseGet(() -> this.worldKey);

    }

    @Override
    public boolean setLocation(final ResourceKey key, final Vector3d position) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(position);

        final Optional<ServerPlayer> player = this.player();
        if (player.isPresent()) {
            final Optional<org.spongepowered.api.world.server.ServerWorld> world = Sponge.server().worldManager().world(key);
            return world.filter(serverWorld -> player.get().setLocation(ServerLocation.of(serverWorld, position))).isPresent();
        }

        this.worldKey = key;
        this.x = position.x();
        this.y = position.y();
        this.z = position.z();
        this.markDirty();
        return true;
    }

    @Override
    public Vector3d rotation() {
        return this.player()
                .map(Entity::rotation)
                .orElseGet(() -> new Vector3d(this.pitch, this.yaw, 0));
    }

    @Override
    public void setRotation(final Vector3d rotation) {
        Objects.requireNonNull(rotation, "Rotation was null!");
        final Optional<ServerPlayer> playerOpt = this.player();
        if (playerOpt.isPresent()) {
            playerOpt.get().setRotation(rotation);
            return;
        }
        this.markDirty();
        this.pitch = ((float) rotation.x()) % 360.0F;
        this.yaw = ((float) rotation.y()) % 360.0F;
    }

    @Override
    public Inventory enderChestInventory() {
        final Optional<ServerPlayer> playerOpt = this.player();
        if (playerOpt.isPresent()) {
            return playerOpt.get().enderChestInventory();
        }
        this.loadEnderInventory();
        return ((Inventory) this.enderChest);
    }

    @Override
    public VanishState bridge$vanishState() {
        return this.vanishState;
    }

    @Override
    public void bridge$vanishState(VanishState state) {
        final Optional<ServerPlayer> playerOpt = this.player();
        if (playerOpt.isPresent()) {
            ((VanishableBridge) playerOpt.get()).bridge$vanishState(state);
            return;
        }
        this.vanishState = state;
        this.markDirty();
        ((SpongeDataHolderBridge) (Object) this).bridge$offer(Keys.VANISH_STATE, state);

    }

    @Override
    public boolean bridge$isInvisible() {
        return this.player().map(player -> ((VanishableBridge) player).bridge$isInvisible()).orElseGet(() -> this.isInvisible);
    }

    @Override
    public void bridge$setInvisible(final boolean invisible) {
        final Optional<ServerPlayer> player = this.player();
        if (player.isPresent()) {
            ((VanishableBridge) player.get()).bridge$setInvisible(invisible);
            return;
        }
        this.isInvisible = invisible;
        if (invisible) {
            ((SpongeDataHolderBridge) (Object) this).bridge$offer(Keys.IS_INVISIBLE, true);
        } else {
            ((SpongeDataHolderBridge) (Object) this).bridge$remove(Keys.IS_INVISIBLE);
        }
    }

    @Override
    public CompoundTag data$getCompound() {
        return this.compound;
    }

    @Override
    public void data$setCompound(final CompoundTag nbt) {
        this.compound = nbt;
    }

    @Override
    public NBTDataType data$getNBTDataType() {
        return NBTDataTypes.PLAYER;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final SpongeUserData other = (SpongeUserData) obj;
        return this.profile.getId().equals(other.profile.getId());
    }

    @Override
    public int hashCode() {
        return this.profile.getId().hashCode();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SpongeUserData.class.getSimpleName() + "[", "]")
                .add("isOnline=" + this.isOnline())
                .add("profile=" + this.profile())
                .toString();
    }

    @Override
    public GameProfile bridge$getGameProfile() {
        return this.profile;
    }

    public Boolean isInvulnerable() {
        return this.player().map(player -> ((net.minecraft.world.entity.Entity) player).isInvulnerable()).orElse(this.invulnerable);
    }

    public void setInvulnerable(final boolean invulnerable) {
        final Optional<ServerPlayer> playerOpt = this.player();
        if (playerOpt.isPresent()) {
            ((net.minecraft.world.entity.Entity) playerOpt.get()).setInvulnerable(invulnerable);
            return;
        }
        this.invulnerable = invulnerable;
        this.markDirty();
    }

    @Override
    public String identifier() {
        return this.uniqueId().toString();
    }

    @Override
    public boolean bridge$isTransient() {
        return this.isTransient;
    }

    @Override
    public void bridge$setTransient(final boolean value) {
        this.isTransient = value;
    }
}
