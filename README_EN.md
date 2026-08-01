# Annihilation Blade · Terminus 2.7.2-1.20.1-forge

> A Forge 1.20.1 SlashBlade expansion mod based on SlashBlade / SlashBlade Resharped. Revolving around themes of "Terminus, World Rift, Collapse, Judgement, Blood Prison, and Cosmic Laws", this mod provides three named blades, three sets of Slash Arts (SA), a complete Special Effect (SE) chain, configurable low-risk parameters, and visual/hotkey controls designed for real-combat readability.

Author: QingYi_Li (青衣_璃)

## Overview

The current version includes:

- Main Blade: `annihilationblade:annihilation_blade`
- Blood Prison Blade: `annihilationblade:blood_prison`
- Ultimate Blade: `annihilationblade:infinity_stellaris`
- 3 Slash Arts (SA): `spatial_fracture`, `infernal_slaughter`, and `vacuum_decay_collapse`
- 15 Registered Special Effects (SE): Annihilation Blade uses 8 "Terminus" SEs, Blood Prison uses 3 "Blood Prison" SEs, and Infinity Stellaris uses 4 "Cosmic Law" SEs
- Named blade datapack definitions
- Forge common configuration file
- Language localization resources (Simplified Chinese, Traditional Chinese, English)
- JEI SlashBlade integration description resources
- Dankong blink-control hotkeys and action bar prompts
- Infinity Stellaris AI erasure hotkey, default `I`, disabled by default
- Config toggles for Annihilation Blade / Infinity Stellaris custom tooltip renderers, enabled by default
- Native SlashBlade friendly-fire / PVP logic unification

## Requirements

| Project | Version |
| --- | --- |
| Minecraft | `1.20.1` |
| Forge | `47.4.21` |
| Java | `17` |
| Prerequisite | SlashBlade / SlashBlade Resharped |

## Weapons

### `annihilationblade:annihilation_blade`

Main weapon: "Annihilation Blade · Terminus". The named blade definition is located at:

`src/main/resources/data/annihilationblade/slashblade/named_blades/annihilation_blade.json`

| Project | Content |
| --- | --- |
| Base Attack | `50.0` |
| Durability | `2000` |
| SA | `annihilationblade:spatial_fracture` |
| SE Count | `8` |
| Extra Passive | Absolute Aegis, Void Flight, Terminus Execution, Fullbright Vision |

When the Annihilation Blade is in the inventory, main hand, or off-hand, the client receives the Fullbright Vision effect. Combat hit-detections strictly follow native SlashBlade rules, ensuring no friendly-fire damage to players, pets, or non-hostile entities by default.

World Rift triggers 5 ticks after Annihilation Blade damage is applied, executing all valid targets around the impact point as a center and cascading to nearby targets. The maximum chain count and range (centered on the initial attacker) can be adjusted in the common config under `annihilation_blade.world_rift.chain_count` and `chain_range`.

### `annihilationblade:blood_prison`

Blood Prison blade: "Demon Blade · Blood Prison". The named blade definition is located at:

`src/main/resources/data/annihilationblade/slashblade/named_blades/blood_prison.json`

| Project | Content |
| --- | --- |
| Base Attack | `16.0` |
| Durability | `2400` |
| SA | `annihilationblade:infernal_slaughter` |
| SE | `blood_leech`, `spirit_shield`, `phantom_mark` |

Blood Prison is designed around low-health risk/reward dynamics, lifesteal, shields, area domains, and phantom burst events. Key logics such as damage, lifesteal ratios, shield triggers, and execution threshold values are hardcoded and not exposed to config files to prevent game-balance breakdown or server abuse.

### `annihilationblade:infinity_stellaris`

Infinity Stellaris is an ultimate weapon blade with a final-tier crafting gate and no balance-oriented scaling. The named blade definition is located at:

`src/main/resources/data/annihilationblade/slashblade/named_blades/infinity_stellaris.json`

| Project | Content |
| --- | --- |
| Base Attack | `1000000.0` |
| Durability | `2147483647` (Max Integer) |
| SA | `annihilationblade:vacuum_decay_collapse` |
| SE | `entropy_dissolution`, `curvature_rupture`, `gamma_thunderburst`, `cosmic_string_cut` |
| Extra Passive | Flight, Invulnerability, Instant Death Protection, Kill Fallback Safeguard, Void Fall Protection, Fullbright Vision |

