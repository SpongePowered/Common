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
package org.spongepowered.common.advancement;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.advancement.Advancement;
import org.spongepowered.api.advancement.AdvancementTree;
import org.spongepowered.common.bridge.advancements.AdvancementBridge;
import org.spongepowered.common.bridge.advancements.DisplayInfoBridge;
import org.spongepowered.common.util.SpongeCatalogBuilder;

public final class SpongeAdvancementTreeBuilder extends SpongeCatalogBuilder<AdvancementTree,
        AdvancementTree.Builder> implements AdvancementTree.Builder {

    private Advancement rootAdvancement;
    private String name;
    private String background;

    public SpongeAdvancementTreeBuilder() {
        this.reset();
    }

    @Override
    public AdvancementTree.Builder rootAdvancement(Advancement rootAdvancement) {
        checkNotNull(rootAdvancement);
        checkState(((AdvancementBridge) rootAdvancement).bridge$isRegistered(), "The root advancement must be registered.");
        checkState(!rootAdvancement.getParent().isPresent(), "The root advancement cannot have a parent.");
        checkState(rootAdvancement.getDisplayInfo().isPresent(), "The root advancement must have display info.");
        checkState(((DisplayInfoBridge) rootAdvancement.getDisplayInfo().get()).bridge$getBackground() == null,
                "The root advancement is already used by a different Advancement Tree.");
        this.rootAdvancement = rootAdvancement;
        return this;
    }

    @Override
    public AdvancementTree.Builder background(final String background) {
        checkNotNull(background, "background");
        this.background = background;
        return this;
    }

    @Override
    public AdvancementTree.Builder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    protected AdvancementTree build(ResourceKey key) {
        checkNotNull(key);
        checkState(this.rootAdvancement != null, "The root advancement must be set");
        final SpongeAdvancementTree advancementTree = new SpongeAdvancementTree(this.rootAdvancement, key, this.name);
        ((DisplayInfoBridge) this.rootAdvancement.getDisplayInfo().get()).bridge$setBackground(this.background);
        ((AdvancementBridge) this.rootAdvancement).bridge$setParent(null);
        applyTree(this.rootAdvancement, advancementTree);
        return advancementTree;
    }

    private static void applyTree(final Advancement advancement, final AdvancementTree tree) {
        ((AdvancementBridge) advancement).bridge$setTree(tree);
        for (final Advancement child : advancement.getChildren()) {
            applyTree(child, tree);
        }
    }

    @Override
    public AdvancementTree.Builder reset() {
        this.key = null;
        this.name = null;
        this.background = "minecraft:textures/gui/advancements/backgrounds/stone.png";
        this.rootAdvancement = null;
        return this;
    }
}
