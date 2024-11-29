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
package org.spongepowered.common.data.provider.entity;

import com.mojang.math.Transformation;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3f;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.display.BillboardType;
import org.spongepowered.api.entity.display.DisplayEntity;
import org.spongepowered.api.entity.display.ItemDisplayType;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.util.Transform;
import org.spongepowered.common.accessor.world.entity.DisplayAccessor;
import org.spongepowered.common.accessor.world.entity.Display_BlockDisplayAccessor;
import org.spongepowered.common.accessor.world.entity.Display_ItemDisplayAccessor;
import org.spongepowered.common.accessor.world.entity.Display_TextDisplayAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.SpongeTicks;
import org.spongepowered.math.matrix.Matrix4d;
import org.spongepowered.math.vector.Vector3d;

public class DisplayEntityData {

    private DisplayEntityData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(Display.class)
                    .create(Keys.TRANSFORM)
                        .get(DisplayEntityData::getTransform)
                        .set((h, v) -> DisplayEntityData.setTransform(h, v.toMatrix()))
                    .create(Keys.MATRIX)
                        .get(DisplayEntityData::getMatrix)
                        .set(DisplayEntityData::setTransform)
                .asMutable(DisplayAccessor.class)
                    .create(Keys.BILLBOARD_TYPE)
                        .get(h -> (BillboardType) (Object) h.invoker$getBillboardConstraints())
                        .set((h, v) -> h.invoker$setBillboardConstraints((Display.BillboardConstraints) (Object) v))
                    .create(Keys.SKY_LIGHT)
                        .get(h -> DisplayEntityData.skyLight(h.invoker$getPackedBrightnessOverride()))
                        .set((h, v) -> h.invoker$setBrightnessOverride(DisplayEntityData.brightness(h.invoker$getPackedBrightnessOverride(), v, null)))
                        .delete(h -> h.invoker$setBrightnessOverride(null))
                    .create(Keys.BLOCK_LIGHT)
                        .get(h -> DisplayEntityData.blockLight(h.invoker$getPackedBrightnessOverride()))
                        .set((h, v) -> h.invoker$setBrightnessOverride(DisplayEntityData.brightness(h.invoker$getPackedBrightnessOverride(), null, v)))
                        .delete(h -> h.invoker$setBrightnessOverride(null))
                    .create(Keys.INTERPOLATION_DURATION)
                        .get(h -> Ticks.of(h.invoker$getInterpolationDuration()))
                        .setAnd((h ,v) -> {
                            if (v.isInfinite()) {
                                return false;
                            }
                            h.invoker$setInterpolationDuration(SpongeTicks.toSaturatedIntOrInfinite(v));
                            return true;
                        })
                    .create(Keys.INTERPOLATION_DELAY)
                        .get(h -> Ticks.of(h.invoker$getInterpolationDelay()))
                        .setAnd((h ,v) -> {
                            if (v.isInfinite()) {
                                return false;
                            }
                            h.invoker$setInterpolationDelay(SpongeTicks.toSaturatedIntOrInfinite(v));
                            return true;
                        })
                    .create(Keys.TELEPORT_DURATION)
                        .get(h -> Ticks.of(h.invoker$getPosRotInterpolationDuration()))
                        .setAnd((h ,v) -> {
                            if (v.isInfinite()) {
                                return false;
                            }
                            h.invoker$setPosRotInterpolationDuration(SpongeTicks.toSaturatedIntOrInfinite(v));
                            return true;
                        })
                    .create(Keys.SHADOW_RADIUS)
                        .get(h -> (double) h.invoker$getShadowRadius())
                        .set((h ,v) -> h.invoker$setShadowRadius(v.floatValue()))
                    .create(Keys.SHADOW_STRENGTH)
                        .get(h -> (double) h.invoker$getShadowStrength())
                        .set((h ,v) -> h.invoker$setShadowStrength(v.floatValue()))
                    .create(Keys.VIEW_RANGE)
                        .get(h -> (double) h.invoker$getViewRange())
                        .set((h ,v) -> h.invoker$setViewRange(v.floatValue()))
                .asMutable(Display_BlockDisplayAccessor.class)
                    .create(Keys.BLOCK_STATE)
                        .get(h -> ((BlockState) h.invoker$getBlockState()))
                        .set((h, v) -> h.invoker$setBlockState((net.minecraft.world.level.block.state.BlockState) v))
                .asMutable(Display_ItemDisplayAccessor.class)
                    .create(Keys.ITEM_STACK_SNAPSHOT)
                        .get(h -> ItemStackUtil.snapshotOf(((Display_ItemDisplayAccessor)h).invoker$getItemStack()))
                        .set((h, v) -> h.invoker$setItemStack(ItemStackUtil.fromSnapshotToNative(v)))
                    .create(Keys.ITEM_DISPLAY_TYPE)
                        .get(h -> (ItemDisplayType) (Object) h.invoker$getItemTransform())
                        .set((h, v) -> ((Display_ItemDisplayAccessor) h).invoker$setItemTransform(((ItemDisplayContext) (Object) v)))
                .asMutable(Display_TextDisplayAccessor.class)
                    .create(Keys.DISPLAY_NAME)
                        .get(h -> SpongeAdventure.asAdventure(h.invoker$getText()))
                        .set((h, v) -> h.invoker$setText(SpongeAdventure.asVanilla(v)))
                    .create(Keys.LINE_WIDTH)
                        .get(Display_TextDisplayAccessor::invoker$getLineWidth)
                        .set(Display_TextDisplayAccessor::invoker$setLineWidth)
                    .create(Keys.OPACITY)
                        .get(Display_TextDisplayAccessor::invoker$getTextOpacity)
                        .set(Display_TextDisplayAccessor::invoker$setTextOpacity)
                    .create(Keys.SEE_THROUGH_BLOCKS)
                        .get(h -> DisplayEntityData.getFlagValue(h, Display.TextDisplay.FLAG_SEE_THROUGH))
                        .set((h, v) -> DisplayEntityData.setFlagValue(h, v, Display.TextDisplay.FLAG_SEE_THROUGH))
                    .create(Keys.HAS_TEXT_SHADOW)
                        .get(h -> DisplayEntityData.getFlagValue(h, Display.TextDisplay.FLAG_SHADOW))
                        .set((h, v) -> DisplayEntityData.setFlagValue(h, v, Display.TextDisplay.FLAG_SHADOW))
                    .create(Keys.HAS_DEFAULT_BACKGROUND)
                        .get(h -> DisplayEntityData.getFlagValue(h, Display.TextDisplay.FLAG_USE_DEFAULT_BACKGROUND))
                        .set((h, v) -> DisplayEntityData.setFlagValue(h, v, Display.TextDisplay.FLAG_USE_DEFAULT_BACKGROUND))
                    .create(Keys.HAS_DEFAULT_BACKGROUND)
                        .get(h -> DisplayEntityData.getFlagValue(h, Display.TextDisplay.FLAG_USE_DEFAULT_BACKGROUND))
                        .set((h, v) -> DisplayEntityData.setFlagValue(h, v, Display.TextDisplay.FLAG_USE_DEFAULT_BACKGROUND))
                    .create(Keys.TEXT_BACKGROUND_COLOR)
                        .get(h -> DisplayEntityData.colorFromInt(h.invoker$getBackgroundColor()))
                        .set((h, v) -> h.invoker$setBackgroundColor(DisplayEntityData.colorToInt(v)))
        ;
        registrator.spongeDataStore(Keys.TELEPORT_DURATION.key(), DisplayEntity.class, Keys.TELEPORT_DURATION);
    }
    // @formatter:on

    private static boolean getFlagValue(final Display_TextDisplayAccessor h, final byte flag) {
        return (h.invoker$getFlags() & flag) != 0;
    }

    private static void setFlagValue(final Display_TextDisplayAccessor h, final Boolean set, final byte flag) {
        byte flags = h.invoker$getFlags();
        if (set) {
            flags = (byte) (flags | flag);
        } else {
            flags = (byte) (flags & ~flag);
        }
        h.invoker$setFlags(flags);
    }

    private static Brightness brightness(final int original, @Nullable Integer block, @Nullable Integer sky) {
        int blockValue;
        int skyValue;
        if (original != -1) {
            final Brightness unpacked = Brightness.unpack(original);
            // Take existing value if not getting set
            blockValue = block == null ? unpacked.block() : block;
            skyValue = sky == null ? unpacked.sky() : sky;
        } else {
            // If no existing value set unset values to 15
            blockValue = block == null ? 15 : block;
            skyValue = sky == null ? 15 : sky;
        }
        return new Brightness(blockValue, skyValue);
    }

    @Nullable
    private static Integer blockLight(final int original) {
        if (original == -1) {
            return null;
        }
        return Brightness.unpack(original).block();
    }

    @Nullable
    private static Integer skyLight(final int original) {
        if (original == -1) {
            return null;
        }
        return Brightness.unpack(original).sky();
    }

    private static Color colorFromInt(final int color) {
        return Color.ofRgb(color);
    }

    private static int colorToInt(final Color color) {
        return color.rgb();
    }

    private static Transform getTransform(final Display display) {
        var vanillaTransform = DisplayAccessor.invoker$createTransformation(display.getEntityData());
        var vMatrix = vanillaTransform.getMatrix();
        var scale = vMatrix.getScale(new Vector3f());
        var translation = vMatrix.getTranslation(new Vector3f());
        var rotation = vMatrix.getNormalizedRotation(new Quaterniond());
        var eulerRot = rotation.getEulerAnglesXYZ(new org.joml.Vector3d());

        final Transform transform = Transform.of(new Vector3d(translation.x, translation.y, translation.z),
                                                 new Vector3d(eulerRot.x, eulerRot.y, eulerRot.z),
                                                 new Vector3d(scale.x, scale.y, scale.z));
        return transform;
    }

    private static Matrix4d getMatrix(final Display display) {
        var vanillaTransform = DisplayAccessor.invoker$createTransformation(display.getEntityData());
        var vMatrix = vanillaTransform.getMatrix();
        return Matrix4d.from(
            vMatrix.get(0, 0), vMatrix.get(1, 0), vMatrix.get(2, 0), vMatrix.get(3, 0),
            vMatrix.get(0, 1), vMatrix.get(1, 1), vMatrix.get(2, 1), vMatrix.get(3, 1),
            vMatrix.get(0, 2), vMatrix.get(1, 2), vMatrix.get(2, 2), vMatrix.get(3, 2),
            vMatrix.get(0, 3), vMatrix.get(1, 3), vMatrix.get(2, 3), vMatrix.get(3, 3));
    }

    private static void setTransform(final Display h, final Matrix4d matrix) {
        var vMatrix = new org.joml.Matrix4f(
                (float) matrix.get(0, 0), (float) matrix.get(1, 0), (float) matrix.get(2, 0), (float) matrix.get(3, 0),
                (float) matrix.get(0, 1), (float) matrix.get(1, 1), (float) matrix.get(2, 1), (float) matrix.get(3, 1),
                (float) matrix.get(0, 2), (float) matrix.get(1, 2), (float) matrix.get(2, 2), (float) matrix.get(3, 2),
                (float) matrix.get(0, 3), (float) matrix.get(1, 3), (float) matrix.get(2, 3), (float) matrix.get(3, 3)
        );
        var vanillaTransform = new Transformation(vMatrix);
        ((DisplayAccessor) h).invoker$setTransformation(vanillaTransform);
    }
}
