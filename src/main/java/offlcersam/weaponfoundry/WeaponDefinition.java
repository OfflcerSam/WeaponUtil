package offlcersam.weaponfoundry;

import offlcersam.weaponfoundry.json.JsonValue;

import java.nio.file.Path;
import java.util.List;

/**
 * Required WeaponDefinition for a weapon.
 */
public record WeaponDefinition(int id, Kind kind, int icon, String color, String name, String description,
                               int tier, String rarity, boolean market, TurretStats turretStats,
                               SalvagerStats salvagerStats, PduStats pduStats, TetherStats tetherStats,
                               Recipe recipe, List<LootEntry> lootTable) {

    /**
     * Which WeaponList.write*() family this weapon uses.
     * TURRET  -> WeaponList.write(...)     (normal weapon slot)
     * BAY     -> WeaponList.writeBay(...)  (fighter bay, launches FighterFX)
     * SALVAGER-> WeaponList.writeSalvager(...)
     * PDU     -> WeaponList.writePDU(...)
     * TETHER  -> WeaponList.writeTether(...)
     */
    public enum Kind { TURRET, BAY, SALVAGER, PDU, TETHER }

    /**
     * Stats for TURRET and BAY kinds.
     */
    public record TurretStats(int weaponType, float volume, long creditValue, float baseDamage, int range,
                              float energyRatio, int effectType, float accuracy, float reloadTime, float bonusCoef) {
    }

    /**
     * Stats for SALVAGER kind. Mirrors WeaponList.writeSalvager's parameter list exactly.
     */
    public record SalvagerStats(double unitVolume, long creditValue, float range, int maxSalvageTier,
                                int salvagerItemBins, float salvageChance, float energyUsage) {
    }

    /**
     * Stats for PDU kind. Mirrors WeaponList.writePDU's parameter list exactly.
     */
    public record PduStats(double unitVolume, long creditValue, float targetRange, float targetPower,
                           float targetAccuracy, float energyUsage) {
    }

    /**
     * Stats for TETHER kind. Mirrors WeaponList.writeTether's parameter list exactly.
     */
    public record TetherStats(double unitVolume, long creditValue, float range, float speedReduction,
                              float targetShieldDrain, float targetEnergyDrain, float targetShieldResist,
                              float targetArmorResist, float energyUsage) {
    }

    /**
     * Optional crafting recipe for this weapon.nt, int, int, int),
     * which always takes exactly a blueprint slot plus 3 fixed ingredient slots.
     */
    public record Recipe(String label, int blueprintId, int blueprintAmount, Ingredient ingredientA,
                         Ingredient ingredientB, Ingredient ingredientC) {
    }

    /**
     * One ingredient slot in a Recipe - an item id and the amount of it consumed.
     */
    public record Ingredient(int id, int amount) {
    }

    /**
     * Parses and validates one weapon JSON object.
     */
    public static WeaponDefinition fromJson(JsonValue root, Path baseDir) {
        int id = root.get("id").asInt();
        Kind kind = parseKind(root.get("kind").asString());
        int icon = WeaponFoundryIcons.resolveIcon(root.get("icon"), baseDir);
        String color = root.get("color").asString();
        String name = root.get("name").asString();
        String description = root.getString("description", "");
        int tier = root.get("tier").asInt();
        String rarity = root.get("rarity").asString();
        boolean market = root.getBoolean("market", false);

        TurretStats turretStats = null;
        SalvagerStats salvagerStats = null;
        PduStats pduStats = null;
        TetherStats tetherStats = null;

        switch (kind) {
            case TURRET, BAY -> turretStats = parseTurretStats(root);
            case SALVAGER -> salvagerStats = parseSalvagerStats(root);
            case PDU -> pduStats = parsePduStats(root);
            case TETHER -> tetherStats = parseTetherStats(root);
        }

        Recipe recipe = parseRecipe(root);
        List<LootEntry> lootTable = LootEntry.parseList(root);

        return new WeaponDefinition(
                id,
                kind,
                icon,
                color,
                name,
                description,
                tier,
                rarity,
                market,
                turretStats,
                salvagerStats,
                pduStats,
                tetherStats,
                recipe,
                lootTable
        );
    }

    private static Kind parseKind(String value) {
        try {
            return Kind.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new JsonValue.JsonException(
                    "Unknown weapon kind \"" + value + "\" - expected one of turret, bay, salvager, pdu, tether"
            );
        }
    }

    /** Parses the "turretStats" section, required for TURRET and BAY kinds. */
    private static TurretStats parseTurretStats(JsonValue root) {
        JsonValue stats = root.get("turretStats");

        return new TurretStats(
                stats.get("weaponType").asInt(),
                stats.get("volume").asFloat(),
                (long) stats.get("creditValue").asDouble(),
                stats.get("baseDamage").asFloat(),
                stats.get("range").asInt(),
                stats.get("energyRatio").asFloat(),
                stats.getInt("effectType", 0),
                stats.get("accuracy").asFloat(),
                stats.get("reloadTime").asFloat(),
                stats.getFloat("bonusCoef", -1.0F)
        );
    }

    /** Parses the "salvagerStats" section, required for SALVAGER kind. */
    private static SalvagerStats parseSalvagerStats(JsonValue root) {
        JsonValue stats = root.get("salvagerStats");

        return new SalvagerStats(
                stats.get("unitVolume").asDouble(),
                (long) stats.get("creditValue").asDouble(),
                stats.get("range").asFloat(),
                stats.get("maxSalvageTier").asInt(),
                stats.get("salvagerItemBins").asInt(),
                stats.get("salvageChance").asFloat(),
                stats.get("energyUsage").asFloat()
        );
    }

    /** Parses the "pduStats" section, required for PDU kind. */
    private static PduStats parsePduStats(JsonValue root) {
        JsonValue stats = root.get("pduStats");

        return new PduStats(
                stats.get("unitVolume").asDouble(),
                (long) stats.get("creditValue").asDouble(),
                stats.get("targetRange").asFloat(),
                stats.get("targetPower").asFloat(),
                stats.get("targetAccuracy").asFloat(),
                stats.get("energyUsage").asFloat()
        );
    }

    /** Parses the "tetherStats" section, required for TETHER kind. */
    private static TetherStats parseTetherStats(JsonValue root) {
        JsonValue stats = root.get("tetherStats");

        return new TetherStats(
                stats.get("unitVolume").asDouble(),
                (long) stats.get("creditValue").asDouble(),
                stats.get("range").asFloat(),
                stats.get("speedReduction").asFloat(),
                stats.get("targetShieldDrain").asFloat(),
                stats.get("targetEnergyDrain").asFloat(),
                stats.get("targetShieldResist").asFloat(),
                stats.get("targetArmorResist").asFloat(),
                stats.get("energyUsage").asFloat()
        );
    }

    /**
     * Parses the optional "recipe" section.
     * If it does not exist, the weapon simply isn't craftable, CraftingTableMixin skips weapons with a null recipe.
     */
    private static Recipe parseRecipe(JsonValue root) {
        JsonValue recipeValue = root.getOrNull("recipe");

        if (recipeValue == null || recipeValue.isNull()) {
            return null;
        }

        String label = recipeValue.get("label").asString();
        int blueprintId = recipeValue.get("blueprintId").asInt();
        int blueprintAmount = recipeValue.getInt("blueprintAmount", 1);

        java.util.List<JsonValue> ingredients = recipeValue.getArray("ingredients");

        // CraftingTable#addRecipe always takes exactly 3 ingredient slots - no vanilla overload takes fewer or more.
        if (ingredients.size() != 3) {
            throw new JsonValue.JsonException(
                    "recipe.ingredients must have exactly 3 entries, found " + ingredients.size()
            );
        }

        Ingredient a = new Ingredient(ingredients.get(0).get("id").asInt(), ingredients.get(0).get("amount").asInt());
        Ingredient b = new Ingredient(ingredients.get(1).get("id").asInt(), ingredients.get(1).get("amount").asInt());
        Ingredient c = new Ingredient(ingredients.get(2).get("id").asInt(), ingredients.get(2).get("amount").asInt());

        return new Recipe(label, blueprintId, blueprintAmount, a, b, c);
    }
}