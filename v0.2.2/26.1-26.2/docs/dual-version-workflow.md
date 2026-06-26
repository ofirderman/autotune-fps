# Dual-Version Workflow

AutoTune FPS now keeps two release lines for the same mod version:

- `1.21-1.21.11` line: the main repo root
- `26.1-26.2` line: a separate port workspace under `project/ports/`

## Canonical Layout

- `build/libs/AutoTune FPS vX.Y.Z.jar`
  Main `1.21-1.21.11` release jar from the repo root
- `build/libs/AutoTune FPS vX.Y.Z-26.1-26.2.jar`
  Convenience copy of the separate `26.1-26.2` release jar in the main repo libs folder
- `project/ports/vX.Y.Z-26.1-26.1.2/build/libs/AutoTune FPS vX.Y.Z-26.1-26.2.jar`
  Separate `26.1-26.2` release jar

## Source Of Truth

- The repo root stays the active main development line
- A `26.1-26.1.2` port workspace is created as a separate workspace and can publish a jar covering `26.1` through `26.2`
- The port workspace keeps its own build files, Gradle wrapper, and tracked `build/libs` jar

## Future Release Flow

1. Finish the main release work in the repo root
2. Build and verify the `1.21-1.21.11` jar from the repo root
3. Run:
   `powershell -ExecutionPolicy Bypass -File .\project\scripts\new-26x-port.ps1 -Version X.Y.Z`
4. Open the generated port workspace at:
   `project/ports/vX.Y.Z-26.1-26.1.2`
5. Build and verify the `26.1-26.2` jar there
6. Keep both jars:
   - `build/libs/AutoTune FPS vX.Y.Z.jar`
   - `build/libs/AutoTune FPS vX.Y.Z-26.1-26.2.jar`
   - `project/ports/vX.Y.Z-26.1-26.1.2/build/libs/AutoTune FPS vX.Y.Z-26.1-26.2.jar`

## Notes

- The helper script copies the current main-line source into a fresh `26.1-26.1.2` workspace
- The helper script reuses the already-working `26.1-26.1.2` build scaffold from `project/ports/v0.1.3-26.1-26.1.2`
- Building a `26.1-26.2` jar also copies its release jar into the main repo `build/libs` folder for easier access
- The helper script does not publish anything and does not touch gameplay code
- Runtime testing is still required for the new `26.1-26.2` jar after the workspace is created
