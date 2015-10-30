package org.spongepowered.common.registry.type;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.effect.EntityWeatherEffect;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.entity.projectile.EntityFishHook;
import org.spongepowered.api.data.type.HorseColors;
import org.spongepowered.api.data.type.HorseStyles;
import org.spongepowered.api.data.type.HorseVariants;
import org.spongepowered.api.data.type.OcelotTypes;
import org.spongepowered.api.data.type.RabbitTypes;
import org.spongepowered.api.data.type.SkeletonTypes;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.common.Sponge;
import org.spongepowered.common.entity.SpongeEntityConstants;
import org.spongepowered.common.entity.SpongeEntityType;
import org.spongepowered.common.entity.living.human.EntityHuman;
import org.spongepowered.common.registry.CatalogRegistryModule;
import org.spongepowered.common.registry.CustomCatalogRegistration;
import org.spongepowered.common.registry.RegisterCatalog;
import org.spongepowered.common.registry.RegistryHelper;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class EntityTypeRegistryModule implements CatalogRegistryModule<EntityType> {

    @RegisterCatalog(EntityTypes.class)
    protected final Map<String, EntityType> entityTypeMappings = Maps.newHashMap();

    public final Map<Class<? extends Entity>, EntityType> entityClassToTypeMappings = Maps.newHashMap();

    private static EntityTypeRegistryModule instance;

    public EntityTypeRegistryModule() {
        instance = this;
    }

    public static EntityTypeRegistryModule getInstance() {
        return checkNotNull(instance);
    }

    public void registerEntityType(EntityType type) {
        this.entityTypeMappings.put(type.getId(), type);
        this.entityClassToTypeMappings.put(((SpongeEntityType) type).entityClass, type);
    }

    @Override
    public Optional<EntityType> getById(String id) {
        if (!checkNotNull(id).contains(":")) {
            id = "minecraft:" + id;
        }
        return Optional.ofNullable(this.entityTypeMappings.get(id.toLowerCase()));
    }

    @Override
    public Collection<EntityType> getAll() {
        return ImmutableList.copyOf(this.entityTypeMappings.values());
    }

    @Override
    public void registerDefaults() {
        this.entityTypeMappings.put("item", newEntityTypeFromName("Item"));
        this.entityTypeMappings.put("experience_orb", newEntityTypeFromName("XPOrb"));
        this.entityTypeMappings.put("leash_hitch", newEntityTypeFromName("LeashKnot"));
        this.entityTypeMappings.put("painting", newEntityTypeFromName("Painting"));
        this.entityTypeMappings.put("arrow", newEntityTypeFromName("Arrow"));
        this.entityTypeMappings.put("snowball", newEntityTypeFromName("Snowball"));
        this.entityTypeMappings.put("fireball", newEntityTypeFromName("LargeFireball", "Fireball"));
        this.entityTypeMappings.put("small_fireball", newEntityTypeFromName("SmallFireball"));
        this.entityTypeMappings.put("ender_pearl", newEntityTypeFromName("ThrownEnderpearl"));
        this.entityTypeMappings.put("eye_of_ender", newEntityTypeFromName("EyeOfEnderSignal"));
        this.entityTypeMappings.put("splash_potion", newEntityTypeFromName("ThrownPotion"));
        this.entityTypeMappings.put("thrown_exp_bottle", newEntityTypeFromName("ThrownExpBottle"));
        this.entityTypeMappings.put("item_frame", newEntityTypeFromName("ItemFrame"));
        this.entityTypeMappings.put("wither_skull", newEntityTypeFromName("WitherSkull"));
        this.entityTypeMappings.put("primed_tnt", newEntityTypeFromName("PrimedTnt"));
        this.entityTypeMappings.put("falling_block", newEntityTypeFromName("FallingSand"));
        this.entityTypeMappings.put("firework", newEntityTypeFromName("FireworksRocketEntity"));
        this.entityTypeMappings.put("armor_stand", newEntityTypeFromName("ArmorStand"));
        this.entityTypeMappings.put("boat", newEntityTypeFromName("Boat"));
        this.entityTypeMappings.put("rideable_minecart", newEntityTypeFromName("MinecartRideable"));
        this.entityTypeMappings.put("chested_minecart", newEntityTypeFromName("MinecartChest"));
        this.entityTypeMappings.put("furnace_minecart", newEntityTypeFromName("MinecartFurnace"));
        this.entityTypeMappings.put("tnt_minecart", newEntityTypeFromName("MinecartTnt", "MinecartTNT"));
        this.entityTypeMappings.put("hopper_minecart", newEntityTypeFromName("MinecartHopper"));
        this.entityTypeMappings.put("mob_spawner_minecart", newEntityTypeFromName("MinecartSpawner"));
        this.entityTypeMappings.put("commandblock_minecart", newEntityTypeFromName("MinecartCommandBlock"));
        this.entityTypeMappings.put("creeper", newEntityTypeFromName("Creeper"));
        this.entityTypeMappings.put("skeleton", newEntityTypeFromName("Skeleton"));
        this.entityTypeMappings.put("spider", newEntityTypeFromName("Spider"));
        this.entityTypeMappings.put("giant", newEntityTypeFromName("Giant"));
        this.entityTypeMappings.put("zombie", newEntityTypeFromName("Zombie"));
        this.entityTypeMappings.put("slime", newEntityTypeFromName("Slime"));
        this.entityTypeMappings.put("ghast", newEntityTypeFromName("Ghast"));
        this.entityTypeMappings.put("pig_zombie", newEntityTypeFromName("PigZombie"));
        this.entityTypeMappings.put("enderman", newEntityTypeFromName("Enderman"));
        this.entityTypeMappings.put("cave_spider", newEntityTypeFromName("CaveSpider"));
        this.entityTypeMappings.put("silverfish", newEntityTypeFromName("Silverfish"));
        this.entityTypeMappings.put("blaze", newEntityTypeFromName("Blaze"));
        this.entityTypeMappings.put("magma_cube", newEntityTypeFromName("LavaSlime"));
        this.entityTypeMappings.put("ender_dragon", newEntityTypeFromName("EnderDragon"));
        this.entityTypeMappings.put("wither", newEntityTypeFromName("WitherBoss"));
        this.entityTypeMappings.put("bat", newEntityTypeFromName("Bat"));
        this.entityTypeMappings.put("witch", newEntityTypeFromName("Witch"));
        this.entityTypeMappings.put("endermite", newEntityTypeFromName("Endermite"));
        this.entityTypeMappings.put("guardian", newEntityTypeFromName("Guardian"));
        this.entityTypeMappings.put("pig", newEntityTypeFromName("Pig"));
        this.entityTypeMappings.put("sheep", newEntityTypeFromName("Sheep"));
        this.entityTypeMappings.put("cow", newEntityTypeFromName("Cow"));
        this.entityTypeMappings.put("chicken", newEntityTypeFromName("Chicken"));
        this.entityTypeMappings.put("squid", newEntityTypeFromName("Squid"));
        this.entityTypeMappings.put("wolf", newEntityTypeFromName("Wolf"));
        this.entityTypeMappings.put("mushroom_cow", newEntityTypeFromName("MushroomCow"));
        this.entityTypeMappings.put("snowman", newEntityTypeFromName("SnowMan"));
        this.entityTypeMappings.put("ocelot", newEntityTypeFromName("Ozelot"));
        this.entityTypeMappings.put("iron_golem", newEntityTypeFromName("VillagerGolem"));
        this.entityTypeMappings.put("horse", newEntityTypeFromName("EntityHorse"));
        this.entityTypeMappings.put("rabbit", newEntityTypeFromName("Rabbit"));
        this.entityTypeMappings.put("villager", newEntityTypeFromName("Villager"));
        this.entityTypeMappings.put("ender_crystal", newEntityTypeFromName("EnderCrystal"));
        this.entityTypeMappings.put("egg", new SpongeEntityType(-1, "Egg", EntityEgg.class));
        this.entityTypeMappings.put("fishing_hook", new SpongeEntityType(-2, "FishingHook", EntityFishHook.class));
        this.entityTypeMappings.put("lightning", new SpongeEntityType(-3, "Lightning", EntityLightningBolt.class));
        this.entityTypeMappings.put("weather", new SpongeEntityType(-4, "Weather", EntityWeatherEffect.class));
        this.entityTypeMappings.put("player", new SpongeEntityType(-5, "Player", EntityPlayerMP.class));
        this.entityTypeMappings.put("complex_part", new SpongeEntityType(-6, "ComplexPart", EntityDragonPart.class));
        this.entityTypeMappings.put("human", registerCustomEntity(EntityHuman.class, "Human", -7));
    }

    @SuppressWarnings("unchecked")
    private SpongeEntityType newEntityTypeFromName(String spongeName, String mcName) {
        return new SpongeEntityType((Integer) EntityList.stringToIDMapping.get(mcName), spongeName,
                                    (Class<? extends Entity>) EntityList.stringToClassMapping.get(mcName));
    }

    private SpongeEntityType newEntityTypeFromName(String name) {
        return newEntityTypeFromName(name, name);
    }

    @SuppressWarnings("unchecked")
    private SpongeEntityType registerCustomEntity(Class<? extends Entity> entityClass, String entityName, int entityId) {
        String entityFullName = String.format("%s.%s", Sponge.ECOSYSTEM_NAME, entityName);
        EntityList.classToStringMapping.put(entityClass, entityFullName);
        EntityList.stringToClassMapping.put(entityFullName, entityClass);
        return new SpongeEntityType(entityId, entityName, Sponge.ECOSYSTEM_NAME, entityClass);
    }

    @CustomCatalogRegistration
    public void registerCatalogs() {
        RegistryHelper.mapFields(EntityTypes.class, fieldName -> {
            if (fieldName.equals("UNKNOWN")) {
                // TODO Something for Unknown?
                return null;
            }
            EntityType entityType = this.entityTypeMappings.get(fieldName.toLowerCase());
            this.entityClassToTypeMappings
                .put(((SpongeEntityType) entityType).entityClass, entityType);
            // remove old mapping
            this.entityTypeMappings.remove(fieldName.toLowerCase());
            // add new mapping with minecraft id
            this.entityTypeMappings.put(entityType.getId(), entityType);
            return entityType;
        });

        RegistryHelper.mapFields(SkeletonTypes.class, SpongeEntityConstants.SKELETON_TYPES);
        RegistryHelper.mapFields(HorseColors.class, SpongeEntityConstants.HORSE_COLORS);
        RegistryHelper.mapFields(HorseVariants.class, SpongeEntityConstants.HORSE_VARIANTS);
        RegistryHelper.mapFields(HorseStyles.class, SpongeEntityConstants.HORSE_STYLES);
        RegistryHelper.mapFields(OcelotTypes.class, SpongeEntityConstants.OCELOT_TYPES);
        RegistryHelper.mapFields(RabbitTypes.class, SpongeEntityConstants.RABBIT_TYPES);
    }

}
