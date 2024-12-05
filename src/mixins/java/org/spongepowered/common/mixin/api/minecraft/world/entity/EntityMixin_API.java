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
package org.spongepowered.common.mixin.api.minecraft.world.entity;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.event.HoverEvent;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.EntityArchetype;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.util.Transform;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Interface.Remap;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.data.VanishableBridge;
import org.spongepowered.common.bridge.world.entity.EntityBridge;
import org.spongepowered.common.data.persistence.NBTTranslator;
import org.spongepowered.common.entity.SpongeEntityArchetypeBuilder;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3d;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

@Mixin(net.minecraft.world.entity.Entity.class)
@Implements(@Interface(iface = org.spongepowered.api.entity.Entity.class, prefix = "entity$", remap = Remap.NONE))
public abstract class EntityMixin_API implements org.spongepowered.api.entity.Entity {

    // @formatter:off
    @Shadow private float yRot;
    @Shadow private float xRot;
    @Shadow @Final protected net.minecraft.util.RandomSource random;
    @Shadow protected UUID uuid;
    @Shadow @Final private net.minecraft.world.entity.EntityType<?> type;
    @Shadow private Level level;
    @Shadow @Final protected SynchedEntityData entityData;

    @Shadow public abstract double shadow$getX();
    @Shadow public abstract double shadow$getY();
    @Shadow public abstract double shadow$getZ();
    @Shadow public abstract Level shadow$getCommandSenderWorld();
    @Shadow @Nullable public abstract MinecraftServer shadow$getServer();
    @Shadow public abstract boolean shadow$isRemoved();
    @Shadow public abstract UUID shadow$getUUID();
    @Shadow public abstract boolean shadow$hurt(DamageSource source, float amount);
    @Shadow protected abstract void shadow$setRot(float yaw, float pitch);
    @Shadow public abstract net.minecraft.world.phys.AABB shadow$getBoundingBox();
    @Shadow public abstract void shadow$setRemoved(Entity.RemovalReason var1);
    @Shadow public abstract void shadow$discard();
    @Shadow public abstract void shadow$lookAt(EntityAnchorArgument.Anchor param0, Vec3 param1);
    @Shadow public abstract CompoundTag shadow$saveWithoutId(CompoundTag $$0);
    @Shadow public abstract Level shadow$level();
    // @formatter:on

    @Override
    public Source random() {
        return (Source) this.random;
    }

    @Override
    public Vector3d position() {
        return new Vector3d(this.shadow$getX(), this.shadow$getY(), this.shadow$getZ());
    }

    @Override
    public boolean setPosition(final Vector3d position) {
        return ((EntityBridge) this).bridge$setPosition(Objects.requireNonNull(position, "The position was null!"));
    }

    @Override
    public World<?, ?> world() {
        return (World<?, ?>) this.level;
    }

    @Override
    public ServerLocation location() {
        return ServerLocation.of((org.spongepowered.api.world.server.ServerWorld) this.shadow$getCommandSenderWorld(), this.position());
    }

    @Override
    public boolean setLocation(final ServerLocation location) {
        return ((EntityBridge) this).bridge$setLocation(Objects.requireNonNull(location, "The location was null!"));
    }

    @Override
    public boolean setLocationAndRotation(final ServerLocation location, final Vector3d rotation) {
        if (this.setLocation(location)) {
            this.setRotation(rotation);
            return true;
        }
        return false;
    }

    @Override
    public Vector3d scale() {
        return Vector3d.ONE;
    }

    @Override
    public void setScale(final Vector3d scale) {
        // do nothing, Minecraft doesn't properly support this yet
    }

    @Override
    public Transform transform() {
        return Transform.of(this.position(), this.rotation(), this.scale());
    }

    @Override
    public boolean setTransform(final Transform transform) {
        if (!PhaseTracker.SERVER.onSidedThread()) {
            return false;
        }

        Objects.requireNonNull(transform, "The transform cannot be null!");
        if (((EntityBridge) this).bridge$setPosition(transform.position())) {
            this.setRotation(transform.rotation());
            this.setScale(transform.scale());
            return true;
        }

        return false;
    }

    @Override
    public boolean transferToWorld(final org.spongepowered.api.world.server.ServerWorld world, final Vector3d position) {
        Objects.requireNonNull(world, "World was null!");
        Objects.requireNonNull(position, "Position was null!");
        return this.setLocation(ServerLocation.of(world, position));
    }

