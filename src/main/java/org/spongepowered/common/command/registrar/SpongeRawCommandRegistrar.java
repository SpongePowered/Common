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
package org.spongepowered.common.command.registrar;

import com.google.common.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.exception.CommandPermissionException;
import org.spongepowered.api.command.manager.CommandFailedRegistrationException;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.command.registrar.CommandRegistrar;
import org.spongepowered.plugin.PluginContainer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * For use with {@link org.spongepowered.api.command.Command.Raw}
 */
public final class SpongeRawCommandRegistrar implements CommandRegistrar<Command.Raw> {

    private static final TypeToken<Command.Raw> COMMAND_TYPE = TypeToken.of(Command.Raw.class);
    private static final ResourceKey CATALOG_KEY = ResourceKey.sponge("raw");
    public static final SpongeRawCommandRegistrar INSTANCE = new SpongeRawCommandRegistrar();

    private final HashMap<CommandMapping, Command.Raw> commands = new HashMap<>();

    @Override
    @NonNull
    public TypeToken<Command.@NonNull Raw> handledType() {
        return SpongeRawCommandRegistrar.COMMAND_TYPE;
    }

    @Override
    public CommandMapping register(
            @NonNull final PluginContainer container,
            final Command.@NonNull Raw command,
            @NonNull final String primaryAlias,
            @NonNull final String @NonNull... secondaryAliases)
            throws CommandFailedRegistrationException {
        final CommandMapping mapping = Sponge.getCommandManager().registerAlias(
                this,
                container,
                command.commandTree(),
                primaryAlias,
                secondaryAliases
        );
        this.commands.put(mapping, command);
        return mapping;
    }

    @Override
    public CommandResult process(final CommandCause cause, final CommandMapping mapping, final String command, final String arguments) throws CommandException {
        final Command.Raw commandToExecute = this.commands.get(mapping);
        if (commandToExecute.canExecute(cause)) {
            return commandToExecute.process(cause, arguments);
        }
        throw new CommandPermissionException(TextComponent.of("You do not have permission to run /" + command));
    }

    @Override
    public List<String> suggestions(final CommandCause cause, final CommandMapping mapping, final String command, final String arguments) throws CommandException {
        final Command.Raw commandToExecute = this.commands.get(mapping);
        if (commandToExecute.canExecute(cause)) {
            return commandToExecute.getSuggestions(cause, arguments);
        }
        return Collections.emptyList();
    }

    @Override
    public Optional<Component> help(final CommandCause cause, final CommandMapping mapping) {
        final Command.Raw commandToExecute = this.commands.get(mapping);
        if (commandToExecute.canExecute(cause)) {
            return commandToExecute.getHelp(cause);
        }
        return Optional.empty();
    }

    @Override
    public boolean canExecute(final CommandCause cause, final CommandMapping mapping) {
        return this.commands.get(mapping).canExecute(cause);
    }

    @Override
    public void reset() {
        if (Sponge.getCommandManager().isResetting()) {
            this.commands.clear();
        }
    }

    @Override
    public ResourceKey getKey() {
        return SpongeRawCommandRegistrar.CATALOG_KEY;
    }

}
