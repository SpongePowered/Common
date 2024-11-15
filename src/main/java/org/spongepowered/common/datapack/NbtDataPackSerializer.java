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
package org.spongepowered.common.datapack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.datapack.DataPackEntry;
import org.spongepowered.common.SpongeCommon;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class NbtDataPackSerializer<T extends DataPackEntry<T>> extends DataPackSerializer<CompoundTag, T> {

    public NbtDataPackSerializer(final DataPackEncoder<CompoundTag, T> encoder, final DataPackDecoder<CompoundTag, T> decoder) {
        super(encoder, decoder);
    }

    @Override
    public String fileEnding() {
        return ".nbt";
    }

    @Override
    public void serializeObject(final SpongeDataPack<CompoundTag, T> pack, final Path packDir, final T packEntry) throws IOException {
        final CompoundTag serialized = this.encoder.encode(packEntry, SpongeCommon.vanillaRegistryAccess());
        final Path file = this.packEntryFile(pack.type(), packEntry.key(), packDir);
        Files.createDirectories(file.getParent());
        NbtDataPackSerializer.writeFile(file, serialized);
    }

    @Override
    protected void serializeAdditional(final SpongeDataPack<CompoundTag, T> type, Path packDir, T entry) throws IOException {
    }

    @Override
    public T deserialize(final SpongeDataPack<CompoundTag, T> pack, final Path file, final ResourceKey key) throws IOException {
        try (final InputStream stream = Files.newInputStream(file)) {
            return this.deserialize(pack, stream, key);
        }
    }

    @Override
    public T deserialize(final SpongeDataPack<CompoundTag, T> pack, final InputStream is, final ResourceKey key) throws IOException {
        try (final DataInputStream dis = new DataInputStream(is)) {
            final CompoundTag element = NbtIo.readCompressed(dis, NbtAccounter.unlimitedHeap());
            if (this.decoder != null) {
                // TODO this is actually blocking
                return this.decoder.decode(pack, key, element, SpongeCommon.vanillaRegistryAccess());
            }
        }
        return null;
    }

    public static void writeFile(final Path file, final CompoundTag object) throws IOException {
        Files.deleteIfExists(file);
        NbtIo.writeCompressed(object, file);
    }

}
