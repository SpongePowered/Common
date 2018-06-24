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
package org.spongepowered.test;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.WorldArchetypes;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

// Allows for testing the rename of a world.
// Includes commands to load, unload, rename and create worlds.
@Plugin(id = "world-rename-test", name = "World Rename Test", description = "Tests renaming of worlds", version = "0.0.0")
public class WorldRenameTest {

    static final Text worldKey = Text.of("world");
    static final Text newNameKey = Text.of("new name");

    @Listener
    public void onInit(GameInitializationEvent event) {
        Sponge.getCommandManager().register(this,
                CommandSpec.builder()
                    .arguments(GenericArguments.world(worldKey))
                    .executor((source, context) -> {
                        Sponge.getServer().unloadWorld(Sponge.getServer().getWorld(context.<WorldProperties>getOne(worldKey).get().getUniqueId())
                                .orElseThrow(() -> new CommandException(Text.of(TextColors.RED, "That world doesn't seem to be loaded."))));
                        return CommandResult.success();
                    }).build(), "unloadworld");

        Sponge.getCommandManager().register(this,
                CommandSpec.builder()
                        .arguments(new WorldParameter())
                        .executor((source, context) -> {
                            Sponge.getServer().unloadWorld(Sponge.getServer().getWorld(context.<WorldProperties>getOne(worldKey).get().getUniqueId())
                                    .orElseThrow(() -> new CommandException(Text.of(TextColors.RED, "That world doesn't seem to be loaded."))));
                            return CommandResult.success();
                        }).build(), "loadworld");

        Sponge.getCommandManager().register(this,
                CommandSpec.builder()
                        .arguments(new WorldParameter(), GenericArguments.string(newNameKey))
                        .executor((source, context) -> {
                            // Name clashes should be handled by the impl.
                            WorldProperties newProperties = Sponge.getServer()
                                    .renameWorld(context.<WorldProperties>getOne(worldKey).get(), context.<String>getOne(newNameKey).get())
                                    .orElseThrow(() -> new CommandException(Text.of(TextColors.RED, "The world was not renamed.")));

                            source.sendMessage(Text.of("The world was renamed to " + newProperties.getWorldName()));
                            return CommandResult.success();
                        }).build(), "renameworld");

        Sponge.getCommandManager().register(this,
                CommandSpec.builder()
                        .arguments(GenericArguments.string(newNameKey))
                        .executor((source, context) -> {
                            try {
                                Sponge.getServer().createWorldProperties(context.<String>getOne(newNameKey).get(), WorldArchetypes.OVERWORLD);
                            } catch (IOException e) {
                                throw new CommandException(Text.of(TextColors.RED, "Could not create the world."), e);
                            }

                            source.sendMessage(Text.of("The world was created"));
                            return CommandResult.success();
                        }).build(), "createworld");

        Sponge.getCommandManager().register(this,
                CommandSpec.builder()
                        .arguments(new WorldParameter(x -> true))
                        .executor((source, context) -> {
                            WorldProperties wp = context.<WorldProperties>getOne(worldKey).get();
                            source.sendMessage(Text.of("World: ", wp.getWorldName(), " - UUID: ", wp.getUniqueId(), " - Dim ID:",
                                    wp.getAdditionalProperties().getInt(DataQuery.of("SpongeData", "dimensionId")).map(Object::toString)
                                            .orElse("unknown")));
                            return CommandResult.success();
                        }).build(), "worldinfo");

    }

    @NonnullByDefault
    static class WorldParameter extends CommandElement {

        private final Predicate<WorldProperties> worldPropertiesPredicate;

        WorldParameter() {
            this(wp -> !Sponge.getServer().getWorld(wp.getUniqueId()).isPresent());
        }

        WorldParameter(Predicate<WorldProperties> worldPropertiesPredicate) {
            super(WorldRenameTest.worldKey);
            this.worldPropertiesPredicate = worldPropertiesPredicate;
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            WorldProperties worldProperties = Sponge.getServer().getWorldProperties(args.next())
                    .orElseThrow(() -> args.createError(Text.of("Cannot find the world")));
            if (this.worldPropertiesPredicate.test(worldProperties)) {
                throw args.createError(Text.of("The world is currently loaded."));
            }

            return worldProperties;
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            Stream<String> tabComplete = Sponge.getServer().getAllWorldProperties().stream()
                    .filter(this.worldPropertiesPredicate)
                    .map(x -> x.getWorldName().toLowerCase());

            try {
                final String input = args.peek().toLowerCase();
                return tabComplete.filter(x -> x.startsWith(input)).collect(Collectors.toList());
            } catch (ArgumentParseException ignored) {
                return tabComplete.collect(Collectors.toList());
            }
        }
    }
}
