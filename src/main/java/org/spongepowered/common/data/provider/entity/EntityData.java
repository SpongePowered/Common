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

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.PushReaction;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.portal.Portal;
import org.spongepowered.api.world.portal.PortalLogic;
import org.spongepowered.common.accessor.world.entity.EntityAccessor;
import org.spongepowered.common.accessor.world.entity.PortalProcessorAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.world.entity.EntityBridge;
import org.spongepowered.common.bridge.world.entity.EntityMaxAirBridge;
import org.spongepowered.common.bridge.world.entity.PortalProcessorBridge;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.entity.SpongeEntityArchetype;
import org.spongepowered.common.entity.SpongeEntitySnapshot;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.SpongeTicks;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3d;

import java.util.stream.Collectors;

public final class EntityData {

    private EntityData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(Entity.class)
                    .create(Keys.AGE)
                        .get(h -> h.tickCount)
                        .setAnd((h, v) -> {
                            if (v < 0) {
                                return false;
                            }
                            h.tickCount = v;
                            return true;
                        })
                    .create(Keys.BASE_SIZE)
                        .get(h -> (double) h.getBbWidth())
                    .create(Keys.BASE_VEHICLE)
                        .get(h -> {
                            final Entity rootVehicle = h.getRootVehicle();
                            if (rootVehicle == h) {
                                return null;
                            }
                            return (org.spongepowered.api.entity.Entity) rootVehicle;
                        })
                    .create(Keys.CUSTOM_NAME)
                        .get(h -> h.hasCustomName() ? SpongeAdventure.asAdventure(h.getCustomName()) : null)
                        .set((h, v) -> h.setCustomName(SpongeAdventure.asVanilla(v)))
                        .delete(h -> {
                            h.setCustomName(null);
                            h.setCustomNameVisible(false);
                        })
                    .create(Keys.DISPLAY_NAME)
                        .get(h -> SpongeAdventure.asAdventure(h.getDisplayName()))
                    .create(Keys.EYE_HEIGHT)
                        .get(h -> (double) h.getEyeHeight())
                    .create(Keys.EYE_POSITION)
                        .get(h -> VecHelper.toVector3d(h.getEyePosition(1f)))
                    .create(Keys.FALL_DISTANCE)
                        .get(h -> (double) h.fallDistance)
                        .setAnd((h, v) -> {
                            if (v < 0) {
                                return false;
                            }
                            h.fallDistance = v.floatValue();
                            return true;
                        })
                    .create(Keys.FIRE_DAMAGE_DELAY)
                        .get(h -> h.fireImmune()
                                ? Ticks.infinite()
                                : Ticks.of(((EntityAccessor) h).invoker$getFireImmuneTicks()))
                        .setAnd((h, v) -> {
                            final int ticks = SpongeTicks.toSaturatedIntOrInfinite(v);
                            if (v.isInfinite() || ticks < 1 || ticks > Short.MAX_VALUE) {
                                return false;
                            }
                            ((EntityBridge) h).bridge$setFireImmuneTicks(ticks);
                            return ((EntityAccessor) h).invoker$getFireImmuneTicks() == ticks;
                        })
                    .create(Keys.FIRE_TICKS)
                        .get(h -> ((EntityAccessor) h).accessor$remainingFireTicks() > 0 ? Ticks.of(((EntityAccessor) h).accessor$remainingFireTicks()) : null)
                        .setAnd((h, v) -> {
                            if (v.isInfinite()) {
                                return false;
                            }
                            final int ticks = SpongeTicks.toSaturatedIntOrInfinite(v);
                            ((EntityAccessor) h).accessor$remainingFireTicks(Math.max(ticks, Constants.Entity.MINIMUM_FIRE_TICKS));
                            return true;
                        })
                        .deleteAndGet(h -> {
                            final EntityAccessor accessor = (EntityAccessor) h;
                            final int ticks = accessor.accessor$remainingFireTicks();
                            if (ticks < Constants.Entity.MINIMUM_FIRE_TICKS) {
                                return DataTransactionResult.failNoData();
                            }
                            final DataTransactionResult.Builder dtrBuilder = DataTransactionResult.builder();
                            dtrBuilder.replace(Value.immutableOf(Keys.FIRE_TICKS, new SpongeTicks(ticks)));
                            dtrBuilder.replace(Value.immutableOf(Keys.FIRE_DAMAGE_DELAY,
                                    new SpongeTicks(((EntityAccessor) h).invoker$getFireImmuneTicks())));
                            h.clearFire();
                            return dtrBuilder.result(DataTransactionResult.Type.SUCCESS).build();
                        })
                    .create(Keys.FROZEN_TIME)
                        .get(h -> Ticks.of(h.getTicksFrozen()))
                        .resetOnDelete(Ticks.zero())
                        .setAnd((h, v) -> {
                            if (v.isInfinite()) {
                                return false;
                            }
                            h.setTicksFrozen(SpongeTicks.toSaturatedIntOrInfinite(v));
                            return true;
                        })
                    .create(Keys.MAX_FROZEN_TIME)
                        .get(h -> Ticks.of(h.getTicksRequiredToFreeze()))
                    .create(Keys.HEIGHT)
                        .get(h -> (double) h.getBbHeight())
                    .create(Keys.INVULNERABILITY_TICKS)
                        .get(h -> new SpongeTicks(h.invulnerableTime))
                        .resetOnDelete(Ticks.zero())
                        .setAnd((h, v) -> {
                            final int ticks = SpongeTicks.toSaturatedIntOrInfinite(v);
                            if (v.isInfinite() || ticks < 0) {
                                return false;
                            }
                            h.invulnerableTime = ticks;
                            if (h instanceof LivingEntity) {
                                ((LivingEntity) h).hurtTime = ticks;
                            }
                            return true;
                        })
                    .create(Keys.IS_CUSTOM_NAME_VISIBLE)
                        .get(Entity::isCustomNameVisible)
                        .set(Entity::setCustomNameVisible)
                    .create(Keys.IS_FLYING)
                        .get(h -> h.hasImpulse)
                        .set((h, v) -> h.hasImpulse = v)
                        .supports(h -> !(h instanceof Player))
                    .create(Keys.IS_GLOWING)
                        .get(Entity::hasGlowingTag)
                        .set(Entity::setGlowingTag)
                    .create(Keys.IS_GRAVITY_AFFECTED)
                        .get(h -> !h.isNoGravity())
                        .set((h, v) -> h.setNoGravity(!v))
                    .create(Keys.IS_SNEAKING)
                        .get(Entity::isShiftKeyDown)
                        .set(Entity::setShiftKeyDown)
                    .create(Keys.IS_SPRINTING)
                        .get(Entity::isSprinting)
                        .set(Entity::setSprinting)
                    .create(Keys.IS_SILENT)
                        .get(Entity::isSilent)
                        .set(Entity::setSilent)
                    .create(Keys.IS_WET)
                        .get(Entity::isInWaterOrRain)
                    .create(Keys.ON_GROUND)
                        .get(Entity::onGround)
                    .create(Keys.PASSENGERS)
                        .get(h -> h.getPassengers().stream().map(org.spongepowered.api.entity.Entity.class::cast).collect(Collectors.toList()))
                        .delete(Entity::ejectPassengers)
                        .set((h, v) -> {
                            h.ejectPassengers();
                            v.forEach(v1 -> ((Entity) v1).startRiding(h, true));
                        })
                    .create(Keys.PUSH_REACTION)
                        .get(h -> (PushReaction) (Object) h.getPistonPushReaction())
                    .create(Keys.REMAINING_AIR)
                        .get(h -> Math.max(0, h.getAirSupply()))
                        .setAnd((h, v) -> {
                            if (v < 0 || v > h.getMaxAirSupply()) {
                                return false;
                            }
                            if (v == 0 && h.getAirSupply() < 0) {
                                return false;
                            }
                            h.setAirSupply(v);
                            return true;
                        })
                    .create(Keys.SCALE)
                        .get(h -> 1d)
                    .create(Keys.SCOREBOARD_TAGS)
                        .get(Entity::getTags)
                        .delete(h -> h.getTags().clear())
                        .set((h, v) -> {
                            h.getTags().clear();
                            h.getTags().addAll(v);
                        })
                    .create(Keys.VEHICLE)
                        .get(h -> (org.spongepowered.api.entity.Entity) h.getVehicle())
                        .set((h, v) -> h.startRiding((Entity) v, true))
                        .delete(h -> h.stopRiding())
                    .create(Keys.VELOCITY)
                        .get(h -> VecHelper.toVector3d(h.getDeltaMovement()))
                        .resetOnDelete(Vector3d.ZERO)
                        .set((h, v) -> {
                            h.setDeltaMovement(VecHelper.toVanillaVector3d(v));
                            h.hurtMarked = true;
                        })
                    .create(Keys.SWIFTNESS)
                        .get(m -> m.getDeltaMovement().length())
                        .set((m, v) -> {
                            m.setDeltaMovement(m.getDeltaMovement().normalize().scale(v));
                            m.hurtMarked = true;
                        })
                        .supports(m -> m.getDeltaMovement().lengthSqr() > 0)
                    .create(Keys.PORTAL)
                        .get(h -> (Portal) h.portalProcess)
                    .create(Keys.PORTAL_LOGIC)
                        .get(h -> h.portalProcess == null ? null : (PortalLogic) ((PortalProcessorAccessor) h.portalProcess).accessor$portal())
                        .set((h, v) -> {
                            h.portalProcess = new PortalProcessor((net.minecraft.world.level.block.Portal) v, h.blockPosition());
                            ((PortalProcessorBridge)h.portalProcess).bridge$init(h.level());
                        })
                        .delete(h -> h.portalProcess = null)
                    .create(Keys.BOUNDING_BOX_BASE_SIZE)
                        .get(h -> (double) h.getBbWidth())
                    .create(Keys.BOUNDING_BOX_HEIGHT)
                        .get(h -> (double) h.getBbHeight())
                .asMutable(EntityMaxAirBridge.class)
                    .create(Keys.MAX_AIR)
                        .get(EntityMaxAirBridge::bridge$getMaxAir)
                        .set(EntityMaxAirBridge::bridge$setMaxAir)
                ;

        registrator.spongeDataStore(ResourceKey.sponge("max_air"), EntityMaxAirBridge.class, Keys.MAX_AIR);
        registrator.newDataStore(SpongeEntitySnapshot.class, SpongeEntityArchetype.class)
                .dataStore(Keys.CUSTOM_NAME,
                    (dv, v) -> dv.set(Constants.Entity.CUSTOM_NAME, GsonComponentSerializer.gson().serialize(v)),
                    dv -> dv.getString(Constants.Entity.CUSTOM_NAME).map(GsonComponentSerializer.gson()::deserialize));

        // @formatter:on
    }

}
