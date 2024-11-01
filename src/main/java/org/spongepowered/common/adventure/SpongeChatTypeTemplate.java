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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.format.Style;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ChatTypeDecoration;
import net.minecraft.resources.RegistryOps;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.adventure.ChatTypeTemplate;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.datapack.DataPack;
import org.spongepowered.api.datapack.DataPacks;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.AbstractDataPackEntryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record SpongeChatTypeTemplate(ResourceKey key, ChatType representedType, DataPack<ChatTypeTemplate> pack) implements ChatTypeTemplate {

    @Override
    public org.spongepowered.api.adventure.ChatType type() {
        return (org.spongepowered.api.adventure.ChatType) (Object) this.representedType;
    }

    @Override
    public int contentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        final JsonElement serialized = SpongeChatTypeTemplate.encode(this, SpongeCommon.vanillaRegistryAccess());
        try {
            return DataFormats.JSON.get().read(serialized.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read deserialized ChatType:\n" + serialized, e);
        }
    }

    public static JsonElement encode(final ChatTypeTemplate template, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return ChatType.DIRECT_CODEC.encodeStart(ops, (ChatType) (Object) template.type()).getOrThrow();
    }

    public static ChatType decode(final JsonElement json, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return ChatType.DIRECT_CODEC.parse(ops, json).getOrThrow();
    }

    public static ChatTypeTemplate decode(final DataPack<ChatTypeTemplate> pack, final ResourceKey key, final JsonElement packEntry, final RegistryAccess registryAccess) {
        final ChatType parsed = SpongeChatTypeTemplate.decode(packEntry, registryAccess);
        return new SpongeChatTypeTemplate(key, parsed, pack);
    }

    public static final class BuilderImpl extends AbstractDataPackEntryBuilder<org.spongepowered.api.adventure.ChatType, ChatTypeTemplate, Builder> implements Builder {

        private String translationKey;
        private net.minecraft.network.chat.Style style;
        private List<ChatTypeDecoration.Parameter> parameters;

        private ChatTypeDecoration narration;

        public BuilderImpl() {
            this.reset();
        }

        @Override
        public Builder fromValue(final org.spongepowered.api.adventure.ChatType value) {
            ChatType chatType = (ChatType) (Object) value;
            this.translationKey = chatType.chat().translationKey();
            this.parameters = chatType.chat().parameters();
            this.style = chatType.chat().style();
            this.narration = chatType.narration();
            return this;
        }

        @Override
        public Builder translationKey(final String translationKey) {
            this.translationKey = translationKey;
            return this;
        }

        @Override
        public Builder style(final Style style) {
            this.style = SpongeAdventure.asVanilla(style);
            return this;
        }

        private void addParameter(final ChatTypeDecoration.Parameter parameter) {
            if (this.parameters.contains(parameter)) {
                // TODO check
                //   throw new IllegalStateException("Parameter already exists");
            }
            this.parameters.add(parameter);
        }

        @Override
        public Builder addSender() {
            if (this.parameters == null) {
                this.parameters = new ArrayList<>();
            }
            this.addParameter(ChatTypeDecoration.Parameter.SENDER);
            return this;
        }

        @Override
        public Builder addContent() {
            if (this.parameters == null) {
                this.parameters = new ArrayList<>();
            }
            this.addParameter(ChatTypeDecoration.Parameter.CONTENT);
            return this;
        }

        @Override
        public Builder addTarget() {
            if (this.parameters == null) {
                this.parameters = new ArrayList<>();
            }
            this.addParameter(ChatTypeDecoration.Parameter.TARGET);
            return this;
        }

        @Override
        public Function<ChatTypeTemplate, org.spongepowered.api.adventure.ChatType> valueExtractor() {
            return ChatTypeTemplate::type;
        }

        @Override
        public Builder reset() {
            this.key = null;
            this.pack = DataPacks.CHAT_TYPE;
            this.translationKey = null;
            this.style = net.minecraft.network.chat.Style.EMPTY;
            this.parameters = null;
            this.narration = ChatTypeDecoration.withSender("chat.type.text.narrate");;
            return this;
        }

        @Override
        public Builder fromDataPack(final DataView pack) throws IOException {
            final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(pack));
            final ChatType chatType = SpongeChatTypeTemplate.decode(json, SpongeCommon.vanillaRegistryAccess());
            this.fromValue((org.spongepowered.api.adventure.ChatType) (Object) chatType);
            return this;
        }

        @Override
        protected ChatTypeTemplate build0() {
            Objects.requireNonNull(this.translationKey, "name");
            Objects.requireNonNull(this.parameters, "parameter");
            Objects.requireNonNull(this.style, "style");
            final ChatType chatType = new ChatType(new ChatTypeDecoration(this.translationKey, this.parameters, this.style), this.narration);
            return new SpongeChatTypeTemplate(this.key, chatType, this.pack);
        }
    }
}
