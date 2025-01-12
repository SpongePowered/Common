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
package org.spongepowered.test.projectile;

import com.google.inject.Inject;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.carrier.Dispenser;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.projectile.source.ProjectileSource;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;
import org.spongepowered.test.LoadableModule;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

@Plugin("projectiletest")
public class ProjectileTest implements LoadableModule {

    @Inject private PluginContainer plugin;
    private ProjectileTestListener listeners;

    @Override
    public void enable(final CommandContext ctx) {
        this.listeners = new ProjectileTestListener();
        Sponge.eventManager().registerListeners(this.plugin, this.listeners);
    }

    @Listener
    private void registerCommand(final RegisterCommandEvent<Command.Parameterized> event) {
        final Parameter.Value<EntityType<@NonNull ?>> entityTypeParameter =
                Parameter.registryElement(
                        new TypeToken<EntityType<? extends Entity>>() {},
                        (ctx) -> Sponge.game(),
                        RegistryTypes.ENTITY_TYPE,
                        "minecraft",
                        "sponge")
                    .key("type")
                    .build();
        final Parameter.Value<Boolean> targetParameter = Parameter.bool().key("target").optional().build();
        final Command.Parameterized launchCommand = Command.builder()
                .addParameters(entityTypeParameter, targetParameter)
                .executor(context -> {
                    final Player player = context.cause().first(Player.class)
                            .orElseThrow(() -> new CommandException(Component.text("Only a player can execute this command")));
                    final EntityType<?> entityType = context.requireOne(entityTypeParameter);
                    final Optional<Projectile> launched;
                    if (context.one(targetParameter).orElse(false)) {
                        final Collection<? extends Entity> nearbyEntities = player.nearbyEntities(10,
                                entity -> entity instanceof Living && entity != player);
                        if (nearbyEntities.isEmpty()) {
                            return CommandResult.error(Component.text("No entity to target nearby"));
                        }
                        final Entity target = nearbyEntities.iterator().next();
                        launched = player.launchProjectileTo((EntityType<Projectile>) entityType, target);
                        if (launched.isPresent()) {
                            player.sendMessage(Identity.nil(), Component.text("Launched projectile to " + RegistryTypes.ENTITY_TYPE.keyFor(Sponge.game(), target.type()).asString()));
                            return CommandResult.success();
                        }
                    } else {
                        launched = player.launchProjectile((EntityType<Projectile>) entityType);
                        if (launched.isPresent()) {
                            player.sendMessage(Identity.nil(), Component.text("Launched projectile"));
                            return CommandResult.success();
                        }
                    }

                    throw new CommandException(Component.text("Could not launch projectile"));
                })
                .build();
        event.register(this.plugin, launchCommand, "launch");

        final Command.Parameterized launchToMeCommand = Command.builder()
                .addParameter(entityTypeParameter)
                .executor(context -> {
                    final Player player = context.cause().first(Player.class)
                            .orElseThrow(() -> new CommandException(Component.text("Only a player can execute this command")));
                    final Collection<? extends ProjectileSource> nearbyProjectileSources =
                            (Collection<? extends ProjectileSource>) player.nearbyEntities(10, entity -> entity instanceof ProjectileSource);
                    if (nearbyProjectileSources.isEmpty()) {
                        return CommandResult.error(Component.text("No projectile source nearby"));
                    }

                    final ProjectileSource projectileSource = nearbyProjectileSources.iterator().next();
                    final EntityType<?> entityType = context.requireOne(entityTypeParameter);
                    final Optional<? extends Projectile> launched = projectileSource.launchProjectileTo((EntityType<Projectile>) entityType, player);
                    final EntityType<?> type = ((Entity) projectileSource).type();
                    if (launched.isPresent()) {
                        final EntityType<?> launchedType = launched.get().type();
                        player.sendMessage(Identity.nil(), Component.text()
                                .append(Component.text("You made a ")).append(Component.text(RegistryTypes.ENTITY_TYPE.keyFor(Sponge.game(), type).asString()))
                                .append(Component.text(" shoot a ")).append(Component.text(RegistryTypes.ENTITY_TYPE.keyFor(Sponge.game(), launchedType).asString()))
                                .append(Component.text(" at you")).build()
                        );
                        return CommandResult.success();
                    }

                    throw new CommandException(Component.text()
                            .append(Component.text("Could not launch a ")).append(Component.text(RegistryTypes.ENTITY_TYPE.keyFor(Sponge.game(), type).asString()))
                            .append(Component.text(" from a ")).append(Component.text(RegistryTypes.ENTITY_TYPE.keyFor(Sponge.game(), entityType).asString()))
                            .append(Component.text(" at you")).build());
                })
                .build();
        event.register(this.plugin, launchToMeCommand, "launchtome");

        final Parameter.Value<ServerLocation> dispenserParameter = Parameter.location()
                .key("dispenser")
                .build();
        final Command.Parameterized triggerDispenserCommand = Command.builder()
                .addParameters(dispenserParameter, entityTypeParameter)
                .executor(context -> {
                    final Player player = context.cause().first(Player.class)
                            .orElseThrow(() -> new CommandException(Component.text("Only a player can execute this command")));
                    final BlockEntity dispenser = context.requireOne(dispenserParameter).blockEntity().orElse(null);
                    if (dispenser == null) {
                        return CommandResult.error(Component.text("Could not find dispenser"));
                    }
                    final EntityType<?> entityType = context.requireOne(entityTypeParameter);
                    final Optional<? extends Projectile> launched = ((Dispenser) dispenser).launchProjectile((EntityType<Projectile>) entityType);
                    if (launched.isPresent()) {
                        launched.get().offer(Keys.SHOOTER, player);
                        player.sendMessage(Identity.nil(), Component.text()
                                .append(Component.text("The dispenser launched a ")).append(Component.text(RegistryTypes.ENTITY_TYPE.keyFor(Sponge.game(), launched.get().type()).asString()))
                                .build()
                        );
                        return CommandResult.success();
                    }

                    return CommandResult.error(Component.text()
                            .append(Component.text("Could not make the dispenser launch a ")).append(Component.text(RegistryTypes.ENTITY_TYPE.keyFor(Sponge.game(), entityType).asString()))
                            .build());
                })
                .build();
        event.register(this.plugin, triggerDispenserCommand, "triggerdispenser");
    }


