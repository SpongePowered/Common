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
package org.spongepowered.vanilla.installer.model.mojang;

import java.net.URL;
import java.util.List;
import java.util.Map;

public record Version(AssetIndex assetIndex, String assets, Downloads downloads, String id,
                      List<Library> libraries, String mainClass, VersionType type, String releaseTime, String time) {

    public record AssetIndex(String id, String sha1, int size, int totalSize, URL url) {}

    public record Downloads(Download client, Download client_mappings, Download server, Download server_mappings) {
        public record Download(String sha1, int size, URL url) {
        }
    }

    public record Library(Library.Downloads downloads, String name) {
        public record Downloads(Artifact artifact, Map<String, Artifact> classifiers) {
            public record Artifact(String path, String sha1, int size, URL url) {
            }
        }
    }
}
