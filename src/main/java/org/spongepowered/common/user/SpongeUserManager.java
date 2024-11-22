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
package org.spongepowered.common.user;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.GameProfileCache;
import org.spongepowered.api.user.UserManager;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.server.players.PlayerListAccessor;
import org.spongepowered.common.accessor.world.level.storage.PlayerDataStorageAccessor;
import org.spongepowered.common.entity.player.SpongeUserData;
import org.spongepowered.common.entity.player.SpongeUserView;
import org.spongepowered.common.profile.SpongeGameProfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@DefaultQualifier(NonNull.class)
public final class SpongeUserManager implements UserManager {

    public static final UUID FAKEPLAYER_UUID = UUID.fromString("41C82C87-7AFB-4024-BA57-13D2C99CAE77");

    private final SpongeUserFileCache userFileCache;
    private final Cache<UUID, SpongeUserData> userCache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    private final Set<SpongeUserData> dirtyUsers = ConcurrentHashMap.newKeySet();

    private final MinecraftServer server;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("Sponge-User-Data-Loader")
            .build());

    public SpongeUserManager(final MinecraftServer server) {
        this.server = server;
        this.userFileCache = new SpongeUserFileCache(this::getSaveHandlerDirectory);
    }

    public void init() {
        this.userFileCache.init();
    }

    @Override
    public CompletableFuture<Optional<User>> load(final UUID uniqueId) {
        return this.fetchUser(uniqueId, false)
                .thenApply(x -> {
                    if (x != null) {
                        return Optional.of(SpongeUserView.create(uniqueId));
                    }
                    return Optional.empty();
                });
    }

    @Override
    public CompletableFuture<User> loadOrCreate(final UUID uuid) {
        return this.fetchUser(uuid, true);
    }

    private CompletableFuture<@Nullable User> fetchUser(final UUID uniqueId, final boolean always) {
        final UUID uuidToUse = this.ensureNonEmptyUUID(uniqueId);
        if (this.server.getPlayerList().getPlayer(uniqueId) != null) {
            return CompletableFuture.completedFuture(SpongeUserView.create(uniqueId));
        }
        final @Nullable SpongeUserData currentUser = this.userCache.getIfPresent(uuidToUse);
        if (currentUser != null) {
            return CompletableFuture.completedFuture(SpongeUserView.create(uuidToUse));
        }
        if (!always && !this.userFileCache.contains(uuidToUse)) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> {
            final com.mojang.authlib.@Nullable GameProfile profile = this.server.getProfileCache().get(uuidToUse)
                .orElseGet(() -> new com.mojang.authlib.GameProfile(uuidToUse, null));
            try {
                this.createUser(profile);
            } catch (final IOException e) {
                throw new CompletionException(e);
            }
            return SpongeUserView.create(uuidToUse);
        }, this.executorService);
    }

    @Override
    public CompletableFuture<Optional<User>> load(final String lastKnownName) {
        Objects.requireNonNull(lastKnownName, "lastKnownName");
        if (lastKnownName.isEmpty() || lastKnownName.length() > 16) {
            throw new IllegalArgumentException(String.format("Invalid username %s", lastKnownName));
        }
        final com.mojang.authlib.@Nullable GameProfile mcProfile =
                Sponge.server().gameProfileManager().cache().findByName(lastKnownName.toLowerCase(Locale.ROOT))
                        .map(SpongeGameProfile::toMcProfile)
                        .orElse(null);
        if (mcProfile != null) {
            return this.load(mcProfile.getId());
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<Optional<User>> load(final GameProfile profile) {
        return this.fetchUser(this.ensureNonEmptyUUID(profile.uniqueId()), false)
                .thenApply(x -> {
                    if (x != null) {
                        return Optional.of(SpongeUserView.create(profile.uniqueId()));
                    }
                    return Optional.empty();
                });
    }

    @Override
    public Stream<GameProfile> streamAll() {
        final GameProfileCache cache = ((Server) this.server).gameProfileManager().cache();
        return this.userFileCache.knownUUIDs().map(x -> cache.findById(x).orElseGet(() -> GameProfile.of(x)));
    }

    @Override
    public CompletableFuture<Boolean> delete(final UUID uuid) {
        if (SpongeCommon.server().getPlayerList().getPlayer(Objects.requireNonNull(uuid, "uuid")) != null) {
            // cannot delete live player.
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            final @Nullable Path dataFile = this.getPlayerDataFile(uuid);
            if (dataFile != null) {
                try {
                    if (Files.deleteIfExists(dataFile)) {
                        final @Nullable SpongeUserData data = this.userCache.getIfPresent(uuid);
                        if (data != null) {
                            this.dirtyUsers.remove(data);
                        }
                        this.userCache.invalidate(uuid);
                    }
                } catch (final SecurityException | IOException e) {
                    SpongeCommon.logger().warn("Unable to delete file {}", dataFile, e);
                    return false;
                }
            }
            return true;
        }, this.executorService);
    }

    @Override
    public boolean removeFromCache(final UUID uuid) {
        final @Nullable SpongeUserData data = this.userCache.getIfPresent(uuid);
        if (data != null) {
            this.dirtyUsers.remove(data);
            this.userCache.invalidate(uuid);
            return true;
        }
        return false;
    }

    @Override
    public CompletableFuture<Boolean> forceSave(final UUID uuid) {
        final @Nullable SpongeUserData data = this.userCache.getIfPresent(uuid);
        if (data != null && this.dirtyUsers.contains(data)) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    data.save();
                } catch (final IOException e) {
                    throw new CompletionException(e);
                }
                return true;
            });

        }
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public boolean exists(final UUID playerUuid) {
        if (this.userCache.getIfPresent(playerUuid) == null) {
            // Does the file exist?
            final @Nullable Path path = this.getPlayerDataFile(playerUuid);
            return path != null && Files.exists(path);
        }
        return true;
    }

    @Override
    public Stream<GameProfile> streamOfMatches(final String lastKnownName) {
        final String nameToCheck = Objects.requireNonNull(lastKnownName, "lastKnownName").toLowerCase(Locale.ROOT);
        return ((Server) this.server).gameProfileManager().cache()
                .streamOfMatches(nameToCheck)
                .filter(gameProfile -> this.exists(gameProfile.uuid()));
    }

    private UUID ensureNonEmptyUUID(final UUID uuid) {
        if (uuid.equals(SpongeGameProfile.EMPTY_UUID)) {
            // Use Forge's FakePlayer UUID
            return SpongeUserManager.FAKEPLAYER_UUID;
        }
        return uuid;
    }

    //

    public void handlePlayerLogin(final com.mojang.authlib.GameProfile mcProfile) throws IOException {
        final @Nullable SpongeUserData currentUser = this.userCache.getIfPresent(mcProfile.getId());
        if (currentUser != null) {
            // If currentUser have this then we know that the user has changed.
            if (this.dirtyUsers.contains(currentUser)) {
                currentUser.save();
            }
            // The views will now point at the player.
            this.userCache.invalidate(currentUser.uniqueId());
        }
    }

    private void createUser(final com.mojang.authlib.GameProfile profile) throws IOException {
        final @Nullable SpongeUserData user = SpongeUserData.create(profile);
        this.userCache.put(profile.getId(), user);
        this.userFileCache.userCreated(profile.getId());
    }

    public void markDirty(final SpongeUserData user) {
        if (user != this.userCache.getIfPresent(user.uniqueId())) {
            SpongeCommon.logger()
                    .error("User {} is either online or the data has has dropped out of the cache and will not be saved.", user.uniqueId());
        } else {
            this.dirtyUsers.add(user);
        }
    }

    private @Nullable Path getPlayerDataFile(final UUID uniqueId) {
        // Note: Uses the overworld's player data
        final Path file = this.getSaveHandlerDirectory().resolve(uniqueId + ".dat");
        if (Files.exists(file)) {
            return file;
        }
        return null;
    }

    private PlayerDataStorage getSaveHandler() {
        return ((PlayerListAccessor) this.server.getPlayerList()).accessor$playerIo();
    }

    private Path getSaveHandlerDirectory() {
        return ((PlayerDataStorageAccessor) this.getSaveHandler()).accessor$playerDir().toPath();
    }

    public void saveDirtyUsers() {
        // If they are online, Minecraft will do the save automatically.
        this.dirtyUsers.removeIf(SpongeUserData::isOnline);
        for (final SpongeUserData user : new HashSet<>(this.dirtyUsers)) {
            try {
                user.save();
            } catch (final IOException ignored) {
                // There isn't much we can do here. The error has been logged, but
                // the user is still available at this time so let's continue...
            }
        }
    }

    public void unmarkDirty(final SpongeUserData user) {
        this.dirtyUsers.remove(user);
    }

    public @Nullable SpongeUserData userFromCache(final UUID uuid) {
        return this.userCache.getIfPresent(uuid);
    }

    public @Nullable User asUser(final SpongeUserData spongeUserData) {
        if (this.userCache.getIfPresent(spongeUserData.uniqueId()) == spongeUserData) {
            return SpongeUserView.create(spongeUserData.uniqueId());
        }
        return null;
    }
}
