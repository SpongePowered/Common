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
package org.spongepowered.common.text.action;

import static com.google.common.base.Preconditions.checkState;

import org.spongepowered.api.text.action.ShiftClickAction;

import javax.annotation.Nullable;

public abstract class SpongeShiftClickTextAction<R> extends SpongeTextAction<R> implements ShiftClickAction<R> {

    SpongeShiftClickTextAction(R result) {
        super(result);
    }

    public static final class SpongeInsertText extends SpongeShiftClickTextAction<String> implements ShiftClickAction.InsertText {

        SpongeInsertText(String result) {
            super(result);
        }

        public static class Builder implements ShiftClickAction.InsertText.Builder {

            @Nullable private String text;

            @Override
            public ShiftClickAction.InsertText.Builder text(String text) {
                this.text = text;
                return this;
            }

            @Override
            public ShiftClickAction.InsertText.Builder from(ShiftClickAction.InsertText value) {
                this.text = value.getResult();
                return this;
            }

            @Override
            public ShiftClickAction.InsertText.Builder reset() {
                this.text = null;
                return this;
            }

            @Override
            public ShiftClickAction.InsertText build() {
                checkState(this.text != null, "text not set");
                return new SpongeInsertText(this.text);
            }
        }
    }
}
