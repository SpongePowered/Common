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
package org.spongepowered.common.util;

import io.netty.channel.local.LocalAddress;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class NetworkUtil {

    public static final String LOCAL_ADDRESS = "local";

    /**
     * Returns a string representation of the host of a given {@link SocketAddress}.
     *
     * <ul>
     *     <li>For {@link InetSocketAddress} this will be a textual representation
     *     according to {@link InetSocketAddress#getHostString()}</li>
     *     <li>For {@link LocalAddress} this will be {@link #LOCAL_ADDRESS}</li>
     *     <li>For every other type this will be {@link SocketAddress#toString()}</li>
     * </ul>
     *
     * @param address The address to get the string representation of
     * @return The string representation of the host
     */
    public static String getHostString(final SocketAddress address) {
        if (address instanceof InetSocketAddress isa) {
            return isa.getHostString();
        } else if (address instanceof LocalAddress) {
            return NetworkUtil.LOCAL_ADDRESS;
        }

        return address.toString();
    }

    /**
     * Returns the cleaned hostname for the input sent by the client.
     *
     * @param host The host sent by the client
     * @return The cleaned hostname
     */
    public static String cleanVirtualHost(String host) {
        // FML appends a marker to the host to recognize FML clients (\0FML\0)
        host = NetworkUtil.substringBefore(host, '\0');

        // When clients connect with a SRV record, their host contains a trailing '.'
        if (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }

        return host;
    }

    public static String substringBefore(final String s, final char c) {
        final int pos = s.indexOf(c);
        return pos >= 0 ? s.substring(0, pos) : s;
    }

    private NetworkUtil() {
    }
}
