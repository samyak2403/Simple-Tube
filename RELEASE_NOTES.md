# SimpleTune v0.2.2.2

## What's Changed

### 🔍 Search Fixes & Enhancements
* **Complete Search Results Restored**: Fixed an issue where searches (like `"mama.cha.gavala jau.ya"`) would only show a single top card result instead of the complete list of matching songs, albums, and artists.
* **YouTube `itemSectionRenderer` Support**: Added parsing support for YouTube Music's individual item section renderers, ensuring all 20+ search result items are extracted and displayed.
* **Search Query Spaces (`+` Bug)**: Fixed the bug where multi-word search queries had spaces converted to `+` symbols causing endless loading.
* **Search Deserialization Safety**: Fixed "No network connection" errors caused by `kotlinx.serialization.MissingFieldException` when YouTube omits optional header, thumbnail, or navigation fields.
* **Retry & Error States**: Added user-friendly retry buttons and loading indicators for search summaries.

### 🎵 Playback & Audio Focus Stability
* **Audio Focus Crash Resolved**: Fixed app crashes occurring when another app took audio focus (e.g. phone calls, notifications, or other media players).
* **Safe Foreground Service Lifecycle**: Wrapped foreground service stops in safe detachment calls (`STOP_FOREGROUND_DETACH` / `STOP_FOREGROUND_REMOVE`).
* **Foreground Service Exception Guards**: Added protection against `ForegroundServiceStartNotAllowedException` on newer Android OS versions.
* **Broadcast Intent Safety**: Protected audio session broadcast intents from security exceptions.
* **Timeline Bounds Checks**: Added checks to prevent IndexOutOfBounds exceptions during media transitions.

---

## Downloads
* **Universal Release APK**: `SimpleTune-0.2.2.2-universal-release.apk`
* **Version Code**: `6`
* **Version Name**: `0.2.2.2`
