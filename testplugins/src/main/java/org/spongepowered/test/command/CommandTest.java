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
package org.spongepowered.test.command;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommonParameters;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParameter;
import org.spongepowered.api.command.parameter.managed.standard.VariableValueParameters;
import org.spongepowered.api.command.selector.Selector;
import org.spongepowered.api.command.selector.SelectorTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.jvm.Plugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Plugin("commandtest")
public final class CommandTest {

    private final PluginContainer plugin;
    private final Logger logger;

    @Inject
    public CommandTest(final PluginContainer plugin, final Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    @Listener
    public void onRegisterSpongeCommand(final RegisterCommandEvent<Command.Parameterized> event) {
        final Parameter.Value<ServerPlayer> playerKey = Parameter.player().setKey("player")
                .setUsage(key -> "[any player]")
                .build();
        event.register(
                this.plugin,
                Command.builder()
                        .parameter(playerKey)
                        .setExecutor(context -> {
                            final ServerPlayer player = context.getOne(playerKey)
                                    .orElse(context.getCause().root() instanceof ServerPlayer ? ((ServerPlayer) context.getCause().root()) : null);
                            this.logger.info(player.getName());
                            return CommandResult.success();
                        })
                        .build(),
                "getplayer");

        final Parameter.Value<String> playerParameterKey = Parameter.string().setKey("name").optional().build();
        event.register(
                this.plugin,
                Command.builder()
                        .parameter(playerParameterKey)
                        .setExecutor(context -> {
                            final Optional<String> result = context.getOne(playerParameterKey);
                            final Collection<GameProfile> collection;
                            if (result.isPresent()) {
                                // check to see if the string matches
                                collection = Sponge.getGame().getServer().getUserManager()
                                        .streamOfMatches(result.get().toLowerCase(Locale.ROOT))
                                        .collect(Collectors.toList());
                            } else {
                                collection = Sponge.getGame().getServer().getUserManager()
                                        .streamAll()
                                        .collect(Collectors.toList());
                            }
                            collection.forEach(x -> this.logger.info(
                                    "GameProfile - UUID: {}, Name - {}", x.getUniqueId().toString(), x.getName().orElse("---")));
                            return CommandResult.success();
                        })
                        .build(),
                "checkuser"
        );

        final Parameter.Key<String> testKey = Parameter.key("testKey", String.class);
        final Parameter.Key<Component> requiredKey = Parameter.key("requiredKey", Component.class);
        event.register(
                this.plugin,
                Command.builder()
                        .flag(Flag.builder().alias("f").alias("flag").build())
                        .flag(Flag.builder().alias("t").alias("text").setParameter(Parameter.string().setKey(testKey).build()).build())
                        .parameter(Parameter.formattingCodeText().setKey(requiredKey).build())
                        .setExecutor(context -> {
                            context.sendMessage(Identity.nil(), Component.text(context.getFlagInvocationCount("flag")));
                            context.sendMessage(Identity.nil(), Component.text(context.getFlagInvocationCount("t")));
                            context.getAll(testKey).forEach(x -> context.sendMessage(Identity.nil(), Component.text(x)));
                            context.sendMessage(Identity.nil(), context.requireOne(requiredKey));
                            return CommandResult.success();
                        })
                        .build(),
                "flagtest"
        );

        event.register(
                this.plugin,
                Command.builder()
                        .setExecutor(x -> {
                            x.sendMessage(Identity.nil(), Component.text().content("Click Me")
                                    .clickEvent(SpongeComponents.executeCallback(ctx -> ctx.sendMessage(Identity.nil(), Component.text("Hello"))))
                                    .build()
                            );
                            return CommandResult.success();
                        }).build(),
                "testCallback"
        );

        event.register(
                this.plugin,
                Command.builder()
                        .setExecutor(x -> {
                            final Collection<Entity> collection = Selector.builder()
                                    .applySelectorType(SelectorTypes.ALL_ENTITIES.get())
                                    .entityType(EntityTypes.PLAYER.get(), false)
                                    .gameMode(GameModes.CREATIVE.get())
                                    .setLimit(1)
                                    .includeSelf()
                                    .build()
                                    .select(x.getCause());
                            for (final Entity entity : collection) {
                                x.sendMessage(Identity.nil(), Component.text(entity.toString()));
                            }
                            return CommandResult.success();
                        })
                        .build(),
                "testselector"
        );

        final Parameter.Key<ServerLocation> serverLocationKey = Parameter.key("serverLocation", ServerLocation.class);
        final Parameter.Value<ServerLocation> serverLocationParameter = Parameter.location().setKey(serverLocationKey).build();
        event.register(
                this.plugin,
                Command.builder()
                        .parameter(serverLocationParameter)
                        .setExecutor(x -> {
                            x.sendMessage(Identity.nil(), Component.text(x.requireOne(serverLocationKey).toString()));
                            return CommandResult.success();
                        })
                        .build(),
                "testlocation"
        );

        final Parameter.Key<ValueParameter<?>> commandParameterKey =
                Parameter.key("valueParameter", new TypeToken<ValueParameter<?>>() {});
        final TypeToken<ValueParameter<?>> typeToken = new TypeToken<ValueParameter<?>>() {};
        event.register(
                this.plugin,
                Command.builder()
                        .parameter(serverLocationParameter)
                        .parameter(
                                Parameter.registryElement(
                                        typeToken,
                                        commandContext -> Sponge.getGame().registries(),
                                        RegistryTypes.REGISTRY_KEYED_VALUE_PARAMETER,
                                        "sponge"
                                ).setKey(commandParameterKey).build())
                        .setExecutor(x -> {
                            x.sendMessage(Identity.nil(), Component.text(x.requireOne(serverLocationKey).toString()));
                            x.sendMessage(Identity.nil(), Component.text(x.requireOne(commandParameterKey).toString()));
                            return CommandResult.success();
                        })
                        .build(),
                "testcatalogcompletion"
        );

        final Parameter.Key<TestEnum> enumParameterKey = Parameter.key("enum", TestEnum.class);
        final Parameter.Key<String> stringKey = Parameter.key("stringKey", String.class);
        event.register(
                this.plugin,
                Command.builder()
                        .parameter(Parameter.enumValue(TestEnum.class).setKey(enumParameterKey).build())
                        .parameter(Parameter.string().setKey(stringKey).setSuggestions((context, currentInput) -> ImmutableList.of("bacon", "eggs", "spam")).build())
                        .setExecutor(x -> {
                            x.sendMessage(Identity.nil(), Component.text(x.getOne(enumParameterKey).orElse(TestEnum.ONE).name()));
                            return CommandResult.success();
                        })
                        .build(),
                "testenum"
        );


        final Parameter.Key<ResourceKey> resourceKeyKey = Parameter.key("rk", ResourceKey.class);
        event.register(
                this.plugin,
                Command.builder()
                        .parameter(Parameter.resourceKey().setKey(resourceKeyKey).build())
                        .setExecutor(x -> {
                            x.sendMessage(Identity.nil(), Component.text(x.requireOne(resourceKeyKey).getFormatted()));
                            return CommandResult.success();
                        })
                        .build(),
                "testrk"
        );

        final Parameter.Key<User> userKey = Parameter.key("user", User.class);
        event.register(
                this.plugin,
                Command.builder()
                    .parameter(Parameter.user().setKey(userKey).build())
                    .setExecutor(context -> {
                        context.sendMessage(Identity.nil(), Component.text(context.requireOne(userKey).getName()));
                        return CommandResult.success();
                    })
                    .build(),
                "getuser"
                );

        final Parameter.Key<String> stringLiteralKey = Parameter.key("literal", String.class);
        event.register(
                this.plugin,
                Command.builder()
                        .setExecutor(context -> {
                            context.sendMessage(Identity.nil(), Component.text("Collected literals: " + String.join(", ", context.getAll(stringLiteralKey))));
                            return CommandResult.success();
                        })
                        .setTerminal(true)
                        .parameter(
                                Parameter.firstOfBuilder(Parameter.literal(String.class, "1", "1").setKey(stringLiteralKey).build())
                                    .or(Parameter.literal(String.class, "2", "2").setKey(stringLiteralKey).build())
                                    .terminal()
                                    .build()
                        )
                        .parameter(Parameter.seqBuilder(Parameter.literal(String.class, "3", "3").setKey(stringLiteralKey).build())
                                .then(Parameter.literal(String.class, "4", "4").setKey(stringLiteralKey).build())
                                .terminal()
                                .build())
                        .parameter(Parameter.literal(String.class, "5", "5").optional().setKey(stringLiteralKey).build())
                        .parameter(Parameter.literal(String.class, "6", "6").setKey(stringLiteralKey).build())
                        .build(),
                "testnesting");

        final Parameter.Value<SystemSubject> systemSubjectValue = Parameter.builder(SystemSubject.class)
                .setKey("systemsubject")
                .parser(VariableValueParameters.literalBuilder(SystemSubject.class).setLiteral(Collections.singleton("-"))
                        .setReturnValue(Sponge::getSystemSubject).build())
                .build();
        event.register(
                this.plugin,
                Command.builder()
                        .parameter(Parameter.firstOf(
                                systemSubjectValue,
                                CommonParameters.PLAYER
                        ))
                        .parameter(Parameter.remainingJoinedStrings().setKey(stringKey).build())
                        .setExecutor(context -> {
                            final Audience audience;
                            final String name;
                            if (context.hasAny(systemSubjectValue)) {
                                audience = context.requireOne(systemSubjectValue);
                                name = "Console";
                            } else {
                                final ServerPlayer player = context.requireOne(CommonParameters.PLAYER);
                                name = player.getName();
                                audience = player;
                            }
                            final String message = context.requireOne(stringKey);
                            context.sendMessage(Identity.nil(), Component.text("To " + name + "> " + message));
                            final Object root = context.getCause().root();
                            final Identity identity = root instanceof ServerPlayer ? ((ServerPlayer) root).identity() : Identity.nil();
                            audience.sendMessage(identity, Component.text("From " + name + "> " + message));
                            return CommandResult.success();
                        })
                        .build(),
                    "testmessage"
                );

        final Command.Builder builder = Command.builder();

        final ValueCompleter stringValueCompleter = (c, s) -> s.isEmpty() ? Collections.singletonList("x") : Arrays.asList(s, s + "bar", "foo_" + s);
//        final ValueCompleter stringValueCompleter = null;

        final Parameter.Value<String> r_opt = Parameter.remainingJoinedStrings().setKey("r_def").optional().build();
        final Parameter.Value<String> r_req = Parameter.remainingJoinedStrings().setKey("r_req").setSuggestions(stringValueCompleter).build();
        final Parameter.Value<String> opt1 = Parameter.string().optional().setKey("opt1").build();
        final Parameter.Value<String> opt2 = Parameter.string().optional().setKey("opt2").build();
        final Parameter.Value<String> topt = Parameter.string().optional().setKey("topt").terminal().build();
        final Parameter.Value<String> req1 = Parameter.string().setKey("req1").setSuggestions(stringValueCompleter).build();
        final Parameter.Value<String> req2 = Parameter.string().setKey("req2").build();
        final Parameter.Value<Boolean> lit1 = Parameter.literal(Boolean.class, true, "lit1").setKey("lit1").build();
        final Parameter.Value<Boolean> lit2 = Parameter.literal(Boolean.class, true, "lit2").setKey("lit2").build();
        final Parameter optSeq_lit_req1 = Parameter.seqBuilder(lit1).then(req1).optional().build();
        final Parameter optSeq_lit_req2 = Parameter.seqBuilder(lit2).then(req2).optional().build();
        final Parameter seq_req_2 = Parameter.seqBuilder(req1).then(req2).build();
        final Parameter seq_opt_2 = Parameter.seqBuilder(opt1).then(opt2).build();
        final Parameter seq_opt_2_req = Parameter.seqBuilder(opt1).then(opt2).then(req1).build();

        // <req1>
        builder.child(Command.builder().setExecutor(context -> CommandTest.printParameters(context, req1))
                        .parameter(req1).build(),"required");

        // subcommand|<r_def>
        builder.child(Command.builder().setExecutor(context -> CommandTest.printParameters(context, r_opt))
                        .parameter(r_opt)
                        .child(Command.builder().setExecutor(c -> CommandTest.printParameters(c, r_opt)).build(), "subcommand")
                        .build(),
                "optional_or_subcmd");


        // https://bugs.mojang.com/browse/MC-165562 usage does not show up after a space if there are no completions

        // [def1] <r_req>
        // TODO missing executed command when only providing a single value
        builder.child(Command.builder().setExecutor(context -> CommandTest.printParameters(context, opt1, r_req))
                .parameters(opt1, r_req).build(), "optional_r_required");

        // [opt1] [opt2] <r_req>
        // TODO missing executed command when only providing a single value
        builder.child(Command.builder().setExecutor(context -> CommandTest.printParameters(context, opt1, opt2, r_req))
                .parameters(opt1, opt2, r_req).build(), "optional_optional_required");

        // [opt1] [opt2]
        // TODO some redundancy in generated nodes because opt1 node can terminate early
        builder.child(Command.builder().setExecutor(context -> CommandTest.printParameters(context, opt1, opt2))
                .parameters(opt1, opt2).build(), "optional_optional");

        // [opt1] [literal <req1>]
        // TODO completion does not include req1 when opt1/literal is ambigous
        builder.child(Command.builder().setExecutor(c -> CommandTest.printParameters(c, opt1, lit1, req1))
                .parameters(opt1, optSeq_lit_req1).build(), "optional_optsequence_literal_required");

        // [literal <req1>] [literal2 <req2>]
        // TODO sequences are not optional
        builder.child(Command.builder().setExecutor(c -> CommandTest.printParameters(c, lit1, req1, lit2, req2))
                .parameters(optSeq_lit_req1, optSeq_lit_req2).build(), "opt_sequence_2_literal_required");

        // <<req1> <req2>>
        builder.child(Command.builder().setExecutor(c -> CommandTest.printParameters(c, req1, req2))
                .parameters(seq_req_2).build(), "seq_required_required");

        // <[opt1] [opt2]>
        // TODO some redundancy in generated nodes because opt1 node can terminate early
        builder.child(Command.builder().setExecutor(c -> CommandTest.printParameters(c, opt1, opt2))
                .parameters(seq_opt_2).build(), "seq_optional_optional");

        // <[opt1] [opt2] <req>>
        builder.child(Command.builder().setExecutor(c -> CommandTest.printParameters(c, opt1, opt2, req1))
                .parameters(seq_opt_2_req).build(), "seq_optional_optional_required");

        // [opt1] <req> [opt2]
        // TODO some redundancy in generated nodes because req1 node can terminate early
        builder.child(Command.builder().setExecutor(c -> CommandTest.printParameters(c, opt1, req1, opt2))
                .parameters(opt1, req1, opt2).build(), "optional_required_optional");

        // [opt1] [topt] !terminal
        // or
        // [opt1] [topt] <req1>
        builder.child(Command.builder().setExecutor(c -> CommandTest.printParameters(c, opt1, topt, req1))
                .parameters(opt1, topt, req1).build(), "optional_toptional_optional");

        event.register(this.plugin, builder.build(), "testcommand", "testcmd");

        // Adapted from https://github.com/SpongePowered/Sponge/issues/3238#issuecomment-750456173

        final Command.Parameterized firstSub = Command.builder()
                .parameter(CommonParameters.BOOLEAN)
                .setExecutor(c -> {
                    c.sendMessage(Identity.nil(), Component.text("first"));
                    return CommandResult.success();
                })
                .build();
        final Command.Parameterized secondSub = Command.builder()
                .parameter(CommonParameters.BOOLEAN)
                .setExecutor(c -> {
                    c.sendMessage(Identity.nil(), Component.text("second"));
                    return CommandResult.success();
                })
                .build();
        final Command.Parameterized parent = Command.builder()
                .setExecutor(c -> {
                    c.sendMessage(Identity.nil(), Component.text("parent"));
                    return CommandResult.success();
                })
                .parameters(CommonParameters.WORLD)
                .parameters(Parameter.firstOf(
                        Parameter.subcommand(firstSub, "first"),
                        Parameter.subcommand(secondSub, "second")
                ))
                .setTerminal(true)
                .build();

        event.register(this.plugin, parent, "testterminal");

        // exceptions

        event.register(this.plugin, Command.builder()
                      .setShortDescription(Component.text("test throwing execptions"))
                      .child(Command.builder()
                            .setExecutor(ctx -> {
                                throw new CommandException(Component.text("Exit via exception"));
                            })
                            .build(), "exception")
                      .child(Command.builder()
                            .setExecutor(ctx -> {
                                return CommandResult.error(Component.text("Exit via failed result"));
                            }).build(), "failedresult")
                      .build(), "testfailure");
    }

    @Listener
    public void onRegisterRawSpongeCommand(final RegisterCommandEvent<Command.Raw> event) {
        event.register(this.plugin, new RawCommandTest(), "rawcommandtest");
    }

    private static CommandResult printParameters(final CommandContext context, final Parameter.Value<?>... params)
    {
        for (final Parameter.Value<?> param : params) {
            final Object paramValue = context.getOne(param).map(Object::toString).orElse("missing");
            final String paramUsage = param.getUsage(context.getCause());
            context.sendMessage(Identity.nil(), Component.text(paramUsage + ": " + paramValue));
        }
        // TODO usage starts with "command" - comes from SpogneParameterizedCommand#getCachedDispatcher
        context.getExecutedCommand().ifPresent(cmd -> context.sendMessage(Identity.nil(), cmd.getUsage(context.getCause()).color(NamedTextColor.GRAY)));
        return CommandResult.success();
    }

    public enum TestEnum {
        ONE,
        TWO,
        THREE
    }
}
