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
package org.spongepowered.common.mixin.core.entity.item;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Maps;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.math.Rotations;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.mutable.entity.ArmorStandData;
import org.spongepowered.api.data.manipulator.mutable.entity.BodyPartRotationalData;
import org.spongepowered.api.data.type.BodyPart;
import org.spongepowered.api.data.type.BodyParts;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongeArmorStandData;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongeBodyPartRotationalData;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import org.spongepowered.common.mixin.core.entity.MixinEntityLivingBase;
import org.spongepowered.common.util.VecHelper;

import java.util.List;
import java.util.Map;

@Mixin(EntityArmorStand.class)
@Implements(@Interface(iface = ArmorStand.class, prefix = "armor$"))
public abstract class MixinEntityArmorStand extends MixinEntityLivingBase implements ArmorStand {

    @Shadow public Rotations leftArmRotation;
    @Shadow public Rotations rightArmRotation;
    @Shadow public Rotations leftLegRotation;
    @Shadow public Rotations rightLegRotation;

    @Shadow public abstract boolean getShowArms();
    @Shadow public abstract boolean hasNoBasePlate();
    @Shadow public abstract boolean hasNoGravity();
    @Shadow public abstract boolean hasMarker();
    @Shadow public abstract boolean shadow$isSmall();
    @Shadow public abstract void setNoBasePlate(boolean p_175426_1_);
    @Shadow public abstract void setNoGravity(boolean p_175425_1_);
    @Shadow public abstract void shadow$setSmall(boolean p_175420_1_);
    @Shadow public abstract void shadow$setShowArms(boolean p_175413_1_);
    @Shadow public abstract Rotations shadow$getHeadRotation();
    @Shadow public abstract Rotations getBodyRotation();

    @Intrinsic
    @Deprecated
    public boolean armor$isSmall() {
        return this.shadow$isSmall();
    }

    @Intrinsic
    @Deprecated
    public void armor$setSmall(boolean small) {
        this.shadow$setSmall(small);
    }

    @Override
    @Deprecated
    public boolean doesShowArms() {
        return this.getShowArms();
    }

    @Intrinsic
    @Deprecated
    public void armor$setShowArms(boolean showArms) {
        this.shadow$setShowArms(showArms);
    }

    @Override
    @Deprecated
    public boolean hasBasePlate() {
        return !this.hasNoBasePlate();
    }

    @Override
    @Deprecated
    public void setHasBasePlate(boolean baseplate) {
        this.setNoBasePlate(!baseplate);
    }

    @Override
    @Deprecated
    public boolean hasGravity() {
        return !this.hasNoGravity();
    }

    @Override
    @Deprecated
    public void setGravity(boolean gravity) {
        this.setNoGravity(!gravity);
    }

    @Override
    public Value<Boolean> marker() {
        return new SpongeValue<>(Keys.ARMOR_STAND_MARKER, false, this.hasMarker());
    }

    @Override
    public Value<Boolean> small() {
        return new SpongeValue<>(Keys.ARMOR_STAND_IS_SMALL, false, this.shadow$isSmall());
    }

    @Override
    public Value<Boolean> basePlate() {
        return new SpongeValue<>(Keys.ARMOR_STAND_HAS_BASE_PLATE, true, !this.hasNoBasePlate());
    }

    @Override
    public Value<Boolean> arms() {
        return new SpongeValue<>(Keys.ARMOR_STAND_HAS_ARMS, false, this.getShowArms());
    }

    @Override
    public Value<Boolean> gravity() {
        return new SpongeValue<>(Keys.ARMOR_STAND_HAS_GRAVITY, true, !this.hasNoGravity());
    }

    @Override
    public ArmorStandData getArmorStandData() {
        return new SpongeArmorStandData(this.hasMarker(), this.shadow$isSmall(), !this.hasNoGravity(), this.getShowArms(), !this.hasNoBasePlate());
    }

    @Override
    public BodyPartRotationalData getBodyPartRotationalData() {
        Map<BodyPart, Vector3d> rotations = Maps.newHashMapWithExpectedSize(6);
        rotations.put(BodyParts.HEAD, VecHelper.toVector3d(this.shadow$getHeadRotation()));
        rotations.put(BodyParts.CHEST, VecHelper.toVector3d(this.getBodyRotation()));
        rotations.put(BodyParts.LEFT_ARM, VecHelper.toVector3d(this.leftArmRotation));
        rotations.put(BodyParts.RIGHT_ARM, VecHelper.toVector3d(this.rightArmRotation));
        rotations.put(BodyParts.LEFT_LEG, VecHelper.toVector3d(this.leftLegRotation));
        rotations.put(BodyParts.RIGHT_LEG, VecHelper.toVector3d(this.rightLegRotation));
        return new SpongeBodyPartRotationalData(rotations);
    }

    @Override
    public void supplyVanillaManipulators(List<DataManipulator<?, ?>> manipulators) {
        super.supplyVanillaManipulators(manipulators);
        manipulators.add(getBodyPartRotationalData());
        manipulators.add(getArmorStandData());
    }
}
