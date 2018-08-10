/*
 * This file is part of SpongeAPI, licensed under the MIT License (MIT).
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
package org.spongepowered.common.command;

import static org.mockito.Mockito.mock;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.dispatcher.SimpleDispatcher;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.util.test.TestHooks;
import org.spongepowered.lwts.runner.LaunchWrapperTestRunner;

/**
 * Test for basic commandspec creation.
 */
@RunWith(LaunchWrapperTestRunner.class)
public class CommandSpecTest {
    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void testNoArgsFunctional() throws CommandException, NoSuchFieldException, IllegalAccessException {
        CommandManager cm = mock(CommandManager.class);
        TestHooks.setInstance("commandManager", cm);

        CommandSpec cmd = CommandSpec.builder()
                .executor((src, args) -> CommandResult.empty())
                .build();

        final SimpleDispatcher dispatcher = new SimpleDispatcher();
        dispatcher.register(cmd, "cmd");
        dispatcher.process(Mockito.mock(CommandSource.class), "cmd");
    }

    @Test
    public void testExecutorRequired() {
        this.expected.expect(NullPointerException.class);
        this.expected.expectMessage("An executor is required");
        CommandSpec.builder()
                .build();

    }
}