    @Override
    public Vector3d rotation() {
        return new Vector3d(this.xRot, this.yRot, 0);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void setRotation(final Vector3d rotation) {
        Objects.requireNonNull(rotation, "Rotation was null!");
        if (this.isRemoved()) {
            return;
        }
        if (((Entity) (Object) this) instanceof ServerPlayer && ((ServerPlayer) (Object) this).connection != null) {
            // Force an update, this also set the rotation in this entity
            ((ServerPlayer) (Object) this).connection.teleport(this.position().x(), this.position().y(),
                    this.position().z(), (float) rotation.y(), (float) rotation.x(), EnumSet.noneOf(RelativeMovement.class));
        } else {
            // Let the entity tracker do its job, this just updates the variables
            this.shadow$setRot((float) rotation.y(), (float) rotation.x());
        }
    }

    @Override
    public Optional<AABB> boundingBox() {
        final net.minecraft.world.phys.AABB boundingBox = this.shadow$getBoundingBox();
        if (boundingBox == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(VecHelper.toSpongeAABB(boundingBox));
        } catch (final IllegalArgumentException exception) {
            // Bounding box is degenerate, the entity doesn't actually have one
            return Optional.empty();
        }
    }

    @Intrinsic
    public boolean entity$isRemoved() {
        return this.shadow$isRemoved();
    }

    @Override
    public boolean isLoaded() {
        // TODO - add flag for entities loaded/unloaded into world
        return !this.shadow$isRemoved();
    }

    @Intrinsic
    public void entity$remove() {
        this.shadow$setRemoved(Entity.RemovalReason.DISCARDED);
    }

    @Override
    public boolean damage(final double damage, final org.spongepowered.api.event.cause.entity.damage.source.DamageSource damageSource) {
        if (!(damageSource instanceof DamageSource)) {
            SpongeCommon.logger().error("An illegal DamageSource was provided in the cause! The damage source must extend AbstractDamageSource!");
            return false;
        }
        // Causes at this point should already be pushed from plugins before this point with the cause system.
        return this.shadow$hurt((DamageSource) damageSource, (float) damage);
    }

    @Override
    public EntityType type() {
        return (EntityType) this.type;
    }

    @Override
    public UUID uniqueId() {
        return this.uuid;
    }

    @Override
    public int contentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        final Registry<net.minecraft.world.entity.EntityType<?>> entityTypeRegistry = SpongeCommon.vanillaRegistry(Registries.ENTITY_TYPE);
        final CompoundTag compound = new CompoundTag();
        compound.putString("id", entityTypeRegistry.getKey((net.minecraft.world.entity.EntityType<?>) this.type()).toString());
        this.shadow$saveWithoutId(compound);
        final DataContainer unsafeNbt = NBTTranslator.INSTANCE.translateFrom(compound);
        final DataContainer container = DataContainer.createNew()
                .set(Queries.CONTENT_VERSION, this.contentVersion())
                .set(Constants.Entity.CLASS, this.getClass().getName())
                .set(Queries.WORLD_KEY, ((org.spongepowered.api.world.server.ServerWorld) this.world()).key().formatted())
                .createView(Constants.Sponge.SNAPSHOT_WORLD_POSITION)
                .set(Queries.POSITION_X, this.position().x())
                .set(Queries.POSITION_Y, this.position().y())
                .set(Queries.POSITION_Z, this.position().z())
                .container()
                .createView(Constants.Entity.ROTATION)
                .set(Queries.POSITION_X, this.rotation().x())
                .set(Queries.POSITION_Y, this.rotation().y())
                .set(Queries.POSITION_Z, this.rotation().z())
                .container()
                .createView(Constants.Entity.SCALE)
                .set(Queries.POSITION_X, this.scale().x())
                .set(Queries.POSITION_Y, this.scale().y())
                .set(Queries.POSITION_Z, this.scale().z())
                .container()
                .set(Constants.Entity.TYPE, entityTypeRegistry.getKey((net.minecraft.world.entity.EntityType<?>) this.type()))
                .set(Constants.Sponge.UNSAFE_NBT, unsafeNbt);
        return container;
    }

    @Override
    public org.spongepowered.api.entity.Entity copy() {
        if ((Object) this instanceof Player) {
            throw new IllegalArgumentException("Cannot copy player entities!");
        }
        try {
            final Registry<net.minecraft.world.entity.EntityType<?>> entityTypeRegistry = SpongeCommon.vanillaRegistry(Registries.ENTITY_TYPE);
            final CompoundTag compound = new CompoundTag();
            compound.putString("id", entityTypeRegistry.getKey((net.minecraft.world.entity.EntityType<?>) this.type()).toString());
            this.shadow$saveWithoutId(compound);
            final Entity entity = net.minecraft.world.entity.EntityType.loadEntityRecursive(compound, this.shadow$getCommandSenderWorld(), (createdEntity) -> {
                createdEntity.setUUID(UUID.randomUUID());
                return createdEntity;
            });
            return (org.spongepowered.api.entity.Entity) entity;
        } catch (final Exception e) {
            throw new IllegalArgumentException("Could not copy the entity:", e);
        }
    }

    @Override
    public boolean canSee(final org.spongepowered.api.entity.Entity entity) {
        return !((VanishableBridge) entity).bridge$vanishState().invisible();
    }

