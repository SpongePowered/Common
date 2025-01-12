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
package org.spongepowered.test.volumestream;

import com.google.inject.Inject;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.standard.VariableValueParameters;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.rotation.Rotation;
import org.spongepowered.api.util.transformation.Transformation;
import org.spongepowered.api.world.biome.Biome;
import org.spongepowered.api.world.schematic.Schematic;
import org.spongepowered.api.world.volume.archetype.ArchetypeVolume;
import org.spongepowered.api.world.volume.stream.StreamOptions;
import org.spongepowered.api.world.volume.stream.VolumeApplicators;
import org.spongepowered.api.world.volume.stream.VolumeCollectors;
import org.spongepowered.api.world.volume.stream.VolumePositionTranslators;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;
import org.spongepowered.test.LoadableModule;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Plugin("volumestreamtest")
public final class VolumeStreamTest implements LoadableModule {

    private static final String FILE_ENDING = ".schem";
    public static final TextColor SAVE = TextColor.color(0x856C);
    private static final TextColor GREEN = TextColor.color(0x6CA9FF);

    @Inject private PluginContainer plugin;
    @Inject private Logger logger;
    @Inject @ConfigDir(sharedRoot = false) private Path config;
    private Path schematicsDir;

    private final CopyPastaListener listener = new CopyPastaListener();
    private static final Map<UUID, PlayerData> player_data = new HashMap<>();

    private static PlayerData get(final Player pl) {
        PlayerData data = VolumeStreamTest.player_data.get(pl.uniqueId());
        if (data == null) {
            data = new PlayerData(pl.uniqueId());
            VolumeStreamTest.player_data.put(pl.uniqueId(), data);
        }
        return data;
    }

    @Override
    public void enable(final CommandContext ctx) {
        Sponge.eventManager().registerListeners(this.plugin, this.listener);
    }

    @Listener
    public void onShutdown(final StoppingEngineEvent<@NonNull Server> serverShutdown) {
        this.logger.log(Level.ERROR, "Clearing player clipboards");
        VolumeStreamTest.player_data.clear();
    }

