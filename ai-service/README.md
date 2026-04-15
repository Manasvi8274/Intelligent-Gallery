# Intelligent Gallery AI Service

Local Python service for free/open-source AI modules:
- face detection + face embedding (InsightFace)
- metadata enrichment (reverse geocode + occasion heuristics)
- natural language query parsing to structured filters

## Run locally

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Android emulator should use `http://10.0.2.2:8000/` as base URL.

## Identity training

To improve recognition quality, keep embeddings for labeled users in:
`ai-service/data/face_profiles.json`

Example:

```json
{
  "myself": [0.01, -0.06, 0.11],
  "rohan": [-0.04, 0.02, 0.09]
}
```

Real embeddings are usually 512 dimensions.
