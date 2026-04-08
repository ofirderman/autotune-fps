# Changelog

## 0.1.3

- Added direct `Auto-apply` control to the preset menu
- Added `Aggressive mode` as an extra performance option with menu, command, and status support
- Added a first Sodium compatibility layer with safer Quality behavior and clear Sodium detection in the menu and status output
- Improved the preset menu layout and wording so it is easier to read
- Fixed older `1.21.x` versions where the top menu section could overlap the preset buttons
- Fixed newer `1.21.x` versions where the top menu section could disappear entirely
- Fixed utility menu buttons so `Auto-apply` and `Aggressive mode` toggle correctly without kicking you out of the menu
- Fixed cross-version graphics option access so restore/preset actions do not crash on newer `1.21.x` versions

## 0.1.2

- Started `0.1.2` development
- Added saved-setting restore points before preset applies
- `Restore` now brings back saved settings instead of acting like a pure no-op state
- `/optimizer status` now shows whether a restore is available
- The preset menu now shows restore availability and presents `Restore` as a dedicated action

## 0.1.1

- Updated metadata for the `1.21.x` patch line
- Improved `/optimizer help` with short command explanations
- Shortened `/optimizer status` to show only the most useful information
- Cleaned up preset menu text and removed duplicate recommendation lines
- Added current and recommended markers directly to preset buttons
- Show the detected recommended preset on the main `Recommended` button
- Disable already-selected preset actions in the preset menu
- Added recovery mode for interrupted preset applies
- Successful manual or recommended actions now clear recovery mode without silently turning auto-apply back on

## 0.1.0

- First public release of AutoTune FPS for Fabric `1.21.1` through `1.21.11`
- Added manual presets: `ultimate_performance`, `performance`, `balanced`, `quality`, and `off`
- Added a separate `recommend` action based on detected hardware tier
- Added an in-game optimizer menu for preset selection
- Added clearer `/optimizer help` and `/optimizer status` output
- Added hardware-tier-aware preset scaling for `LOW`, `MID`, and `HIGH`
- Added safe `Fabulous` handling so `quality` falls back to `Fancy` when needed
- Added compatibility-safe runtime fallbacks instead of crashing on unsafe paths
- Kept personal settings untouched:
  - Max FPS / Framerate Limit
  - VSync
  - FOV
  - GUI Scale
  - Brightness
  - Resolution / fullscreen mode
