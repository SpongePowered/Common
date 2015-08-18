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

package org.spongepowered.common.text;

import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.IChatComponent;

import java.util.List;

public class ChatComponentPlaceholder extends ChatComponentStyle {

    private final String placeholderKey;
    private IChatComponent fallback;

    public ChatComponentPlaceholder(String placeholderKey) {
        this(placeholderKey, null);
    }

    public ChatComponentPlaceholder(String placeholderKey, IChatComponent fallback) {
        this.placeholderKey = placeholderKey;
        this.fallback = fallback;
    }

    public String getPlaceholderKey() {
        return this.placeholderKey;
    }

    @Override
    public String getUnformattedTextForChat() {
        return this.fallback == null ? "{" + this.placeholderKey + "}" : this.fallback.getUnformattedTextForChat();
    }

    public IChatComponent getFallback() {
        return this.fallback;
    }

    public void setFallback(IChatComponent fallback) {
        this.fallback = fallback;
    }

    @Override
    public ChatComponentPlaceholder createCopy() {
        ChatComponentPlaceholder copy = new ChatComponentPlaceholder(this.placeholderKey, this.fallback == null ? null : this.fallback.createCopy());
        copy.setChatStyle(this.getChatStyle().createShallowCopy());
        @SuppressWarnings("unchecked")
        List<IChatComponent> siblings = this.getSiblings();
        for (IChatComponent sibling : siblings) {
            copy.appendSibling(sibling.createCopy());
        }
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof ChatComponentPlaceholder)) {
            return false;
        } else {
            ChatComponentPlaceholder other = (ChatComponentPlaceholder) obj;
            return placeholderKey.equals(other.placeholderKey) && super.equals(obj);
        }
    }

    @Override
    public String toString() {
        return "PlaceholderComponent{component=\'" + this.placeholderKey +
                "\', fallback=\'" + this.fallback +
                "\', siblings=" + this.siblings +
                ", style=" + this.getChatStyle() + '}';
    }

}
