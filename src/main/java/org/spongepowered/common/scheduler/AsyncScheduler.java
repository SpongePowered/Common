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
package org.spongepowered.common.scheduler;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.Level;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.PrettyPrinter;

import java.util.concurrent.*;

public class AsyncScheduler extends SpongeScheduler {
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    private final ScheduledExecutorService scheduler;

    public AsyncScheduler() {
        super("A");
        final ScheduledThreadPoolExecutor scheduler =
                new ScheduledThreadPoolExecutor(NCPU, new ThreadFactoryBuilder()
                        .setNameFormat("Sponge-AsyncScheduler-%d")
                        .build()
                );
        scheduler.setRemoveOnCancelPolicy(true);
        this.scheduler = scheduler;
    }

    @Override
    public ScheduledFuture<?> scheduleAtTick(Runnable command, long ticks) {
        return scheduler.schedule(command,
                ticks * TICK_DURATION_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> scheduleAtTime(Runnable command, long nanos) {
        return scheduler.schedule(command, nanos, TimeUnit.NANOSECONDS);
    }

    public <T> CompletableFuture<T> submit(final Callable<T> callable) {
        final CompletableFuture<T> ret = new CompletableFuture<>();
        execute(() -> {
            try {
                ret.complete(callable.call());
            } catch (final Throwable e) {
                ret.completeExceptionally(e);
            }
        });
        return ret;
    }
    @Override
    public void close() {
        final ExecutorService scheduler = this.scheduler;
        if (scheduler.isTerminated()) {
            return;
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                new PrettyPrinter()
                        .add("Sponge async scheduler failed to shut down in 5 seconds! Tasks that may have been active:")
                        .addWithIndices(activeTasks())
                        .add()
                        .add("We will now attempt immediate shutdown.")
                        .log(SpongeCommon.logger(), Level.WARN);

                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            SpongeCommon.logger().error("The async scheduler was interrupted while awaiting shutdown!");

            Thread.currentThread().interrupt();
        }
    }

}
