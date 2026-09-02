package offlcersam.weaponfoundry;

import com.sector.bridge.SSFMLLogger;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces the old DropTable.tierGearWeapon/tierRareGearWeapon registration (removed by the game update)
 * with entries patched directly into the game's own data-driven loot/gear tables:
 * - gear_tables.sc  -> weapon lootTable entries (TURRET/BAY/SALVAGER/TETHER; PDU has no pool)
 * - loot_tables.sc  -> ammo lootTable entries (MISSILE/RAIL/FIGHTER; have drop pools)
 * <p>
 * The "rare" split from the old DropTable no longer has a direct equivalent.
 * The new tables use a single pool per tier with a per-row probability instead of two separate common/rare pools.
 * See LootEntry's javadoc for how "weight" is currently mapped onto that probability.
 * <p>
 * Both .sc files are read by the game from disk at "<gameDir>/resources/data/<file>" first, only falling back to the copy embedded in Sector_Space.jar if that disk file is missing.
 * Since that disk file persists across launches and normal (non-dev) play never refreshes it from the jar, this patcher wraps every row it inserts in "// WEAPONFOUNDRY:BEGIN/END" marker comments
 * and strips any previously-inserted block before re-inserting fresh ones each boot, so repeated launches don't accumulate duplicates and removed/renamed weapons/ammo don't leave stale rows behind.
 */
public final class LootTablePatcher {

    private static final String GEAR_TABLE_FILE = "gear_tables.sc";
    private static final String LOOT_TABLE_FILE = "loot_tables.sc";

    private static final String MARKER_BEGIN_PREFIX = "// WEAPONFOUNDRY:BEGIN";
    private static final String MARKER_END = "// WEAPONFOUNDRY:END";

    // Matches a table header line like "{ 10, Tier 0 Mixed Generic Weapons" and captures the index.
    private static final Pattern HEADER_PATTERN = Pattern.compile("^\\{\\s*(-?\\d+)\\s*,");

    private LootTablePatcher() {
    }

    /** One row to insert into a specific table index, tagged with the source id for the marker comment. */
    private record Insertion(int sourceId, String row) {
    }

    public static void patch() {
        patchFile(GEAR_TABLE_FILE, buildGearInsertions());
        patchFile(LOOT_TABLE_FILE, buildAmmoInsertions());
    }

    /** table index -> rows to insert, built from every loaded weapon's lootTable entries. */
    private static Map<Integer, List<Insertion>> buildGearInsertions() {
        Map<Integer, List<Insertion>> insertions = new LinkedHashMap<>();

        for (WeaponDefinition def : WeaponRegistrar.getLoadedWeapons()) {
            if (def.lootTable().isEmpty()) {
                continue;
            }

            Integer tableBase = weaponTableBase(def.kind());

            if (tableBase == null) {
                SSFMLLogger.log(
                        "[WeaponFoundry] lootTable ignored for " + def.name()
                                + " (id: " + def.id()
                                + ") - PDU weapons have no gear table pool in " + GEAR_TABLE_FILE + "."
                );
                continue;
            }

            for (LootEntry entry : def.lootTable()) {
                int tableIndex = tableBase + entry.tier();
                String row = formatRow(def.name(), entry);
                insertions.computeIfAbsent(tableIndex, k -> new ArrayList<>()).add(new Insertion(def.id(), row));
            }
        }

        return insertions;
    }

    /** table index -> rows to insert, built from every loaded ammo's lootTable entries. */
    private static Map<Integer, List<Insertion>> buildAmmoInsertions() {
        Map<Integer, List<Insertion>> insertions = new LinkedHashMap<>();

        for (AmmoDefinition def : AmmoRegistrar.getLoadedAmmo()) {
            if (def.lootTable().isEmpty()) {
                continue;
            }

            int tableBase = ammoTableBase(def.kind());

            for (LootEntry entry : def.lootTable()) {
                int tableIndex = tableBase + entry.tier();
                String row = formatRow(def.name(), entry);
                insertions.computeIfAbsent(tableIndex, k -> new ArrayList<>()).add(new Insertion(def.id(), row));
            }
        }

        return insertions;
    }

    /** Weapon kind -> base gear_tables.sc table index (add tier 0-6 to get the exact table). */
    private static Integer weaponTableBase(WeaponDefinition.Kind kind) {
        return switch (kind) {
            case TURRET, BAY -> 10; // "Tier N Mixed Generic Weapons"
            case SALVAGER -> 80;    // "Tier N Mixed Generic Salvagers"
            case TETHER -> 90;      // "Tier N Mixed Generic Tethers"
            case PDU -> null;       // no pool exists for PDUs
        };
    }

    /** Ammo kind -> base loot_tables.sc table index (add tier 0-6 to get the exact table). */
    private static int ammoTableBase(AmmoDefinition.Kind kind) {
        return switch (kind) {
            case MISSILE -> 80; // "Tier N Missile Ammo Drops"
            case RAIL -> 90;    // "Tier N Rifle Round Ammo Drops"
            case FIGHTER -> 100; // "Tier N Fighter Ammo Drops"
        };
    }

