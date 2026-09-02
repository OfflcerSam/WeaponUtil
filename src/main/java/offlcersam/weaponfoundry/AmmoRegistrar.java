package offlcersam.weaponfoundry;

import com.sector.bridge.SSFMLLogger;
import illuminatus.core.graphics.Color;
import items.ItemTypeConstantsInterface;
import items.TypeTag;
import items.lists.ConsumableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON ammo registration.
 */
public final class AmmoRegistrar {

    /** Resolved Fx with Color objects instead of Strings, so the FX mixins never touch reflection. */
    public record ResolvedFx(double bonusPHDamage, double bonusEMDamage, double speed, float scale, Color glowColor,
                             Color baseColor, Color weaponColor, int fighterGFXIndex) {
    }

    private static final List<Integer> REGISTERED_AMMO_IDS = new ArrayList<>();
    private static final List<Integer> MARKET_AMMO_IDS = new ArrayList<>();
    private static final Map<Integer, AmmoDefinition> LOADED_AMMO = new HashMap<>();

    private static final Map<Integer, ResolvedFx> MISSILE_FX = new HashMap<>();
    private static final Map<Integer, ResolvedFx> RAIL_FX = new HashMap<>();
    private static final Map<Integer, ResolvedFx> FIGHTER_FX = new HashMap<>();

    private AmmoRegistrar() {
    }

    private static int registerAmmoID(int id) {
        REGISTERED_AMMO_IDS.add(id);
        SSFMLLogger.log("[WeaponFoundry] Added ammo ID to registry: " + id);
        return id;
    }

    /** Returns database ID for all registered ammo. */
    public static int[] getAmmoDatabaseIDs() {
        int[] ids = new int[REGISTERED_AMMO_IDS.size()];

        for (int i = 0; i < REGISTERED_AMMO_IDS.size(); i++) {
            ids[i] = toDatabaseID(REGISTERED_AMMO_IDS.get(i));
        }
        return ids;
    }

    /** Converts an ammo base ID into the game's consumable database/item ID. */
    public static int toDatabaseID(int ammoBaseId) {
        return ItemTypeConstantsInterface.CONSUMABLE * 10000 + ammoBaseId;
    }

    /** Returns database IDs for ammo that opted into market registration. */
    public static int[] getMarketAmmoDatabaseIDs() {
        int[] ids = new int[MARKET_AMMO_IDS.size()];

        for (int i = 0; i < MARKET_AMMO_IDS.size(); i++) {
            ids[i] = toDatabaseID(MARKET_AMMO_IDS.get(i));
        }
        return ids;
    }

    public static void registerAmmo(AmmoDefinition def) {
        Color color = resolveConstant(Color.class, def.color(), "color");
        TypeTag rarity = resolveConstant(TypeTag.class, def.rarity(), "rarity");

        ResolvedFx resolvedFx = resolveFx(def.fx(), def.kind());

        switch (def.kind()) {
            case MISSILE -> MISSILE_FX.put(def.id(), resolvedFx);
            case RAIL -> RAIL_FX.put(def.id(), resolvedFx);
            case FIGHTER -> FIGHTER_FX.put(def.id(), resolvedFx);
        }

        ConsumableList.write(
                registerAmmoID(def.id()),
                def.icon(),
                color,
                def.name(),
                def.description() + statLine(def.kind(), resolvedFx),
                def.tier(),
                def.volume(),
                def.creditValue(),
                0.0F,
                -1,
                rarity,
                false,
                false
        );

        if (def.market()) {
            MARKET_AMMO_IDS.add(def.id());
            SSFMLLogger.log("[WeaponFoundry] Registered ammo " + def.name() + " for market listings");
        }

        LOADED_AMMO.put(def.id(), def);

        SSFMLLogger.log("[WeaponFoundry] Registered ammo " + def.name() + " (id: " + def.id() + ")");
    }

    /** Mirrors the "Missile/Projectile/Fighter modifiers: +X PH damage, +Y EM damage[, +Z speed]." suffix vanilla appends. */
    private static String statLine(AmmoDefinition.Kind kind, ResolvedFx fx) {
        StringBuilder line = new StringBuilder();

        line.append(kind == AmmoDefinition.Kind.RAIL ? " Projectile modifiers: +" : kind == AmmoDefinition.Kind.FIGHTER
                ? " Fighter modifiers: +" : " Missile modifiers: +");
        line.append(fx.bonusPHDamage()).append(" PH damage, +").append(fx.bonusEMDamage()).append(" EM damage");

        if (kind != AmmoDefinition.Kind.RAIL) {
            line.append(", +").append(fx.speed()).append(" speed");
        }

        line.append(".");
        return line.toString();
    }

    private static ResolvedFx resolveFx(AmmoDefinition.Fx fx, AmmoDefinition.Kind kind) {
        Color glowColor = resolveConstant(Color.class, fx.glowColor(), "fx.glowColor");

        if (kind == AmmoDefinition.Kind.RAIL) {
            return new ResolvedFx(fx.bonusPHDamage(), fx.bonusEMDamage(), 0, 0, glowColor, null, null, 0);
        }

        Color baseColor = resolveConstant(Color.class, fx.baseColor(), "fx.baseColor");

        if (kind == AmmoDefinition.Kind.MISSILE) {
            return new ResolvedFx(
                    fx.bonusPHDamage(), fx.bonusEMDamage(), fx.speed(), fx.scale(), glowColor, baseColor, null, 0
            );
        }

        // FIGHTER
        Color weaponColor = resolveConstant(Color.class, fx.weaponColor(), "fx.weaponColor");

        return new ResolvedFx(
                fx.bonusPHDamage(), fx.bonusEMDamage(), fx.speed(), fx.scale(),
                glowColor, baseColor, weaponColor, fx.fighterGFXIndex()
        );
    }

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

    /** Looked up by MissileFXMixin. Returns null if the id isn't one of ours (vanilla ammo falls through as normal). */
    public static ResolvedFx getMissileFx(int ammoBaseId) {
        return MISSILE_FX.get(ammoBaseId);
    }

    /** Looked up by RailGunFXMixin. Returns null if the id isn't one of ours (vanilla ammo falls through as normal). */
    public static ResolvedFx getRailFx(int ammoBaseId) {
        return RAIL_FX.get(ammoBaseId);
    }

    /** Looked up by FighterFXMixin. Returns null if the id isn't one of ours (vanilla ammo falls through as normal). */
    public static ResolvedFx getFighterFx(int ammoBaseId) {
        return FIGHTER_FX.get(ammoBaseId);
    }

    /** Returns the number of registered ammo items. */
    public static int getRegisteredAmmoCount() {
        return REGISTERED_AMMO_IDS.size();
    }

    /** Returns every successfully registered ammo's full definition. */
    public static List<AmmoDefinition> getLoadedAmmo() {
        return List.copyOf(LOADED_AMMO.values());
    }
}