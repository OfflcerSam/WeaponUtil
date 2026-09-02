# WeaponFoundry

A Fabric mod for Sector Space that loads weapon and ammo definitions from JSON files, so a weapon or ammo item
(and its market listing and crafting recipe) can be written as JSON data instead of Java/Mixin code.

Requires my fork of [SSFML](https://github.com/OfflcerSam/SectorSpaceFabricModLoader).

Be careful editing certain stats mid-playthrough, it could cause it to be technically a separate item and make the original null.

Latest game version support: 0.6.0.0

### Changelog Notes (Unreleased currently, will add changelog file later)

- `market` changed from a plain boolean to an object, so buy/sell can be controlled independently -
  see [`market`](#market-optional).
- `lootTable` entries no longer support `rare` - there's no separate rare pool anymore. See
  [`lootTable`](#loottable-optional).
- `lootTable` now works for **ammo** too (missile/rail/fighter all have real drop pools in this game
  version) - previously it only did anything for weapons.
- `lootTable` no longer works for **PDU** weapons - there's no gear pool for them in this game version
  (previously PDUs were just as supported as any other weapon `kind`).
- Added a config file (`<gameDirectory>/config/weaponfoundry/weaponfoundry.cfg`), backed by SSFML's new config
  API - see [Config](#config). The debug-item-grant toggle and debug character name are no longer hardcoded
  constants in `DebugItemGrant`; they're config options now (`debugItemGrantEnabled` /
  `debugItemGrantCharacterName`), same defaults as before so existing behavior doesn't change unless you edit the file.

## Folder convention

Place JSON files under:

```
<gameDirectory>/weapons/<yourWeaponPackName>/*.json
```

Each subfolder of `weapons/` is treated as its own namespace (typically your pack's name).
Every `.json` file directly inside it is loaded as either one weapon or one ammo item,
depending on its top-level `"type"` field (`"weapon"` or `"ammo"`, defaults to `"weapon"` if omitted).

Weapon ids and ammo ids are tracked as two separate pools, a weapon and an ammo item are allowed to share
the same `id` number, since they end up in different database ranges either way (see [`id`](#id) below).
Within each pool, ids must be unique across **all** `weapons/` subfolders. If two files claim the same id in
the same pool, the second one loaded is skipped with a log message naming the folder that already claimed it.

Icons can use vanilla Icon Index or sprite png.

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
  "market": {},

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
    "plusId": 5006,
    "ingredients": [
      { "id": 10701, "amount": 6 },
      { "id": 10711, "amount": 8 },
      { "id": 10702, "amount": 4 }
    ]
  },

  "lootTable": [
    { "tier": 0, "weight": 15 },
    { "tier": 1, "weight": 12 },
    { "tier": 1, "weight": 4 }
  ]
}
```

### Base fields

| Field         | Type          | Notes                                                                                                                     |
|---------------|---------------|---------------------------------------------------------------------------------------------------------------------------|
| `type`        | string        | `"weapon"`. Optional, this is the default if omitted.                                                                     |
| `id`          | int           | Unique weapon base ID. See [`id`](#id) below.                                                                             |
| `kind`        | string        | One of `turret`, `bay`, `salvager`, `pdu`, `tether` (case-insensitive). See below.                                        |
| `icon`        | int or string | Vanilla spritesheet index, **or** a path to a custom PNG next to this JSON. See [`icon`](#icon-weapon) below.             |
| `color`       | string        | Name of a `Color` constant (case-insensitive). Same list as ShipFoundry's README.                                         |
| `name`        | string        | Display name.                                                                                                             |
| `description` | string        | Display description. Optional, defaults to `""`.                                                                          |
| `tier`        | int           | Affects usable level and stat scaling, same `tier * 10` level formula as ships.                                           |
| `rarity`      | string        | Name of a `TypeTag` constant (case-insensitive).                                                                          |
| `market`      | object        | Optional. If present, listed at station index 502/512 (Military Station markets). See [`market`](#market-optional) below. |

### `id`

Weapon ids share a pool separate from ammo ids (see [Folder convention](#folder-convention)), but weapon ids
still collide with vanilla's own weapon item ids if reused, since they're written to the same database range
(`items.ItemTypeConstantsInterface.WEAPON * 10000 + id`). Stick to a clearly out-of-range block (the samples
use `5000+`) to be safe. Do not change an id once a save has that weapon in it.

### `icon` (weapon)

Either:
- **A number** - a plain vanilla spritesheet index, same convention as ShipFoundry's `icon` field
  (`items/items.png`, a 32x32 grid, `column = iconNumber % 32`, `row = iconNumber / 32`).
- **A string** - a path to a PNG, resolved relative to the folder this weapon's JSON file is in. A bare
  filename (`"rift_railgun.png"`) looks for the image right next to the JSON; a relative path with
  subfolders (`"icons/rift_railgun.png"`) also works. The image is loaded the first time this exact path
  is referenced by any JSON in `weapons/`, and reused (not reloaded) if another weapon or ammo item in the
  same or a different pack references that same path again.

There's no fixed pixel size requirement for a custom image - unlike vanilla's spritesheet cells, which are a
fixed 32x32, a custom icon is drawn as its own full image, scaled to fit wherever the game would normally
draw a 32x32 icon. A non-square image will look stretched in-game, so square source art is still recommended,
but any resolution works (see [How custom icons are drawn](#how-custom-icons-are-drawn) below for why).
A missing or corrupt file logs an error naming the exact path it tried and skips loading that JSON, the same
as any other invalid field.

### `market` (optional)

Shared by both weapons and ammo. Omitting this field entirely means the item isn't listed in any market at
all - the same as the old `"market": false`.

```json
"market": {
  "produce": true,
  "consume": true
}
```

| Field     | Type    | Notes                                                                                      |
|-----------|---------|--------------------------------------------------------------------------------------------|
| `produce` | boolean | Optional, defaults to `true`. Whether it's always buyable from the market (a "sell" line). |
| `consume` | boolean | Optional, defaults to `true`. Whether the market always buys it back (a "buy" line).       |

Game update: `MarketItem.BUY_AND_SELL_ALWAYS` (a single constant covering both directions on one item)
was removed. Vanilla now achieves "buy and sell" by using two *different* item ids with two different
constants (`PRODUCES_ALWAYS` / `CONSUMES_ALWAYS`), never the same id twice - see `MarketList.writeMilitaryStation()`.
Since our custom weapons/ammo only have one item id apiece, `produce`/`consume` independently control
whether `MarketRegistrar` registers a `PRODUCES_ALWAYS` entry, a `CONSUMES_ALWAYS` entry, or both for
that id. `"market": {}` (both flags at their `true` default) reproduces the old always-both behavior.
Setting both to `false` is the same as omitting `market` entirely since there'd be nothing left to list.

The market-category tag shown on the item (Military/Industrial/etc.) is read from whichever `Market`
object it actually lands in, the same way vanilla tags its own items - it isn't something this mod's
JSON controls directly.

### `kind`

| Kind       | Registers via                | Notes                                                                                     |
|------------|------------------------------|-------------------------------------------------------------------------------------------|
| `turret`   | `WeaponList.write()`         | Normal weapon slot item (lasers, railguns, plasma, disruptors, missile launchers, etc).   |
| `bay`      | `WeaponList.writeBay()`      | Fighter bay / catapult tube. Same stats section as `turret`, just a different write call. |
| `salvager` | `WeaponList.writeSalvager()` | Automatic salvaging device.                                                               |
| `pdu`      | `WeaponList.writePDU()`      | Point Defense Unit, while you can do it, it might not fully work.                         |
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

Same shape and rules as ShipFoundry's `recipe` section - see that README for the full blueprint/material ID tables.
Product ID in the crafting table is `WeaponRegistrar.toDatabaseID(id)` (`items.ItemTypeConstantsInterface.WEAPON * 10000 + id`).

### `lootTable` (optional)

Game update: `_database.DropTable` (and its separate common/rare pools per tier) was removed entirely.
Weapon and ammo `lootTable` entries are now patched directly into the game's own `gear_tables.sc` /
`loot_tables.sc` data files at boot by `LootTablePatcher`, which uses the game's current format: one
pool per tier, with a per-row probability instead of a separate rare pool. `LootTablePatcher` marks
every row it inserts and strips its own previous rows before re-inserting fresh ones each boot, so this
is safe to leave as a persistent hand-edited file (see the note in `Setup` about `resources/data/`).

```json
"lootTable": [
  { "tier": 0, "weight": 15 },
  { "tier": 1, "weight": 12 }
]
```

| Field    | Type | Notes                                                                                                      |
|----------|------|------------------------------------------------------------------------------------------------------------|
| `tier`   | int  | Which tier bucket (0-6) this entry targets. Which pool that tier belongs to depends on `kind` - see below. |
| `weight` | int  | Optional, defaults to `1`. Currently treated as a percent chance (1-100) - see below.                      |

**`rare` has been removed** - there's no separate rare pool to add to anymore, just one pool per tier
with a probability per row. To get a "shows up, but not often" entry the way `rare: true` used to, add a
second entry at the same tier with a lower `weight` instead - see `rift_railgun.json`'s two tier-1 rows.

**weight -> probability**: this is a reinterpretation, not a mechanical port of the old behavior. The old
`weight` counted how many times a weapon was duplicated into a flat list, so a higher number *relative to
other entries in the same pool* meant more common. The new table format wants an explicit probability per
row instead, so `weight` is currently divided by 100 and clamped to `1.0` (`weight: 15` -> a 15% chance to
be picked whenever that table rolls). Treat the sample weights as a starting point, not a precise
migration of old drop rates - what "feels right" will likely need retuning by playtesting.

**Which pool a `lootTable` entry lands in**, by `kind`:

| Weapon `kind`    | Pool                                                                                                                           |
|------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `turret` / `bay` | Weapon gear pool                                                                                                               |
| `salvager`       | Salvager gear pool                                                                                                             |
| `tether`         | Tether gear pool                                                                                                               |
| `pdu`            | **Not supported** - there's no gear pool for PDUs in this game version. A `lootTable` on a `pdu` weapon is logged and ignored. |


## Ammo

### Current Schema

Ammo also needs a `"kind"`, which selects which FX class its bonuses feed into.
Example rail ammo with a crafting recipe included in repo:

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
  "market": {},
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
  },

  "lootTable": [
    { "tier": 0, "weight": 18 },
    { "tier": 1, "weight": 10 }
  ]
}
```

### Base fields

| Field         | Type          | Notes                                                                                                               |
|---------------|---------------|---------------------------------------------------------------------------------------------------------------------|
| `type`        | string        | `"ammo"`, required (otherwise it's loaded as a weapon).                                                             |
| `id`          | int           | Unique ammo base ID. See [Ammo `id` ranges](#ammo-id-ranges) below.                                                 |
| `kind`        | string        | One of `missile`, `rail`, `fighter` (case-insensitive). Selects the FX class it feeds.                              |
| `icon`        | int or string | Vanilla spritesheet index, **or** a path to a custom PNG next to this JSON. See [`icon`](#icon-ammo) below.         |
| `color`       | string        | Name of a `Color` constant (case-insensitive).                                                                      |
| `name`        | string        | Display name.                                                                                                       |
| `description` | string        | Display description. A stat-summary line is appended to this automatically (see `fx`).                              |
| `tier`        | int           | Affects usable level.                                                                                               |
| `rarity`      | string        | Name of a `TypeTag` constant (case-insensitive).                                                                    |
| `market`      | object        | Optional. If present, listed at station index 502/512 (Military Station markets). See [`market`](#market-optional). |
| `volume`      | double        | Cargo volume per unit.                                                                                              |
| `creditValue` | long          | Credit value per unit.                                                                                              |
| `lootTable`   | array         | Optional. Same shape as [weapons' `lootTable`](#loottable-optional) - see that section for the pool table.          |

### Ammo `id` ranges

Ammo ids are registered as consumables (`items.ItemTypeConstantsInterface.CONSUMABLE * 10000 + id` for the
database id), **and** they're also the literal key each FX class's `configureEFXandBonus(int)` switches on
at fire-time (see [How ammo bonuses actually work](#how-ammo-bonuses-actually-work) below).
Vanilla currently occupies roughly `801-999` (missiles/rounds) and `1001-1050ish` (fighters).

### `icon` (ammo)

Same rules as [weapon `icon`](#icon-weapon) - a number for a vanilla spritesheet index, or a string path to a
PNG resolved relative to this ammo item's own JSON file.

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

Ammo items don't store their own damage/speed bonuses. At fire-time, `MissileFX` and `FighterFX` each call
an instance method (`configureEFXandBonus(int)` on `MissileFX`, `configureFighter(int)` on `FighterFX` as of
this game update) that's a plain `switch` on literal vanilla item ids - the same pattern as
`Unique_NPC_Drops` being keyed on literal vanilla ship ids. `RailGunFX` does the same thing but as a
`static` method (`configureEFXandBonus(int, SpaceShip)`) writing `static` fields, also as of this game
update. A custom ammo id isn't one of those cases, so without intervention it would silently fall through
with stale/zero bonuses.

`MissileFXMixin`, `RailGunFXMixin`, and `FighterFXMixin` each `@Inject` at the `HEAD` of that method and
`cancel()` it, substituting the resolved `fx` values from this mod's `AmmoRegistrar` whenever the id matches
one this mod registered. Vanilla ammo ids fall through untouched.

### `lootTable` pools (ammo)

Which pool an ammo `lootTable` entry lands in, by `kind`:

| Ammo `kind` | Pool                   |
|-------------|------------------------|
| `missile`   | Missile ammo drop pool |
| `rail`      | Rifle round drop pool  |
| `fighter`   | Fighter ammo drop pool |

See [weapons' `lootTable`](#loottable-optional) for the `tier`/`weight` fields themselves - the shape is
identical, only which pool it lands in differs.

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

## How custom icons are drawn

Vanilla item icons are always drawn from one shared spritesheet (`items/items.png`) at a fixed 32x32 cell size.
Referenced purely by numeric index, there's no file-path lookup route for items in vanilla the way
`DeferedTextureLoader` resolves ship sprites by filename convention.

So a string `icon` doesn't plug into that spritesheet at all. Instead, `WeaponFoundryIcons` loads the given
PNG as its own standalone `Texture` the first time it's referenced, and hands back a made-up integer (starting
at `WeaponFoundryIcons.CUSTOM_ICON_BASE`, currently `1024`, comfortably past vanilla's real range) purely to
act as a lookup key - it is never an index into any spritesheet. `ItemIconMixin` `@Redirect`s every place
`Item` draws its icon (`drawIcon`, `drawIconAsMount`, `drawQuickMenuIcon`) so that when an item's icon number
resolves to one of these custom textures, it draws that whole image (scaled into whatever destination size
the game intended for a normal icon) instead of sampling a spritesheet cell.

Two images referencing the exact same resolved path (e.g. a weapon and its ammo sharing an icon, or two
different mod folders using `"icon": "../SharedPack/laser.png"`) are only loaded onto the GPU once and share
the same custom icon number.

Note: If another mod uses this same method, it could cause lookup collisions until the texture loading for icons is changed.

## Known limitations

- No name-based lookup for recipe ingredient/blueprint IDs.
  See ShipFoundry's README for the blueprint/material ID tables (they're shared across ships, weapons, and ammo recipes).
- Weapon `effectType` values beyond the vanilla ones already in use (0 for plain hitscan, 800s for missiles, 1001 for fighters) haven't been individually verified.
- `lootTable` doesn't do anything for `pdu` weapons - there's no gear pool for them in this game version. (PDUs still might be unfinished technically)
- The `weight` -> probability mapping on `lootTable` entries (see that section) is a placeholder
  interpretation, not a verified match to any particular in-game drop rate feel.

## Config

WeaponFoundry's settings live at `<gameDirectory>/config/weaponfoundry/weaponfoundry.cfg`, generated on first load via SSFML's config API.
Comment lines starting with `#` are safe to read but not meant to be edited.

| Key                           | Default | Notes                                                                                                                                                                                |
|-------------------------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabledPacks`                | (blank) | Comma-separated `weapons/` subfolder names to load. Blank means load every pack found, same as before this option existed.                                                           |
| `debugLogging`                | `false` | When `true`, logs one line per weapon/ammo/market listing registered, instead of just per-pack and per-boot summaries.                                                               |
| `debugItemGrantEnabled`       | `true`  | Whether loading a character whose name matches `debugItemGrantCharacterName` or a pack folder name grants weapons/ammo to their cargo hold at all. See [Setup](#setup) below.        |
| `debugItemGrantCharacterName` | `WTEST` | Character save name that grants every registered weapon and 100x every registered ammo (case-insensitive). Named `weapons/<name>/` folders keep working independently of this value. |

## Setup

`weapons/WeaponSample/` recreates weapons and ammo items as JSON, exercising every `kind`, and now also
every `market` produce/consume combination:

- `rift_railgun.json` - a `turret` weapon with a default (`{}`, both directions) market listing, a
  crafting recipe with a `plusId` upgrade product, and a `lootTable` showing the old-`rare`-replacement
  pattern (two entries at the same tier, the second at a lower `weight`).
- `rift_railgun_plus.json` - the `+` product of `rift_railgun.json`'s recipe. Omits `market` entirely
  (not listed anywhere - a craft-only upgrade), and uses a single low-`weight` `lootTable` entry.
- `twin_catapult_tube.json` - a `bay` weapon with `"market": { "produce": true, "consume": false }` -
  always buyable, but the market won't buy it back.
- `voidburst_cannon.json` - a `turret` weapon using a **custom icon** (`voidburst_icon.png`, included in
  the same folder) instead of a vanilla spritesheet index, and `"market": { "produce": false, "consume": true }` -
  a loot-only rare weapon you can sell but never buy.
- `scrap_reclaimer.json` - a `salvager` weapon with a default market listing, no recipe.
- `aegis_pdu.json` - a `pdu` weapon with a default market listing, no recipe, and **no `lootTable`** -
  PDUs don't have a gear pool in this game version, see [`lootTable`](#loottable-optional).
- `gravity_tether.json` - a `tether` weapon with a default market listing, no recipe.
- `rift_rounds.json` - `rail` ammo with a default market listing and a stack-output crafting recipe.
- `rift_missiles.json` - `missile` ammo with a default market listing, no recipe, and a `lootTable`
  entry (ammo drop pools are new in this game version - previously ammo couldn't use `lootTable` at all).
- `rift_fighters.json` - `fighter` ammo with a default market listing, no recipe, and a `lootTable` entry.

Copy the `WeaponSample` folder to `<gameDirectory>/weapons/` to try it. If the `weapons` folder doesn't exist, create it.

Note on `lootTable`: entries get patched into `<gameDirectory>/resources/data/gear_tables.sc` and `loot_tables.sc` on boot (marked and kept idempotent - see `LootTablePatcher`).
Those files are meant to be  hand-editable and persist across launches, so it's safe to tweak drop rates there directly too.
Just be  aware a fresh `lootTable` in JSON will still patch its own rows back in on the next boot regardless.

Use `WTEST` on character save name to get all weapons and 100x of all ammo on load (configurable via
`debugItemGrantCharacterName`). Otherwise, use the folder name for that folder's items on load.