    @Listener
    public void onGamePreInitialization(final RegisterCommandEvent<Command.Parameterized> event) throws IOException, CommandException {
        this.schematicsDir = this.config.resolve("schematics");
        Files.createDirectories(this.config);
        final Parameter.Value<Biome> biomeKey = Parameter.registryElement(
            TypeToken.get(Biome.class),
            List.of(
                VariableValueParameters.RegistryEntryBuilder.WORLD_FROM_LOCATABLE_HOLDER_PROVIDER ,
                VariableValueParameters.RegistryEntryBuilder.WORLD_FROM_CAUSE_HOLDER_PROVIDER
            ),
            RegistryTypes.BIOME,
            "minecraft"
        ).key("format")
            .build();
        event.register(
            this.plugin,
            Command.builder()
                .shortDescription(Component.text("Sets the biome in a selected region"))
                .permission(this.plugin.metadata().id() + ".command.setbiome")
                .addParameter(biomeKey)
                .executor(src -> {
                    if (!(src.cause().root() instanceof ServerPlayer)) {
                        src.sendMessage(Identity.nil(), Component.text("Player only.", NamedTextColor.RED));
                        return CommandResult.success();
                    }
                    final ServerPlayer player = (ServerPlayer) src.cause().root();
                    final PlayerData data = VolumeStreamTest.get(player);
                    if (data.getPos1() == null || data.getPos2() == null) {
                        player.sendMessage(
                            Identity.nil(),
                            Component.text("You must set both positions before copying", NamedTextColor.RED)
                        );
                        return CommandResult.success();
                    }
                    final Vector3i min = data.getPos1().min(data.getPos2());
                    final Vector3i max = data.getPos1().max(data.getPos2());
                    final Biome target = src.requireOne(biomeKey);
                    player.world().biomeStream(min, max, StreamOptions.forceLoadedAndCopied())
                        .map((world, biome, x, y, z) -> target)
                        .apply(VolumeCollectors.of(
                            player.world(),
                            VolumePositionTranslators.identity(),
                            VolumeApplicators.applyBiomes()
                        ));
                    return CommandResult.success();
                })
            .build(),
            "setBiome"
        );
        event.register(
            this.plugin,
            Command.builder()
                .shortDescription(Component.text("Copies a region of the world to your clipboard"))
                .permission(this.plugin.metadata().id() + ".command.copy")
                .executor(src -> {
                    if (!(src.cause().root() instanceof Player)) {
                        src.sendMessage(Identity.nil(), Component.text("Player only.", NamedTextColor.RED));
                        return CommandResult.success();
                    }
                    final Player player = (Player) src.cause().root();
                    final PlayerData data = VolumeStreamTest.get(player);
                    if (data.getPos1() == null || data.getPos2() == null) {
                        player.sendMessage(
                            Identity.nil(),
                            Component.text("You must set both positions before copying", NamedTextColor.RED)
                        );
                        return CommandResult.success();
                    }
                    final Vector3i min = data.getPos1().min(data.getPos2());
                    final Vector3i max = data.getPos1().max(data.getPos2());
                    data.setOrigin(player.blockPosition());
                    final ArchetypeVolume archetypeVolume = player.world().createArchetypeVolume(
                        min, max, player.blockPosition());
                    data.setClipboard(archetypeVolume);
                    player.sendMessage(Identity.nil(), Component.text("Saved to clipboard.", VolumeStreamTest.GREEN));
                    return CommandResult.success();
                }).build(),
            "copy"
        );
        event.register(
            this.plugin,
            Command.builder()
                .shortDescription(Component.text("Pastes your clipboard to where you are standing"))
                .permission(this.plugin.metadata().id() + ".command.paste")
                .executor(src -> {
                    if (!(src.cause().root() instanceof ServerPlayer)) {
                        src.sendMessage(Identity.nil(), Component.text("Player only.", NamedTextColor.RED));
                        return CommandResult.success();
                    }
                    final ServerPlayer player = (ServerPlayer) src.cause().root();
                    final PlayerData data = VolumeStreamTest.get(player);
                    final ArchetypeVolume volume = data.getClipboard();
                    if (volume == null) {
                        player.sendMessage(
                            Identity.nil(),
                            Component.text("You must copy something before pasting", NamedTextColor.RED)
                        );
                        return CommandResult.success();
                    }
                    try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
                        frame.pushCause(this.plugin);
                        volume.applyToWorld(player.world(), player.blockPosition(), SpawnTypes.PLACEMENT::get);
                    }
                    src.sendMessage(
                        Identity.nil(), Component.text("Pasted clipboard into world.", VolumeStreamTest.GREEN));
                    return CommandResult.success();
                }).build(),
            "paste"
        );

