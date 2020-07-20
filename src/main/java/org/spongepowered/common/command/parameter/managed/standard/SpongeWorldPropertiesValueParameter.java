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
package org.spongepowered.common.command.parameter.managed.standard;

import net.kyori.adventure.text.TextComponent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.command.brigadier.argument.CatalogedArgumentParser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SpongeWorldPropertiesValueParameter extends CatalogedArgumentParser<WorldProperties> {

    private static final ResourceKey RESOURCE_KEY = ResourceKey.sponge("world_properties");

    @Override
    @NonNull
    public ResourceKey getKey() {
        return SpongeWorldPropertiesValueParameter.RESOURCE_KEY;
    }

    @Override
    @NonNull
    public List<String> complete(@NonNull final CommandContext context) {
        return SpongeCommon.getGame().getServer().getWorldManager().getAllProperties().stream().map(WorldProperties::getDirectoryName)
                .collect(Collectors.toList());
    }

    @Override
    @NonNull
    public Optional<? extends WorldProperties> getValue(
            final Parameter.@NonNull Key<? super WorldProperties> parameterKey,
            final ArgumentReader.@NonNull Mutable reader,
            final CommandContext.@NonNull Builder context) throws ArgumentParseException {

        final String name = reader.parseString();
        final Optional<WorldProperties> worldProperties = SpongeWorldPropertiesValueParameter.getWorldProperties(name);

        if (worldProperties.isPresent()) {
            return worldProperties;
        }

        throw reader.createException(TextComponent.of("Could not find world with identifier \"" + name + "\""));
    }

    static Optional<WorldProperties> getWorldProperties(final String name) {
        try {
            final UUID uuid = UUID.fromString(name);
            return SpongeCommon.getGame().getServer().getWorldManager().getProperties(uuid);
        } catch (final Exception ignored) {
            return SpongeCommon.getGame().getServer().getWorldManager().getProperties(name);
        }
    }

}
