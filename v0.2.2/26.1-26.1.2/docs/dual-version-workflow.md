# Why There Are Two Folders

AutoTune FPS v0.2.2 is available for two Minecraft target ranges:

- `1.21-1.21.11`
- `26.1-26.1.2`

Each target range has its own folder because the Minecraft/Fabric build target, mappings, and compatibility details are different enough that keeping separate Gradle workspaces is safer and easier to review.

## What To Use

Use the folder that matches the Minecraft version range you want to inspect or build:

- `1.21-1.21.11` for the Minecraft 1.21 line.
- `26.1-26.1.2` for the Minecraft 26.1 line.

Both folders contain the same AutoTune FPS mod version and the same feature set for v0.2.2. The split is only for version compatibility and build safety.

## What Is Included

Each folder includes the source code, Gradle build files, Gradle wrapper, README, CHANGELOG, and license for that target range.

Generated build outputs, local validation logs, private planning notes, and release packaging files are not part of this public source folder.
