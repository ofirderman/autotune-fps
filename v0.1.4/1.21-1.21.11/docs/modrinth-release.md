# Modrinth 0.1.4 Release Notes

## Suggested Version Metadata

- Name: `AutoTune FPS 0.1.4`
- Version number: `0.1.4`
- Release channel: `release`
- Minecraft versions: `1.21` through `1.21.11`
- Loaders: `Fabric`
- Environment: `Client`

## Suggested Summary

`AutoTune FPS` keeps its preset-based workflow simple, while making the optimizer menu more useful and more stable across the full `1.21-1.21.11` range.

## If This Is A New Modrinth Project

- Project type: `Mod`
- Title: `AutoTune FPS`
- Slug: `autotune-fps` if available
- Single-player side: `Required`
- Server side: `Unsupported`
- License: `MIT`

## Suggested Changelog

- Added `/optimizer presets` to explain each preset in-game
- Added `/optimizer preview recommended` so players can preview the recommended preset before applying it
- Added Minecraft `1.21` support alongside `1.21.1` through `1.21.11`
- Added direct `Auto-apply` control to the preset menu
- Added `Aggressive mode` as an extra performance option with menu, command, and status support
- Added a first Sodium compatibility layer with safer Quality behavior and clear Sodium detection in the menu and status output
- Improved the preset menu layout and wording so it is easier to read
- Improved inactive preset wording so it displays `Selected: None`
- Fixed older `1.21-1.21.11` versions where the top menu section could overlap the preset buttons
- Fixed newer `1.21-1.21.11` versions where the top menu section could disappear entirely
- Fixed utility menu buttons so `Auto-apply` and `Aggressive mode` toggle correctly without kicking you out of the menu
- Fixed cross-version graphics option access so restore and preset actions do not crash on newer `1.21-1.21.11` versions

## Files To Upload

Use the existing final jar:

- `build/libs/AutoTune FPS v0.1.4-1.21-1.21.11.jar`

## Upload Checklist

1. Confirm the final jar version is `0.1.4`.
2. In Modrinth, create a new version for the project.
3. Set the version channel to `release`.
4. Mark the intended `1.21-1.21.11` versions from `1.21` through `1.21.11`, and loader `Fabric`.
5. Upload the main jar as the primary file.
6. Paste the changelog above.

## Release Notes

- This is the stable `1.21-1.21.11` line of `0.1.4`.
- Jar filename: `AutoTune FPS v0.1.4-1.21-1.21.11.jar`
- Internal mod version stays `0.1.4`.

## Notes

Modrinth versions accept a `.jar` file and use a release channel per version.
Official Modrinth docs for version uploads:

- https://docs.modrinth.com/api/operations/createversion/
