# WeaponFoundry

A Fabric mod for Sector Space that loads weapon and ammo definitions from JSON files, so a weapon or ammo item
(and its market listing and crafting recipe) can be written as JSON data instead of Java/Mixin code.

Requires my fork of [SSFML](https://github.com/OfflcerSam/SectorSpaceFabricModLoader).

Be careful editing certain stats mid-playthrough, it could cause it to be technically a separate item and make the original null.

Latest game version support: 0.5.9.6

## Folder convention

Place JSON files under:

```
<gameDirectory>/weapons/<yourWeaponPackName>/*.json
```

Each subfolder of `weapons/` is treated as its own namespace (typically your mod's name).
Every `.json` file directly inside it is loaded as either one weapon or one ammo item, depending on its
top-level `"type"` field (`"weapon"` or `"ammo"`, defaults to `"weapon"` if omitted).

Weapon ids and ammo ids are tracked as two separate pools - a weapon and an ammo item are allowed to share
the same `id` number, since they end up in different database ranges either way (see [`id`](#id) below).
Within each pool, ids must be unique across **all** `weapons/` subfolders. If two files claim the same id in
the same pool, the second one loaded is skipped with a log message naming the folder that already claimed it.

There is no equivalent to ShipFoundry's automatic sprite copying - weapons and ammo use the vanilla item icon
spritesheet system (see [`icon`](#icon) below) instead of per-entity PNGs, so no extra texture step is needed.

## Weapons

### Current Schema

Every weapon needs a `"kind"`, which selects which stats section is required and which `WeaponList.write*()`
call actually registers it. Example turret weapon included in repo:

```json
{
  "type": "weapon",
  "id": 5000,
  "kind": "turret",
  "icon": 608,
  "color": "AZURE",
  "name": "Rift Railgun",
  "description": "A modified railgun firing compressed rail slugs.",
  "tier": 0,
  "rarity": "UNCOMMON",
  "market": true,

  "turretStats": {
    "weaponType": 5,
    "volume": 3.0,
    "creditValue": 0,
    "baseDamage": 8.5,
    "range": 350,
    "energyRatio": 2.0,
    "effectType": 0,
    "accuracy": 0.78,
    "reloadTime": 3.5,
    "bonusCoef": -1.0
  },

  "recipe": {
    "label": "T1:Rift Railgun",
    "blueprintId": 20107,
    "blueprintAmount": 1,
    "ingredients": [
      { "id": 10701, "amount": 6 },
      { "id": 10711, "amount": 8 },
      { "id": 10702, "amount": 4 }
    ]
  }
}
```

### Base fields

| Field         | Type    | Notes                                                                                   |
|---------------|---------|-----------------------------------------------------------------------------------------|
| `type`        | string  | `"weapon"`. Optional, this is the default if omitted.                                   |
| `id`          | int     | Unique weapon base ID. See [`id`](#id) below.                                           |
| `kind`        | string  | One of `turret`, `bay`, `salvager`, `pdu`, `tether` (case-insensitive). See below.      |
| `icon`        | int     | Spritesheet icon index, same convention as ShipFoundry's `icon` field.                  |
| `color`       | string  | Name of a `Color` constant (case-insensitive). Same list as ShipFoundry's README.       |
| `name`        | string  | Display name.                                                                           |
| `description` | string  | Display description. Optional, defaults to `""`.                                        |
| `tier`        | int     | Affects usable level and stat scaling, same `tier * 10` level formula as ships.         |
| `rarity`      | string  | Name of a `TypeTag` constant (case-insensitive).                                        |
| `market`      | boolean | Optional, defaults to `false`. If `true`, listed for buy/sell at station index 502/512. |

### `id`

Weapon ids share a pool separate from ammo ids (see [Folder convention](#folder-convention)), but weapon ids
still collide with vanilla's own weapon item ids if reused, since they're written to the same database range
(`items.ItemTypeConstantsInterface.WEAPON * 10000 + id`). Stick to a clearly out-of-range block (the samples
use `5000+`) to be safe. Do not change an id once a save has that weapon in it.

### `kind`

| Kind       | Registers via                | Notes                                                                                     |
|------------|------------------------------|-------------------------------------------------------------------------------------------|
| `turret`   | `WeaponList.write()`         | Normal weapon slot item (lasers, railguns, plasma, disruptors, missile launchers, etc).   |
| `bay`      | `WeaponList.writeBay()`      | Fighter bay / catapult tube. Same stats section as `turret`, just a different write call. |
| `salvager` | `WeaponList.writeSalvager()` | Automatic salvaging device.                                                               |
| `pdu`      | `WeaponList.writePDU()`      | Point Defense Unit, shoots down missiles/fighters automatically.                          |
| `tether`   | `WeaponList.writeTether()`   | Grappler/tether device.                                                                   |

### `turretStats` (required for `turret` and `bay`)

| Field         | Type  | Notes                                                                                                                                        |
|---------------|-------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `weaponType`  | int   | Selects the weapon "family" from `game.WeaponFX` - also picks the FX class used to fire it and any auto-appended crit text. See table below. |
| `volume`      | float | Base cargo volume, scaled per-tier internally same as vanilla.                                                                               |
| `creditValue` | long  | Base credit value, scaled per-tier internally same as vanilla.                                                                               |
| `baseDamage`  | float | Base DPS before tier/reloadTime scaling.                                                                                                     |
| `range`       | int   | Base range, scaled per-tier internally same as vanilla.                                                                                      |
| `energyRatio` | float | Energy usage as a ratio of computed damage.                                                                                                  |
| `effectType`  | int   | Graphics/behavior variant. Optional, defaults to `0`. Missile-type weapons use effect ids in the 800s, fighter bays 1000s.                   |
| `accuracy`    | float | Accuracy as a 0-1 percentage.                                                                                                                |
| `reloadTime`  | float | Reload time in seconds (before the internal `*30`/`*45`/`*60` tier multiplier applied inside `WeaponList.write()`).                          |
| `bonusCoef`   | float | Unknown exact usage, vanilla near-universally sets `-1.0`. Optional, defaults to `-1.0`.                                                     |

**`weaponType` table** (from `game.WeaponFX`):

| # | Type             | #  | Type          |
|---|------------------|----|---------------|
| 1 | EMP              | 7  | Shockwave     |
| 2 | Laser            | 8  | Mining Beam   |
| 3 | Disruptor        | 9  | Tether        |
| 4 | Plasma           | 10 | Healer        |
| 5 | Railgun          | 11 | Salvager      |
| 6 | Missile Launcher | 12 | Point Defense |

### `salvagerStats` (required for `salvager`)

| Field              | Type   | Notes                                     |
|--------------------|--------|-------------------------------------------|
| `unitVolume`       | double | Cargo volume.                             |
| `creditValue`      | long   | Credit value.                             |
| `range`            | float  | Salvaging range.                          |
| `maxSalvageTier`   | int    | Highest wreck tier this can salvage.      |
| `salvagerItemBins` | int    | Number of item slots it can pull at once. |
| `salvageChance`    | float  | Chance per tick as a 0-1 percentage.      |
| `energyUsage`      | float  | Energy usage.                             |

### `pduStats` (required for `pdu`)

| Field            | Type   | Notes               |
|------------------|--------|---------------------|
| `unitVolume`     | double | Cargo volume.       |
| `creditValue`    | long   | Credit value.       |
| `targetRange`    | float  | Intercept range.    |
| `targetPower`    | float  | Intercept damage.   |
| `targetAccuracy` | float  | Intercept accuracy. |
| `energyUsage`    | float  | Energy usage.       |

### `tetherStats` (required for `tether`)

| Field                | Type   | Notes                         |
|----------------------|--------|-------------------------------|
| `unitVolume`         | double | Cargo volume.                 |
| `creditValue`        | long   | Credit value.                 |
| `range`              | float  | Tether range.                 |
| `speedReduction`     | float  | Target speed reduction, 0-1.  |
| `targetShieldDrain`  | float  | Target shield drain per tick. |
| `targetEnergyDrain`  | float  | Target energy drain per tick. |
| `targetShieldResist` | float  | Target shield resist debuff.  |
| `targetArmorResist`  | float  | Target armor resist debuff.   |
| `energyUsage`        | float  | Energy usage.                 |

### `recipe` (optional)

Same shape and rules as ShipFoundry's `recipe` section - see that README for the full blueprint/material ID
tables. Product ID in the crafting table is `WeaponRegistrar.toDatabaseID(id)` (`items.ItemTypeConstantsInterface.WEAPON * 10000 + id`).

## Ammo

### Current Schema

Ammo also needs a `"kind"`, which selects which FX class its bonuses feed into. Example rail ammo with a
crafting recipe included in repo:

```json
{
  "type": "ammo",
  "id": 5100,
  "kind": "rail",
  "icon": 816,
  "color": "WHITE",
  "name": "Rift Rounds",
  "description": "Dense slugs machined for the Rift Railgun.",
  "tier": 0,
  "rarity": "UNCOMMON",
  "market": true,
  "volume": 0.03,
  "creditValue": 40,

  "fx": {
    "bonusPHDamage": 6.0,
    "bonusEMDamage": 4.0,
    "glowColor": "AZURE"
  },

  "recipe": {
    "label": "T1:Rift Rounds",
    "productAmount": 300,
    "blueprintId": 20107,
    "blueprintAmount": 30,
    "ingredients": [
      { "id": 10072, "amount": 30 },
      { "id": 10202, "amount": 20 },
      { "id": 10058, "amount": 20 }
    ]
  }
}
```

### Base fields

| Field         | Type    | Notes                                                                                   |
|---------------|---------|-----------------------------------------------------------------------------------------|
| `type`        | string  | `"ammo"`, required (otherwise it's loaded as a weapon).                                 |
| `id`          | int     | Unique ammo base ID. See [Ammo `id` ranges](#ammo-id-ranges) below.                     |
| `kind`        | string  | One of `missile`, `rail`, `fighter` (case-insensitive). Selects the FX class it feeds.  |
| `icon`        | int     | Spritesheet icon index.                                                                 |
| `color`       | string  | Name of a `Color` constant (case-insensitive).                                          |
| `name`        | string  | Display name.                                                                           |
| `description` | string  | Display description. A stat-summary line is appended to this automatically (see `fx`).  |
| `tier`        | int     | Affects usable level.                                                                   |
| `rarity`      | string  | Name of a `TypeTag` constant (case-insensitive).                                        |
| `market`      | boolean | Optional, defaults to `false`. If `true`, listed for buy/sell at station index 502/512. |
| `volume`      | double  | Cargo volume per unit.                                                                  |
| `creditValue` | long    | Credit value per unit.                                                                  |

### Ammo `id` ranges

Ammo ids are registered as consumables (`items.ItemTypeConstantsInterface.CONSUMABLE * 10000 + id` for the
database id), **and** they're also the literal key each FX class's `configureEFXandBonus(int)` switches on
at fire-time (see [How ammo bonuses actually work](#how-ammo-bonuses-actually-work) below). Vanilla currently
occupies roughly `801-999` (missiles/rounds) and `1001-1050ish` (fighters) - stay well clear of that (the
samples use `5100+`/`5200+`/`5300+`) to avoid colliding with a real vanilla case in that switch.

### `fx`

Not every field applies to every `kind` - unused fields for a given kind are simply omitted from JSON.

| Field             | Type   | `missile` | `rail` | `fighter` | Notes                                                       |
|-------------------|--------|:---------:|:------:|:---------:|-------------------------------------------------------------|
| `bonusPHDamage`   | double |    Yes    |  Yes   |    Yes    | Physical damage bonus.                                      |
| `bonusEMDamage`   | double |    Yes    |  Yes   |    Yes    | EM damage bonus.                                            |
| `speed`           | double |    Yes    |   -    |    Yes    | Projectile/fighter speed.                                   |
| `scale`           | float  |    Yes    |   -    |    Yes    | Visual scale.                                               |
| `glowColor`       | string |    Yes    |  Yes   |    Yes    | `Color` constant name. Maps to `RailGunFX.glow` for `rail`. |
| `baseColor`       | string |    Yes    |   -    |    Yes    | `Color` constant name.                                      |
| `weaponColor`     | string |     -     |   -    |    Yes    | `Color` constant name, the fighter's own gun color.         |
| `fighterGFXIndex` | int    |     -     |   -    |    Yes    | Which fighter sprite to render as.                          |

### How ammo bonuses actually work

Ammo items don't store their own damage/speed bonuses. At fire-time, `MissileFX`/`RailGunFX`/`FighterFX` each
call an instance method `configureEFXandBonus(int itemBaseId)` that's a plain `switch` on literal vanilla item
ids - the same pattern as `Unique_NPC_Drops` being keyed on literal vanilla ship ids. A custom ammo id isn't
one of those cases, so without intervention it would silently fall through with stale/zero bonuses.

`MissileFXMixin`, `RailGunFXMixin`, and `FighterFXMixin` each `@Inject` at the `HEAD` of that method and
`cancel()` it, substituting the resolved `fx` values from this mod's `AmmoRegistrar` whenever the id matches
one this mod registered. Vanilla ammo ids fall through untouched.

### `recipe` (optional)

Unlike weapons/ships, ammo crafts in stacks (vanilla usually does 100-600 units per craft), so this uses
`CraftingTable#addRecipeStackOutput` instead of `addRecipe`.

| Field             | Type   | Notes                                                                             |
|-------------------|--------|-----------------------------------------------------------------------------------|
| `label`           | string | Recipe label shown in the crafting UI.                                            |
| `productAmount`   | int    | How many units of ammo this recipe produces per craft. Optional, defaults to `1`. |
| `blueprintId`     | int    | Item ID of the blueprint consumed.                                                |
| `blueprintAmount` | int    | Optional, defaults to `1`.                                                        |
| `ingredients`     | array  | Must have **exactly 3** entries, each `{ "id": #, "amount": # }`.                 |

Product ID in the crafting table is `AmmoRegistrar.toDatabaseID(id)` (`items.ItemTypeConstantsInterface.CONSUMABLE * 10000 + id`).

## Known limitations

- No name-based lookup for recipe ingredient/blueprint IDs yet, same limitation as ShipFoundry - use raw
  database item ids. See ShipFoundry's README for the blueprint/material ID tables (they're shared across
  ships, weapons, and ammo recipes).
- No custom PNG icon sheet is wired up yet - `WeaponFoundryIcons`/`ItemIconMixin` (ported from WeaponTest)
  support one, but `src/main/resources/items/custom_item_icons.png` needs to actually be supplied; without
  it, `icon` values below `WeaponFoundryIcons.CUSTOM_ICON_BASE` (1024) just use vanilla's existing spritesheet
  icons, which is what every sample JSON in this repo does.
- Weapon `effectType` values beyond the vanilla ones already in use (0 for plain hitscan, 800s for missiles,
  1001 for fighters) haven't been individually verified - cross-check against `WeaponFX`/`ChainGun` in a
  decompile if a custom `effectType` doesn't render the way you expect.

## Setup

`weapons/WeaponSample/` recreates 2 weapons and 2 ammo items as JSON, exercising every `kind`:

- `rift_railgun.json` - a `turret` weapon with a market listing and a crafting recipe.
- `twin_catapult_tube.json` - a `bay` weapon with a market listing, no recipe.
- `rift_rounds.json` - `rail` ammo with a market listing and a stack-output crafting recipe.
- `rift_missiles.json` - `missile` ammo with a market listing, no recipe.
- `rift_fighters.json` - `fighter` ammo with a market listing, no recipe.

(`salvager`/`pdu`/`tether` kinds aren't included as samples yet - their stats sections above are complete
and taken directly from `WeaponList`'s decompiled signatures, but untested in-game.)

Copy the `WeaponSample` folder to `<gameDirectory>/weapons/` to try it. If the `weapons` folder doesn't exist, create it.

Use `WTEST` on character save name to get all weapons and 100x of all ammo on load. Otherwise, use the folder
name for that folder's items on load.
