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
package org.spongepowered.common.data.contextual;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.DataPerspective;
import org.spongepowered.api.data.DataPerspectiveResolver;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.world.World;
import org.spongepowered.common.data.SpongeDataManager;

import java.util.Objects;

public final class ContextualDataDelegate extends ContextualDataOwner<DataPerspective> {

    public ContextualDataDelegate(final DataPerspective perspective) {
        super(perspective);
    }

    @Override
    public PerspectiveContainer<?, ?> createDataPerception(final DataPerspective perspective) {
        return this.perspectives.computeIfAbsent(perspective, p -> {
            if (p instanceof final Entity entity) {
                return new EntityPerspectiveContainer(this, entity);
            } else if (p instanceof final Team team) {
                return new TeamPerspectiveContainer(this, team);
            } else if (p instanceof final World<?, ?> world) {
                return new WorldPerspectiveContainer(this, world);
            }
            throw new UnsupportedOperationException();
        });
    }

    private static abstract class AbstractPerspectiveContainer<P extends DataPerspective> extends PerspectiveContainer<ContextualDataDelegate, P> {

        protected AbstractPerspectiveContainer(final PerspectiveType perspectiveType, final ContextualDataDelegate holder, final P perspective) {
            super(perspectiveType, holder, perspective);
        }

        @Override
        protected final <E> void offer(final PerspectiveType perspectiveType, final DataPerspectiveResolver<Value<E>, E> resolver, final E value) {
            if (Objects.equals(this.activeValues.put(resolver.key(), value), value)) {
                return;
            }

            for (final DataPerspective perspective : this.holder.owner.perceives()) {
                ((ContextualData) perspective).createDataPerception(this.perspective).offer(PerspectiveType.TEAM, this.holder.owner, resolver, value);
            }
        }

        @Override
        protected void remove(PerspectiveType perspectiveType, DataPerspectiveResolver<Value<?>, ?> resolver) {
            this.activeValues.remove(resolver.key());

            for (final DataPerspective perspective : this.holder.owner.perceives()) {
                final @Nullable PerspectiveContainer<?, ?> container = ((ContextualData) perspective).dataPerception(this.perspective);
                if (container != null) {
                    container.remove(PerspectiveType.TEAM, this.holder.owner, resolver);
                }
            }
        }

        @Override
        protected void addPerspective(final DataPerspective perspective) {
            final PerspectiveContainer<?, ?> container = ((ContextualData) perspective).createDataPerception(this.perspective);
            this.activeValues.forEach((k, v) -> {
                final DataPerspectiveResolver<Value<Object>, Object> resolver =
                        Objects.requireNonNull(SpongeDataManager.getDataPerspectiveResolverRegistry().get((Key) k));

                container.offer(PerspectiveType.TEAM, this.holder.owner, resolver, v);
            });
        }

        @Override
        protected void removePerspective(final DataPerspective perspective) {
            final @Nullable PerspectiveContainer<?, ?> container = ((ContextualData) perspective).dataPerception(this.perspective);
            if (container != null) {
                this.activeValues.forEach((k, v) -> {
                    final DataPerspectiveResolver<Value<?>, ?> resolver =
                            Objects.requireNonNull(SpongeDataManager.getDataPerspectiveResolverRegistry().get((Key) k));

                    container.remove(PerspectiveType.TEAM, this.holder.owner, resolver);
                });
            }
        }
    }

    private static final class EntityPerspectiveContainer extends AbstractPerspectiveContainer<Entity> {

        private EntityPerspectiveContainer(final ContextualDataDelegate holder, final Entity entity) {
            super(PerspectiveType.ENTITY, holder, entity);
        }
    }

    private static final class TeamPerspectiveContainer extends AbstractPerspectiveContainer<Team> {

        private TeamPerspectiveContainer(final ContextualDataDelegate holder, final Team team) {
            super(PerspectiveType.TEAM, holder, team);
        }
    }

    private static final class WorldPerspectiveContainer extends AbstractPerspectiveContainer<World<?, ?>> {

        private WorldPerspectiveContainer(final ContextualDataDelegate holder, final World<?, ?> world) {
            super(PerspectiveType.WORLD, holder, world);
        }
    }
}
