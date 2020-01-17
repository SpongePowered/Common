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

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.ai.goal.GoalExecutorTypes;
import org.spongepowered.api.entity.ai.goal.Goal;
import org.spongepowered.api.entity.ai.goal.GoalTypes;
import org.spongepowered.api.entity.ai.goal.AbstractGoal;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.entity.living.Human;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.util.Transform;
import org.spongepowered.api.world.Location;
import org.spongepowered.math.vector.Vector3d;

// Only newly spawned Humans will work. If you want prior ones, update the code in onSpawnEntity to lookup the creator and add the task :p.
@Plugin(id = "humanaitest", name = "Human AI Test", description = "Simple plugin used for AI and other tests", version = "0.0.0")
public class HumanAITest implements LoadableModule {

    @Inject private PluginContainer container;

    private final HumanAIListener listener = new HumanAIListener();

    @Override
    public void enable(MessageReceiver src) {
        Sponge.getEventManager().registerListeners(this.container, this.listener);
    }

    public static class HumanAIListener {

        @Listener
        public void onSpawnEntity(SpawnEntityEvent event, @First Player player) {
            if (player != null) {
                event.getEntities().stream().filter(entity -> entity instanceof Human).map(entity -> (Human) entity).forEach(human ->
                        human.getGoal(GoalExecutorTypes.NORMAL.get()).get().addGoal(0, new MoveSillyToPlayerGoal(player)));
            }
        }

        @Listener
        public void onInteractEntity(InteractEntityEvent.Secondary.MainHand event, @Root Player player) {
            if (event.getEntity() instanceof Human) {
                event.getEntity().addPassenger(player);
            }
        }
    }

    private static class MoveSillyToPlayerGoal extends AbstractGoal<Agent> {
        final Player player;

        protected MoveSillyToPlayerGoal(Player player) {
            super(GoalTypes.WANDER.get());
            this.player = player;
        }

        @Override
        public void start() {
        }

        @Override
        public boolean shouldUpdate() {
            return getOwner().isPresent();
        }

        @Override
        public void update() {
            // TODO Just some code to make them move comparative to us
            Agent agent = this.getOwner().orElse(null);
            if (agent != null) {
                agent.setRotation(new Vector3d(player.getRotation().getX() * 0.5f, player.getRotation().getY(), player.getRotation().getZ()));
                Vector3d position = player.getLocation().getPosition().add(new Vector3d(1f, 0f, 1f));

                agent.setLocation(Location.of(agent.getWorld(), position.getX(), agent.getLocation().getY(), position.getZ()));
            }
        }

        @Override
        public boolean continueUpdating() {
            return true;
        }

        @Override
        public void reset() {
        }

        @Override
        public boolean canRunConcurrentWith(Goal<Agent> other) {
            return false;
        }

        @Override
        public boolean canBeInterrupted() {
            return false;
        }
    }
}
