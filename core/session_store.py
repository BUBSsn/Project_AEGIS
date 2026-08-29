"""Session registry for AEGIS multi-repo scanning.

Persists one entry per repo scan to ``reports/sessions.json``.
The registry is capped at MAX_SESSIONS entries; when the cap is exceeded the
oldest entry (by timestamp) is evicted and its workspace clone directory is
deleted to reclaim disk space.
"""
import json
import shutil
import stat
import os
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

SESSIONS_PATH = Path("reports/sessions.json")
WORKSPACES_DIR = Path("workspaces")

MAX_SESSIONS: int = 20


# ---------------------------------------------------------------------------
# Internal helpers
# ---------------------------------------------------------------------------

def _force_rmtree(path: Path) -> None:
    """Remove a directory tree, clearing read-only flags on Windows first."""
    def _on_error(func, fpath, exc_info):
        os.chmod(fpath, stat.S_IWRITE)
        func(fpath)

    shutil.rmtree(path, onerror=_on_error)


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def load_sessions() -> List[Dict[str, Any]]:
    """Load the session registry from disk.

    Returns a list of session dicts ordered by ``timestamp`` ascending
    (oldest first).  Returns ``[]`` on any read or parse error.
    """
    try:
        if SESSIONS_PATH.exists():
            raw: Any = json.loads(SESSIONS_PATH.read_text(encoding="utf-8"))
            if isinstance(raw, list):
                return raw
    except (json.JSONDecodeError, OSError):
        pass
    return []


def save_sessions(sessions: List[Dict[str, Any]]) -> None:
    """Persist the session registry to disk.  Fails silently on OSError."""
    try:
        SESSIONS_PATH.parent.mkdir(parents=True, exist_ok=True)
        SESSIONS_PATH.write_text(
            json.dumps(sessions, indent=2, ensure_ascii=False),
            encoding="utf-8",
        )
    except OSError:
        pass


def add_session(
    session_id: str,
    repo_url: str,
    clone_dir: str,
    sarif_path: str,
    finding_count: int,
    project_root: Optional[Path] = None,
) -> List[Dict[str, Any]]:
    """Register a completed scan, enforce MAX_SESSIONS cap, and persist.

    Parameters
    ----------
    session_id:    UUID string identifying this scan session.
    repo_url:      Original GitHub URL that was scanned.
    clone_dir:     Path to the workspace clone directory (relative to project root).
    sarif_path:    Path to the SARIF output file (relative to project root).
    finding_count: Number of findings in the SARIF.
    project_root:  Project root Path used to resolve absolute clone dirs for
                   pruning.  Defaults to the directory two levels above this file.

    Returns the updated (post-cap) session list.
    """
    if project_root is None:
        project_root = Path(__file__).resolve().parent.parent

    sessions = load_sessions()

    entry: Dict[str, Any] = {
        "session_id":    session_id,
        "repo_url":      repo_url,
        "repo_name":     repo_url.rstrip("/").split("/")[-1],
        "clone_dir":     clone_dir,
        "sarif_path":    sarif_path,
        "timestamp":     datetime.now(timezone.utc).isoformat(),
        "finding_count": finding_count,
    }
    sessions.append(entry)

    # Enforce cap: evict oldest entries until len <= MAX_SESSIONS
    sessions.sort(key=lambda s: s.get("timestamp", ""))
    while len(sessions) > MAX_SESSIONS:
        evicted = sessions.pop(0)
        evicted_clone = project_root / evicted.get("clone_dir", "")
        if evicted_clone.exists():
            try:
                _force_rmtree(evicted_clone)
            except OSError:
                pass  # best-effort; don't crash on prune failure

    save_sessions(sessions)
    return sessions


def get_session(session_id: str) -> Optional[Dict[str, Any]]:
    """Return the session dict for *session_id*, or ``None`` if not found."""
    for s in load_sessions():
        if s.get("session_id") == session_id:
            return s
    return None