        final Parameter.Value<String> fileName = Parameter.string().key("fileName").build();
        event.register(
            this.plugin,
            Command.builder()
                .shortDescription(Component.text("Pastes your clipboard to where you are standing"))
                .permission(this.plugin.metadata().id() + ".command.paste")
                .addParameter(fileName)
                .executor(src -> {
                    if (!(src.cause().root() instanceof ServerPlayer)) {
                        src.sendMessage(Identity.nil(), Component.text("Player only.", NamedTextColor.RED));
                        return CommandResult.success();
                    }
                    final String file = src.requireOne(fileName);
                    final Path desiredFilePath = this.schematicsDir.resolve(file + VolumeStreamTest.FILE_ENDING);
                    if (Files.exists(desiredFilePath)) {
                        throw new CommandException(Component.text(file + " already exists, please delete the file first", NamedTextColor.RED));
                    }
                    if (Files.isDirectory(desiredFilePath)) {
                        throw new CommandException(Component.text(file + "is a directory, please use a file name", NamedTextColor.RED));
                    }

                    final ServerPlayer player = (ServerPlayer) src.cause().root();
                    final PlayerData data = VolumeStreamTest.get(player);
                    final ArchetypeVolume volume = data.getClipboard();
                    if (volume == null) {
                        player.sendMessage(
                            Identity.nil(),
                            Component.text("You must copy something before pasting", NamedTextColor.RED)
                        );
                        return CommandResult.success();
                    }
                    final Schematic schematic = Schematic.builder()
                        .volume(data.getClipboard())
                        .metaValue(Schematic.METADATA_AUTHOR, player.name())
                        .metaValue(Schematic.METADATA_NAME, file)
                        .build();
                    final DataContainer schematicData = Sponge.dataManager().translator(Schematic.class)
                        .orElseThrow(
                            () -> new IllegalStateException("Sponge doesn't have a DataTranslator for Schematics!"))
                        .translate(schematic);

                    try {
                        final Path output = Files.createFile(desiredFilePath);
                        DataFormats.NBT.get().writeTo(
                            new GZIPOutputStream(Files.newOutputStream(output)), schematicData);
                        player.sendMessage(
                            Identity.nil(),
                            Component.text("Saved schematic to " + output.toAbsolutePath(), VolumeStreamTest.SAVE)
                        );
                    } catch (final Exception e) {
                        e.printStackTrace();
                        final StringWriter writer = new StringWriter();
                        e.printStackTrace(new PrintWriter(writer));
                        final Component errorText = Component.text(writer.toString().replace("\t", "    ")
                            .replace("\r\n", "\n")
                            .replace("\r", "\n")
                        );

                        final TextComponent text = Component.text(
                            "Error saving schematic: " + e.getMessage(), NamedTextColor.RED)
                            .hoverEvent(HoverEvent.showText(errorText));

                        return CommandResult.builder()
                            .error(text).build();
                    }
                    return CommandResult.success();
                }).build(),
            "save"
        );
        event.register(
            this.plugin,
            Command.builder()
                .shortDescription(Component.text("Load a schematic from file"))
                .permission(this.plugin.metadata().id() + ".command.load")
                .addParameter(fileName)
                .executor(src -> {
                    if (!(src.cause().root() instanceof ServerPlayer)) {
                        src.sendMessage(Identity.nil(), Component.text("Player only.", NamedTextColor.RED));
                        return CommandResult.success();
                    }
                    final ServerPlayer player = (ServerPlayer) src.cause().root();
                    final String file = src.requireOne(fileName);
                    final Path desiredFilePath = this.schematicsDir.resolve(file + VolumeStreamTest.FILE_ENDING);
                    if (!Files.isRegularFile(desiredFilePath)) {
                        throw new CommandException(Component.text("File " + file + " was not a normal schemaic file"));
                    }
                    final Schematic schematic;
                    final DataContainer schematicContainer;
                    try (final GZIPInputStream stream = new GZIPInputStream(Files.newInputStream(desiredFilePath))) {
                        schematicContainer = DataFormats.NBT.get().readFrom(stream);
                    } catch (IOException e) {
                        e.printStackTrace();
                        final StringWriter writer = new StringWriter();
                        e.printStackTrace(new PrintWriter(writer));
                        final Component errorText = Component.text(writer.toString().replace("\t", "    ")
                            .replace("\r\n", "\n")
                            .replace("\r", "\n")
                        );

                        final TextComponent text = Component.text(
                            "Error loading schematic: " + e.getMessage(), NamedTextColor.RED)
                            .hoverEvent(HoverEvent.showText(errorText));

                        return CommandResult.builder()
                            .error(text).build();
                    }
                    schematic = Sponge.dataManager().translator(Schematic.class)
                        .orElseThrow(() -> new IllegalStateException("Expected a DataTranslator for a Schematic"))
                        .translate(schematicContainer);
                    src.sendMessage(Identity.nil(), Component.text("Loaded schematic from " + file, TextColor.color(0x003434)));
                    final PlayerData data = VolumeStreamTest.get(player);
                    data.setClipboard(schematic);
                    data.setOrigin(player.blockPosition());
                    return CommandResult.success();
                })
                .build(),
            "load"
        );

