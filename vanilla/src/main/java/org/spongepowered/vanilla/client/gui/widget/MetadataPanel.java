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
package org.spongepowered.vanilla.client.gui.widget;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.Tesselator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.plugin.metadata.PluginMetadata;
import org.spongepowered.vanilla.client.gui.screen.PluginScreen;
import org.spongepowered.vanilla.util.Bounds;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MetadataPanel extends ScrollPanel implements NarratableEntry {

    private static final Component NO_RESULTS = Component.literal("No data...").withStyle(ChatFormatting.GRAY);

    static final Pattern URL_PATTERN = Pattern.compile(
        //         schema                          ipv4            OR        namespace                 port     path         ends
        //   |-----------------|        |-------------------------|  |-------------------------|    |---------| |--|   |---------------|
        "((?:[a-z0-9]{2,}://)?(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|(?:[-\\w_]+\\.[a-z]{2,}?))(?::[0-9]{1,5})?.*?(?=[!\"\u00A7 \n]|$))",
        Pattern.CASE_INSENSITIVE);
    static final int INDENT_SIZE = 12;
    private final Minecraft minecraft;
    private final PluginScreen screen;
    private final int lineHeight;
    private String activePluginName;
    private final List<Category> categories = new ObjectArrayList<>();
    private final List<Category> resizedCategories = new ObjectArrayList<>();
    private int maxKeyWidth;

    public MetadataPanel(final Minecraft minecraft, final PluginScreen screen, final int width, final int height, final int top, final int left) {
        super(minecraft, width, height, top, left);
        this.minecraft = minecraft;
        this.screen = screen;
        this.lineHeight = this.minecraft.font.lineHeight + 1;
    }

    public void setMetadata(@Nullable final PluginMetadata metadata) {
        this.categories.clear();
        this.maxKeyWidth = 0;

        if (metadata == null) {
            this.resizedCategories.clear();
            this.activePluginName = null;
            return;
        }

        this.activePluginName = metadata.name().orElse(metadata.id());

        // Details
        this.categories.add(new Category("Details")
            .addEntry(new Entry("ID", metadata.id())).addEntry(new Entry("Name", metadata.name().orElse(null)))
            .addEntry(new Entry("Version", metadata.version().toString())).addEntry(new Entry("Entry", metadata.entrypoint()))
            .addEntry(new Entry("Description", metadata.description().orElse(null))));

        // Contributors
        this.categories.add(new Category("Contributors",
            metadata.contributors().stream().map(c -> new Entry(c.name(), c.description().orElse(""))).collect(Collectors.toList())));

        // Dependencies
        this.categories.add(new Category("Dependencies", metadata.dependencies().stream()
            .map(d -> Lists.newArrayList(
                new Entry(d.id(), ""),
                new Entry("Version", d.version().toString(), 1),
                new Entry("Optional", String.valueOf(d.optional()), 1),
                new Entry("Load Order", d.loadOrder().name(), 1)))
            .flatMap(List::stream)
            .collect(Collectors.toList())));

        // Resources
        this.categories.add(
            new Category("Resources")
                .addEntry(new Entry("Homepage", metadata.links().homepage().map(URL::toString).orElse(null)))
                .addEntry(new Entry("Issues", metadata.links().issues().map(URL::toString).orElse(null)))
                .addEntry(new Entry("Source", metadata.links().source().map(URL::toString).orElse(null))));

        // Other
        this.categories.add(new Category("Other",
            metadata.properties().entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue().toString())).collect(Collectors.toList())));

        this.categories.stream().flatMap(c -> c.getEntries().stream()).forEach(e -> {
            final int width = e.key == null ? 0 : this.minecraft.font.width(e.key);
            this.maxKeyWidth = Math.max(this.maxKeyWidth, width + (e.level * MetadataPanel.INDENT_SIZE));
        });

        this.resizeContent();
    }

    private void resizeContent() {
        this.resizedCategories.clear();
        this.resizedCategories.addAll(this.categories);

        for (final Category category : this.resizedCategories) {
            final List<Entry> newEntries = new ArrayList<>();
            for (final Entry entry : category.getEntries()) {
                if (entry.rawValue == null) {
                    continue;
                }

                final int levelOffset = entry.level * MetadataPanel.INDENT_SIZE;
                final int baseX = 4;
                final int keyX = baseX + MetadataPanel.INDENT_SIZE + levelOffset;
                final int separatorX = keyX + this.maxKeyWidth + 4 - levelOffset;
                final int valueX = separatorX + this.minecraft.font.width(":") + 4;
                final int maxWidth = this.width - valueX - 8;
                if (maxWidth >= 0) {
                    final List<String> lines = Arrays.asList(this.efficientWrapper(entry.rawValue, maxWidth).split("\n"));
                    newEntries.add(new Entry(entry.rawKey, lines.get(0), entry.level, entry.rawValue));
                    newEntries.addAll(lines.stream().skip(1).map(l -> new Entry(null, l, entry.level, entry.rawValue)).collect(Collectors.toList()));
                }
            }

            category.setEntries(newEntries);
        }
    }

    private int getTotalLineCount() {
        final int categoryCount = this.resizedCategories.size();
        int entryCount = 0;
        for (final Category category : this.resizedCategories) {
            entryCount += category.getEntries().size();
        }
        return (entryCount + categoryCount + categoryCount);
    }

    private List<Entry> getAllEntries() {
        final List<Entry> entries = new ObjectArrayList<>();
        for (final Category category : this.resizedCategories) {
            entries.addAll(category.entries);
        }
        return entries;
    }

    private String efficientWrapper(final String value, final int width) {
        final List<String> lines = new ObjectArrayList<>();
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);

            final String lineCandidate = builder.toString() + c;
            if (this.minecraft.font.width(lineCandidate) > width) {
                // Add the line with a trailing new line
                lines.add(builder.append("\n").toString());
                // Clear the builder for a new line
                builder.delete(0, builder.length());
            }
            // Add the character
            builder.append(c);

            if (i + 1 >= value.length()) {
                lines.add(builder.toString());
            }
        }

        return String.join("", lines);
    }

    @Override
    public int getContentHeight() {
        int height = this.getTotalLineCount() * this.lineHeight;
        if (height < this.bottom - this.top - 8) {
            height = this.bottom - this.top - 8;
        }
        return height;
    }

    @Override
    protected int getScrollAmount() {
        return this.lineHeight * 3;
    }

    @Override
    protected void drawPanel(final GuiGraphics stack, final int entryRight, int relativeY, final Tesselator tess, final int mouseX,
            final int mouseY) {
        final int baseX = this.left + 4;

        final Font font = this.minecraft.font;
        if (this.resizedCategories.isEmpty()) {
            final int noResultsWidth = font.width(MetadataPanel.NO_RESULTS);

            stack.drawString(font,
                    MetadataPanel.NO_RESULTS,
                    (int) (((float) this.width / 2) + this.left - ((float) noResultsWidth / 2)),
                    this.top + 10,
                    0xFFFFFF
            );
            return;
        }

        // Iterate and draw categories
        for (final Category category : this.resizedCategories) {
            // Skip empty categories
            if (category.getEntries().size() == 0) {
                continue;
            }

            // Draw category name
            stack.drawString(font, category.name, baseX, relativeY, 0xFFFFFF);
            relativeY += this.lineHeight;

            // Iterate and draw entries
            for (final Entry entry : category.getEntries()) {
                if (entry.value == null) {
                    continue;
                }

                final int levelOffset = entry.level * MetadataPanel.INDENT_SIZE;
                final int keyX = baseX + MetadataPanel.INDENT_SIZE + levelOffset;
                final int separatorX = keyX + this.maxKeyWidth + 4 - levelOffset;
                final int valueX = separatorX + font.width(":") + 4;

                // Only draw key and separator if there is any key present
                if (entry.key != null) {
                    stack.drawString(font, entry.key, keyX, relativeY, 0xFFFFFF);

                    if (entry.rawValue != null && !entry.rawValue.isEmpty()) {
                        stack.drawString(font, ":", separatorX, relativeY, 0xFFFFFF);
                    }
                }

                // Draw the value, and update the value bounds if needed
                stack.drawString(font, entry.value, valueX, relativeY, 0xFFFFFF);
                if (entry.value.getStyle().getClickEvent() != null) {
                    entry.valueBounds =
                        new Bounds(valueX, relativeY + 1, valueX + font.width(entry.value),
                            relativeY + this.lineHeight);
                }
                relativeY += this.lineHeight;
            }

            relativeY += this.lineHeight;
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        // Find an entry match for where we clicked
        final Entry entry =
            this.getAllEntries().stream()
                .filter(e -> e.valueBounds != null && e.valueBounds.isInBounds((int) mouseX, (int) mouseY))
                .findFirst().orElse(null);

        if (entry == null) {
            return false;
        }

        final Component component = entry.value;
        if (component != null) {
            this.screen.handleComponentClicked(component.getStyle());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void drawBackground() {
    }

    /*
     * Credit: MinecraftForge
     * Changes: Set ichat to link if ichat is null
     */
    public static Component newChatWithLinks(final String string, final boolean allowMissingHeader) {
        // Includes ipv4 and domain pattern
        // Matches an ip (xx.xxx.xx.xxx) or a domain (something.com) with or
        // without a protocol or path.
        MutableComponent ichat = null;
        final Matcher matcher = MetadataPanel.URL_PATTERN.matcher(string);
        int lastEnd = 0;

        // Find all urls
        while (matcher.find()) {
            final int start = matcher.start();
            final int end = matcher.end();

            // Append the previous left overs.
            final String part = string.substring(lastEnd, start);
            if (part.length() > 0) {
                if (ichat == null) {
                    ichat = Component.literal(part);
                } else {
                    ichat.append(part);
                }
            }
            lastEnd = end;
            String url = string.substring(start, end);
            final MutableComponent link = Component.literal(url);
            final URI toOpen;

            try {
                // Add schema so client doesn't crash.
                if ((new URI(url)).getScheme() == null) {
                    if (!allowMissingHeader) {
                        if (ichat == null) {
                            ichat = Component.literal(url);
                        } else {
                            ichat.append(url);
                        }
                        continue;
                    }
                    url = "https://" + url;
                }
                toOpen = new URI(url);
            } catch (final URISyntaxException e) {
                // Bad syntax bail out!
                if (ichat == null) {
                    ichat = Component.literal(url);
                } else {
                    ichat.append(url);
                }
                continue;
            }

            // Set the click event and append the link.
            final ClickEvent click = new ClickEvent.OpenUrl(toOpen);
            link.withStyle(style -> style.withClickEvent(click)
                        .withUnderlined(true)
                        .withColor(ChatFormatting.BLUE));
            ichat = ichat == null ? link : ichat.append(link);
        }

        // Append the rest of the message.
        final String end = string.substring(lastEnd);
        if (ichat == null) {
            ichat = Component.literal(end);
        } else if (end.length() > 0) {
            ichat.append(string.substring(lastEnd));
        }
        return ichat;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE; // todo
    }

    @Override
    public void updateNarration(final NarrationElementOutput elementOutput) {
        if (this.activePluginName != null) {
            elementOutput.add(NarratedElementType.TITLE, this.activePluginName);
        }
    }

    private static final class Category {

        protected final Component name;
        protected final String rawName;
        private final List<Entry> entries = new ArrayList<>();

        public Category(final String name) {
            this.name = Component.literal(name)
                    .withStyle(s -> s.withBold(true).withUnderlined(true));
            this.rawName = name;
        }

        public Category(final String name, final List<Entry> entries) {
            this(name);
            this.entries.addAll(entries);
        }

        public Category addEntry(final Entry entry) {
            this.entries.add(entry);
            return this;
        }

        public Category setEntries(final List<Entry> entries) {
            this.entries.clear();
            this.entries.addAll(entries);
            return this;
        }

        public Category clearEntries() {
            this.entries.clear();
            return this;
        }

        public List<Entry> getEntries() {
            return Collections.unmodifiableList(this.entries);
        }
    }

    private static final class Entry {

        protected final int level;
        @Nullable protected final String rawKey;
        @Nullable protected final String rawValue;
        @Nullable protected Component key;
        @Nullable protected Component value;
        @Nullable protected Bounds valueBounds;

        public Entry(@Nullable final String key, @Nullable final String value) {
            this(key, value, 0, null);
        }

        public Entry(@Nullable final String key, @Nullable final String value, final int level) {
            this(key, value, level, null);
        }

        public Entry(@Nullable final String key, @Nullable final String value, final int level, @Nullable final String originalValue) {
            if (key != null) {
                this.key = Component.literal(key);
            }
            this.rawKey = key;

            if (value != null) {
                final MutableComponent newValue = Component.literal(value).withStyle(ChatFormatting.GRAY);

                // Account for text components that were split to new lines
                if (originalValue != null) {
                    final Component linkComponent = MetadataPanel.newChatWithLinks(originalValue, false);
                    if (linkComponent.getStyle().getClickEvent() != null) {
                        newValue.withStyle(s -> linkComponent.getStyle().applyTo(s));
                    }
                }
                this.value = newValue;
            }
            this.rawValue = value;

            this.level = level;
        }
    }
}
