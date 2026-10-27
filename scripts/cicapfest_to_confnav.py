#!/usr/bin/env python3
"""Convert the CICAP Fest programme page into a ConfNav agenda JSON.

Usage:
    python3 cicapfest_to_confnav.py                       # fetch the live page, write cicapfest-2026.json
    python3 cicapfest_to_confnav.py -o agenda.json
    python3 cicapfest_to_confnav.py --html saved_page.html
    python3 cicapfest_to_confnav.py --skip-date 2026-11-05 --skip-date 2026-11-06

Standard library only (Python 3.8+).
"""
import argparse
import html
import json
import re
import sys
import urllib.request

URL = "https://www.cicapfest.it/programma"
YEAR = 2026
DEFAULT_SKIP_DATES = ["2026-11-05"]  # Rovigo preview, not in Padova

# Location table, keyed by the page's `data-location` text (normalised, see norm()).
# value: (locationName, address, latitude, longitude, colorHex, track)
# Rooms of the same building share an address but have their own pin and colour.
ALTINATE = "Via Altinate 71, 35121 Padova PD"
BO = "Via VIII Febbraio 2, 35122 Padova PD"
LOCATIONS = {
    # Centro Altinate
    "acquario | centro altinate": ("Centro Altinate – Acquario", ALTINATE, 45.408545, 11.882156, "#7E2910", "Acquario"),
    "agora' | centro altinate": ("Centro Altinate – Agorà", ALTINATE, 45.408688, 11.882018, "#AC3816", "Agorà"),
    "auditorium | centro altinate": ("Centro Altinate – Auditorium", ALTINATE, 45.408633, 11.881794, "#D9471C", "Auditorium"),
    "foyer | centro altinate": ("Centro Altinate – Foyer", ALTINATE, 45.408457, 11.881794, "#E66741", "Foyer"),
    "spazio 35 | centro altinate": ("Centro Altinate – Spazio 35", ALTINATE, 45.408402, 11.882018, "#EC8B6E", "Spazio 35"),
    # Palazzo del Bo
    "aula e | palazzo del bo": ("Palazzo del Bo – Aula E", BO, 45.4067, 11.8776, "#0C8379", "Aula E"),
    "aula l | palazzo del bo": ("Palazzo del Bo – Aula L", BO, 45.40685, 11.8774, "#11C2B2", "Aula L"),
    "aula magna | palazzo del bo": ("Palazzo del Bo – Aula Magna", BO, 45.4067, 11.8772, "#2AECDB", "Aula Magna"),
    "aula nievo | palazzo del bo": ("Palazzo del Bo – Aula Nievo", BO, 45.40655, 11.8774, "#69F2E6", "Aula Nievo"),
    # Caffè Pedrocchi
    "sala verde | caffè pedrocchi": ("Caffè Pedrocchi – Sala Verde", "Via VIII Febbraio 15, 35122 Padova PD", 45.4077, 11.8771, "#883F07", "Sala Verde"),
    # Single-room venues
    "liston": ("Liston", "Via VIII Febbraio 2 (di fronte a Palazzo del Bo), 35122 Padova PD", 45.4072, 11.8772, "#7C3AED", None),
    "orto botanico dell'università di padova": ("Orto botanico di Padova", "Via Orto Botanico 15, 35123 Padova PD", 45.3986, 11.8809, "#16A34A", None),
    "mnu museo della natura e dell'uomo dell'università di padova": ("Museo della Natura e dell'Uomo", "Corso Giuseppe Garibaldi 39, 35121 Padova PD", 45.412, 11.8783, "#2563EB", None),
    "libreria feltrinelli": ("Libreria Feltrinelli", "Via San Francesco 7, 35121 Padova PD", 45.4062, 11.8779, "#DB2777", None),
    "musme": ("MUSME - Museo di Storia della Medicina", "Via San Francesco 94, 35121 Padova PD", 45.4052, 11.8804, "#0891B2", None),
    "dip. medicina | univ. di padova": ("Dipartimento di Medicina - Università di Padova", "Via Giustiniani 2, 35128 Padova PD", 45.4028, 11.887, "#65A30D", None),
}


def norm(loc):
    """Normalise a data-location string so small spacing/case/apostrophe changes still match."""
    loc = html.unescape(loc).replace("’", "'").replace("‘", "'").replace("\xa0", " ")
    loc = re.sub(r"\s*\|\s*", " | ", loc)
    return re.sub(r"\s+", " ", loc).strip().casefold()


# Description lines that carry people: "Con ...", "Modera ...", "A cura di ...", etc.
# Moderators/presenters are included among the speakers. "Con il sostegno di ..." (sponsors) is not.
CREDIT_RE = re.compile(
    r"^(?:di e con|con(?! (?:il|la|lo|l'|i|gli|le)\b)|moderano|modera|presentano|presenta|"
    r"introduce|coordina|animazione a cura di|a cura di)\b",
    re.I,
)


def clean(s):
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", "", s))).strip()


