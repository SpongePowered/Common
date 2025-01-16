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
package org.spongepowered.common.profile;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.util.UndashedUuid;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.core.util.Throwables;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.GameProfileProvider;
import org.spongepowered.api.profile.ProfileNotFoundException;
import org.spongepowered.common.SpongeCommon;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class UncachedGameProfileProvider implements GameProfileProvider {

    private static final Gson GSON = new Gson();

    private <T> CompletableFuture<T> submit(final Callable<T> callable) {
        return SpongeCommon.asyncScheduler().submit(callable);
    }

    private void submit(final Runnable runnable) {
        SpongeCommon.asyncScheduler().execute(runnable);
    }

    /*
     * Uncached, but not really. The Mojang API limits only allows a single profile request
     * for the same unique id once per minute. This request is also shared by signed and
     * unsigned data.
     */

    private final LoadingCache<UUID, CompletableFuture<@Nullable CachedProfile>> profileCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .build(this::requestProfile);

    private final Cache<String, @Nullable CachedProfile> profileByNameCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .build();

    private CompletableFuture<@Nullable CachedProfile> requestProfile(final UUID uniqueId) {
        return this.submit(() -> {
            final com.mojang.authlib.GameProfile mcProfile;

            if (SpongeGameProfileManager.canLookup(uniqueId)) { // only attempt to resolve profiles for online mode clients
                ProfileResult result = SpongeCommon.server().getSessionService().fetchProfile(uniqueId, true);
                if (result != null) {
                    mcProfile = result.profile();
                } else {
                    mcProfile = null;
                }
            } else {
                mcProfile = null;
            }
            if (mcProfile == null) {
                return null;
            }
            final CachedProfile cachedProfile = new CachedProfile(SpongeGameProfile.of(mcProfile));
            this.profileByNameCache.put(mcProfile.getName().toLowerCase(Locale.ROOT), cachedProfile);
            return cachedProfile;
        });
    }

    @Override
    public CompletableFuture<GameProfile> profile(final UUID uniqueId, final boolean signed) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return Objects.requireNonNull(this.profileCache.get(uniqueId)).thenApply(profile -> {
            if (profile == null) {
                throw new ProfileNotFoundException(uniqueId.toString());
            }
            return signed ? profile.getSigned() : profile.getUnsigned();
        });
    }

    @Override
    public CompletableFuture<GameProfile> profile(final String name, final boolean signed) {
        Objects.requireNonNull(name, "name");
        final CachedProfile cachedProfile = this.profileByNameCache.getIfPresent(name.toLowerCase(Locale.ROOT));
        if (cachedProfile != null) {
            return CompletableFuture.completedFuture(signed ? cachedProfile.getSigned() : cachedProfile.getUnsigned());
        }
        return this.basicProfile(name).thenCompose(basicProfile -> this.profile(basicProfile.uniqueId(), signed));
    }

    private static final class CachedProfile {

        private final GameProfile signed;
        private volatile @Nullable GameProfile unsigned;

        private CachedProfile(final GameProfile signed) {
            this.signed = signed;
        }

        public GameProfile getSigned() {
            return this.signed;
        }

        public GameProfile getUnsigned() {
            GameProfile unsigned = this.unsigned;
            if (unsigned != null) {
                return unsigned;
            }
            unsigned = SpongeGameProfile.unsignedOf(this.signed);
            this.unsigned = unsigned;
            return unsigned;
        }
    }

    @Override
    public CompletableFuture<GameProfile> basicProfile(final UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return Objects.requireNonNull(this.profileCache.get(uniqueId)).thenApply(profile -> {
            if (profile == null) {
                throw new ProfileNotFoundException(uniqueId.toString());
            }
            return profile.getSigned().withoutProperties();
        });
    }

    @Override
    public CompletableFuture<GameProfile> basicProfile(final String name, final @Nullable Instant time) {
        Objects.requireNonNull(name, "name");
        if (time != null) {
            return this.submit(() -> {
                final GameProfile profile;
                try {
                    profile = this.requestBasicProfileAt(name, time);
                } catch (final Exception ex) {
                    Throwables.rethrow(ex);
                    return null;
                }
                if (profile == null) {
                    throw new ProfileNotFoundException(name);
                }
                return profile;
            });
        }
        final CompletableFuture<GameProfile> result = new CompletableFuture<>();
        this.submit(() -> SpongeCommon.server().getProfileRepository().findProfilesByNames(new String[]{name},
                new SingleProfileLookupCallback(result)));
        return result;
    }

    @Override
    public CompletableFuture<Map<String, GameProfile>> basicProfiles(final Iterable<String> names, final @Nullable Instant time) {
        Objects.requireNonNull(names, "names");
        final CompletableFuture<Map<String, GameProfile>> result = new CompletableFuture<>();
        if (time != null) {
            this.submit(() -> {
                final Map<String, GameProfile> resultMap = new HashMap<>();
                for (final String name : names) {
                    try {
                        final GameProfile profile = this.requestBasicProfileAt(name, time);
                        if (profile != null) {
                            resultMap.put(name, profile);
                        }
                    } catch (final Exception e) {
                        result.completeExceptionally(e);
                        return;
                    }
                }
                result.complete(resultMap);
            });
        }
        final List<String> nameList = Lists.newArrayList(names);
        final String[] namesArray = nameList.toArray(new String[0]);
        this.submit(() -> SpongeCommon.server().getProfileRepository().findProfilesByNames(namesArray,
                new MapProfileLookupCallback(result, nameList)));
        return result;
    }

    private @Nullable GameProfile requestBasicProfileAt(final String name, final Instant time) throws Exception {
        final URL url = new URI("https://api.mojang.com/user/profiles/minecraft/" + name + "?at=" + time.getEpochSecond()).toURL();
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        try {
            final int code = connection.getResponseCode();
            if (code == 200) {
                final JsonObject json = UncachedGameProfileProvider.GSON.fromJson(new InputStreamReader(connection.getInputStream()), JsonObject.class);
                return this.parseGameProfile(json);
            } else if (code == 204) {
                return null;
            } else {
                this.handleError(connection);
                return null;
            }
        } finally {
            IOUtils.closeQuietly(connection.getInputStream());
        }
    }

    private void handleError(final HttpURLConnection connection) throws IOException {
        final int code = connection.getResponseCode();
        if (code == 404) {
            throw new IOException("Not found");
        }
        final InputStream is = connection.getInputStream();
        if (is.available() > 0) {
            final JsonObject json = UncachedGameProfileProvider.GSON.fromJson(new InputStreamReader(is), JsonObject.class);
            final String error = json.has("error") ? json.get("error").getAsString() : null;
            final String errorMessage = json.has("errorMessage") ? json.get("errorMessage").getAsString() : null;
            if (error != null) {
                if (error.equals("IllegalArgumentException")) {
                     throw new IOException(new IllegalArgumentException(errorMessage));
                }
                throw new IOException(error + ": " + errorMessage);
            }
        } else {
            throw new IOException("Error code: " + code);
        }
    }

    private GameProfile parseGameProfile(final JsonObject json) {
        final UUID uniqueId = UndashedUuid.fromStringLenient(json.get("id").getAsString());
        final String name = json.get("name").getAsString();

        final JsonArray propertiesJson = json.getAsJsonArray("properties");
        final ImmutableList.Builder<SpongeProfileProperty> properties = ImmutableList.builder();
        for (final JsonElement propertyJson : propertiesJson) {
            final JsonObject propertyObj = propertyJson.getAsJsonObject();
            final String propertyName = propertyObj.get("name").getAsString();
            final String value = propertyObj.get("value").getAsString();
            final String signature = propertyObj.has("signature") ?
                    propertyObj.get("signature").getAsString() : null;
            properties.add(new SpongeProfileProperty(propertyName, value, signature));
        }

        return new SpongeGameProfile(uniqueId, name, properties.build());
    }

    private static final class SingleProfileLookupCallback implements ProfileLookupCallback {

        private final CompletableFuture<GameProfile> result;

        private SingleProfileLookupCallback(final CompletableFuture<GameProfile> result) {
            this.result = result;
        }

        @Override
        public void onProfileLookupSucceeded(final com.mojang.authlib.GameProfile profile) {
            this.result.complete(SpongeGameProfile.of(profile));
        }

        @Override
        public void onProfileLookupFailed(final String profileName, final Exception exception) {
            if (exception instanceof com.mojang.authlib.yggdrasil.ProfileNotFoundException) {
                this.result.completeExceptionally(new ProfileNotFoundException(profileName, exception.getCause()));
            } else {
                this.result.completeExceptionally(exception);
            }
        }
    }

    private static final class MapProfileLookupCallback implements ProfileLookupCallback {

        private final CompletableFuture<Map<String, GameProfile>> result;
        private final List<String> nameQueue;

        private Map<String, GameProfile> resultMap = new HashMap<>();

        private MapProfileLookupCallback(final CompletableFuture<Map<String, GameProfile>> result, final List<String> nameQueue) {
            this.result = result;
            this.nameQueue = nameQueue;
        }

        public void complete() {
            if (this.resultMap != null) {
                this.result.complete(this.resultMap);
            }
        }

        @Override
        public void onProfileLookupSucceeded(final com.mojang.authlib.GameProfile profile) {
            String originalName = null;
            while (originalName == null && !this.nameQueue.isEmpty()) {
                final String name = this.nameQueue.remove(0);
                if (name.equalsIgnoreCase(profile.getName())) {
                    originalName = name;
                }
            }
            if (originalName == null) {
                throw new IllegalStateException();
            }
            if (this.resultMap != null) {
                this.resultMap.put(originalName, SpongeGameProfile.of(profile));
            }
        }

        @Override
        public void onProfileLookupFailed(final String profileName, final Exception exception) {
            if (exception instanceof com.mojang.authlib.yggdrasil.ProfileNotFoundException) {
                return;
            }
            this.resultMap = null;
            this.result.completeExceptionally(exception);
        }
    }
}
