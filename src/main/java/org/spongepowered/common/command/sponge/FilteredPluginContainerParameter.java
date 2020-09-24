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
package org.spongepowered.common.command.sponge;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.TextComponent;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueParameter;
import org.spongepowered.common.launch.Launch;
import org.spongepowered.common.launch.plugin.DummyPluginContainer;
import org.spongepowered.plugin.PluginContainer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class FilteredPluginContainerParameter implements ValueParameter<PluginContainer> {

    private final Map<String, PluginContainer> validPluginContainers = FilteredPluginContainerParameter.getValidContainers();

    private static Map<String, PluginContainer> getValidContainers() {
        return Launch.getInstance().getPluginManager().getPlugins()
                .stream()
                .filter(x -> !(x instanceof DummyPluginContainer))
                .collect(Collectors.toMap(x -> x.getMetadata().getId(), x -> x));
    }

    @Override
    public List<String> complete(final CommandContext context, String currentInput) {
        return ImmutableList.copyOf(this.validPluginContainers.keySet());
    }

    @Override
    public Optional<? extends PluginContainer> getValue(
            final Parameter.Key<? super PluginContainer> parameterKey,
            final ArgumentReader.Mutable reader,
            final CommandContext.Builder context) throws ArgumentParseException {
        final String id = reader.parseString();
        final PluginContainer pluginContainer = this.validPluginContainers.get(id);
        if (pluginContainer != null) {
            return Optional.of(pluginContainer);
        }
        throw reader.createException(TextComponent.of("Could not find valid plugin to refresh with ID \"" + id + "\""));
    }
}
