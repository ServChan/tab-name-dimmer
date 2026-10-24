# Tab Name Dimmer — User Guide

Mod version: 1.2.3 · Minecraft 26.3 · Fabric · client-only

[Русская версия](USER_GUIDE_RU.md)

## Contents

1. [What the mod does](#what-the-mod-does)
2. [Installation](#installation)
3. [Getting started](#getting-started)
4. [How the effect turns on](#how-the-effect-turns-on)
5. [Player groups](#player-groups)
6. [List modes](#list-modes)
7. [Outlines and transparency in the world](#outlines-and-transparency-in-the-world)
8. [AFK players](#afk-players)
9. [Join and leave notifications](#join-and-leave-notifications)
10. [Compact HUD](#compact-hud)
11. [Import and export](#import-and-export)
12. [All settings](#all-settings)
13. [Files](#files)
14. [FAQ](#faq)
15. [Building from source](#building-from-source)

## What the mod does

On big servers the player list (`Tab`) holds dozens of names, and the ones you care about — friends, clan mates, enemies or moderators — are hard to find. Tab Name Dimmer sorts players into your own groups:

- it **dims** everyone outside your groups and **moves** tracked players to the top;
- it can **show only tracked players** or put them in a separate compact list on screen;
- it **colours** names in the group colour;
- it **outlines** group members right in the world and makes everyone else see-through;
- it **tells you** when a tracked player joins or leaves;
- it can move AFK players to the end or hide them.

Groups can be global for every server or specific to one server. The mod runs only on your side and sends nothing to the server.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) `0.19.3` or newer for Minecraft 26.3.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) `0.161.0+26.3` or newer into the `mods` folder.
3. Put `tab-name-dimmer-1.2.3.jar` into `mods` (see [Building](#building-from-source)).
4. Recommended: [Mod Menu](https://modrinth.com/mod/modmenu) `21.0.0`, which opens the settings.

Java 25 is required.

## Getting started

1. Open **Mod Menu → Tab Name Dimmer → settings icon**.
2. Press "Groups and profiles". A green "Default" group is already there.
3. Rename it, say to "Friends", and pick a colour.
4. Press "Edit members" and type names. Or open "Online players" and add people with a click.
5. Press Save on the main screen.
6. On a server open the player list (`Tab`) and hold `Shift`: friends move to the top, everyone else is dimmed.

## How the effect turns on

The mod does not change the list all the time, only while the effect is **active**. The Activation setting chooses how:

| Mode | How it works |
|---|---|
| Hold either Shift (default) | active while the left or right Shift is held |
| Hold custom key | active while your own key is held |
| Custom key toggle | one press turns it on, the next turns it off |

The custom key is set in **Options → Controls → Tab Name Dimmer** (left Alt by default).

Dimming and sorting work in the `Tab` list. Outlines and transparency in the world also work only while the `Tab` list is open and the effect is active.

## Player groups

The "Groups and profiles" screen.

### A group

| Field | Meaning |
|---|---|
| Group name | anything |
| Group color (#RRGGBB) | the name and outline colour; the button next to it cycles through presets |
| Group enabled | a disabled group is kept but has no effect |
| Colorize Tab names | members' names take the group colour |
| Group outline | members get an outline in the world |
| Group transparency | members become see-through in the world |
| Group priority | the higher one wins when a player is in several groups, and is listed higher in Tab |

- `<` and `>` switch between groups, "Add group" creates one, "Duplicate" copies a group with its members.
- "Move up" and "Move down" change the order. With equal priority, the group higher in the list wins.
- `-` and `+` change the priority (by 10 with Shift); clicking the value resets it to 0.
- "Delete group" needs a second click.

### Members

"Edit members" opens the group's name list.

- Type names one at a time and press Add.
- **Masks:** `*` matches any text, `?` one character. `Clan_*` adds everyone whose name starts with `Clan_`, `Bot?` matches `Bot1`, `BotA` and so on.
- Search by name, "Sort A-Z" (sorts and removes duplicates), "Clear list".
- "Online players" lists everyone on the server and shows which group a player is already in, or whether a mask matches them.
- Name case is ignored while Case sensitive is off.

### Global groups and server groups

The "Editing: Global groups / This server's groups" switch chooses what you edit.

- Global groups apply on every server.
- A server's own groups **fully replace** the global groups on that server.
- The first switch creates a copy of the global groups. An unchanged copy is discarded when you leave.
- "Use global fallback" deletes the server's groups so the global ones apply there again.

The line under the title tells you what applies on the current server.

Changes apply with Save on the main screen, or at once with `Ctrl+S` on the nested screens. Unsaved changes are flagged with an "Unsaved changes" line.

## List modes

The Mode setting:

| Mode | What happens |
|---|---|
| Animated sorting (default) | tracked players glide to the top, everyone else is dimmed |
| Show tracked only | only players from your groups stay in the list |
| Separate compact list | the normal list is untouched, and tracked players get a separate list on screen (see [Compact HUD](#compact-hud)) |

Within-group sorting: server order (default), by name or by ping. Sorting speed sets how fast players move in the list.

## Outlines and transparency in the world

- **Group outline.** Players in a group with the outline on glow in its colour, like the glowing effect. Only you see it.
- **Untracked player transparency.** Players outside your groups become see-through, and "Transparent player opacity" sets how much. Handy for making your people stand out in a crowd.
- **Group transparency** makes the members of a particular group see-through.

All of this works while the `Tab` list is open and the effect is active, for players in your line of sight.

## AFK players

Many servers mark away players with an icon or `[AFK]` text. The AFK players setting:

- Show normally (default);
- Move to end: AFK players go to the end of the list. The vanilla list shows at most 80 entries, so with many players online the visible spots go to active players;
- Hide.

AFK icon codes: the server resource pack's AFK icon, as a code (`A416`, `U+A416`) or the symbol itself, several separated by spaces. Leave empty to recognise only text markers such as `[AFK]` and `zzz`.

## Join and leave notifications

- Join/leave notifications (on): "Tracked player … joined the server".
- Compact notification text (off): short `[+] Name`, `[-] Name`.
- Notification sounds (off): a sound on join and leave.

## Compact HUD

The "Compact HUD" button on the main screen. Used by the Separate compact list mode.

| Setting | Default | What it does |
|---|---|---|
| HUD position | bottom center | top left, top right, bottom left, bottom center, bottom right |
| HUD columns | 1 | number of columns |
| Maximum HUD rows | 20 | rows per column |
| HUD avatars | on | player heads |
| HUD ping | on | each player's ping |

## Import and export

- "Import .txt" adds names from a text file (one per line, up to 1 MiB).
- "Export profile" and "Import profile" save groups to a file and load them, to share with a friend, for example.

## All settings

The main settings screen:

| Section | Setting | Default |
|---|---|---|
| General | Enabled | on |
| General | Activation | hold either Shift |
| General | Case sensitive | off |
| Players in Tab | Untracked player transparency | off |
| Players in Tab | Transparent player opacity | 30% (15, 30, 50, 75 or 100%) |
| Players in Tab | Sorting speed | 5% (1, 2.5, 5, 10 or 20%) |
| Players in Tab | Within-group sorting | server order |
| Players in Tab | AFK players | show normally |
| Players in Tab | AFK icon in Tab | default icons |
| Players in Tab | Mode | animated sorting |
| Join and leave | Join/leave notifications | on |
| Join and leave | Compact notification text | off |
| Join and leave | Notification sounds | off |

## Files

`config/tab-name-dimmer.json` holds every setting and group. It is written atomically with a `.bak` backup, which the mod uses to restore both a damaged and a deleted file. Manual edits are picked up within about a second.

Example:

```json
{
  "schemaVersion": 2,
  "enabled": true,
  "dimOpacity": 0.3,
  "displayMode": "ANIMATED_SORT",
  "activationMode": "HOLD_SHIFT",
  "afkHandlingMode": "MOVE_TO_END",
  "globalProfile": {
    "name": "Global",
    "groups": [
      { "name": "Friends", "color": 5635925, "priority": 10, "enabled": true, "members": ["PlayerName", "Clan_*"] }
    ]
  }
}
```

## FAQ

**Nothing changes in the Tab list.** The effect works only while active: by default hold `Shift` with `Tab` open. The activation mode is in the settings.

**Friends have no outline.** Turn on Group outline for their group. The outline shows while `Tab` is open and the effect is active.

**Groups are different on one server.** That server has its own groups, which replace the global ones. Press "Use global fallback" to go back to the global groups.

**A player lands in the wrong group.** When a player matches several groups, the highest priority wins, and with equal priority the one higher in the list.

**AFK players are not recognised.** The server marks AFK with its own resource pack icon. Find its code and put it into AFK icon codes.

## Building from source

JDK 25 is required.

```powershell
git clone https://github.com/ServChan/tab-name-dimmer.git
cd tab-name-dimmer
.\gradlew.bat build
```

On Linux and macOS use `./gradlew`. The jar is `build/libs/tab-name-dimmer-1.2.3.jar`.
