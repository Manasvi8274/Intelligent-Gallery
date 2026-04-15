from __future__ import annotations

from datetime import datetime, timedelta, timezone
from typing import Optional, Tuple

from dateutil import parser as dateparser
from geopy.geocoders import Nominatim


class MetadataEnricher:
    def __init__(self) -> None:
        self._geo = Nominatim(user_agent="intelligent-gallery-ai")

    def resolve_place(self, latitude: Optional[float], longitude: Optional[float]) -> Tuple[Optional[str], Optional[str]]:
        if latitude is None or longitude is None:
            return None, None
        try:
            location = self._geo.reverse((latitude, longitude), zoom=18, exactly_one=True)
            if not location:
                return None, None
            address = location.raw.get("address", {})
            landmark = (
                address.get("tourism")
                or address.get("attraction")
                or address.get("building")
                or address.get("neighbourhood")
            )
            place = ", ".join(part for part in [address.get("city") or address.get("town"), address.get("state")] if part)
            return place or location.address, landmark
        except Exception:
            return None, None

    def infer_occasion(self, image_path: str, month: Optional[int] = None) -> Optional[str]:
        path = image_path.lower()
        if "wedding" in path or "marriage" in path:
            return "marriage"
        if "trip" in path or "travel" in path:
            return "road trip"
        if month in (10, 11, 12):
            return "festival"
        return None


def parse_time_window(query: str) -> Tuple[Optional[int], Optional[int]]:
    now = datetime.now(tz=timezone.utc)
    q = query.lower()
    if "last week" in q:
        end = now
        start = now - timedelta(days=7)
        return int(start.timestamp() * 1000), int(end.timestamp() * 1000)
    if "last month" in q:
        end = now
        start = now - timedelta(days=30)
        return int(start.timestamp() * 1000), int(end.timestamp() * 1000)
    if "last year" in q:
        end = now
        start = now - timedelta(days=365)
        return int(start.timestamp() * 1000), int(end.timestamp() * 1000)

    try:
        dt = dateparser.parse(query, fuzzy=True)
        if dt:
            start = datetime(dt.year, dt.month, dt.day, tzinfo=timezone.utc)
            end = start + timedelta(days=1)
            return int(start.timestamp() * 1000), int(end.timestamp() * 1000)
    except Exception:
        pass
    return None, None
