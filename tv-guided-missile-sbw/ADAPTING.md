# Adapting TV Guide Missile

This guide is the shortest path to reuse the system on another Superb Warfare vehicle while keeping server authority, camera smoothing, HUD behavior, and performance safeguards intact.

## Code map

| Responsibility | File |
| --- | --- |
| Missile lifecycle, controller validation, steering, range, and tuning constants | `src/main/kotlin/com/atsuishio/superbwarfare/entity/projectile/WireGuideMissileEntity.kt` |
| Mouse input, smooth camera position/rotation, client control state | `src/main/kotlin/com/atsuishio/superbwarfare/client/TvMissileClientHandler.kt` |
| TV HUD and dynamic black-and-white reticle | `src/main/kotlin/com/atsuishio/superbwarfare/client/overlay/TvGuidedMissileOverlay.kt` |
| Camera takeover | `src/main/java/com/atsuishio/superbwarfare/mixins/CameraMixin.java` |
| F5/input guards and manual cancel | `src/main/kotlin/com/atsuishio/superbwarfare/event/ClickEventHandler.kt` and `ClientMouseHandler.kt` |
| Start, control, and end packets | `src/main/kotlin/com/atsuishio/superbwarfare/network/message/` |
| 9M336 external model and effect rendering | `src/main/kotlin/com/atsuishio/superbwarfare/client/renderer/projectile/TvGuidedMissileRenderer.kt` |
| Local first-person visual suppression | `src/main/kotlin/com/atsuishio/superbwarfare/entity/projectile/TvMissileVisualState.kt` |
| Projectile renderer registration | `src/main/kotlin/com/atsuishio/superbwarfare/init/ModEntityRenderers.kt` |
| Launcher vehicle UUID assignment | `src/main/java/com/atsuishio/superbwarfare/item/gun/GunItem.kt` |
| Current Mi-28 weapon and launch points | `src/main/resources/data/superbwarfare/sbw/vehicles/mi_28.json` |

## Add it to another vehicle

1. In that vehicle's JSON, create or modify the intended passenger weapon so its `Projectile` is `superbwarfare:wire_guide_missile`. Copy the Mi-28 `PassengerMissile` entry as the working reference, then adjust its magazine and `ShootPos` for the new model.
2. In `WireGuideMissileEntity.isTvControllerEligible`, allow the new `ModEntities` vehicle type and its intended seat index. Seat indexes are zero-based in `getSeatIndex`; the Mi-28 secondary gunner is index `1`.
3. Apply the same vehicle-and-seat rule in `TvMissileClientHandler` and `ClickEventHandler`. Client checks improve input behavior, but the server check in `WireGuideMissileEntity` is the security boundary and must remain authoritative.
4. Add or reuse a localized weapon name in `src/main/resources/assets/superbwarfare/lang/en_us.json`.
5. Keep `GunItem`'s `setLauncherVehicle` assignment. It binds the missile to the vehicle that fired it and prevents a player in another vehicle from taking control.
6. Build the replacement jar and test the pilot and gunner simultaneously. Confirm that only the intended seat gets TV control, the pilot retains flight control, cancel returns the view, F5 is blocked only during TV mode, and the camera returns on impact or timeout.

## Main tuning points

`WireGuideMissileEntity.kt` contains the flight envelope and server limits:

- `MI_28_GUNNER_SEAT`: current authorized seat
- `MAX_TV_CONTROL_RANGE`: maximum link distance
- `MAX_TURN_PER_TICK`: hard turn-rate limit
- `MIN_TV_PITCH` / `MAX_TV_PITCH`: vertical control limits
- `MIN_TV_SPEED` / `MAX_TV_SPEED`: controlled speed envelope
- `STEERING_RESPONSE`: how strongly commanded direction affects velocity
- `INPUT_DECAY` and `INPUT_TIMEOUT_TICKS`: packet-loss safety behavior

`TvMissileClientHandler.kt` contains mouse sensitivity, input smoothing, and camera interpolation. Change one value at a time and flight-test it; camera smoothing and server steering solve different parts of perceived motion.

## Build and install

From this directory on Windows, run `gradlew.bat build --no-problems-report`. The distributable is the `-all.jar` under `build/libs/`.

Remove the normal Superb Warfare jar before installing the custom build. In multiplayer, install the same custom jar on the server and every client. Preserve the upstream licenses and notices when redistributing a modified build.
