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
package org.spongepowered.common.mixin.core.util;

import com.google.common.base.MoreObjects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Explosion;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.cause.entity.damage.DamageType;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.accessor.world.ExplosionAccessor;
import org.spongepowered.common.bridge.CreatorTrackedBridge;
import org.spongepowered.common.bridge.util.DamageSourceBridge;
import org.spongepowered.common.bridge.world.WorldBridge;
import org.spongepowered.common.registry.provider.DamageSourceToTypeProvider;
import org.spongepowered.common.util.MemoizedSupplier;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@Mixin(DamageSource.class)
public abstract class DamageSourceMixin implements DamageSourceBridge {

    // @formatter:off
    @Shadow @Final @Mutable public static DamageSource LAVA;
    @Shadow @Final @Mutable public static DamageSource IN_FIRE;
    @Shadow @Final @Mutable public static DamageSource LIGHTNING_BOLT;
    @Shadow @Final @Mutable public static DamageSource HOT_FLOOR;
    @Shadow @Final @Mutable public static DamageSource FIREWORKS;
    @Shadow @Final @Mutable public static DamageSource ANVIL;
    @Shadow @Final @Mutable public static DamageSource FALLING_BLOCK;
    @Shadow @Final @Mutable public static DamageSource CACTUS;

    @Shadow @Final public String damageType;

    @Shadow @Nullable public abstract Entity shadow$getTrueSource();
    // @formatter:on

    Supplier<DamageType> impl$damageType;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void impl$setDamageTypeOnConstruction(final String damageType, final CallbackInfo ci) {
        this.bridge$resetDamageType();
    }

    @Inject(method = "getDeathMessage", cancellable = true, at = @At(value = "RETURN"))
    private void beforeGetDeathMessageReturn(final LivingEntity livingEntity, final CallbackInfoReturnable<ITextComponent> cir) {
        // This prevents untranslated keys from appearing in death messages, switching out those that are untranslated with the generic message.
        if (cir.getReturnValue().getString().equals("death.attack." + this.damageType)) {
            cir.setReturnValue(new TranslationTextComponent("death.attack.generic", livingEntity.getDisplayName()));
        }
    }

    @Inject(method = "causeExplosionDamage(Lnet/minecraft/world/Explosion;)Lnet/minecraft/util/DamageSource;",
            at = @At("HEAD"),
            cancellable = true)
    private static void onSetExplosionSource(@Nullable final Explosion explosion,
            final CallbackInfoReturnable<net.minecraft.util.DamageSource> cir) {
        if (explosion != null) {
            final Entity entity = ((ExplosionAccessor) explosion).accessor$getExploder();
            if (entity != null && !((WorldBridge) ((ExplosionAccessor) explosion).accessor$getWorld()).bridge$isFake()) {
                if (explosion.getExplosivePlacedBy() == null && entity instanceof CreatorTrackedBridge) {
                    // check creator
                    final CreatorTrackedBridge creatorBridge = (CreatorTrackedBridge) entity;
                    creatorBridge.tracked$getCreatorReference()
                            .filter(user -> user instanceof PlayerEntity)
                            .map(user -> (PlayerEntity) user)
                            .ifPresent(player -> {
                                final IndirectEntityDamageSource damageSource = new IndirectEntityDamageSource("explosion.player", entity, player);
                                damageSource.setDifficultyScaled().setExplosion();
                                cir.setReturnValue(damageSource);
                            });
                }
            }
        }
    }

    @Override
    public DamageType bridge$getDamageType() {
        return this.impl$damageType.get();
    }

    @Override
    public void bridge$resetDamageType() {
        if (!this.damageType.contains(":")) {
            this.impl$damageType = MemoizedSupplier.memoize(() -> DamageSourceToTypeProvider.getInstance().getOrCustom(this.damageType).get());
        } else {
            this.impl$damageType = MemoizedSupplier.memoize(() -> Sponge.getRegistry().getCatalogRegistry().get(DamageType.class, ResourceKey.resolve(this.damageType)).orElseGet(DamageTypes.CUSTOM));
        }
    }

    @Override
    public void bridge$setLava() {
        DamageSourceMixin.LAVA = (DamageSource) (Object) this;
    }

    @Override
    public void bridge$setFireSource() {
        DamageSourceMixin.IN_FIRE = (DamageSource) (Object) this;
    }

    @Override
    public void bridge$setLightningSource() {
        DamageSourceMixin.LIGHTNING_BOLT = (DamageSource) (Object) this;
    }

    @Override
    public void bridge$setHotFloorSource() {
        DamageSourceMixin.HOT_FLOOR = (DamageSource) (Object) this;
    }

    @Override
    public void bridge$setFireworksSource() {
        DamageSourceMixin.FIREWORKS = (DamageSource) (Object) this;
    }

    @Override
    public void bridge$setFallingBlockSource() {
        DamageSourceMixin.FALLING_BLOCK = (DamageSource) (Object) this;
    }

    @Override
    public void bridge$setAnvilSource() {
        DamageSourceMixin.ANVIL = (DamageSource) (Object) this;
    }

    @Override
    public void bridge$setCactusSource() {
        DamageSourceMixin.CACTUS = (DamageSource) (Object) this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper("DamageSource")
                .add("Name", this.damageType)
                .add("Key", this.impl$damageType.get().getKey().toString())
                .toString();
    }
}