    static class ProjectileTestListener {

        private Queue<EntityType<? extends Projectile>> projectileTypes = new LinkedList<>();

        public ProjectileTestListener() {
            this.projectileTypes.add(EntityTypes.SPECTRAL_ARROW.get());
            this.projectileTypes.add(EntityTypes.ARROW.get());
            this.projectileTypes.add(EntityTypes.EGG.get());
            this.projectileTypes.add(EntityTypes.SMALL_FIREBALL.get());
            this.projectileTypes.add(EntityTypes.FIREWORK_ROCKET.get());
            this.projectileTypes.add(EntityTypes.SNOWBALL.get());
            this.projectileTypes.add(EntityTypes.EXPERIENCE_BOTTLE.get());
            this.projectileTypes.add(EntityTypes.ENDER_PEARL.get());
            this.projectileTypes.add(EntityTypes.FIREBALL.get());
            this.projectileTypes.add(EntityTypes.WITHER_SKULL.get());
            this.projectileTypes.add(EntityTypes.EYE_OF_ENDER.get());
            //             this.projectileTypes.add(EntityTypes.FISHING_BOBBER.get());
            this.projectileTypes.add(EntityTypes.POTION.get());
            this.projectileTypes.add(EntityTypes.LLAMA_SPIT.get());
            this.projectileTypes.add(EntityTypes.DRAGON_FIREBALL.get());
            this.projectileTypes.add(EntityTypes.SHULKER_BULLET.get());
        }

        @Listener
        private void onClickBlock(final InteractBlockEvent.Secondary.Pre event, @First final ServerPlayer player) {
            final Vector3d interactionPoint = event.interactionPoint();
            final ServerWorld world = player.world();
            final EntityType<? extends Projectile> nextType = this.projectileTypes.poll();
            this.projectileTypes.offer(nextType);
            final Optional<? extends BlockEntity> blockEntity = world.blockEntity(interactionPoint.toInt());
            if (blockEntity.isPresent() && blockEntity.get() instanceof Dispenser) {
                ((Dispenser) blockEntity.get()).launchProjectile(nextType);
            } else {
                player.launchProjectile(nextType);
            }
            event.setCancelled(true);
            player.sendMessage(Identity.nil(), Component.text(RegistryTypes.ENTITY_TYPE.keyFor(Sponge.game(), nextType).toString()));
        }

    }

}
