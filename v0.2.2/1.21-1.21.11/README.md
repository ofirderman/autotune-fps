# AutoTune FPS

`AutoTune FPS` is a Fabric client mod for players who want Minecraft to run better without spending fifteen minutes in the video settings menu.

## Current Status

- current release line: `0.2.2`
- Supported Minecraft versions: `1.21` through `1.21.11` and `26.1` through `26.1.2`
- client-side Fabric mod
- preset-based optimization plus an optional client-side Optimization Engine

## Product Promise

This mod is not trying to replace Sodium or win a benchmark war.

The goal is simpler:

- give normal players a few smart presets
- keep the setup fast and understandable
- make optimization feel like a product, not homework

## Compatibility

AutoTune FPS is designed to work alongside common client optimization mods such as Sodium and similar mods.

- AutoTune FPS does not expect to be the only optimization mod installed
- prefer compatibility over forcing settings that may conflict with other mods
- avoid changing options that are likely to be owned or overridden by other optimization mods
- detect common compatibility situations where possible and use fallback behavior
- if another mod blocks a setting change, use a fallback instead of crashing

## Current Scope

The current release is preset-driven and focused on understandable optimization.

The Optimization Engine is separate from presets:

- `Off` keeps runtime optimization disabled.
- `Optimized` applies a conservative particle budget during excessive client-side particle spam.
- `Aggressive` applies a stronger particle budget and exposes Aggressive Optimization settings with per-module control.

Particle Reduction has clear hover tooltips, FPS Impact / Visual Impact labels, and a module kill switch if you prefer full vanilla particles.

Particle Reduction never changes server state, server logic, gameplay mechanics, hitboxes, entity behavior, interactions, or world data. If the runtime particle hook is unavailable, the module disables itself safely.

The v0.2.2 menu adds a scrollable glass-style UI, improved readability and spacing, sharper Minecraft 1.21 rendering, and fixes the custom-menu blur and duplicate selected-button text issues.

Manual presets and restore:

- `ultimate_performance`
- `performance`
- `balanced`
- `quality`
- `restore`

Every public preset is adaptive. AutoTune chooses sensible settings for your PC before applying them.
This means the same preset can apply lighter or stronger settings on different PCs.

`ultimate_performance` is adaptive only in a narrow FPS-first range. Stronger systems may get slightly more useful distance, but it still keeps a Maximum FPS focus with limited distance scaling.

Preset meanings:

- `ultimate_performance` = Maximum FPS focus with limited distance scaling.
- `performance` = FPS-focused settings with light adaptive scaling.
- `balanced` = Middle point between FPS and visual quality.
- `quality` = Higher visual quality while keeping performance reasonable.
- `restore` = Restore your original settings from before AutoTune FPS changed them.

`Recommended preset` is a separate action, not a fourth visual preset.
It only chooses which adaptive public preset to use, then applies that preset through the same adaptive resolver as the manual buttons and commands.
It recommends `Performance`, `Balanced`, or `Quality` based on your setup:

- lower-end systems -> `Performance`
- middle systems -> `Balanced`
- higher-end systems -> `Quality` only when shaders and risk flags are not active

The recommended preset never automatically chooses `ultimate_performance`.
Current settings are used for preview diffs, Custom settings detection, and the restore baseline, not as proof of performance.
Restore reuses the original pre-AutoTune settings snapshot instead of replacing it automatically.
Internal levels and bands are not shown in normal player-facing UI.

Each preset applies explicit values for:

- render distance
- simulation distance
- particles
- clouds
- graphics mode
- biome blend
- entity shadows
- chunk update priority
- entity distance scaling

AutoTune does not change:

- Max FPS / Framerate Limit
- VSync
- FOV
- GUI Scale
- Brightness
- Resolution / fullscreen mode

`quality` prefers stability and compatibility over forcing Fabulous.

## Commands

- `/optimizer`
- `/optimizer help`
- `/optimizer status`
- `/optimizer menu`
- `/optimizer recommend`
- `/optimizer preview recommended`
- `/optimizer preset ultimate_performance / performance / balanced / quality / restore`
- `/optimizer autoapply off`
- `/optimizer autoapply selected`
- `/optimizer autoapply recommended`
- `/optimizer autoapply smart` is kept as a legacy alias for `recommended`
- `/optimizer autoapply on` as a legacy alias for `selected`
- `/optimizer engine off / optimized / aggressive`
- `/optimizer module particle_reduction on / off`
- `/optimizer aggressive on / off` as a legacy alias for Aggressive/Off engine mode

## Config

On first launch the mod creates:

`config/autotune_fps.json`

It also uses:

`config/autotune_fps_state.json`

- `autotune_fps.json` stores the selected preset, auto-apply mode, Optimization Engine mode, and module kill switches
- `autotune_fps_state.json` stores the original pre-AutoTune restore baseline, the last recommendation target, and temporary recovery state for interrupted preset applies

Auto-apply modes:

- `Off` does nothing on world/server join
- `Selected preset` applies the saved manual preset on join
- `Recommended preset` computes and applies the current recommendation once on join
