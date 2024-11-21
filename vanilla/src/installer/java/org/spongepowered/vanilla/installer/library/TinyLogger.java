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
package org.spongepowered.vanilla.installer.library;

import org.spongepowered.libs.Logger;
import org.tinylog.configuration.Configuration;
import org.tinylog.format.AdvancedMessageFormatter;
import org.tinylog.format.MessageFormatter;
import org.tinylog.provider.LoggingProvider;
import org.tinylog.provider.ProviderRegistry;

public final class TinyLogger implements Logger {
    private static final MessageFormatter formatter = new AdvancedMessageFormatter(Configuration.getLocale(), Configuration.isEscapingEnabled());
    private static final LoggingProvider provider = ProviderRegistry.getLoggingProvider();

    public static final TinyLogger INSTANCE = new TinyLogger();

    private TinyLogger() {}

    @Override
    public void log(final Level level, final String message, final Object... args) {
        TinyLogger.provider.log(2, null, this.convertLevel(level), null, TinyLogger.formatter, message, args);
    }

    @Override
    public void log(final Level level, final String message, final Throwable throwable) {
        TinyLogger.provider.log(2, null, this.convertLevel(level), throwable, TinyLogger.formatter, message, (Object[]) null);
    }

    private org.tinylog.Level convertLevel(final Level level) {
        return switch (level) {
            case DEBUG -> org.tinylog.Level.DEBUG;
            case INFO -> org.tinylog.Level.INFO;
            case WARN -> org.tinylog.Level.WARN;
            case ERROR -> org.tinylog.Level.ERROR;
        };
    }
}
