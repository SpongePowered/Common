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
package org.spongepowered.common.map.decoration;

import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.map.decoration.MapDecoration;
import org.spongepowered.common.util.Constants;

import java.util.Optional;

public interface SpongeMapDecoration extends MapDecoration {
    /**
     * If this MapDecoration should be saved to disk,
     * not necessary if it would be recalculated.
     * @param persistent if should be saved to disk
     */
    void setPersistent(boolean persistent);

    /**
     * If this MapDecoration will be saved to disk
     * @return if this MapDecoration will be saved to disk
     */
    boolean isPersistent();

    /**
     * Gets the Minecraft NBT of this decoration, used for saving
     * @return NBTTagCompound NBT of this MapDecoration
     */
    NBTTagCompound getMCNBT();

    static SpongeMapDecoration fromNBT(NBTTagCompound nbt) {
        SpongeMapDecoration spongeMapDecoration = (SpongeMapDecoration) new net.minecraft.world.storage.MapDecoration(
                net.minecraft.world.storage.MapDecoration.Type.byIcon(nbt.getByte(Constants.Map.DECORATION_TYPE.asString(""))),
                (byte)nbt.getDouble(Constants.Map.DECORATION_X.asString("")),
                (byte)nbt.getDouble(Constants.Map.DECORATION_Y.asString("")),
                (byte)nbt.getDouble(Constants.Map.DECORATION_ROTATION.asString(""))
        );
        spongeMapDecoration.setKey(nbt.getString(Constants.Map.DECORATION_ID.asString("")));
        return spongeMapDecoration;
    }

    static Optional<MapDecoration> fromContainer(DataView container) {
        if (!container.contains(
                Constants.Map.DECORATION_TYPE,
                Constants.Map.DECORATION_ID,
                Constants.Map.DECORATION_X,
                Constants.Map.DECORATION_Y,
                Constants.Map.DECORATION_ROTATION
        )) {
            return Optional.empty();
        }
        SpongeMapDecoration spongeMapDecoration = (SpongeMapDecoration) new net.minecraft.world.storage.MapDecoration(
                net.minecraft.world.storage.MapDecoration.Type.byIcon(container.getByte(Constants.Map.DECORATION_TYPE).get()),
                container.getByte(Constants.Map.DECORATION_X).get(),
                container.getByte(Constants.Map.DECORATION_Y).get(),
                container.getByte(Constants.Map.DECORATION_ROTATION).get()
        );
       spongeMapDecoration.setKey(container.getString(Constants.Map.DECORATION_ID).get());
       return Optional.of(spongeMapDecoration);
    }

    /**
     * Sets the key used in the decoration map of a MapData
     * This can be compared using == since they are the same String
     * @param key String to set as key
     */
    void setKey(String key);

    /**
     * Gets the key used in the decoration map of a MapData
     * This can be compared using == since they are the same String
     * @return String key
     */
    String getKey();
}
