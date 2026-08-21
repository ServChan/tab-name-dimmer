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

- **Группировка игроков**: Разделение участников сервера по категориям с отдельными цветами и приоритетами.
- **Затемнение (Dimming)**: Приглушает яркость имён игроков, не входящих в ваш список отслеживания, позволяя мгновенно замечать важных игроков.
- **3 режима отображения**: плавная сортировка списка Tab, фильтр только отслеживаемых игроков и отдельный настраиваемый HUD.
- **Подсветка силуэтов в мире**: Выделяет соклановцев или союзников цветным свечением в пределах видимости.
- **Разделение по серверам**: Индивидуальные списки групп для каждого сервера и глобальный список друзей.
- **Уведомления**: полные или краткие сообщения `[+]/[-]`, а также отдельно включаемые звуки входа и выхода.
- **AFK-фильтр**: серверные AFK-маркеры можно оставить как есть, переместить в конец списка или скрыть при активации мода. Ваниль показывает не более 80 записей, поэтому режим «В конец» при большом онлайне отдаёт видимые места активным игрокам.

### Особенности и архитектура

- **Ограниченная карта сущностей (Bounded Map)**: Предотвращает утечки памяти на серверах с частой сменой игроков.
- **Без дискового I/O в рендере**: конфигурация обновляется на клиентском тике, а render-пути читают готовый снимок.
- **Атомарное сохранение**: Настройки сохраняются в `config/tabnamedimmer.json` с защитой от повреждений.

### Управление

- Удерживайте `Shift` по умолчанию, чтобы активировать эффекты мода; режим можно сменить на удержание или переключение назначаемой клавиши (`Left Alt` по умолчанию).
- Откройте настройки через Mod Menu, чтобы управлять цветами, группами, режимами, HUD и уведомлениями.

### Настройки (`config/tabnamedimmer.json`)

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
      {
        "name": "Friends",
        "color": 5635925,
        "priority": 10,
        "enabled": true,
        "members": ["PlayerName"]
      }
    ]
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

- **Player Group Categorization**: Assign custom groups (allies, clanmates, rivals, staff) with colors and priorities.
- **Visual Dimming**: Softens untracked player names in crowded tab lists, bringing prioritized players to immediate focus.
- **3 Display Modes**: animated Tab sorting, tracked-player filtering, and a configurable extra HUD.
- **World Glowing Outlines**: Renders soft color-coded glowing halos around marked group members.
- **Server Isolation**: Dedicated contact lists per server IP as well as global friend registries.
- **Notifications**: full or compact `[+]/[-]` messages with independently configurable join/leave sounds.
- **AFK Handling**: keep server-marked AFK players in place, move them behind active players, or hide them while the mod is active.

### Features

- bounded entity tracking map automatically flushed on disconnects;
- no configuration disk I/O in render paths; client ticks prepare the state consumed by rendering;
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