    /**
     * Formats one data row in the "Name, minQty, maxQty, probability" form the .sc files use.
     * minQty/maxQty are always 1/1 - weapons and ammo alike are single-unit installs/drops here.
     * <p>
     * weight -> probability mapping: weight is treated directly as a percent chance (1-100),
     * divided down to the 0.0-1.0 range the game's RandomizedItemTable expects.
     * This is a reinterpretation of the old "weight" field (previously a duplicate-entry count biasing a flat list) now that the game uses an explicit per-row probability instead.
     */
    private static String formatRow(String name, LootEntry entry) {
        double probability = Math.min(1.0, entry.weight() / 100.0);
        return name + ", 1, 1, " + String.format(Locale.ROOT, "%.4f", probability);
    }

    private static void patchFile(String filename, Map<Integer, List<Insertion>> insertions) {
        Path diskPath = FabricLoader.getInstance().getGameDir()
                .resolve("resources").resolve("data").resolve(filename);

        if (!Files.isRegularFile(diskPath) && !extractBaseFile(filename, diskPath)) {
            SSFMLLogger.log(
                    "[WeaponFoundry] Could not find or extract " + filename
                            + " - skipping lootTable patch for it."
            );
            return;
        }

        String original;

        try {
            original = Files.readString(diskPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            SSFMLLogger.log("[WeaponFoundry] Could not read " + diskPath + ": " + e);
            return;
        }

        String lineSeparator = original.contains("\r\n") ? "\r\n" : "\n";
        List<String> lines = new ArrayList<>(List.of(original.split("\r\n|\n", -1)));

        // A trailing empty element from a final newline shouldn't get its own line on write-back.
        boolean trailingNewline = !lines.isEmpty() && lines.get(lines.size() - 1).isEmpty();
        if (trailingNewline) {
            lines.remove(lines.size() - 1);
        }

        List<String> stripped = stripPreviousMarkers(lines);
        List<String> patched = insertRows(stripped, insertions);

        String result = String.join(lineSeparator, patched) + (trailingNewline ? lineSeparator : "");

        if (result.equals(original)) {
            return;
        }

        try {
            Files.writeString(diskPath, result, StandardCharsets.UTF_8);
        } catch (IOException e) {
            SSFMLLogger.log("[WeaponFoundry] Could not write " + diskPath + ": " + e);
            return;
        }

        int rowCount = insertions.values().stream().mapToInt(List::size).sum();
        SSFMLLogger.log("[WeaponFoundry] Patched " + rowCount + " lootTable row(s) into " + filename);
    }

    /** Copies the game's own embedded copy of the file to disk so we have a base to patch. */
    private static boolean extractBaseFile(String filename, Path diskPath) {
        try (InputStream in = game.Main.class.getResourceAsStream("/data/" + filename)) {
            if (in == null) {
                return false;
            }

            Files.createDirectories(diskPath.getParent());
            Files.copy(in, diskPath);
            return true;
        } catch (IOException e) {
            SSFMLLogger.log("[WeaponFoundry] Could not extract base " + filename + ": " + e);
            return false;
        }
    }

    /** Removes any "// WEAPONFOUNDRY:BEGIN" . . . "// WEAPONFOUNDRY:END" span from a previous patch pass. */
    private static List<String> stripPreviousMarkers(List<String> lines) {
        List<String> stripped = new ArrayList<>(lines.size());
        boolean inBlock = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith(MARKER_BEGIN_PREFIX)) {
                inBlock = true;
                continue;
            }

            if (trimmed.equals(MARKER_END)) {
                inBlock = false;
                continue;
            }

            if (!inBlock) {
                stripped.add(line);
            }
        }

        return stripped;
    }

    /** Walks the file tracking which table index is currently open, inserting rows just before its closing "}". */
    private static List<String> insertRows(List<String> lines, Map<Integer, List<Insertion>> insertions) {
        if (insertions.isEmpty()) {
            return lines;
        }

        List<String> output = new ArrayList<>(lines.size());
        int currentTable = Integer.MIN_VALUE;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.equals("}")) {
                List<Insertion> toInsert = insertions.remove(currentTable);

                if (toInsert != null) {
                    for (Insertion insertion : toInsert) {
                        output.add(MARKER_BEGIN_PREFIX + " " + insertion.sourceId());
                        output.add(insertion.row());
                        output.add(MARKER_END);
                    }
                }

                currentTable = Integer.MIN_VALUE;
                output.add(line);
                continue;
            }

            Matcher header = HEADER_PATTERN.matcher(trimmed);
            if (header.find()) {
                currentTable = Integer.parseInt(header.group(1));
            }

            output.add(line);
        }

        for (Integer missingTable : insertions.keySet()) {
            SSFMLLogger.log(
                    "[WeaponFoundry] Table index " + missingTable
                            + " not found - " + insertions.get(missingTable).size()
                            + " lootTable row(s) could not be placed."
            );
        }

        return output;
    }
}