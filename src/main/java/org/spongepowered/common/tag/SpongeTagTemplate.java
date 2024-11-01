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
package org.spongepowered.common.tag;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagFile;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.datapack.DataPack;
import org.spongepowered.api.tag.TagTemplate;
import org.spongepowered.api.tag.Taggable;
import org.spongepowered.common.SpongeCommon;

import java.io.IOException;
import java.util.Map;

public record SpongeTagTemplate<T extends Taggable<T>>(
        ResourceKey key, boolean replace, Map<ResourceKey, Boolean> elements,
        Map<ResourceKey, Boolean> subTags,
        DataPack<TagTemplate<T>> pack) implements TagTemplate<T> {

    @Override
    public int contentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        final JsonObject jsonObject = SpongeTagTemplate.encode(this, SpongeCommon.vanillaRegistryAccess());
        try {
            final DataContainer container = DataFormats.JSON.get().read(jsonObject.getAsString());
            container.set(DataQuery.of("replace"), this.replace);
            return container;
        } catch (IOException e) {
            SpongeCommon.logger().error("Error reading json serialized by minecraft", e);
            return DataContainer.createNew();
        }
    }

    public static <T extends Taggable<T>> JsonObject encode(TagTemplate<T> template, RegistryAccess registryAccess) {
        final TagBuilder builder = TagBuilder.create();
        final SpongeTagTemplate<T> spongeTemplate = (SpongeTagTemplate<T>) template;
        spongeTemplate.elements.forEach((k, v) -> {
            final ResourceLocation location = (ResourceLocation) (Object) k;
            // "N/A" is supposed to be the source, but we don't know it, and we're serializing it so it isn't used anyway. (Gone when we serializeToJson)
            if (v) {
                builder.addElement(location);
            } else {
                builder.addOptionalElement(location);
            }
        });
        spongeTemplate.subTags.forEach((k, v) -> {
            final ResourceLocation location = (ResourceLocation) (Object) k;
            if (v) {
                builder.addTag(location);
            } else {
                builder.addOptionalTag(location);
            }
        });
        return TagFile.CODEC.encodeStart(JsonOps.INSTANCE, new TagFile(builder.build(), spongeTemplate.replace)).getOrThrow().getAsJsonObject();
    }

}