Infinity Stellaris only grants active combat authority when held in the main hand or off-hand, though it provides essential survival fallback logic when just kept in the inventory. It comes pre-enchanted with level 10 sword, bow, and crossbow enchantments, excluding Fire Aspect and Flame by design. Its current recipe requires four Annihilation Cores, one Dragon Egg, two Beacons, one Nether Star, and a Blood Prison blade with `5000` kills, `25000` Proud Souls, and `50` refine as the center blade core. It features a flagship custom tooltip renderer on the client: a constricted outer black hole background, a rotating white magic circle, a dynamic white border, orbiting/self-spinning white hexagram symbols, overlapping star chart overlays, a cosmic spectral line title, authority chips, attribute logs, and enchantment circuit graphics. If an inventory UI, tooltip enhancement, or another client rendering mod has compatibility issues, the custom renderers for Annihilation Blade and Infinity Stellaris can be disabled separately under the common config `client_tooltips` group, falling back to vanilla item tooltips. Blood Prison currently has no separate custom tooltip renderer and continues to use normal tooltips.

Combat operations feature multiple ultimate mechanics: Entropy Dissolution triggers Heat Death reset based on stacks; all Infinity Stellaris damage trails 5-tick particle chains; Gamma Thunderburst drops custom colored lightning continuously for 3 ticks in a 128-block radius. Curvature Rupture AI erasure is now disabled by default and toggled with `I`; while enabled and holding Infinity Stellaris, it temporarily disables AI only for legal targets that pass SlashBlade friendly-fire/PVP rules, then restores AI after the blade is no longer held or the toggle is turned off. Cosmic String Cut runs as a native SE triggered via SlashBlade slash events instead of binding to sneak-right-click. These logics reside in the `QWQ.QingYi.annihilationblade.infinity_stellaris` package and do not reuse the Annihilation Blade's Terminus execution flow.

## SA (Slash Arts)

### `Spatial Fracture`

The main SA bound to the Annihilation Blade. Upon activation, it scans along the player's line of sight to find the rift center, generating a spatial rift ring, rift cobwebs, portal particles, lightning scatters, and a sword rain performance.

Logically, it targets the block looked at; if a valid target is in the path, it centers the rift focus on the entity. Stricken entities are executed one by one, utilizing native SlashBlade kill paths to preserve correct kill counts.

Configurable parameters include: max distance, rift radius, scan step, sample radius, lock radius, fallback search radius, max targets, visible particle limits, slash line count, and visual scale.

### `Infernal Slaughter`

The SA bound to the Blood Prison blade. It deploys a Blood Prison domain and syncs the screen overlay on the client. During the domain's duration, the player's attacks perform warp strikes on hostile entities inside, tracking damage dealt to calculate healing feedback upon expiration.

Configurable parameters include: domain duration, domain radius, boundary particle tick rate, player aura particle rate, pulse interval, and visual scale.

### `Absolute Annihilation Zone`

The SA bound to the Infinity Stellaris (internally registered as `annihilationblade:vacuum_decay_collapse` for compatibility). Casts a ray from the player's sight, deploying the zone at the targeted block or at a fallback distance in the looking direction.

The zone spans a horizontal `128×128` square (about `64` blocks high) and lasts for `100 ticks`. Every tick, it scans for valid targets, executing them instantly with drops and XP suppressed; blocks are not destroyed to preserve performance. Visually, it features square boundaries, corner pillars, center collapse particles, and 12 colored lightning strikes per tick to simulate a vacuum decay event.

## SE (Special Effects)

