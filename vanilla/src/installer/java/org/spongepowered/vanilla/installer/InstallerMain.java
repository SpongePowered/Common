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
package org.spongepowered.vanilla.installer;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.minecraftforge.fart.api.Renamer;
import net.minecraftforge.fart.api.SignatureStripperConfig;
import net.minecraftforge.fart.api.SourceFixerConfig;
import net.minecraftforge.fart.api.Transformer;
import net.minecraftforge.srgutils.IMappingFile;
import org.spongepowered.libs.LibraryManager;
import org.spongepowered.libs.LibraryUtils;
import org.spongepowered.vanilla.installer.library.TinyLogger;
import org.spongepowered.vanilla.installer.model.GroupArtifactVersion;
import org.spongepowered.vanilla.installer.model.mojang.BundleElement;
import org.spongepowered.vanilla.installer.model.mojang.BundlerMetadata;
import org.spongepowered.vanilla.installer.model.mojang.FormatVersion;
import org.spongepowered.vanilla.installer.model.mojang.Version;
import org.spongepowered.vanilla.installer.model.mojang.VersionManifest;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public final class InstallerMain {

    private static final String COLLECTION_BOOTSTRAP = "bootstrap"; // boot layer
    private static final String COLLECTION_MAIN = "main"; // game layer
    private static final int MAX_TRIES = 2;

    private final Installer installer;

    public InstallerMain(final String[] args) throws Exception {
        LauncherCommandLine.configure(args);
        this.installer = new Installer(LauncherCommandLine.installerDirectory);
    }

    public static void main(final String[] args) throws Exception {
        new InstallerMain(args).run();
    }

    public void run() {
        try  {
            this.downloadAndRun();
        } catch (final Exception ex) {
            Logger.error(ex, "Failed to download Sponge libraries and/or Minecraft");
            System.exit(2);
        } finally {
            this.installer.getLibraryManager().finishedProcessing();
        }
    }

    public void downloadAndRun() throws Exception {
        ServerAndLibraries remappedMinecraftJar = null;
        Version mcVersion = null;
        try {
            mcVersion = this.downloadMinecraftManifest();
        } catch (final IOException ex) {
            remappedMinecraftJar = this.recoverFromMinecraftDownloadError(ex);
            this.installer.getLibraryManager().validate();
        }

        final LibraryManager libraryManager = this.installer.getLibraryManager();
        try {
            if (mcVersion != null) {
                final CompletableFuture<Path> mappingsFuture = this.downloadMappings(mcVersion, LauncherCommandLine.librariesDirectory);
                final CompletableFuture<Path> originalMcFuture = this.downloadMinecraft(mcVersion, LauncherCommandLine.librariesDirectory);
                final CompletableFuture<ServerAndLibraries> extractedFuture = originalMcFuture
                    .thenApplyAsync(bundle -> this.extractBundle(bundle, LauncherCommandLine.librariesDirectory), libraryManager.preparationWorker());
                final CompletableFuture<ServerAndLibraries> remappedMinecraftJarFuture = mappingsFuture.thenCombineAsync(extractedFuture, (mappings, minecraft) -> {
                    try {
                        return this.remapMinecraft(minecraft, mappings);
                    } catch (final IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                }, libraryManager.preparationWorker());
                libraryManager.validate();
                remappedMinecraftJar = remappedMinecraftJarFuture.get();
            }
        } catch (final ExecutionException ex) {
            final /* @Nullable */ Throwable cause = ex.getCause();
            remappedMinecraftJar = this.recoverFromMinecraftDownloadError(cause instanceof Exception ? (Exception) cause : ex);
        }
        assert remappedMinecraftJar != null; // always assigned or thrown

        // Minecraft itself is on the main layer
        libraryManager.addLibrary(InstallerMain.COLLECTION_MAIN, new LibraryManager.Library("minecraft", remappedMinecraftJar.server()));

        // Other libs are on the bootstrap layer
        for (final Map.Entry<GroupArtifactVersion, Path> entry : remappedMinecraftJar.libraries().entrySet()) {
            final GroupArtifactVersion artifact = entry.getKey();
            final Path path = entry.getValue();

            libraryManager.addLibrary(InstallerMain.COLLECTION_BOOTSTRAP, new LibraryManager.Library(artifact.toString(), path));
        }

        this.installer.getLibraryManager().finishedProcessing();

        Logger.info("Environment has been verified.");

        final Set<String> seenLibs = new HashSet<>();
        final Path[] bootLibs = this.installer.getLibraryManager().getAll(InstallerMain.COLLECTION_BOOTSTRAP).stream()
            .peek(lib -> seenLibs.add(lib.name()))
            .map(LibraryManager.Library::file)
            .toArray(Path[]::new);

        final Path[] gameLibs = this.installer.getLibraryManager().getAll(InstallerMain.COLLECTION_MAIN).stream()
            .filter(lib -> !seenLibs.contains(lib.name()))
            .map(LibraryManager.Library::file)
            .toArray(Path[]::new);

        final URL rootJar = InstallerMain.class.getProtectionDomain().getCodeSource().getLocation();
        final URI fsURI = new URI("jar:" + rootJar);
        System.setProperty("sponge.rootJarFS", fsURI.toString());

        final FileSystem fs = FileSystems.newFileSystem(fsURI, Map.of());
        final Path spongeBoot = newJarInJar(fs.getPath("jars", "spongevanilla-boot.jar"));

        String launchTarget = LauncherCommandLine.launchTarget;
        if (launchTarget == null) {
            final Path manifestFile = fs.getPath("META-INF", "MANIFEST.MF");
            try (final InputStream stream = Files.newInputStream(manifestFile)) {
                final Manifest manifest = new Manifest(stream);
                launchTarget = manifest.getMainAttributes().getValue(Constants.ManifestAttributes.LAUNCH_TARGET);
            }
        }

        final StringBuilder gameLibsEnv = new StringBuilder();
        for (final Path lib : gameLibs) {
            gameLibsEnv.append(lib.toAbsolutePath()).append(';');
        }
        gameLibsEnv.setLength(gameLibsEnv.length() - 1);
        System.setProperty("sponge.gameResources", gameLibsEnv.toString());

        final List<String> gameArgs = new ArrayList<>(LauncherCommandLine.remainingArgs);
        gameArgs.add("--launchTarget");
        gameArgs.add(launchTarget);
        Collections.addAll(gameArgs, this.installer.getLauncherConfig().args.split(" "));

        InstallerMain.bootstrap(bootLibs, spongeBoot, gameArgs.toArray(new String[0]));
    }

    private static Path newJarInJar(final Path jar) {
        try {
            URI jij = new URI("jij:" + jar.toAbsolutePath().toUri().getRawSchemeSpecificPart()).normalize();
            final Map<String, ?> env = Map.of("packagePath", jar);
            FileSystem jijFS = FileSystems.newFileSystem(jij, env);
            return jijFS.getPath("/"); // root of the archive to load
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends Throwable> ServerAndLibraries recoverFromMinecraftDownloadError(final T ex) throws T {
        final Path expectedUnpacked = this.expectedMinecraftLocation(LauncherCommandLine.librariesDirectory, Constants.Libraries.MINECRAFT_VERSION_TARGET);
        final Path expectedRemapped = this.expectedRemappedLocation(expectedUnpacked);
        // Re-read bundler metadata (needs original bundled location)
        if (Files.exists(expectedRemapped)) {
            Logger.warn(ex, "Failed to download and remap Minecraft. An existing jar exists, so we will attempt to use that instead.");
            return this.extractBundle(this.expectedBundleLocation(expectedUnpacked), LauncherCommandLine.librariesDirectory);
        } else {
            throw ex;
        }
    }

    private static void bootstrap(final Path[] bootLibs, final Path spongeBoot, final String[] args) throws Exception {
        final URL[] urls = new URL[bootLibs.length];
        for (int i = 0; i < bootLibs.length; i++) {
            urls[i] = bootLibs[i].toAbsolutePath().toUri().toURL();
        }

        final List<Path[]> classpath = new ArrayList<>();
        for (final Path lib : bootLibs) {
            classpath.add(new Path[] { lib });
        }
        classpath.add(new Path[] { spongeBoot });

        URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());
        ClassLoader previousLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(loader);
            final Class<?> cl = Class.forName("net.minecraftforge.bootstrap.Bootstrap", false, loader);
            final Object instance = cl.getDeclaredConstructor().newInstance();
            final Method m = cl.getDeclaredMethod("bootstrapMain", String[].class, List.class);
            m.setAccessible(true);
            m.invoke(instance, args, classpath);
        } catch (final Exception ex) {
            final Throwable cause = ex instanceof InvocationTargetException ? ex.getCause() : ex;
            Logger.error(cause, "Failed to invoke bootstrap main due to an error");
            System.exit(1);
        } finally {
            Thread.currentThread().setContextClassLoader(previousLoader);
        }
    }

    private Version downloadMinecraftManifest() throws Exception {
        Logger.info("Downloading the Minecraft versions manifest...");

        VersionManifest.Version foundVersionManifest = null;

        final Gson gson = new Gson();
        final URLConnection conn = new URI(Constants.Libraries.MINECRAFT_MANIFEST_URL).toURL().openConnection();
        conn.setConnectTimeout(5 /* seconds */ * 1000);
        try (final JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()))) {
            final VersionManifest manifest = gson.fromJson(reader, VersionManifest.class);
            for (final VersionManifest.Version version : manifest.versions()) {
                if (Constants.Libraries.MINECRAFT_VERSION_TARGET.equals(version.id())) {
                    foundVersionManifest = version;
                    break;
                }
            }
        }

        if (foundVersionManifest == null) {
            throw new IOException(String.format("Failed to find version manifest for '%s'!", Constants.Libraries.MINECRAFT_VERSION_TARGET));
        }

        final Version version;
        try (final JsonReader reader = new JsonReader(new InputStreamReader(foundVersionManifest.url().openStream()))) {
            version = gson.fromJson(reader, Version.class);
        }

        if (version == null) {
            throw new IOException(String.format("Failed to download version information for '%s'!", Constants.Libraries.MINECRAFT_VERSION_TARGET));
        }

        return version;
    }

    private Path expectedMinecraftLocation(final Path librariesDirectory, final String version) {
        return librariesDirectory.resolve(Constants.Libraries.MINECRAFT_PATH_PREFIX)
            .resolve(version)
            .resolve(Constants.Libraries.MINECRAFT_SERVER_JAR_NAME + ".jar");
    }

    private Path expectedRemappedLocation(final Path originalLocation) {
        return originalLocation.resolveSibling(Constants.Libraries.MINECRAFT_SERVER_JAR_NAME + "-remapped.jar");
    }

    private Path expectedBundleLocation(final Path originalLocation) {
        return originalLocation.resolveSibling(Constants.Libraries.MINECRAFT_SERVER_JAR_NAME + "-bundle.jar");
    }

    private CompletableFuture<Path> downloadMinecraft(final Version version, final Path librariesDirectory) {
        return LibraryUtils.asyncFailableFuture(() -> {
            final Path downloadTarget = this.expectedBundleLocation(this.expectedMinecraftLocation(librariesDirectory, version.id()));
            final Version.Downloads.Download server = version.downloads().server();

            if (Files.notExists(downloadTarget)) {
                if (!this.installer.getLauncherConfig().autoDownloadLibraries) {
                    throw new IOException(String.format("The Minecraft jar is not located at '%s' and downloading it has been turned off.", downloadTarget));
                }
                LibraryUtils.downloadAndVerifyDigest(TinyLogger.INSTANCE, server.url(), downloadTarget, "SHA-1", server.sha1());
            } else {
                if (this.installer.getLauncherConfig().checkLibraryHashes) {
                    Logger.info("Detected existing Minecraft Server jar, verifying hashes...");

                    // Pipe the download stream into the file and compute the SHA-1
                    if (LibraryUtils.validateDigest("SHA-1", server.sha1(), downloadTarget)) {
                        Logger.info("Minecraft Server jar verified!");
                    } else {
                        Logger.error("Checksum verification failed: Expected {}. Deleting cached Minecraft Server jar...", server.sha1());
                        Files.delete(downloadTarget);
                        LibraryUtils.downloadAndVerifyDigest(TinyLogger.INSTANCE, server.url(), downloadTarget, "SHA-1", server.sha1());
                    }
                } else {
                    Logger.info("Detected existing Minecraft jar. Skipping hash check as that is turned off...");
                }
            }
            return downloadTarget;
        }, this.installer.getLibraryManager().preparationWorker());
    }

    private ServerAndLibraries extractBundle(final Path bundleJar, final Path librariesDirectory) {
        final Path serverDestination = this.expectedMinecraftLocation(librariesDirectory, Constants.Libraries.MINECRAFT_VERSION_TARGET);
        try (final JarFile bundle = new JarFile(bundleJar.toFile())) {
            final Optional<BundlerMetadata> metaOpt = BundlerMetadata.read(bundle);
            if (metaOpt.isEmpty()) {
                return new ServerAndLibraries(bundleJar, Map.of());
            }
            final BundlerMetadata md = metaOpt.get();
            // Check version
            if (!md.version().equals(new FormatVersion(1, 0))) {
                Logger.warn("Read bundler metadata from server jar with version {}, but we only support 1.0", md.version());
            }

            // Extract server
            boolean serverExtractionNeeded = true;
            final BundleElement server = md.server();
            if (Files.exists(serverDestination)) {
                if (LibraryUtils.validateDigest("SHA-256", server.sha256(), serverDestination)) {
                    // library is valid
                    serverExtractionNeeded = false;
                }
            }
            if (serverExtractionNeeded) {
                final ZipEntry serverEntry = bundle.getEntry(server.path());
                try (final InputStream in = bundle.getInputStream(serverEntry)) {
                    LibraryUtils.transferAndVerifyDigest(TinyLogger.INSTANCE, in, serverDestination, "SHA-256", server.sha256());
                }
            }

            // Extract libraries
            final Map<GroupArtifactVersion, Path> libs = new HashMap<>();
            for (final BundleElement library : md.libraries()) {
                final GroupArtifactVersion gav = GroupArtifactVersion.parse(library.id());
                final Path destination = gav.resolve(librariesDirectory).resolve(gav.artifact() + '-' + gav.version() + (gav.classifier() == null ? "" : '-' + gav.classifier()) +".jar");

                if (Files.exists(destination)) {
                    if (LibraryUtils.validateDigest("SHA-256", library.sha256(), destination)) {
                       // library is valid
                       libs.put(gav, destination);
                       continue;
                    }
                }

                final ZipEntry entry = bundle.getEntry(library.path());
                try (final InputStream in = bundle.getInputStream(entry)) {
                    LibraryUtils.transferAndVerifyDigest(TinyLogger.INSTANCE, in, destination, "SHA-256", library.sha256());
                    libs.put(gav, destination);
                }
            }

            return new ServerAndLibraries(serverDestination, libs);
        } catch (final IOException | NoSuchAlgorithmException ex) {
            Logger.error(ex, "Failed to extract bundle from {}", bundleJar);
            throw new RuntimeException(ex);
        }
    }

    private CompletableFuture<Path> downloadMappings(final Version version, final Path librariesDirectory) {
        return LibraryUtils.asyncFailableFuture(() -> {
            Logger.info("Setting up names for Minecraft {}", Constants.Libraries.MINECRAFT_VERSION_TARGET);
            final Path downloadTarget = librariesDirectory.resolve(Constants.Libraries.MINECRAFT_MAPPINGS_PREFIX)
                    .resolve(Constants.Libraries.MINECRAFT_VERSION_TARGET)
                    .resolve(Constants.Libraries.MINECRAFT_MAPPINGS_NAME);

            final Version.Downloads.Download mappings = version.downloads().server_mappings();
            if (mappings == null) {
                throw new IOException(String.format("Mappings were not included in version manifest for %s", Constants.Libraries.MINECRAFT_VERSION_TARGET));
            }

            final boolean checkHashes = this.installer.getLauncherConfig().checkLibraryHashes;
            if (Files.exists(downloadTarget)) {
                if (checkHashes) {
                    Logger.info("Detected existing mappings, verifying hashes...");
                    if (LibraryUtils.validateDigest("SHA-1", mappings.sha1(), downloadTarget)) {
                        Logger.info("Mappings verified!");
                        return downloadTarget;
                    } else {
                        Logger.error("Checksum verification failed: Expected {}. Deleting cached server mappings file...", mappings.sha1());
                        Files.delete(downloadTarget);
                    }
                } else {
                    return downloadTarget;
                }
            }

            if (this.installer.getLauncherConfig().autoDownloadLibraries) {
                if (checkHashes) {
                    LibraryUtils.downloadAndVerifyDigest(TinyLogger.INSTANCE, mappings.url(), downloadTarget, "SHA-1", mappings.sha1());
                } else {
                    LibraryUtils.download(TinyLogger.INSTANCE, mappings.url(), downloadTarget, false);
                }
            } else {
                throw new IOException(String.format("Mappings were not located at '%s' and downloading them has been turned off.", downloadTarget));
            }

            return downloadTarget;
        }, this.installer.getLibraryManager().preparationWorker());
    }

    private ServerAndLibraries remapMinecraft(final ServerAndLibraries minecraft, final Path serverMappings) throws IOException {
        Logger.info("Checking if we need to remap Minecraft...");
        final Path outputJar = this.expectedRemappedLocation(minecraft.server());
        final Path tempOutput = outputJar.resolveSibling(Constants.Libraries.MINECRAFT_SERVER_JAR_NAME + "_remapped.jar.tmp");

        if (Files.exists(outputJar)) {
            Logger.info("Remapped Minecraft detected, skipping...");
            return minecraft.server(outputJar);
        }

        Logger.info("Remapping Minecraft. This may take a while...");
        final IMappingFile mappings = IMappingFile.load(serverMappings.toFile()).reverse();

        final Renamer.Builder renamerBuilder = Renamer.builder()
            .add(Transformer.parameterAnnotationFixerFactory())
            .add(ctx -> {
                final Transformer backing = Transformer.renamerFactory(mappings, false).create(ctx);
                return new Transformer() {
                    @Override
                    public ClassEntry process(final ClassEntry entry) {
                        final String name = entry.getName();
                        if (name.startsWith("it/unimi")
                            || name.startsWith("com/google")
                            || name.startsWith("com/mojang/datafixers")
                            || name.startsWith("com/mojang/brigadier")
                            || name.startsWith("org/apache")) {
                            return entry;
                        }
                        return backing.process(entry);
                    }

                    @Override
                    public ManifestEntry process(final ManifestEntry entry) {
                        return backing.process(entry);
                    }

                    @Override
                    public ResourceEntry process(final ResourceEntry entry) {
                        return backing.process(entry);
                    }

                    @Override
                    public Collection<? extends Entry> getExtras() {
                        return backing.getExtras();
                    }
                };
            })
            .add(Transformer.recordFixerFactory())
            .add(Transformer.parameterAnnotationFixerFactory())
            .add(Transformer.sourceFixerFactory(SourceFixerConfig.JAVA))
            .add(Transformer.signatureStripperFactory(SignatureStripperConfig.ALL))
            .logger(Logger::debug); // quiet

        try (final Renamer ren = renamerBuilder.build()) {
            ren.run(minecraft.server.toFile(), tempOutput.toFile());
        }

        // Restore file
        try {
            Files.move(tempOutput, outputJar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (final AccessDeniedException ex) {
            // Sometimes because of file locking this will fail... Let's just try again and hope for the best
            // Thanks Windows!
            for (int tries = 0; tries < InstallerMain.MAX_TRIES; ++tries) {
                // Pause for a bit
                try {
                    Thread.sleep(5 * tries);
                    Files.move(tempOutput, outputJar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (final AccessDeniedException ex2) {
                    if (tries == InstallerMain.MAX_TRIES - 1) {
                        throw ex;
                    }
                } catch (final InterruptedException exInterrupt) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }

        return minecraft.server(outputJar);
    }

    record ServerAndLibraries(Path server, Map<GroupArtifactVersion, Path> libraries) {
        ServerAndLibraries {
            libraries = Map.copyOf(libraries);
        }

        public ServerAndLibraries server(final Path server) {
            return new ServerAndLibraries(server, this.libraries);
        }
    }
}
