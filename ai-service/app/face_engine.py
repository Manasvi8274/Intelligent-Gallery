from __future__ import annotations

import json
from pathlib import Path
from typing import Dict, List, Tuple

import cv2
import numpy as np

try:
    from insightface.app import FaceAnalysis
except Exception:  # pragma: no cover - optional dependency import safety
    FaceAnalysis = None


class FaceEngine:
    def __init__(self, profiles_file: str = "data/face_profiles.json") -> None:
        self.profiles_path = Path(profiles_file)
        self.profiles_path.parent.mkdir(parents=True, exist_ok=True)
        self._profiles = self._load_profiles()
        self._app = None

    def _ensure_model(self) -> bool:
        if self._app is not None:
            return True
        if FaceAnalysis is None:
            return False
        self._app = FaceAnalysis(name="buffalo_l")
        self._app.prepare(ctx_id=-1, det_size=(640, 640))
        return True

    def _load_profiles(self) -> Dict[str, List[float]]:
        if self.profiles_path.exists():
            return json.loads(self.profiles_path.read_text(encoding="utf-8"))
        return {}

    def _match_identity(self, embedding: np.ndarray) -> Tuple[str, float]:
        best_name = "unknown"
        best_score = -1.0
        for name, ref_embedding in self._profiles.items():
            ref = np.array(ref_embedding, dtype=np.float32)
            score = float(np.dot(embedding, ref) / (np.linalg.norm(embedding) * np.linalg.norm(ref) + 1e-6))
            if score > best_score:
                best_score = score
                best_name = name

        if best_score < 0.35:
            return "unknown", max(best_score, 0.0)
        return best_name, best_score

    def detect_and_recognize(self, image_path: str) -> List[Tuple[str, float]]:
        # Fallback to empty output if AI model not available.
        if not self._ensure_model():
            return []
        image = cv2.imread(image_path)
        if image is None:
            return []

        faces = self._app.get(image)
        predictions: List[Tuple[str, float]] = []
        for face in faces:
            embedding = np.array(face.embedding, dtype=np.float32)
            name, confidence = self._match_identity(embedding)
            predictions.append((name, confidence))
        return predictions
