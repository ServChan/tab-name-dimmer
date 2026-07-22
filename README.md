# Tab Name Dimmer

Client-side Fabric mod for Minecraft that makes the Tab player list easier to scan on large servers by dimming, filtering, sorting, or separately showing selected player names while Shift is held.

## Русский

### Что это

Tab Name Dimmer это клиентский Fabric-мод для Minecraft, который помогает быстро находить нужных игроков в списке Tab на больших серверах, где полный онлайн уже не помещается на экране.

Мод работает от удержания `Shift`: когда открыт список игроков, он выделяет ники из вашего списка исключений, а остальных игроков затемняет, скрывает или оставляет ниже в списке в зависимости от выбранного режима.

Основной сценарий - большие серверы, где нужно быстро понять, кто из вашего клана, команды или списка друзей сейчас онлайн, даже если обычный Tab переполнен и не показывает всех игроков сразу.

Он полезен в тех случаях, когда:
- на сервере много игроков и нужные ники сложно найти глазами;
- онлайн настолько большой, что обычный Tab не вмещает весь список игроков;
- нужно видеть состав своего клана, команды или группы прямо во время игры;
- нужно быстро отслеживать друзей, участников команды, администрацию или важных игроков;
- хочется оставить обычный Tab чистым, но иметь быстрый способ сфокусироваться на выбранных никах;
- нужно импортировать список игроков из заранее подготовленного `.txt` файла.

### Что дает мод

Tab Name Dimmer добавляет к стандартному Tab-списку Minecraft:
- затемнение всех ников, которых нет в списке исключений;
- список ников, которые остаются яркими и заметными;
- несколько режимов отображения для разных сценариев;
- настройку цвета затемнения;
- чувствительность или нечувствительность к регистру;
- импорт ников из `.txt`;
- интеграцию с Mod Menu для настройки через интерфейс.

### Режимы отображения

Мод поддерживает три режима:
- `ANIMATED_SORT`: выбранные игроки плавно поднимаются вверх списка при удержании `Shift`;
- `FILTER`: при удержании `Shift` в Tab остаются только игроки из списка исключений;
- `EXTRA_HUD`: при удержании `Shift` выбранные игроки дополнительно показываются в отдельном небольшом HUD-блоке.

Во всех режимах ники, которых нет в списке исключений, могут затемняться выбранным цветом.

### Настройки

