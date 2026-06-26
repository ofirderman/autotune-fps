# Changelog

## 0.2.2

- Added the client-side Optimization Engine with `Off`, `Optimized`, and `Aggressive` modes
- Added a real Particle Reduction runtime module with conservative and aggressive per-tick budgets
- Added a Particle Reduction kill switch, saved engine configuration, and migration from the old aggressive toggle
- Added Aggressive Optimization settings with per-module control
- Added hover-only tooltips with realistic FPS Impact and Visual Impact labels
- Added a scrollable glass-style settings UI with improved readability and spacing
- Improved Minecraft 1.21 menu sharpness by removing the custom-menu blur/render ordering issue
- Fixed duplicate selected-button text on Minecraft 1.21
- Added compatibility detection and logging for Sodium, Iris, Entity Culling, ImmediatelyFast, and Lithium
- Added safe unsupported-runtime behavior so a missing particle hook disables the module instead of crashing
- Kept presets, recommendation, preview, apply, restore, protected settings, and auto-apply behavior independent from the engine
- Kept Optimization Engine behavior client-side only; it does not change server logic, world data, hitboxes, or gameplay mechanics
- Kept `/optimizer aggressive on/off` as a legacy alias for Aggressive/Off engine mode

## 0.2.1

- Supported Minecraft versions: `1.21` through `1.21.11` and `26.1` through `26.1.2`
- Made `/optimizer status` easier to scan
- Cleaned up preset menu wording and removed confusing recommendation labels
- Renamed the recommendation actions so it is clear they apply or preview the recommended preset
- Restored the current preset visual state in the preset menu, including the `(current)` marker
- Added clear feedback after pressing `Re-scan`
- Clarified restore availability wording without changing restore behavior
- Kept preset behavior, protected settings, and restore behavior unchanged

## 0.2.0

- Added the full preset system: `Ultimate Performance`, `Performance`, `Balanced`, `Quality`, `Restore`, `Recommend`, `Auto-apply on join`, `Aggressive mode`, the optimizer menu, and the related `/optimizer` commands
- Added recommended preset selection for `Performance`, `Balanced`, and `Quality` using detected hardware plus shader/compatibility signals
- Added auto-apply modes: `Off`, `Selected preset`, and `Recommended preset`
- Fixed current mode ownership so the mod reports whether settings are controlled by a live preset, recommendation target, Custom settings, or restore state
- Cleaned up ownership rules between presets, restore, manual changes, and auto-apply on join
- Made every public preset adaptive so manual presets, commands, previews, and recommendation actions use the same target resolver
- Kept `Ultimate Performance` adaptive only in a narrow FPS-first range
- Simplified recommendation menu/status/preview/apply text so internal levels and change counts stay out of normal UI
- Improved preset preview/status output so changed settings, recommendation previews, and protected settings are easier to understand
- Kept protected-settings behavior in place, including the original pre-AutoTune restore baseline, clear ownership handoff, and protection for personal settings
- The recommended preset never automatically chooses `Ultimate Performance`

## 0.1.3

- Added direct `Auto-apply` control to the preset menu
- Added `Aggressive mode` as an extra performance option with menu, command, and status support
- Added a first Sodium compatibility layer with compatibility-focused Quality behavior and clear Sodium detection in the menu and status output
- Improved the preset menu layout and wording so it is easier to read
- Fixed older `1.21-1.21.11` versions where the top menu section could overlap the preset buttons
- Fixed newer `1.21-1.21.11` versions where the top menu section could disappear entirely
- Fixed utility menu buttons so `Auto-apply` and `Aggressive mode` toggle correctly without kicking you out of the menu
- Fixed cross-version graphics option access so restore/preset actions do not crash on newer `1.21-1.21.11` versions

## 0.1.2

- Started `0.1.2` development
- Added saved-setting restore points before preset applies
- `Restore` now brings back saved settings instead of acting like a pure no-op state
- `/optimizer status` now shows whether a restore is available
- The preset menu now shows restore availability and presents `Restore` as a dedicated action

## 0.1.1

- Updated metadata for the `1.21-1.21.11` patch line
- Improved `/optimizer help` with short command explanations
- Shortened `/optimizer status` to show only the most useful information
- Cleaned up preset menu text and removed duplicate recommendation lines
- Added current and selected-state markers directly to preset buttons
- Show the preset flow more clearly from the main optimizer button
- Disable already-selected preset actions in the preset menu
- Added recovery mode for interrupted preset applies
- Successful manual or recommended actions now clear recovery mode without silently turning auto-apply back on

## 0.1.0

- First public release of AutoTune FPS for Fabric `1.21` through `1.21.11`
- Added manual presets: `ultimate_performance`, `performance`, `balanced`, `quality`, and `off`
- Added the first `recommend` action for the preset flow
- Added an in-game optimizer menu for preset selection
- Added clearer `/optimizer help` and `/optimizer status` output
- Added hardware-tier-aware preset scaling for `LOW`, `MID`, and `HIGH`
- Added guarded `Fabulous` handling so `quality` falls back to `Fancy` when needed
- Added compatibility-focused runtime fallbacks instead of crashing on blocked paths
- Kept personal settings untouched:
  - Max FPS / Framerate Limit
  - VSync
  - FOV
  - GUI Scale
  - Brightness
  - Resolution / fullscreen mode
