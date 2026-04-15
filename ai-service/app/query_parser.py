from __future__ import annotations

import re
from typing import List, Optional

from .enrichment import parse_time_window
from .models import ParsedQueryResponse


KNOWN_OCCASIONS = ["road trip", "trip", "marriage", "wedding", "family function", "festival"]


def _extract_people(query: str) -> List[str]:
    lowered = query.lower()
    cleaned = re.sub(r"[^a-z0-9,\s]", " ", lowered)
    tokens = [token.strip() for token in re.split(r",| and ", cleaned) if token.strip()]
    people = []
    for token in tokens:
        if token in {"me", "myself", "i"}:
            people.append("myself")
        elif token not in {"show", "give", "images", "photos", "of", "with", "where", "on", "in", "from", "last"}:
            if len(token.split()) == 1:
                people.append(token)
    # keep stable order and unique values
    deduped = list(dict.fromkeys(people))
    return deduped


def _extract_place(query: str) -> Optional[str]:
    q = query.lower()
    marker_pairs = [(" in ", " from "), (" on ", " from "), (" at ", " from ")]
    for start_marker, end_marker in marker_pairs:
        if start_marker in q:
            tail = q.split(start_marker, maxsplit=1)[1]
            if end_marker in tail:
                return tail.split(end_marker, maxsplit=1)[0].strip()
            return tail.strip()
    return None


def _extract_occasion(query: str) -> Optional[str]:
    q = query.lower()
    for occasion in KNOWN_OCCASIONS:
        if occasion in q:
            return occasion
    return None


def _extract_exclusions(query: str) -> List[str]:
    q = query.lower()
    matches = re.findall(r"(?:without|except)\s+([a-z]+)", q)
    return list(dict.fromkeys(matches))


def parse_query(query: str) -> ParsedQueryResponse:
    people = _extract_people(query)
    place = _extract_place(query)
    occasion = _extract_occasion(query)
    start_ms, end_ms = parse_time_window(query)
    excludes = _extract_exclusions(query)
    return ParsedQueryResponse(
        people=people,
        place=place,
        occasion=occasion,
        startEpochMs=start_ms,
        endEpochMs=end_ms,
        excludePeople=excludes
    )
