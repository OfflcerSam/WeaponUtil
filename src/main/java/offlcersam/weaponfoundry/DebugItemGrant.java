package offlcersam.weaponfoundry;

import com.sector.bridge.SSFMLLogger;
import game.Player;

public final class DebugItemGrant {
    private DebugItemGrant() { }

    public static void grantItemsToDebugCharacter() {
        if (!WeaponFoundryConfig.debugItemGrantEnabled()) {
            return;
        }

        String characterName = Player.currentName;
        
        boolean isMasterDebugName = WeaponFoundryConfig.debugItemGrantCharacterName().equalsIgnoreCase(characterName);

        int[] weapons;
        int[] ammo;

        if (isMasterDebugName) {
            weapons = WeaponRegistrar.getWeaponDatabaseIDs();
            ammo = AmmoRegistrar.getAmmoDatabaseIDs();
        } else {
            weapons = WeaponFoundryLoader.getWeaponDatabaseIDsForPack(characterName);
            ammo = WeaponFoundryLoader.getAmmoDatabaseIDsForPack(characterName);

            if (weapons.length == 0 && ammo.length == 0) {
                return;
            }
        }

        if (Player.ship == null || Player.ship.cargo == null) {
            SSFMLLogger.log("[WeaponFoundry] Could not grant items: player cargo is not loaded.");
            return;
        }

        for (int weaponID : weapons) {Player.ship.cargo.add(weaponID, 1);}
        for (int ammoID : ammo) {Player.ship.cargo.add(ammoID, 100);}

        SSFMLLogger.log(
                "[WeaponFoundry] Granted "
                        + weapons.length
                        + " weapon(s) and "
                        + ammo.length
                        + " ammo stack(s) to \""
                        + characterName
                        + "\" cargo hold successfully."
        );
    }
}