    @Override
    public void lookAt(final Vector3d targetPos) {
        final Vec3 vec = VecHelper.toVanillaVector3d(targetPos);
        // TODO Should we expose FEET to the API?
        this.shadow$lookAt(EntityAnchorArgument.Anchor.EYES, vec);
    }

    @Override
    public Set<Value.Immutable<?>> getValues() {
        // TODO: Merge custom and Vanilla values and return the merged result.
        return this.api$getVanillaValues();
    }

    @Override
    public EntitySnapshot createSnapshot() {
        return EntitySnapshot.builder().from(this).build();
    }

    @Override
    public EntityArchetype createArchetype() {
        return SpongeEntityArchetypeBuilder.pooled().from(this).build();
    }

    @Override
    public HoverEvent<HoverEvent.ShowEntity> asHoverEvent(final UnaryOperator<HoverEvent.ShowEntity> op) {
        final Registry<net.minecraft.world.entity.EntityType<?>> entityTypeRegistry = SpongeCommon.vanillaRegistry(Registries.ENTITY_TYPE);
        final ResourceLocation entityTypeKey = entityTypeRegistry.getKey((net.minecraft.world.entity.EntityType<?>) this.type());
        return HoverEvent.showEntity(op.apply(HoverEvent.ShowEntity.of((Key) (Object) entityTypeKey, this.uniqueId(), this.displayName().get())));
    }

    protected Set<Value.Immutable<?>> api$getVanillaValues() {
        final Set<Value.Immutable<?>> values = new HashSet<>();

        values.add(this.requireValue(Keys.AGE).asImmutable());
        values.add(this.requireValue(Keys.BASE_SIZE).asImmutable());
        values.add(this.requireValue(Keys.DISPLAY_NAME).asImmutable());
        values.add(this.requireValue(Keys.EYE_HEIGHT).asImmutable());
        values.add(this.requireValue(Keys.EYE_POSITION).asImmutable());
        values.add(this.requireValue(Keys.FALL_DISTANCE).asImmutable());
        values.add(this.requireValue(Keys.FIRE_DAMAGE_DELAY).asImmutable());
        values.add(this.requireValue(Keys.FROZEN_TIME).asImmutable());
        values.add(this.requireValue(Keys.HEIGHT).asImmutable());
        values.add(this.requireValue(Keys.INVULNERABILITY_TICKS).asImmutable());
        values.add(this.requireValue(Keys.INVULNERABLE).asImmutable());
        values.add(this.requireValue(Keys.IS_CUSTOM_NAME_VISIBLE).asImmutable());
        values.add(this.requireValue(Keys.IS_GLOWING).asImmutable());
        values.add(this.requireValue(Keys.IS_GRAVITY_AFFECTED).asImmutable());
        values.add(this.requireValue(Keys.IS_INVISIBLE).asImmutable());
        values.add(this.requireValue(Keys.IS_SILENT).asImmutable());
        values.add(this.requireValue(Keys.IS_SNEAKING).asImmutable());
        values.add(this.requireValue(Keys.IS_SPRINTING).asImmutable());
        values.add(this.requireValue(Keys.IS_WET).asImmutable());
        values.add(this.requireValue(Keys.MAX_AIR).asImmutable());
        values.add(this.requireValue(Keys.MAX_FROZEN_TIME).asImmutable());
        values.add(this.requireValue(Keys.ON_GROUND).asImmutable());
        values.add(this.requireValue(Keys.PASSENGERS).asImmutable());
        values.add(this.requireValue(Keys.REMAINING_AIR).asImmutable());
        values.add(this.requireValue(Keys.SCALE).asImmutable());
        values.add(this.requireValue(Keys.SCOREBOARD_TAGS).asImmutable());
        values.add(this.requireValue(Keys.TRANSIENT).asImmutable());
        values.add(this.requireValue(Keys.VANISH_STATE).asImmutable());
        values.add(this.requireValue(Keys.VELOCITY).asImmutable());

        this.getValue(Keys.BASE_VEHICLE).map(Value::asImmutable).ifPresent(values::add);
        this.getValue(Keys.CREATOR).map(Value::asImmutable).ifPresent(values::add);
        this.getValue(Keys.CUSTOM_NAME).map(Value::asImmutable).ifPresent(values::add);
        this.getValue(Keys.FIRE_TICKS).map(Value::asImmutable).ifPresent(values::add);
        this.getValue(Keys.NOTIFIER).map(Value::asImmutable).ifPresent(values::add);
        this.getValue(Keys.SWIFTNESS).map(Value::asImmutable).ifPresent(values::add);
        this.getValue(Keys.VEHICLE).map(Value::asImmutable).ifPresent(values::add);

        return values;
    }

    @Override
    public net.kyori.adventure.text.Component teamRepresentation() {
        return net.kyori.adventure.text.Component.text(this.shadow$getUUID().toString());
    }
}
