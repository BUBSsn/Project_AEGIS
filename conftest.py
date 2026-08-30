"""pytest configuration — ensures the project root is on sys.path.

This file is loaded by pytest before any test module, so all test files can
import ``core.*`` and ``src.*`` without each needing a manual
``sys.path.insert(0, ...)`` at the top.
"""
import sys
from pathlib import Path

# Insert the project root (the directory containing this file) at the front of
# sys.path so that ``import core.ast_scanner`` and ``from src.sample_service``
# work whether pytest is invoked from the project root or from within tests/.
_PROJECT_ROOT = str(Path(__file__).resolve().parent)
if _PROJECT_ROOT not in sys.path:
    sys.path.insert(0, _PROJECT_ROOT)
