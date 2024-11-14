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
package org.spongepowered.common.effect.particle;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.util.ARGB;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleType;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.PositionSource;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class SpongeParticleHelper {

    public static ClientboundBundlePacket createPacket(final ParticleEffect effect, final double x, final double y, final double z) {
        final List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        SpongeParticleHelper.getCachedPacket((SpongeParticleEffect) effect).process(x, y, z, packets);
        return new ClientboundBundlePacket(packets);
    }

    public static net.minecraft.core.particles.@Nullable ParticleOptions vanillaParticleOptions(final ParticleEffect effect) {
        return SpongeParticleHelper.getCachedPacket((SpongeParticleEffect) effect).particleOptions();
    }

    private static CachedParticlePacket getCachedPacket(final SpongeParticleEffect effect) {
        if (effect.cachedPacket == null) {
            final ParticleType type = effect.type();

            effect.cachedPacket = SpongeParticleHelper.getNamedPacket(effect, (net.minecraft.core.particles.ParticleType<?>) type);
        }

        return effect.cachedPacket;
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private static CachedParticlePacket getNamedPacket(final ParticleEffect effect, final net.minecraft.core.particles.ParticleType<?> internalType) {
        // Named particles always support OFFSET and QUANTITY.
        final Vector3f offset = effect.optionOrDefault(ParticleOptions.OFFSET).get().toFloat();
        final int quantity = effect.optionOrDefault(ParticleOptions.QUANTITY).get();
        final Vector3f velocity = effect.optionOrDefault(ParticleOptions.VELOCITY).orElse(Vector3d.ZERO).toFloat();

        if (internalType instanceof SimpleParticleType) {
            // Simple named particle without any additional supported options.
            return new NamedCachedPacket((net.minecraft.core.particles.ParticleOptions) internalType, offset, quantity, velocity);
        }

        // The only way we can see what options are supported for a particular named particle
        // is to compare the internal type's deserializer to some static deserializer fields.
        // If only mojang had some type akin to our ParticleEffect...
        if (internalType == ParticleTypes.BLOCK || internalType == ParticleTypes.BLOCK_MARKER
                || internalType == ParticleTypes.FALLING_DUST || internalType == ParticleTypes.DUST_PILLAR) {
            //This particle type supports a block state option.
            final BlockState state = effect.optionOrDefault(ParticleOptions.BLOCK_STATE).get();
            final BlockParticleOption particleData = new BlockParticleOption(
                    (net.minecraft.core.particles.ParticleType<BlockParticleOption>) internalType,
                    (net.minecraft.world.level.block.state.BlockState) state);
            return new NamedCachedPacket(particleData, offset, quantity, velocity);
        } else if (internalType == ParticleTypes.DUST_COLOR_TRANSITION) {
            final Color color = effect.optionOrDefault(ParticleOptions.COLOR).get();
            final Color toColor = effect.optionOrDefault(ParticleOptions.TO_COLOR).get();
            final double scale = effect.optionOrDefault(ParticleOptions.SCALE).get();
            final DustColorTransitionOptions particleData = new DustColorTransitionOptions(
                    ARGB.color(color.red(), color.green(), color.blue()),
                    ARGB.color(toColor.red(), toColor.green(), toColor.blue()),
                    (float) scale);
            return new NamedCachedPacket(particleData, offset, quantity, velocity);
        } else if (internalType == ParticleTypes.DUST) {
            //This particle type supports a color option.
            final Color color = effect.optionOrDefault(ParticleOptions.COLOR).get();
            final double scale = effect.optionOrDefault(ParticleOptions.SCALE).get();
            final DustParticleOptions particleData = new DustParticleOptions(ARGB.color(color.red(), color.green(), color.blue()), (float) scale);
            return new NamedCachedPacket(particleData, offset, quantity, velocity);
        } else if (internalType == ParticleTypes.ITEM) {
            //This particle type supports an item option.
            final ItemStackSnapshot snapshot = effect.optionOrDefault(ParticleOptions.ITEM_STACK_SNAPSHOT).get();
            final ItemParticleOption particleData = new ItemParticleOption(
                    (net.minecraft.core.particles.ParticleType<ItemParticleOption>) internalType,
                    ItemStackUtil.fromSnapshotToNative(snapshot));
            return new NamedCachedPacket(particleData, offset, quantity, velocity);
        } else if (internalType == ParticleTypes.SCULK_CHARGE) {
            final double roll = effect.optionOrDefault(ParticleOptions.ROLL).get();
            final SculkChargeParticleOptions particleData = new SculkChargeParticleOptions((float) roll);
            return new NamedCachedPacket(particleData, offset, quantity, velocity);
        } else if (internalType == ParticleTypes.SHRIEK) {
            final int delay = effect.optionOrDefault(ParticleOptions.DELAY).get();
            final ShriekParticleOption particleData = new ShriekParticleOption(delay);
            return new NamedCachedPacket(particleData, offset, quantity, velocity);
        } else if (internalType == ParticleTypes.VIBRATION) {
            final PositionSource source  = effect.optionOrDefault(ParticleOptions.DESTINATION).get();
            final Ticks delay = effect.optionOrDefault(ParticleOptions.TRAVEL_TIME).get();
            final VibrationParticleOption particleData = new VibrationParticleOption(
                    (net.minecraft.world.level.gameevent.PositionSource) source,
                    (int) delay.ticks());
            return new NamedCachedPacket(particleData, offset, quantity, velocity);
        } else if (internalType == ParticleTypes.ENTITY_EFFECT) {
            // This particle type supports color and opacity options.
            final Color color = effect.optionOrDefault(ParticleOptions.COLOR).get();
            final double opacity = effect.optionOrDefault(ParticleOptions.OPACITY).get();
            final ColorParticleOption particleData = ColorParticleOption.create(
                    (net.minecraft.core.particles.ParticleType<ColorParticleOption>) internalType,
                    ARGB.color(ARGB.as8BitChannel((float) opacity), color.red(), color.green(), color.blue()));
            return new NamedCachedPacket(particleData, offset, quantity, velocity);
        }

        // Otherwise, we don't really know how to get a valid ParticleOptions. Sorry mods!
        return EmptyCachedPacket.INSTANCE;
    }

    public static ParticleEffect spongeParticleOptions(final net.minecraft.core.particles.ParticleOptions effect) {
        final net.minecraft.core.particles.ParticleType<?> type = effect.getType();
        if (type instanceof SimpleParticleType) {
            return new SpongeParticleEffect((ParticleType) type, Collections.emptyMap());
        }

        if (effect instanceof BlockParticleOption blockOptions) {
            return new SpongeParticleEffect((ParticleType) type, Map.of(ParticleOptions.BLOCK_STATE.get(), blockOptions.getState()));
        } else if (effect instanceof DustColorTransitionOptions dustColorTransitionOptions) {
            return new SpongeParticleEffect((ParticleType) type, Map.of(
                    ParticleOptions.COLOR.get(), Color.of(
                            Vector3f.from(dustColorTransitionOptions.getFromColor().x, dustColorTransitionOptions.getFromColor().y, dustColorTransitionOptions.getFromColor().z).mul(255)),
                    ParticleOptions.TO_COLOR.get(), Color.of(
                            Vector3f.from(dustColorTransitionOptions.getToColor().x, dustColorTransitionOptions.getToColor().y, dustColorTransitionOptions.getToColor().z).mul(255)),
                    ParticleOptions.SCALE.get(), dustColorTransitionOptions.getScale()
            ));
        } else if (effect instanceof DustParticleOptions dustOptions) {
            // This particle type supports a color option.
            return new SpongeParticleEffect((ParticleType) type, Map.of(
                    ParticleOptions.COLOR.get(), Color.of(
                            Vector3f.from(dustOptions.getColor().x, dustOptions.getColor().y, dustOptions.getColor().z).mul(255)),
                    ParticleOptions.SCALE.get(), dustOptions.getScale()
            ));
        } else if (effect instanceof ItemParticleOption itemOptions) {
            // This particle type supports an item option.
            return new SpongeParticleEffect((ParticleType) type, Map.of(ParticleOptions.ITEM_STACK_SNAPSHOT.get(), ItemStackUtil.snapshotOf(itemOptions.getItem())));
        } else if (effect instanceof SculkChargeParticleOptions sculkChargeOptions) {
            return new SpongeParticleEffect((ParticleType) type, Map.of(ParticleOptions.ROLL.get(), sculkChargeOptions.roll()));
        } else if (effect instanceof ShriekParticleOption shriekOption) {
            return new SpongeParticleEffect((ParticleType) type, Map.of(ParticleOptions.DELAY.get(), shriekOption.getDelay()));
        } else if (effect instanceof VibrationParticleOption vibrationOptions) {
            return new SpongeParticleEffect((ParticleType) type, Map.of(
                ParticleOptions.DESTINATION.get(), vibrationOptions.getDestination(),
                ParticleOptions.TRAVEL_TIME.get(), Ticks.of(vibrationOptions.getArrivalInTicks())
            ));
        } else if (effect instanceof ColorParticleOption colorOptions) {
            // This particle type supports color and opacity options.
            return new SpongeParticleEffect((ParticleType) type, Map.of(
                ParticleOptions.COLOR.get(), Color.of(
                    Vector3f.from(colorOptions.getRed(), colorOptions.getGreen(), colorOptions.getBlue()).mul(255)),
                ParticleOptions.OPACITY.get(), colorOptions.getAlpha()
            ));
        }

        return new SpongeParticleEffect((ParticleType) type, Collections.emptyMap());
    }

    private static final class EmptyCachedPacket implements CachedParticlePacket {

        public static final EmptyCachedPacket INSTANCE = new EmptyCachedPacket();

        @Override
        public void process(final double x, final double y, final double z, final List<Packet<? super ClientGamePacketListener>> output) {
        }

        @Override
        public net.minecraft.core.particles.@Nullable ParticleOptions particleOptions() {
            return null;
        }
    }

    private static final class NamedCachedPacket implements CachedParticlePacket {

        private final net.minecraft.core.particles.ParticleOptions particleData;
        private final Vector3f offset;
        private final int quantity;
        private final Vector3f velocity;

        public NamedCachedPacket(final net.minecraft.core.particles.ParticleOptions particleData, final Vector3f offset, final int quantity, Vector3f velocity) {
            this.particleData = particleData;
            this.offset = offset;
            this.quantity = quantity;
            this.velocity = velocity;
        }

        @Override
        public void process(final double x, final double y, final double z, final List<Packet<? super ClientGamePacketListener>> output) {
            final float posX = (float) x;
            final float posY = (float) y;
            final float posZ = (float) z;

            final float offX = this.offset.x();
            final float offY = this.offset.y();
            final float offZ = this.offset.z();

            if (this.velocity.equals(Vector3f.ZERO)) {
                final ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                        this.particleData,
                        true,
                        true,
                        posX, posY, posZ,
                        offX, offY, offZ,
                        0.0F,
                        this.quantity
                );

                output.add(packet);
            } else {
                final float velocityX = this.velocity.x();
                final float velocityY = this.velocity.y();
                final float velocityZ = this.velocity.z();
                final Random random = new Random();
                for (int i = 0; i < this.quantity; i++) {
                    final float px0 = posX + (random.nextFloat() * 2f - 1f) * offX;
                    final float py0 = posY + (random.nextFloat() * 2f - 1f) * offY;
                    final float pz0 = posZ + (random.nextFloat() * 2f - 1f) * offZ;

                    final ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                            this.particleData,
                            true,
                            true,
                            px0, py0, pz0,
                            velocityX, velocityY, velocityZ,
                            1f,
                            0);

                    output.add(packet);
                }
            }
        }

        @Override
        public net.minecraft.core.particles.@Nullable ParticleOptions particleOptions() {
            return this.particleData;
        }
    }
}
