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
package org.spongepowered.common.config.inheritable;


import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.HashMap;
import java.util.Map;

@ConfigSerializable
public final class BlockEntityActivationCategory {

    @Setting
    @Comment("If 'true', newly discovered block entities will be added to this config with default settings.")
    private boolean autoPopulate = false;
    @Setting
    @Comment("Default activation block range used for all block entities unless overridden.")
    private int defaultBlockRange = 64;
    @Setting
    @Comment("Default tick rate used for all block entities unless overridden.")
    private int defaultTickRate = 1;
    @Setting("mods")
    @Comment("Per-mod overrides. Refer to the minecraft default mod for example.")
    private Map<String, BlockEntityActivationModCategory> modList = new HashMap<>();

    public BlockEntityActivationCategory() {
    }

    public boolean autoPopulateData() {
        return this.autoPopulate;
    }

    public int getDefaultBlockRange() {
        return this.defaultBlockRange;
    }

    public int getDefaultTickRate() {
        return this.defaultTickRate;
    }

    public Map<String, BlockEntityActivationModCategory> getModList() {
        return this.modList;
    }
}
