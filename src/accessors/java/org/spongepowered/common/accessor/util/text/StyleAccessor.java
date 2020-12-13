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
package org.spongepowered.common.accessor.util.text;

import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Style.class)
public interface StyleAccessor {

    @Accessor("color") TextFormatting accessor$getColor();

    @Accessor("color") void accessor$setColor(TextFormatting color);

    @Accessor("bold") Boolean accessor$getBold();

    @Accessor("bold") void accessor$setBold(Boolean bold);

    @Accessor("italic") Boolean accessor$getItalic();

    @Accessor("italic") void accessor$setItalic(Boolean italic);

    @Accessor("underlined") Boolean accessor$getUnderlined();

    @Accessor("underlined") void accessor$setUnderlined(Boolean underlined);

    @Accessor("strikethrough") Boolean accessor$getStrikethrough();

    @Accessor("strikethrough") void accessor$setStrikethrough(Boolean strikethrough);

    @Accessor("obfuscated") Boolean accessor$getObfuscated();

    @Accessor("obfuscated") void accessor$setObfuscated(Boolean obfuscated);
}
