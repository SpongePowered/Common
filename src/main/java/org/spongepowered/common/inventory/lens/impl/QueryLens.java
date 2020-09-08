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
package org.spongepowered.common.inventory.lens.impl;

import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.query.Query;
import org.spongepowered.common.inventory.adapter.impl.BasicInventoryAdapter;
import org.spongepowered.common.inventory.adapter.impl.QueryResultAdapter;
import org.spongepowered.common.inventory.fabric.Fabric;
import org.spongepowered.common.inventory.lens.Lens;

import java.util.Collection;
import java.util.Map;

public class QueryLens extends AbstractLens {

    private Query query;

    public QueryLens(Map<Lens, Integer> lensesWithOffsets, Query query) {
        super(0, lensesWithOffsets.keySet().stream().map(Lens::slotCount).mapToInt(i -> i).sum(), BasicInventoryAdapter.class);
        this.query = query;
        for (Map.Entry<Lens, Integer> entry : lensesWithOffsets.entrySet()) {
            final Integer offset = entry.getValue();
            final Lens lens = entry.getKey();
            this.addSpanningChild(new DelegatingLens(offset, lens));
        }
    }

    public QueryLens(Collection<Lens> lenses) {
        super(0, lenses.stream().map(Lens::slotCount).mapToInt(i -> i).sum(), BasicInventoryAdapter.class);
        for (Lens match : lenses) {
            this.addSpanningChild(match); // TODO properties?
        }
    }

    @Override
    public Inventory getAdapter(Fabric fabric, Inventory parent) {
        return new QueryResultAdapter(fabric, this, parent);
    }

}
