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
package org.spongepowered.common.mixin.core.adventure.text.format;

import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.accessor.network.chat.StyleAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.adventure.StyleBridge;

@Mixin(targets = "net.kyori.adventure.text.format.StyleImpl", remap = false)
public abstract class StyleImplMixin implements StyleBridge {
    private Style bridge$vanilla;

    @Override
    @SuppressWarnings("ConstantConditions")
    public Style bridge$asVanilla() {
        if ((Object) this == net.kyori.adventure.text.format.Style.empty()) {
            return Style.EMPTY;
        }

        if (this.bridge$vanilla == null) {
            final net.kyori.adventure.text.format.Style $this = (net.kyori.adventure.text.format.Style) (Object) this;
            this.bridge$vanilla = StyleAccessor.invoker$new(
                // color
                SpongeAdventure.asVanillaNullable($this.color()),
                null,
                // decorations
                SpongeAdventure.asVanillaNullable($this.decoration(TextDecoration.BOLD)),
                SpongeAdventure.asVanillaNullable($this.decoration(TextDecoration.ITALIC)),
                SpongeAdventure.asVanillaNullable($this.decoration(TextDecoration.UNDERLINED)),
                SpongeAdventure.asVanillaNullable($this.decoration(TextDecoration.STRIKETHROUGH)),
                SpongeAdventure.asVanillaNullable($this.decoration(TextDecoration.OBFUSCATED)),
                // events
                SpongeAdventure.asVanillaNullable($this.clickEvent()),
                SpongeAdventure.asVanillaNullable($this.hoverEvent()),
                // insertion
                $this.insertion(),
                // font
                SpongeAdventure.asVanillaNullable($this.font())
            );
        }
        // Style is immutable, no need to copy
        return this.bridge$vanilla;
    }
}
