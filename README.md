# GamesLibrary (Android)

GamesLibrary is a lightweight Android catalog for tracking how games from other platforms run on Android through emulation.  
Browse supported titles, filter by platform, and quickly check compatibility on your device.  
You can also submit anonymous test results to keep your own testing history and help others find the best emulator settings.

## Features

- **Fast library browsing**  
  Smooth search, sorting, and favorites to keep the collection organized.

- **Platform filters**  
  Separate lists for **PS3**, **PC (Windows)**, and **Nintendo Switch** titles.

- **Game compatibility statuses**  
  Mark games as:
  - Working
  - Untested
  - Issues  
  Add notes, tested device/CPU, GPU driver, emulator app and version.

- **Anonymous results sharing**  
  Test results are synced to Firebase without accounts or personal data.

- **Offline-first**  
  Games list is loaded from bundled JSON and cached locally. Sync runs in background on demand.

## Tech stack

- Kotlin
- Jetpack Compose + Material 3
- Coil (image loading)
- Firebase Firestore (anonymous test results sync)

## Screens / flow

- Home screen with quick platform entry points  
- Games list with search, sorting, and favorites  
- Game card with cover, year/rating/platform badges, status, and test details  
- Test history per game with local + remote results

## Project setup

1. Clone the repo:
   ```bash
   git clone https://github.com/<sashkinbro>/GamesLibrary-Android.git
2. Open in Android Studio.
3. Add Firebase config:

- Create a Firebase project

- Enable Cloud Firestore

- Put google-services.json into /app

- Make sure Firestore rules allow anonymous write/read if you want public sharing

4. Run on a device or emulator (Android 7.0+ recommended).

## Data source

The internal games database is bundled with the app (JSON in assets).
Remote tests are stored per game ID in Firestore and merged into local history on sync.

## Notes

- This project does not include any game ROMs or copyrighted content.
Only public metadata (title, cover, year, platform, etc.) is used.

- Emulator names are mentioned for convenience and are not affiliated with this project.

## Backup/export test history

- More platforms (PS2 / Wii / etc.)

- Better per-game filters (FPS, resolution, settings presets)

## Contributing

Pull requests and issue reports are welcome.
If you add a new platform filter or data source, keep the UI minimal and consistent.
