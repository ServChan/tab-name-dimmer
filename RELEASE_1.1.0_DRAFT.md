# Tab Name Dimmer 1.1.0 — release draft

Статус: **не публиковать до закрытия release blockers**.

## Основа текста обновления

Последний опубликованный тег: `1.0.4` (`1913ff0`). Текущий `origin/main` — `3a04312`, локальная ветка `main` на `15cda67` опережает её на 12 коммитов. Для changelog использовать диапазон `1.0.4..HEAD`: группы и серверные профили (`8d8e6bd`), защита конфигурации и ограничение кэша прозрачности (`c05e352`), устранение сбоя цвета имени (`dfab9d6`), AFK/LoS/отслеживание игроков (`313a18e`), переработка экрана настроек (`b47d39d`, `c5b1fa3`). После коммитов остаются незакоммиченные изменения README, иконки и очистки комментариев; перед созданием тега нужно ещё раз сверить текст с итоговым деревом.

## Решение по релизу

- Версия: `1.1.0`. Группы, профили серверов, новые режимы активации, AFK-фильтр и отдельные настройки HUD меняют модель работы мода; это минорный релиз, а не патч `1.0.x`.
- Git tag: `1.1.0`.
- GitHub release title: `26.1.2–26.2 [1.1.0]`.
- Modrinth version name: `Tab Name Dimmer 1.1.0`.
- Version number: `1.1.0`.
- Version type: `Release`.
- Loaders: `Fabric`.
- Game versions: `26.1.2`, `26.2`.
- Environment: client-side.
- GitHub assets: `tab-name-dimmer-1.1.0.jar` and `tab-name-dimmer-1.1.0-sources.jar`.
- Modrinth file: only `tab-name-dimmer-1.1.0.jar`, marked as primary.

Both public project summaries still describe the old allowlist-only version. Update them with the release:

> Client-side Fabric mod for grouping, filtering, sorting, and highlighting players in the Minecraft Tab list and HUD.

The main Modrinth description should also be replaced with the current English README section. The present page still treats hold-Shift and `allowedNames` as the primary workflow.

## Release blockers

1. Restore a valid `.bak` before creating a default config when the main config is missing. Add tests for missing main, corrupt main, valid backup, and no files.
2. Preserve group name, color, and member drafts across window resize/fullscreen changes. Test the return path from the online-player picker.

Strongly recommended before release because groups are the main 1.1.0 feature:

- make the currently active profile distinct from the profile being edited;
- make the nested `Done`/back behavior and the final `Save` boundary explicit and consistent.

## Final verification

1. Run `clean check build` for Minecraft 26.1.2.
2. Run `clean check build "-Pminecraft_version=26.2"`.
3. Run the 26.1.2 build once more so `build/libs` contains the primary distributable.
4. Check `fabric.mod.json`, the version badge, JAR names, translations, and `git diff --check`.
5. Perform one fresh-launch smoke test with the release JAR:
   - migrate an existing 1.0.x allowlist to the schema-v2 global group;
   - create, edit, save, and reload global and server-specific groups;
   - resize the window while editing a group and its members;
   - check hold-Shift, hold-key, and toggle-key activation;
   - check animated sort, filter, and separate HUD modes;
   - check all three AFK modes against a formatted player-list objective;
   - check join/leave messages, compact text, and optional sounds;
   - check name colors and world outlines with the normal Everything Voxy stack;
   - reconnect and change dimension; confirm that stale state is cleared.
6. Commit the intended README/icon changes. Keep audit/architecture files out of the release commit unless they are intentionally public.
7. Push the release commit to `main`, tag that exact commit as `1.1.0`, then publish GitHub first and Modrinth second.
8. Compare the SHA-256 of the local primary JAR, the GitHub asset, and the Modrinth file.

Recommended Modrinth gallery update:

- the main settings screen with the three activation/display controls visible;
- the group editor with two clearly different groups;
- the separate HUD in a real multiplayer Tab-list scenario.

## GitHub release body

