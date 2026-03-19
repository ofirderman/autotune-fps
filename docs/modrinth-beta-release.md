# Modrinth Beta Release Notes

## Suggested Version Metadata

- Name: `AutoTune FPS 0.1.0-beta.1`
- Version number: `0.1.0-beta.1`
- Release channel: `beta`
- Minecraft versions: `1.21.1` through `1.21.11`
- Loaders: `Fabric`
- Environment: `Client`

## Suggested Summary

`AutoTune FPS` gives Minecraft players a few smart optimization presets instead of making them dig through every video setting by hand.

## If This Is A New Modrinth Project

- Project type: `Mod`
- Title: `AutoTune FPS`
- Slug: `autotune-fps` if available
- Single-player side: `Required`
- Server side: `Unsupported`
- License: `MIT`

## Suggested Changelog

- First public beta release of AutoTune FPS
- Manual presets:
  - `ultimate_performance`
  - `performance`
  - `balanced`
  - `quality`
  - `off`
- Recommended preset selection based on detected hardware tier
- In-game preset menu via `/optimizer menu`
- Clear command help and status output
- `quality` uses `Fabulous` only when it is considered safe
- Does not modify Max FPS / Framerate Limit, VSync, FOV, GUI Scale, Brightness, or Resolution / fullscreen mode

## Files To Upload

After rebuilding with the beta version, the main upload target should be:

- `build/modrinth/autotune-fps-0.1.0-beta.1.jar`

## Upload Checklist

1. Build the project with your working local JDK 21 setup.
2. Confirm the output jar version is `0.1.0-beta.1`.
3. In Modrinth, create a new version for the project.
4. Set the version channel to `beta`.
5. Mark the intended `1.21.x` versions from `1.21.1` through `1.21.11`, and loader `Fabric`.
6. Upload the main jar as the primary file.
7. Paste the changelog above.

## Notes

Modrinth versions accept a `.jar` file and use a release channel per version.
Official Modrinth docs for version uploads:

- https://docs.modrinth.com/api/operations/createversion/
