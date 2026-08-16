# Tab Name Dimmer

[![Minecraft Version](https://img.shields.io/badge/Minecraft-26.1.2%20%7C%2026.2-brightgreen?style=flat-square&logo=minecraft)](README.md)
[![Platform](https://img.shields.io/badge/Platform-Fabric-blue?style=flat-square&logo=fabric)](README.md)
[![Java Target](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)](README.md)
[![Mod Version](https://img.shields.io/badge/Version-1.1.0-purple?style=flat-square)](README.md)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

Organizes players into global or per-server groups, dimming, filtering, sorting, or outlining them in Tab list and HUD view.

## Русский

### Что это

`Tab Name Dimmer` — клиентский Fabric-мод для Minecraft 26.1.2–26.2, позволяющий настраивать отображение игроков в списке Tab: распределять игроков по группам (друзья, соклановцы, враги, модераторы), затемнять незнакомцев, фильтровать список и выделять силуэты игроков в мире.

### Что дает мод

- **Группировка игроков**: Разделение участников сервера по категориям с назначением цветовых тегов и префиксов.
- **Затемнение (Dimming)**: Приглушает яркость имён игроков, не входящих в ваш список отслеживания, позволяя мгновенно замечать важных игроков.
- **4 режима отображения Tab**: Classic (ванильный с цветами), Compact, Grouped (с разделением по блокам групп) и Filtered (только отслеживаемые).
- **Подсветка силуэтов в мире**: Выделяет соклановцев или союзников цветным свечением в пределах видимости.
- **Разделение по серверам**: Индивидуальные списки групп для каждого сервера и глобальный список друзей.

### Особенности и архитектура

- **Ограниченная карта сущностей (Bounded Map)**: Предотвращает утечки памяти на серверах с частой сменой игроков.
- **0.0 ms задержка рендера Tab**: Оптимизированный проход отрисовки списка игроков без пересчёта форматирования каждый кадр.
- **Атомарное сохранение**: Настройки сохраняются в `config/tabnamedimmer.json` с защитой от повреждений.

### Управление

- `Ctrl + Tab` — открыть быстрое меню назначения группы игроку;
- Меню Mod Menu — подробная настройка цветов, групп и режимов затемнения.

### Настройки (`config/tabnamedimmer.json`)

```json
{
  "enabled": true,
  "dimOpacity": 0.35,
  "displayMode": "GROUPED",
  "highlightWorldOutlines": true,
  "groups": {
    "allies": { "color": "0xFF55FF55", "priority": 1 },
    "clan": { "color": "0xFF55FFFF", "priority": 2 },
    "enemies": { "color": "0xFFFF5555", "priority": 3 }
  }
}
```

### Сборка

```powershell
.\gradlew.bat clean build --warning-mode all
.\gradlew.bat clean build '-Pminecraft_version=26.2' --warning-mode all
.\gradlew.bat clean build --warning-mode all
```

Итоговый JAR: `build/libs/tab-name-dimmer-1.1.0.jar`.

---

## English

### What It Is

`Tab Name Dimmer` is a client-side Fabric mod for Minecraft 26.1.2–26.2 that organizes players into global or per-server groups, then dims, filters, sorts, outlines, or displays them in a configurable Tab list and HUD view.

### Key Features

- **Player Group Categorization**: Assign custom group tags (allies, clanmates, rivals, staff) with colored labels.
- **Visual Dimming**: Softens untracked player names in crowded tab lists, bringing prioritized players to immediate focus.
- **4 Tab Layout Modes**: Classic, Compact, Grouped, and Filtered.
- **World Glowing Outlines**: Renders soft color-coded glowing halos around marked group members.
- **Server Isolation**: Dedicated contact lists per server IP as well as global friend registries.

### Features

- bounded entity tracking map automatically flushed on disconnects;
- 0.0 ms Tab rendering overhead optimized for high player-count servers;
- atomic configuration persistence with `.bak` recovery fallback.

### Build

```powershell
.\gradlew.bat clean build --warning-mode all
.\gradlew.bat clean build '-Pminecraft_version=26.2' --warning-mode all
.\gradlew.bat clean build --warning-mode all
```

Output: `build/libs/tab-name-dimmer-1.1.0.jar`.

## Credits

Developed by LTS_Server. Licensed under MIT.
