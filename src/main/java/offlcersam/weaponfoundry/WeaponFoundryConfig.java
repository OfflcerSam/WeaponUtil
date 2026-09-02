package offlcersam.weaponfoundry;

import com.sector.bridge.SSFMLConfig;
import com.sector.bridge.SSFMLLogger;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * WeaponFoundry's config, backed by SSFML's new SSFMLConfig API.
 * Lives at{@code <gameDir>/config/weaponfoundry/weaponfoundry.cfg} once loaded.
 * <p>
 * Two options:
 * - enabledPacks: comma-separated list of weapons/ subfolder names to load.
 *   Blank (the default) means load every pack found, same as before this config existed.
 * - debugLogging: when true, logs one line per weapon/ammo/market listing registered instead of just per-pack and per-boot summaries.
 *   Off by default to keep normal logs quiet.
 */
public final class WeaponFoundryConfig {

    private static final String MOD_ID = "weaponfoundry";

    private static final String KEY_ENABLED_PACKS = "enabledPacks";
    private static final String KEY_DEBUG_LOGGING = "debugLogging";

    private static boolean loaded;
    private static SSFMLConfig.Config config;
    private static Set<String> enabledPacks = Set.of();

    private WeaponFoundryConfig() {
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;

        List<SSFMLConfig.ConfigEntry> schema = List.of(
                new SSFMLConfig.ConfigEntry(
                        KEY_ENABLED_PACKS, "",
                        "Comma-separated weapons/ subfolder names to load. Leave blank to load all of them."
                ),
                new SSFMLConfig.ConfigEntry(
                        KEY_DEBUG_LOGGING, "false",
                        "Logs one line per weapon/ammo/market listing registered, instead of just summaries."
                )
        );

        config = SSFMLConfig.load(MOD_ID, schema);
        enabledPacks = parsePackList(config.getString(KEY_ENABLED_PACKS));
    }

    /** True if no packs were explicitly listed (meaning: load everything), or this one was named. */
    public static boolean isPackEnabled(String packName) {
        return enabledPacks.isEmpty() || enabledPacks.contains(packName.toLowerCase());
    }

    public static boolean debugLogging() {
        return config != null && config.getBoolean(KEY_DEBUG_LOGGING);
    }

    /** Only logs when debugLogging is enabled - for per-item confirmations, not warnings/errors/summaries. */
    public static void debug(String message) {
        if (debugLogging()) {
            SSFMLLogger.log(message);
        }
    }

    private static Set<String> parsePackList(String raw) {
        Set<String> packs = new LinkedHashSet<>();

        for (String entry : raw.split(",")) {
            String trimmed = entry.trim();

            if (!trimmed.isEmpty()) {
                packs.add(trimmed.toLowerCase());
            }
        }

        return packs;
    }
}