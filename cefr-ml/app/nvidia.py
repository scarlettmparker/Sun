"""
Preloads the pip-installed NVIDIA runtime libraries for ONNX Runtime.
"""

import ctypes
import glob
import sysconfig
from pathlib import Path

import onnxruntime

loaded = set()


def _candidate_roots():
    """
    Returns the site-packages directories that may hold the NVIDIA wheels.
    """
    roots = {Path(onnxruntime.__file__).resolve().parent.parent}
    for key in ("purelib", "platlib"):
        roots.add(Path(sysconfig.get_paths()[key]))
    return roots


def preload_nvidia_libs():
    """
    Loads CUDA shared libraries so the ONNX GPU provider can resolve them.

    onnxruntime-gpu does not bundle the CUDA runtime; pip wheels ship it under
    site-packages/nvidia. Without this preload the provider fails to initialize
    unless LD_LIBRARY_PATH is set. The preload is a no-op when no CUDA libraries
    exist, so CPU-only environments still work.
    """
    for root in _candidate_roots():
        matches = glob.glob(str(root / "nvidia" / "*" / "lib" / "*.so*"))
        for library in sorted(matches):
            real = Path(library).resolve()
            if real in loaded:
                continue
            try:
                ctypes.CDLL(str(real), mode=ctypes.RTLD_GLOBAL)
                loaded.add(real)
            except OSError:
                continue
