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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.SpongeCommon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is an optimization for frequent calls to
 * retrieve all the players who have visited
 * the server. This could be part of tab completion
 * where good server performance is critical.
 *
 * While one could achieve the same with other
 * relevant caches, the file system is always the
 * source of truth.
 */
final class SpongeUserFileCache {

    private final Supplier<Path> path;

    private Set<UUID> knownUniqueIds = new HashSet<>();

    private @Nullable WatchService watchService = null;
    private @Nullable WatchKey watchKey = null;

    SpongeUserFileCache(final Supplier<Path> path) {
        this.path = path;
    }

    public void init() {
        final Path path = this.path.get();
        this.shutdownWatcher();
        try {
            this.watchService = path.getFileSystem().newWatchService();
            this.watchKey = path.register(this.watchService,
                StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
        } catch (final IOException e) {
            SpongeCommon.logger().warn("Could not start file watcher", e);
            this.shutdownWatcher();
            return;
        }

        this.scanFiles(path);
    }

    private void scanFiles(final Path path) {
        if (!Files.isDirectory(path)) {
            return;
        }

        try (final Stream<Path> list = Files.list(path)) {
            this.knownUniqueIds = list.map(SpongeUserFileCache::getUniqueIdFromPath)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        } catch (final IOException e) {
            SpongeCommon.logger().error("Failed to get player files", e);
            return;
        }

        this.pollFilesystemWatcher(true);
    }

    private void pollFilesystemWatcher() {
        this.pollFilesystemWatcher(false);
    }

    private void pollFilesystemWatcher(final boolean initialPoll) {
        if (this.watchKey == null || !this.watchKey.isValid()) {
            if (!initialPoll) {
                // Reboot this if it's somehow failed.
                this.init();
            }
            return;
        }

        // We've already got the UUIDs, so we need to just see if the file system
        // watcher has found anymore (or removed any).
        final Map<String, MutableWatchEvent> watcherUpdateMap = new HashMap<>();
        for (final WatchEvent<?> event : this.watchKey.pollEvents()) {
            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                if (!initialPoll) {
                    this.scanFiles(this.path.get());
                } else {
                    this.watchKey.cancel();
                }
                return;
            }

            @SuppressWarnings("unchecked") final WatchEvent<Path> ev = (WatchEvent<Path>) event;
            final @Nullable Path file = ev.context();

            // It is possible that the context is null, in which case, ignore it.
            if (file != null) {
                final String filename = file.getFileName().toString();

                // We don't determine the UUIDs yet, we'll only do that if we need to.
                watcherUpdateMap.computeIfAbsent(filename, f -> new MutableWatchEvent()).set(ev.kind());
            }
        }

        // Now we know what the final result is, we can act upon it.
        for (final Map.Entry<String, MutableWatchEvent> entry : watcherUpdateMap.entrySet()) {
            final WatchEvent.Kind<?> kind = entry.getValue().get();
            if (kind == null) {
                continue;
            }

            final @Nullable UUID uuid = SpongeUserFileCache.getUniqueIdFromPath(entry.getKey());
            if (uuid == null) {
                continue;
            }

            // It will only be create or delete here.
            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                this.knownUniqueIds.add(uuid);
            } else {
                this.knownUniqueIds.remove(uuid);
            }
        }
    }

    public void userCreated(final UUID uniqueId) {
        this.pollFilesystemWatcher();
        this.knownUniqueIds.add(uniqueId);
    }

    public boolean contains(final UUID uniqueId) {
        this.pollFilesystemWatcher();
        return this.knownUniqueIds.contains(uniqueId);
    }

    public Stream<UUID> knownUUIDs() {
        this.pollFilesystemWatcher();
        return this.knownUniqueIds.stream();
    }

    public void shutdownWatcher() {
        if (this.watchKey != null) {
            this.watchKey.cancel();
            this.watchKey = null;
        }

        if (this.watchService != null) {
            try {
                this.watchService.close();
            } catch (final IOException ignored) {
            }

            this.watchService = null;
        }
    }

    private static @Nullable UUID getUniqueIdFromPath(final Path path) {
        return SpongeUserFileCache.getUniqueIdFromPath(path.getFileName().toString());
    }

    private static @Nullable UUID getUniqueIdFromPath(final String fileName) {
        final String[] parts = fileName.split("\\.", 2);
        if (parts.length != 2 || parts[0].length() != 36 || !parts[1].equals("dat")) {
            return null;
        }
        try {
            return UUID.fromString(parts[0]);
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Filters that sequences of CREATE -> DELETE
     * or DELETE -> CREATE do not raise changes.
     */
    private static final class MutableWatchEvent {

        private WatchEvent.Kind<?> kind = null;

        public WatchEvent.@Nullable Kind<?> get() {
            return this.kind;
        }

        public void set(WatchEvent.Kind<?> kind) {
            if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                // This should never happen, we don't listen to this.
                // However, if it does, treat it as a create, because it
                // infers the existence of the file.
                kind = StandardWatchEventKinds.ENTRY_CREATE;
            }

            if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_DELETE) {
                if (this.kind != null && this.kind != kind) {
                    this.kind = null;
                } else {
                    this.kind = kind;
                }
            }
        }
    }
}
