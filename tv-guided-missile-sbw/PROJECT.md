# TV Guide Missile for Superb Warfare 1.20.1

This is a custom full build of Superb Warfare based on upstream commit `1bb4690` (`0.8.9.1-snapshot`). It adds a first-person, manually controlled 9M120 missile exclusively to the Mi-28 secondary gunner seat. The complete source is kept here so the system can be tuned or adapted to another vehicle later.

> This is a replacement Superb Warfare jar, not a separate add-on. Never install it beside another Superb Warfare jar.

## Installation

1. Back up your world and modpack.
2. Remove the existing Superb Warfare jar from the modpack's `mods` folder.
3. Download `SuperbWarfare-Mi28-TV-Guided-1.20.1.jar` from the [TV Guide Missile release](https://github.com/Synergy902/Project/releases/tag/tv-guide-missile-v1.0.0) and place it in `mods`.
4. Keep the normal Superb Warfare 1.20.1 dependencies installed.
5. For multiplayer, the server and every client must use this same custom jar. Do not install it alongside another Superb Warfare jar.

## Controls

1. Enter the Mi-28 second seat (the forward gunner/operator seat). The pilot cannot use TV guidance.
2. Select **9M120 TV-Guided Missile** from the gunner weapon list.
3. Fire normally. The view transfers to the missile after launch.
4. Guide the missile with the mouse.
5. Press **X** (SBW's configurable Interact key) to break the link. The missile then continues unguided.

The view returns automatically when the missile hits, expires, is destroyed, exceeds the 1,024-block control link, or the gunner leaves the Mi-28.

## Included behavior

- Server-authoritative steering with ownership, Mi-28 type, launcher UUID, seat index, turn-rate, and range validation.
- Responsive mouse steering with smoothing and a 7.5-degree-per-tick hard limit.
- Launch-authority ramp so the missile clears the helicopter before maximum maneuverability.
- Missile-nose camera with interpolated position and motion-derived rotation.
- TV HUD with scan lines, telemetry, link status, and a clean black-and-white dynamic reticle. The open circular reticle and side scales bank together with missile steering.
- Normal SBW overlays and weapon inputs are suppressed while controlling the missile.
- The pilot retains normal helicopter control while the gunner guides the missile.
- Wire-guided missile tracking range raised to 1,024 blocks.
- The external missile uses the 9M336 model and heavy-missile presentation.
- The local first-person view omits the close-up missile model, flare, and long-lived trail particles to protect frame rate; other players and external views still see the complete effect.
- Non-text HUD geometry is batched, static reticle geometry is precomputed, and telemetry text is refreshed at 5 Hz to reduce render overhead.
- F5 perspective switching is blocked during active TV control to prevent the known camera-mode crash.

## Verification

- `gradlew build --no-problems-report`: **BUILD SUCCESSFUL**
- Kotlin and Java compilation passed.
- Mixin processing, resources, reobfuscation, and jar-in-jar packaging passed.
- Runtime bytecode verification confirms that Mixin shadow fields use Forge's obfuscated production names.
- Modified Mi-28 and localization JSON files parsed successfully.
- The release jar was audited for the controller, HUD, camera mixin, networking messages, renderer, and Mi-28 data.
- SHA-256: `8F2A792CC81C23945BC20012A716FFBEF89F6B27816DCC6AD86E6CBB5965D51A`

The final build has been flight-tested in single-player. A two-client multiplayer flight test is still recommended; the server and every client must use the same jar.

## Main tuning points

Steering and control constants are in `WireGuideMissileEntity.kt` and `TvMissileClientHandler.kt`:

- Maximum turn: `7.5` degrees/tick
- Control range: `1024` blocks
- Steering response: `0.82`
- Mouse conversion rate: `0.075`
- Missile speed envelope: `2.35-3.25` blocks/tick

The code remains subject to Superb Warfare's upstream licenses. Preserve all upstream notices and license terms when modifying or redistributing it.

For the code map and the safest way to add TV guidance to another vehicle, see [ADAPTING.md](ADAPTING.md).
