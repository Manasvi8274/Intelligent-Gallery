from typing import List, Optional

from pydantic import BaseModel, Field


class EnrichImageRequest(BaseModel):
    imagePath: str
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    capturedAtEpochMs: Optional[int] = None


class FacePrediction(BaseModel):
    personName: str
    confidence: float


class EnrichImageResponse(BaseModel):
    people: List[FacePrediction] = Field(default_factory=list)
    place: Optional[str] = None
    nearestLandmark: Optional[str] = None
    occasion: Optional[str] = None


class ParseQueryRequest(BaseModel):
    query: str


class ParsedQueryResponse(BaseModel):
    people: List[str] = Field(default_factory=list)
    place: Optional[str] = None
    occasion: Optional[str] = None
    startEpochMs: Optional[int] = None
    endEpochMs: Optional[int] = None
    excludePeople: List[str] = Field(default_factory=list)
