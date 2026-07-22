<p align="center">
  <img src="assets/logo.svg" alt="Pixel E-Book Reader logo" width="120" />
</p>

<h1 align="center">Pixel E-Book Reader</h1>

<p align="center">
  A modern e-book reader for Android, built with Material 3 Expressive to feel at home alongside
  Google's own Pixel apps. Everything you import and read stays on your device: no accounts, no
  internet permission, no analytics.
</p>

<p align="center">
  <a href="https://github.com/wwwescape/pixel-e-book-reader/releases"><img src="https://img.shields.io/github/v/release/wwwescape/pixel-e-book-reader.svg?style=flat-square" alt="GitHub release" /></a>
  <a href="https://github.com/wwwescape/pixel-e-book-reader/commits/master"><img src="https://img.shields.io/github/last-commit/wwwescape/pixel-e-book-reader.svg?style=flat-square" alt="GitHub last commit" /></a>
  <a href="https://github.com/wwwescape/pixel-e-book-reader"><img src="https://img.shields.io/github/languages/code-size/wwwescape/pixel-e-book-reader.svg?color=red&style=flat-square" alt="GitHub code size" /></a>
</p>

## Features

- **7 file formats** — PDF, EPUB, FB2, TXT, HTML, HTM, and Markdown.
- **PDF page mode** — pinch-to-zoom, pan, and an optional page-curl animation.
- **Bookmarks and highlights** — bookmarks and 5-color text highlights.
- **Text-to-speech** — built-in read-aloud with synchronized highlighting.
- **Word translation** — double-tap a word to look it up in your preferred dictionary or
  translation app.
- **Library** — custom categories with manual reordering, per-category sorting, grid or list
  layouts, search, multi-select, and reading history.
- **Rich metadata** — series, tags, rating, ISBN, and publish date.
- **Reader customization** — typography and layout controls, reading color presets independent
  of the app theme, a chapters/table-of-contents drawer, scroll checkpoints, and focus aids
  (Reading Ruler, Perception Expander, and Highlighted Reading).
- **Reading statistics** — time spent reading, reading streaks, and books finished.
- **Local backup & restore** — export your library and settings to a file and restore them
  later, with no account required.
- **Material 3 Expressive design** — light, dark, and system themes, dynamic color (Material
  You), 16+ curated color themes, adjustable contrast, and Pure Black / Absolute Black modes.
- **Languages** — English, Spanish, French, Hindi, and Portuguese.

## Installation

Download the APK from the [latest release](https://github.com/wwwescape/pixel-e-book-reader/releases/latest)
and install it on your device.

Requires Android 8.0 (API 26) or newer.

## Privacy

Pixel E-Book Reader collects nothing. It has no internet permission, no accounts, no analytics, and no
crash-reporting SDKs. Books are imported through Android's Storage Access Framework, so no broad
storage permission is needed, and your library, reading progress, bookmarks, highlights, and
settings never leave your device. See the in-app Privacy Policy (**Settings → About**) for the
full breakdown.

## Development

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (Narwhal or newer)
- JDK 17+ (bundled with Android Studio)
- An Android device or emulator running Android 8.0 (API 26) or newer

### Build & run

```bash
git clone https://github.com/wwwescape/pixel-e-book-reader.git
cd pixel-e-book-reader
./gradlew installDebug
```

Or open the project in Android Studio and run the `app` configuration.

### Test

```bash
./gradlew lint testDebugUnitTest
```

### Release a new version

Bump `versionCode` and `versionName` in `app/build.gradle.kts`, commit, then:

```bash
git tag v0.1.0
git push origin v0.1.0
```

The tag push builds a signed release APK and AAB and attaches them to a new GitHub Release (see
`.github/workflows/release.yml`). It needs the `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, and `KEY_PASSWORD` repository secrets, which are kept locally in the gitignored
`keystore.properties`.

### Project layout

```
app/       Kotlin, Jetpack Compose (Material 3), Room + DataStore, single module
design/    Source logo and Play Store icon assets
assets/    README assets
```

## License

GPL-3.0 — see [LICENSE](LICENSE).

## Support

If you find Pixel E-Book Reader useful, consider buying me a coffee:

[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" height="40" />](https://buymeacoffee.com/wwwescape)