        final Parameter.Value<Rotation> rotation = Parameter.registryElement(
            TypeToken.get(Rotation.class), RegistryTypes.ROTATION)
            .key("rotation")
            .build();
        event.register(this.plugin,
            Command
                .builder()
                .shortDescription(Component.text("Rotate clipboard"))
                .permission(this.plugin.metadata().id() + ".command.rotate")
                .addParameter(rotation)
                .executor(src -> {
                    if (!(src.cause().root() instanceof ServerPlayer)) {
                        src.sendMessage(Identity.nil(), Component.text("Player only.", NamedTextColor.RED));
                        return CommandResult.success();
                    }
                    final ServerPlayer player = (ServerPlayer) src.cause().root();
                    final Rotation desiredRotation = src.requireOne(rotation);
                    final Schematic schematic;
                    final PlayerData data = VolumeStreamTest.get(player);
                    if (data.clipboard == null) {
                        throw new CommandException(Component.text("Load a clipboard first before trying to rotate it"));
                    }
                    final ArchetypeVolume newClipboard = data.clipboard.transform(Transformation.builder()
                        .origin(data.clipboard.min().toDouble().add(data.clipboard.size().toDouble().div(2)))
                        .rotate(desiredRotation)
                        .build());
                    src.sendMessage(Identity.nil(), Component.text("Rotated clipboard " + desiredRotation.angle().degrees() + " degrees"));
                    data.setClipboard(newClipboard);
                    return CommandResult.success();
                })
                .build(),
            "rotate"
        );
    }


    static class CopyPastaListener {

        public static final TextColor TEAL = TextColor.color(0x008080);

        @Listener
        private void onInteract(final InteractBlockEvent.Primary event, @Root final Player player) {
            event.context().get(EventContextKeys.USED_ITEM).ifPresent(snapshot -> {
                final BlockSnapshot block = event.block();
                if (snapshot.type().equals(ItemTypes.WOODEN_AXE.get()) && block != BlockSnapshot.empty()) {
                    VolumeStreamTest.get(player).setPos1(block.position());
                    player.sendMessage(
                        Component.text("Position 1 set to " + block.position(), CopyPastaListener.TEAL));

                    if (event instanceof Cancellable) {
                        ((Cancellable) event).setCancelled(true);
                    }
                }
            });
        }

        @Listener
        public void onInteract(final InteractBlockEvent.Secondary.Pre event, @Root final Player player) {
            event.context().get(EventContextKeys.USED_ITEM).ifPresent(snapshot -> {
                final BlockSnapshot block = event.block();
                if (snapshot.type().equals(ItemTypes.WOODEN_AXE.get()) && block != BlockSnapshot.empty()) {
                    VolumeStreamTest.get(player).setPos2(block.position());
                    player.sendMessage(
                        Component.text("Position 2 set to " + block.position(), CopyPastaListener.TEAL));
                    event.setCancelled(true);
                }
            });
        }
    }

    public static class PlayerData {

        private final UUID uid;
        private Vector3i pos1;
        private Vector3i pos2;
        private Vector3i origin;
        private ArchetypeVolume clipboard;

        public PlayerData(final UUID uid) {
            this.uid = uid;
        }

        public UUID getUid() {
            return this.uid;
        }

        public Vector3i getPos1() {
            return this.pos1;
        }

        public void setPos1(final Vector3i pos) {
            this.pos1 = pos;
        }

        public Vector3i getPos2() {
            return this.pos2;
        }

        public void setPos2(final Vector3i pos) {
            this.pos2 = pos;
        }

        public ArchetypeVolume getClipboard() {
            return this.clipboard;
        }

        public void setClipboard(final ArchetypeVolume volume) {
            this.clipboard = volume;
        }

        public Vector3i getOrigin() {
            return this.origin;
        }

        public void setOrigin(final Vector3i origin) {
            this.origin = origin;
        }
    }
}
