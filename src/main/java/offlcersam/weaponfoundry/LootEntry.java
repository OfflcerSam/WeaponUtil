package offlcersam.weaponfoundry;

import offlcersam.weaponfoundry.json.JsonValue;

import java.util.ArrayList;
import java.util.List;

public record LootEntry(int tier, int weight, boolean rare) {

    /** Matches _database.DropTable.MAX_TIER - tiers run 0-6 inclusive. */
    private static final int MAX_TIER = 6;

    /**
     * Parses the optional "lootTable" array off the given root object.
     * Returns an empty list if the field is absent, meaning the item simply isn't added to any generic drop pool.
     */
    public static List<LootEntry> parseList(JsonValue root) {
        List<LootEntry> entries = new ArrayList<>();

        JsonValue lootTableValue = root.getOrNull("lootTable");
        if (lootTableValue == null || lootTableValue.isNull()) {
            return entries;
        }

        for (JsonValue entry : lootTableValue.asArray()) {
            int tier = entry.get("tier").asInt();
            int weight = entry.getInt("weight", 1);
            boolean rare = entry.getBoolean("rare", false);

            if (tier < 0 || tier > MAX_TIER) {
                throw new JsonValue.JsonException("lootTable tier must be between 0 and " + MAX_TIER);
            }

            if (weight < 1) {
                throw new JsonValue.JsonException("lootTable weight must be at least 1");
            }

            entries.add(new LootEntry(tier, weight, rare));
        }

        return entries;
    }
}