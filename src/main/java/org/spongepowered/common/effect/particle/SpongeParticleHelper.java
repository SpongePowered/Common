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

import com.flowpowered.math.vector.Vector3d;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketEffect;
import net.minecraft.network.play.server.SPacketParticles;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionType;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.type.NotePitch;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Color;
import org.spongepowered.common.data.type.SpongeNotePitch;
import org.spongepowered.common.item.inventory.SpongeItemStackSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class SpongeParticleHelper {

    private static int getBlockState(SpongeParticleEffect effect, Optional<BlockState> defaultBlockState) {
        Optional<BlockState> blockState = effect.getOption(ParticleOptions.BLOCK_STATE);
        if (blockState.isPresent()) {
            return Block.getStateId((IBlockState) blockState.get());
        } else {
            Optional<ItemStackSnapshot> optSnapshot = effect.getOption(ParticleOptions.ITEM_STACK_SNAPSHOT);
            if (optSnapshot.isPresent()) {
                ItemStackSnapshot snapshot = optSnapshot.get();
                Optional<BlockType> blockType = snapshot.getType().getBlock();
                if (blockType.isPresent()) {
                    return Block.getStateId(((Block) blockType.get()).getStateFromMeta(
                            ((SpongeItemStackSnapshot) snapshot).getDamageValue()));
                } else {
                    return 0;
                }
            } else {
                return Block.getStateId((IBlockState) defaultBlockState.get());
            }
        }
    }

    /**
     * Gets the list of packets that are needed to spawn the particle effect at
     * the position. This method tries to minimize the amount of packets for
     * better performance and lower bandwidth use.
     *
     * @param effect The particle effect
     * @param position The position
     * @return The packets
     */
    public static List<Packet<?>> toPackets(SpongeParticleEffect effect, Vector3d position) {
        SpongeParticleType type = effect.getType();

        EnumParticleTypes internal = type.getInternalType();
        // Special cases
        if (internal == null) {
            BlockPos pos = new BlockPos(Math.round(position.getX()), Math.round(position.getY()), Math.round(position.getZ()));
            if (type == ParticleTypes.FERTILIZER) {
                int quantity = effect.getOptionOrDefault(ParticleOptions.QUANTITY).get();
                return Collections.singletonList(new SPacketEffect(2005, pos, quantity, false));
            } else if (type == ParticleTypes.SPLASH_POTION) {
                Potion potion = (Potion) effect.getOptionOrDefault(ParticleOptions.POTION_EFFECT_TYPE).get();
                for (PotionType potionType : PotionType.REGISTRY) {
                    for (net.minecraft.potion.PotionEffect potionEffect : potionType.getEffects()) {
                        if (potionEffect.getPotion() == potion) {
                            return Collections.singletonList(new SPacketEffect(2002, pos, PotionType.getID(potionType), false));
                        }
                    }
                }
                return Collections.emptyList();
            } else if (type == ParticleTypes.BREAK_BLOCK) {
                int state = getBlockState(effect, type.getDefaultOption(ParticleOptions.BLOCK_STATE));
                if (state == 0) {
                    return Collections.emptyList();
                }
                return Collections.singletonList(new SPacketEffect(2001, pos, state, false));
            } else if (type == ParticleTypes.MOBSPAWNER_FLAMES) {
                return Collections.singletonList(new SPacketEffect(2004, pos, 0, false));
            } else if (type == ParticleTypes.ENDER_TELEPORT) {
                return Collections.singletonList(new SPacketEffect(2003, pos, 0, false));
            } else if (type == ParticleTypes.DRAGON_BREATH_ATTACK) {
                return Collections.singletonList(new SPacketEffect(2006, pos, 0, false));
            }
            return Collections.emptyList();
        }

        Vector3d offset = effect.getOption(ParticleOptions.OFFSET).orElse(Vector3d.ZERO);

        int quantity = effect.getOption(ParticleOptions.QUANTITY).orElse(1);
        int[] extra = null;

        float px = (float) position.getX();
        float py = (float) position.getY();
        float pz = (float) position.getZ();

        double ox = offset.getX();
        double oy = offset.getY();
        double oz = offset.getZ();

        // The extra values, normal behavior offsetX, offsetY, offsetZ
        double f0 = 0f;
        double f1 = 0f;
        double f2 = 0f;

        // Depends on behavior
        // Note: If the count > 0 -> speed = 0f else if count = 0 -> speed = 1f

        Optional<BlockState> defaultBlockState;
        if (internal != EnumParticleTypes.ITEM_CRACK && (defaultBlockState = type.getDefaultOption(ParticleOptions.BLOCK_STATE)).isPresent()) {
            int state = getBlockState(effect, defaultBlockState);
            if (state == 0) {
                return Collections.emptyList();
            }
            extra = new int[] { state };
        }

        Optional<ItemStackSnapshot> defaultSnapshot;
        if (extra == null && (defaultSnapshot = type.getDefaultOption(ParticleOptions.ITEM_STACK_SNAPSHOT)).isPresent()) {
            Optional<ItemStackSnapshot> optSnapshot = effect.getOption(ParticleOptions.ITEM_STACK_SNAPSHOT);
            if (optSnapshot.isPresent()) {
                ItemStackSnapshot snapshot = optSnapshot.get();
                extra = new int[] { Item.getIdFromItem((Item) snapshot.getType()), ((SpongeItemStackSnapshot) snapshot).getDamageValue() };
            } else {
                Optional<BlockState> optBlockState = effect.getOption(ParticleOptions.BLOCK_STATE);
                if (optBlockState.isPresent()) {
                    BlockState blockState = optBlockState.get();
                    Optional<ItemType> optItemType = blockState.getType().getItem();
                    if (optItemType.isPresent()) {
                        extra = new int[] { Item.getIdFromItem((Item) optItemType.get()),
                                ((Block) blockState.getType()).getMetaFromState((IBlockState) blockState) };
                    } else {
                        return Collections.emptyList();
                    }
                } else {
                    ItemStackSnapshot snapshot = defaultSnapshot.get();
                    extra = new int[] { Item.getIdFromItem((Item) snapshot.getType()), ((SpongeItemStackSnapshot) snapshot).getDamageValue() };
                }
            }
        }

        if (extra == null) {
            extra = new int[0];
        }

        Optional<Double> defaultScale = type.getDefaultOption(ParticleOptions.SCALE);
        Optional<Color> defaultColor;
        Optional<NotePitch> defaultNote;
        Optional<Vector3d> defaultVelocity;
        if (defaultScale.isPresent()) {
            double scale = effect.getOption(ParticleOptions.SCALE).orElse(defaultScale.get());

            // The formula of the large explosion acts strange
            // Client formula: sizeClient = 1 - sizeServer * 0.5
            // The particle effect returns the client value so
            // Server formula: sizeServer = (-sizeClient * 2) + 2
            if (internal == EnumParticleTypes.EXPLOSION_LARGE || internal == EnumParticleTypes.SWEEP_ATTACK) {
                scale = (-scale * 2f) + 2f;
            }

            if (scale == 0f) {
                return Collections.singletonList(
                        new SPacketParticles(internal, true, px, py, pz, (float) ox, (float) oy, (float) oz, 0f, quantity, extra));
            }

            f0 = scale;
        } else if ((defaultColor = type.getDefaultOption(ParticleOptions.COLOR)).isPresent()) {
            Color color = effect.getOption(ParticleOptions.COLOR).orElse(null);

            boolean isSpell = internal == EnumParticleTypes.SPELL_MOB || internal == EnumParticleTypes.SPELL_MOB_AMBIENT;

            if (!isSpell && (color == null || color.equals(defaultColor.get()))) {
                return Collections.singletonList(
                        new SPacketParticles(internal, true, px, py, pz, (float) ox, (float) oy, (float) oz, 0f, quantity, extra));
            } else if (isSpell && color == null) {
                color = defaultColor.get();
            }

            f0 = color.getRed() / 255f;
            f1 = color.getGreen() / 255f;
            f2 = color.getBlue() / 255f;

            // Make sure that the x and z component are never 0 for these effects,
            // they would trigger the slow horizontal velocity (unsupported on the server),
            // but we already chose for the color, can't have both
            if (isSpell) {
                f0 = Math.max(f0, 0.001f);
                f2 = Math.max(f0, 0.001f);
            }

            // If the f0 value 0 is, the redstone will set it automatically to red 255
            if (f0 == 0f && internal == EnumParticleTypes.REDSTONE) {
                f0 = 0.00001f;
            }
        } else if ((defaultNote = type.getDefaultOption(ParticleOptions.NOTE)).isPresent()) {
            NotePitch notePitch = effect.getOption(ParticleOptions.NOTE).orElse(defaultNote.get());
            float note = ((SpongeNotePitch) notePitch).getByteId();

            if (note == 0f) {
                return Collections.singletonList(
                        new SPacketParticles(internal, true, px, py, pz, (float) ox, (float) oy, (float) oz, 0f, quantity, extra));
            }

            f0 = note / 24f;
        } else if ((defaultVelocity = type.getDefaultOption(ParticleOptions.VELOCITY)).isPresent()) {
            Vector3d velocity = effect.getOption(ParticleOptions.VELOCITY).orElse(defaultVelocity.get());

            f0 = velocity.getX();
            f1 = velocity.getY();
            f2 = velocity.getZ();

            Optional<Boolean> slowHorizontalVelocity = type.getDefaultOption(ParticleOptions.SLOW_HORIZONTAL_VELOCITY);
            if (slowHorizontalVelocity.isPresent() && slowHorizontalVelocity.get()) {
                f0 = 0f;
                f2 = 0f;
            }

            // The y value won't work for this effect, if the value isn't 0 the velocity won't work
            if (internal == EnumParticleTypes.WATER_SPLASH) {
                f1 = 0f;
            }

            if (f0 == 0f && f1 == 0f && f2 == 0f) {
                return Collections.singletonList(
                        new SPacketParticles(internal, true, px, py, pz, (float) ox, (float) oy, (float) oz, 0f, quantity, extra));
            }
        }

        // Is this check necessary?
        if (f0 == 0f && f1 == 0f && f2 == 0f) {
            return Collections.singletonList(new SPacketParticles(internal, true, px, py, pz, (float) ox, (float) oy, (float) oz, 0f, quantity, extra));
        }

        List<Packet<?>> packets = new ArrayList<>(quantity);

        if (ox == 0f && oy == 0f && oz == 0f) {
            for (int i = 0; i < quantity; i++) {
                packets.add(new SPacketParticles(internal, true, px, py, pz, (float) f0, (float) f1, (float) f2, 1f, 0, extra));
            }
        } else {
            Random random = new Random();

            for (int i = 0; i < quantity; i++) {
                double px0 = (px + (random.nextFloat() * 2f - 1f) * ox);
                double py0 = (py + (random.nextFloat() * 2f - 1f) * oy);
                double pz0 = (pz + (random.nextFloat() * 2f - 1f) * oz);

                packets.add(new SPacketParticles(internal, true, (float) px0, (float) py0, (float) pz0, (float) f0, (float) f1, (float) f2, 1f, 0,
                        extra));
            }
        }

        return packets;
    }

    private SpongeParticleHelper() {
    }
}
