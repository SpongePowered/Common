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
package org.spongepowered.common.data.fixer.entity;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.datafix.fixes.References;
import org.spongepowered.common.data.fixer.world.SpongeLevelFixer;
import org.spongepowered.common.util.Constants;

public final class EntityTrackedUser extends DataFix {

    public EntityTrackedUser(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected TypeRewriteRule makeRule() {
        final Type<?> entityType = this.getInputSchema().getType(References.ENTITY);
        final Type<?> forgeDataType = entityType.findFieldType(Constants.Forge.FORGE_DATA);
        final Type<?> spongeDataType = forgeDataType.findFieldType(Constants.Sponge.Data.V2.SPONGE_DATA);

        final OpticFinder<?> forgeDataFinder = DSL.fieldFinder(Constants.Forge.FORGE_DATA, forgeDataType);
        final OpticFinder<?> spongeDataFinder = DSL.fieldFinder(Constants.Sponge.Data.V2.SPONGE_DATA, spongeDataType);

        return TypeRewriteRule.seq(this.fixTracked(forgeDataFinder, spongeDataFinder, spongeDataType, Constants.Sponge.SPONGE_ENTITY_CREATOR),
                                   this.fixTracked(forgeDataFinder, spongeDataFinder, spongeDataType, Constants.Sponge.SPONGE_ENTITY_NOTIFIER));
    }

    private TypeRewriteRule fixTracked(OpticFinder<?> forgeDataFinder, OpticFinder<?> spongeDataFinder, Type<?> spongeDataType, String name) {
        final Type<?> trackedType = spongeDataType.findFieldType(name);
        final OpticFinder<?> trackedFinder = DSL.fieldFinder(name, trackedType);

        return this.fixTypeEverywhereTyped("Entity" + name + "UserFix", this.getInputSchema().getType(References.ENTITY),
                type -> {
                    final Typed<?> forge = type.getTyped(forgeDataFinder);
                    final Typed<?> sponge = forge.getTyped(spongeDataFinder);
                    return SpongeLevelFixer.updateUUIDIn(sponge.getTyped(trackedFinder));
                });
    }

}
