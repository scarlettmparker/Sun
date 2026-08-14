#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

python3 -m venv --without-pip .venv
curl -sS https://bootstrap.pypa.io/get-pip.py -o /tmp/get-pip.py
.venv/bin/python /tmp/get-pip.py

# CUDA torch, pulls in the nvidia runtime wheels that onnxruntime-gpu needs.
.venv/bin/pip install torch --index-url https://download.pytorch.org/whl/cu126
.venv/bin/pip install -r requirements.txt

echo "Ready. Training: .venv/bin/python training/train.py"
echo "Serving:  cd app && ../.venv/bin/python -m uvicorn main:app --port 8084"
