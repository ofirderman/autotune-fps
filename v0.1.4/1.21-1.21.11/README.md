# AutoTune FPS

`AutoTune FPS` is a Fabric client mod for players who want Minecraft to run better without spending fifteen minutes in the video settings menu.

## Current Status

- current release line: `0.1.4`
- tested on Minecraft `1.21` through `1.21.11`
- client-side Fabric mod
- preset-based optimization with in-game commands and menu

## Product Promise

This mod is not trying to replace Sodium or win a benchmark war.

The goal is simpler:

- give normal players a few smart presets
- keep the setup fast and understandable
- make optimization feel like a product, not homework

## Compatibility Direction

AutoTune FPS should work safely alongside common client optimization mods such as Sodium and similar mods.

- do not assume AutoTune FPS is the only optimization mod installed
- prefer compatibility over forcing settings that may conflict with other mods
- avoid changing options that are likely to be owned or overridden by other optimization mods
- detect common compatibility situations where possible and fail safely
- if a setting cannot be applied safely because of another mod, use a fallback instead of crashing

This is a product and roadmap direction first.
Future compatibility improvements should stay safety-focused and avoid assuming full control over the client's optimization stack.

## Current Scope

The current release is preset-driven and focused on safe, understandable optimization.

Manual presets and restore:

- `ultimate_performance`
- `performance`
- `balanced`
- `quality`
- `restore`

These presets are tier-scaled by detected hardware tier, except `ultimate_performance`, which stays nearly global:

- `LOW`
- `MID`
- `HIGH`

Preset meanings:

- `ultimate_performance` = absolute lowest practical visual settings for maximum FPS
- `performance` = FPS-focused, but less extreme
- `balanced` = recommended middle point for this hardware tier
- `quality` = highest safe visual quality for this hardware tier
- `restore` = brings back the settings AutoTune first saved for you

In practice, `ultimate_performance` is the most aggressive visual-reduction preset.
The tier differences matter most for `balanced` and `quality`, where stronger hardware can safely keep more visuals.

`Recommended` is a separate action, not a fourth visual preset.
It chooses the default for the detected PC:

- `LOW -> performance`
- `MID -> balanced`
- `HIGH -> balanced`

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

On `HIGH` tier, `quality` may use `Fabulous` only when the environment looks safe.
Otherwise it falls back to `Fancy`.

## Commands

- `/optimizer`
- `/optimizer help`
- `/optimizer status`
- `/optimizer menu`
- `/optimizer recommend`
- `/optimizer preset ultimate_performance / performance / balanced / quality / restore`
- `/optimizer autoapply on`
- `/optimizer autoapply off`

## Config

On first launch the mod creates:

`config/autotune_fps.json`

It also uses:

`config/autotune_fps_state.json`

- `autotune_fps.json` stores the selected preset and whether it should be re-applied automatically when you join a world
- `autotune_fps_state.json` stores temporary recovery state for interrupted preset applies

## Looking Ahead

AutoTune FPS is still being actively improved.
The next stage is focused on making the mod safer, more polished, and easier to use across more real-world client setups.

- improve preset stability and recovery behavior
- refine the in-game experience and preset explanations
- expand safe compatibility with common client optimization mods
- keep improving the overall product feel without turning setup into homework

## Near-Term Roadmap

- detect common mod combinations and avoid conflicting tweaks
- future versions should improve compatibility with common optimization mods such as Sodium and similar client-side performance mods
- add onboarding text that recommends a preset instead of expecting players to know what they need
- keep refining the preset screen without turning it into a wall of text
