# Project Rose administrator guide

This guide describes the current graphical and command-based alpha workflows.

## Configure the map pool

Create or select a map before editing it:

```text
/rose map create nuketown
/rose map select nuketown
/rose map list
```

Map IDs are persistent, lowercase identifiers. `create` also makes the new map active. Project Rose supports a pool of configured maps but runs only one active arena at a time. Every `/rose arena` command below edits the currently active map.

## Configure the active arena

Stand at each desired location and use:

```text
/rose arena setlobby
/rose arena setspectator
/rose arena setcorner1
/rose arena setcorner2
/rose arena addspawn red
/rose arena addspawn blue
```

Run each `addspawn` command from as many safe spawn locations as desired. Project Rose chooses the configured spawn farthest from online enemies when a participant respawns.

Other arena commands:

```text
/rose arena clearspawns red
/rose arena clearspawns blue
/rose arena status
```

## Save the five administrator loadouts

### Graphical editor

Open the team/class menu with `M`, select **ADMIN LOADOUTS**, then choose one of the five classes. Only players with permission level 2 can see and use these controls.

The editor contains ghost copies of all 36 inventory slots, four armor slots, and offhand. It never consumes the operator's items.

- Click a carried item into a ghost slot to copy the exact stack and NBT.
- Right-click a carried item to place one; repeat right-clicking the same item to increase the amount.
- Right-click a ghost stack with an empty cursor to reduce its amount by one.
- Left-click a ghost slot with an empty cursor, press the drop key, or use **CLEAR** to remove items.
- Shift-click an item in the operator inventory to copy it into the next empty class slot.
- **IMPORT** copies the operator's complete current inventory and armor into the editor.
- **SAVE** stores the ghost inventory and captures the operator's currently equipped Curios slots.

This supports arbitrary vanilla and modded items, including complete TACZ gunpack NBT, attachments, ammunition state, and fire mode.

### Snapshot command

Arrange the administrator's inventory exactly as the class should appear, including hotbar, main inventory, armor, offhand, TACZ attachments, loaded ammunition, throwables, healing items, and food. Then run:

```text
/rose class save 1 Assault
/rose class save 2 Heavy
/rose class save 3 Marksman
/rose class save 4 Recon
/rose class save 5 Support
```

These names are examples. The saved preset is the complete inventory snapshot, not a restricted list of item categories.

Useful loadout commands:

```text
/rose class apply 1
/rose class clear 1
/rose class select 1
```

Closing the screen without selecting a team leaves the player as a spectator. During an active match, a player with a team and configured class deploys immediately. Class changes made during combat apply on the next respawn.

`apply` is an operator-only setup aid. `select` is available to players and takes effect when their inventory is next replenished.

## Player workflow

The team/class screen opens on login and can be reopened with `M` or:

```text
/rose menu
```

Command alternatives are:

```text
/rose join red
/rose join blue
/rose class select 1
```

## Match controls

```text
/rose match start
/rose match status
/rose match stop
```

Automatic start is disabled by default. The common config can enable it.

The initial match remains administrator-started. At match end, all players become invulnerable while the winning team appears, followed by a synchronized map vote. Players can reopen an active poll with:

```text
/rose vote
```

When voting ends, the map with the highest total becomes active; tied maps are resolved automatically. Team assignments are cleared, class selections are retained, and every player returns to spectator so they can choose Red or Blue again. After the configurable team-selection window, the next match starts automatically once both teams and their classes are valid. Players who do not choose remain spectators.

## Configuration files

Forge creates common/server and client configuration files after the mod is loaded.

The common options include score and time limits, automatic start, winner-display, map-vote and next-map team-selection durations, maximum vote choices, friendly fire, item/drop restrictions, arena protection, and the complete pre-match balance thresholds.

The client options include HUD visibility, personal statistics, balance warnings, opacity, and screen offsets.

## Current testing boundary

The alpha has been compiled and statically inspected but has intentionally not launched Minecraft. It should not be copied into a live CurseForge instance until the owner requests an installation/test build.
