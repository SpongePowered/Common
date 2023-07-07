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
package org.spongepowered.common.util.persistence.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.data.persistence.DataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.common.data.persistence.NBTTranslator;
import org.spongepowered.common.launch.SpongeExtension;

import java.util.Optional;

@ExtendWith(SpongeExtension.class)
public class NBTTranslationTest {

    @Test
    public void testContainerToNBT() {
        DataManager service = Mockito.mock(DataManager.class);
        DataBuilder<FakeSerializable> builder = new FakeBuilder();
        when(service.builder(FakeSerializable.class)).thenReturn(Optional.of(builder));
        DataContainer container = DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED);
        container.set(DataQuery.of("foo"), "bar");
        FakeSerializable temp = new FakeSerializable("bar", 7, 10.0D, "nested");
        container.set(DataQuery.of("myFake"), temp);
        CompoundTag compound = NBTTranslator.INSTANCE.translate(container);
        DataView translatedContainer = NBTTranslator.INSTANCE.translateFrom(compound);
        assertEquals(container, translatedContainer);
    }

    @Test
    public void testDotContainerKeys() {
        final DataContainer container = DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED).set(DataQuery.of("my.key.to.data"), 1);
        CompoundTag compound = NBTTranslator.INSTANCE.translate(container);
        DataView translatedContainer = NBTTranslator.INSTANCE.translateFrom(compound);
        assertEquals(container, translatedContainer);
    }

}