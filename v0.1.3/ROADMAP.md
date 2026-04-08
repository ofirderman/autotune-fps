# ROADMAP - AutoTune FPS

## Vision
Build a Minecraft mod that players actually want to install because it improves performance and makes optimization simple.

This mod is not trying to replace major rendering or performance mods.
Its goal is to be the easy optimization layer that helps regular players get better settings fast, safely, and with minimal effort.

Core promise:
> Optimize Minecraft in one click.

---

## Product Direction
The mod should focus on:
- simple performance presets
- safe automatic optimization
- better default settings for common situations
- recovery and fallback options when the game runs badly
- real AutoTune-owned performance systems beyond vanilla setting wrappers
- clear UX instead of technical complexity

This should feel like:
- easy to install
- easy to understand
- useful immediately
- safe to test and easy to revert

---

## Positioning
This mod is not:
- a full render rewrite
- a Sodium replacement
- an experimental "AI optimizer"
- a giant all-in-one kitchen sink mod

This mod is:
- a smart optimization helper
- a preset-based performance manager
- a safety layer for users who do not want to tweak every setting manually

---

## v0.1 - Foundation Release
### Goal
Ship a small, stable, useful first version that works reliably and gives instant value.

### Shipped Scope
- first public beta shipped
- client-only Fabric mod
- config system
- preset system
- in-game commands and preset menu
- safe settings application with runtime fallbacks
- hardware-tier-aware recommendations
- public documentation and release packaging

### Presets
Initial presets:
- Ultimate Performance
- Performance
- Balanced
- Quality
- Off

### v0.1 Settings Targets
Only apply safe, low-risk settings in v0.1, such as:
- particles
- clouds
- graphics-related safe toggles
- chunk update priority where appropriate
- entity-related distance options where safe
- other lightweight visual and performance options that do not create major compatibility risk

### v0.1 Requirements
- mod loads without crashing
- presets can be switched reliably
- applied settings persist correctly
- config is readable and maintainable
- behavior is predictable
- codebase is clean enough to extend

### Non-goals for v0.1
- no heavy mixin work unless absolutely necessary
- no dynamic "change settings every second" system
- no benchmarking system yet
- no advanced compatibility engine yet
- no shader-specific tuning yet
- no multi-loader support

### Success Criteria
v0.1 is successful if:
- it launches cleanly
- presets work reliably
- users can test it easily
- it works across the supported `1.21.x` patch range
- the project becomes a stable base for future versions

---

## v0.2 - Smart Optimization
### Goal
Move from static presets to guided optimization.

### Scope
- first-run setup flow
- recommendation logic for presets
- optional adaptive mode
- first true non-vanilla optimization feature owned by AutoTune itself
- target-FPS or dynamic tuning behavior exposed in the mod menu
- better config editing
- clearer explanations for each preset
- safe revert to previous settings

### Adaptive Mode Rules
Adaptive mode must:
- be optional
- avoid constant switching
- use cooldowns and thresholds
- never feel chaotic
- always favor stability over aggression

### Success Criteria
- users understand what changed and why
- changes are not too frequent
- optimization feels smart, not random
- reverting is easy and safe

---

## v0.3 - Recovery and Compatibility
### Goal
Make the mod something users keep installed long-term.

### Scope
- safe mode and recovery mode after bad launches
- fallback behavior for problematic configs
- better compatibility with common optimization mods
- profile export and import
- usage-based profiles such as:
  - PvP
  - Survival
  - Low-end PC
  - Shaders-light

### Success Criteria
- users can recover from bad settings easily
- compatibility issues are reduced
- switching profiles is reliable
- mod feels safer and more polished

---

## v0.4 - Standout Features
### Goal
Add features that make the mod genuinely memorable and worth recommending.

### Candidate Features
- quick in-game benchmark
- one-click "Fix My FPS"
- lightweight performance report
- automatic recommendations based on observed performance
- warnings before enabling heavy settings
- smarter onboarding for less technical users

### Rule for v0.4
Only add standout features after the foundation is stable.

---

## Technical Rules
To avoid build and tooling chaos:

- use a proper Gradle Wrapper only
- do not keep custom Gradle hacks inside the project
- keep the project clean and reproducible
- prefer small isolated changes
- prioritize stability over cleverness
- add complex systems only after the simple version works

---

## Code Principles
- keep classes focused
- avoid unnecessary mixins
- document anything version-sensitive
- isolate version-dependent logic where possible
- keep config names clear
- make future expansion easy

---

## UX Principles
- simple wording
- obvious preset names
- clear explanation of what each preset does
- easy to undo changes
- avoid overwhelming the player
- make the mod feel safe and friendly
