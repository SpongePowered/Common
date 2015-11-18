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
package org.spongepowered.common.data.util;

import net.minecraft.util.IChatComponent;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.type.BigMushroomType;
import org.spongepowered.api.data.type.BigMushroomTypes;
import org.spongepowered.api.data.type.BrickType;
import org.spongepowered.api.data.type.BrickTypes;
import org.spongepowered.api.data.type.Career;
import org.spongepowered.api.data.type.Careers;
import org.spongepowered.api.data.type.ComparatorType;
import org.spongepowered.api.data.type.ComparatorTypes;
import org.spongepowered.api.data.type.DirtType;
import org.spongepowered.api.data.type.DirtTypes;
import org.spongepowered.api.data.type.DisguisedBlockType;
import org.spongepowered.api.data.type.DisguisedBlockTypes;
import org.spongepowered.api.data.type.DoublePlantType;
import org.spongepowered.api.data.type.DoublePlantTypes;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.Axis;
import org.spongepowered.api.util.Direction;
import org.spongepowered.common.text.SpongeTexts;

/**
 * A standard class where all various "constants" for various data are stored.
 * This is for a singular unique point of reference that can be changed
 * for implementation requirements.
 *
 * <p><em>WARNING</em>: USAGE OF THESE CONSTANTS, DUE TO STATIC INITIALIZATION,
 * IS ABSOLUTELY FORBIDDEN UNTIL THE GAME IS DURING THE POST-INIT PHASE DUE
 * TO REGISTRATION OF CATALOG TYPES. UNTIL THE REGISTRATION IS HANDLED WHERE
 * THE PROVIDED CATALOG TYPES ARE PROPERLY REGISTERED AND NOT <code>null</code>,
 * ANY USE OF THIS CLASS WILL RESULT IN A GLORIOUS FAIL INDESCRIBABLE MAGNITUDES.
 * </p>
 */
public final class DataConstants {

    public static final Text EMPTY_TEXT = Texts.of();

    public static final IChatComponent EMPTY_TEXT_COMPONENT = SpongeTexts.toComponent(Texts.of());
    public static final Axis DEFAULT_AXIS = Axis.X;
    public static final BigMushroomType DEFAULT_BIG_MUSHROOM_TYPE = BigMushroomTypes.ALL_OUTSIDE;
    public static final BrickType DEFAULT_BRICK_TYPE = BrickTypes.DEFAULT;
    public static final ComparatorType DEFAULT_COMPARATOR_TYPE = ComparatorTypes.COMPARE;
    public static final boolean DEFAULT_DECAYABLE_VALUE = false;
    public static final Direction DEFAULT_DIRECTION = Direction.NONE;
    public static final DirtType DEFAULT_DIRT_TYPE = DirtTypes.DIRT;
    public static final boolean DEFAULT_DISARMED = true;
    public static final DisguisedBlockType DEFAULT_DISGUISED_BLOCK = DisguisedBlockTypes.STONE;
    public static final DoublePlantType DEFAULT_DOUBLE_PLANT = DoublePlantTypes.GRASS;
    public static final boolean DEFAULT_SHOULD_DROP = true;
    public static final boolean DEFAULT_PISTON_EXTENDED = false;

    private DataConstants() {}

    // yes... we really can use this ;)
    public static final int ZERO = 0;

    // A bunch of entity defaults (for use in constructing "default" values)
    public static final boolean CAN_FLY_DEFAULT = false;
    public static final Career CAREER_DEFAULT = Careers.FARMER;
    public static final boolean ELDER_GUARDIAN_DEFAULT = false;
    public static final boolean IS_WET_DEFAULT = false;
    public static final GameMode DEFAULT_GAMEMODE = GameModes.NOT_SET;
    public static final boolean DEFAULT_ATTACHED = false;

    public static final int DEFAULT_FIRE_TICKS = 10;
    public static final int MINIMUM_FIRE_TICKS = 1;

    public static final double DEFAULT_FLYING_SPEED = 0.05D;

    public static final double DEFAULT_EXHAUSTION = ZERO;
    public static final double MINIMUM_EXHAUSTION = ZERO;
    public static final double DEFAULT_SATURATION = ZERO;
    public static final int DEFAULT_FOOD_LEVEL = 20;

    public static final BlockState DEFAULT_BLOCK_STATE = BlockTypes.STONE.getDefaultState();


}
