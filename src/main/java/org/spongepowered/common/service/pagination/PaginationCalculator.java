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
package org.spongepowered.common.service.pagination;

import com.google.common.annotations.VisibleForTesting;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.loader.HeaderMode;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.math.GenericMath;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.PrimitiveIterator;

/**
 * Pagination calculator for players. Handles calculation of text widths,
 * centering text, adding padding, adding spacing, and more.
 */
class PaginationCalculator {

    private static final String NON_UNICODE_CHARS;
    private static final int[] NON_UNICODE_CHAR_WIDTHS;
    private static final byte[] UNICODE_CHAR_WIDTHS;
    private static final int LINE_WIDTH = 320;

    private final int linesPerPage;

    /**
     * Constructs a new pagination calculator.
     *
     * @param linesPerPage The amount of lines per page there should be
     */
    PaginationCalculator(int linesPerPage) {
        this.linesPerPage = linesPerPage;
    }

    static {
        ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder()
                .setURL(PaginationCalculator.class.getResource("font-sizes.json"))
                .setHeaderMode(HeaderMode.NONE)
                .build();
        try {
            ConfigurationNode node = loader.load();
            NON_UNICODE_CHARS = node.getNode("non-unicode").getString();
            List<? extends ConfigurationNode> charWidths = node.getNode("char-widths").getChildrenList();
            int[] nonUnicodeCharWidths = new int[charWidths.size()];
            for (int i = 0; i < nonUnicodeCharWidths.length; ++i) {
                nonUnicodeCharWidths[i] = charWidths.get(i).getInt();
            }
            NON_UNICODE_CHAR_WIDTHS = nonUnicodeCharWidths;

            List<? extends ConfigurationNode> glyphWidths = node.getNode("glyph-widths").getChildrenList();
            byte[] unicodeCharWidths = new byte[glyphWidths.size()];
            for (int i = 0; i < unicodeCharWidths.length; ++i) {
                unicodeCharWidths[i] = (byte) glyphWidths.get(i).getInt();
            }
            UNICODE_CHAR_WIDTHS = unicodeCharWidths;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    int getLinesPerPage() {
        return this.linesPerPage;
    }

    /**
     * Gets the number of lines the specified text flows into.
     *
     * @param text The text to calculate the number of lines for
     * @return The number of lines that this text flows into
     */
    int getLines(Component text) {
        //TODO: this needs fixing as well.
        return (int) Math.ceil((double) this.getWidth(text) / PaginationCalculator.LINE_WIDTH);
    }

    /**
     * Gets the width of a character with the specified code
     * point, accounting for if its text is bold our not.
     *
     * @param codePoint The code point of the character
     * @param isBold Whether or not the character is bold or not
     * @return The width of the character at the code point
     */
    @VisibleForTesting
    int getWidth(int codePoint, boolean isBold) {
        int nonUnicodeIdx = PaginationCalculator.NON_UNICODE_CHARS.indexOf(codePoint);
        int width;
        if (codePoint == 32) {
            width = 4;
        } else if (codePoint > 0 && nonUnicodeIdx != -1) {
            width = PaginationCalculator.NON_UNICODE_CHAR_WIDTHS[nonUnicodeIdx];
        } else if (PaginationCalculator.UNICODE_CHAR_WIDTHS[codePoint] != 0) {
            //from 1.9 & 255 to avoid strange signed int math ruining things.
            //https://bugs.mojang.com/browse/MC-7181
            final int temp = PaginationCalculator.UNICODE_CHAR_WIDTHS[codePoint] & 255;
            // Split into high and low nibbles.
            //bit digits
            //87654321 >>> 4 = 00008765
            int startColumn = temp >>> 4;
            //87654321 & 00001111 = 00004321
            int endColumn = temp & 15;

            width = (endColumn + 1) - startColumn;
            //Why does this scaling happen?
            //I believe it makes unicode fonts skinnier to better match the character widths of the default Minecraft
            // font however there is a int math vs float math bug in the Minecraft FontRenderer.
            //The float math is adjusted for rendering, they attempt to do the same thing for calculating string widths
            //using integer math, this has potential rounding errors, but we should copy it and use ints as well.
            width = (width / 2) + 1;
        } else {
            width = 0;
        }
        //if bolded width gets 1 added.
        if (isBold && width > 0) {
            width = width + 1;
        }

        return width;
    }

    /**
     * Calculates the width of a given text as the number of character
     * pixels/columns the line takes up.
     *
     * @param text The text to get the width of
     * @return The amount of character pixels/columns the text takes up
     */
    @VisibleForTesting
    int getWidth(Component text) {
        Iterable<ITextComponent> children = SpongeAdventure.asVanilla(text);
        int total = 0;

        for (ITextComponent child : children) {
            PrimitiveIterator.OfInt i_it;
            if (child instanceof StringTextComponent || child instanceof TranslationTextComponent) {
                i_it = child.getUnformattedComponentText().codePoints().iterator();
            } else {
                continue;
            }

            boolean bold = child.getStyle().getBold();

            Integer cp;
            boolean newLine = false;
            while (i_it.hasNext()) {
                cp = i_it.next();
                if (cp == '\n') {
                    // if the previous character is a '\n'
                    if (newLine) {
                        total += PaginationCalculator.LINE_WIDTH;
                    } else {
                        total = ((int) Math.ceil((double) total / PaginationCalculator.LINE_WIDTH)) * PaginationCalculator.LINE_WIDTH;
                        newLine = true;
                    }
                } else {
                    int width = this.getWidth(cp, bold);
                    total += width;
                    newLine = false;
                }
            }
        }

        return total;
    }

    /**
     * Centers a text within the middle of the chat box.
     *
     * <p>Generally used for titles and footers.</p>
     *
     * <p>To use no heading, just pass in a 0 width text for
     * the first argument.</p>
     *
     * @param text The text to center
     * @param padding A padding character with a width >1
     * @return The centered text, or if too big, the original text
     */
    //TODO: Probably should completely rewrite this to not compute padding, but loop until the padding is done, unless
    //we can get accurate computation of padding ahead of time.
    Component center(Component text, Component padding) {
        int inputLength = this.getWidth(text);
        //Minecraft breaks lines when the next character would be > then LINE_WIDTH, this seems most graceful way to fail
        if (inputLength >= PaginationCalculator.LINE_WIDTH) {
            return text;
        }
        final Component textWithSpaces = this.addSpaces(TextComponent.space(), text);

        //Minecraft breaks lines when the next character would be > then LINE_WIDTH
        boolean addSpaces = this.getWidth(textWithSpaces) <= PaginationCalculator.LINE_WIDTH;

        int paddingLength = this.getWidth(padding);
        final TextComponent.Builder output = TextComponent.builder();

        //Using 0 width unicode symbols as padding throws us into an unending loop, replace them with the default padding
        if (paddingLength < 1) {
            padding = TextComponent.of("=");
            paddingLength = this.getWidth(padding);
        }

        //if we only need padding
        if (inputLength == 0) {
            this.addPadding(padding, output, GenericMath.floor((double) PaginationCalculator.LINE_WIDTH / paddingLength));
        } else {
            if (addSpaces) {
                text = textWithSpaces;
                inputLength = this.getWidth(textWithSpaces);
            }

            int paddingNecessary = PaginationCalculator.LINE_WIDTH - inputLength;

            int paddingCount = GenericMath.floor(paddingNecessary / paddingLength);
            //pick a halfway point
            int beforePadding = GenericMath.floor(paddingCount / 2.0);
            //Do not use ceil, this prevents floating point errors.
            int afterPadding = paddingCount - beforePadding - 1;

            this.addPadding(padding, output, beforePadding);
            output.append(text);
            this.addPadding(padding, output, afterPadding);
        }

        return this.finalizeBuilder(text, output);
    }

    /**
     * Finalizes the builder used in centering text.
     *
     * @param text The text to get the style from
     * @param build The work in progress text builder
     * @return The finalized, properly styled text.
     */
    private Component finalizeBuilder(Component text, TextComponent.Builder build) {
        return build.style(text.style().toBuilder().build()).build();
    }

    /**
     * Adds spaces to both sides of the specified text.
     *
     * <p>Overrides all color and style with the
     * text's color and style.</p>
     *
     * @param spaces The spaces to use
     * @param text The text to add to
     * @return The text with the added spaces
     */
    private Component addSpaces(Component spaces, Component text) {
        return TextComponent.builder()
                .append(spaces)
                .append(text)
                .append(spaces)
                .style(text.style().toBuilder().build())
                .build();
    }

    /**
     * Adds the specified padding text to a piece of text being built
     * up to a certain amount specified by a count.
     *
     * @param padding The padding text to use
     * @param build The work in progress text to add to
     * @param count The amount of padding to add
     */
    private void addPadding(Component padding, TextComponent.Builder build, int count) {
        if (count > 0) {
            build.append(Collections.nCopies(count, padding));
        }
    }
}
