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
package org.spongepowered.common.util;

import static org.spongepowered.api.data.persistence.DataQuery.of;

import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContentUpdater;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.type.ArtType;
import org.spongepowered.api.data.type.ArtTypes;
import org.spongepowered.api.data.type.CatType;
import org.spongepowered.api.data.type.CatTypes;
import org.spongepowered.api.data.type.ComparatorMode;
import org.spongepowered.api.data.type.ComparatorModes;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.data.type.HandPreference;
import org.spongepowered.api.data.type.HandPreferences;
import org.spongepowered.api.data.type.HorseColor;
import org.spongepowered.api.data.type.HorseColors;
import org.spongepowered.api.data.type.HorseStyle;
import org.spongepowered.api.data.type.HorseStyles;
import org.spongepowered.api.data.type.LlamaType;
import org.spongepowered.api.data.type.LlamaTypes;
import org.spongepowered.api.data.type.ParrotType;
import org.spongepowered.api.data.type.ParrotTypes;
import org.spongepowered.api.data.type.RabbitType;
import org.spongepowered.api.data.type.RabbitTypes;
import org.spongepowered.api.data.type.StructureMode;
import org.spongepowered.api.data.type.StructureModes;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.util.Axis;
import org.spongepowered.api.util.Direction;
import org.spongepowered.math.vector.Vector3i;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Supplier;

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
public final class Constants {

    public static final String UUID = "UUID";
    public static final String UUID_MOST = "UUIDMost";
    public static final DataQuery UUID_MOST_QUERY = DataQuery.of(Constants.UUID_MOST);
    public static final String UUID_LEAST = "UUIDLeast";
    public static final DataQuery UUID_LEAST_QUERY = DataQuery.of(Constants.UUID_LEAST);
    public static final String MINECRAFT = "minecraft";
    public static final String MINECRAFT_CLIENT = "net.minecraft.client.Minecraft";
    public static final String DEDICATED_SERVER = "net.minecraft.server.dedicated.DedicatedServer";
    public static final String MINECRAFT_SERVER = "net.minecraft.server.MinecraftServer";
    public static final String INTEGRATED_SERVER = "net.minecraft.client.server.IntegratedServer";

    private Constants() {
    }

    public static final class Sponge {

        public static final class Data {

            /**
             * Class based custom data.
             */
            @Deprecated
            public static final class V1 {
                public static final String CUSTOM_DATA_CLASS = "DataClass";
                public static final DataQuery DATA_CLASS = DataQuery.of(CUSTOM_DATA_CLASS);
                public static final String DATA_VERSION = "DataVersion";
            }

            /**
             * Data Manipulator with DataId based custom data
             */
            @Deprecated
            public static final class V2 {
                public static final DataQuery MANIPULATOR_DATA = DataQuery.of("ManipulatorData");
                public static final DataQuery MANIPULATOR_ID = DataQuery.of("ManipulatorId");

                public static final String FAILED_CUSTOM_DATA = "FailedData";

                public static final String CUSTOM_MANIPULATOR_TAG_LIST = "CustomManipulators";
                public static final DataQuery CUSTOM_MANIPULATOR_LIST = of(V2.CUSTOM_MANIPULATOR_TAG_LIST);

                public static final String SPONGE_DATA = "SpongeData";
                public static final DataQuery SPONGE_DATA_ROOT = of(V2.SPONGE_DATA);
            }

            /**
             * {@link org.spongepowered.api.data.persistence.DataStore} based data
             */
            public static final class V3 {
                public static final DataQuery SPONGE_DATA_ROOT = DataQuery.of("sponge-data");
                public static final DataQuery CONTENT_VERSION = DataQuery.of("version");
                public static final DataQuery CONTENT = DataQuery.of("content");
            }
        }

        public static final int MAX_DEATH_EVENTS_BEFORE_GIVING_UP = 3;

        public static final String SPONGE_ENTITY_CREATOR = "Creator";
        public static final String SPONGE_ENTITY_NOTIFIER = "Notifier";
        public static final String SPONGE_BLOCK_POS_TABLE = "BlockPosTable";

        @Deprecated
        public static final String LEGACY_SPONGE_PLAYER_UUID_TABLE = "PlayerIdTable";
        public static final String SPONGE_PLAYER_UUID_TABLE = "player-uuid-table";

        // General DataQueries
        public static final DataQuery UNSAFE_NBT = of("UnsafeData");
        public static final DataQuery DATA_MANIPULATORS = of("Data");
        // Snapshots
        public static final DataQuery SNAPSHOT_WORLD_POSITION = of("Position");

        /**
         * Modifies bits in an integer.
         *
         * @param num Integer to modify
         * @param data Bits of data to add
         * @param which Index of nibble to start at
         * @param bitsToReplace The number of bits to replace starting from nibble index
         * @return The modified integer
         */
        public static int setNibble(final int num, final int data, final int which, final int bitsToReplace) {
            return (num & ~(bitsToReplace << (which * 4)) | (data << (which * 4)));
        }

        /**
         * Serialize this BlockPos into a short value
         */
        public static short blockPosToShort(final BlockPos pos) {
            short serialized = (short) Sponge.setNibble(0, pos.getX() & Constants.Chunk.XZ_MASK, 0, Constants.Chunk.NUM_XZ_BITS);
            serialized = (short) Sponge.setNibble(serialized, pos.getY() & Constants.Chunk.Y_SHORT_MASK, 1, Constants.Chunk.NUM_SHORT_Y_BITS);
            serialized = (short) Sponge.setNibble(serialized, pos.getZ() & Constants.Chunk.XZ_MASK, 3, Constants.Chunk.NUM_XZ_BITS);
            return serialized;
        }

        /**
         * Serialize this BlockPos into an int value
         */
        public static int blockPosToInt(final BlockPos pos) {
            int serialized = Sponge.setNibble(0, pos.getX() & Constants.Chunk.XZ_MASK, 0, Constants.Chunk.NUM_XZ_BITS);
            serialized = Sponge.setNibble(serialized, pos.getY() & Constants.Chunk.Y_INT_MASK, 1, Constants.Chunk.NUM_INT_Y_BITS);
            serialized = Sponge.setNibble(serialized, pos.getZ() & Constants.Chunk.XZ_MASK, 7, Constants.Chunk.NUM_XZ_BITS);
            return serialized;
        }

        public static final class PlayerData {

            public static final DataQuery PLAYER_DATA_JOIN = of("FirstJoin");
            public static final DataQuery PLAYER_DATA_LAST = of("LastPlayed");
        }

        public static final class EntityArchetype {

            public static final int BASE_VERSION = 1;
            public static final String ENTITY_ID = "Id";
            public static final DataQuery ENTITY_TYPE = of("EntityType");
            public static final DataQuery ENTITY_DATA = of("EntityData");
        }

        public static final class Entity {

            public static final String IS_VANISHED = "IsVanished";
            public static final String IS_INVISIBLE = "IsInvisible";
            public static final String VANISH_UNCOLLIDEABLE = "VanishUnCollideable";
            public static final String VANISH_UNTARGETABLE = "VanishUnTargetable";
            public static final String CAN_GRIEF = "CanGrief";

            public static final class Item {

                // These are used by pickup/despawn delay for ItemEntity
                public static final String INFINITE_PICKUP_DELAY = "InfinitePickupDelay";
                public static final String INFINITE_DESPAWN_DELAY = "InfiniteDespawnDelay";
                public static final String PREVIOUS_PICKUP_DELAY = "PreviousPickupDelay";
                public static final String PREVIOUS_DESPAWN_DELAY = "PreviousDespawnDelay";
            }

            public static final class Projectile {

                public static final String PROJECTILE_DAMAGE_AMOUNT = "damageAmount";
            }

            public static final class EyeOfEnder {

                public static final int DESPAWN_TIMER_MAX = 80;
            }

            public static final class Player {

                public static final String HEALTH_SCALE = "HealthScale";
                public static final int ITEM_COOLDOWN_CANCELLED = -2;
            }

            public static final class Human {
                public static final byte PLAYER_MODEL_FLAG_ALL = (byte) 0b01111111;
            }

