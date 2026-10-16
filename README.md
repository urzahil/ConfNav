# ConfNav

An Android app for finding your way around a conference. Load a schedule, see what's on each day, spot every venue on a map, and get walking directions to your next session.

Easily build and customise your agenda in the companion web tool, [ConfNav](https://urzahil.github.io/ConfNav/), then load the exported JSON file into the app.

## Features

- Agenda with day-by-day filtering and a "next event" pill that jumps straight to directions
- Map with colour-coded pins, with sessions grouped by location
- Add, edit and delete sessions directly in the app
- JSON import and export, either adding to your agenda or replacing it (re-importing a file never duplicates sessions)
- Everything is stored on the device, so your agenda is available offline

## Building

### Android app

1. Install Android Studio (JDK 17).
2. Copy `.env.example` to `.env` and set `MAPS_API_KEY` to your Google Maps key. Enable the Maps SDK for Android, Geocoding API and Places API for it.
   **Restrict the key** in Google Cloud (Android app package + SHA-1 fingerprint, and only those APIs) and set a quota cap: the key ends up in the APK.
3. Build and test:

   ```bash
   gradle assembleDebug
   gradle testDebugUnitTest lintDebug
   ```

Release builds are produced by the manual *Build ConfNav Release APK* workflow. It uploads the R8 `mapping.txt` next to the APK; keep it to de-obfuscate crash stack traces.

Room schemas are exported to `app/schemas/`. Commit the files that appear there after a build, and add an explicit `Migration` whenever the database version changes.

### Web builder (`docs/`)

```bash
cd docs
bun install     # commit the generated bun.lock for reproducible builds
bun run dev     # http://localhost:3000
bun run lint    # type-check
bun run build
```

Pushing changes under `docs/` to `main` deploys it to GitHub Pages.

## Sessions JSON format

Both tools read and write the same file. The machine-readable definition is [`docs/public/sessions.schema.json`](docs/public/sessions.schema.json). The root is an array of sessions, or an object `{ "sessions": [...] }`.

```json
[
  {
    "title": "Opening Keynote",
    "speaker": "Ada Lovelace",
    "speakerRole": "Chief Scientist",
    "date": "2026-10-06",
    "startTime": "09:30",
    "endTime": "10:30",
    "locationName": "Main Hall",
    "address": "747 Howard St, San Francisco",
    "latitude": 37.7841,
    "longitude": -122.4012,
    "colorHex": "#4F46E5",
    "track": "Keynote",
    "description": "Welcome and highlights."
  }
]
```

| Field | Required | Notes |
| --- | --- | --- |
| `title` | yes | Rows without a title are skipped |
| `date` | yes | `YYYY-MM-DD`, a real calendar date |
| `startTime`, `endTime` | yes | 24-hour `HH:mm`; the end must be after the start |
| `speaker` | no | Shown as "TBD" when empty |
| `speakerRole`, `track`, `description`, `address` | no | |
| `locationName` | no | Sessions with the same name share one pin and colour |
| `latitude`, `longitude` | no | Within ±90 / ±180. Without valid coordinates the session stays in the agenda but has no map pin or directions |
| `colorHex` | no | `#RGB` or `#RRGGBB`; assigned automatically when missing |

Invalid rows are skipped and reported on import (the app shows how many; the web builder lists each reason). Unknown properties are kept by the web builder and ignored by the app.

## Map data and attribution

The web builder shows [OpenStreetMap](https://www.openstreetmap.org/copyright) tiles and looks addresses up with [Nominatim](https://operations.osmfoundation.org/policies/nominatim/): lookups are limited to one per second and cached, in line with that policy.
