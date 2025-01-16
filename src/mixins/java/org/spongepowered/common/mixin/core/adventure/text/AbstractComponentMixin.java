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
package org.spongepowered.common.mixin.core.adventure.text;

import net.kyori.adventure.text.AbstractComponent;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.adventure.ComponentBridge;

@SuppressWarnings("deprecation")
@Mixin(AbstractComponent.class)
public abstract class AbstractComponentMixin implements ComponentBridge {
    private MutableComponent bridge$vanillaComponent;

    @Override
    @SuppressWarnings("ConstantConditions")
    public net.minecraft.network.chat.Component bridge$asVanillaComponent() {
        if (this.bridge$vanillaComponent == null) {
            this.bridge$vanillaComponent = SpongeAdventure.asVanillaMutable((Component) this);
        }
        // To prevent potential issues with using this mutable component as part of another text component,
        // of if Minecraft or a mod decides to change this text component, we make a deep copy so that this
        // cache and its siblings are not affected (particularly important for TextComponent#setStyle).
        return this.bridge$vanillaComponent.copy();
    }

}
