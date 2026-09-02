package offlcersam.weaponfoundry;

import offlcersam.weaponfoundry.json.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Game update: the old DropTable.tierGearWeapon/tierRareGearWeapon split (a separate "rare" pool per tier) was removed along with DropTable itself.
 * The replacement gear/loot tables use a single pool per tier with a per-row probability instead, so "rare" has no direct equivalent anymore and has been dropped from this record.
 * <p>
 * weight is temporarily being treated as a percent chance (1-100) by LootTablePatcher, converted down to the 0.0-1.0 probability the new tables expect.
 * This is a placeholder for now.
 */
public record LootEntry(int tier, int weight) {

    /** Matches the new table format's tier range - tiers still run 0-6 inclusive. */
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

            if (tier < 0 || tier > MAX_TIER) {
                throw new JsonValue.JsonException("lootTable tier must be between 0 and " + MAX_TIER);
            }

            if (weight < 1) {
                throw new JsonValue.JsonException("lootTable weight must be at least 1");
            }

            entries.add(new LootEntry(tier, weight));
        }

        return entries;
    }
}