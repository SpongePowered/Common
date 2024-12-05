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
package org.spongepowered.test.entity;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.Command.Parameterized;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.display.BillboardTypes;
import org.spongepowered.api.entity.display.DisplayEntity;
import org.spongepowered.api.entity.display.TextAlignments;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.registry.DefaultedRegistryReference;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.util.Transform;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.imaginary.Quaterniond;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("displayentitytest")
public class DisplayEntityTest {

    private final PluginContainer plugin;

    @Inject
    public DisplayEntityTest(final PluginContainer plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onRegisterCommand(final RegisterCommandEvent<Parameterized> event) {
        event.register(this.plugin, Command.builder()
                .executor(ctx -> {
                    ctx.cause().first(ServerPlayer.class).ifPresent(player -> {
                        player.nearbyEntities(200, e -> e instanceof DisplayEntity).forEach(Entity::remove);
                        var pos = player.serverLocation().position();
                        var forwardDir = player.headDirection().normalize();
                        forwardDir = new Vector3d(forwardDir.x(), 0, forwardDir.z());
                        var centerPos = pos.add(forwardDir.x() * 4, 0, forwardDir.z() * 4);

                        final int col0 = -5;
                        final int col1 = -4;
                        final int col2 = -3;
                        final int col3 = 0;
                        final int col4 = 2;
                        final int col5 = 3;
                        final int col6 = 5;
                        final int col7 = 6;
                        var textDisplay = spawnEntity(player.world(), EntityTypes.TEXT_DISPLAY, centerPos, forwardDir, -4, -1);
                        textDisplay.offer(Keys.DISPLAY_NAME, Component.text("DisplayEntityTest").color(NamedTextColor.GOLD));
                        textDisplay.offer(Keys.SEE_THROUGH_BLOCKS, true);
                        textDisplay.offer(Keys.TEXT_ALIGNMENT, TextAlignments.LEFT.get());
                        textDisplay.offer(Keys.TEXT_BACKGROUND_COLOR, Color.GRAY);

                        textDisplay = spawnEntity(player.world(), EntityTypes.TEXT_DISPLAY, centerPos, forwardDir, col0, 0);
                        textDisplay.offer(Keys.DISPLAY_NAME, Component.text("Fixed"));
                        textDisplay.offer(Keys.BILLBOARD_TYPE, BillboardTypes.FIXED.get());

                        var itemDisplay = spawnEntity(player.world(), EntityTypes.ITEM_DISPLAY, centerPos, forwardDir, col2, 0);
                        itemDisplay.offer(Keys.ITEM_STACK_SNAPSHOT, ItemStack.of(ItemTypes.NETHERITE_INGOT).createSnapshot());
                        itemDisplay.offer(Keys.BILLBOARD_TYPE, BillboardTypes.FIXED.get());

                        textDisplay = spawnEntity(player.world(), EntityTypes.TEXT_DISPLAY, centerPos, forwardDir, col1, 0);
                        textDisplay.offer(Keys.DISPLAY_NAME, Component.text("default\nlight"));

                        textDisplay = spawnEntity(player.world(), EntityTypes.TEXT_DISPLAY, centerPos, forwardDir, col0, 1);
                        textDisplay.offer(Keys.DISPLAY_NAME, Component.text("Center"));
                        textDisplay.offer(Keys.BILLBOARD_TYPE, BillboardTypes.CENTER.get());

                        itemDisplay = spawnEntity(player.world(), EntityTypes.ITEM_DISPLAY, centerPos, forwardDir, col2, 1);
                        itemDisplay.offer(Keys.ITEM_STACK_SNAPSHOT, ItemStack.of(ItemTypes.DIAMOND).createSnapshot());
                        itemDisplay.offer(Keys.BLOCK_LIGHT, 15);
                        itemDisplay.offer(Keys.SKY_LIGHT, 15);
                        itemDisplay.offer(Keys.BILLBOARD_TYPE, BillboardTypes.CENTER.get());

                        textDisplay = spawnEntity(player.world(), EntityTypes.TEXT_DISPLAY, centerPos, forwardDir, col1, 1);
                        textDisplay.offer(Keys.DISPLAY_NAME, Component.text("Full\nlight"));

                        textDisplay = spawnEntity(player.world(), EntityTypes.TEXT_DISPLAY, centerPos, forwardDir, col0, 2);
                        textDisplay.offer(Keys.DISPLAY_NAME, Component.text("Horizontal"));
                        textDisplay.offer(Keys.BILLBOARD_TYPE, BillboardTypes.HORIZONTAL.get());

                        itemDisplay = spawnEntity(player.world(), EntityTypes.ITEM_DISPLAY, centerPos, forwardDir, col2, 2);
                        itemDisplay.offer(Keys.ITEM_STACK_SNAPSHOT, ItemStack.of(ItemTypes.IRON_INGOT).createSnapshot());
                        itemDisplay.offer(Keys.BLOCK_LIGHT, 15);
                        itemDisplay.offer(Keys.SKY_LIGHT, 0);
                        itemDisplay.offer(Keys.BILLBOARD_TYPE, BillboardTypes.HORIZONTAL.get());

                        textDisplay = spawnEntity(player.world(), EntityTypes.TEXT_DISPLAY, centerPos, forwardDir, col1, 2);
                        textDisplay.offer(Keys.DISPLAY_NAME, Component.text("block\nlight"));


                        textDisplay = spawnEntity(player.world(), EntityTypes.TEXT_DISPLAY, centerPos, forwardDir, col0, 3);
                        textDisplay.offer(Keys.DISPLAY_NAME, Component.text("Vertical"));
                        textDisplay.offer(Keys.BILLBOARD_TYPE, BillboardTypes.VERTICAL.get());

                        itemDisplay = spawnEntity(player.world(), EntityTypes.ITEM_DISPLAY, centerPos, forwardDir, col2, 3);
                        itemDisplay.offer(Keys.ITEM_STACK_SNAPSHOT, ItemStack.of(ItemTypes.GOLD_INGOT).createSnapshot());
                        itemDisplay.offer(Keys.BLOCK_LIGHT, 0);
                        itemDisplay.offer(Keys.SKY_LIGHT, 15);
                        itemDisplay.offer(Keys.BILLBOARD_TYPE, BillboardTypes.VERTICAL.get());

                        textDisplay = spawnEntity(player.world(), EntityTypes.TEXT_DISPLAY, centerPos, forwardDir, col1, 3);
                        textDisplay.offer(Keys.DISPLAY_NAME, Component.text("sky\nlight"));

                        textDisplay = spawnEntity(player.world(), EntityTypes.TEXT_DISPLAY, centerPos, forwardDir, col0, 4);
                        textDisplay.offer(Keys.DISPLAY_NAME, Component.text("Low\nViewRange"));

                        itemDisplay = spawnEntity(player.world(), EntityTypes.ITEM_DISPLAY, centerPos, forwardDir, col2, 4);
                        itemDisplay.offer(Keys.ITEM_STACK_SNAPSHOT, ItemStack.of(ItemTypes.EMERALD).createSnapshot());
                        itemDisplay.offer(Keys.VIEW_RANGE, 0.02); // 1 is supposed to be view range for fireballs in vanilla, 0.02 seems to be around a block


                        textDisplay = spawnEntity(player.world(), EntityTypes.TEXT_DISPLAY, centerPos, forwardDir, col3, 0);
                        textDisplay.offer(Keys.DISPLAY_NAME, Component.text("Translation"));
                        textDisplay.offer(Keys.BILLBOARD_TYPE, BillboardTypes.FIXED.get());

                        var blockDisplay = spawnEntity(player.world(), EntityTypes.BLOCK_DISPLAY, centerPos, forwardDir, col4, 0);
                        blockDisplay.offer(Keys.BLOCK_STATE, BlockTypes.NETHERITE_BLOCK.get().defaultState());
                        final Vector3d blockCenterOffset = Vector3d.ONE.mul(-0.5);
                        blockDisplay.offer(Keys.TRANSFORM,
                                Transform.of(blockCenterOffset.add(Vector3d.RIGHT.mul(-0.2).add(Vector3d.FORWARD.mul(-0.1)))));

                        blockDisplay = spawnEntity(player.world(), EntityTypes.BLOCK_DISPLAY, centerPos, forwardDir, col4, 0);
                        blockDisplay.offer(Keys.BLOCK_STATE, BlockTypes.NETHERITE_BLOCK.get().defaultState());
                        blockDisplay.offer(Keys.TRANSFORM, Transform.of(blockCenterOffset));

                        textDisplay = spawnEntity(player.world(), EntityTypes.TEXT_DISPLAY, centerPos, forwardDir, col3, 1);
                        textDisplay.offer(Keys.DISPLAY_NAME, Component.text("& Scale"));
                        textDisplay.offer(Keys.BILLBOARD_TYPE, BillboardTypes.FIXED.get());

                        blockDisplay = spawnEntity(player.world(), EntityTypes.BLOCK_DISPLAY, centerPos, forwardDir, col4, 1);
                        blockDisplay.offer(Keys.BLOCK_STATE, BlockTypes.DIAMOND_BLOCK.get().defaultState());
                        blockDisplay.offer(Keys.TRANSFORM, Transform.of(blockCenterOffset.mul(0.75)).scale(Vector3d.ONE.mul(0.75)));

                        blockDisplay = spawnEntity(player.world(), EntityTypes.BLOCK_DISPLAY, centerPos, forwardDir, col4, 1);
                        blockDisplay.offer(Keys.BLOCK_STATE, BlockTypes.DIAMOND_BLOCK.get().defaultState());
                        blockDisplay.offer(Keys.TRANSFORM,
                                Transform.of(blockCenterOffset.mul(0.75).add(Vector3d.RIGHT.mul(-0.25)), Vector3d.ZERO,
                                        Vector3d.ONE.mul(0.25)));

                        textDisplay = spawnEntity(player.world(), EntityTypes.TEXT_DISPLAY, centerPos, forwardDir, col3, 2);
                        textDisplay.offer(Keys.DISPLAY_NAME, Component.text("& Rotation"));
                        textDisplay.offer(Keys.BILLBOARD_TYPE, BillboardTypes.FIXED.get());


                        var rotY = Quaterniond.fromAxesAnglesDeg(0, 45, 0);
                        var rotatedOffset = rotY.rotate(blockCenterOffset);
                        var transform = Transform.of(rotatedOffset).rotate(rotY);

                        blockDisplay = spawnEntity(player.world(), EntityTypes.BLOCK_DISPLAY, centerPos, forwardDir, col4, 2);
                        blockDisplay.offer(Keys.BLOCK_STATE, BlockTypes.GREEN_STAINED_GLASS.get().defaultState());
                        blockDisplay.offer(Keys.TRANSFORM, transform);

                        var rotX = Quaterniond.fromAxesAnglesDeg(45, 0, 0);
                        rotatedOffset = rotX.rotate(blockCenterOffset);
                        transform = Transform.of(rotatedOffset).rotate(rotX);

                        blockDisplay = spawnEntity(player.world(), EntityTypes.BLOCK_DISPLAY, centerPos, forwardDir, col4, 2);
                        blockDisplay.offer(Keys.BLOCK_STATE, BlockTypes.RED_STAINED_GLASS.get().defaultState());
                        blockDisplay.offer(Keys.TRANSFORM, transform);

                        var rotZ = Quaterniond.fromAxesAnglesDeg(0, 0, 45);
                        rotatedOffset = rotZ.rotate(blockCenterOffset);
                        transform = Transform.of(rotatedOffset).rotate(rotZ);

                        blockDisplay = spawnEntity(player.world(), EntityTypes.BLOCK_DISPLAY, centerPos, forwardDir, col4, 2);
                        blockDisplay.offer(Keys.BLOCK_STATE, BlockTypes.BLUE_STAINED_GLASS.get().defaultState());
                        blockDisplay.offer(Keys.TRANSFORM, transform);

                        var rot = Quaterniond.fromAngleDegAxis(-54.736, 1, 0, -1);
                        rotatedOffset = rot.rotate(blockCenterOffset);
                        transform = Transform.of(rotatedOffset).rotate(rot);

                        blockDisplay = spawnEntity(player.world(), EntityTypes.BLOCK_DISPLAY, centerPos, forwardDir, col4, 2);
                        blockDisplay.offer(Keys.BLOCK_STATE, BlockTypes.WHITE_STAINED_GLASS.get().defaultState());
                        blockDisplay.offer(Keys.TRANSFORM, transform);

                        // Interpolations...
                        blockDisplay = createEntity(player.world(), EntityTypes.BLOCK_DISPLAY, centerPos, forwardDir, col5, 0);
                        blockDisplay.offer(Keys.BLOCK_STATE, BlockTypes.GOLD_BLOCK.get().defaultState());
                        blockDisplay.offer(Keys.TRANSFORM, Transform.of(blockCenterOffset)); // set initial value before spawning
                        blockDisplay.offer(Keys.SHADOW_RADIUS, 2d); // set initial value before spawning
                        blockDisplay.offer(Keys.SHADOW_STRENGTH, 5d); // set initial value before spawning
                        player.world().spawnEntity(blockDisplay);
                        blockDisplay.offer(Keys.TRANSFORM, Transform.of(Vector3d.UNIT_Y.mul(3).add(blockCenterOffset.mul(-1)), Vector3d.from(0, 180, 0)));
                        blockDisplay.offer(Keys.SHADOW_RADIUS, 1d); // set initial value before spawning
                        blockDisplay.offer(Keys.SHADOW_STRENGTH, 0d); // set initial value before spawning
                        blockDisplay.offer(Keys.INTERPOLATION_DURATION, Ticks.of(20));
                        blockDisplay.offer(Keys.INTERPOLATION_DELAY, Ticks.of(20));

                        blockDisplay = createEntity(player.world(), EntityTypes.BLOCK_DISPLAY, centerPos, forwardDir, col6, 0);
                        blockDisplay.offer(Keys.BLOCK_STATE, BlockTypes.OBSIDIAN.get().defaultState());
                        blockDisplay.offer(Keys.TRANSFORM, Transform.of(blockCenterOffset)); // set initial value before spawning
                        blockDisplay.offer(Keys.SHADOW_RADIUS, 2d); // set initial value before spawning
                        blockDisplay.offer(Keys.SHADOW_STRENGTH, 5d); // set initial value before spawning
                        player.world().spawnEntity(blockDisplay);
                        blockDisplay.offer(Keys.TELEPORT_DURATION, Ticks.of(20));
                        blockDisplay.setLocation(((ServerLocation) blockDisplay.location().add(0.0, 4.0, 0.0)));
                        blockDisplay.setScale(new Vector3d(1.0,5.0,1.0));


                        textDisplay = createEntity(player.world(), EntityTypes.TEXT_DISPLAY, centerPos, forwardDir, col7, 0);
                        textDisplay.offer(Keys.DISPLAY_NAME, Component.text("Look at these interpolations").color(NamedTextColor.RED));
                        textDisplay.offer(Keys.LINE_WIDTH, 100);
                        textDisplay.offer(Keys.SEE_THROUGH_BLOCKS, true);
                        textDisplay.offer(Keys.TEXT_BACKGROUND_COLOR, Color.BLACK); // set initial value before spawning
                        player.world().spawnEntity(textDisplay);
                        textDisplay.offer(Keys.TEXT_BACKGROUND_COLOR, Color.WHITE);
                        // TODO force interpolator start value update?
                        textDisplay.offer(Keys.INTERPOLATION_DURATION, Ticks.of(20));
                        textDisplay.offer(Keys.INTERPOLATION_DELAY, Ticks.of(20));

                        // TODO interpolate text opacity?

                    });
                    return CommandResult.success();
                })
                .build(), "testdisplayentities");
    }


    public <E extends Entity> E createEntity(final ServerWorld world,
            final DefaultedRegistryReference<EntityType<E>> type,
            final Vector3d centerPos,
            final Vector3d forwardDir,
            final int x,
            final int y) {
        var rightDir = forwardDir.cross(Vector3d.UNIT_Y).normalize();
        final Vector3d entityPos = centerPos.add(rightDir.mul(x)).add(Vector3d.UNIT_Y.mul(y));
        final E entity = world.createEntity(type, entityPos);
        entity.lookAt(entityPos.add(forwardDir.mul(-1)));
        return entity;
    }
    public <E extends Entity> E spawnEntity(final ServerWorld world,
            final DefaultedRegistryReference<EntityType<E>> type,
            final Vector3d centerPos,
            final Vector3d forwardDir,
            final int x,
            final int y) {
        var rightDir = forwardDir.cross(Vector3d.UNIT_Y).normalize();
        final Vector3d entityPos = centerPos.add(rightDir.mul(x)).add(Vector3d.UNIT_Y.mul(y));
        final E entity = world.createEntity(type, entityPos);
        entity.lookAt(entityPos.add(forwardDir.mul(-1)));
        world.spawnEntity(entity);
        return entity;
    }

}
