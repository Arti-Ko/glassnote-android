# Glassnote — Android

Диктофон-архив голосовых заметок. Порт macOS-версии (`~/Documents/glassnote`)
с тем же форматом хранения, чтобы заметки были sync-ready между платформами.

## Что уже есть (v0.1 scaffold)

- **Запись**: `MediaRecorder` → `audio.m4a` (AAC mono 48kHz) — формат как на macOS.
- **Хранилище**: заметка = папка `yyyy-MM-dd_HH-mm-ss/` с `audio.m4a + note.json +
  transcript.md`. Схема `note.json` 1:1 с macOS (id, createdAt ISO8601, durationSec,
  language, model, title, edited, segments[start,end,text,speaker]).
- **Quick Settings тайл** «Быстрая заметка» (`QuickRecordTileService`) — открывает
  плашку записи, в т.ч. с экрана блокировки (`unlockAndRun` / `startActivityAndCollapse`).
- **Стеклянная плашка** снизу (`RecordActivity` + `RecordingPanelSheet`), показывается
  поверх локскрина (`showWhenLocked`), с волной/таймером/стоп/свернуть.
- **Foreground-сервис** (`RecordingService`, type=microphone) — запись переживает
  блокировку и сворачивание.
- **Главное окно** (Compose): список, поиск (подстрока), деталь с правкой текста
  и автосохранением, копирование в Markdown, удаление.
- **Настройки**: принудительный язык записи (auto/ru/en) — как на macOS.

## Что заглушено

- **Транскрипция**: `PendingTranscriber` возвращает пустой результат. Записи
  сохраняются и архивируются уже сейчас; текст появится после интеграции whisper.cpp.

## Следующие шаги

1. **whisper.cpp через JNI**: добавить нативный модуль (`externalNativeBuild` +
   CMake, NDK), собрать `libwhisper`, реализовать `WhisperBridge : Transcriber`,
   передавать `languageCode` из `Settings`. Модель ggml (например `ggml-small`/`medium`
   для ru) скачивать в `filesDir` при первом запуске.
2. **Gradle wrapper**: открыть проект в Android Studio (оно дозагрузит wrapper-jar),
   либо `gradle wrapper` при установленном Gradle.
3. **Поиск**: заменить подстроку на Room FTS4 (паритет с SQLite FTS5 на macOS).
4. **Диаризация** (v2): поле `speaker` в схеме уже заложено.
5. **Синхронизация**: формат папок готов под общий синк (SAF/MediaStore/Syncthing).

## Сборка

```bash
# открыть в Android Studio (рекомендуется) — настроит SDK/wrapper автоматически,
# либо вручную при наличии Gradle:
cd ~/Documents/glassnote-android
gradle wrapper
./gradlew assembleDebug
```
Требуется Android SDK (compileSdk 34), minSdk 26.
