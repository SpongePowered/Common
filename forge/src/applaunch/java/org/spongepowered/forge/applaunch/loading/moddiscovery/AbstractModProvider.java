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
package org.spongepowered.forge.applaunch.loading.moddiscovery;

import cpw.mods.jarhandling.SecureJar;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class AbstractModProvider implements IModProvider {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean isValid(final IModFile modFile) {
        return true;
    }

    @Override
    public void initArguments(final Map<String, ?> arguments) {
    }

    @Override
    public void scanFile(final IModFile file, final Consumer<Path> pathConsumer) {
        LOGGER.debug("Scan started: {}", file);
        final SecureJar jar = file.getSecureJar();

        Consumer<Path> consumer = pathConsumer;
        final AtomicReference<SecureJar.Status> minStatus = new AtomicReference<>(SecureJar.Status.NONE);
        if (jar.hasSecurityData()) {
            minStatus.set(SecureJar.Status.VERIFIED);
            consumer = path -> {
                pathConsumer.accept(path);
                SecureJar.Status status = jar.verifyPath(path);
                if (status.ordinal() < minStatus.get().ordinal())
                    minStatus.set(status);
            };
        }

        try (final Stream<Path> files = Files.walk(jar.getRootPath())) {
            files.filter(p -> p.toString().endsWith(".class")).forEach(consumer);
        } catch (IOException e) {
            LOGGER.debug("Scan failed: {}", file);
            minStatus.set(SecureJar.Status.NONE);
        }

        file.setSecurityStatus(minStatus.get());
        LOGGER.debug("Scan finished: {}", file);
    }
}
