package offlcersam.weaponfoundry;

import offlcersam.weaponfoundry.json.JsonValue;

/**
 * Required AmmoDefinition for a consumable ammo item (Missiles, Rounds, or Fighters).
 */
public record AmmoDefinition(int id, Kind kind, int icon, String color, String name, String description, int tier,
                             String rarity, boolean market, double volume, long creditValue, Fx fx, Recipe recipe) {

    /** Which FX class's configureEFXandBonus(int) this ammo's bonuses are read by. */
    public enum Kind { MISSILE, RAIL, FIGHTER }

    /**
     * Optional crafting recipe for this ammo.
     */
    public record Recipe(String label, int productAmount, int blueprintId, int blueprintAmount,
                         Ingredient ingredientA, Ingredient ingredientB, Ingredient ingredientC) {
    }

    /** One ingredient slot in a Recipe - an item id and the amount of it consumed. */
    public record Ingredient(int id, int amount) {
    }

    /**
     * Per-kind visual/damage bonus fields. Not every field applies to every kind:
     * - RAIL only uses bonusPHDamage, bonusEMDamage, glowColor (maps to RailGunFX's "glow" field).
     * - MISSILE uses bonusPHDamage, bonusEMDamage, speed, scale, glowColor, baseColor.
     * - FIGHTER uses all of the above plus weaponColor and fighterGFXIndex.
     * Fields unused by a kind are simply left null in JSON and ignored by the corresponding mixin.
     */
    public record Fx(double bonusPHDamage, double bonusEMDamage, Double speed, Float scale, String glowColor,
                     String baseColor, String weaponColor, Integer fighterGFXIndex) {
    }

    /**
     * Parses and validates one ammo JSON object.
     * Throws JsonValue.JsonException with a specific field name on any missing/malformed required field.
     */
    public static AmmoDefinition fromJson(JsonValue root) {
        int id = root.get("id").asInt();
        Kind kind = parseKind(root.get("kind").asString());
        int icon = root.get("icon").asInt();
        String color = root.get("color").asString();
        String name = root.get("name").asString();
        String description = root.getString("description", "");
        int tier = root.get("tier").asInt();
        String rarity = root.get("rarity").asString();
        boolean market = root.getBoolean("market", false);
        double volume = root.get("volume").asDouble();
        long creditValue = (long) root.get("creditValue").asDouble();

        Fx fx = parseFx(root.get("fx"), kind);
        Recipe recipe = parseRecipe(root);

        return new AmmoDefinition(
                id, kind, icon, color, name, description, tier, rarity, market, volume, creditValue, fx, recipe
        );
    }

    /**
     * Parses the optional "recipe" section.
     * If it does not exist, the ammo simply isn't craftable, CraftingTableMixin skips ammo with a null recipe.
     */
    private static Recipe parseRecipe(JsonValue root) {
        JsonValue recipeValue = root.getOrNull("recipe");

        if (recipeValue == null || recipeValue.isNull()) {
            return null;
        }

        String label = recipeValue.get("label").asString();
        int productAmount = recipeValue.getInt("productAmount", 1);
        int blueprintId = recipeValue.get("blueprintId").asInt();
        int blueprintAmount = recipeValue.getInt("blueprintAmount", 1);

        java.util.List<JsonValue> ingredients = recipeValue.getArray("ingredients");

        if (ingredients.size() != 3) {
            throw new JsonValue.JsonException(
                    "recipe.ingredients must have exactly 3 entries, found " + ingredients.size()
            );
        }

        Ingredient a = new Ingredient(ingredients.get(0).get("id").asInt(), ingredients.get(0).get("amount").asInt());
        Ingredient b = new Ingredient(ingredients.get(1).get("id").asInt(), ingredients.get(1).get("amount").asInt());
        Ingredient c = new Ingredient(ingredients.get(2).get("id").asInt(), ingredients.get(2).get("amount").asInt());

        return new Recipe(label, productAmount, blueprintId, blueprintAmount, a, b, c);
    }

    private static Kind parseKind(String value) {
        try {
            return Kind.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new JsonValue.JsonException(
                    "Unknown ammo kind \"" + value + "\" - expected one of missile, rail, fighter"
            );
        }
    }

    private static Fx parseFx(JsonValue fx, Kind kind) {
        double bonusPHDamage = fx.get("bonusPHDamage").asDouble();
        double bonusEMDamage = fx.get("bonusEMDamage").asDouble();
        String glowColor = fx.get("glowColor").asString();

        if (kind == Kind.RAIL) {
            return new Fx(bonusPHDamage, bonusEMDamage, null, null, glowColor, null, null, null);
        }

        Double speed = fx.get("speed").asDouble();
        Float scale = fx.get("scale").asFloat();
        String baseColor = fx.get("baseColor").asString();

        if (kind == Kind.MISSILE) {
            return new Fx(bonusPHDamage, bonusEMDamage, speed, scale, glowColor, baseColor, null, null);
        }

        // FIGHTER
        String weaponColor = fx.get("weaponColor").asString();
        int fighterGFXIndex = fx.get("fighterGFXIndex").asInt();

        return new Fx(bonusPHDamage, bonusEMDamage, speed, scale, glowColor, baseColor, weaponColor, fighterGFXIndex);
    }
}
