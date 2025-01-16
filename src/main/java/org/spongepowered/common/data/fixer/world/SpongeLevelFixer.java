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
package org.spongepowered.common.data.fixer.world;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List;
import net.minecraft.util.datafix.fixes.References;
import org.spongepowered.common.util.Constants;

public final class SpongeLevelFixer extends DataFix {

    public SpongeLevelFixer(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        final Type<?> levelType = this.getOutputSchema().getType(References.LEVEL);
        return TypeRewriteRule.seq(
                this.fixTypeEverywhereTyped("FixWorldUniqueId", levelType, SpongeLevelFixer::updateUUIDIn),
                this.fixTypeEverywhereTyped("FixPlayerIdTable", levelType, type -> this.fixPlayerIdTable(type, levelType))
        );
    }

    @SuppressWarnings("deprecation")
    private Typed<?> fixPlayerIdTable(Typed<?> typed, Type<?> levelType) {
        Type<?> fieldType = levelType.findFieldType(Constants.Sponge.LEGACY_SPONGE_PLAYER_UUID_TABLE);
        OpticFinder<List.ListType<?>> listFinder = DSL.fieldFinder(Constants.Sponge.LEGACY_SPONGE_PLAYER_UUID_TABLE, (List.ListType)fieldType);

        Typed<List.ListType<?>> listTyped = typed.getTyped(listFinder);
        // TODO is this correct?
        return listTyped.updateRecursiveTyped(DSL.remainderFinder(), SpongeLevelFixer::updateUUIDIn);
    }

    public static Typed<?> updateUUIDIn(Typed<?> typed) {
        return typed.update(DSL.remainderFinder(), data -> {
            final long least = data.get(Constants.Legacy.Entity.UUID_LEAST_1_8).asLong(0L);
            final long most = data.get(Constants.Legacy.Entity.UUID_MOST_1_8).asLong(0L);
            if (least != 0 && most != 0) {
                return data.remove(Constants.Legacy.Entity.UUID_LEAST_1_8)
                        .remove(Constants.Legacy.Entity.UUID_MOST_1_8)
                        .set(Constants.UUID_MOST, data.createLong(most))
                        .set(Constants.UUID_LEAST, data.createLong(least));
            }
            return data;
        });
    }
}
