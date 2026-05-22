# Changelog

## 0.2.0

- Added the full preset system: `Ultimate Performance`, `Performance`, `Balanced`, `Quality`, `Restore`, `Recommend`, `Auto-apply on join`, `Aggressive mode`, the optimizer menu, and the related `/optimizer` commands
- Added Smart Recommendation for `Performance`, `Balanced`, and `Quality` using detected hardware plus shader/compatibility signals
- Added auto-apply modes: `Off`, `Selected preset`, and `Smart recommendation`
- Fixed current mode ownership so the mod reports whether settings are controlled by a live preset, Smart target, Custom settings, or restore state
- Cleaned up ownership rules between presets, restore, manual changes, and auto-apply on join
- Made every public preset adaptive so manual presets, commands, previews, and Smart Recommendation use the same target resolver
- Kept `Ultimate Performance` adaptive only in a narrow FPS-first range
- Simplified Smart Recommendation menu/status/preview/apply text so internal levels and change counts stay out of normal UI
- Improved preset preview/status output so changed settings, Smart previews, and protected settings are easier to understand
- Kept protected-settings behavior in place, including the original pre-AutoTune restore baseline, clear ownership handoff, and protection for personal settings
- Smart Recommendation never automatically chooses `Ultimate Performance`

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
