package offlcersam.weaponfoundry;

import com.sector.bridge.SSFMLLogger;
import illuminatus.core.graphics.Color;
import items.ItemTypeConstantsInterface;
import items.TypeTag;
import items.lists.WeaponList;
import offlcersam.weaponfoundry.mixin.WeaponListAccessorMixin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON weapon registration.
 */
public final class WeaponRegistrar {

    /** Stores the base IDs of every weapon we add. */
    private static final List<Integer> REGISTERED_WEAPON_IDS = new ArrayList<>();

    /** Stores weapons that opted into market listings, with their resolved produce/consume flags. */
    private static final List<MarketListing> MARKET_WEAPON_LISTINGS = new ArrayList<>();

    /** Stores every successfully registered weapon's full definition, for anything that needs more than just an id (e.g. recipes). */
    private static final Map<Integer, WeaponDefinition> LOADED_WEAPONS = new HashMap<>();

    private WeaponRegistrar() {
    }

    /** Registers a weapon ID and remembers it for later use. */
    private static int registerWeaponID(int id) {
        REGISTERED_WEAPON_IDS.add(id);
        SSFMLLogger.log("[WeaponFoundry] Added weapon ID to registry: " + id);
        return id;
    }

    /** Returns database ID for all registered weapons. */
    public static int[] getWeaponDatabaseIDs() {
        int[] ids = new int[REGISTERED_WEAPON_IDS.size()];

        for (int i = 0; i < REGISTERED_WEAPON_IDS.size(); i++) {
            ids[i] = toDatabaseID(REGISTERED_WEAPON_IDS.get(i));
        }
        return ids;
    }

    /** Returns every weapon that opted into market registration, with its resolved produce/consume flags. */
    public static List<MarketListing> getMarketWeaponListings() {
        return List.copyOf(MARKET_WEAPON_LISTINGS);
    }

    /** Converts a weapon base ID into the game's weapon database/item ID. */
    public static int toDatabaseID(int weaponBaseId) {
        return ItemTypeConstantsInterface.WEAPON * 10000 + weaponBaseId;
    }

    /**
     * Registers a weapon from its JSON definition.
     */
    public static void registerWeapon(WeaponDefinition def) {
        Color color = resolveConstant(Color.class, def.color(), "color");
        TypeTag rarity = resolveConstant(TypeTag.class, def.rarity(), "rarity");

        switch (def.kind()) {
            case TURRET -> writeTurret(def, color, rarity, false);
            case BAY -> writeTurret(def, color, rarity, true);
            case SALVAGER -> writeSalvager(def, color, rarity);
            case PDU -> writePdu(def, color, rarity);
            case TETHER -> writeTether(def, color, rarity);
        }

        if (def.market() != null) {
            MARKET_WEAPON_LISTINGS.add(
                    new MarketListing(toDatabaseID(def.id()), def.market().produce(), def.market().consume())
            );
            SSFMLLogger.log(
                    "[WeaponFoundry] Registered weapon " + def.name()
                            + " for market listings (produce=" + def.market().produce()
                            + ", consume=" + def.market().consume() + ")"
            );
        }

        LOADED_WEAPONS.put(def.id(), def);

        SSFMLLogger.log("[WeaponFoundry] Registered weapon " + def.name() + " (id: " + def.id() + ")");
    }

    /** TURRET and BAY both need setBaseAttributes() called first - see WeaponList.write/writeBay. */
    private static void writeTurret(WeaponDefinition def, Color color, TypeTag rarity, boolean bay) {
        WeaponDefinition.TurretStats stats = def.turretStats();

        WeaponListAccessorMixin.invokeSetBaseAttributes(
                stats.weaponType(),
                stats.volume(),
                stats.creditValue(),
                stats.baseDamage(),
                stats.range(),
                stats.energyRatio()
        );

        int id = registerWeaponID(def.id());

        if (bay) {
            WeaponList.writeBay(
                    id,
                    def.icon(),
                    color,
                    def.name(),
                    def.description(),
                    def.tier(),
                    rarity,
                    stats.effectType(),
                    stats.accuracy(),
                    stats.reloadTime(),
                    stats.bonusCoef()
            );
        } else {
            WeaponList.write(
                    id,
                    def.icon(),
                    color,
                    def.name(),
                    def.description(),
                    def.tier(),
                    rarity,
                    stats.effectType(),
                    stats.accuracy(),
                    stats.reloadTime(),
                    stats.bonusCoef()
            );
        }
    }

    private static void writeSalvager(WeaponDefinition def, Color color, TypeTag rarity) {
        WeaponDefinition.SalvagerStats stats = def.salvagerStats();

        WeaponList.writeSalvager(
                registerWeaponID(def.id()),
                def.icon(),
                color,
                def.name(),
                def.description(),
                def.tier(),
                rarity,
                stats.unitVolume(),
                stats.creditValue(),
                stats.range(),
                stats.maxSalvageTier(),
                stats.salvagerItemBins(),
                stats.salvageChance(),
                stats.energyUsage()
        );
    }

    private static void writePdu(WeaponDefinition def, Color color, TypeTag rarity) {
        WeaponDefinition.PduStats stats = def.pduStats();

        WeaponList.writePDU(
                registerWeaponID(def.id()),
                def.icon(),
                color,
                def.name(),
                def.description(),
                def.tier(),
                rarity,
                stats.unitVolume(),
                stats.creditValue(),
                stats.targetRange(),
                stats.targetPower(),
                stats.targetAccuracy(),
                stats.energyUsage()
        );
    }

    private static void writeTether(WeaponDefinition def, Color color, TypeTag rarity) {
        WeaponDefinition.TetherStats stats = def.tetherStats();

        WeaponList.writeTether(
                registerWeaponID(def.id()),
                def.icon(),
                color,
                def.name(),
                def.description(),
                def.tier(),
                rarity,
                stats.unitVolume(),
                stats.creditValue(),
                stats.range(),
                stats.speedReduction(),
                stats.targetShieldDrain(),
                stats.targetEnergyDrain(),
                stats.targetShieldResist(),
                stats.targetArmorResist(),
                stats.energyUsage()
        );
    }

    /**
     * Resolves game constants such as Color.AZURE and TypeTag.UNCOMMON from their names stored in JSON.
     */
    private static <T> T resolveConstant(Class<T> type, String name, String fieldLabel) {
        try {
            return type.cast(type.getField(name.toUpperCase()).get(null));
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "Unknown " + fieldLabel + " " + name
                            + " - check the exact constant name on "
                            + type.getName()
            );
        }
    }

    /** Returns the number of registered weapons. */
    public static int getRegisteredWeaponCount() {
        return REGISTERED_WEAPON_IDS.size();
    }

    /** Returns every successfully registered weapon's full definition. */
    public static List<WeaponDefinition> getLoadedWeapons() {
        return List.copyOf(LOADED_WEAPONS.values());
    }
}