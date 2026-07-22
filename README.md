<p align="center">
  <img src="assets/logo.svg" alt="Pixel E-Book Reader logo" width="120" />
</p>

<h1 align="center">Pixel E-Book Reader</h1>

A modern e-book reader for Android, visually aligned with first-party Google Pixel apps
(Material 3 Expressive). Everything it reads stays on your device — no accounts, no network
calls, no analytics.

Reads 7 file formats and adds reading statistics, local backup & restore, richer book metadata,
and text-to-speech.

## Features

- **7 supported file formats** — PDF, EPUB, FB2, TXT, HTML, HTM, and Markdown, all imported
  locally via the Storage Access Framework.
- **Organized library** — custom categories, grid/list layout, multi-select, search, and a
  chronological reading history.
- **Deep reader customization** — typography, layout, reading color presets independent of the
  app theme, a chapters/table-of-contents drawer, scroll checkpoints, and focus aids (reading
  ruler, perception expander, highlighted reading).
- **Material 3 Expressive design** — light/dark mode, dynamic color (Material You), 16+ curated
  themes with adjustable contrast, pure/absolute dark modes.
- **Reading statistics** — time spent reading, streaks, and books finished.
- **Text-to-speech** — in-app playback of book content with synced highlighting.
- **Local backup & restore** — export/import your library and settings to a file, no account
  required.
- **Richer book metadata** — series, tags, rating, ISBN, and publish date.
- No accounts, no backend, no analytics or crash-reporting SDKs.

## Prerequisites

- Android Studio (Narwhal or newer): https://developer.android.com/studio
- JDK 17+ (bundled with Android Studio)
- An Android device or emulator running API 26 (Android 8.0) or newer

## Build & run

```
git clone https://github.com/wwwescape/pixel-e-book-reader.git
cd pixel-e-book-reader
```

Open the project in Android Studio and run the `app` configuration, or from the command line:

```
./gradlew installDebug
```

## Test

```
./gradlew lint testDebugUnitTest connectedDebugAndroidTest
```

`connectedDebugAndroidTest` needs a connected device or running emulator.

## Release a new version

```
git tag v0.1.0
git push origin v0.1.0
```

That tag push builds a signed release APK and AAB and attaches them to an auto-generated
GitHub Release. See `.github/workflows/release.yml`; it needs the `KEYSTORE_BASE64`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` repository secrets set (see
`keystore.properties`, which is gitignored and holds these locally).

## Project layout

```
app/       Kotlin, Jetpack Compose (Material 3), Room + DataStore, single module
design/    Source logo and Play Store icon assets
assets/    README/repo assets
```

## Privacy

Pixel E-Book Reader collects nothing — everything you import and read stays on your device.
Once built, the full breakdown will be available in-app under Settings → About.

## License

GPL-3.0 — see `LICENSE`.

## Support

If you find Pixel E-Book Reader useful, consider buying me a coffee:

[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" height="40" />](https://buymeacoffee.com/wwwescape)
