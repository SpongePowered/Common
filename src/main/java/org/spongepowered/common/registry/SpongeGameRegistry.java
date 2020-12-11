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
package org.spongepowered.common.registry;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.AdventureRegistry;
import org.spongepowered.api.item.merchant.VillagerRegistry;
import org.spongepowered.api.item.recipe.RecipeRegistry;
import org.spongepowered.api.registry.FactoryProvider;
import org.spongepowered.api.registry.GameRegistry;
import org.spongepowered.common.adventure.AdventureRegistryImpl;

@Singleton
public final class SpongeGameRegistry implements GameRegistry {

    private final SpongeCatalogRegistry catalogRegistry;
    private final SpongeBuilderProvider builderRegistry;
    private final SpongeFactoryProvider factoryRegistry;

    @Inject
    public SpongeGameRegistry(final SpongeCatalogRegistry catalogRegistry, final SpongeBuilderProvider builderRegistry,
        final SpongeFactoryProvider factoryRegistry) {
        this.catalogRegistry = catalogRegistry;
        this.builderRegistry = builderRegistry;
        this.factoryRegistry = factoryRegistry;
    }

    @Override
    public SpongeCatalogRegistry getCatalogRegistry() {
        return this.catalogRegistry;
    }

    @Override
    public AdventureRegistry getAdventureRegistry() {
        return AdventureRegistryImpl.INSTANCE;
    }

    @Override
    public SpongeBuilderProvider getBuilderRegistry() {
        return this.builderRegistry;
    }

    @Override
    public FactoryProvider getFactoryRegistry() {
        return this.factoryRegistry;
    }

    @Override
    public RecipeRegistry getRecipeRegistry() {
        return ((RecipeRegistry) ((MinecraftServer) Sponge.getServer()).getRecipeManager());
    }

    @Override
    public VillagerRegistry getVillagerRegistry() {
        return SpongeVillagerRegistry.getInstance();
    }
}