```markdown
## Русский

Tab Name Dimmer 1.1.0 заменяет единый список отслеживаемых игроков группами и профилями серверов, добавляет обработку AFK-меток и расширяет управление списком Tab и отдельным HUD.

### Что нового

- Группы игроков со своими цветами, приоритетами, подсветкой имён, прозрачностью моделей и свечением в мире.
- Глобальный профиль и отдельные профили для серверов.
- Импорт и экспорт профилей, а также быстрое добавление игроков из текущего онлайна.
- Три способа активации: удержание Shift, удержание назначенной клавиши или переключение клавишей.
- Обработка AFK-игроков: оставить на месте, переместить в конец списка или скрыть.
- Настраиваемый отдельный HUD: позиция, число строк и столбцов, аватары и пинг.
- Полные или компактные уведомления о входе и выходе отслеживаемых игроков; звуки включаются отдельно.
- Сортировка игроков внутри групп.

### Надёжность и совместимость

- Конфигурация обновляется на клиентском тике; рендер не читает файл с диска.
- Ограничены кэши прозрачности и проверки видимости; состояние очищается при отключении и смене мира.
- Исправлен сбой при выключенной перекраске имён группы.
- Старый список `allowedNames` автоматически переносится в глобальную группу при первом запуске.
- Один JAR работает с Minecraft `26.1.2` и `26.2`.

### Требования

- Fabric Loader `0.19.3+`
- Fabric API
- Java `25+`
- Mod Menu — опционально, для экрана настроек

Мод работает только на клиенте. Перед обновлением удалите старый JAR из папки `mods`; файл настроек сохранять можно.

---

## English

Tab Name Dimmer 1.1.0 replaces the single tracked-player list with groups and per-server profiles, adds AFK handling, and expands control over the Tab list and separate HUD.

### New

- Player groups with their own colors, priorities, name styling, model transparency, and world outlines.
- A global profile plus optional profiles for individual servers.
- Profile import/export and quick player selection from the current online list.
- Three activation methods: hold Shift, hold a custom key, or toggle with a custom key.
- AFK handling: keep marked players in place, move them to the end, or hide them.
- A configurable separate HUD with position, row/column limits, avatars, and ping.
- Full or compact join/leave notifications for tracked players, with separately controlled sounds.
- Within-group player sorting.

### Reliability and compatibility

- Configuration polling now runs on the client tick; render paths no longer read the config file from disk.
- Transparency and line-of-sight caches are bounded, and session state is cleared on disconnect and world changes.
- Fixed a crash when group name coloring was disabled.
- Existing `allowedNames` entries are migrated to a global group on first launch.
- One JAR supports Minecraft `26.1.2` and `26.2`.

### Requirements

- Fabric Loader `0.19.3+`
- Fabric API
- Java `25+`
- Mod Menu is optional and provides the settings screen

This is a client-side mod. Remove the previous JAR from `mods` before updating; the existing configuration file can stay in place.
```

## Modrinth changelog

```markdown
Tab Name Dimmer 1.1.0 replaces the single tracked-player list with groups and per-server profiles, adds AFK handling, and expands the separate HUD.

### New

- Player groups with separate colors, priorities, name styling, model transparency, and world outlines.
- Global and per-server profiles, profile import/export, and quick selection from online players.
- Hold-Shift, hold-key, and toggle-key activation modes.
- AFK handling: show normally, move to the end, or hide.
- Configurable HUD position, rows, columns, avatars, and ping.
- Full or compact join/leave notifications with optional sounds.
- Within-group sorting.

### Fixed and changed

- Existing `allowedNames` entries migrate to a global group automatically.
- Config file checks were moved out of render paths.
- Transparency and line-of-sight caches are bounded and cleared with session state.
- Fixed a crash when group name coloring was disabled.

One JAR supports Minecraft `26.1.2` and `26.2`. Requires Fabric Loader `0.19.3+`, Fabric API, and Java `25+`. Mod Menu is optional. Client-side only.
```

## Publication note

Do not describe the release as runtime-verified until the fresh-launch checklist above has passed. Build and unit-test success alone do not verify the Tab UI, AFK scoreboard integration, optional mod compatibility, sounds, or rendering behavior.
