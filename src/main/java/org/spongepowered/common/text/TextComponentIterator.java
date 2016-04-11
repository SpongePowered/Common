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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.UnmodifiableIterator;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.common.interfaces.text.IMixinChatComponent;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

public class TextComponentIterator extends UnmodifiableIterator<ITextComponent> {

    private IMixinChatComponent component;
    private Iterator<ITextComponent> children;
    @Nullable private Iterator<ITextComponent> currentChildIterator;

    public TextComponentIterator(IMixinChatComponent component) {
        this.component = checkNotNull(component, "component");
    }

    public TextComponentIterator(Iterator<ITextComponent> children) {
        this.children = checkNotNull(children, "children");
    }

    @Override
    public boolean hasNext() {
        return this.component != null || (this.currentChildIterator != null && this.currentChildIterator.hasNext()) || this.children.hasNext();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ITextComponent next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        if (this.component != null) {
            this.children = this.component.childrenIterator();

            ITextComponent result = this.component;
            this.component = null;
            return result;
        } else if (this.currentChildIterator == null || !this.currentChildIterator.hasNext()) {
            this.currentChildIterator = ((IMixinChatComponent) this.children.next()).withChildren().iterator();
        }

        return this.currentChildIterator.next();
    }

}
