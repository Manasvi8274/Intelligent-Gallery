from __future__ import annotations

from datetime import datetime, timezone

from fastapi import FastAPI

from .enrichment import MetadataEnricher
from .face_engine import FaceEngine
from .models import (
    EnrichImageRequest,
    EnrichImageResponse,
    FacePrediction,
    ParseQueryRequest,
    ParsedQueryResponse,
)
from .query_parser import parse_query

app = FastAPI(title="Intelligent Gallery AI Service", version="0.1.0")
face_engine = FaceEngine()
enricher = MetadataEnricher()


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post("/enrich_image", response_model=EnrichImageResponse)
def enrich_image(request: EnrichImageRequest) -> EnrichImageResponse:
    face_predictions = face_engine.detect_and_recognize(request.imagePath)
    place, landmark = enricher.resolve_place(request.latitude, request.longitude)
    month = None
    if request.capturedAtEpochMs is not None:
        dt = datetime.fromtimestamp(request.capturedAtEpochMs / 1000.0, tz=timezone.utc)
        month = dt.month
    occasion = enricher.infer_occasion(request.imagePath, month=month)

    return EnrichImageResponse(
        people=[FacePrediction(personName=name, confidence=confidence) for name, confidence in face_predictions],
        place=place,
        nearestLandmark=landmark,
        occasion=occasion
    )


@app.post("/parse_query", response_model=ParsedQueryResponse)
def parse_query_endpoint(request: ParseQueryRequest) -> ParsedQueryResponse:
    return parse_query(request.query)
