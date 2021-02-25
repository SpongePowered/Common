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
package org.spongepowered.common.mixin.api.mcp.server.level;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerTickList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.LevelResource;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.DataProvider;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.fluid.FluidType;
import org.spongepowered.api.registry.RegistryHolder;
import org.spongepowered.api.registry.RegistryScope;
import org.spongepowered.api.scheduler.ScheduledUpdateList;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.ChunkRegenerateFlag;
import org.spongepowered.api.world.generation.ChunkGenerator;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.WorldTemplate;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
import org.spongepowered.api.world.storage.WorldStorage;
import org.spongepowered.api.world.weather.Weather;
import org.spongepowered.api.world.weather.WeatherType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.accessor.world.entity.raid.RaidsAccessor;
import org.spongepowered.common.bridge.server.level.ServerLevelBridge;
import org.spongepowered.common.data.SpongeDataManager;
import org.spongepowered.common.mixin.api.mcp.world.level.LevelMixin_API;
import org.spongepowered.common.util.MissingImplementationException;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.server.SpongeWorldTemplate;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings({"unchecked", "rawtypes"})
@Mixin(ServerLevel.class)
@Implements(@Interface(iface = org.spongepowered.api.world.server.ServerWorld.class, prefix = "serverWorld$"))
public abstract class ServerLevelMixin_API extends LevelMixin_API<org.spongepowered.api.world.server.ServerWorld, ServerLocation> implements org.spongepowered.api.world.server.ServerWorld {

    // @formatter:off
    @Shadow @Final private ServerTickList<Block> blockTicks;
    @Shadow @Final private ServerTickList<Fluid> liquidTicks;
    @Shadow @Final private Int2ObjectMap<Entity> entitiesById;

    @Shadow public abstract void shadow$save(@Nullable ProgressListener p_217445_1_, boolean p_217445_2_, boolean p_217445_3_);
    @Shadow public abstract void shadow$unload(LevelChunk p_217466_1_);
    @Shadow public abstract void shadow$playSound(@Nullable Player p_184148_1_, double p_184148_2_, double p_184148_4_, double p_184148_6_, SoundEvent p_184148_8_, SoundSource p_184148_9_, float p_184148_10_, float p_184148_11_);
    @Shadow public abstract ServerChunkCache shadow$getChunkSource();
    @Nonnull @Shadow public abstract MinecraftServer shadow$getServer();
    @Nullable @Shadow public abstract Entity shadow$getEntity(UUID p_217461_1_);
    @Shadow public abstract List<net.minecraft.server.level.ServerPlayer> shadow$players();
    @Shadow public abstract Raids shadow$getRaids();
    @Nullable @Shadow public abstract Raid shadow$getRaidAt(BlockPos p_217475_1_);
    @Shadow public abstract long shadow$getSeed();
    // @formatter:on

    @Intrinsic
    public long serverWorld$getSeed() {
        return this.shadow$getSeed();
    }

    // World

    @Override
    public boolean isLoaded() {
        return ((ServerLevelBridge) this).bridge$isLoaded();
    }

    // LocationCreator

    @Override
    public ServerLocation getLocation(final Vector3i position) {
        return ServerLocation.of(this, Objects.requireNonNull(position, "position"));
    }

    @Override
    public ServerLocation getLocation(final Vector3d position) {
        return ServerLocation.of(this, Objects.requireNonNull(position, "position"));
    }

    // ServerWorld

    @Override
    public ServerWorldProperties getProperties() {
        return (ServerWorldProperties) this.shadow$getLevelData();
    }

    @Override
    public ChunkGenerator getGenerator() {
        return (ChunkGenerator) this.shadow$getChunkSource().getGenerator();
    }

    @Override
    public WorldTemplate asTemplate() {
        return new SpongeWorldTemplate((ServerLevel) (Object) this);
    }

    @Override
    public ResourceKey getKey() {
        return (ResourceKey) (Object) this.shadow$dimension().location();
    }

    @Override
    public Server getEngine() {
        return (Server) this.shadow$getServer();
    }

    @Override
    public Optional<org.spongepowered.api.world.chunk.Chunk> regenerateChunk(final int cx, final int cy, final int cz, final ChunkRegenerateFlag flag) {
        throw new MissingImplementationException("ServerWorld", "regenerateChunk");
    }

    @Override
    public BlockSnapshot createSnapshot(final int x, final int y, final int z) {
        return ((ServerLevelBridge) this).bridge$createSnapshot(x, y, z);
    }

    @Override
    public boolean restoreSnapshot(final BlockSnapshot snapshot, final boolean force, final BlockChangeFlag flag) {
        return snapshot.restore(force, Objects.requireNonNull(flag, "flag"));
    }

    @Override
    public boolean restoreSnapshot(final int x, final int y, final int z, final BlockSnapshot snapshot, final boolean force, final BlockChangeFlag flag) {
        return Objects.requireNonNull(snapshot, "snapshot").withLocation(this.getLocation(x, y, z)).restore(force, Objects.requireNonNull(flag, "flag"));
    }