def credit_names(segment):
    """Names (or organisations) from one credit line: every <b> block, split into its links if it has several."""
    names = []
    for block in re.findall(r"<b>(.*?)</b>", segment, re.S):
        parts = re.findall(r"<a [^>]*>(.*?)</a>|<mylinks>(.*?)</mylinks>", block, re.S)
        for name in [clean(a or b) for a, b in parts] if parts else [clean(block)]:
            if name.startswith("&") and names:  # "Francesco Lancia <b>& Friends</b>"
                names[-1] += " " + name
            elif name and not re.search(r"ospite a sorpresa", name, re.I):  # "a surprise guest" is not a name
                names.append(name)
    if not names:  # no bold markup: fall back to the plain text after the keyword
        text = CREDIT_RE.sub("", clean(segment), count=1)
        names = [n.strip() for n in re.split(r",\s*|\s+(?:e|ed)\s+", text) if n.strip()]
    return names


def speakers_from(description_html, card_html):
    """Credit lines of the description first (guests, then moderators), then anyone only in the card's guest avatars."""
    names = []
    for segment in re.split(r"<br\s*/?>", description_html):
        if CREDIT_RE.match(clean(segment)):
            names += credit_names(segment)
        else:  # "Esperimenti spettacolari a cura di ..." (credit in the middle of the line)
            mid = re.search(r"\ba cura di\b", segment, re.I)
            if mid:
                names += credit_names(segment[mid.end():])
    unique = list(dict.fromkeys(names))
    for n in (clean(x) for x in re.findall(r'rounded-full[^"]*overflow-hidden"\s+title="([^"]*)"', card_html)):
        if n and not any(n.casefold() in u.casefold() for u in unique):  # also skips "X & Friends" vs "X"
            unique.append(n)
    return unique


def load_page(args):
    if args.html:
        with open(args.html, encoding="utf-8") as f:
            return f.read()
    req = urllib.request.Request(args.url, headers={"User-Agent": "Mozilla/5.0 (confnav-import)"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read().decode("utf-8")


def parse(page, skip_dates):
    day_marks = [(m.group(1), m.start()) for m in re.finditer(r'<div id="day-(\d\d-\d\d)" class="day-content', page)]
    if not day_marks:
        sys.exit("No day sections found: the page layout may have changed.")
    sessions, unknown = [], {}
    for n, (day, start) in enumerate(day_marks):
        end = day_marks[n + 1][1] if n + 1 < len(day_marks) else len(page)
        date = f"{YEAR}-{day}"
        if date in skip_dates:
            continue
        for card in re.split(r"<!-- Card Evento -->", page[start:end])[1:]:
            loc = re.search(r'data-location="([^"]*)"', card)
            times = re.search(r"(\d\d:\d\d) - (\d\d:\d\d)", card)
            title = re.search(r'<h3[^>]*>\s*<a href="([^"]*)">(.*?)</a>', card, re.S)
            desc = re.search(r"description-text[^>]*>(.*?)</div>", card, re.S)
            if not (loc and times and title):
                sys.exit(f"Could not parse an event card on {date}; the page layout may have changed.")
            info = LOCATIONS.get(norm(loc.group(1)))
            title_text = html.unescape(title.group(2)).strip()
            if info is None:
                unknown.setdefault(html.unescape(loc.group(1)).strip(), []).append(f"{date} {times.group(1)} {title_text}")
                continue
            name, address, lat, lon, color, track = info
            speakers = speakers_from(desc.group(1) if desc else "", card)
            if times.group(1) >= times.group(2):
                print(f"warning: end time not after start time: {date} {title_text}", file=sys.stderr)
            session = {
                "title": title_text,
                "speaker": ", ".join(speakers) or "TBD",
                "date": date,
                "startTime": times.group(1),
                "endTime": times.group(2),
                "locationName": name,
                "address": address,
                "latitude": lat,
                "longitude": lon,
                "colorHex": color,
            }
            if track:
                session["track"] = track
            session["url"] = title.group(1)
            sessions.append(session)
    if unknown:
        lines = ["Locations missing from the LOCATIONS table (add them near the top of this script):"]
        for loc, evs in unknown.items():
            lines.append(f"  {loc!r}  ({len(evs)} events, e.g. {evs[0]})")
        sys.exit("\n".join(lines))
    sessions.sort(key=lambda s: (s["date"], s["startTime"], s["endTime"], s["title"]))
    return sessions


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--url", default=URL, help="programme page URL")
    ap.add_argument("--html", help="read a saved copy of the page instead of fetching it")
    ap.add_argument("-o", "--output", default=f"cicapfest-{YEAR}.json")
    ap.add_argument("--skip-date", action="append", metavar="YYYY-MM-DD",
                    help=f"drop this day (repeatable; default: {', '.join(DEFAULT_SKIP_DATES)})")
    args = ap.parse_args()
    skip = set(args.skip_date if args.skip_date is not None else DEFAULT_SKIP_DATES)

    sessions = parse(load_page(args), skip)
    if not sessions:
        sys.exit("No sessions found.")
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(sessions, f, ensure_ascii=False, indent=2)
    per_day = {}
    for s in sessions:
        per_day[s["date"]] = per_day.get(s["date"], 0) + 1
    print(f"Wrote {len(sessions)} sessions to {args.output}: " + ", ".join(f"{d}: {n}" for d, n in sorted(per_day.items())))


if __name__ == "__main__":
    main()
