# AutoTune FPS

`AutoTune FPS` is a Fabric client mod for players who want Minecraft to run better without spending fifteen minutes in the video settings menu.

## Product Promise

This mod is not trying to replace Sodium or win a benchmark war.

The goal is simpler:

- give normal players a few smart presets
- keep the setup fast and understandable
- make optimization feel like a product, not homework

## Current v0.1 Direction

The current first version is preset-driven.

Manual presets:

- `ultimate_performance`
- `performance`
- `balanced`
- `quality`
- `off`

These presets are not global.
They scale by detected hardware tier:

- `LOW`
- `MID`
- `HIGH`

Preset meanings:

- `ultimate_performance` = absolute lowest practical visual settings for maximum FPS
- `performance` = FPS-focused, but less extreme
- `balanced` = recommended middle point for this hardware tier
- `quality` = highest safe visual quality for this hardware tier

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
- `/optimizer preset ultimate_performance / performance / balanced / quality / off`
- `/optimizer preset performance`
- `/optimizer preset balanced`
- `/optimizer preset quality`
- `/optimizer preset off`
- `/optimizer autoapply on`
- `/optimizer autoapply off`

## Config

On first launch the mod creates:

`config/autotune_fps.json`

The config stores the selected preset and whether it should be re-applied automatically when you join a world.

## Build Note

The project should use a normal Gradle Wrapper setup only.

If your local Gradle install is broken, generate `gradlew`, `gradlew.bat`, and `gradle/wrapper/*` from a healthy environment once and commit only those standard wrapper files.

## Near-Term Roadmap

- expand the preset screen with better explanations
- add a recovery / safe-mode flow after bad launches
- detect common mod combinations and avoid conflicting tweaks
- add onboarding text that recommends a preset instead of expecting players to know what they need
