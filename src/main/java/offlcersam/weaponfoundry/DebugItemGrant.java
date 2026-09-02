package offlcersam.weaponfoundry;

import com.sector.bridge.SSFMLLogger;
import game.Player;

public final class DebugItemGrant {
    // Set to true to automatically deposit weapons/ammo when loading your character
    // Maybe make into config option if a config manager is made.
    private static final boolean ENABLE_DEBUG_GRANT = true;
    private static final String DEBUG_CHARACTER_NAME = "WTEST";

    private DebugItemGrant() { }

    public static void grantItemsToDebugCharacter() {
        if (!ENABLE_DEBUG_GRANT) {
            return;
        }
        if (!DEBUG_CHARACTER_NAME.equalsIgnoreCase(Player.currentName)) {
            return;
        }
        if (Player.ship == null || Player.ship.cargo == null) {
            SSFMLLogger.log("[WeaponFoundry] Could not grant items: player cargo is not loaded.");
            return;
        }

        int[] weapons = WeaponRegistrar.getWeaponDatabaseIDs();
        for (int weaponID : weapons) {Player.ship.cargo.add(weaponID, 1);}

        int[] ammo = AmmoRegistrar.getAmmoDatabaseIDs();
        for (int ammoID : ammo) {Player.ship.cargo.add(ammoID, 100);}

        SSFMLLogger.log(
                "[WeaponFoundry] Granted "
                        + weapons.length
                        + " weapon(s) and "
                        + ammo.length
                        + " ammo stack(s) to WTEST cargo hold successfully."
        );
    }
}