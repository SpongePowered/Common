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
package org.spongepowered.common.datapack.recipe;

import org.spongepowered.common.datapack.DataPackSerializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RecipeDataPackSerializer extends DataPackSerializer<RecipeSerializedObject> {

    public RecipeDataPackSerializer() {
        super("Recipes", "recipes");
    }

    @Override
    protected void serializeAdditional(final Path dataDirectory, final RecipeSerializedObject object) throws IOException {
        if (object.getAdvancementObject() != null) {
            final Path advancementFile = dataDirectory.resolve("advancements").resolve(object.getAdvancementObject().getKey().getValue() + ".json");
            Files.createDirectories(advancementFile.getParent());
            this.writeFile(advancementFile, object.getAdvancementObject().getObject());
        }
    }
}
