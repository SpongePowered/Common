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
package org.spongepowered.common.mixin.api.mcp.advancements;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.advancement.AdvancementTree;
import org.spongepowered.api.advancement.criteria.AdvancementCriterion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.advancements.AdvancementBridge;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

@Mixin(Advancement.class)
public abstract class AdvancementMixin_API implements org.spongepowered.api.advancement.Advancement, AdvancementTree {

    @Shadow @Final @Nullable private DisplayInfo display;
    @Shadow @Final private Set<Advancement> children;
    @Shadow @Final private ResourceLocation id;
    @Shadow @Final private ITextComponent displayText;
    @Shadow @Final private Advancement parent;

    @Override
    public Optional<AdvancementTree> getTree() {
        if (this.parent == null) {
            return Optional.of(this);
        }
        return ((org.spongepowered.api.advancement.Advancement) this.parent).getTree();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Collection<org.spongepowered.api.advancement.Advancement> getChildren() {
        return ImmutableList.copyOf((Collection) this.children);
    }

    @Override
    public AdvancementCriterion getCriterion() {
        return ((AdvancementBridge) this).bridge$getCriterion();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<org.spongepowered.api.advancement.Advancement> getParent() {
        return (Optional<org.spongepowered.api.advancement.Advancement>) (Optional<?>) ((AdvancementBridge) this).bridge$getParent();
    }

    @Override
    public Optional<org.spongepowered.api.advancement.DisplayInfo> getDisplayInfo() {
        return Optional.ofNullable((org.spongepowered.api.advancement.DisplayInfo) this.display);
    }

    @Override
    public List<Component> toToastText() {
        return ((AdvancementBridge) this).bridge$getToastText();
    }

    @Override
    public ResourceKey getKey() {
        return (ResourceKey) (Object) this.id;
    }

    @Override
    public Component asComponent() {
        return SpongeAdventure.asAdventure(this.displayText);
    }

    @Override
    public org.spongepowered.api.advancement.Advancement getRootAdvancement() {
        return this;
    }

    @Override
    public String getBackgroundPath() {
        if (this.display == null) {
            return null;
        }
        return this.display.getBackground().getPath();
    }
}
