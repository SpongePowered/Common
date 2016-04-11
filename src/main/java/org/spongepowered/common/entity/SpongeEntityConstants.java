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
package org.spongepowered.common.entity;

import com.google.common.collect.Maps;
import net.minecraft.entity.passive.HorseArmorType;
import org.spongepowered.api.data.type.HorseColor;
import org.spongepowered.api.data.type.HorseStyle;
import org.spongepowered.api.data.type.HorseVariant;
import org.spongepowered.api.data.type.OcelotType;
import org.spongepowered.api.data.type.RabbitType;
import org.spongepowered.api.data.type.SkeletonType;
import org.spongepowered.api.entity.living.animal.Horse;

import java.util.Map;

public class SpongeEntityConstants {

    public static final Map<String, SkeletonType> SKELETON_TYPES = Maps.newHashMap();
    public static final Map<Integer, SkeletonType> SKELETON_IDMAP = Maps.newHashMap();
    public static final Map<String, OcelotType> OCELOT_TYPES = Maps.newHashMap();
    public static final Map<Integer, OcelotType> OCELOT_IDMAP = Maps.newHashMap();
    public static final Map<String, RabbitType> RABBIT_TYPES = Maps.newHashMap();
    public static final Map<Integer, RabbitType> RABBIT_IDMAP = Maps.newHashMap();
    public static final Map<String, HorseColor> HORSE_COLORS = Maps.newHashMap();
    public static final Map<Integer, HorseColor> HORSE_COLOR_IDMAP = Maps.newHashMap();
    public static final Map<String, HorseStyle> HORSE_STYLES = Maps.newHashMap();
    public static final Map<Integer, HorseStyle> HORSE_STYLE_IDMAP = Maps.newHashMap();
    public static final Map<String, HorseVariant> HORSE_VARIANTS = Maps.newHashMap();
    public static final Map<HorseArmorType, HorseVariant> HORSE_VARIANT_IDMAP = Maps.newHashMap();

    // skeleton types
    public static final SpongeSkeletonType NORMAL_SKELETON = new SpongeSkeletonType(0, "NORMAL");
    public static final SpongeSkeletonType WITHER_SKELETON = new SpongeSkeletonType(1, "WITHER");

    // ocelot types
    public static final SpongeOcelotType WILD_OCELOT = new SpongeOcelotType(0, "WILD_OCELOT");
    public static final SpongeOcelotType BLACK_CAT = new SpongeOcelotType(1, "BLACK_CAT");
    public static final SpongeOcelotType RED_CAT = new SpongeOcelotType(2, "RED_CAT");
    public static final SpongeOcelotType SIAMESE_CAT = new SpongeOcelotType(3, "SIAMESE_CAT");

    // rabbit types
    public static final SpongeRabbitType BROWN_RABBIT = new SpongeRabbitType(0, "BROWN");
    public static final RabbitType WHITE_RABBIT = new SpongeRabbitType(1, "WHITE");
    public static final RabbitType BLACK_RABBIT = new SpongeRabbitType(2, "BLACK");
    public static final RabbitType BLACK_AND_WHITE_RABBIT = new SpongeRabbitType(3, "BLACK_AND_WHITE");
    public static final RabbitType GOLD_RABBIT = new SpongeRabbitType(4, "GOLD");
    public static final RabbitType SALT_AND_PEPPER_RABBIT = new SpongeRabbitType(5, "SALT_AND_PEPPER");
    public static final RabbitType KILLER_RABBIT = new SpongeRabbitType(99, "KILLER");

    // horse colors
    public static final SpongeHorseColor WHITE = new SpongeHorseColor(0, "WHITE");
    public static final SpongeHorseColor CREAMY = new SpongeHorseColor(1, "CREAMY");
    public static final SpongeHorseColor CHESTNUT = new SpongeHorseColor(2, "CHESTNUT");
    public static final SpongeHorseColor BROWN = new SpongeHorseColor(3, "BROWN");
    public static final SpongeHorseColor BLACK = new SpongeHorseColor(4, "BLACK");
    public static final SpongeHorseColor GRAY = new SpongeHorseColor(5, "GRAY");
    public static final SpongeHorseColor DARK_BROWN = new SpongeHorseColor(6, "DARK_BROWN");

    // horse styles
    public static final SpongeHorseStyle NONE = new SpongeHorseStyle(0, "NONE");
    public static final SpongeHorseStyle WHITE_STYLE = new SpongeHorseStyle(1, "WHITE");
    public static final SpongeHorseStyle WHITEFIELD = new SpongeHorseStyle(2, "WHITEFIELD");
    public static final SpongeHorseStyle WHITE_DOTS = new SpongeHorseStyle(3, "WHITE_DOTS");
    public static final SpongeHorseStyle BLACK_DOTS = new SpongeHorseStyle(4, "BLACK_DOTS");