| Name | Type | Effect / Behavior |
| --- | --- | --- |
| `断空` / `Dankong` | Warp Slash | Successive blinks between targets for rapid executions, returning to the starting point. |
| `裂界` / `World Rift` | AoE Rift | Creates a rift at the victim's location, pulling in and executing nearby hostiles. |
| `归墟回响` / `Terminus Echo` | Forward Echoes | Fires multiple waves of echo slashes in the facing direction. |
| `虚无权域` / `Void Dominion` | Large Domain | Opens a wide spatial rift ahead, clearing out all targets. |
| `因果坍缩` / `Causality Collapse` | Chain Judgement | Connects and executes targets sequentially from the nearest, generating causal links. |
| `星寂裁决` / `Starless Judgement` | Linear Judgement | Spawns a projection corridor ahead, executing all intersected entities. |
| `幻影审判` / `Phantom Judgement` | Summoned Swords | Circles targets with phantom blades, raining them down for focused damage. |
| `归墟天诏` / `Abyssal Decree` | High Decree | Forms a halo crown above, dropping vertical judgements from the sky. |
| `嗜血` / `Blood Leech` | Blood Prison Passive | Synergizes with the Blood Prison blade to provide lifesteal. |
| `源流灵盾` / `Spirit Shield` | Blood Prison Passive | Deploys a shield and short buffs when health drops low. |
| `幻影印记` / `Phantom Mark` | Blood Prison Passive | Accumulates marks on targets to trigger phantom blade burst events. |
| `熵增蚀解` / `Entropy Dissolution` | Infinity Stellaris Passive | Deducts 10% max health per hit; at 10 stacks, triggers Heat Death execution and strips all protective fallbacks. |
| `曲率撕裂` / `Curvature Rupture` | Infinity Stellaris Control | Disabled by default and toggled with `I`; while held and enabled, freezes legal targets within 25 blocks using SlashBlade friendly-fire/PVP rules and restores AI after release. |
| `伽马霆爆` / `Gamma Thunderburst` | Infinity Stellaris Burst | Triggers on damage; spawns 12 random colored lightning bolts per tick within 128 blocks for 3 ticks. |
| `宇宙弦切` / `Cosmic String Cut` | Infinity Stellaris Slash SE | Triggers via native slash events; spawns a local 5×5×5 starline and executes valid targets within 128 blocks. |

## Dankong Control

`Dankong` is a high-speed blink-strike SE. To avoid visual disorientation during casual mob clearing, two layers of safety configurations are provided:

- Holding **Shift** prevents Dankong from initiating a new teleport sequence.
- Holding **Shift** mid-teleport aborts the sequence and returns the player to the starting position.
- Added a configurable keybind "Toggle Dankong Blink Mode" (default: `Left Ctrl`).
- The keybind only works when the player holds the Annihilation Blade.
- Pressing the hotkey displays an action bar message: "Toggle Dankong Blink Mode: ON / OFF".

The display string is localized in language files and is not hardcoded, allowing easy translation and customization.

## Configuration

Upon the first launch, Forge generates:

`config/annihilationblade-common.toml`

The configuration file only exposes low-risk parameters:

- **Range**: search ranges, domain radii, judgement corridor widths.
- **Intervals**: teleport intervals, echo wave delays, domain particle refresh rates.
- **Cooldowns**: trigger cooldowns for various SEs.
- **Amounts**: target limits, summoned sword counts, visible particle limits.
- **Visual Scale**: particle density, visual radius, or performance density.
- **Client compatibility toggles**: `client_tooltips.enable_annihilation_blade_renderer` and `client_tooltips.enable_infinity_stellaris_renderer`, enabled by default; disabling one makes that blade use vanilla tooltip rendering.

**Unexposed Logic (Hardcoded)**:

- Damage multipliers
- Terminus execution mechanics
- Blood Prison lifesteal and shield values
- Invulnerability, flight, and protective safeguards
- Native SlashBlade kill-counts path forwarding

Every option includes both Chinese and English comments, stating the recommended Min/Max values. Bound checks via `defineInRange` are active to prevent game freezes or logic anomalies caused by extreme custom values.

### Config Groups

The configuration sections are grouped as follows:

```toml
[client_tooltips]
[annihilation_blade.spatial_fracture]
[annihilation_blade.dankong]
[annihilation_blade.world_rift]
[annihilation_blade.terminus_echo]
[annihilation_blade.void_dominion]
[annihilation_blade.causality_collapse]
[annihilation_blade.starless_judgement]
[annihilation_blade.phantom_judgement]
[annihilation_blade.abyssal_decree]
[blood_prison.domain]
[blood_prison.phantom_burst]
[infinity_stellaris]
```

## Localization

Language files are located at:

`src/main/resources/assets/annihilationblade/lang/`

Currently supported:

- `zh_cn.json` (Simplified Chinese)
- `zh_tw.json` (Traditional Chinese)
- `zh_hk.json` (Hong Kong Traditional Chinese)
- `en_us.json` (English)

