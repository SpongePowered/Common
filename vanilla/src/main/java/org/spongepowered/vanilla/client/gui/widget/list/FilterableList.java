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
package org.spongepowered.vanilla.client.gui.widget.list;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationSupplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.accessor.client.gui.components.AbstractSelectionListAccessor;
import org.spongepowered.vanilla.util.Bounds;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class FilterableList<P extends FilterableList<P, E>, E extends FilterableList.Entry<P, E>> extends AbstractSelectionList<E> {

    private static final Component USAGE_NARRATION = Component.translatable("narration.selection.usage");

    private final Screen screen;
    private Supplier<List<E>> filterSupplier;
    private Consumer<E> selectConsumer;
    protected final Font fontRenderer;
    protected E currentHoveredEntry;

    public FilterableList(final Screen screen, final int x, final int y, final int width, final int height, final int entryHeight) {
        super(Minecraft.getInstance(), width, screen.height, y, y + height, entryHeight);
        this.screen = screen;
        this.x0 = x;
        this.x1 = x + width;
        this.fontRenderer = Minecraft.getInstance().font;
    }

    public Screen getScreen() {
        return this.screen;
    }

    public int getX() {
        return this.x0;
    }

    public P setX(final int x) {
        this.x0 = x;
        return (P) this;
    }

    public int getY() {
        return this.y0;
    }

    public P setY(final int y) {
        this.y0 = y;
        return (P) this;
    }

    public P setPosition(final int x, final int y) {
        this.x0 = x;
        this.y0 = y;
        return (P) this;
    }

    public int getWidth() {
        return this.width;
    }

    public P setWidth(final int width) {
        this.width = width;
        return (P) this;
    }

    public int getHeight() {
        return this.height;
    }

    public P setHeight(final int height) {
        this.height = height;
        return (P) this;
    }

    public P setSize(final int width, final int height) {
        this.width = width;
        this.height = height;
        return (P) this;
    }

    public P setBounds(final int x, final int y, final int width, final int height) {
        this.x0 = x;
        this.y0 = y;
        this.width = width;
        this.height = height;
        return (P) this;
    }

    public int getRight() {
        return this.x0 + this.width;
    }

    public int getBottom() {
        return this.y0 + this.headerHeight;
    }

    public E getCurrentHoveredEntry() {
        return this.currentHoveredEntry;
    }

    public P setCurrentHoveredEntry(final E currentHoveredEntry) {
        this.currentHoveredEntry = currentHoveredEntry;
        return (P) this;
    }

    @SafeVarargs
    public final P addEntries(final E... entries) {
        this.addEntries(Arrays.asList(entries));
        return (P) this;
    }

    public P addEntries(final List<E> entries) {
        entries.forEach(this::addEntry);
        return (P) this;
    }

    public Consumer<E> getSelectConsumer() {
        return this.selectConsumer;
    }

    public P setSelectConsumer(final Consumer<E> selectConsumer) {
        this.selectConsumer = selectConsumer;
        return (P) this;
    }

    public Supplier<List<E>> getFilterSupplier() {
        return this.filterSupplier;
    }

    public P setFilterSupplier(final Supplier<List<E>> filterSupplier) {
        this.filterSupplier = filterSupplier;
        return (P) this;
    }

    public int getRowHeight() {
        return this.itemHeight;
    }

    @Override
    public int getRowWidth() {
        return this.width - 12;
    }

    @Override
    public int getRowLeft() {
        return this.x0 + 4;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.x0 + this.width - 6;
    }

    @Override
    public void setSelected(@Nullable final E entry) {
        if (this.selectConsumer != null) {
            this.selectConsumer.accept(entry);
        }

        super.setSelected(entry);
    }

    @Override
    public void render(final GuiGraphics stack, final int p_render_1_, final int p_render_2_, final float p_render_3_) {
        super.render(stack, p_render_1_, p_render_2_, p_render_3_);
    }

    @Override
    public boolean mouseClicked(final double p_mouseClicked_1_, final double p_mouseClicked_3_, final int p_mouseClicked_5_) {
        this.updateScrollingState(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_);
        if (!this.isMouseOver(p_mouseClicked_1_, p_mouseClicked_3_)) {
            return false;
        } else {
            final E e = this.getEntryAtPosition(p_mouseClicked_1_, p_mouseClicked_3_);
            if (e != null) {
                if (e.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_)) {
                    this.setFocused(e);
                    this.setDragging(true);
                    return true;
                }
            } else if (p_mouseClicked_5_ == 0) {
                this.clickedHeader((int) (p_mouseClicked_1_ - (double) (this.x0 + this.width / 2 - this.getRowWidth() / 2)),
                    (int) (p_mouseClicked_3_ - (double) this.y0) + (int) this.getScrollAmount() - 4);
                return true;
            }

            return true;
        }
    }

    @Override
    protected void renderList(final GuiGraphics $$0, final int $$1, final int $$2, final float $$3) {
        // Most of this is based on AbstractList::renderList logic
        final List<E> filteredList = this.filterSupplier == null ? this.children() : this.filterSupplier.get();

        if (filteredList.isEmpty()) {
            final Font font = this.minecraft.font;
            final String noResults = "No results...";
            final int noResultsWidth = font.width(noResults);

            $$0.drawString(font, noResults, (this.width / 2) + this.x0 - (noResultsWidth / 2), this.y0 + 10, ChatFormatting.GRAY.getColor());

            return;
        }

        final int $$4 = this.getRowLeft();
        final int $$5 = this.getRowWidth();
        final int $$6 = this.itemHeight - 4;
        final int $$7 = filteredList.size();

        for (int $$8 = 0; $$8 < $$7; ++$$8) {
            final int $$9 = this.getRowTop($$8);
            final int $$10 = this.getRowBottom($$8);
            if ($$10 >= this.y0 && $$9 <= this.y1) {
                this.renderItemFromList(filteredList, $$0, $$1, $$2, $$3, $$8, $$4, $$9, $$5, $$6);
            }
        }
    }

    private void renderItemFromList(final List<E> list, final GuiGraphics $$0, final int $$1, final int $$2, final float $$3, final int $$4, final int $$5, final int $$6, final int $$7, final int $$8) {
        final E $$9 = list.get($$4);
        $$9.renderBack($$0, $$4, $$6, $$5, $$7, $$8, $$1, $$2, Objects.equals(this.getHovered(), $$9), $$3);
        if (this.isSelectedItem($$4)) {
            final int $$10 = this.isFocused() ? -1 : -8355712;
            this.renderSelection($$0, $$6, $$7, $$8, $$10, -16777216);
        }

        $$9.render($$0, $$4, $$6, $$5, $$7, $$8, $$1, $$2, Objects.equals(this.getHovered(), $$9), $$3);
    }

    @Override
    public void updateNarration(final @NonNull NarrationElementOutput narrationConsumer) {
        final @org.checkerframework.checker.nullness.qual.Nullable E hovered = this.getCurrentHoveredEntry();
        if (hovered != null) {
            this.narrateListElementPosition(narrationConsumer, hovered);
            hovered.updateNarration(narrationConsumer);
        } else {
            final E selected = this.getSelected();
            if (selected != null) {
                this.narrateListElementPosition(narrationConsumer.nest(), selected);
                selected.updateNarration(narrationConsumer);
            }
        }

        if (this.isFocused()) {
            narrationConsumer.add(NarratedElementType.USAGE, FilterableList.USAGE_NARRATION);
        }
    }

    public static abstract class Entry<P extends FilterableList<P, E>, E extends org.spongepowered.vanilla.client.gui.widget.list.FilterableList.Entry<P, E>> extends net.minecraft.client.gui.components.AbstractSelectionList.Entry<E> implements NarrationSupplier {

        private final P parentList;

        public Entry(final P parentList) {
            this.parentList = parentList;
        }

        public P getParentList() {
            return this.parentList;
        }

        public abstract Bounds getInteractBounds();

        @SuppressWarnings("unchecked")
        @Override
        public void render(final GuiGraphics stack, final int p_render_1_, final int renderY, final int renderX, final int p_render_4_,
                final int p_render_5_, final int mouseX, final int mouseY, final boolean p_render_8_,
            final float p_render_9_) {
            if (this.getInteractBounds().isInBounds(mouseX, mouseY, renderX, renderY)) {
                this.parentList.currentHoveredEntry = (E) this;
            } else if (this.parentList.getCurrentHoveredEntry() == this) {
                this.parentList.currentHoveredEntry = null;
            }
        }
    }
}
