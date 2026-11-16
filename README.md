# ConfNav

An Android app for finding your way around a conference. Load a schedule, browse each day's sessions, see every venue on a map, and get walking directions to whatever is on next, even when you're offline.

Agendas are built in the companion **web builder** ([urzahil.github.io/ConfNav](https://urzahil.github.io/ConfNav)), exported as JSON, and imported into the app.

## Features

### Android app
- **Welcome slides**: on first launch, four short slides introduce the app, the web builder and the Wear OS app. Skip them at any time.
- **Agenda**: filter sessions by day. Star the sessions you don't want to miss: they get a gold star next to their title and are drawn on top of overlapping sessions in the Calendar. A "next event" pill jumps straight to directions for the upcoming session.
- **Map**: colour-coded pins, with sessions grouped by location.
- **Editing**: add, edit and delete sessions inside the app.
- **Import / export**: load a JSON agenda either by merging it into your existing one or by replacing it. Re-importing the same file never creates duplicate sessions.
- **Offline storage**: everything lives on the device, so your agenda works without a connection.

### Wear OS watch
- **Tile**: your next session in the app's colours: an UP NEXT (or NOW) label, the time, title, venue and speaker, and a Directions button in the session's pin colour. It switches to the next session on its own as the day goes on.
- **Complication**: the start time and venue (or the session title) on your watch face. Tap it for directions.
- **Directions**: opens walking navigation in Google Maps on the watch, or on your phone if the watch has no Maps.
- The phone app sends your agenda to the watch over Bluetooth whenever it changes, so the watch works offline too. Open the phone app once after installing the watch app to send it.

### Web builder
- Create and edit sessions with title, speaker, time, venue and colour, and star the ones that matter most.
- Look up venue addresses and place them on a map.
- Export the agenda as JSON for the app to import.

## Installing on a watch

The release workflow builds `ConfNav-release.apk` (phone) and `ConfNav-wear-release.apk` (watch). The watch app only receives the agenda when both apps come from the same build type: install both release APKs, or both debug builds (`./gradlew :app:installDebug` and `:wear:installDebug`).

To sideload the watch APK:
1. On the watch, open **Settings → System → About → Versions** and tap **Build number** seven times.
2. In **Settings → Developer options**, turn on **ADB debugging** and **Wireless debugging**. Keep the watch on the same Wi-Fi as your computer.
3. Tap **Pair new device**, then run `adb pair <ip>:<port>` with the code it shows.
4. Run `adb connect <ip>:<port>` with the address on the Wireless debugging screen, then `adb install ConfNav-wear-release.apk`.
5. Swipe to the end of your tiles, tap **+** and add **ConfNav: up next**. To add the complication, long-press the watch face, tap **Edit** and pick **ConfNav: next session** for a slot.

## Agenda JSON format

Each exported session is an object with these fields:

| Field | Type | Required | Notes |
|---|---|---|---|
| `title` | string | yes | |
| `speaker` | string | yes | |
| `speakerRole` | string | no | e.g. "Staff Engineer" |
| `date` | string | yes | `YYYY-MM-DD` |
| `startTime` | string | yes | `HH:mm` |
| `endTime` | string | yes | `HH:mm` |
| `locationName` | string | yes | |
| `address` | string | yes | |
| `latitude` | number | yes | |
| `longitude` | number | yes | |
| `colorHex` | string | yes | `#RRGGBB` |
| `track` | string | no | e.g. "Keynote" |
| `description` | string | no | |
| `starred` | boolean | no | `true` marks the session with a gold star; omitted when not starred |

Custom keys are allowed and are preserved when a file is re-imported and exported.

```json
{
  "title": "Opening Keynote",
  "speaker": "Ada Lovelace",
  "speakerRole": "Principal Engineer",
  "date": "2026-10-20",
  "startTime": "09:00",
  "endTime": "10:00",
  "locationName": "Main Hall",
  "address": "1 Conference Way, Springfield",
  "latitude": 40.7128,
  "longitude": -74.006,
  "colorHex": "#0D9488",
  "track": "Keynote"
}
```

## Map data and attribution

The web builder uses [OpenStreetMap](https://www.openstreetmap.org/copyright) tiles and looks up addresses through [Nominatim](https://operations.osmfoundation.org/policies/nominatim/). Lookups are limited to one per second and cached, in line with Nominatim's usage policy. The Android app uses Google Maps.

## License

ConfNav is licensed under the [GNU General Public License v3.0](LICENSE).
