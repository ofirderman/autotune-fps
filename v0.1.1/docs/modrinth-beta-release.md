# Modrinth 0.1.1 Release Notes

## Suggested Version Metadata

- Name: `AutoTune FPS 0.1.1`
- Version number: `0.1.1`
- Release channel: `release`
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

- Updated metadata for the `1.21.x` patch line
- Improved `/optimizer help` with short command explanations
- Shortened `/optimizer status` to keep only the most useful information
- Cleaned up preset menu text and removed duplicate recommendation lines
- Added current and recommended markers directly to preset buttons
- Show the detected recommended preset on the main `Recommended` button
- Disable already-selected preset actions in the preset menu
- Added recovery mode for interrupted preset applies
- Successful manual or recommended actions now clear recovery mode without silently turning auto-apply back on
- Still does not modify Max FPS / Framerate Limit, VSync, FOV, GUI Scale, Brightness, or Resolution / fullscreen mode

## Files To Upload

After rebuilding with the `0.1.1` version, the main upload target should be:

- `build/modrinth/autotune-fps-0.1.1.jar`

## Upload Checklist

1. Build the project with your working local JDK 21 setup.
2. Confirm the output jar version is `0.1.1`.
3. In Modrinth, create a new version for the project.
4. Set the version channel to `release`.
5. Mark the intended `1.21.x` versions from `1.21.1` through `1.21.11`, and loader `Fabric`.
6. Upload the main jar as the primary file.
7. Paste the changelog above.

## Notes

Modrinth versions accept a `.jar` file and use a release channel per version.
Official Modrinth docs for version uploads:

- https://docs.modrinth.com/api/operations/createversion/
