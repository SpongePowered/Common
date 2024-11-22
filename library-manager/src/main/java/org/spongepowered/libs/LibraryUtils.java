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
package org.spongepowered.libs;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class LibraryUtils {
    private static final char[] hexChars = "0123456789abcdef".toCharArray();

    private LibraryUtils() {
    }

    public static <T> CompletableFuture<T> asyncFailableFuture(final Callable<T> action, final Executor executor) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                future.complete(action.call());
            } catch (final Exception ex) {
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    public static String toHexString(final byte[] bytes) {
        final char[] chars = new char[bytes.length << 1];
        int i = 0;
        for (final byte b : bytes) {
            chars[i++] = LibraryUtils.hexChars[(b >> 4) & 15];
            chars[i++] = LibraryUtils.hexChars[b & 15];
        }
        return new String(chars);
    }

    public static boolean validateDigest(final String algorithm, final String expectedHash, final Path path) throws IOException, NoSuchAlgorithmException {
        try (final InputStream in = Files.newInputStream(path)) {
            return LibraryUtils.validateDigest(algorithm, expectedHash, in);
        }
    }

    public static boolean validateDigest(final String algorithm, final String expectedHash, final InputStream stream) throws IOException, NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance(algorithm);

        final byte[] buf = new byte[4096];
        int read;

        while ((read = stream.read(buf)) != -1) {
            digest.update(buf,0, read);
        }

        return expectedHash.equals(LibraryUtils.toHexString(digest.digest()));
    }

    /**
     * Downloads a file.
     *
     * @param url The source URL
     * @param file The destination file
     * @throws IOException If there is a problem while downloading the file
     */
    public static void download(final Logger logger, final URL url, final Path file, final boolean requiresRequest) throws IOException {
        Files.createDirectories(file.getParent());

        final String name = file.getFileName().toString();

        logger.info("Downloading {}. This may take a while...", name);
        logger.debug("URL -> <{}>", url);

        if (!requiresRequest) {
            try (final ReadableByteChannel in = Channels.newChannel(url.openStream());
                 final FileChannel out = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                out.transferFrom(in, 0, Long.MAX_VALUE);
            }
        } else {
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Sponge-Downloader");

            connection.connect();

            try (final ReadableByteChannel in = Channels.newChannel(connection.getInputStream());
                 final FileChannel out = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                out.transferFrom(in, 0, Long.MAX_VALUE);
            }
        }
    }

    /**
     * Downloads a file and verify its digest.
     *
     * @param url The source URL
     * @param file The destination file
     * @param algorithm The digest algorithm
     * @param expected The expected digest
     * @throws IOException If there is a problem while downloading the file
     */
    public static void downloadAndVerifyDigest(final Logger logger, final URL url, final Path file, final String algorithm, final String expected) throws IOException, NoSuchAlgorithmException {
        final String name = file.getFileName().toString();

        logger.info("Downloading {}. This may take a while...", name);
        logger.debug("URL -> <{}>", url);

        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Sponge-Downloader");

        connection.connect();

        try (final InputStream in = connection.getInputStream()) {
            LibraryUtils.transferAndVerifyDigest(logger, in, file, algorithm, expected);
        }
    }

    /**
     * Transfer a file and verify its digest.
     *
     * @param source The source stream
     * @param file The destination file
     * @param algorithm The digest algorithm
     * @param expected The expected digest
     * @throws IOException If there is a problem while transferring the file
     */
    public static void transferAndVerifyDigest(final Logger logger, final InputStream source, final Path file, final String algorithm, final String expected) throws IOException, NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance(algorithm);

        Files.createDirectories(file.getParent());

        final String name = file.getFileName().toString();
        // Pipe the download stream into the file and compute the hash
        try (final DigestInputStream stream = new DigestInputStream(source, digest);
             final ReadableByteChannel in = Channels.newChannel(stream);
             final FileChannel out = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            out.transferFrom(in, 0, Long.MAX_VALUE);
        }

        final String fileDigest = LibraryUtils.toHexString(digest.digest());

        if (expected.equals(fileDigest)) {
            logger.info("Successfully processed {} and verified checksum!", name);
        } else {
            Files.delete(file);
            throw new IOException(String.format("Checksum verification for %s failed: Expected '%s', got '%s'.", name, expected, fileDigest));
        }
    }
}
