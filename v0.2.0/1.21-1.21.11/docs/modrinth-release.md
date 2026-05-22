# Modrinth 0.2.0 Release Notes

## Suggested Version Metadata

- Name: `AutoTune FPS 0.2.0`
- Version number: `0.2.0`
- Release channel: `release`
- Minecraft versions: `1.21` through `1.21.11`
- Companion supported range: `26.1` through `26.1.2` is shipped by the sibling 0.2.0 jar.
- Loaders: `Fabric`
- Environment: `Client`

## Suggested Summary

`AutoTune FPS` adds Smart Recommendation, persistent restore behavior, and join-time auto-apply modes while keeping the optimizer preset workflow simple.

## If This Is A New Modrinth Project

- Project type: `Mod`
- Title: `AutoTune FPS`
- Slug: `autotune-fps` if available
- Single-player side: `Required`
- Server side: `Unsupported`
- License: `MIT`

## Suggested Changelog

- Added Smart Recommendation for `Performance`, `Balanced`, and `Quality`
- Every public preset is adaptive; Smart Recommendation only chooses which adaptive preset to use
- `Ultimate Performance`: Maximum FPS focus with limited distance scaling.
- `Performance`: FPS-focused settings with light adaptive scaling.
- `Balanced`: Middle point between FPS and visual quality.
- `Quality`: Higher visual quality while keeping performance reasonable.
- `Restore`: Restore your original settings from before AutoTune FPS changed them.
- Smart Recommendation uses detected hardware plus shader/compatibility signals
- Current settings are used for preview diffs, Custom settings detection, and the restore baseline, not as proof of performance
- Smart Recommendation never automatically chooses `Ultimate Performance`
- `Ultimate Performance` is adaptive only in a narrow FPS-first range
- Simplified Smart Recommendation menu/status/preview/apply text so internal levels and change counts stay out of normal UI
- Added auto-apply modes: `Off`, `Selected preset`, and `Smart recommendation`
- Kept `/optimizer autoapply on` as a legacy alias for selected-preset auto-apply
- `/optimizer recommend` applies Smart Recommendation
- `/optimizer preview recommended` previews Smart Recommendation without changing settings
- Restore reuses the original pre-AutoTune settings snapshot instead of replacing it automatically
- Protected settings remain untouched: Max FPS, VSync, FOV, GUI Scale, Brightness, and resolution/fullscreen

## Commands

- `/optimizer recommend`
- `/optimizer preview recommended`
- `/optimizer autoapply off`
- `/optimizer autoapply selected`
- `/optimizer autoapply smart`
- `/optimizer autoapply on` as a legacy alias for `selected`

## Files To Upload

Use the existing final jar for this range:

- `build/libs/AutoTune FPS v0.2.0-1.21-1.21.11.jar`

The 0.2.0 release also includes the sibling `26.1` through `26.1.2` jar:

- `build/libs/AutoTune FPS v0.2.0-26.1-26.1.2.jar`

## Upload Checklist

1. Confirm the final jar version is `0.2.0`.
2. In Modrinth, create a new version for the project.
3. Set the version channel to `release`.
4. Mark the intended `1.21-1.21.11` versions from `1.21` through `1.21.11`, and loader `Fabric`.
5. Upload the main jar as the primary file.
6. Paste the changelog above.

## Notes

Modrinth versions accept a `.jar` file and use a release channel per version.
