# Changelog

## 0.1.0-beta.1

- First public beta release of AutoTune FPS for Fabric `1.21.1` through `1.21.11`
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
