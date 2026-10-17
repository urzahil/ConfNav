# ConfNav

An Android app for finding your way around a conference. Load a schedule, browse each day's sessions, see every venue on a map, and get walking directions to whatever is on next, even when you're offline.

Agendas are built in the companion **web builder** ([urzahil.github.io/ConfNav](https://urzahil.github.io/ConfNav)), exported as JSON, and imported into the app.

## Features

### Android app
- **Agenda**: filter sessions by day. A "next event" pill jumps straight to directions for the upcoming session.
- **Map**: colour-coded pins, with sessions grouped by location.
- **Editing**: add, edit and delete sessions inside the app.
- **Import / export**: load a JSON agenda either by merging it into your existing one or by replacing it. Re-importing the same file never creates duplicate sessions.
- **Offline storage**: everything lives on the device, so your agenda works without a connection.

### Web builder
- Create and edit sessions with title, speaker, time, venue and colour.
- Look up venue addresses and place them on a map.
- Export the agenda as JSON for the app to import.

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
