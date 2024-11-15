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
package org.spongepowered.common.event.cause.entity.damage;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DeathMessageType;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.datapack.DataPack;
import org.spongepowered.api.datapack.DataPacks;
import org.spongepowered.api.event.cause.entity.damage.DamageEffect;
import org.spongepowered.api.event.cause.entity.damage.DamageScaling;
import org.spongepowered.api.event.cause.entity.damage.DamageTypeTemplate;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.AbstractDataPackEntryBuilder;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

public record SpongeDamageTypeTemplate(ResourceKey key, DamageType representedType, DataPack<DamageTypeTemplate> pack) implements DamageTypeTemplate {

    @Override
    public org.spongepowered.api.event.cause.entity.damage.DamageType type() {
        return (org.spongepowered.api.event.cause.entity.damage.DamageType) (Object) this.representedType;
    }

    @Override
    public int contentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        final JsonElement serialized = SpongeDamageTypeTemplate.encode(this, SpongeCommon.vanillaRegistryAccess());
        try {
            return DataFormats.JSON.get().read(serialized.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read deserialized DamageType:\n" + serialized, e);
        }
    }

    public static JsonElement encode(final DamageTypeTemplate template, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return DamageType.DIRECT_CODEC.encodeStart(ops, (DamageType) (Object) template.type()).getOrThrow();
    }

    public static DamageType decode(final JsonElement json, final RegistryAccess registryAccess) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return DamageType.DIRECT_CODEC.parse(ops, json).getOrThrow();
    }

    public static DamageTypeTemplate decode(final DataPack<DamageTypeTemplate> pack, final ResourceKey key, final JsonElement packEntry,
            final RegistryAccess registryAccess) {
        final DamageType parsed = SpongeDamageTypeTemplate.decode(packEntry, registryAccess);
        return new SpongeDamageTypeTemplate(key, parsed, pack);
    }

    public static final class BuilderImpl
            extends AbstractDataPackEntryBuilder<org.spongepowered.api.event.cause.entity.damage.DamageType, DamageTypeTemplate, Builder>
            implements Builder {

        private String msgId;
        private net.minecraft.world.damagesource.DamageScaling scaling;
        private float exhaustion;
        private DamageEffects effects;
        private DeathMessageType deathMessageType;

        public BuilderImpl() {
            this.reset();
        }

        @Override
        public Builder fromValue(final org.spongepowered.api.event.cause.entity.damage.DamageType value) {
            var mcValue = (DamageType) (Object) value;
            this.msgId = mcValue.msgId();
            this.exhaustion = mcValue.exhaustion();
            this.scaling = mcValue.scaling();
            this.effects = mcValue.effects();
            this.deathMessageType = mcValue.deathMessageType();
            return this;
        }

        @Override
        public Builder name(final String name) {
            this.msgId = name;
            return this;
        }

        @Override
        public Builder scaling(final DamageScaling scaling) {
            this.scaling = (net.minecraft.world.damagesource.DamageScaling) (Object) scaling;
            return this;
        }

        @Override
        public Builder exhaustion(final double exhaustion) {
            this.exhaustion = (float) exhaustion;
            return this;
        }

        @Override
        public Builder effect(final DamageEffect effect) {
            this.effects = ((DamageEffects) (Object) effect);
            return this;
        }

        @Override
        public Function<DamageTypeTemplate, org.spongepowered.api.event.cause.entity.damage.DamageType> valueExtractor() {
            return DamageTypeTemplate::type;
        }

        @Override
        public Builder reset() {
            this.key = null;
            this.pack = DataPacks.DAMAGE_TYPE;
            this.msgId = null;
            this.scaling = net.minecraft.world.damagesource.DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER;
            this.effects = DamageEffects.HURT;
            this.deathMessageType = DeathMessageType.DEFAULT;
            return this;
        }

        @Override
        public Builder fromDataPack(final DataView pack) throws IOException {
            final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(pack));
            final DamageType damageType = SpongeDamageTypeTemplate.decode(json, SpongeCommon.vanillaRegistryAccess());
            this.fromValue((org.spongepowered.api.event.cause.entity.damage.DamageType) (Object) damageType);
            return this;
        }

        @Override
        protected DamageTypeTemplate build0() {
            Objects.requireNonNull(this.msgId, "name");
            Objects.requireNonNull(this.scaling, "scaling");
            Objects.requireNonNull(this.effects, "effects");
            Objects.requireNonNull(this.deathMessageType, "deathMessageType");
            final DamageType damageType = new DamageType(this.msgId, this.scaling, this.exhaustion, this.effects, this.deathMessageType);
            return new SpongeDamageTypeTemplate(this.key, damageType, this.pack);
        }
    }
}
