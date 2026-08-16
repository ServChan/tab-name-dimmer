# Tab Name Dimmer

[![Minecraft Version](https://img.shields.io/badge/Minecraft-26.1.2%20%7C%2026.2-brightgreen?style=flat-square&logo=minecraft)](README.md)
[![Platform](https://img.shields.io/badge/Platform-Fabric-blue?style=flat-square&logo=fabric)](README.md)
[![Java Target](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)](README.md)
[![Mod Version](https://img.shields.io/badge/Version-1.1.0-purple?style=flat-square)](README.md)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

Organizes players into global or per-server groups, dimming, filtering, sorting, or outlining them in Tab list and HUD view.

## Русский

### Что это

`Tab Name Dimmer` — клиентский Fabric-мод для Minecraft 26.1.2–26.2, позволяющий группировать игроков в списке Tab (друзья, соклановцы, враги, нейтралы), затемнять неактивных игроков, сортировать и выделять контурами в игровом мире.

### Что дает мод

- кастомизацию списка Tab с разделением игроков на настраиваемые группы и цветовые теги;
- затемнение (dimming) или скрытие имён игроков вне белого списка;
- подсветку силуэтов союзников и врагов в игровом пространстве;
- раздельные списки контактов для каждого сервера и глобальный профиль.

### Особенности

- ограниченная карта сущностей (bounded entity map) с автоматической очисткой при выходе;
- исключение просадок FPS при рендере Tab на серверах с большим онлайном;
- безопасная перезапись конфигурации с созданием `.bak` резервной копии.

### Настройки

Файл настроек: `config/tabnamedimmer.json`. Графический интерфейс управления группами и цветами открывается через Mod Menu или назначенную горячую клавишу.

### Установка

1. Установите **Fabric Loader** 0.19.3+ и **Java 25**.
2. Поместите `tab-name-dimmer-1.1.0.jar` из `build/libs/` в папку `mods/`.

### Совместимость

- **Minecraft:** 26.1.2 – 26.2;
- **Fabric Loader:** 0.19.3+;
- **Java:** 25;
- **Сторона:** Клиент.

### Сборка

```powershell
.\gradlew.bat clean build --warning-mode all
.\gradlew.bat clean build '-Pminecraft_version=26.2' --warning-mode all
.\gradlew.bat clean build --warning-mode all
```

Итоговый файл: `build/libs/tab-name-dimmer-1.1.0.jar`.

---

## English

### What It Is

`Tab Name Dimmer` is a client-side Fabric mod for Minecraft 26.1.2–26.2 that organizes tracked players into global or per-server groups, then dims, filters, sorts, outlines, or displays them in a configurable Tab list and HUD view.

### What It Adds

- Tab list categorization with customizable player groups (allies, clanmates, enemies, neutrals);
- dimming or filtering out untracked player names in crowded server lobbies;
- world glowing outlines for marked group members;
- per-server and global player directory profiles.

### Features

- bounded entity tracking map automatically cleared on disconnects;
- 0.0 ms Tab rendering overhead optimized for high player-count servers;
- atomic configuration persistence with `.bak` recovery fallback.

### Settings

Configured via `config/tabnamedimmer.json`. Group management GUI accessible through Mod Menu or assigned keybinding.

### Installation

1. Install **Fabric Loader** 0.19.3+ with **Java 25**.
2. Place `tab-name-dimmer-1.1.0.jar` from `build/libs/` into `.minecraft/mods`.

### Compatibility

- **Minecraft:** 26.1.2 – 26.2 (single JAR);
- **Fabric Loader:** 0.19.3+;
- **Java:** 25;
- **Environment:** Client-only.

### Build

```powershell
.\gradlew.bat clean build --warning-mode all
.\gradlew.bat clean build '-Pminecraft_version=26.2' --warning-mode all
.\gradlew.bat clean build --warning-mode all
```

Output: `build/libs/tab-name-dimmer-1.1.0.jar`.

## Credits

Developed by LTS_Server. Licensed under the MIT License.
