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

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import io.netty.util.AttributeKey;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.BlockNBTComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.EntityNBTComponent;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.NBTComponentBuilder;
import net.kyori.adventure.text.ScoreComponent;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.StorageNBTComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.VirtualComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.util.Codec;
import net.kyori.adventure.util.TriState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.selector.SelectorPattern;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.HoverEvent.Action;
import net.minecraft.network.chat.HoverEvent.ItemStackInfo;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.BlockDataSource;
import net.minecraft.network.chat.contents.EntityDataSource;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.StorageDataSource;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.adventure.ResolveOperation;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.registry.DefaultedRegistryReference;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.network.chat.HoverEvent_ItemStackInfoAccessor;
import org.spongepowered.common.accessor.network.chat.StyleAccessor;
import org.spongepowered.common.bridge.adventure.BossBarBridge;
import org.spongepowered.common.bridge.adventure.ComponentBridge;
import org.spongepowered.common.bridge.adventure.StyleBridge;
import org.spongepowered.common.bridge.world.BossEventBridge;
import org.spongepowered.common.launch.Launch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class SpongeAdventure {

    public static final AttributeKey<Locale> CHANNEL_LOCALE = AttributeKey.newInstance("sponge:locale");
    public static final Codec<CompoundTag, String, IOException, IOException> NBT_CODEC = new Codec<CompoundTag, String, IOException, IOException>() {
        @Override
        public @NonNull CompoundTag decode(final @NonNull String encoded) throws IOException {
            try {
                return TagParser.parseTag(encoded);
            } catch (final CommandSyntaxException e) {
                throw new IOException(e);
            }
        }

        @Override
        public @NonNull String encode(final @NonNull CompoundTag decoded) {
            return decoded.toString();
        }
    };

    public static final PrimitiveCodec<Component> STRING_CODEC = new PrimitiveCodec<Component>() {
        @Override
        public <T> DataResult<Component> read(final DynamicOps<T> ops, final T input) {
            return ops.getStringValue(input).map(GsonComponentSerializer.gson()::deserialize);
        }

        @Override
        public <T> T write(final DynamicOps<T> ops, final Component value) {
            return ops.createString(GsonComponentSerializer.gson().serialize(value));
        }

        @Override
        public String toString() {
            return "String[Component]";
        }
    };

    public static final ConfigurateComponentSerializer CONFIGURATE = ConfigurateComponentSerializer.builder()
            .scalarSerializer(GsonComponentSerializer.gson())
            .build();

    private static final Set<ServerBossEvent> ACTIVE_BOSS_BARS = ConcurrentHashMap.newKeySet();

    public static final ThreadLocal<Locale> ENCODING_LOCALE = new ThreadLocal<>();

    // --------------
    // ---- Core ----
    // --------------

    public static TriState asAdventure(final Tristate state) {
        if (state == Tristate.UNDEFINED) {
            return TriState.NOT_SET;
        } else if (state == Tristate.FALSE) {
            return TriState.FALSE;
        } else if (state == Tristate.TRUE) {
            return TriState.TRUE;
        }
        throw new IllegalArgumentException(state.name());
    }

    // -------------
    // ---- Key ----
    // -------------

    // org.spongepowered.common.mixin.core.adventure.KeyMixin
    public static Key asAdventure(final ResourceLocation key) {
        return (Key) (Object) key;
    }

    // ------------------------
    // ---- ChatType.Bound ----
    // ------------------------

    public static ChatType.Bound asVanilla(final RegistryAccess access, final net.kyori.adventure.chat.ChatType.Bound adv) {
        if ((Object) adv instanceof ChatType.Bound vanilla) {
            return vanilla;
        }

        ChatType.Bound vanilla = ChatType.bind(
            ResourceKey.create(Registries.CHAT_TYPE, SpongeAdventure.asVanilla(adv.type().key())),
            access,
            SpongeAdventure.asVanilla(adv.name())
        );

        if (adv.target() != null) {
            vanilla = vanilla.withTargetName(SpongeAdventure.asVanilla(adv.target()));
        }

        return vanilla;
    }

    // -------------------
    // ---- Component ----
    // -------------------

    public static net.minecraft.network.chat.@Nullable Component asVanillaNullable(final @Nullable Component component) {
        if (component == null) {
            return null;
        }
        return SpongeAdventure.asVanilla(component);
    }

    public static net.minecraft.network.chat.Component asVanilla(final Component component) {
        return new AdventureTextComponent(component, GlobalTranslator.renderer());
    }

    public static Optional<net.minecraft.network.chat.Component> asVanillaOpt(final @Nullable Component component) {
        return component == null ? Optional.empty() : Optional.of(((ComponentBridge) component).bridge$asVanillaComponent());
    }

    // no caching
    public static MutableComponent asVanillaMutable(final Component component) {
        final MutableComponent vanilla = SpongeAdventure.asVanillaMutable0(component);
        for (final Component child : component.children()) {
            vanilla.append(((ComponentBridge) child).bridge$asVanillaComponent());
        }
        vanilla.setStyle(((StyleBridge) component.style()).bridge$asVanilla());
        return vanilla;
    }

    private static MutableComponent asVanillaMutable0(final Component component) {
        return switch (component) {
            case VirtualComponent virtual -> net.minecraft.network.chat.Component.literal(virtual.content());
            case TextComponent text -> net.minecraft.network.chat.Component.literal(text.content());
            case TranslatableComponent translatable -> {
                final List<net.minecraft.network.chat.Component> with = new ArrayList<>(translatable.args().size());
                for (final Component arg : translatable.args()) {
                    with.add(((ComponentBridge) arg).bridge$asVanillaComponent());
                }
                yield net.minecraft.network.chat.Component.translatable(translatable.key(), with.toArray(new Object[0]));
            }
            case KeybindComponent keybind -> net.minecraft.network.chat.Component.keybind(keybind.keybind());
            case ScoreComponent score -> net.minecraft.network.chat.Component.score(score.name(), score.objective());
            case SelectorComponent selector -> net.minecraft.network.chat.Component.selector(
                SelectorPattern.parse(selector.pattern()).getOrThrow(), SpongeAdventure.asVanillaOpt(selector.separator())
            );
            case BlockNBTComponent block -> net.minecraft.network.chat.Component.nbt(block.nbtPath(), block.interpret(),
                SpongeAdventure.asVanillaOpt(block.separator()),
                new BlockDataSource(block.pos().asString()));
            case EntityNBTComponent entity -> net.minecraft.network.chat.Component.nbt(entity.nbtPath(), entity.interpret(),
                SpongeAdventure.asVanillaOpt(entity.separator()),
                new EntityDataSource(entity.selector()));
            case StorageNBTComponent storage -> net.minecraft.network.chat.Component.nbt(storage.nbtPath(), storage.interpret(),
                SpongeAdventure.asVanillaOpt(storage.separator()),
                new StorageDataSource(SpongeAdventure.asVanilla(storage.storage())));
            default -> throw new UnsupportedOperationException("Cannot convert Component of type " + component.getClass());
        };
    }

    // no caching
    public static Component asAdventure(final net.minecraft.network.chat.Component component) {
        if (component instanceof final AdventureTextComponent ac) {
            return ac.wrapped();
        }

        final ComponentBuilder<?, ?> builder = SpongeAdventure.asAdventureBuilder(component.getContents());

        for (final net.minecraft.network.chat.Component child : component.getSiblings()) {
            builder.append(SpongeAdventure.asAdventure(child));
        }

        builder.style(((org.spongepowered.common.bridge.network.chat.StyleBridge) component.getStyle()).bridge$asAdventure());

        return builder.build();
    }

    private static ComponentBuilder<?, ?> asAdventureBuilder(final ComponentContents contents) {
        if (contents instanceof final PlainTextContents lc) {
            if (contents == PlainTextContents.EMPTY) {
                return Component.empty().toBuilder();
            }
            return Component.text().content(lc.text());
        }
        if (contents instanceof final TranslatableContents tc) {
            final List<Component> argList = Arrays.stream(tc.getArgs())
                    .map(arg -> arg instanceof final net.minecraft.network.chat.Component argComponent ?
                                    SpongeAdventure.asAdventure(argComponent) : Component.text(arg.toString())).toList();
            return Component.translatable().key(tc.getKey()).args(argList);
        }
        if (contents instanceof final KeybindContents kc) {
            return Component.keybind().keybind(kc.getName());
        }
        if (contents instanceof final ScoreContents sc) {
            return Component.score().name(sc.name().mapLeft(SelectorPattern::pattern).orThrow()).objective(sc.objective());
        }
        if (contents instanceof final SelectorContents sc) {
            return Component.selector().pattern(sc.selector().pattern())
                                              .separator(SpongeAdventure.asAdventure(sc.separator()));
        }
        if (contents instanceof final NbtContents nc) {
            NBTComponentBuilder<?, ?> nbtBuilder;
            if (nc.getDataSource() instanceof final BlockDataSource ds) {
                nbtBuilder = Component.blockNBT().pos(BlockNBTComponent.Pos.fromString(ds.posPattern()));
            } else if (nc.getDataSource() instanceof final EntityDataSource ds) {
                nbtBuilder = Component.entityNBT().selector(ds.selectorPattern());
            } else if (nc.getDataSource() instanceof final StorageDataSource ds) {
                nbtBuilder = Component.storageNBT().storage(SpongeAdventure.asAdventure(ds.id()));
            } else {
                throw new UnsupportedOperationException("Cannot convert NBTContents with DataSource " + nc.getDataSource().getClass());
            }
            return nbtBuilder.nbtPath(nc.getNbtPath())
                                    .interpret(nc.isInterpreting())
                                    .separator(SpongeAdventure.asAdventure(nc.getSeparator()));
        }

        throw new UnsupportedOperationException("Cannot convert ComponentContents of type " + contents.getClass());
    }

    public static @Nullable Component asAdventure(final Optional<net.minecraft.network.chat.Component> component) {
        return component.map(SpongeAdventure::asAdventure).orElse(null);
    }

    // no caching
    public static Style asAdventure(final net.minecraft.network.chat.Style mcStyle) {
        final net.kyori.adventure.text.format.Style.Builder builder = net.kyori.adventure.text.format.Style.style();
        final StyleAccessor $access = (StyleAccessor) mcStyle;

        builder.font(SpongeAdventure.asAdventure($access.accessor$font())); // font
        builder.color(SpongeAdventure.asAdventure(mcStyle.getColor())); // color
        // decorations
        builder.decoration(TextDecoration.OBFUSCATED, TextDecoration.State.byBoolean($access.accessor$obfuscated()));
        builder.decoration(TextDecoration.BOLD, TextDecoration.State.byBoolean($access.accessor$bold()));
        builder.decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.byBoolean($access.accessor$strikethrough()));
        builder.decoration(TextDecoration.UNDERLINED, TextDecoration.State.byBoolean($access.accessor$underlined()));
        builder.decoration(TextDecoration.ITALIC, TextDecoration.State.byBoolean($access.accessor$italic()));
        // events
        final net.minecraft.network.chat.HoverEvent hoverEvent = mcStyle.getHoverEvent();
        if (hoverEvent != null) {
            builder.hoverEvent(SpongeAdventure.asAdventure(hoverEvent));
        }
        final net.minecraft.network.chat.ClickEvent clickEvent = mcStyle.getClickEvent();
        if (clickEvent != null) {
            builder.clickEvent(ClickEvent.clickEvent(SpongeAdventure.asAdventure(clickEvent.getAction()), clickEvent.getValue()));
        }
        builder.insertion(mcStyle.getInsertion()); // insertion
        return builder.build();
    }

    public static Component asAdventure(final Message message) {
        if (message instanceof net.minecraft.network.chat.Component) {
            return SpongeAdventure.asAdventure((net.minecraft.network.chat.Component) message);
        } else if (message instanceof Component) {
            return (Component) message;
        }
        return Component.text(message.getString());
    }

    public static net.minecraft.network.chat.Style asVanilla(final Style style) {
        return ((StyleBridge) (Object) style).bridge$asVanilla();
    }

    public static net.minecraft.network.chat.@Nullable TextColor asVanillaNullable(final @Nullable TextColor color) {
        if (color == null) {
            return null;
        }
        return net.minecraft.network.chat.TextColor.fromRgb(color.value());
    }

    public static ChatFormatting asVanilla(final NamedTextColor color) {
        if (color == NamedTextColor.BLACK) {
            return ChatFormatting.BLACK;
        } else if (color == NamedTextColor.DARK_BLUE) {
            return ChatFormatting.DARK_BLUE;
        } else if (color == NamedTextColor.DARK_GREEN) {
            return ChatFormatting.DARK_GREEN;
        } else if (color == NamedTextColor.DARK_AQUA) {
            return ChatFormatting.DARK_AQUA;
        } else if (color == NamedTextColor.DARK_RED) {
            return ChatFormatting.DARK_RED;
        } else if (color == NamedTextColor.DARK_PURPLE) {
            return ChatFormatting.DARK_PURPLE;
        } else if (color == NamedTextColor.GOLD) {
            return ChatFormatting.GOLD;
        } else if (color == NamedTextColor.GRAY) {
            return ChatFormatting.GRAY;
        } else if (color == NamedTextColor.DARK_GRAY) {
            return ChatFormatting.DARK_GRAY;
        } else if (color == NamedTextColor.BLUE) {
            return ChatFormatting.BLUE;
        } else if (color == NamedTextColor.GREEN) {
            return ChatFormatting.GREEN;
        } else if (color == NamedTextColor.AQUA) {
            return ChatFormatting.AQUA;
        } else if (color == NamedTextColor.RED) {
            return ChatFormatting.RED;
        } else if (color == NamedTextColor.LIGHT_PURPLE) {
            return ChatFormatting.LIGHT_PURPLE;
        } else if (color == NamedTextColor.YELLOW) {
            return ChatFormatting.YELLOW;
        } else if (color == NamedTextColor.WHITE) {
            return ChatFormatting.WHITE;
        }
        throw new IllegalArgumentException();
    }

    public static @Nullable TextColor asAdventure(final net.minecraft.network.chat.@Nullable TextColor color) {
        if (color == null) {
            return null;
        }
        return TextColor.color(color.getValue());
    }

    public static @Nullable TextColor asAdventure(final ChatFormatting formatting) {
        if (formatting == null) {
            return null;
        }
        final Integer color = formatting.getColor();
        if (color == null) {
            return null;
        }
        return TextColor.color(color);
    }

    public static @Nullable NamedTextColor asAdventureNamed(final @Nullable ChatFormatting color) {
        if (color == null) {
            return null;
        }
        if (color == ChatFormatting.BLACK) {
            return NamedTextColor.BLACK;
        } else if (color == ChatFormatting.DARK_BLUE) {
            return NamedTextColor.DARK_BLUE;
        } else if (color == ChatFormatting.DARK_GREEN) {
            return NamedTextColor.DARK_GREEN;
        } else if (color == ChatFormatting.DARK_AQUA) {
            return NamedTextColor.DARK_AQUA;
        } else if (color == ChatFormatting.DARK_RED) {
            return NamedTextColor.DARK_RED;
        } else if (color == ChatFormatting.DARK_PURPLE) {
            return NamedTextColor.DARK_PURPLE;
        } else if (color == ChatFormatting.GOLD) {
            return NamedTextColor.GOLD;
        } else if (color == ChatFormatting.GRAY) {
            return NamedTextColor.GRAY;
        } else if (color == ChatFormatting.DARK_GRAY) {
            return NamedTextColor.DARK_GRAY;
        } else if (color == ChatFormatting.BLUE) {
            return NamedTextColor.BLUE;
        } else if (color == ChatFormatting.GREEN) {
            return NamedTextColor.GREEN;
        } else if (color == ChatFormatting.AQUA) {
            return NamedTextColor.AQUA;
        } else if (color == ChatFormatting.RED) {
            return NamedTextColor.RED;
        } else if (color == ChatFormatting.LIGHT_PURPLE) {
            return NamedTextColor.LIGHT_PURPLE;
        } else if (color == ChatFormatting.YELLOW) {
            return NamedTextColor.YELLOW;
        }
        return NamedTextColor.WHITE;
    }

    public static @Nullable Boolean asVanillaNullable(final TextDecoration.State state) {
        if (state == TextDecoration.State.TRUE) {
            return true;
        } else if (state == TextDecoration.State.FALSE) {
            return false;
        }
        return null;
    }

    @SuppressWarnings("ConstantConditions")
    public static HoverEvent<?> asAdventure(final net.minecraft.network.chat.HoverEvent event) {
        final Action<?> action = event.getAction();
        if (action == Action.SHOW_TEXT) {
            return HoverEvent.showText(SpongeAdventure.asAdventure(event.getValue(Action.SHOW_TEXT)));
        } else if (action == Action.SHOW_ENTITY) {
            final net.minecraft.network.chat.HoverEvent.EntityTooltipInfo value = event.getValue(
                Action.SHOW_ENTITY);
            final Registry<EntityType<?>> entityTypeRegistry = SpongeCommon.vanillaRegistry(Registries.ENTITY_TYPE);
            return HoverEvent.showEntity(
                SpongeAdventure.asAdventure(entityTypeRegistry.getKey(value.type)),
                value.id,
                SpongeAdventure.asAdventure(value.name)
            );
        } else if (action == Action.SHOW_ITEM) {
            final ItemStackInfo value = event.getValue(Action.SHOW_ITEM);
            final Registry<Item> itemRegistry = SpongeCommon.vanillaRegistry(Registries.ITEM);
            final ItemStack itemStack = value.getItemStack();
            return HoverEvent.showItem(
                SpongeAdventure.asAdventure(itemRegistry.getKey(itemStack.getItem())),
                itemStack.getCount(),
                SpongeAdventure.asAdventure(itemStack.getComponentsPatch())
            );
        }
        throw new IllegalArgumentException(event.toString());
    }

    public static net.minecraft.network.chat.@Nullable ClickEvent asVanillaNullable(final @Nullable ClickEvent event) {
        if (event == null) {
            return null;
        }
        return new net.minecraft.network.chat.ClickEvent(SpongeAdventure.asVanilla(event.action()), event.value());
    }

    public static net.minecraft.network.chat.@Nullable HoverEvent asVanillaNullable(final @Nullable HoverEvent<?> event) {
        if (event == null) {
            return null;
        }
        final HoverEvent.Action<?> action = event.action();
        if (action == HoverEvent.Action.SHOW_TEXT) {
            return new net.minecraft.network.chat.HoverEvent(
                Action.SHOW_TEXT,
                SpongeAdventure.asVanilla((Component) event.value())
            );
        } else if (action == HoverEvent.Action.SHOW_ENTITY) {
            final HoverEvent.ShowEntity value = (HoverEvent.ShowEntity) event.value();
            final Registry<EntityType<?>> entityTypeRegistry = SpongeCommon.vanillaRegistry(Registries.ENTITY_TYPE);
            return new net.minecraft.network.chat.HoverEvent(
                Action.SHOW_ENTITY,
                new net.minecraft.network.chat.HoverEvent.EntityTooltipInfo(
                    entityTypeRegistry.getValue(SpongeAdventure.asVanilla(value.type())),
                    value.id(),
                    SpongeAdventure.asVanillaNullable(value.name())
                )
            );
        } else if (action == HoverEvent.Action.SHOW_ITEM) {
            final HoverEvent.ShowItem value = (HoverEvent.ShowItem) event.value();
            final Registry<Item> itemRegistry = SpongeCommon.vanillaRegistry(Registries.ITEM);
            return new net.minecraft.network.chat.HoverEvent(
                Action.SHOW_ITEM,
                HoverEvent_ItemStackInfoAccessor.invoker$new(
                    Holder.direct(itemRegistry.getValue(SpongeAdventure.asVanilla(value.item()))),
                    value.count(),
                    SpongeAdventure.asVanilla(value.dataComponents())
                )
            );
        }
        throw new IllegalArgumentException(event.toString());
    }

    public static Action<?> asVanilla(final HoverEvent.Action<?> action) {
        if (action == HoverEvent.Action.SHOW_TEXT) {
            return Action.SHOW_TEXT;
        } else if (action == HoverEvent.Action.SHOW_ITEM) {
            return Action.SHOW_ITEM;
        } else if (action == HoverEvent.Action.SHOW_ENTITY) {
            return Action.SHOW_ENTITY;
        }
        throw new IllegalArgumentException(action.toString());
    }

    public static ClickEvent.Action asAdventure(final net.minecraft.network.chat.ClickEvent.Action action) {
        if (action == net.minecraft.network.chat.ClickEvent.Action.OPEN_URL) {
            return ClickEvent.Action.OPEN_URL;
        } else if (action == net.minecraft.network.chat.ClickEvent.Action.OPEN_FILE) {
            return ClickEvent.Action.OPEN_FILE;
        } else if (action == net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND) {
            return ClickEvent.Action.RUN_COMMAND;
        } else if (action == net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND) {
            return ClickEvent.Action.SUGGEST_COMMAND;
        } else if (action == net.minecraft.network.chat.ClickEvent.Action.CHANGE_PAGE) {
            return ClickEvent.Action.CHANGE_PAGE;
        }
        throw new IllegalArgumentException(action.toString());
    }

    public static net.minecraft.network.chat.ClickEvent.Action asVanilla(final ClickEvent.Action action) {
        if (action == ClickEvent.Action.OPEN_URL) {
            return net.minecraft.network.chat.ClickEvent.Action.OPEN_URL;
        } else if (action == ClickEvent.Action.OPEN_FILE) {
            return net.minecraft.network.chat.ClickEvent.Action.OPEN_FILE;
        } else if (action == ClickEvent.Action.RUN_COMMAND) {
            return net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND;
        } else if (action == ClickEvent.Action.SUGGEST_COMMAND) {
            return net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND;
        } else if (action == ClickEvent.Action.CHANGE_PAGE) {
            return net.minecraft.network.chat.ClickEvent.Action.CHANGE_PAGE;
        } else if (action == ClickEvent.Action.COPY_TO_CLIPBOARD) {
            return net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD;
        }
        throw new IllegalArgumentException(action.toString());
    }

    // Horrible-ness

    public static List<Component> json(final List<String> strings) {
        final GsonComponentSerializer gcs = GsonComponentSerializer.gson();
        final List<Component> components = new ArrayList<>();
        for (final String string : strings) {
            components.add(gcs.deserialize(string));
        }
        return components;
    }

    public static ListTag listTagJson(final List<Component> components) {
        final GsonComponentSerializer gcs = GsonComponentSerializer.gson();
        final ListTag nbt = new ListTag();
        for (final Component component : components) {
            nbt.add(StringTag.valueOf(gcs.serialize(component)));
        }
        return nbt;
    }

    // -----------------
    // ---- BossBar ----
    // -----------------

    public static BossBar asAdventure(final BossEvent bar) {
        return ((BossEventBridge) bar).bridge$asAdventure();
    }

    public static ServerBossEvent asVanillaServer(final BossBar bar) {
        return ((BossBarBridge) bar).bridge$asVanillaServerBar();
    }

    public static BossEvent.BossBarColor asVanilla(final BossBar.Color color) {
        if(color == BossBar.Color.PINK) {
            return BossEvent.BossBarColor.PINK;
        } else if(color == BossBar.Color.BLUE) {
            return BossEvent.BossBarColor.BLUE;
        } else if(color == BossBar.Color.RED) {
            return BossEvent.BossBarColor.RED;
        } else if(color == BossBar.Color.GREEN) {
            return BossEvent.BossBarColor.GREEN;
        } else if(color == BossBar.Color.YELLOW) {
            return BossEvent.BossBarColor.YELLOW;
        } else if(color == BossBar.Color.PURPLE) {
            return BossEvent.BossBarColor.PURPLE;
        } else if(color == BossBar.Color.WHITE) {
            return BossEvent.BossBarColor.WHITE;
        }
        throw new IllegalArgumentException(color.name());
    }

    public static BossBar.Color asAdventure(final BossEvent.BossBarColor color) {
        if(color == BossEvent.BossBarColor.PINK) {
            return BossBar.Color.PINK;
        } else if(color == BossEvent.BossBarColor.BLUE) {
            return BossBar.Color.BLUE;
        } else if(color == BossEvent.BossBarColor.RED) {
            return BossBar.Color.RED;
        } else if(color == BossEvent.BossBarColor.GREEN) {
            return BossBar.Color.GREEN;
        } else if(color == BossEvent.BossBarColor.YELLOW) {
            return BossBar.Color.YELLOW;
        } else if(color == BossEvent.BossBarColor.PURPLE) {
            return BossBar.Color.PURPLE;
        } else if(color == BossEvent.BossBarColor.WHITE) {
            return BossBar.Color.WHITE;
        }
        throw new IllegalArgumentException(color.name());
    }

    public static BossEvent.BossBarOverlay asVanilla(final BossBar.Overlay overlay) {
        if (overlay == BossBar.Overlay.PROGRESS) {
            return BossEvent.BossBarOverlay.PROGRESS;
        } else if (overlay == BossBar.Overlay.NOTCHED_6) {
            return BossEvent.BossBarOverlay.NOTCHED_6;
        } else if (overlay == BossBar.Overlay.NOTCHED_10) {
            return BossEvent.BossBarOverlay.NOTCHED_10;
        } else if (overlay == BossBar.Overlay.NOTCHED_12) {
            return BossEvent.BossBarOverlay.NOTCHED_12;
        } else if (overlay == BossBar.Overlay.NOTCHED_20) {
            return BossEvent.BossBarOverlay.NOTCHED_20;
        }
        throw new IllegalArgumentException(overlay.name());
    }

    public static BossBar.Overlay asAdventure(final BossEvent.BossBarOverlay overlay) {
        if (overlay == BossEvent.BossBarOverlay.PROGRESS) {
            return BossBar.Overlay.PROGRESS;
        } else if (overlay == BossEvent.BossBarOverlay.NOTCHED_6) {
            return BossBar.Overlay.NOTCHED_6;
        } else if (overlay == BossEvent.BossBarOverlay.NOTCHED_10) {
            return BossBar.Overlay.NOTCHED_10;
        } else if (overlay == BossEvent.BossBarOverlay.NOTCHED_12) {
            return BossBar.Overlay.NOTCHED_12;
        } else if (overlay == BossEvent.BossBarOverlay.NOTCHED_20) {
            return BossBar.Overlay.NOTCHED_20;
        }
        throw new IllegalArgumentException(overlay.name());
    }

    public static Set<BossBar.Flag> asAdventureFlags(final boolean darkenScreen, final boolean playBossMusic, final boolean createWorldFog) {
        final Set<BossBar.Flag> flags = EnumSet.noneOf(BossBar.Flag.class);
        if (darkenScreen) {
            flags.add(BossBar.Flag.DARKEN_SCREEN);
        }
        if (playBossMusic) {
            flags.add(BossBar.Flag.PLAY_BOSS_MUSIC);
        }
        if (createWorldFog) {
            flags.add(BossBar.Flag.CREATE_WORLD_FOG);
        }
        return flags;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static DataComponentPatch asVanilla(final Map<Key, DataComponentValue> componentMap) {
        if (componentMap == null) {
            return DataComponentPatch.EMPTY;
        }
        final DataComponentPatch.Builder builder = DataComponentPatch.builder();
        componentMap.forEach((key, value) -> BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(SpongeAdventure.asVanilla(key)).ifPresent(type -> {
            if (value instanceof SpongeDataComponentValue dcv) {
                builder.set((DataComponentType) type, dcv.value.orElse(null));
            }
        }));
        return builder.build();
    }

    public static Map<Key, DataComponentValue> asAdventure(final DataComponentPatch components) {
        if (components == null) {
            return Map.of();
        }

        Map<Key, DataComponentValue> map = new HashMap<>();
        components.entrySet().forEach(entry -> {
            final ResourceLocation key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
            map.put(SpongeAdventure.asAdventure(key), new SpongeDataComponentValue<>(entry.getValue()));
        });
        return map;
    }

    public static @Nullable Integer asVanillaNullable(@Nullable ShadowColor shadowColor) {
        if (shadowColor == null) {
            return null;
        }

        return shadowColor.value();
    }

    private record SpongeDataComponentValue<T>(Optional<T> value) implements DataComponentValue {

    }

    // Key

    public static ResourceLocation asVanilla(final Key key) {
        if ((Object) key instanceof ResourceLocation) {
            return (ResourceLocation) (Object) key;
        }
        return ResourceLocation.fromNamespaceAndPath(key.namespace(), key.value());
    }

    public static @Nullable ResourceLocation asVanillaNullable(final @Nullable Key key) {
        if (key == null) {
            return null;
        }
        return SpongeAdventure.asVanilla(key);
    }

    // Sound

    public static Sound.Source asAdventure(final SoundSource source) {
        switch (source) {
            case MASTER: return Sound.Source.MASTER;
            case MUSIC: return Sound.Source.MUSIC;
            case RECORDS: return Sound.Source.RECORD;
            case WEATHER: return Sound.Source.WEATHER;
            case BLOCKS: return Sound.Source.BLOCK;
            case HOSTILE: return Sound.Source.HOSTILE;
            case NEUTRAL: return Sound.Source.NEUTRAL;
            case PLAYERS: return Sound.Source.PLAYER;
            case AMBIENT: return Sound.Source.AMBIENT;
            case VOICE: return Sound.Source.VOICE;
        }
        throw new IllegalArgumentException(source.name());
    }

    public static SoundSource asVanilla(final Sound.Source source) {
        switch (source) {
            case MASTER: return SoundSource.MASTER;
            case MUSIC: return SoundSource.MUSIC;
            case RECORD: return SoundSource.RECORDS;
            case WEATHER: return SoundSource.WEATHER;
            case BLOCK: return SoundSource.BLOCKS;
            case HOSTILE: return SoundSource.HOSTILE;
            case NEUTRAL: return SoundSource.NEUTRAL;
            case PLAYER: return SoundSource.PLAYERS;
            case AMBIENT: return SoundSource.AMBIENT;
            case VOICE: return SoundSource.VOICE;
        }
        throw new IllegalArgumentException(source.name());
    }

    public static @Nullable SoundSource asVanillaNullable(final Sound.@Nullable Source source) {
        if (source == null) {
            return null;
        }
        return SpongeAdventure.asVanilla(source);
    }

    public static Iterable<? extends Audience> unpackAudiences(final Audience audience) {
        if (audience instanceof ForwardingAudience) {
            final List<Audience> list = new ArrayList<>();
            for (final Audience subAudience : ((ForwardingAudience) audience).audiences()) {
                SpongeAdventure.unpackAudiences(subAudience).forEach(list::add);
            }
            return list;
        }
        return Collections.singletonList(audience);
    }

    public static class Factory implements SpongeComponents.Factory {
        @Override
        public @NonNull ClickEvent callbackClickEvent(final @NonNull Consumer<CommandCause> callback) {
            Objects.requireNonNull(callback);
            final UUID key = CallbackCommand.INSTANCE.registerCallback(callback);
            return ClickEvent.runCommand(String.format("/%s:%s %s", Launch.instance().id(), CallbackCommand.NAME, key));
        }

        @Override
        @SafeVarargs
        public final @NonNull Component render(
            final @NonNull Component component,
            final @NonNull CommandCause senderContext,
            @Nullable Audience viewer,
            final @NonNull DefaultedRegistryReference<ResolveOperation> firstOperation,
            final @NonNull DefaultedRegistryReference<ResolveOperation>@NonNull... otherOperations
        ) {
            Component output = Objects.requireNonNull(component, "component");
            Objects.requireNonNull(senderContext, "senderContext");

            // Unwrap the Audience to an entity
            while (viewer instanceof ForwardingAudience.Single && !(viewer instanceof Entity)) {
                viewer = ((ForwardingAudience.Single) viewer).audience();
            }
            final Entity backing;
            if (viewer instanceof Entity) {
                backing = (Entity) viewer;
            } else {
                backing = null;
            }

            output = ((SpongeResolveOperation) Objects.requireNonNull(firstOperation, "firstOperation").get())
                .resolve(output, senderContext, backing);

            for (final DefaultedRegistryReference<ResolveOperation> ref : otherOperations) {
                output = ((SpongeResolveOperation) ref.get()).resolve(output, senderContext, backing);
            }
            return output;
        }

        @Override
        @SafeVarargs
        public final @NonNull Component render(
            final @NonNull Component component,
            final @NonNull CommandCause senderContext,
            final @NonNull DefaultedRegistryReference<ResolveOperation> firstOperation,
            final @NonNull DefaultedRegistryReference<ResolveOperation>@NonNull... otherOperations
        ) {
            return this.render(component, senderContext, null, firstOperation, otherOperations);
        }

        @Override
        public ComponentFlattener flattener() {
            return ComponentFlattenerProvider.INSTANCE;
        }
    }

    // Boss bar tracking
    // So we can update viewed bars for players when their locales change

    public static void registerBossBar(final ServerBossEvent mcBar) {
        SpongeAdventure.ACTIVE_BOSS_BARS.add(mcBar);
    }

    public static void unregisterBossBar(final ServerBossEvent mcBar) {
        SpongeAdventure.ACTIVE_BOSS_BARS.remove(mcBar);
    }

    public static void forEachBossBar(final Consumer<ServerBossEvent> info) {
        SpongeAdventure.ACTIVE_BOSS_BARS.forEach(info);
    }
}
