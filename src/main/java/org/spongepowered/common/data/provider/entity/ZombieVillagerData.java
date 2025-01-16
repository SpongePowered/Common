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

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.ProfessionType;
import org.spongepowered.api.data.type.VillagerType;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.data.provider.DataProviderRegistrator;

public final class ZombieVillagerData {

    private ZombieVillagerData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(ZombieVillager.class)
                    .create(Keys.PROFESSION_LEVEL)
                        .get(h -> h.getVillagerData().level())
                        .set((h, v) -> h.setVillagerData(h.getVillagerData().withLevel(v)))
                    .create(Keys.PROFESSION_TYPE)
                        .get(h -> (ProfessionType) (Object) h.getVillagerData().profession())
                        .set((h, v) -> {
                            final var villagerProfession = SpongeCommon.vanillaRegistry(Registries.VILLAGER_PROFESSION).wrapAsHolder((VillagerProfession) (Object) v);
                            h.setVillagerData(h.getVillagerData().withProfession(villagerProfession));
                        })
                    .create(Keys.VILLAGER_TYPE)
                        .get(h -> (VillagerType) (Object) h.getVillagerData().type())
                        .set((h, v) -> {
                            final var villagerType = SpongeCommon.vanillaRegistry(Registries.VILLAGER_TYPE).wrapAsHolder((net.minecraft.world.entity.npc.VillagerType) (Object) v);
                            h.setVillagerData(h.getVillagerData().withType(villagerType));
                        });
    }
    // @formatter:on
}