            public static final class DataRegistration {
                public static final String INVENTORY = "inventory";
                public static final String BLOCKENTITY = "blockentity";
                public static final String LOCATION = "location";
                public static final String BLOCKSTATE = "blockstate";
                public static final String ENTITY = "entity";
                public static final String GENERIC = "generic";
                public static final String ITEMSTACK = "itemstack";
                public static final String ITEM = "item";
                public static final String NBT = "nbt";
            }

            public static final class RangedAttackGoal {
                public static final int INFINITE_ATTACK_TIME = -2;
            }
        }

        public static final class User {

            public static final String USER_SPAWN_X = "SpawnX";
            public static final String USER_SPAWN_Y = "SpawnY";
            public static final String USER_SPAWN_Z = "SpawnZ";
            public static final String USER_SPAWN_FORCED = "SpawnForced";
            public static final String USER_SPAWN_LIST = "Spawns";
        }

        public static final class World {

            public static final String DIMENSION_TYPE = "dimensionType";
            public static final String HAS_CUSTOM_DIFFICULTY = "HasCustomDifficulty";
            public static final String LEVEL_SPONGE_DAT = "level_sponge.dat";
            public static final String LEVEL_SPONGE_DAT_OLD = org.spongepowered.common.util.Constants.Sponge.World.LEVEL_SPONGE_DAT + "_old";
            public static final String LEVEL_SPONGE_DAT_NEW = org.spongepowered.common.util.Constants.Sponge.World.LEVEL_SPONGE_DAT + "_new";
            public static final String UNIQUE_ID = "UUID";
            public static final String DIMENSIONS_DIRECTORY = "dimensions";
            public static final String WORLD_KEY = "WorldKey";
        }

        public static final class Schematic {

            public static final DataQuery NAME = of("Name");
            public static final int CURRENT_VERSION = 3;
            public static final int MAX_SIZE = Integer.MAX_VALUE & 0xFFFF;
            public static final DataQuery VERSION = of("Version");
            public static final DataQuery DATA_VERSION = of("DataVersion");
            public static final DataQuery METADATA = of("Metadata");
            public static final DataQuery REQUIRED_MODS = of(org.spongepowered.api.world.schematic.Schematic.METADATA_REQUIRED_MODS);
            public static final DataQuery WIDTH = of("Width");
            public static final DataQuery HEIGHT = of("Height");
            public static final DataQuery LENGTH = of("Length");
            public static final DataQuery OFFSET = of("Offset");
            public static final DataQuery BLOCK_PALETTE = of("Palette");
            public static final DataQuery BLOCK_CONTAINER = of("Blocks");
            public static final DataQuery BIOME_CONTAINER = of("Biomes");
            public static final DataQuery PALETTE = of("Palette");
            public static final DataQuery BLOCK_DATA = of("Data");
            public static final DataQuery BIOME_DATA = of("Data");
            public static final DataQuery BLOCKENTITY_CONTAINER = of("BlockEntities");
            public static final DataQuery BLOCKENTITY_DATA = of("Data");
            public static final DataQuery BLOCKENTITY_ID = of("Id");
            public static final DataQuery BLOCKENTITY_POS = of("Pos");
            public static final DataQuery ENTITIES = of("Entities");
            public static final DataQuery ENTITIES_ID = of("Id");
            public static final DataQuery ENTITIES_POS = of("Pos");
            public static final DataQuery BIOME_PALETTE = of("Palette");
            public static final DataQuery SCHEMATIC = of("Schematic");

            public static final class Versions {

                public static final DataQuery V1_TILE_ENTITY_DATA = of("TileEntities");
                public static final DataQuery V1_TILE_ENTITY_ID = of("id");

                public static final DataQuery V1_BLOCK_PALETTE = of("Palette");
                public static final DataQuery V1_BLOCK_PALETTE_MAX = of("Palette");
                public static final DataQuery V2_BLOCK_PALETTE = of("Palette");
                public static final DataQuery V2_BIOME_PALETTE = of("BiomePalette");
                public static final DataQuery V2_BLOCK_DATA = of("BlockData");
                public static final DataQuery V2_BIOME_DATA = of("BiomeData");
                public static final DataQuery V2_BLOCK_ENTITIES = of("BlockEntities");

            }

            /**
             * The NBT structure of the legacy Schematic format used by MCEdit and WorldEdit etc.
             */
            public static final class Legacy {

                public static final DataQuery X_POS = of("x");
                public static final DataQuery Y_POS = of("y");
                public static final DataQuery Z_POS = of("z");
                public static final DataQuery MATERIALS = of("Materials");
                public static final DataQuery WE_OFFSET_X = of("WEOffsetX");
                public static final DataQuery WE_OFFSET_Y = of("WEOffsetY");
                public static final DataQuery WE_OFFSET_Z = of("WEOffsetZ");
                public static final DataQuery BLOCKS = of("Blocks");
                public static final DataQuery BLOCK_DATA = of("Data");
                public static final DataQuery ADD_BLOCKS = of("AddBlocks");
                public static final DataQuery TILE_ENTITIES = of("TileEntities");
                public static final DataQuery ENTITIES = of("Entities");
                public static final DataQuery ENTITY_ID = of("id");
            }

        }

        public static final class BlockEntityArchetype {

            public static final int BASE_VERSION = 1;
            public static final String TILE_ENTITY_ID = "Id";
            public static final String TILE_ENTITY_POS = "Pos";
            public static final DataQuery BLOCK_ENTITY_TYPE = of("TileEntityType");
            public static final DataQuery BLOCK_STATE = of("BlockState");
            public static final DataQuery BLOCK_ENTITY_DATA = of("TileEntityData");
        }

        public static final class BlockSnapshot {

            public static final String TILE_ENTITY_POSITION_X = "x";
            public static final String TILE_ENTITY_POSITION_Y = "y";
            public static final String TILE_ENTITY_POSITION_Z = "z";
            public static final DataQuery WORLD_UUID = DataQuery.of("WorldUuid"); // legacy data
        }


        public static final class Potion {

            public static final int POTION_V2 = 2;
            public static final int CURRENT_VERSION = Potion.POTION_V2;
        }

        public static final class BlockState {

            public static final int BLOCK_TYPE_WITH_DAMAGE_VALUE = 1;
            public static final int STATE_AS_CATALOG_ID = 2;
        }

    }

    public static final class Permissions {

        public static final String SELECTOR_PERMISSION = "minecraft.selector";
        public static final String COMMAND_BLOCK_PERMISSION = "minecraft.commandblock";
        public static final int COMMAND_BLOCK_LEVEL = 2;
        public static final int SELECTOR_LEVEL = 2;
        public static final String SPONGE_HELP_PERMISSION = "sponge.command.help";
        public static final String DEBUG_HOVER_STACKTRACE = "sponge.debug.hover-stacktrace";
        public static final int SPONGE_HELP_LEVEL = 0;
    }

    /**
     * https://wiki.vg/Protocol#Effect
     */
    public static final class WorldEvents {

        public static final int PLAY_RECORD_EVENT = 1010;
        public static final int PLAY_WITHER_SPAWN_EVENT = 1023;
        public static final int PLAY_ENDERDRAGON_DEATH_EVENT = 1028;
        public static final int PLAY_BLOCK_END_PORTAL_SPAWN_EVENT = 1038;

    }

    public static final class World {

        public static final Vector3i BLOCK_MIN = new Vector3i(-30000000, 0, -30000000);
        public static final Vector3i BLOCK_MAX = new Vector3i(30000000, 256, 30000000).sub(Vector3i.ONE);
        public static final Vector3i BLOCK_SIZE = Constants.World.BLOCK_MAX.sub(Constants.World.BLOCK_MIN).add(Vector3i.ONE);
        public static final EnumSet<net.minecraft.core.Direction> NOTIFY_DIRECTION_SET = EnumSet
            .of(net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.DOWN,
                net.minecraft.core.Direction.UP, net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH);
        public static final ResourceKey INVALID_WORLD_KEY = ResourceKey.sponge("invalid_world");
        public static final String LEVEL_DAT_OLD = LevelResource.LEVEL_DATA_FILE.getId() + "_old";
        public static final int DEFAULT_BLOCK_CHANGE_LIMIT = 512;
    }

