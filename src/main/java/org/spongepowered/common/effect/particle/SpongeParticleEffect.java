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
package org.spongepowered.common.effect.particle;

import com.google.common.collect.ImmutableMap;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleOption;
import org.spongepowered.common.data.util.DataQueries;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpongeParticleEffect implements ParticleEffect {

    private final SpongeParticleType type;
    private final Map<ParticleOption<?>, Object> options;

    public SpongeParticleEffect(SpongeParticleType type, Map<ParticleOption<?>, Object> options) {
        this.options = ImmutableMap.copyOf(options);
        this.type = type;
    }

    @Override
    public SpongeParticleType getType() {
        return this.type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Optional<V> getOption(ParticleOption<V> option) {
        return (Optional) Optional.ofNullable(this.options.get(option));
    }

    @Override
    public Map<ParticleOption<?>, Object> getOptions() {
        return this.options;
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        DataContainer dataContainer = new MemoryDataContainer();
        dataContainer.set(DataQueries.PARTICLE_TYPE, this.type);
        dataContainer.set(DataQueries.PARTICLE_OPTIONS, this.options.entrySet().stream().map(entry -> new MemoryDataContainer()
                .set(DataQueries.PARTICLE_OPTION_KEY, entry.getKey())
                .set(DataQueries.PARTICLE_OPTION_VALUE, entry.getValue()))
                .collect(Collectors.toList()));
        return dataContainer;
    }
}