    // horse variants
    public static final SpongeHorseVariant HORSE = new SpongeHorseVariant(0, HorseArmorType.HORSE, "HORSE");
    public static final SpongeHorseVariant DONKEY = new SpongeHorseVariant(1, HorseArmorType.DONKEY, "DONKEY");
    public static final SpongeHorseVariant MULE = new SpongeHorseVariant(2, HorseArmorType.MULE, "MULE");
    public static final SpongeHorseVariant UNDEAD_HORSE = new SpongeHorseVariant(3, HorseArmorType.ZOMBIE, "UNDEAD_HORSE");
    public static final SpongeHorseVariant SKELETON_HORSE = new SpongeHorseVariant(4, HorseArmorType.SKELETON, "SKELETON_HORSE");

    static {
        SKELETON_TYPES.put("normal", NORMAL_SKELETON);
        SKELETON_TYPES.put("wither", WITHER_SKELETON);

        SKELETON_IDMAP.put(0, NORMAL_SKELETON);
        SKELETON_IDMAP.put(1, WITHER_SKELETON);

        OCELOT_TYPES.put("wild_ocelot", WILD_OCELOT);
        OCELOT_TYPES.put("black_cat", BLACK_CAT);
        OCELOT_TYPES.put("red_cat", RED_CAT);
        OCELOT_TYPES.put("siamese_cat", SIAMESE_CAT);

        OCELOT_IDMAP.put(0, WILD_OCELOT);
        OCELOT_IDMAP.put(1, BLACK_CAT);
        OCELOT_IDMAP.put(2, RED_CAT);
        OCELOT_IDMAP.put(3, SIAMESE_CAT);

        RABBIT_TYPES.put("brown", BROWN_RABBIT);
        RABBIT_TYPES.put("white", WHITE_RABBIT);
        RABBIT_TYPES.put("black", BLACK_RABBIT);
        RABBIT_TYPES.put("black_and_white", BLACK_AND_WHITE_RABBIT);
        RABBIT_TYPES.put("gold", GOLD_RABBIT);
        RABBIT_TYPES.put("salt_and_pepper", SALT_AND_PEPPER_RABBIT);
        RABBIT_TYPES.put("killer", KILLER_RABBIT);

        RABBIT_IDMAP.put(0, BROWN_RABBIT);
        RABBIT_IDMAP.put(1, WHITE_RABBIT);
        RABBIT_IDMAP.put(2, BLACK_RABBIT);
        RABBIT_IDMAP.put(3, BLACK_AND_WHITE_RABBIT);
        RABBIT_IDMAP.put(4, GOLD_RABBIT);
        RABBIT_IDMAP.put(5, SALT_AND_PEPPER_RABBIT);
        RABBIT_IDMAP.put(99, KILLER_RABBIT);

        HORSE_COLORS.put("white", WHITE);
        HORSE_COLORS.put("creamy", CREAMY);
        HORSE_COLORS.put("chestnut", CHESTNUT);
        HORSE_COLORS.put("brown", BROWN);
        HORSE_COLORS.put("black", BLACK);
        HORSE_COLORS.put("gray", GRAY);
        HORSE_COLORS.put("dark_brown", DARK_BROWN);

        HORSE_COLOR_IDMAP.put(0, WHITE);
        HORSE_COLOR_IDMAP.put(1, CREAMY);
        HORSE_COLOR_IDMAP.put(2, CHESTNUT);
        HORSE_COLOR_IDMAP.put(3, BROWN);
        HORSE_COLOR_IDMAP.put(4, BLACK);
        HORSE_COLOR_IDMAP.put(5, GRAY);
        HORSE_COLOR_IDMAP.put(6, DARK_BROWN);

        HORSE_STYLES.put("none", NONE);
        HORSE_STYLES.put("white", WHITE_STYLE);
        HORSE_STYLES.put("whitefield", WHITEFIELD);
        HORSE_STYLES.put("white_dots", WHITE_DOTS);
        HORSE_STYLES.put("black_dots", BLACK_DOTS);

        HORSE_STYLE_IDMAP.put(0, NONE);
        HORSE_STYLE_IDMAP.put(1, WHITE_STYLE);
        HORSE_STYLE_IDMAP.put(2, WHITEFIELD);
        HORSE_STYLE_IDMAP.put(3, WHITE_DOTS);
        HORSE_STYLE_IDMAP.put(4, BLACK_DOTS);

        HORSE_VARIANTS.put("horse", HORSE);
        HORSE_VARIANTS.put("donkey", DONKEY);
        HORSE_VARIANTS.put("mule", MULE);
        HORSE_VARIANTS.put("undead_horse", UNDEAD_HORSE);
        HORSE_VARIANTS.put("skeleton_horse", SKELETON_HORSE);

        HORSE_VARIANT_IDMAP.put(HorseArmorType.HORSE, HORSE);
        HORSE_VARIANT_IDMAP.put(HorseArmorType.DONKEY, DONKEY);
        HORSE_VARIANT_IDMAP.put(HorseArmorType.MULE, MULE);
        HORSE_VARIANT_IDMAP.put(HorseArmorType.ZOMBIE, UNDEAD_HORSE);
        HORSE_VARIANT_IDMAP.put(HorseArmorType.SKELETON, SKELETON_HORSE);
    }
}
