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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.datapack.DataPackEntry;
import org.spongepowered.common.SpongeCommon;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonDataPackSerializer<T extends DataPackEntry<T>> extends DataPackSerializer<JsonElement, T> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public JsonDataPackSerializer(final DataPackEncoder<JsonElement, T> encoder, final DataPackDecoder<JsonElement, T> decoder) {
        super(encoder, decoder);
    }

    @Override
    public String fileEnding() {
        return ".json";
    }

    @Override
    public void serializeObject(final SpongeDataPack<JsonElement, T> pack, final Path packDir, final T packEntry) throws IOException {
        final JsonElement serialized = this.encoder.encode(packEntry, SpongeCommon.vanillaRegistryAccess());
        final Path file = this.packEntryFile(pack.type(), packEntry.key(), packDir);
        final JsonElement finalJson = this.transformSerialized(file, packEntry, serialized);
        Files.createDirectories(file.getParent());
        JsonDataPackSerializer.writeFile(file, finalJson);
    }

    protected JsonElement transformSerialized(final Path file, final T entry, final JsonElement serialized) throws IOException {
        return serialized;
    }

    @Override
    protected void serializeAdditional(final SpongeDataPack<JsonElement, T> type, Path packDir, T entry) throws IOException {
    }

    @Override
    public T deserialize(final SpongeDataPack<JsonElement, T> pack, final Path file, final ResourceKey key) throws IOException {
        try (final InputStream stream = Files.newInputStream(file)) {
            return this.deserialize(pack, stream, key);
        }
    }

    @Override
    public T deserialize(final SpongeDataPack<JsonElement, T> pack, final InputStream is, final ResourceKey key) throws IOException {
        try (final InputStreamReader reader = new InputStreamReader(is)) {
            final JsonElement element = JsonParser.parseReader(reader);

            if (this.decoder != null) {
                // TODO this is actually blocking
                return this.decoder.decode(pack, key, element, SpongeCommon.vanillaRegistryAccess());
            }
        }
        return null;
    }

    public static void writeFile(final Path file, final JsonElement object) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            JsonDataPackSerializer.GSON.toJson(object, writer);
        }
    }

}
