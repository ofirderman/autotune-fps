# Modrinth 0.1.4 26.1-26.1.2 Release Notes

## Suggested Version Metadata

- Name: `AutoTune FPS 0.1.4 (26.1-26.1.2)`
- Version number: `0.1.4`
- Release channel: `release`
- Minecraft versions: `26.1`, `26.1.1`, `26.1.2`
- Loaders: `Fabric`
- Environment: `Client`

## Suggested Summary

`AutoTune FPS` `0.1.4` is available as a separate `26.1-26.1.2` Fabric build for players who want the preset-based release on the newer Minecraft line.

## Suggested Changelog

- Ported `0.1.4` to the `26.1-26.1.2` Minecraft line as a separate Fabric jar
- Kept the preset-based workflow and commands from the `1.21-1.21.11` line
- Added `/optimizer presets` and `/optimizer preview recommended`
- Updated the client command and menu compatibility layer for the newer client API
- Fixed graphics option detection so recommended/manual presets apply correctly on the `26.1` line
- Improved inactive preset wording so it displays `Selected: None`

## Files To Upload

Use the separate `26.1-26.1.2` port jar:

- `build/libs/AutoTune FPS v0.1.4-26.1-26.1.2.jar`

## Upload Checklist

1. Confirm the final jar version is `0.1.4`.
2. Create a new version for the project.
3. Set the version channel to `release`.
4. Mark the intended `26.1`, `26.1.1`, and `26.1.2` versions and loader `Fabric`.
5. Upload the `26.1-26.1.2` jar as its own file/version entry.
6. Paste the changelog above.

## Release Notes

- This is a separate `26.1-26.1.2` build of `0.1.4`.
- Jar filename: `AutoTune FPS v0.1.4-26.1-26.1.2.jar`
- Internal mod version stays `0.1.4`.
- Jar metadata depends on `minecraft >=26.1 <=26.1.2` and `java >=25`.
