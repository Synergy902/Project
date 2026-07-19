# Project Rose

Project Rose is a Minecraft Forge 1.20.1 team-combat mod built for TACZ 1.1.8 and dynamically loaded TACZ gunpacks.

The initial release focuses on one active Red-versus-Blue Team Deathmatch arena at a time, an administrator-managed map pool, five administrator-owned full-inventory loadouts, server-authoritative scoring, pre-match team balancing, post-match map voting, and a Black Ops 1-inspired HUD and menu system.

## Development target

- Minecraft 1.20.1
- Forge 47.4.10
- Java 17
- TACZ 1.1.8-hotfix

## Safety constraints for this project

- Development builds do not launch Minecraft automatically.
- The CurseForge instance is inspected read-only unless the owner explicitly approves installing a built JAR.
- Nothing is pushed or published to GitHub until the owner explicitly approves it.

## Build

```powershell
.\gradlew.bat build
```

The reobfuscated mod JAR is produced in `build/libs`.

Minecraft is not launched by the build task.

## Current administrator workflow

See [`docs/ADMIN_GUIDE.md`](docs/ADMIN_GUIDE.md) for the arena, loadout, team, and match commands implemented in the current alpha.
