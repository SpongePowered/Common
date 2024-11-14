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
package org.spongepowered.common.entity.projectile;

import com.google.common.collect.Maps;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.entity.carrier.Dispenser;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.golem.Shulker;
import org.spongepowered.api.entity.projectile.Egg;
import org.spongepowered.api.entity.projectile.EnderPearl;
import org.spongepowered.api.entity.projectile.ExperienceBottle;
import org.spongepowered.api.entity.projectile.FishingBobber;
import org.spongepowered.api.entity.projectile.LlamaSpit;
import org.spongepowered.api.entity.projectile.Potion;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.Snowball;
import org.spongepowered.api.entity.projectile.arrow.Arrow;
import org.spongepowered.api.entity.projectile.arrow.SpectralArrow;
import org.spongepowered.api.entity.projectile.explosive.FireworkRocket;
import org.spongepowered.api.entity.projectile.explosive.WitherSkull;
import org.spongepowered.api.entity.projectile.explosive.fireball.DragonFireball;
import org.spongepowered.api.entity.projectile.explosive.fireball.ExplosiveFireball;
import org.spongepowered.api.entity.projectile.explosive.fireball.SmallFireball;
import org.spongepowered.api.projectile.source.ProjectileSource;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.common.util.Constants;
import org.spongepowered.math.vector.Vector3d;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class ProjectileUtil {

    private static final Map<EntityType<? extends Projectile>, ProjectileLogic<?>> projectileLogic = Maps.newHashMap();
    private static final Map<Class<? extends ProjectileSource>, ProjectileSourceLogic<?>> projectileSourceLogic = Maps.newHashMap();

    public static <T extends Projectile> Optional<T> launch(final EntityType<T> projectileType, final ProjectileSource source,
            final @Nullable Vector3d vel) {
        final ProjectileLogic<T> logic = ProjectileUtil.getLogic(projectileType);
        if (logic == null) {
            return Optional.empty();
        }
        final Optional<T> projectile = logic.launch(source);
        projectile.ifPresent(t -> {
            if (vel != null) {
                t.offer(Keys.VELOCITY, vel);
            }
            t.offer(Keys.SHOOTER, source);
        });
        return projectile;
    }

    public static <T extends Projectile, S extends ProjectileSource> Optional<T> launchWithArgs(final EntityType<T> projectileType,
            final Class<S> projectileSourceClass, final S source, final @Nullable Vector3d vel, final Object... args) {
        final ProjectileSourceLogic<S> sourceLogic = ProjectileUtil.getSourceLogic(projectileSourceClass);
        if (sourceLogic == null) {
            return Optional.empty();
        }

        final ProjectileLogic<T> logic = ProjectileUtil.getLogic(projectileType);
        if (logic == null) {
            return Optional.empty();
        }

        final Optional<T> projectile = sourceLogic.launch(logic, source, projectileType, args);
        projectile.ifPresent(t -> {
            if (vel != null) {
                t.offer(Keys.VELOCITY, vel);
            }
            t.offer(Keys.SHOOTER, source);
        });
        return projectile;
    }

    // From ThrowableEntity constructor
    private static void configureThrowable(final ThrowableProjectile entity) {
        final double x = entity.getX() - Mth.cos(entity.getYRot() / 180.0F * (float) Math.PI) * 0.16F;
        final double y = entity.getY() - 0.1D;
        final double z = entity.getZ() - Mth.sin(entity.getYRot() / 180.0F * (float) Math.PI) * 0.16F;
        entity.setPos(x, y, z);
        final float f = 0.4F;
        final double motionX = -Mth.sin(entity.getYRot() / 180.0F * (float) Math.PI)
                * Mth.cos(entity.getXRot() / 180.0F * (float) Math.PI) * f;
        final double motionZ = Mth.cos(entity.getYRot() / 180.0F * (float) Math.PI)
                * Mth.cos(entity.getXRot() / 180.0F * (float) Math.PI) * f;
        final double motionY = -Mth.sin((entity.getXRot()) / 180.0F * (float) Math.PI) * f;
        entity.setDeltaMovement(motionX, motionY, motionZ);
    }

    public static <T extends Projectile> void registerProjectileLogic(final Supplier<EntityType<T>> projectileType, final ProjectileLogic<T> logic) {
        ProjectileUtil.projectileLogic.put(projectileType.get(), logic);
    }

    public static <T extends ProjectileSource> void registerProjectileSourceLogic(final Class<T> projectileSourceClass,
            final ProjectileSourceLogic<T> logic) {
        ProjectileUtil.projectileSourceLogic.put(projectileSourceClass, logic);
    }

    @SuppressWarnings("unchecked")
    static <T extends ProjectileSource> ProjectileSourceLogic<T> getSourceLogic(final Class<T> sourceClass) {
        return (ProjectileSourceLogic<T>) ProjectileUtil.projectileSourceLogic.get(sourceClass);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Projectile> ProjectileLogic<T> getLogic(final EntityType<T> projectileType) {
        return (ProjectileLogic<T>) ProjectileUtil.projectileLogic.get(projectileType);
    }

    @SuppressWarnings("unchecked")
    static <P extends Projectile> Optional<P> defaultLaunch(final ProjectileSource source,
            final EntityType<P> projectileType, final ServerLocation loc) {
        final Entity projectile = loc.world().createEntity(projectileType, loc.position());
        if (projectile instanceof ThrowableProjectile) {
            ProjectileUtil.configureThrowable((ThrowableProjectile) projectile);
        }
        return ProjectileUtil.doLaunch(loc.world(), (P) projectile);
    }

    static <P extends Projectile> Optional<P> doLaunch(final World extent, final P projectile) {
        if (extent.spawnEntity(projectile)) {
            return Optional.of(projectile);
        }
        return Optional.empty();
    }

    static {
        ProjectileUtil.registerProjectileSourceLogic(Dispenser.class, new DispenserSourceLogic());

        ProjectileUtil.registerProjectileSourceLogic(Shulker.class, new ShulkerSourceLogic());

        ProjectileUtil.registerProjectileLogic(EntityTypes.ARROW, new SimpleItemLaunchLogic<Arrow>(EntityTypes.ARROW, Items.ARROW) {

            @Override
            protected Optional<Arrow> createProjectile(final LivingEntity source, final ServerLocation loc) {
                final var arrow = new net.minecraft.world.entity.projectile.Arrow(source.level(), source, new ItemStack(this.item), source.getWeaponItem());
                arrow.shoot(source.getXRot(), source.getYRot(), 0.0F, 3.0F, 0);
                return ProjectileUtil.doLaunch(loc.world(), (Arrow) arrow);
            }
        });
        ProjectileUtil.registerProjectileLogic(EntityTypes.SPECTRAL_ARROW, new SimpleItemLaunchLogic<SpectralArrow>(
                EntityTypes.SPECTRAL_ARROW, Items.SPECTRAL_ARROW) {

            @Override
            protected Optional<SpectralArrow> createProjectile(final LivingEntity source, final ServerLocation loc) {
                final var arrow = new net.minecraft.world.entity.projectile.SpectralArrow(source.level(), source, new ItemStack(this.item), source.getWeaponItem());
                arrow.shoot(source.getXRot(), source.getYRot(), 0.0F, 3.0F, 0);
                return ProjectileUtil.doLaunch(loc.world(), (SpectralArrow) arrow);
            }
        });
        ProjectileUtil.registerProjectileLogic(EntityTypes.EGG, new SimpleItemLaunchLogic<Egg>(EntityTypes.EGG, Items.EGG) {

            @Override
            protected Optional<Egg> createProjectile(final LivingEntity source, final ServerLocation loc) {
                final ThrownEgg egg = new ThrownEgg(source.level(), source, new ItemStack(Items.EGG));
                egg.shoot(source.getXRot(), source.getYRot(), 0.0F, 1.5F, 0);
                return ProjectileUtil.doLaunch(loc.world(), (Egg) egg);
            }
        });
        ProjectileUtil.registerProjectileLogic(EntityTypes.SMALL_FIREBALL, new SimpleItemLaunchLogic<SmallFireball>(
                EntityTypes.SMALL_FIREBALL, Items.FIRE_CHARGE) {

            @Override
            protected Optional<SmallFireball> createProjectile(final LivingEntity source, final ServerLocation loc) {
                final net.minecraft.world.phys.Vec3 lookVec = source.getViewVector(1);
                final var fireball = new net.minecraft.world.entity.projectile.SmallFireball(source.level(), source, lookVec.scale(4));
                fireball.setPos(fireball.getX(), fireball.getY() + source.getEyeHeight(), fireball.getZ());
                return ProjectileUtil.doLaunch(loc.world(), (SmallFireball) fireball);
            }
        });
        ProjectileUtil.registerProjectileLogic(EntityTypes.FIREWORK_ROCKET, new SimpleItemLaunchLogic<FireworkRocket>(
                EntityTypes.FIREWORK_ROCKET, Items.FIREWORK_ROCKET) {

            @Override
            protected Optional<FireworkRocket> createProjectile(final LivingEntity source, final ServerLocation loc) {
                final FireworkRocketEntity firework = new FireworkRocketEntity(source.level(), loc.x(), loc.y(), loc.z(), ItemStack.EMPTY);
                return ProjectileUtil.doLaunch(loc.world(), (FireworkRocket) firework);
            }
        });
        ProjectileUtil.registerProjectileLogic(EntityTypes.SNOWBALL, new SimpleItemLaunchLogic<Snowball>(EntityTypes.SNOWBALL, Items.SNOWBALL) {

            @Override
            protected Optional<Snowball> createProjectile(final LivingEntity source, final ServerLocation loc) {
                final net.minecraft.world.entity.projectile.Snowball snowball = new net.minecraft.world.entity.projectile.Snowball(
                    source.level(), source, new ItemStack(Items.SNOWBALL));
                snowball.shoot(source.getXRot(), source.getYRot(), 0.0F, 1.5F, 0);
                return ProjectileUtil.doLaunch(loc.world(), (Snowball) snowball);
            }
        });
        ProjectileUtil.registerProjectileLogic(EntityTypes.EXPERIENCE_BOTTLE, new SimpleItemLaunchLogic<ExperienceBottle>(
                EntityTypes.EXPERIENCE_BOTTLE, Items.EXPERIENCE_BOTTLE) {

            @Override
            protected Optional<ExperienceBottle> createProjectile(final LivingEntity source, final ServerLocation loc) {
                final ThrownExperienceBottle expBottle = new ThrownExperienceBottle(source.level(), source, new ItemStack(Items.EXPERIENCE_BOTTLE));
                expBottle.shoot(source.getXRot(), source.getYRot(), -20.0F, 0.7F, 0);
                return ProjectileUtil.doLaunch(loc.world(), (ExperienceBottle) expBottle);
            }
        });

        ProjectileUtil.registerProjectileLogic(EntityTypes.ENDER_PEARL, new SimpleItemLaunchLogic<EnderPearl>(
                EntityTypes.ENDER_PEARL, Items.ENDER_PEARL) {

            @Override
            protected Optional<EnderPearl> createProjectile(final LivingEntity source, final ServerLocation loc) {
                final ThrownEnderpearl pearl = new ThrownEnderpearl(source.level(), source, new ItemStack(Items.ENDER_PEARL));
                pearl.shoot(source.getXRot(), source.getYRot(), 0.0F, 1.5F, 0);
                return ProjectileUtil.doLaunch(loc.world(), (EnderPearl) pearl);
            }
        });
        ProjectileUtil.registerProjectileLogic(EntityTypes.FIREBALL, new SimpleDispenserLaunchLogic<ExplosiveFireball>(EntityTypes.FIREBALL) {

            @Override
            protected Optional<ExplosiveFireball> createProjectile(final LivingEntity source, final ServerLocation loc) {
                final net.minecraft.world.phys.Vec3 lookVec = source.getViewVector(1);
                final LargeFireball fireball = new LargeFireball(source.level(), source, lookVec.scale(4), Constants.Entity.Fireball.DEFAULT_EXPLOSION_RADIUS);
                fireball.setPos(fireball.getX(), fireball.getY() + source.getEyeHeight(), fireball.getZ());
                return ProjectileUtil.doLaunch(loc.world(), (ExplosiveFireball) fireball);
            }

            @Override
            public Optional<ExplosiveFireball> createProjectile(final ProjectileSource source, final EntityType<ExplosiveFireball> projectileType,
                    final ServerLocation loc) {
                if (!(source instanceof DispenserBlockEntity)) {
                    return super.createProjectile(source, projectileType, loc);
                }
                final DispenserBlockEntity dispenser = (DispenserBlockEntity) source;
                final Direction enumfacing = DispenserSourceLogic.getFacing(dispenser);
                final LivingEntity thrower = new ArmorStand(dispenser.getLevel(), loc.x() + enumfacing.getStepX(), loc.y() + enumfacing.getStepY(), loc.z() + enumfacing.getStepZ());
                final LargeFireball fireball = new LargeFireball(dispenser.getLevel(), thrower, Vec3.ZERO, Constants.Entity.Fireball.DEFAULT_EXPLOSION_RADIUS);
                // Acceleration is set separately because the constructor applies a random value to it
                // As for 0.1;  it is a reasonable default value
                fireball.accelerationPower = 0.1;
                return ProjectileUtil.doLaunch(loc.world(), (ExplosiveFireball) fireball);
            }
        });
        ProjectileUtil.registerProjectileLogic(EntityTypes.WITHER_SKULL, new SimpleDispenserLaunchLogic<WitherSkull>(EntityTypes.WITHER_SKULL) {

            @Override
            protected Optional<WitherSkull> createProjectile(final LivingEntity source, final ServerLocation loc) {
                final net.minecraft.world.phys.Vec3 lookVec = source.getViewVector(1);
                final var skull = new net.minecraft.world.entity.projectile.WitherSkull(source.level(), source, lookVec.scale(4));
                skull.setPos(skull.getX(), skull.getY() + source.getEyeHeight(), skull.getZ());
                return ProjectileUtil.doLaunch(loc.world(), (WitherSkull) skull);
            }
        });
        ProjectileUtil.registerProjectileLogic(EntityTypes.EYE_OF_ENDER, new SimpleDispenserLaunchLogic<>(EntityTypes.EYE_OF_ENDER));
        ProjectileUtil.registerProjectileLogic(EntityTypes.FISHING_BOBBER, new SimpleDispenserLaunchLogic<FishingBobber>(
                EntityTypes.FISHING_BOBBER) {

            @Override
            protected Optional<FishingBobber> createProjectile(final LivingEntity source, final ServerLocation loc) {
                if (source instanceof Player) {
                    final FishingHook hook = new FishingHook((Player) source, source.level(), 0, 0);
                    hook.setPos(loc.x(), loc.y(), loc.z());
                    return ProjectileUtil.doLaunch(loc.world(), (FishingBobber) hook);
                }
                return super.createProjectile(source, loc);
            }
        });
        ProjectileUtil.registerProjectileLogic(EntityTypes.POTION, new SimpleItemLaunchLogic<Potion>(EntityTypes.POTION, Items.SPLASH_POTION) {

            @Override
            protected Optional<Potion> createProjectile(final LivingEntity source, final ServerLocation loc) {
                final ThrownPotion potion = new ThrownPotion(source.level(), source, new ItemStack(this.item));
                potion.setItem(new ItemStack(Items.SPLASH_POTION, 1));
                potion.shoot(source.getXRot(), source.getYRot(), -20.0F, 0.5F, 0);
                return ProjectileUtil.doLaunch(loc.world(), (Potion) potion);
            }
        });
        ProjectileUtil.registerProjectileLogic(EntityTypes.LLAMA_SPIT, new SimpleEntityLaunchLogic<LlamaSpit>(EntityTypes.LLAMA_SPIT) {

            @Override
            public Optional<LlamaSpit> launch(final ProjectileSource source) {
                if (!(source instanceof Llama)) {
                    return Optional.empty();
                }
                return super.launch(source);
            }

            @Override
            public Optional<LlamaSpit> createProjectile(final ProjectileSource source,
                    final EntityType<LlamaSpit> projectileType, final ServerLocation loc) {
                final Llama llama = (Llama) source;
                final net.minecraft.world.entity.projectile.LlamaSpit llamaSpit = new net.minecraft.world.entity.projectile.LlamaSpit(llama.level(), (Llama) source);
                final net.minecraft.world.phys.Vec3 lookVec = llama.getViewVector(1);
                llamaSpit.shoot(lookVec.x, lookVec.y, lookVec.z, 1.5F, 0);
                return ProjectileUtil.doLaunch(loc.world(), (LlamaSpit) llamaSpit);
            }
        });
        ProjectileUtil.registerProjectileLogic(EntityTypes.DRAGON_FIREBALL, new SimpleDispenserLaunchLogic<DragonFireball>(
                EntityTypes.DRAGON_FIREBALL) {

            @Override
            protected Optional<DragonFireball> createProjectile(final LivingEntity source, final ServerLocation loc) {
                final net.minecraft.world.phys.Vec3 lookVec = source.getViewVector(1);
                final var fireball = new net.minecraft.world.entity.projectile.DragonFireball(source.level(), source, lookVec.scale(4));
                fireball.setPos(fireball.getX(), fireball.getY() + source.getEyeHeight(), fireball.getZ());
                return ProjectileUtil.doLaunch(loc.world(), (DragonFireball) fireball);
            }
        });
        ProjectileUtil.registerProjectileLogic(EntityTypes.SHULKER_BULLET, new SimpleEntityLaunchLogic<>(EntityTypes.SHULKER_BULLET));
    }
}
