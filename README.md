# ConfNav

An Android app for finding your way around a conference. Load a schedule, see what's on each day, spot every venue on a map, and get walking directions to your next session.

Easily build and customise your agenda in the companion web tool, [ConfNav](https://urzahil.github.io/ConfNav/), then load the exported JSON file into the app.

## Features

- Agenda with day-by-day filtering and a "next event" pill that jumps straight to directions
- Map with colour-coded pins, with sessions grouped by location
- Add, edit and delete sessions directly in the app
- JSON import and export, either adding to your agenda or replacing it (re-importing a file never duplicates sessions)
- Everything is stored on the device, so your agenda is available offline

## Map data and attribution

The web builder shows [OpenStreetMap](https://www.openstreetmap.org/copyright) tiles and looks addresses up with [Nominatim](https://operations.osmfoundation.org/policies/nominatim/): lookups are limited to one per second and cached, in line with that policy.
