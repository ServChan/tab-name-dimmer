# Tab Name Dimmer

[![Minecraft Version](https://img.shields.io/badge/Minecraft-26.3-brightgreen?style=flat-square&logo=minecraft)](README.md)
[![Platform](https://img.shields.io/badge/Platform-Fabric-blue?style=flat-square&logo=fabric)](README.md)
[![Java Target](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)](README.md)
[![Mod Version](https://img.shields.io/badge/Version-1.2.0-purple?style=flat-square)](README.md)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

Client-side Fabric mod that organizes tracked players into global or per-server groups, then dims, filters, sorts, outlines, or shows them in a configurable Tab/HUD view, with an AFK detector and a line-of-sight cache.

## Русский

### Что это

`Tab Name Dimmer` (mod id `tabnamedimmer`) — клиентский Fabric-мод для Minecraft 26.3, настраивающий отображение игроков в списке Tab и на HUD: группы (друзья, соклановцы, враги, модераторы), затемнение незнакомцев, фильтрация списка и подсветка силуэтов в мире.

### Возможности

- **Группировка игроков** по категориям с отдельными цветами и приоритетами;
- **Затемнение**: приглушает яркость имён игроков не из вашего списка отслеживания;
- **3 режима отображения**: плавная сортировка Tab, фильтр только отслеживаемых игроков и отдельный настраиваемый HUD;
- **Подсветка силуэтов в мире**: цветное свечение соклановцев/союзников в пределах видимости;
- **Разделение по серверам**: индивидуальные списки групп для каждого сервера и глобальный список друзей;
- **Уведомления**: полные или краткие `[+]/[-]`, раздельно включаемые звуки входа/выхода;
- **AFK-фильтр**: серверные AFK-маркеры можно оставить, переместить в конец списка или скрыть (ваниль показывает не более 80 записей, поэтому режим «В конец» при большом онлайне отдаёт видимые места активным игрокам);
- ограниченная карта сущностей против утечек памяти; без дискового I/O в рендере — конфиг обновляется на клиентском тике, render-пути читают готовый снимок; атомарное сохранение.

### Управление

| Действие | Клавиша по умолчанию | Переназначение |
|---|---|---|
| Активировать Tab Name Dimmer | удержание `Shift` (режим настраивается); назначаемая клавиша — `Left Alt` | Options → Controls → Tab Name Dimmer |

Режим активации (удержание `Shift`, удержание клавиши или переключение) выбирается в настройках. Экран настроек — через **Mod Menu**.

### Настройки

Экран настроек — через **Mod Menu** (цвета, группы, режимы, HUD, уведомления). Файл `config/tabnamedimmer.json` (`schemaVersion` 2), атомарная запись с защитой от повреждений:

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
      { "name": "Friends", "color": 5635925, "priority": 10, "enabled": true, "members": ["PlayerName"] }
    ]
  }
}
```

### Установка

1. **Fabric Loader** `0.19.3+` и **Fabric API** `0.161.0+26.3`.
2. Скопируйте `tab-name-dimmer-1.2.0.jar` из `build/libs/` в папку `mods/`.
3. **Mod Menu** `21.0.0` — опционально, открывает экран настроек.

**Требования:** Minecraft `26.3` · Java `25` · только клиент.

### Сборка

```powershell
.\gradlew.bat clean build
```

Готовый JAR: `build/libs/tab-name-dimmer-1.2.0.jar`.

---

## English

### What It Is

`Tab Name Dimmer` (mod id `tabnamedimmer`) is a client-side Fabric mod for Minecraft 26.3 that configures how players appear in the Tab list and HUD: groups (allies, clanmates, rivals, staff), dimming of strangers, list filtering, and world outlines.

### Features

- **Player grouping** into categories with colors and priorities;
- **Dimming**: softens the names of players not on your tracking list;
- **3 display modes**: animated Tab sorting, tracked-player filtering, and a separate configurable HUD;
- **World outlines**: color-coded glow on clanmates/allies within line of sight;
- **Server isolation**: dedicated group lists per server plus a global friends list;
- **Notifications**: full or compact `[+]/[-]` messages with independently toggled join/leave sounds;
- **AFK handling**: keep server-marked AFK players in place, move them to the end, or hide them (vanilla shows at most 80 rows, so "move to end" hands visible slots to active players on a busy server);
- a bounded entity map against memory leaks; no config disk I/O in render paths — the config updates on the client tick and render reads a prepared snapshot; atomic saves.

### Controls

| Action | Default key | Rebind |
|---|---|---|
| Activate Tab Name Dimmer | hold `Shift` (mode configurable); assignable key is `Left Alt` | Options → Controls → Tab Name Dimmer |

The activation mode (hold `Shift`, hold key, or toggle) is chosen in settings. The settings screen opens through **Mod Menu**.

### Configuration

The settings screen opens through **Mod Menu** (colors, groups, modes, HUD, notifications). `config/tabnamedimmer.json` (`schemaVersion` 2), atomic corruption-safe write — see the JSON above.

### Installation

1. **Fabric Loader** `0.19.3+` and **Fabric API** `0.161.0+26.3`.
2. Copy `tab-name-dimmer-1.2.0.jar` from `build/libs/` into `mods/`.
3. **Mod Menu** `21.0.0` — optional, opens the settings screen.

**Requirements:** Minecraft `26.3` · Java `25` · client-only.

### Building

```powershell
.\gradlew.bat clean build
```

Output JAR: `build/libs/tab-name-dimmer-1.2.0.jar`.

## Лицензия / License

[MIT](LICENSE). Developed by LTS_Server.