Dankong keybind names, action bar warnings, item names, SA/SE titles, tooltips, and JEI info are all fully localized.

## JEI SlashBlade Integration

Resource-level support for `jei_slashblade` has been added, documenting descriptions for the named blades, SAs, and SEs:

- `assets/annihilationblade/blade_desc/annihilation_blade.json`
- `assets/annihilationblade/blade_desc/blood_prison.json`
- `assets/annihilationblade/blade_desc/infinity_stellaris.json`
- SA description keys: `slashblade.slash_art.annihilationblade.*.desc`
- SE description keys: `se.annihilationblade.*.desc`

With JEI SlashBlade installed, you can read the introductions of all three named blades directly in JEI, alongside comprehensive effect entries for the SAs and SEs.

## Registry & Source Paths

SA / SE registration files:

- `src/main/java/QWQ/QingYi/annihilationblade/registry/ModSlashArts.java`
- `src/main/java/QWQ/QingYi/annihilationblade/registry/ModSpecialEffects.java`

Core implementation directories:

- `src/main/java/QWQ/QingYi/annihilationblade/annihilation_blade/`
- `src/main/java/QWQ/QingYi/annihilationblade/blood_prison/`
- `src/main/java/QWQ/QingYi/annihilationblade/infinity_stellaris/`
- `src/main/java/QWQ/QingYi/annihilationblade/common/`
- `src/main/java/QWQ/QingYi/annihilationblade/config/ModConfig.java`
- `src/main/java/QWQ/QingYi/annihilationblade/network/`

## Building

It is recommended to compile using Java 17:

```powershell
$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8'
./gradlew.bat --no-daemon clean build --console=plain
```

Build output will be generated at:

`build/libs/annihilationblade-2.7.2-1.20.1-forge.jar`

## Changelog

### 2.7.0-1.20.1-forge

**Features & Refactoring**
- **Added Ultimate Weapon SlashBlade "Infinity Stellaris" (`annihilationblade:infinity_stellaris`)**:
  - Ultimate Weapon Profile: Base attack damage increased to `1,000,000.0`, durability increased to `2,147,483,647` (uncraftable, un-levelable).
  - Passive Power: Grants flight, invulnerability, instant death protection, kill fallback safeguard, void fall protection, and fullbright vision when held or kept in inventory.
  - Enchantment Suite: Preserves level 10 sword, bow, and crossbow enchantments; removed Fire Aspect and Flame per weapon settings.
  - Cleaned up Maxwell's demon siphon and fake ultimate protocols; aligned language files, tooltips, and JEI entries with the ultimate weapon profiles.
- **Overhauled Exclusive Special Effects (SE)**:
  - `entropy_dissolution`: Hits accumulate stacks; 10 stacks trigger Heat Death reset execution and strip target of all death-prevention fallbacks.
  - `curvature_rupture`: Freezes targets in a 25-block radius; permanently locks AI, navigation, and movement (removed performance-intensive displacement drag pull logic).
  - `gamma_thunderburst`: Spawns 12 random colored lightning bolts per tick within 128 blocks for 3 ticks. Rewritten with custom bolt entities and a dedicated renderer for execution efficiency, removing the old mixin targeting vanilla lightning rendering.
  - `cosmic_string_cut`: Refactored to fire via native `DoSlashEvent` triggers, removing the sneak-right-click entry that blocked normal right-clicks and SA releases. Visually optimized to display only a local 5×5×5 starline instead of wide plane meshes.
- **Overhauled Exclusive Slash Arts (SA)**:
  - `vacuum_decay_collapse` (Reconstructed as "Absolute Annihilation Zone"): Deploys a horizontal `128×128` (about 64 blocks high) square zone at the looked-at position for 100 ticks. Instantly executes targets with drops and XP suppressed; blocks are no longer replaced with air to keep server frame rates high.

