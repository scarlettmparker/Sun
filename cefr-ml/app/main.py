"""
FastAPI service exposing the CEFR classifier over ONNX Runtime.
"""

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from model import CefrClassifier

app = FastAPI(title="cefr-ml")
classifier = CefrClassifier()


class ClassifyRequest(BaseModel):
    text: str


@app.get("/health")
def health():
    """
    Returns a lightweight liveness check.
    """
    return {"ok": True}


@app.post("/classify")
def classify(request: ClassifyRequest):
    """
    Predicts the CEFR level of a text.
    """
    text = request.text.strip()
    if not text:
        raise HTTPException(status_code=400, detail="text is required")
    return classifier.classify(text)
