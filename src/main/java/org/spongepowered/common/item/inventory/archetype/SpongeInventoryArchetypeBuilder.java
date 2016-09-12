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
package org.spongepowered.common.item.inventory.archetype;

import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.item.inventory.InventoryArchetype;
import org.spongepowered.api.item.inventory.InventoryProperty;
import org.spongepowered.api.item.inventory.property.AcceptsItems;
import org.spongepowered.api.item.inventory.property.InventorySize;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.property.TitleProperty;
import org.spongepowered.api.text.Text;
import org.spongepowered.common.registry.CommonModuleRegistry;
import org.spongepowered.common.text.translation.SpongeTranslation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SpongeInventoryArchetypeBuilder implements InventoryArchetype.Builder {

    private List<InventoryArchetype> types = new ArrayList<>();
    private Map<String, InventoryProperty<String, ?>> properties = new HashMap<>();
    private Set<Class<? extends InteractInventoryEvent>> events = new HashSet<>();

    @Override
    public InventoryArchetype.Builder property(InventoryProperty<String, ?> property) {
        this.properties.put(property.getKey(), property);
        return this;
    }

    @Override
    public InventoryArchetype.Builder with(InventoryArchetype archetype) {
        this.types.add(archetype);
        return this;
    }

    @Override
    public InventoryArchetype.Builder with(InventoryArchetype... archetypes) {
        for (InventoryArchetype archetype : archetypes) {
            this.types.add(archetype);
        }
        return this;
    }

    @Override
    public InventoryArchetype build(String id, String name) {
        // TODO register archetype
        // TODO events
        return new CompositeInventoryArchetype(id, name, types, properties);
    }

    @Override
    public InventoryArchetype.Builder from(InventoryArchetype value) {
        if (value instanceof CompositeInventoryArchetype) {
            this.types.addAll(value.getChildArchetypes());
            this.properties.putAll(value.getProperties());
        }
        return this;
    }

    @Override
    public InventoryArchetype.Builder reset() {
        this.types = new ArrayList<>();
        this.properties = new HashMap<>();
        return this;
    }

}
