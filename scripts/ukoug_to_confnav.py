#!/usr/bin/env python3
"""Convert a Sessionize "GridSmart" embed (Novotel London West event) into a ConfNav agenda JSON.

Usage:
    python3 ukoug_to_confnav.py                      # fetch the live grid, write sessionize-agenda.json
    python3 ukoug_to_confnav.py -o agenda.json
    python3 ukoug_to_confnav.py --html saved_grid.html

Standard library only (Python 3.8+).
"""
import argparse
import html
import json
import math
import re
import sys
import urllib.request

URL = "https://sessionize.com/api/v2/tbqv7l55/view/GridSmart"

VENUE = "Novotel London West"
ADDRESS = "Novotel London West, 1 Shortlands, London W6 8DR, United Kingdom"
BASE_LAT, BASE_LON = 51.4918, -0.2206
# Rooms sit on a small ring around the hotel so their pins don't overlap (about 20 m radius).
RING_LAT, RING_LON = 0.00018, 0.00027

# Room name as shown on the grid -> pin colour. Order = position around the ring.
ROOMS = [
    ("Novotel Reception", "#E6194B"),
    ("Cremant Suite", "#3CB44B"),
    ("Bouzy", "#FFE119"),
    ("Epernay", "#4363D8"),
    ("Chalon", "#F58231"),
    ("Reims", "#911EB4"),
    ("Mezzanine - Alsace", "#42D4F4"),
    ("Mezzanine - Beaujolais", "#F032E6"),
    ("Mezzanine - Bourg", "#BFEF45"),
    ("Mezzanine - Muscadet", "#469990"),
    ("Mezzanine - Pouilly", "#9A6324"),
    ("Mezzanine - Sancerre", "#800000"),
]
LOCATIONS = {}
for i, (room, color) in enumerate(ROOMS):
    angle = 2 * math.pi * i / len(ROOMS)
    LOCATIONS[room.casefold()] = (
        room,
        round(BASE_LAT + RING_LAT * math.sin(angle), 6),
        round(BASE_LON + RING_LON * math.cos(angle), 6),
        color,
    )

TRACK_CATEGORY = "track_selection"  # the Sessionize tag category used as the session's track


def clean(s):
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", "", s))).strip()


def load_page(args):
    if args.html:
        with open(args.html, encoding="utf-8") as f:
            return f.read()
    req = urllib.request.Request(args.url, headers={"User-Agent": "Mozilla/5.0 (confnav-import)"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read().decode("utf-8")


def parse(page):
    blocks = re.split(r'<div data-sessionid="', page)[1:]
    if not blocks:
        sys.exit("No sessions found: the page layout may have changed.")
    sessions, unknown = [], {}
    for block in blocks:
        room = re.search(r'class="sz-session__room">(.*?)</div>', block, re.S)
        times = re.search(r'data-sztz="TimeWithDuration\|[^|]*\|(\d{4}-\d\d-\d\d)T(\d\d:\d\d)[^|]*\|\d{4}-\d\d-\d\dT(\d\d:\d\d)', block)
        title = re.search(r'<h3 class="sz-session__title">(.*?)</h3>', block, re.S)
        if not (room and times and title):
            sys.exit("Could not parse a session; the page layout may have changed.")
        room_name, title_text = clean(room.group(1)), clean(title.group(1))
        info = LOCATIONS.get(room_name.casefold())
        if info is None:
            unknown.setdefault(room_name, []).append(f"{times.group(1)} {times.group(2)} {title_text}")
            continue
        name, lat, lon, color = info
        speakers = re.search(r'<ul class="sz-session__speakers">(.*?)</ul>', block, re.S)
        speakers = [clean(x) for x in re.findall(r"<li[^>]*>(.*?)</li>", speakers.group(1), re.S)] if speakers else []
        track = re.search(r'data-categoryname="%s">(.*?)</li>' % TRACK_CATEGORY, block, re.S)
        session = {
            "title": title_text,
            "speaker": ", ".join(dict.fromkeys(speakers)),
            "date": times.group(1),
            "startTime": times.group(2),
            "endTime": times.group(3),
            "locationName": name,
            "address": ADDRESS,
            "latitude": lat,
            "longitude": lon,
            "colorHex": color,
        }
        if track:
            session["track"] = clean(track.group(1))
        sessions.append(session)
    if unknown:
        lines = ["Rooms missing from the ROOMS table (add them near the top of this script):"]
        for room, evs in unknown.items():
            lines.append(f"  {room!r}  ({len(evs)} sessions, e.g. {evs[0]})")
        sys.exit("\n".join(lines))
    sessions.sort(key=lambda s: (s["date"], s["startTime"], s["endTime"], s["locationName"], s["title"]))
    return sessions


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--url", default=URL, help="Sessionize GridSmart URL")
    ap.add_argument("--html", help="read a saved copy of the grid instead of fetching it")
    ap.add_argument("-o", "--output", default="sessionize-agenda.json")
    args = ap.parse_args()

    sessions = parse(load_page(args))
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(sessions, f, ensure_ascii=False, indent=2)
    per_day = {}
    for s in sessions:
        per_day[s["date"]] = per_day.get(s["date"], 0) + 1
    print(f"Wrote {len(sessions)} sessions to {args.output}: " + ", ".join(f"{d}: {n}" for d, n in sorted(per_day.items())))


if __name__ == "__main__":
    main()