    @Override
    public Path getDirectory() {
        return ((ServerLevelBridge) this).bridge$getLevelSave().getLevelPath(LevelResource.ROOT);
    }

    @Override
    public WorldStorage getWorldStorage() {
        return (WorldStorage) this.shadow$getChunkSource();
    }

    @Override
    public boolean save() throws IOException {
        ((ServerLevelBridge) this).bridge$setManualSave(true);
        this.shadow$save(null, false, true);
        return true;
    }

    @Override
    public boolean unloadChunk(final org.spongepowered.api.world.chunk.Chunk chunk) {
        this.shadow$unload((LevelChunk) Objects.requireNonNull(chunk, "chunk"));
        return true;
    }

    @Override
    public void triggerExplosion(final org.spongepowered.api.world.explosion.Explosion explosion) {
        ((ServerLevelBridge) this).bridge$triggerExplosion(Objects.requireNonNull(explosion, "explosion"));
    }

    @Override
    public Collection<ServerPlayer> getPlayers() {
        return Collections.unmodifiableCollection((Collection<ServerPlayer>) (Collection<?>) ImmutableList.copyOf(this.shadow$players()));
    }

    @Override
    public Collection<org.spongepowered.api.entity.Entity> getEntities() {
        return (Collection< org.spongepowered.api.entity.Entity>) (Object) Collections.unmodifiableCollection(this.entitiesById.values());
    }

    @Override
    public Collection<org.spongepowered.api.raid.Raid> getRaids() {
        return (Collection<org.spongepowered.api.raid.Raid>) (Collection) ((RaidsAccessor) this.shadow$getRaids()).accessor$raidMap().values();
    }

    @Override
    public Optional<org.spongepowered.api.raid.Raid> getRaidAt(final Vector3i blockPosition) {
        return Optional.ofNullable((org.spongepowered.api.raid.Raid) this.shadow$getRaidAt(VecHelper.toBlockPos(Objects.requireNonNull(blockPosition, "blockPosition"))));
    }

    // Volume

    @Override
    public boolean containsBlock(final int x, final int y, final int z) {
        return Level.isInWorldBounds(new BlockPos(x, y, z));
    }

    // EntityVolume

    @Override
    public Optional<org.spongepowered.api.entity.Entity> getEntity(final UUID uniqueId) {
        return Optional.ofNullable((org.spongepowered.api.entity.Entity) this.shadow$getEntity(Objects.requireNonNull(uniqueId, "uniqueId")));
    }

    // MutableBlockEntityVolume

    @Override
    public void removeBlockEntity(final int x, final int y, final int z) {
        this.shadow$removeBlockEntity(new BlockPos(x, y, z));
    }

    // UpdateableVolume

    @Override
    public ScheduledUpdateList<BlockType> getScheduledBlockUpdates() {
        return (ScheduledUpdateList<BlockType>) this.blockTicks;
    }

    @Override
    public ScheduledUpdateList<FluidType> getScheduledFluidUpdates() {
        return (ScheduledUpdateList<FluidType>) this.liquidTicks;
    }

    @Override
    public <E> DataTransactionResult offer(final int x, final int y, final int z, final Key<? extends Value<E>> key, final E value) {
        Objects.requireNonNull(value, "value");

        final DataProvider<? extends Value<E>, E> dataProvider = SpongeDataManager.getProviderRegistry().getProvider(Objects.requireNonNull(key, "key"), ServerLocation.class);
        return dataProvider.offer(ServerLocation.of(this, new Vector3d(x, y, z)), value);
    }

    @Override
    public <E> Optional<E> get(final int x, final int y, final int z, final Key<? extends Value<E>> key) {
        final Optional<E> value = SpongeDataManager.getProviderRegistry().getProvider(Objects.requireNonNull(key, "key"), ServerLocation.class).get(ServerLocation.of(this, new Vector3d(x, y, z)));
        if (value.isPresent()) {
            return value;
        }
        return this.getBlock(x, y, z).get(key);
    }

    @Override
    public DataTransactionResult remove(final int x, final int y, final int z, final Key<?> key) {
        return SpongeDataManager.getProviderRegistry().getProvider((Key) Objects.requireNonNull(key, "key"), ServerLocation.class).remove(ServerLocation.of(this, new Vector3d(x, y, z)));
    }

    // WeatherUniverse

    @Override
    public Weather weather() {
        return ((ServerWorldProperties) this.shadow$getLevelData()).weather();
    }

    @Override
    public void setWeather(final WeatherType type) {
        ((ServerWorldProperties) this.shadow$getLevelData()).setWeather(Objects.requireNonNull(type, "type"));
    }

    @Override
    public void setWeather(final WeatherType type, final Ticks ticks) {
        ((ServerWorldProperties) this.shadow$getLevelData()).setWeather(Objects.requireNonNull(type, "type"), Objects.requireNonNull(ticks, "ticks"));
    }

    @Override
    public RegistryScope registryScope() {
        return RegistryScope.WORLD;
    }

    @Override
    public RegistryHolder registries() {
        return ((ServerLevelBridge) this).bridge$registries();
    }
}