    public static final class Chunk {

        public static final Direction[] CARDINAL_DIRECTIONS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

        // Neighbor Constants

        public static final int NUM_XZ_BITS = 4;
        public static final int NUM_SHORT_Y_BITS = 8;
        public static final int NUM_INT_Y_BITS = 24;
        public static final short XZ_MASK = 0xF;
        public static final short Y_SHORT_MASK = 0xFF;
        public static final int Y_INT_MASK = 0xFFFFFF;
        public static final String CHUNK_DATA_SECTIONS = "Sections";
    }

    public static final class ChunkTicket {

        public static final int MAX_FULL_CHUNK_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);

        // Highest ticket level that will cause loading a full chunk, plus one.
        public static final int MAX_FULL_CHUNK_DISTANCE = ChunkTicket.MAX_FULL_CHUNK_TICKET_LEVEL + 1;

        public static final int INFINITE_TIMEOUT = 0;
    }

    public static final class Networking {

        public static final int MAX_STRING_LENGTH_BYTES = Short.MAX_VALUE;

        public static final int MAX_STRING_LENGTH = Constants.Networking.MAX_STRING_LENGTH_BYTES >> 2;
        // Inventory static fields
        public static final int MAGIC_CLICK_OUTSIDE_SURVIVAL = -999;
        public static final int MAGIC_CLICK_OUTSIDE_CREATIVE = -1;
        // Flag masks
        public static final int MASK_NONE = 0x00000;
        public static final int MASK_OUTSIDE = 0x30000;
        public static final int MASK_MODE = 0x0FE00;
        public static final int MASK_DRAGDATA = 0x001F8;
        public static final int MASK_BUTTON = 0x00007;
        // Mask presets
        public static final int MASK_ALL = Networking.MASK_OUTSIDE | Networking.MASK_MODE | Networking.MASK_BUTTON | Networking.MASK_DRAGDATA;
        public static final int MASK_NORMAL = Networking.MASK_MODE | Networking.MASK_BUTTON | Networking.MASK_DRAGDATA;
        public static final int MASK_DRAG = Networking.MASK_OUTSIDE | Networking.MASK_NORMAL;
        // Click location semaphore flags
        public static final int CLICK_INSIDE_WINDOW = 0x01 << 16; // << 0
        public static final int CLICK_OUTSIDE_WINDOW = 0x01 << 16 << 1;
        public static final int CLICK_ANYWHERE = Networking.CLICK_INSIDE_WINDOW | Networking.CLICK_OUTSIDE_WINDOW;
        // Modes flags
        public static final int MODE_CLICK = 0x01 << 9 << ClickType.PICKUP.ordinal();
        public static final int MODE_SHIFT_CLICK = 0x01 << 9 << ClickType.QUICK_MOVE.ordinal();
        public static final int MODE_HOTBAR = 0x01 << 9 << ClickType.SWAP.ordinal();
        public static final int MODE_PICKBLOCK = 0x01 << 9 << ClickType.CLONE.ordinal();
        public static final int MODE_DROP = 0x01 << 9 << ClickType.THROW.ordinal();
        public static final int MODE_DRAG = 0x01 << 9 << ClickType.QUICK_CRAFT.ordinal();
        public static final int MODE_DOUBLE_CLICK = 0x01 << 9 << ClickType.PICKUP_ALL.ordinal();
        // Drag mode flags, bitmasked from button and only set if MODE_DRAG
        public static final int DRAG_MODE_PRIMARY_BUTTON = 0x01 << 6; // << 0
        public static final int DRAG_MODE_SECONDARY_BUTTON = 0x01 << 6 << 1;
        public static final int DRAG_MODE_MIDDLE_BUTTON = 0x01 << 6 << 2;
        public static final int DRAG_MODE_ANY = Networking.DRAG_MODE_PRIMARY_BUTTON | Networking.DRAG_MODE_SECONDARY_BUTTON | Networking.DRAG_MODE_MIDDLE_BUTTON;
        // Drag status flags, bitmasked from button and only set if MODE_DRAG
        public static final int DRAG_STATUS_STARTED = 0x01 << 3; // << 0;
        public static final int DRAG_STATUS_ADD_SLOT = 0x01 << 3 << 1;
        public static final int DRAG_STATUS_STOPPED = 0x01 << 3 << 2;
        // Buttons flags, only set if *not* MODE_DRAG
        public static final int BUTTON_PRIMARY = 0x01 /* << 0 */; // << 0
        public static final int BUTTON_SECONDARY = 0x01 /* << 0 */ << 1;
        public static final int BUTTON_MIDDLE = 0x01 /* << 0 */ << 2;
        // Only use these with data from the actual packet. DO NOT
        // use them as enum constant values (the 'stateId')
        public static final int PACKET_BUTTON_PRIMARY_ID = 0;
        public static final int PACKET_BUTTON_SECONDARY_ID = 0;
        public static final int PACKET_BUTTON_MIDDLE_ID = 0;
        public static final InetSocketAddress LOCALHOST = InetSocketAddress.createUnresolved("127.0.0.1", 0);
    }

    public static final class Item {

        // These are the various tag compound id's for getting to various places
        public static final String BLOCK_ENTITY_TAG = "BlockEntityTag";
        public static final String BLOCK_ENTITY_ID = "id";
        public static final String ENTITY_TAG = "EntityTag";
        public static final String ITEM_ENCHANTMENT_LIST = "Enchantments";
        public static final String ITEM_STORED_ENCHANTMENTS_LIST = "StoredEnchantments";
        public static final String ITEM_DISPLAY = "display";
        public static final String ITEM_LORE = "Lore";
        public static final String ITEM_NAME = "Name";
        public static final String ITEM_ORIGINAL_LORE = "SpongeOriginalLore";
        public static final String ITEM_ORIGINAL_NAME = "SpongeOriginalName";
        public static final String ITEM_ENCHANTMENT_ID = "id";
        public static final String ITEM_ENCHANTMENT_LEVEL = "lvl";
        public static final String ITEM_HIDE_FLAGS = "HideFlags";
        public static final String ITEM_UNBREAKABLE = "Unbreakable";
        public static final String CUSTOM_POTION_EFFECTS = "custom_potion_effects";
        public static final String LOCK = "Lock";

        public static final class Book {

            public static final String ITEM_BOOK_PAGES = "pages";
            public static final String ITEM_BOOK_TITLE = "title";
            public static final String ITEM_BOOK_AUTHOR = "author";
            public static final String ITEM_BOOK_RESOLVED = "resolved";
            public static final String ITEM_BOOK_GENERATION = "generation";
            public static final String INVALID_TITLE = "invalid";

        }

        public static final class Skull {

            public static final String ITEM_SKULL_OWNER = "SkullOwner";
        }

        public static final class Potions {

            // Potions
            public static final DataQuery POTION_TYPE = of("PotionType");
            public static final DataQuery POTION_AMPLIFIER = of("Amplifier");
            public static final DataQuery POTION_SHOWS_PARTICLES = of("ShowsParticles");
            public static final DataQuery POTION_SHOWS_ICON = of("ShowsIcon");
            public static final DataQuery POTION_AMBIANCE = of("Ambiance");
            public static final DataQuery POTION_DURATION = of("Duration");
        }

        public static final class TradeOffer {

            // TradeOffers
            public static final DataQuery FIRST_QUERY = of("FirstItem");
            public static final DataQuery SECOND_QUERY = of("SecondItem");
            public static final DataQuery BUYING_QUERY = of("BuyingItem");
            public static final DataQuery EXPERIENCE_QUERY = of("GrantsExperience");
            public static final DataQuery MAX_QUERY = of("MaxUses");
            public static final DataQuery USES_QUERY = of("Uses");
            public static final DataQuery EXPERIENCE_GRANTED_TO_MERCHANT_QUERY = of("ExperienceGrantedToMerchant");
            public static final DataQuery PRICE_GROWTH_MULTIPLIER_QUERY = of("PriceGrowthMultiplier");
            public static final DataQuery DEMAND_BONUS_QUERY = of("DemandBonus");
            public static final int DEFAULT_USE_COUNT = 0;
            public static final int DEFAULT_MAX_USES = 7;
        }

        public static final class Fireworks {

            // Firework Effects
            public static final DataQuery FIREWORK_SHAPE = of("Type");
            public static final DataQuery FIREWORK_COLORS = of("Colors");
            public static final DataQuery FIREWORK_FADE_COLORS = of("Fades");
            public static final DataQuery FIREWORK_TRAILS = of("Trails");
            public static final DataQuery FIREWORK_FLICKERS = of("Flickers");
            public static final String FIREWORKS = "Fireworks";
            public static final String EXPLOSIONS = "Explosions";
            public static final String FADE_COLORS = "FadeColors";
            public static final String COLORS = "Colors";
            public static final String FLICKER = "Flicker";
            public static final String TRAIL = "Trail";
            public static final String SHAPE_TYPE = "Type";
            public static final String FLIGHT = "Flight";
        }
    }

    public static final class Recipe {
        public static final String GROUP = "group";

        public static final String RESULT = "result";
        public static final String ITEM = "item";
        public static final String CATEGORY = "category";
        public static final String COUNT = "count";

        public static final String SPONGE_TYPE = "sponge:type";
        public static final String SPONGE_ID = "sponge:id";
        public static final String SPONGE_RESULT = "sponge:result";
        public static final String SPONGE_RESULTFUNCTION = "sponge:result_function";
        public static final String SPONGE_REMAINING_ITEMS = "sponge:remaining_items";

        public static final String COOKING_EXP = "experience";
        public static final String COOKING_TIME = "cookingtime";

        public static final String COOKING_INGREDIENT = "ingredient";
        public static final String STONECUTTING_INGREDIENT = "ingredient";
        public static final String SMITHING_BASE_INGREDIENT = "base";
        public static final String SMITHING_TEMPLATE_INGREDIENT = "template";
        public static final String SMITHING_ADDITION_INGREDIENT = "addition";
        public static final String SHAPED_PATTERN = "pattern";
        public static final String SHAPED_INGREDIENTS = "key";
        public static final String SHAPELESS_INGREDIENTS = "ingredients";
    }

    public static final class TileEntity {

        public static final String SIGN = "Sign";
        public static final String X_POS = "x";
        public static final String Y_POS = "y";
        public static final String Z_POS = "z";
        // TileEntities
        public static final DataQuery TILE_TYPE = of("TileType");
        public static final DataQuery LOCK_CODE = of("Lock");
        public static final DataQuery ITEM_CONTENTS = of("Contents");
        public static final DataQuery SLOT = of("SlotId");
        public static final DataQuery SLOT_ITEM = of("Item");
        public static final DataQuery LOCKABLE_CONTAINER_CUSTOM_NAME = of("CustomName");
        // TileEntity names
        public static final DataQuery CUSTOM_NAME = of("CustomName");

        public static final class Structure {

            // Structure block entity
            public static final String DEFAULT_STRUCTURE_AUTHOR = ""; // intentionally empty, as in vanilla
            public static final boolean DEFAULT_STRUCTURE_IGNORE_ENTITIES = true;
            public static final Supplier<StructureMode> DEFAULT_STRUCTURE_MODE = StructureModes.DATA;
            public static final Vector3i DEFAULT_STRUCTURE_POSITION = Vector3i.ONE;
            public static final boolean DEFAULT_STRUCTURE_POWERED = false;
            public static final boolean DEFAULT_STRUCTURE_SHOW_AIR = false;
            public static final long DEFAULT_STRUCTURE_SEED = 0L;
            public static final Vector3i DEFAULT_STRUCTURE_SIZE = Vector3i.ONE;
        }

        public static final class Spawner {

            public static final short DEFAULT_SPAWN_COUNT = 4;
            public static final short DEFAULT_MAXMIMUM_NEARBY_ENTITIES = 6;
        }

        public static final class Furnace {

            public static final int MAX_BURN_TIME = 1600;
            public static final int DEFAULT_COOK_TIME = 200;

            public static final DataQuery BURN_TIME = of("BurnTime");
            public static final DataQuery BURN_TIME_TOTAL = of("BurnTimeTotal");
            public static final DataQuery COOK_TIME = of("CookTime");
            public static final DataQuery COOK_TIME_TOTAL = of("CookTimeTotal");
        }

        public static final class Banner {

            public static final String BANNER_PATTERN_ID = "Pattern";
            public static final String BANNER_PATTERN_COLOR = "Color";
            public static final String BANNER_BASE = "Base";
            public static final String BANNER_PATTERNS = "Patterns";
            // Banners
            public static final DataQuery BASE = of("Base");
            // BannerPatterns
            public static final DataQuery SHAPE = of("BannerShapeId");
            public static final DataQuery COLOR = of("DyeColor");
        }

        public static final class CommandBlock {

            // Commands
            public static final DataQuery SUCCESS_COUNT = of("SuccessCount");
            public static final DataQuery DOES_TRACK_OUTPUT = of("DoesTrackOutput");
            public static final DataQuery STORED_COMMAND = of("StoredCommand");
            public static final DataQuery TRACKED_OUTPUT = of("TrackedOutput");
        }

        public static final class Hopper {

            public static final DataQuery TRANSFER_COOLDOWN = of("TransferCooldown");
        }

        public static final class Beacon {

            // Beacons
            public static final DataQuery PRIMARY = of("primary");
            public static final DataQuery SECONDARY = of("secondary");
        }

        public static final class Anvils {

            // UpdateAnvilEventCost
            public static final DataQuery MATERIALCOST = DataQuery.of("materialcost");
            public static final DataQuery LEVELCOST = DataQuery.of("levelcost");
        }

        public static final class Sign {

            public static final DataQuery LINES = of("SignLines");
            public static final DataQuery GLOWING_TEXT = of("GlowingText");
        }
    }

    public static final class Catalog {

        public static final Supplier<DyeColor> DEFAULT_SHULKER_COLOR = DyeColors.PURPLE;
        public static final Supplier<ComparatorMode> DEFAULT_COMPARATOR_MODE = ComparatorModes.COMPARE;
        public static final Supplier<GameMode> DEFAULT_GAMEMODE = GameModes.SURVIVAL;
        public static final Supplier<ArtType> DEFAULT_ART = ArtTypes.KEBAB;
        public static final Supplier<HandPreference> DEFAULT_HAND = HandPreferences.RIGHT;

        private Catalog() {
        }
    }

    public static final class Profile {

        public static final DataQuery UUID = of("UUID");
        public static final DataQuery NAME = of("Name");
        public static final DataQuery PROPERTIES = DataQuery.of("Properties");
        public static final DataQuery VALUE = DataQuery.of("Value");
        public static final DataQuery SIGNATURE = DataQuery.of("Signature");
    }

    public static final class Entity {

        public static final String LIGHTNING_EFFECT = "effect";
        public static final int ELYTRA_FLYING_FLAG = 7;
        public static final int MINIMUM_FIRE_TICKS = 1;
        public static final boolean DEFAULT_GLOWING = false;
        public static final BlockPos HANGING_OFFSET_EAST = new BlockPos(1, 1, 0);
        public static final BlockPos HANGING_OFFSET_WEST = new BlockPos(-1, 1, 0);
        public static final BlockPos HANGING_OFFSET_NORTH = new BlockPos(0, 1, -1);
        public static final BlockPos HANGING_OFFSET_SOUTH = new BlockPos(0, 1, 1);
        // These are used by Minecraft's internals for entity spawning
        public static final String ENTITY_TYPE_ID = "id";
        public static final String ENTITY_POSITION = "Pos";
        public static final String ENTITY_DIMENSION = "Dimension";
        public static final String ENTITY_ROTATION = "Rotation";
        public static final String ENTITY_UUID = "UUID";
        // Entities
        public static final DataQuery CLASS = of("EntityClass");
        public static final DataQuery UUID = of("EntityUniqueId");
        public static final DataQuery TYPE = of("EntityType");
        public static final DataQuery ROTATION = of("Rotation");
        public static final DataQuery SCALE = of("Scale");
        public static final DataQuery CUSTOM_NAME = of("CustomName");

        public static final class Ageable {

            public static final int ADULT = 6000;
            public static final int CHILD = -24000;
        }

        public static final class Boat {

            public static final String BOAT_MAX_SPEED = "maxSpeed";
            public static final String BOAT_MOVE_ON_LAND = "moveOnLand";
            public static final String BOAT_OCCUPIED_DECELERATION_SPEED = "occupiedDecelerationSpeed";
            public static final String BOAT_UNOCCUPIED_DECELERATION_SPEED = "unoccupiedDecelerationSpeed";
            public static final float DEFAULT_MAX_SPEED = 0.9f;
            public static final double OCCUPIED_DECELERATION_SPEED = 0D;
            public static final double UNOCCUPIED_DECELERATION_SPEED = 0.8D;
            public static final boolean MOVE_ON_LAND = false;
        }

        public static final class Creeper {

            public static final int DEFAULT_EXPLOSION_RADIUS = 3;
            public static final int STATE_IDLE = -1;
            public static final int STATE_PRIMED = 1;
            public static final int FUSE_DURATION = 30;
        }

        public static final class EnderCrystal {

            public static final int DEFAULT_EXPLOSION_STRENGTH = 6;
        }

        public static final class FallingBlock {

            public static final double DEFAULT_MAX_FALL_DAMAGE = 40;
            public static final int DEFAULT_FALL_TIME = 1;
            public static final boolean DEFAULT_CAN_HURT_ENTITIES = false;

        }

        public static final class Fireball {

            public static final int DEFAULT_EXPLOSION_RADIUS = 1;
        }

        public static final class Firework {

            public static final int DEFAULT_EXPLOSION_RADIUS = 0;
            public static final String EXPLOSION = "Explosion";
        }

        public static final class Horse {

            public static final Supplier<HorseStyle> DEFAULT_STYLE = HorseStyles.NONE;
            public static final Supplier<HorseColor> DERAULT_TYPE = HorseColors.WHITE;

            private Horse() {
            }
        }

        public static final class Item {

            public static final int MAX_PICKUP_DELAY = Short.MAX_VALUE;
            public static final int DEFAULT_PICKUP_DELAY = 0;
            public static final double DEFAULT_ITEM_MERGE_RADIUS = 0.5D;
            public static final int MIN_DESPAWN_DELAY = Short.MIN_VALUE;

            public static final int MAGIC_NO_PICKUP = Constants.Entity.Item.MAX_PICKUP_DELAY;
            public static final int MAGIC_NO_DESPAWN = Constants.Entity.Item.MIN_DESPAWN_DELAY;
            public static final int INFINITE_PICKUP_DELAY = 32767;

            private Item() {
            }

        }

        public static final class Llama {

            public static final Supplier<LlamaType> DEFAULT_TYPE = LlamaTypes.WHITE;
        }

        public static final class Minecart {

            public static final double DEFAULT_AIRBORNE_MOD = 0.94999998807907104D;
            public static final double DEFAULT_DERAILED_MOD = 0.5D;
            public static final String MINECART_TYPE = "Type";
            public static final double DEFAULT_MAX_SPEED = 0.4D;
            public static final double DEFAULT_FURNACE_MAX_SPEED = 0.2D;
            public static final String MAX_SPEED = "maxSpeed";
            public static final String SLOW_WHEN_EMPTY = "slowWhenEmpty";
            public static final String AIRBORNE_MODIFIER = "airborneModifier";
            public static final String DERAILED_MODIFIER = "derailedModifier";
            public static final int DEFAULT_FUSE_DURATION = 80;
        }

        public static final class Cat {

            public static final Supplier<CatType> DEFAULT_TYPE = CatTypes.WHITE;
        }

        public static final class Panda {

            public static final int UNHAPPY_TIME = 32;
        }

        public static final class Parrot {

            public static final Supplier<ParrotType> DEFAULT_TYPE = ParrotTypes.RED_BLUE;
        }

        public static final class Player {

            public static final double DEFAULT_FLYING_SPEED = 0.05D;
            public static final double DEFAULT_HEALTH_SCALE = 20D;
            public static final String INVENTORY = "Inventory";
            public static final String INVULNERABLE = "Invulnerable";
            public static final String SELECTED_ITEM_SLOT = "SelectedItemSlot";
            public static final String ENDERCHEST_INVENTORY = "EnderItems";
            // User
            public static final DataQuery UUID = of("UUID");
            public static final DataQuery NAME = of("Name");
            public static final DataQuery SPAWNS = of("Spawns");
            public static final float PLAYER_WIDTH = 0.6F;
            public static final float PLAYER_HEIGHT = 1.8F;
            public static final int TRACKING_RANGE = 32;
            public static final float PLAYER_Y_OFFSET = -0.35F;

            public static final class Abilities {
                public static final DataQuery IS_FLYING = of("abilities", "flying");
                public static final DataQuery CAN_FLY = of("abilities", "mayfly");
            }
        }

        public static final class PrimedTNT {

            public static final int DEFAULT_EXPLOSION_RADIUS = 4;

            public static final int DEFAULT_FUSE_DURATION = 80;
        }

        public static final class Rabbit {

            public static final Supplier<RabbitType> DEFAULT_TYPE = RabbitTypes.WHITE;
        }

        public static final class Ravager {

            public static final int ROAR_TIME = 10;
        }

        public static final class Wither {

            public static final int DEFAULT_FUSE_DURATION = 220;
        }

        public static final class WitherSkull {

            public static final int DEFAULT_EXPLOSION_RADIUS = 1;
            public static final float DEFAULT_WITHER_CREATED_SKULL_DAMAGE = 8.0f;
            public static final float DEFAULT_NO_SOURCE_SKULL_DAMAGE = 5.0f;
        }
    }

    public static final class BlockChangeFlags {

        /* TODO - Re-evaluate how the flags are used, The current flow of a World#setBlockState with an example of 3
            goes as follows:
            (3 & 2 != 0) && (!world.isClientSide || 3 & 4 == 0) && (world.isClientSide || chunk.getLocationType().isTicking) ? world.notifyBlockUpdate() (send update to client)
            (!world.isClientSide && (3 & 1 != 0)) ? world.notifyNeighbors()
            3 & 16 == 0 ? {
              newFlag = 3 & -2 = 2;
              originalState.updateDiagonal(world, pos, 2);
              newState.notifyNeighbors(world, pos, 2);
              newState.updateDiagonalNeighbors(world, pos, 2);
            }

            The tricky part is in the updateDiagonal and notifyNeighbors, currently updateDiagonal is used by redstone wire
            to update placement of diagonally oriented neighboring blocks, and of course, it's a -2 anded, so it's negating the client notification update
            for any new blocks changed, but retaining neighbor updates
            notifyNeighbors however is slightly different:
            it goes through to update neighboring blocks on the new state's position in relation to the neighbor's state based on the neighbor's decision of what block state it should replace itself with, and as usual
             disabling the neighbor  notification since the flag is already anded with -2 (2's complement)

             The bigger issue here though is that Block.replaceBlock does some really silly things:
             if the new state is air, it will do a world.destroyBlock(pos, 2 & 32 == 0 [true]) to drop the old block
             Otherwise, the world.setBlockState(pos, newState, 2 & -33 [ basically says to allow future breakages and update neighbors] )

         */

        /**
         * Calls {@link net.minecraft.world.level.Level#blockUpdated(BlockPos, net.minecraft.world.level.block.Block)}
         * if the flag is set.
         */
        public static final int BLOCK_UPDATED =   1 << 0; // 1
        /**
         * Calls {@link net.minecraft.world.level.Level#sendBlockUpdated(BlockPos, BlockState, BlockState, int)}
         * if the flag is set (which is basically notifying clients).
         */
        public static final int NOTIFY_CLIENTS =  1 << 1; // 2
        /**
         * Stops the blocks from being marked for a render update if set (defaults to flag & 4 == 0)
         */
        public static final int IGNORE_RENDER =   1 << 2; // 4
        /**
         * Makes the block be re-rendered immediately, on the main thread.
         * If {@link #IGNORE_RENDER} is set, then this will be ignored.
         */
        public static final int FORCE_RE_RENDER = 1 << 3; // 8
        /**
         * Causes neighboring states to be notified of changes (including diagonal positions),
         * which effectively calls
         * {@link net.minecraft.world.level.block.state.BlockState#updateNeighbourShapes(LevelAccessor, BlockPos, int)}
         * and
         * {@link net.minecraft.world.level.block.state.BlockState#updateIndirectNeighbourShapes(LevelAccessor, BlockPos, int)}
         * if unset (defaults to flag & 16 == 0)
         */
        public static final int DENY_NEIGHBOR_SHAPE_UPDATE =   1 << 4; // 16
        /**
         * If unset, allows for a block being destroyed to drop itself, used in
         * {@link net.minecraft.world.level.block.Block#updateOrDestroy(BlockState, BlockState, LevelAccessor, BlockPos, int, int)}.
         */
        public static final int NEIGHBOR_DROPS =  1 << 5; // 32
        /**
         * Tell the block being changed that it's being moved, rather than being replaced/removed.
         * The flag, if set, is set as a boolean to {@link BlockState#onPlace(Level, BlockPos, BlockState, boolean)}
         * by means of calling
         * {@link net.minecraft.world.level.chunk.LevelChunk#setBlockState(BlockPos, BlockState, boolean)}
         */
        public static final int BLOCK_MOVING =    1 << 6; // 64
        /**
         * Used in {@link Level#setBlock(BlockPos, BlockState, int)}  as {@code (var3 & 128) == 0} to
         * check if lighting is queued for the block change.
         */
        public static final int LIGHTING_UPDATES = 1 << 7; // 128 if set, blocks lighting updates
        public static final int STRUCTURE_PLACEMENT = 1 << 8; // 256 if set, blocks structure placement

        public static final int PHYSICS_MASK =    1 << 16; // Sponge Added mask, because vanilla doesn't support it yet
        public static final int PATHFINDING_UPDATES = 1 << 17; // Sponge Added mask, because vanilla doesn't allow bypassing notifications to ai pathfinders
        // All of these flags are what we "expose" to the API
        // The flags that are naturally inverted are already inverted here by being masked in
        // with the opposite OR.
        // Example: If we DO want physics, we don't include the physics flag, if we DON'T want physics, we | it in.
        public static final int FORCED_RESTORE = Constants.BlockChangeFlags.NOTIFY_CLIENTS
            | Constants.BlockChangeFlags.PHYSICS_MASK
            | Constants.BlockChangeFlags.FORCE_RE_RENDER
            | Constants.BlockChangeFlags.NEIGHBOR_DROPS
            | Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE
            | Constants.BlockChangeFlags.STRUCTURE_PLACEMENT
            ;
        public static final int DEFAULT = Constants.BlockChangeFlags.BLOCK_UPDATED
            | Constants.BlockChangeFlags.NOTIFY_CLIENTS;
        public static final int NONE = Constants.BlockChangeFlags.PHYSICS_MASK
            | Constants.BlockChangeFlags.DENY_NEIGHBOR_SHAPE_UPDATE
            | Constants.BlockChangeFlags.NEIGHBOR_DROPS
            | Constants.BlockChangeFlags.PATHFINDING_UPDATES
            | Constants.BlockChangeFlags.LIGHTING_UPDATES
            ;


    }

    public static final class NBT {

        public static final byte TAG_END = 0;
        public static final byte TAG_BYTE = 1;
        public static final byte TAG_SHORT = 2;
        public static final byte TAG_INT = 3;
        public static final byte TAG_LONG = 4;
        public static final byte TAG_FLOAT = 5;
        public static final byte TAG_DOUBLE = 6;
        public static final byte TAG_BYTE_ARRAY = 7;
        public static final byte TAG_STRING = 8;
        public static final byte TAG_LIST = 9;
        public static final byte TAG_COMPOUND = 10;
        public static final byte TAG_INT_ARRAY = 11;
        public static final byte TAG_LONG_ARRAY = 12;
        public static final byte TAG_ANY_NUMERIC = 99;

        public static CompoundTag filterSpongeCustomData(final CompoundTag rootCompound) {
            if (rootCompound.contains(Forge.FORGE_DATA, NBT.TAG_COMPOUND)) {
                final CompoundTag forgeCompound = rootCompound.getCompound(Forge.FORGE_DATA);
                if (forgeCompound.contains(Sponge.Data.V2.SPONGE_DATA, NBT.TAG_COMPOUND)) {
                    NBT.cleanseInnerCompound(forgeCompound);
                }
                if (forgeCompound.isEmpty()) {
                    rootCompound.remove(Forge.FORGE_DATA);
                }
            } else if (rootCompound.contains(Sponge.Data.V2.SPONGE_DATA, NBT.TAG_COMPOUND)) {
                NBT.cleanseInnerCompound(rootCompound);
            }
            return rootCompound;
        }

        private static void cleanseInnerCompound(final CompoundTag compound) {
            final CompoundTag inner = compound.getCompound(Sponge.Data.V2.SPONGE_DATA);
            if (inner.isEmpty()) {
                compound.remove(Sponge.Data.V2.SPONGE_DATA);
            }
        }

        public static ListTag newDoubleNBTList(final double... numbers) {
            final ListTag nbttaglist = new ListTag();

            for (final double d1 : numbers) {
                nbttaglist.add(DoubleTag.valueOf(d1));
            }

            return nbttaglist;
        }

        public static ListTag newFloatNBTList(final float... numbers) {
            final ListTag nbttaglist = new ListTag();

            for (final float f : numbers) {
                nbttaglist.add(FloatTag.valueOf(f));
            }

            return nbttaglist;
        }
    }

    /**
     * Compatibility constants used by Forge that may
     * or may not be used in Common, but are needed in
     * SpongeVanilla.
     */
    public static class Forge {

        public static final String PERSISTED_NBT_TAG = "PlayerPersisted";
        public static final String FORGE_DATA = "ForgeData";
        public static final DataQuery FORGE_DATA_ROOT = of(Forge.FORGE_DATA);
        public static final String FORGE_CAPS = "ForgeCaps";
        /**
         * Cross compatibility so that Sponge's multi-world format is in sync
         * with Forge.
         */
        public static final String USED_DIMENSION_IDS = "UsedIDs";
    }

    public static final class Bukkit {

        // Legacy migration tags from Bukkit
        public static final String BUKKIT = "bukkit";
        public static final String BUKKIT_FIRST_PLAYED = "firstPlayed";
        public static final String BUKKIT_LAST_PLAYED = "lastPlayed";
    }

    public static final class Canary {

        // Legacy migration tags from CanaryMod
        public static final String ROOT = "Canary";
        public static final String FIRST_JOINED = "FirstJoin";
        public static final String LAST_JOINED = "LastJoin";
    }

    public static final class Legacy {

        public static final String LEGACY_DIMENSION_ARRAY = "DimensionArray";

        private Legacy() {
        }

        public static final class Entity {

            public static final String UUID_LEAST_1_8 = "uuid_least";
            public static final String UUID_MOST_1_8 = "uuid_most";
            public static final int TRACKER_ID_VERSION = 0;

            private Entity() {
            }
        }

        public static final class World {

            public static final String WORLD_UUID_LEAST_1_8 = "uuid_least";
            public static final String WORLD_UUID_MOST_1_8 = "uuid_most";
            public static final int WORLD_UUID_1_9_VERSION = 0;

            private World() {
            }
        }
    }

    public static final class GameProfile {

        public static final DataQuery SKIN_UUID = of("SkinUUID");
        // RepresentedPlayerData
        public static final DataQuery GAME_PROFILE_ID = of("Id");
        public static final DataQuery GAME_PROFILE_NAME = of("Name");

        public static final String DUMMY_NAME = "[sponge]";
    }

    public static final class Block {

        // Blocks
        public static final DataQuery BLOCK_STATE = of("BlockState");
        public static final DataQuery BLOCK_EXTENDED_STATE = of("BlockExtendedState");
        public static final DataQuery BLOCK_TYPE = of("BlockType");
        public static final DataQuery BLOCK_STATE_UNSAFE_META = of("UnsafeMeta");
        public static final DataQuery BLOCK_STATE_MATCHER = of("BlockStateMatcher");
        public static final int PIXELS_PER_BLOCK = 16;
    }

    public static final class DataSerializers {

        // Java API Queries for DataSerializers
        public static final DataQuery LOCAL_TIME_HOUR = of("LocalTimeHour");
        public static final DataQuery LOCAL_TIME_MINUTE = of("LocalTimeMinute");
        public static final DataQuery LOCAL_TIME_SECOND = of("LocalTimeSecond");
        public static final DataQuery LOCAL_TIME_NANO = of("LocalTimeNano");
        public static final DataQuery LOCAL_DATE_YEAR = of("LocalDateYear");
        public static final DataQuery LOCAL_DATE_MONTH = of("LocalDateMonth");
        public static final DataQuery LOCAL_DATE_DAY = of("LocalDateDay");
        public static final DataQuery ZONE_TIME_ID = of("ZoneDateTimeId");
        public static final DataQuery X_POS = of("x");
        public static final DataQuery Y_POS = of("y");
        public static final DataQuery Z_POS = of("z");
        public static final DataQuery W_POS = of("w");
        public static final DataQuery DIRECTION = of("Direction");
    }

    public static final class Fluids {

        // Fluids
        public static final DataQuery FLUID_TYPE = of("FluidType");
        public static final DataQuery FLUID_VOLUME = of("FluidVolume");
        public static final DataQuery FLUID_STATE = of("FluidState");
    }

    public static final class ItemStack {

        public static final DataQuery COUNT = of("count");
        public static final DataQuery TYPE = of("id");
        public static final DataQuery COMPONENTS = of("components");
        public static final DataQuery CUSTOM_DATA = of("minecraft:custom_data");
        public static final DataQuery DAMAGE = of("minecraft:damage");

        @Deprecated
        public static final class V2 {

            public static final DataQuery DAMAGE_VALUE = of("UnsafeDamage");
            // ItemStacks
            public static final DataQuery COUNT = of("Count");
            public static final DataQuery TYPE = of("ItemType");
        }

        // Previously only ItemStackSnapshot
        public static final class Data {

            public static final int DUPLICATE_MANIPULATOR_DATA_VERSION = 1;
            public static final int REMOVED_DUPLICATE_DATA = 2;
            public static final int DATA_COMPONENTS = 3;
            public static final int CURRENT_VERSION = Data.DATA_COMPONENTS;
        }
    }

    public static final class Map {
        public static final String MAP_INFO_DATA_PROVIDER_NAME = "map_info";
        public static final String MAP_INDEX_DATA_NAME = "idcounts";
        public static final String MAP_ID = "map";

        public static final String MAP_UUID_INDEX = "MapUUIDs";

        public static final DataQuery MAP_UNSAFE_ID = of("UnsafeMapId");
        public static final DataQuery MAP_DATA = of("MapData");
        public static final DataQuery SHADE_NUM = of("shade");
        public static final DataQuery COLOR_INDEX = of("colorIndex");


        // @formatter:off
        public static final DataQuery MAP_LOCATION =           of("MapLocation");
        public static final DataQuery MAP_WORLD =              of("MapWorld");
        public static final DataQuery MAP_TRACKS_PLAYERS =     of("TracksPlayers");
        public static final DataQuery MAP_UNLIMITED_TRACKING = of("MapUnlimitedTracking");
        public static final DataQuery MAP_SCALE =              of("MapScale");
        public static final DataQuery MAP_CANVAS =             of("MapCanvas");
        public static final DataQuery MAP_LOCKED =             of("MapLocked");
        public static final DataQuery MAP_DECORATIONS =        of("MapDecorations");
        // @formatter:on

        // This need to be what they are to be easily convertable to MC NBT
        public static final DataQuery DECORATION_TYPE = of("type");
        public static final DataQuery DECORATION_ID = of("id");
        public static final DataQuery DECORATION_X = of("x");
        public static final DataQuery DECORATION_Y = of("z"); // This isn't a mistake
        public static final DataQuery DECORATION_ROTATION = of("rot");
        public static final DataQuery NAME = of("Name");

        // Sponge's way to save decorations, to ensure persistence
        public static final String DECORATIONS_KEY = "Decorations";
        // Key in the map for getting the highest map number
        public static final String ID_COUNTS_KEY = "map";
        // Used to add a UUID to maps. Prefixed with sponge- to show it is added by sponge
        public static final String SPONGE_UUID_KEY = "sponge-uuid";

        // Doesn't particulary matter what this is, just something that identifies it
        public static final String DECORATION_KEY_PREFIX = "sponge-";

        // Filled maps
        public static final int DEFAULT_MAP_SCALE = 0;
        public static final int MIN_MAP_SCALE = 0;
        public static final int MAX_MAP_SCALE = Byte.MAX_VALUE;
        public static final boolean DEFAULT_TRACKS_PLAYERS = true;
        public static final boolean DEFAULT_UNLIMITED_TRACKING = false;
        public static final boolean DEFAULT_MAP_LOCKED = false;
        public static final int MAP_SIZE = 16384;
        public static final int MAP_MAX_INDEX = 127;
        public static final int MAP_PIXELS = 128;
        public static final int MAP_SHADES = 4;

        // Colors are multiplied by something then divided by 255 to make the true RGB displayed
        public static final int SHADE_DIVIDER = 255;

        // Converts directions into a byte from 0-15 relating to the
        // BiMap - Allows Direction -> Byte and Byte -> Direction easily.
        public static final BiMap<Direction, Byte> DIRECTION_CONVERSION_MAP = HashBiMap.create(16);

        static {
            DIRECTION_CONVERSION_MAP.put(Direction.SOUTH,             (byte) 0);
            DIRECTION_CONVERSION_MAP.put(Direction.SOUTH_SOUTHWEST,   (byte) 1);
            DIRECTION_CONVERSION_MAP.put(Direction.SOUTHWEST,         (byte) 2);
            DIRECTION_CONVERSION_MAP.put(Direction.WEST_SOUTHWEST,    (byte) 3);
            DIRECTION_CONVERSION_MAP.put(Direction.WEST,              (byte) 4);
            DIRECTION_CONVERSION_MAP.put(Direction.WEST_NORTHWEST,    (byte) 5);
            DIRECTION_CONVERSION_MAP.put(Direction.NORTHWEST,         (byte) 6);
            DIRECTION_CONVERSION_MAP.put(Direction.NORTH_NORTHWEST,   (byte) 7);
            DIRECTION_CONVERSION_MAP.put(Direction.NORTH,             (byte) 8);
            DIRECTION_CONVERSION_MAP.put(Direction.NORTH_NORTHEAST,   (byte) 9);
            DIRECTION_CONVERSION_MAP.put(Direction.NORTHEAST,         (byte) 10);
            DIRECTION_CONVERSION_MAP.put(Direction.EAST_NORTHEAST,    (byte) 11);
            DIRECTION_CONVERSION_MAP.put(Direction.EAST,              (byte) 12);
            DIRECTION_CONVERSION_MAP.put(Direction.EAST_SOUTHEAST,    (byte) 13);
            DIRECTION_CONVERSION_MAP.put(Direction.SOUTHEAST,         (byte) 14);
            DIRECTION_CONVERSION_MAP.put(Direction.SOUTH_SOUTHEAST,   (byte) 15);
            // This should always be 16 long, unless minecraft changes the supported amount of directions for maps.
        }
    }

    public static final class Particles {

        // Particle Effects
        public static final DataQuery PARTICLE_TYPE = of("Type");
        public static final DataQuery PARTICLE_OPTIONS = of("Options");
        public static final DataQuery PARTICLE_OPTION_KEY = of("Option");
        public static final DataQuery PARTICLE_OPTION_VALUE = of("Value");
    }

    public static final class Scoreboards {

        public static final int OBJECTIVE_PACKET_ADD = 0;
        public static final int OBJECTIVE_PACKET_REMOVE = 1;
    }

    public static final class Functional {

        public static final Comparator<DataContentUpdater> DATA_CONTENT_UPDATER_COMPARATOR =
            (o1, o2) -> ComparisonChain.start()
                .compare(o2.inputVersion(), o1.inputVersion())
                .compare(o2.outputVersion(), o1.outputVersion())
                .result();

        public static Comparator<Integer> intComparator() {
            return Integer::compareTo;
        }

        public static Comparator<Long> longComparator() {
            return Long::compareTo;
        }

        public static Comparator<Short> shortComparator() {
            return Short::compareTo;
        }

        public static Comparator<Byte> byteComparator() {
            return Byte::compareTo;
        }

        public static Comparator<Double> doubleComparator() {
            return Double::compareTo;
        }

        public static Comparator<Float> floatComparator() {
            return Float::compareTo;
        }
    }

    public static final class Command {

        public static final String TYPE = "type";
        public static final String ROOT = "root";
        public static final String LITERAL = "literal";
        public static final String ARGUMENT = "argument";
        public static final String CHILDREN = "children";
        public static final String PARSER = "parser";
        public static final String PROPERTIES = "properties";
        public static final String EXECUTABLE = "executable";
        public static final String REDIRECT = "redirect";

        public static final byte ROOT_NODE_BIT          =  0; // 000000
        public static final byte LITERAL_NODE_BIT       =  1; // 000001
        public static final byte ARGUMENT_NODE_BIT      =  2; // 000010
        public static final byte EXECUTABLE_BIT         =  4; // 000100
        public static final byte REDIRECT_BIT           =  8; // 001000
        public static final byte CUSTOM_SUGGESTIONS_BIT = 16; // 010000

        public static final ArgumentType<?> STANDARD_STRING_ARGUMENT_TYPE = StringArgumentType.string();
        public static final ArgumentType<?> GREEDY_STRING_ARGUMENT_TYPE = StringArgumentType.greedyString();
        public static final ArgumentType<?> NBT_ARGUMENT_TYPE = CompoundTagArgument.compoundTag();
        public static final ResourceLocationArgument RESOURCE_LOCATION_TYPE = ResourceLocationArgument.id();
        public static final String COMMAND_BLOCK_COMMAND = "";
        public static final String SELECTOR_COMMAND = "@";
        public static final String SPONGE_HELP_COMMAND = "sponge:help";
    }

    public static final class DirectionFunctions {

        public static net.minecraft.core.Direction getFor(final Direction direction) {
            switch (Objects.requireNonNull(direction)) {
                case UP:
                    return net.minecraft.core.Direction.UP;
                case DOWN:
                    return net.minecraft.core.Direction.DOWN;
                case WEST:
                    return net.minecraft.core.Direction.WEST;
                case SOUTH:
                    return net.minecraft.core.Direction.SOUTH;
                case EAST:
                    return net.minecraft.core.Direction.EAST;
                case NORTH:
                    return net.minecraft.core.Direction.NORTH;
                default:
                    throw new IllegalArgumentException("No matching direction found for direction: " + direction);
            }
        }

        public static Direction getFor(final net.minecraft.core.Direction facing) {
            switch (Objects.requireNonNull(facing)) {
                case UP:
                    return Direction.UP;
                case DOWN:
                    return Direction.DOWN;
                case WEST:
                    return Direction.WEST;
                case SOUTH:
                    return Direction.SOUTH;
                case EAST:
                    return Direction.EAST;
                case NORTH:
                    return Direction.NORTH;
                default:
                    throw new IllegalArgumentException("No matching enum facing direction found for direction: " + facing);
            }
        }


        public static Direction checkDirectionToHorizontal(final Direction dir) {
            switch (dir) {
                case EAST:
                    break;
                case NORTH:
                    break;
                case SOUTH:
                    break;
                case WEST:
                    break;
                default:
                    return Direction.NORTH;
            }
            return dir;
        }

        public static Direction checkDirectionNotUp(final Direction dir) {
            switch (dir) {
                case EAST:
                    break;
                case NORTH:
                    break;
                case SOUTH:
                    break;
                case WEST:
                    break;
                case DOWN:
                    break;
                default:
                    return Direction.NORTH;
            }
            return dir;
        }

        public static Direction checkDirectionNotDown(final Direction dir) {
            switch (dir) {
                case EAST:
                    break;
                case NORTH:
                    break;
                case SOUTH:
                    break;
                case WEST:
                    break;
                case UP:
                    break;
                default:
                    return Direction.NORTH;
            }
            return dir;
        }

        public static net.minecraft.core.Direction.Axis convertAxisToMinecraft(final Axis axis) {
            switch (axis) {
                case X:
                    return net.minecraft.core.Direction.Axis.X;
                case Y:
                    return net.minecraft.core.Direction.Axis.Y;
                case Z:
                    return net.minecraft.core.Direction.Axis.Z;
                default:
                    return net.minecraft.core.Direction.Axis.X;

            }
        }

        public static Axis convertAxisToSponge(final net.minecraft.core.Direction.Axis axis) {
            switch (axis) {
                case X:
                    return Axis.X;
                case Y:
                    return Axis.Y;
                case Z:
                    return Axis.Z;
                default:
                    return Axis.X;
            }
        }
    }

    public static final class Channels {

        /**
         * The transaction id of a "normal" payload packet during the login phase.
         */
        public static final int LOGIN_PAYLOAD_TRANSACTION_ID = Integer.MAX_VALUE;

        /**
         * The transaction id of a "normal" payload packet response during the login phase. Can always be ignored.
         */
        public static final int LOGIN_PAYLOAD_IGNORED_TRANSACTION_ID = Integer.MAX_VALUE - 1;

        /**
         * A channel Forge uses to wrap custom login packets in.
         */
        public static final ResourceKey FML_LOGIN_WRAPPER_CHANNEL = ResourceKey.of("fml", "loginwrapper");

        /**
         * A sponge channel used to sync channel registry information.
         */
        public static final ResourceKey SPONGE_CHANNEL_REGISTRY = ResourceKey.sponge("channel_registry");

        /**
         * A sponge channel used to determine the type of client connecting
         */
        public static final ResourceKey SPONGE_CLIENT_TYPE = ResourceKey.sponge("client_type");

        /**
         * A minecraft channel used to register channels keys.
         */
        public static final ResourceKey REGISTER_KEY = ResourceKey.minecraft("register");

        /**
         * A minecraft channel used to unregister channels keys.
         */
        public static final ResourceKey UNREGISTER_KEY = ResourceKey.minecraft("unregister");
    }

    public static final class KeyValueMatcher {

        public static final DataQuery VALUE = DataQuery.of("Value");
        public static final DataQuery OPERATOR = DataQuery.of("Operator");
        public static final DataQuery KEY = DataQuery.of("Key");
    }

    public static final class TickConversions {

        public static final int TICK_DURATION_MS = 50;
        public static final Duration EFFECTIVE_MINIMUM_DURATION = Duration.ofMillis(TickConversions.TICK_DURATION_MS);

        public static final int MINECRAFT_DAY_TICKS = 24000;
        public static final int MINECRAFT_HOUR_TICKS = TickConversions.MINECRAFT_DAY_TICKS / 24;
        public static final double MINECRAFT_MINUTE_TICKS = TickConversions.MINECRAFT_HOUR_TICKS / 60.0;
        public static final double MINECRAFT_SECOND_TICKS = TickConversions.MINECRAFT_MINUTE_TICKS / 60.0;
        public static final int MINECRAFT_EPOCH_OFFSET = 6000;
        public static final int INFINITE_TICKS = -1;
    }

    public static final class Universe {

        public static final class Weather {

            public static final DataQuery TYPE = DataQuery.of("Type");
            public static final DataQuery REMAINING_DURATION = DataQuery.of("RemainingDuration");

            public static final DataQuery RUNNING_DURATION = DataQuery.of("RunningDuration");
        }
    }
}
