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
package org.spongepowered.gradle.impl;

import dev.architectury.at.AccessChange;
import dev.architectury.at.AccessTransform;
import dev.architectury.at.AccessTransformSet;
import dev.architectury.at.ModifierChange;
import dev.architectury.at.io.AccessTransformFormats;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerVisitor;
import org.cadixdev.bombe.type.signature.MethodSignature;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

public abstract class ConvertAWToAT extends DefaultTask {

    @InputFiles
    public abstract ConfigurableFileCollection getAccessWideners();

    public void accessWideners(Object... paths) {
        this.getAccessWideners().from(paths);
    }

    @OutputFile
    public abstract RegularFileProperty getAccessTransformer();

    @TaskAction
    public void convert() {
        final Set<File> awFiles = this.getAccessWideners().getFiles();
        final File atFile = this.getAccessTransformer().get().getAsFile();

        final AccessTransformSet at = AccessTransformSet.create();

        for (final File awFile : awFiles) {
            try (final BufferedReader reader = Files.newBufferedReader(awFile.toPath())) {
                ConvertAWToAT.convert(reader, at);
            } catch (final IOException e) {
                throw new GradleException("Failed to read access widener: " + awFile, e);
            }
        }

        try (final BufferedWriter writer = Files.newBufferedWriter(atFile.toPath())) {
            AccessTransformFormats.FML.write(writer, at);
        } catch (IOException e) {
            throw new GradleException("Failed to write access transformer: " + atFile, e);
        }
    }

    public static void convert(final BufferedReader reader, final AccessTransformSet at) throws IOException {
        new AccessWidenerReader(new AccessWidenerVisitor() {
            @Override
            public void visitClass(final String name, final  AccessWidenerReader.AccessType access, final  boolean transitive) {
                at.getOrCreateClass(name).merge(ConvertAWToAT.convertEntry(access));
            }

            @Override
            public void visitMethod(final String owner, final String name, final String descriptor, final AccessWidenerReader.AccessType access, final boolean transitive) {
                at.getOrCreateClass(owner).mergeMethod(MethodSignature.of(name, descriptor), ConvertAWToAT.convertEntry(access));
            }

            @Override
            public void visitField(final String owner, final String name, final String descriptor, final AccessWidenerReader.AccessType access, final boolean transitive) {
                at.getOrCreateClass(owner).mergeField(name, ConvertAWToAT.convertEntry(access));
            }
        }).read(reader);
    }

    public static AccessTransform convertEntry(final AccessWidenerReader.AccessType access) {
        return switch (access) {
            case ACCESSIBLE -> AccessTransform.of(AccessChange.PUBLIC);
            case EXTENDABLE, MUTABLE -> AccessTransform.of(AccessChange.PUBLIC, ModifierChange.REMOVE);
        };
    }
}
