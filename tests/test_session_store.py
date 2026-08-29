"""Tests for core/session_store.py — session registry persistence helpers."""
import json
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from core.session_store import (
    MAX_SESSIONS,
    add_session,
    get_session,
    load_sessions,
    save_sessions,
)


# ---------------------------------------------------------------------------
# load_sessions / save_sessions round-trip
# ---------------------------------------------------------------------------

def test_load_sessions_missing_file(tmp_path, monkeypatch):
    """load_sessions returns [] when the file does not exist."""
    import core.session_store as ss
    monkeypatch.setattr(ss, "SESSIONS_PATH", tmp_path / "sessions.json")
    assert ss.load_sessions() == []


def test_load_sessions_empty_list(tmp_path, monkeypatch):
    """load_sessions returns [] for an empty JSON array."""
    import core.session_store as ss
    p = tmp_path / "sessions.json"
    p.write_text("[]", encoding="utf-8")
    monkeypatch.setattr(ss, "SESSIONS_PATH", p)
    assert ss.load_sessions() == []


def test_load_sessions_corrupt_json(tmp_path, monkeypatch):
    """load_sessions returns [] and does not raise on corrupt JSON."""
    import core.session_store as ss
    p = tmp_path / "sessions.json"
    p.write_text("NOT JSON {{{{", encoding="utf-8")
    monkeypatch.setattr(ss, "SESSIONS_PATH", p)
    assert ss.load_sessions() == []


def test_load_sessions_wrong_type(tmp_path, monkeypatch):
    """load_sessions returns [] when the JSON root is not a list."""
    import core.session_store as ss
    p = tmp_path / "sessions.json"
    p.write_text('{"key": "val"}', encoding="utf-8")
    monkeypatch.setattr(ss, "SESSIONS_PATH", p)
    assert ss.load_sessions() == []


def test_save_and_load_round_trip(tmp_path, monkeypatch):
    """save_sessions then load_sessions returns the original data."""
    import core.session_store as ss
    p = tmp_path / "sessions.json"
    monkeypatch.setattr(ss, "SESSIONS_PATH", p)
    data = [{"session_id": "abc", "repo_url": "https://github.com/x/y"}]
    ss.save_sessions(data)
    assert ss.load_sessions() == data


def test_save_sessions_oserror(monkeypatch):
    """save_sessions fails silently on OSError."""
    import core.session_store as ss

    def _bad_write(*args, **kwargs):
        raise OSError("disk full")

    monkeypatch.setattr(Path, "write_text", _bad_write)
    # Should not raise
    ss.save_sessions([{"session_id": "x"}])


# ---------------------------------------------------------------------------
# add_session — basic registration
# ---------------------------------------------------------------------------

def test_add_session_creates_entry(tmp_path, monkeypatch):
    """add_session persists a session entry with the expected fields."""
    import core.session_store as ss
    p = tmp_path / "sessions.json"
    monkeypatch.setattr(ss, "SESSIONS_PATH", p)

    result = ss.add_session(
        session_id="sess-1",
        repo_url="https://github.com/owner/repo",
        clone_dir="workspaces/sess-1",
        sarif_path="reports/sessions/sess-1.sarif",
        finding_count=5,
        project_root=tmp_path,
    )

    assert len(result) == 1
    entry = result[0]
    assert entry["session_id"] == "sess-1"
    assert entry["repo_url"] == "https://github.com/owner/repo"
    assert entry["repo_name"] == "repo"
    assert entry["clone_dir"] == "workspaces/sess-1"
    assert entry["sarif_path"] == "reports/sessions/sess-1.sarif"
    assert entry["finding_count"] == 5
    assert "timestamp" in entry


def test_add_session_persists_to_disk(tmp_path, monkeypatch):
    """add_session writes the session list to SESSIONS_PATH."""
    import core.session_store as ss
    p = tmp_path / "sessions.json"
    monkeypatch.setattr(ss, "SESSIONS_PATH", p)

    ss.add_session(
        session_id="sess-disk",
        repo_url="https://github.com/a/b",
        clone_dir="workspaces/sess-disk",
        sarif_path="reports/sessions/sess-disk.sarif",
        finding_count=0,
        project_root=tmp_path,
    )

    assert p.exists()
    on_disk = json.loads(p.read_text(encoding="utf-8"))
    assert isinstance(on_disk, list)
    assert on_disk[0]["session_id"] == "sess-disk"


# ---------------------------------------------------------------------------
# add_session — cap enforcement
# ---------------------------------------------------------------------------

def test_add_session_cap_enforcement(tmp_path, monkeypatch):
    """add_session prunes oldest entries so len never exceeds MAX_SESSIONS."""
    import core.session_store as ss
    p = tmp_path / "sessions.json"
    monkeypatch.setattr(ss, "SESSIONS_PATH", p)

    for i in range(MAX_SESSIONS + 5):
        ss.add_session(
            session_id=f"sess-{i:03d}",
            repo_url=f"https://github.com/owner/repo{i}",
            clone_dir=f"workspaces/sess-{i:03d}",
            sarif_path=f"reports/sessions/sess-{i:03d}.sarif",
            finding_count=i,
            project_root=tmp_path,
        )

    sessions = ss.load_sessions()
    assert len(sessions) == MAX_SESSIONS
    # The oldest entries (sess-000 … sess-004) should have been evicted
    ids = [s["session_id"] for s in sessions]
    assert "sess-000" not in ids
    assert f"sess-{MAX_SESSIONS + 4:03d}" in ids


def test_add_session_prunes_clone_dir(tmp_path, monkeypatch):
    """Evicted sessions have their clone directory removed from disk."""
    import core.session_store as ss
    p = tmp_path / "sessions.json"
    monkeypatch.setattr(ss, "SESSIONS_PATH", p)

    # Create a real directory for the first session's clone
    clone = tmp_path / "workspaces" / "evicted"
    clone.mkdir(parents=True)
    (clone / "file.py").write_text("x = 1", encoding="utf-8")

    # Fill up to cap with the eviction candidate first
    ss.add_session(
        session_id="evicted",
        repo_url="https://github.com/o/evicted",
        clone_dir="workspaces/evicted",
        sarif_path="reports/sessions/evicted.sarif",
        finding_count=0,
        project_root=tmp_path,
    )
    for i in range(MAX_SESSIONS):
        ss.add_session(
            session_id=f"keep-{i:03d}",
            repo_url=f"https://github.com/o/keep{i}",
            clone_dir=f"workspaces/keep-{i:03d}",
            sarif_path=f"reports/sessions/keep-{i:03d}.sarif",
            finding_count=1,
            project_root=tmp_path,
        )

    # The evicted clone directory must have been deleted
    assert not clone.exists()


# ---------------------------------------------------------------------------
# get_session
# ---------------------------------------------------------------------------

def test_get_session_found(tmp_path, monkeypatch):
    """get_session returns the matching entry."""
    import core.session_store as ss
    p = tmp_path / "sessions.json"
    monkeypatch.setattr(ss, "SESSIONS_PATH", p)
    ss.add_session(
        session_id="target",
        repo_url="https://github.com/x/y",
        clone_dir="workspaces/target",
        sarif_path="reports/sessions/target.sarif",
        finding_count=3,
        project_root=tmp_path,
    )
    entry = ss.get_session("target")
    assert entry is not None
    assert entry["session_id"] == "target"


def test_get_session_not_found(tmp_path, monkeypatch):
    """get_session returns None when no matching session exists."""
    import core.session_store as ss
    p = tmp_path / "sessions.json"
    monkeypatch.setattr(ss, "SESSIONS_PATH", p)
    assert ss.get_session("no-such-session") is None
