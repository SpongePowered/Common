package org.spongepowered.common.world;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldBorder;
import org.spongepowered.common.scheduler.SpongeScheduler;
import org.spongepowered.common.world.storage.SpongeChunkLayout;

import java.util.function.Consumer;

import javax.annotation.Nullable;

public class SpongeChunkPreGenerate implements WorldBorder.ChunkPreGenerate {

    private static final int TICK_INTERVAL = 10;
    private static final float DEFAULT_TICK_PERCENT = 0.15f;
    private final World world;
    private final Vector3d center;
    private final double diameter;
    @Nullable private Object plugin = null;
    @Nullable private Logger logger = null;
    private int tickInterval = TICK_INTERVAL;
    private int chunkCount = 0;
    private float tickPercent = DEFAULT_TICK_PERCENT;

    public SpongeChunkPreGenerate(World world, Vector3d center, double diameter) {
        this.world = world;
        this.center = center;
        this.diameter = diameter;
    }

    @Override
    public WorldBorder.ChunkPreGenerate owner(Object plugin) {
        checkNotNull(plugin, "plugin");
        this.plugin = plugin;
        return this;
    }

    @Override
    public WorldBorder.ChunkPreGenerate logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    @Override
    public WorldBorder.ChunkPreGenerate tickInterval(int tickInterval) {
        checkArgument(tickInterval > 0, "tickInterval must be greater than zero");
        this.tickInterval = tickInterval;
        return this;
    }

    @Override
    public WorldBorder.ChunkPreGenerate chunksPerTick(int chunkCount) {
        this.chunkCount = chunkCount;
        return this;
    }

    @Override
    public WorldBorder.ChunkPreGenerate tickPercentLimit(float tickPercent) {
        checkArgument(tickPercent <= 1, "tickPercent must be smaller or equal to 1");
        this.tickPercent = tickPercent;
        return this;
    }

    @Override
    public Task start() {
        checkNotNull(this.plugin, "owner not set");
        checkArgument(this.chunkCount > 0 || this.tickPercent > 0, "Must use at least one of \"chunks per tick\" or \"tick percent limit\"");
        return Task.builder().name(toString())
                .execute(new ChunkPreGenerator(this.world, this.center, this.diameter, this.chunkCount, this.tickPercent, this.logger))
                .intervalTicks(this.tickInterval).submit(this.plugin);
    }

    @Override
    public WorldBorder.ChunkPreGenerate reset() {
        this.plugin = null;
        this.logger = null;
        this.tickInterval = 0;
        this.chunkCount = 0;
        this.tickPercent = DEFAULT_TICK_PERCENT;
        return this;
    }

    @Override public String toString() {
        return "SpongeChunkPreGen{" +
                "center=" + this.center +
                ", diameter=" + this.diameter +
                ", plugin=" + this.plugin +
                ", world=" + this.world +
                ", tickInterval=" + this.tickInterval +
                ", chunkCount=" + this.chunkCount +
                ", tickPercent=" + this.tickPercent +
                '}';
    }

    private static class ChunkPreGenerator implements Consumer<Task> {

        private static final Vector3i[] OFFSETS = {
                Vector3i.UNIT_X,
                Vector3i.UNIT_Z,
                Vector3i.UNIT_X.negate(),
                Vector3i.UNIT_Z.negate()
        };
        private static final String TIME_FORMAT = "s's 'S'ms'";
        private final World world;
        private final int chunkRadius;
        private final int chunkCount;
        private final float tickPercent;
        private final long tickTimeLimit;
        @Nullable private final Logger logger;
        private Vector3i currentPosition;
        private int currentLayerIndex;
        private int currentLayerSize;
        private int currentIndexInLayer;
        private int totalCount;
        private long totalTime;

        public ChunkPreGenerator(World world, Vector3d center, double diameter, int chunkCount, float tickPercent, @Nullable Logger logger) {
            this.world = world;
            this.chunkRadius = GenericMath.floor(diameter / 32);
            this.chunkCount = chunkCount;
            this.tickPercent = tickPercent;
            this.logger = logger;
            this.tickTimeLimit = Math.round(SpongeScheduler.getInstance().getPreferredTickInterval() * tickPercent);
            this.currentPosition = SpongeChunkLayout.instance.toChunk(center.toInt()).get();
            this.currentLayerIndex = 0;
            this.currentLayerSize = 0;
            this.currentIndexInLayer = 0;
            this.totalCount = 0;
            this.totalTime = 0;
        }

        @Override
        public void accept(Task task) {
            final long startTime = System.currentTimeMillis();
            int count = 0;
            do {
                this.world.loadChunk(nextChunkPosition(), true).ifPresent(Chunk::unloadChunk);
            } while (hasNextChunkPosition() && checkChunkCount(++count) && checkTickTime(System.currentTimeMillis() - startTime));
            if (this.logger != null) {
                this.totalCount += count;
                final long deltaTime = System.currentTimeMillis() - startTime;
                this.totalTime += deltaTime;
                this.logger.info("Generated {} chunks in {}, {}% complete", count,
                        DurationFormatUtils.formatDuration(deltaTime, TIME_FORMAT, false),
                        Math.round((float) this.totalCount / (this.chunkRadius * this.chunkRadius * 4) * 100));
            }
            if (!hasNextChunkPosition()) {
                if (this.logger != null) {
                    this.logger.info("Done! Generated a total of {} chunks in {}", this.totalCount,
                            DurationFormatUtils.formatDuration(this.totalTime, TIME_FORMAT, false));
                }
                task.cancel();
            }
        }

        private boolean hasNextChunkPosition() {
            return this.currentLayerIndex <= this.chunkRadius;
        }

        private Vector3i nextChunkPosition() {
            final Vector3i nextPosition = this.currentPosition;
            if (++this.currentIndexInLayer >= this.currentLayerSize * 4) {
                this.currentLayerIndex++;
                this.currentLayerSize += 2;
                this.currentIndexInLayer = 0;
                this.currentPosition = this.currentPosition.sub(Vector3i.UNIT_Z).sub(Vector3i.UNIT_X);
            }
            this.currentPosition = this.currentPosition.add(OFFSETS[this.currentIndexInLayer / this.currentLayerSize]);
            return nextPosition;
        }

        private boolean checkChunkCount(int count) {
            return this.chunkCount <= 0 || count < this.chunkCount;
        }

        private boolean checkTickTime(long tickTime) {
            return this.tickPercent <= 0 || tickTime < this.tickTimeLimit;
        }

    }

}
