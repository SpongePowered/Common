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
package org.spongepowered.common.service.game.pagination;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapMaker;
import com.google.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.TextComponent;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.util.Nameable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

@Singleton
public final class SpongePaginationService implements PaginationService {

    static final class SourcePaginations {

        private final Map<UUID, ActivePagination> paginations = new ConcurrentHashMap<>();
        @Nullable private volatile UUID lastUuid;

        @Nullable public ActivePagination get(final UUID uuid) {
            return this.paginations.get(uuid);
        }

        public void put(final ActivePagination pagination) {
            synchronized (this.paginations) {
                this.paginations.put(pagination.getId(), pagination);
                this.lastUuid = pagination.getId();
            }
        }

        public Set<UUID> keys() {
            return this.paginations.keySet();
        }

        @Nullable
        public UUID getLastUuid() {
            return this.lastUuid;
        }
    }

    private final ConcurrentMap<Audience, SourcePaginations> activePaginations = new MapMaker().weakKeys().makeMap();

    // We have a second active pagination system because of the way Players are handled by the server.
    // As Players are recreated every time they die in game, just storing the player in a weak map will
    // cause the player to be removed form the map upon death. Thus, player paginations get redirected
    // through to this cache instead, which last for 10 minutes from last access.
    private final Cache<UUID, SourcePaginations> playerActivePaginations = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    @Override
    public PaginationList.Builder builder() {
        return new SpongePaginationBuilder(this);
    }

    @Nullable
    SourcePaginations getPaginationState(final Audience source, final boolean create) {
        if (source instanceof Player) {
            return this.getPaginationStateForPlayer((Player) source, create);
        }

        return this.getPaginationStateForNonPlayer(source, create);
    }

    @Nullable
    private SourcePaginations getPaginationStateForNonPlayer(final Audience source, final boolean create) {
        SourcePaginations ret = this.activePaginations.get(source);
        if (ret == null && create) {
            ret = new SourcePaginations();
            final SourcePaginations existing = this.activePaginations.putIfAbsent(source, ret);
            if (existing != null) {
                ret = existing;
            }
        }
        return ret;
    }

    @Nullable
    private SourcePaginations getPaginationStateForPlayer(final Player source, final boolean create) {
        return this.playerActivePaginations.get(source.getUniqueId(), k -> create ? new SourcePaginations() : null);
    }

    public Command.Parameterized createPaginationCommand() {
        final Parameter.Value<ActivePagination> paginationIdParameter = Parameter.builder(ActivePagination.class)
                .parser(new ActivePaginationParameter())
                .setKey("pagination-id")
                .build();

        final Command.Parameterized next = Command.builder()
                .setShortDescription(TextComponent.of("Go to the next page"))
                .setExecutor((context) -> {
                    context.requireOne(paginationIdParameter).nextPage();
                    return CommandResult.success();
                }).build();

        final Command.Parameterized prev = Command.builder()
                .setShortDescription(TextComponent.of("Go to the previous page"))
                .setExecutor((context) -> {
                    context.requireOne(paginationIdParameter).previousPage();
                    return CommandResult.success();
                }).build();

        final Parameter.Value<Integer> pageParameter = Parameter.integerNumber().setKey("page").build();

        final CommandExecutor pageExecutor = (context) -> {
            context.requireOne(paginationIdParameter).specificPage(context.requireOne(pageParameter));
            return CommandResult.success();
        };

        final Command.Parameterized page = Command.builder()
                .setShortDescription(TextComponent.of("Go to a specific page"))
                .parameter(pageParameter)
                .setExecutor(pageExecutor)
                .build();

        return Command.builder()
                .parameters(paginationIdParameter, Parameter.firstOf(pageParameter,
                        Parameter.subcommand(next, "next", "n"),
                        Parameter.subcommand(prev, "prev", "p", "previous"),
                        Parameter.subcommand(page, "page")))
                .child(page, "page")
                .setExecutor(page)
                .setShortDescription(TextComponent.of("Helper command for paginations occurring"))
                .build();
    }

    private final class ActivePaginationParameter implements ValueParameter<ActivePagination>, ValueCompleter.All {

        @Override
        public Optional<? extends ActivePagination> getValue(final Parameter.Key<? super ActivePagination> parameterKey,
                final ArgumentReader.Mutable reader,
                final CommandContext.Builder context) throws ArgumentParseException {
            final Audience source = context.getCause().getAudience();

            final SourcePaginations paginations = SpongePaginationService.this.getPaginationState(source, false);
            if (paginations == null) {
                final String name = source instanceof Nameable ? ((Nameable) source).getName() : source.toString();
                throw reader.createException(TextComponent.of(String.format("Source %s has no paginations!", name)));
            }

            final UUID id;
            final ArgumentReader.Immutable state = reader.getImmutable();
            try {
                id = UUID.fromString(reader.parseString());
            } catch (final IllegalArgumentException ex) {
                if (paginations.getLastUuid() != null) {
                    reader.setState(state);
                    return Optional.ofNullable(paginations.get(paginations.getLastUuid()));
                }
                throw reader.createException(TextComponent.of("Input was not a valid UUID!"));
            }
            final ActivePagination pagination = paginations.get(id);
            if (pagination == null) {
                throw reader.createException(TextComponent.of("No pagination registered for id " + id));
            }
            return Optional.ofNullable(paginations.get(id));
        }

        @Override
        public List<String> complete(final CommandContext context) {
            final Audience audience = context.getCause().getAudience();
            final SourcePaginations paginations = SpongePaginationService.this.getPaginationState(audience, false);
            if (paginations != null) {
                return ImmutableList.copyOf(Iterables.transform(paginations.keys(), Object::toString));
            }
            return ImmutableList.of();
        }
    }
}
