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
package org.spongepowered.common.data.processor.multi.item;

import com.flowpowered.math.vector.Vector2i;
import com.google.common.collect.Maps;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.MapData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.item.ImmutableMapItemData;
import org.spongepowered.api.data.manipulator.mutable.item.MapItemData;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.common.bridge.optimization.OptimizedMapDataBridge;
import org.spongepowered.common.bridge.world.WorldServerBridge;
import org.spongepowered.common.data.manipulator.mutable.item.SpongeMapItemData;
import org.spongepowered.common.data.processor.common.AbstractItemDataProcessor;
import org.spongepowered.common.data.util.DataUtil;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.world.WorldManager;

import java.util.Map;
import java.util.Optional;

public class MapItemDataProcessor extends AbstractItemDataProcessor<MapItemData, ImmutableMapItemData> {


    public MapItemDataProcessor() {
        super(input -> ((org.spongepowered.api.item.inventory.ItemStack)input).getType() == ItemTypes.FILLED_MAP);
    }

    @Override
    public boolean set(ItemStack itemStack, Map<Key<?>, Object> keyValues) {
        int metaData = itemStack.getMetadata();
        MapData mapData = (MapData)
                ((net.minecraft.world.storage.MapStorage)Sponge.getServer().getMapStorage().get())
                        .getOrLoadData(MapData.class, Constants.ItemStack.MAP_PREFIX + metaData);
        if (mapData == null)
            return false;
        Vector2i location = (Vector2i)keyValues.get(Keys.MAP_LOCATION);
        mapData.xCenter = location.getX();
        mapData.zCenter = location.getY();
        int id = ((WorldServerBridge)keyValues.get(Keys.MAP_WORLD)).bridge$getDimensionId();
        ((OptimizedMapDataBridge)mapData).setWorldId(id);
        mapData.trackingPosition = (boolean)keyValues.get(Keys.MAP_TRACKS_PLAYERS);
        mapData.unlimitedTracking = (boolean)keyValues.get(Keys.MAP_UNLIMITED_TRACKING);
        mapData.scale = (byte)keyValues.get(Keys.MAP_SCALE);
        return true;
    }

    @Override
    public Map<Key<?>, ?> getValues(ItemStack dataHolder) {
        Map<Key<?>, Object> values = Maps.newIdentityHashMap();

        int metaData = dataHolder.getMetadata();
        MapData mapData = (MapData)
                ((net.minecraft.world.storage.MapStorage)Sponge.getServer().getMapStorage().get())
                        .getOrLoadData(MapData.class, Constants.ItemStack.MAP_PREFIX + metaData);
        values.put(Keys.MAP_LOCATION, new Vector2i(mapData.xCenter, mapData.zCenter));
        values.put(Keys.MAP_WORLD, WorldManager.getWorldByDimensionId(mapData.dimension).get());
        values.put(Keys.MAP_TRACKS_PLAYERS, mapData.trackingPosition);
        values.put(Keys.MAP_UNLIMITED_TRACKING, mapData.unlimitedTracking);
        values.put(Keys.MAP_SCALE, mapData.scale);

        return values;
    }

    @Override
    public MapItemData createManipulator() {
        return new SpongeMapItemData();
    }

    @Override
    public Optional<MapItemData> fill(DataContainer container, MapItemData data) {
        if (!container.contains(
                Keys.MAP_LOCATION.getQuery(),
                Keys.MAP_WORLD.getQuery(),
                Keys.MAP_TRACKS_PLAYERS.getQuery(),
                Keys.MAP_UNLIMITED_TRACKING.getQuery(),
                Keys.MAP_SCALE.getQuery()
        )) {
            return Optional.empty();
        }

        data.set(Keys.MAP_LOCATION, DataUtil.getData(container, Keys.MAP_LOCATION));
        data.set(Keys.MAP_WORLD, DataUtil.getData(container, Keys.MAP_WORLD));
        data.set(Keys.MAP_TRACKS_PLAYERS, DataUtil.getData(container, Keys.MAP_TRACKS_PLAYERS));
        data.set(Keys.MAP_UNLIMITED_TRACKING, DataUtil.getData(container, Keys.MAP_UNLIMITED_TRACKING));
        data.set(Keys.MAP_SCALE, DataUtil.getData(container, Keys.MAP_SCALE));

        return Optional.of(data);
    }

    @Override
    public DataTransactionResult remove(DataHolder dataHolder) {
        return DataTransactionResult.failNoData();
    }

    @Override
    public boolean doesDataExist(ItemStack dataHolder) {
        return ((org.spongepowered.api.item.inventory.ItemStack)dataHolder).getType() == ItemTypes.FILLED_MAP
                && ((net.minecraft.world.storage.MapStorage)Sponge.getServer().getMapStorage().get())
                .getOrLoadData(MapData.class, Constants.ItemStack.MAP_PREFIX + dataHolder.getMetadata()) != null;
    }
}
