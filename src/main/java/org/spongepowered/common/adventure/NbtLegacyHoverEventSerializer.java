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
package org.spongepowered.common.adventure;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.gson.LegacyHoverEventSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.util.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.util.UUID;

@SuppressWarnings({"PatternValidation","deprecation"})
public final class NbtLegacyHoverEventSerializer implements LegacyHoverEventSerializer {
    public static final NbtLegacyHoverEventSerializer INSTANCE = new NbtLegacyHoverEventSerializer();
    private static final Codec<CompoundTag, String, CommandSyntaxException, RuntimeException> SNBT_CODEC = Codec.of(TagParser::parseTag, Tag::toString);

    static final String ITEM_TYPE = "id";
    static final String ITEM_COUNT = "Count";
    static final String ITEM_TAG = "tag";

    static final String ENTITY_NAME = "name";
    static final String ENTITY_TYPE = "type";
    static final String ENTITY_ID = "id";

    private NbtLegacyHoverEventSerializer() {
    }

    @Override
    public HoverEvent.ShowItem deserializeShowItem(final Component input) throws IOException {
        final String rawContent = PlainTextComponentSerializer.plainText().serialize(input);
        try {
            final CompoundTag contents = NbtLegacyHoverEventSerializer.SNBT_CODEC.decode(rawContent);
            final CompoundTag tag = contents.getCompound(NbtLegacyHoverEventSerializer.ITEM_TAG);
            return HoverEvent.ShowItem.of(
                Key.key(contents.getString(NbtLegacyHoverEventSerializer.ITEM_TYPE)),
                contents.contains(NbtLegacyHoverEventSerializer.ITEM_COUNT) ? contents.getByte(NbtLegacyHoverEventSerializer.ITEM_COUNT) : 1,
                tag.isEmpty() ? null : BinaryTagHolder.encode(tag, NbtLegacyHoverEventSerializer.SNBT_CODEC)
            );
        } catch (final CommandSyntaxException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public HoverEvent.@NonNull ShowEntity deserializeShowEntity(final Component input, final Codec.Decoder<Component, String, ? extends RuntimeException> componentCodec) throws IOException {
        final String raw = PlainTextComponentSerializer.plainText().serialize(input);
        try {
            final CompoundTag contents = NbtLegacyHoverEventSerializer.SNBT_CODEC.decode(raw);
            return HoverEvent.ShowEntity.of(
                Key.key(contents.getString(NbtLegacyHoverEventSerializer.ENTITY_TYPE)),
                UUID.fromString(contents.getString(NbtLegacyHoverEventSerializer.ENTITY_ID)),
                componentCodec.decode(contents.getString(NbtLegacyHoverEventSerializer.ENTITY_NAME))
            );
        } catch (final CommandSyntaxException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public @NonNull Component serializeShowItem(final HoverEvent.@NonNull ShowItem input) throws IOException {
        final CompoundTag tag = new CompoundTag();
        tag.putString(NbtLegacyHoverEventSerializer.ITEM_TYPE, input.item().asString());
        tag.putByte(NbtLegacyHoverEventSerializer.ITEM_COUNT, (byte) input.count());
        if (input.nbt() != null) {
            try {
                tag.put(NbtLegacyHoverEventSerializer.ITEM_TAG, input.nbt().get(NbtLegacyHoverEventSerializer.SNBT_CODEC));
            } catch (final CommandSyntaxException ex) {
                throw new IOException(ex);
            }
        }

        return Component.text(NbtLegacyHoverEventSerializer.SNBT_CODEC.encode(tag));
    }

    @Override
    public @NonNull Component serializeShowEntity(final HoverEvent.ShowEntity input, final Codec.Encoder<Component, String, ? extends RuntimeException> componentCodec) {
        final CompoundTag tag = new CompoundTag();
        tag.putString(NbtLegacyHoverEventSerializer.ENTITY_ID, input.id().toString());
        tag.putString(NbtLegacyHoverEventSerializer.ENTITY_TYPE, input.type().asString());
        if (input.name() != null) {
            tag.putString(NbtLegacyHoverEventSerializer.ENTITY_NAME, componentCodec.encode(input.name()));
        }
        return Component.text(NbtLegacyHoverEventSerializer.SNBT_CODEC.encode(tag));
    }
}