Открыть настройки можно через [Mod Menu](https://modrinth.com/mod/modmenu), если он установлен.

В меню доступны:
- `Enabled`: включает или выключает мод;
- `Case sensitive`: учитывает регистр букв при сравнении ников;
- `Mode`: переключает режим отображения;
- `Dim color`: задает цвет затемнения в формате `#RRGGBB`;
- `Names that stay bright`: список ников, которые не нужно затемнять;
- `Import .txt`: импортирует ники из текстового файла.

Файл конфигурации:
- `config/tab-name-dimmer.json`

Формат списка ников:
- ники можно вводить отдельными строками;
- также поддерживаются разделители `,` и `;`;
- пустые строки игнорируются;
- повторяющиеся ники удаляются при сохранении.

### Установка

Для работы нужны:
- [Fabric Loader](https://fabricmc.net/use/installer/) `0.19.3+`
- Minecraft `26.1.2`-`26.2` (one JAR)
- Java `25+`

Рекомендуется:
- [Mod Menu](https://modrinth.com/mod/modmenu) для настройки через GUI

Важно:
- мод полностью клиентский;
- Fabric API не требуется, если он не нужен другим вашим модам;
- для работы затемнения удерживайте `Shift` при открытом Tab-списке.

### Совместимость

- Minecraft `26.1.2`-`26.2`
- Java `25`
- Fabric Loader `0.19.3`
- Mod Menu `18.0.0-alpha.8` опционально
- Текущая версия мода в проекте: `1.0.3`

### Сборка

Требования:
- JDK 25

Команда сборки:
```bash
./gradlew clean build
```

Для Windows:
```bat
gradlew.bat clean build
```

Результат:
- `build/libs/*.jar`

## English

### What It Is

Tab Name Dimmer is a client-side Fabric mod for Minecraft that makes it easier to find important players in the Tab player list on large servers where the full online player list no longer fits on screen.

The mod is activated by holding `Shift`: while the player list is open, it highlights the names from your allowlist and dims, hides, or moves other players depending on the selected display mode.

The main use case is large multiplayer servers where you want to quickly see which clan members, teammates, or friends are online, even when the normal Tab list is too crowded to show everyone at once.

It is useful when:
- a server has many online players and important names are hard to spot;
- the online player count is larger than the regular Tab list can comfortably display;
- you need to see your clan, team, or group roster while playing;
- you need to track friends, teammates, staff members, or specific players quickly;
- you want to keep the normal Tab list unchanged until you explicitly hold `Shift`;
- you want to import a prepared player list from a `.txt` file.

### What It Adds

Tab Name Dimmer extends the standard Minecraft player list with:
- dimming for every name that is not on your allowlist;
- a list of names that stay bright and easy to spot;
- several display modes for different workflows;
- configurable dim color;
- optional case-sensitive matching;
- `.txt` name import;
- Mod Menu integration for in-game configuration.

### Display Modes

The mod supports three modes:
- `ANIMATED_SORT`: allowlisted players smoothly move to the top of the list while `Shift` is held;
- `FILTER`: only allowlisted players remain visible in Tab while `Shift` is held;
- `EXTRA_HUD`: allowlisted players are additionally shown in a small separate HUD block while `Shift` is held.

In all modes, names outside the allowlist can be dimmed with the configured color.

### Settings

The config screen is available through [Mod Menu](https://modrinth.com/mod/modmenu), if installed.

Available settings:
- `Enabled`: enables or disables the mod;
- `Case sensitive`: toggles case-sensitive name matching;
- `Mode`: switches the display mode;
- `Dim color`: sets the dim color in `#RRGGBB` format;
- `Names that stay bright`: names that should not be dimmed;
- `Import .txt`: imports names from a text file.

Config file:
- `config/tab-name-dimmer.json`

Name list format:
- names can be entered on separate lines;
- `,` and `;` separators are also supported;
- blank entries are ignored;
- duplicate names are removed on save.

### Installation

Required:
- [Fabric Loader](https://fabricmc.net/use/installer/) `0.19.3+`
- Minecraft `26.1.2`-`26.2` (one JAR)
- Java `25+`

Recommended:
- [Mod Menu](https://modrinth.com/mod/modmenu) for GUI configuration

Important:
- this is a fully client-side mod;
- Fabric API is not required unless another installed mod needs it;
- hold `Shift` while the Tab player list is open to activate the dimming behavior.

### Compatibility

- Minecraft `26.1.2`-`26.2`
- Java `25`
- Fabric Loader `0.19.3`
- Mod Menu `18.0.0-alpha.8` optional
- Current project mod version: `1.0.3`

### Build

Requirements:
- JDK 25

Build command:
```bash
./gradlew clean build
```

Windows:
```bat
gradlew.bat clean build
```

Output:
- `build/libs/*.jar`

### Persistence and verification

Settings are written to a sibling temporary file and atomically replace the main JSON where supported. A failed save keeps the screen open and reports the failure instead of showing the success toast. When the mod is disabled, the extra HUD, filtering, sorting, and glowing outlines are inactive.

The same sources were compiled against Minecraft 26.1.2 and 26.2 on 2026-07-22. Tab rendering with BetterTab and a populated live server list still requires an in-game compatibility pass.

The optional BetterTab compatibility mixin is now loaded only when Fabric reports that `bettertab` is installed, avoiding the previous missing-target warning in profiles without BetterTab.
