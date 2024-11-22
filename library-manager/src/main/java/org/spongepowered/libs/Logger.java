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
package org.spongepowered.libs;

// Common interface for log4j and tinylog
public interface Logger {

    enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    default void debug(final String message, final Object... args) {
        this.log(Level.DEBUG, message, args);
    }

    default void debug(final Level level, final String message, final Throwable throwable) {
        this.log(Level.DEBUG, message, throwable);
    }

    default void info(final String message, final Object... args) {
        this.log(Level.INFO, message, args);
    }

    default void info(final Level level, final String message, final Throwable throwable) {
        this.log(Level.INFO, message, throwable);
    }

    default void warn(final String message, final Object... args) {
        this.log(Level.WARN, message, args);
    }

    default void warn(final Level level, final String message, final Throwable throwable) {
        this.log(Level.WARN, message, throwable);
    }

    default void error(final String message, final Object... args) {
        this.log(Level.ERROR, message, args);
    }

    default void error(final Level level, final String message, final Throwable throwable) {
        this.log(Level.ERROR, message, throwable);
    }

    void log(final Level level, final String message, final Object... args);

    void log(final Level level, final String message, final Throwable throwable);
}
