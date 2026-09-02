package offlcersam.weaponfoundry;

import com.sector.bridge.SSFMLLogger;
import net.fabricmc.loader.api.FabricLoader;
import offlcersam.weaponfoundry.json.JsonParser;
import offlcersam.weaponfoundry.json.JsonValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Scans <gameDir>/weapons/<modName>/*.json to register each one as a weapon or an ammo item, depending on its top-level "type" field ("weapon" or "ammo").
 */
public final class WeaponFoundryLoader {
    private static final String WEAPONS_FOLDER_NAME = "weapons";

    // Track which id came from which mod folder, weapon ids and ammo ids are tracked separately since
    // they're written to two different item type ranges and can't collide with each other anyway.
    private static final Map<Integer, String> CLAIMED_WEAPON_IDS = new HashMap<>();
    private static final Map<Integer, String> CLAIMED_AMMO_IDS = new HashMap<>();

    private static boolean loaded;

    private WeaponFoundryLoader() {
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;

        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path weaponsRoot = gameDir.resolve(WEAPONS_FOLDER_NAME);

        if (!Files.isDirectory(weaponsRoot)) {
            SSFMLLogger.log(
                    "[WeaponFoundry] No \"" + WEAPONS_FOLDER_NAME
                            + "\" folder found at " + weaponsRoot
                            + " - nothing to load."
            );
            return;
        }

        int totalLoaded = 0;

        try (Stream<Path> modFolders = Files.list(weaponsRoot)) {
            for (Path modFolder : modFolders.filter(Files::isDirectory).toList()) {
                totalLoaded += loadModFolder(modFolder);
            }
        } catch (IOException e) {
            SSFMLLogger.log("[WeaponFoundry] Failed to list " + weaponsRoot + ": " + e);
            return;
        }

        SSFMLLogger.log(
                "[WeaponFoundry] Loaded " + totalLoaded
                        + " item(s) total from " + weaponsRoot
        );
    }

    private static int loadModFolder(Path modFolder) {
        String modName = modFolder.getFileName().toString();
        int loaded = 0;

        try (Stream<Path> jsonFiles = Files.list(modFolder)) {
            for (Path file : jsonFiles
                    .filter(p -> p.toString().toLowerCase().endsWith(".json"))
                    .toList()) {

                if (loadFile(modName, file)) {
                    loaded++;
                }
            }
        } catch (IOException e) {
            SSFMLLogger.log("[WeaponFoundry] Failed to list " + modFolder + ": " + e);
        }

        SSFMLLogger.log(
                "[WeaponFoundry] Loaded " + loaded
                        + " item(s) from weapon pack folder \"" + modName + "\""
        );

        return loaded;
    }

    private static boolean loadFile(String modName, Path file) {
        String text;

        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            SSFMLLogger.log("[WeaponFoundry] Could not read " + file + ": " + e);
            return false;
        }

        JsonValue root;

        try {
            root = JsonParser.parse(text);
        } catch (JsonValue.JsonException e) {
            SSFMLLogger.log("[WeaponFoundry] Invalid JSON in " + file + " (mod \"" + modName + "\"): " + e.getMessage());
            return false;
        }

        String type = root.getString("type", "weapon");

        return switch (type.toLowerCase()) {
            case "weapon" -> loadWeaponFile(modName, file, root);
            case "ammo" -> loadAmmoFile(modName, file, root);
            default -> {
                SSFMLLogger.log(
                        "[WeaponFoundry] Unknown \"type\" \"" + type + "\" in " + file
                                + " - expected \"weapon\" or \"ammo\"."
                );
                yield false;
            }
        };
    }

    private static boolean loadWeaponFile(String modName, Path file, JsonValue root) {
        WeaponDefinition def;

        try {
            def = WeaponDefinition.fromJson(root, file.getParent());
        } catch (RuntimeException e) {

            SSFMLLogger.log(
                    "[WeaponFoundry] Invalid weapon JSON in " + file
                            + " (mod \"" + modName + "\"): " + e.getMessage()
            );
            return false;
        }

        String owner = CLAIMED_WEAPON_IDS.get(def.id());

        if (owner != null) {
            SSFMLLogger.log(
                    "[WeaponFoundry] Skipping " + file
                            + ": weapon id " + def.id()
                            + " is already claimed by mod \"" + owner
                            + "\" - ids must be unique across all weapons/ folders."
            );
            return false;
        }

        try {
            WeaponRegistrar.registerWeapon(def);
        } catch (Exception e) {
            SSFMLLogger.log("[WeaponFoundry] Failed to register weapon id " + def.id() + " from " + file + ": " + e);
            return false;
        }

        CLAIMED_WEAPON_IDS.put(def.id(), modName);
        return true;
    }

    private static boolean loadAmmoFile(String modName, Path file, JsonValue root) {
        AmmoDefinition def;

        try {
            def = AmmoDefinition.fromJson(root, file.getParent());
        } catch (RuntimeException e) {

            SSFMLLogger.log(
                    "[WeaponFoundry] Invalid ammo JSON in " + file
                            + " (mod \"" + modName + "\"): " + e.getMessage()
            );
            return false;
        }

        String owner = CLAIMED_AMMO_IDS.get(def.id());

        if (owner != null) {
            SSFMLLogger.log(
                    "[WeaponFoundry] Skipping " + file
                            + ": ammo id " + def.id()
                            + " is already claimed by mod \"" + owner
                            + "\" - ids must be unique across all weapons/ folders."
            );
            return false;
        }

        try {
            AmmoRegistrar.registerAmmo(def);
        } catch (Exception e) {
            SSFMLLogger.log("[WeaponFoundry] Failed to register ammo id " + def.id() + " from " + file + ": " + e);
            return false;
        }

        CLAIMED_AMMO_IDS.put(def.id(), modName);
        return true;
    }
}