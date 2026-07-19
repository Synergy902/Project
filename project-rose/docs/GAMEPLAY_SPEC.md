# Project Rose gameplay specification

## Initial scope

- One active arena per server, selected from an administrator-managed persistent map pool.
- Red and Blue teams selected freely before a match.
- One Team Deathmatch ruleset in the first playable release; the match engine remains mode-extensible.
- Five loadout presets configured only by server operators or players with permission level 2.
- A loadout is an exact snapshot of all 36 inventory slots, four armor slots, the offhand slot, and—when installed—the player's Curios inventory.
- TACZ gun NBT, attachments, ammunition, fire mode, heat, and gunpack identifiers are preserved by copying the complete item stacks.
- Curios integration is optional; Project Rose still loads when Curios is absent.

## Team Deathmatch defaults

- Score limit: 7500 team points, with 100 points per enemy elimination (75 kills).
- Time limit: 10 minutes.
- Friendly fire: disabled.
- Individual immediate respawn after the player presses Respawn.
- No inventory or equipment drops.
- On respawn, the selected preset replaces the player's complete carried inventory and armor.
- No map pickups; loadouts are the only source of combat items.
- The first match is started manually by an administrator. After a map vote, the next match starts automatically when its selected arena is ready.
- Players who join an active match remain spectators until they choose a team and configured class, then deploy immediately.
- Existing participants can change class during a match, but the new class applies only on the next respawn.
- Spawn protection lasts three seconds and ends immediately when the protected player fires a TACZ gun.

## Pre-match balancing

- Balancing runs only before a match.
- It becomes eligible when the larger team has at least five players and leads by at least two.
- A bold red eight-second warning is shown to all players.
- The warning cancels if the teams become balanced.
- When the countdown completes, the minimum number of recently joined non-administrator players is moved so the team sizes differ by no more than one.
- Administrators are completely immune to forced team switching. If administrators are the only remaining cause of an imbalance, balancing stops without moving them.

## Match lifecycle

`WAITING -> COUNTDOWN -> ACTIVE -> POST_MATCH -> MAP_VOTE -> COUNTDOWN`

- Waiting: players select a team and class; administrators can edit the arena and presets.
- Countdown: a short start countdown freezes combat and locks teams.
- Active: scores, time, deaths, and respawns are tracked server-side.
- Post-match: every player is invulnerable while a full-screen fade reveals the winning team and final score.
- Map vote: every connected player, including voluntary spectators, may vote for one of up to five configured arena names. The highest vote total wins; a tied result is resolved automatically.
- After voting, all team assignments are cleared while class selections are preserved. Everyone returns to spectator, chooses Red or Blue again, and receives a configurable 15-second team-selection window before Project Rose can start the next match automatically.
- If the winning arena is incomplete or the teams/classes are not ready, the server returns to Waiting and tells administrators what must be corrected.

Players may close the team/class screen without joining and remain spectators for as long as they choose. Class hotbar and inventory positions are unrestricted; Project Rose does not impose weapon, throwable, healing, or food slot labels.

## Visual direction

- Original, non-infringing interface specifically inspired by the 2010 Black Ops 1 military-terminal presentation.
- Charcoal and near-black glass panels, warm amber highlights, condensed labels, white numerals, subtle scanline/noise accents, and strong red/blue team identification.
- TACZ retains ownership of its ammo and weapon HUD area; Project Rose avoids duplicating it.
- HUD modules will be movable, scalable, and individually toggleable on the client.

The first client configuration exposes HUD visibility, personal-stat visibility, balance-warning visibility, panel opacity, and scoreboard/stat-panel offsets. A matching in-game options screen is planned after the visual direction is confirmed.

## Explicit project rules

- Do not launch Minecraft during development.
- Do not push to or create content in the provided GitHub repository until the owner explicitly gives approval.