**Visuals & UI Aesthetics**
- **Designed Ultimate "Infinity Stellaris" Hover Tooltip Renderer**:
  - **Backdrop & Circles**:
    - Replaces default tooltip container with a custom black hole backdrop and star chart overlays.
    - Draws segmented glowing frames alongside orbiting and rotating white hexagram/diamond symbols.
    - Implemented a complex hand-drawn multi-layered magic circle (concentric circles, star nodes, scale lines) rendered in real-time with differential rotation speeds.
    - Constrained backdrop boundaries to prevent extreme widths in inventory or Creative/JEI menus.
  - **Flowing Titles & Layout Zones**:
    - The title line `Ultimate Weapon: Infinity Stellaris` uses real-time char-by-char RGB flowing spectral gradients with safe index sampling, eliminating rare client crashes on color index bounds.
    - Tabulated specifications: details are categorized into Core Power, Attribute Logs, Cosmic Chips, and Enchantment Circuits, with micro-animations applied to status bars.
    - Decoupled tooltip insertion logic into a dedicated client-side handler class, removing core weapon combat dependency on tooltip client APIs.

**Improvements & Bug Fixes**
- **Hotkeys & Combat Fixes**: Fixed Cosmic String Cut intercepting normal right-click charges, returning full control back to normal SlashArts.
- **Render Optimizations**: Removed the pre-packed high-resolution asset `infinity_stellaris_cosmic_backdrop.png` from GUI asset paths, rendering the magic circle entirely in real-time GUI code to save file size.
- **Localization & Assets Sync**: Synced Simplified Chinese, Traditional Chinese, Hong Kong Traditional Chinese, and English language resources. Aligned named blade JSON definitions, JEI entries, and README formats.

### 2.7.2-1.20.1-forge

**Bug Fixes**
- Fixed Blood Prison's inventory refresh replacing the whole named-blade NBT tag, which removed enchantments added later through an anvil. Blood Prison now preserves existing item enchantments while refreshing its base blade definition.
- Cleans stale `minecraft:multishot` from old Blood Prison stacks and migrates it to `minecraft:power`, keeping source, named-blade JSON, and runtime refresh behavior aligned.
- Limits Blood Prison named-blade NBT replacement to the one-time acquisition path. Later inventory, main-hand, and off-hand runtime refreshes only keep the blade identity intact, without copying the named-blade definition or rewriting enchantments.
- Prevents enchantments added later through anvils, enchanted books, or other item-editing paths from being overwritten by periodic refresh logic.
into the wrong `OptionInstance` tooltip field in some 1.20.1 Forge runtime environments.
- Preserved the gamma overwrite fullbright behavior while replacing loose field guessing with runtime validation of the real gamma value field and a safe fallback if reflection is unavailable.
- Fixed a client Tick crash where the Annihilation Blade / Infinity Stellaris fullbright vision path could reflectively write a `Double` 

**Tuning & Safety**
- Reduced Infinity Stellaris Curvature Rupture AI erasure: added a configurable keybind, default `I`, disabled by default, so nearby AI is no longer erased passively.
- Curvature Rupture targeting now uses SlashBlade's native PVP / friendly-fire gate, preventing default impact on players, pets, allies, and non-hostile entities.
- Mobs temporarily disabled by Curvature Rupture record their original NoAI state and are restored when the player toggles the effect off, stops holding Infinity Stellaris, logs out, or changes dimension.

**Features**
- Added a `slashblade:shaped_blade` recipe for the ultimate named blade `annihilationblade:infinity_stellaris`.
- The recipe uses a final-tier gate: four Annihilation Cores, one Dragon Egg, two Beacons, one Nether Star, and a Blood Prison blade with `5000` kills, `25000` Proud Souls, and `50` refine as the center blade core.
- Updated Simplified Chinese, Traditional Chinese, Hong Kong Traditional Chinese, English localization, and JEI descriptions from the old "uncraftable" wording to the new final ritual recipe.
- Fixed the `slashblade:shaped_blade` JSON format for Infinity Stellaris and Blood Prison: `result` now uses the vanilla `ShapedRecipe` `item` field, and single SlashBlade ingredients now follow the generated source recipe's `item` form.

**Compatibility Config**
- Added `client_tooltips.enable_annihilation_blade_renderer` and `client_tooltips.enable_infinity_stellaris_renderer`, both defaulting to `true`, to control the custom tooltip renderer for Annihilation Blade and Infinity Stellaris separately.
- When the matching option is disabled, that blade no longer cancels the vanilla tooltip render event, allowing inventory screens, Creative tabs, JEI, or tooltip enhancement mods to stay on the more compatible vanilla rendering path.
- Blood Prison currently has no separate custom tooltip renderer in source, so no no-op Blood Prison option was added.
