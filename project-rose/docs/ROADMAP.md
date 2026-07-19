# Project Rose roadmap

## Implemented in the first local alpha

- Forge 1.20.1 / Java 17 / TACZ 1.1.8-hotfix build target.
- A persistent map pool with one active arena at a time; each arena stores its own lobby, spectator position, bounds, and multiple team spawns.
- Five administrator-owned full-inventory loadout snapshots.
- TACZ gunpack-safe item NBT preservation.
- Waiting, countdown, active, winner reveal, and map-vote states.
- Black Ops 1-style 7500-point, ten-minute Team Deathmatch defaults (100 points per kill).
- Red and Blue team selection, no friendly fire, and scoreboard coloring.
- Eight-second pre-match forced-balance warning with a configurable five-player eligibility threshold.
- Complete administrator immunity from forced balancing.
- Join-in-progress deployment after team and class selection.
- Three-second spawn protection that ends when a TACZ gun fires.
- Individual vanilla respawn followed by complete loadout replenishment.
- Death/drop/pickup protection and basic arena protection.
- TACZ gun ID and headshot-aware kill attribution.
- Network-synchronized Black Ops 1-inspired team/class screen and HUD.
- Client HUD toggles, opacity, and offsets through Forge client configuration.
- Graphical administrator editor with 41 ghost inventory/equipment slots, stack-count controls, import, clear, rename, and save.
- Optional Curios inventory snapshot and restoration.
- Custom Black Ops 1-inspired buttons and panels instead of vanilla menu styling.
- Full-screen winning-team fade, transition invulnerability, synchronized map poll, vote tallying, active-map selection, and automatic next-match start.
- Voluntary spectator support and unrestricted administrator-defined inventory/hotbar positions.
- Per-map team reselection with preserved classes and an automatic next-match preparation window.

## Next implementation pass

- In-game HUD options screen.
- Graphical administrator arena editor.
- Killfeed entries as timed HUD cards instead of chat-only notifications.
- Assist damage tracking.
- Spawn line-of-sight and recent-death weighting.
- Out-of-bounds countdown.
- Map preview images and richer match-end statistics.
- Dedicated-server-safe automated tests and GameTests that do not require launching a graphical client.
- Curios slot previews and direct ghost editing inside the Project Rose editor.

## Later mode framework

After Team Deathmatch is stable, add separately configurable COD-style defaults for Free-for-All, Domination, Hardpoint, Kill Confirmed, and Search and Destroy. No killstreak system is planned for the initial scope.
