import pytest
import sqlite3
import os
import sys
from pathlib import Path

# Add project root to sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from src.sample_service import UserService, JWT_SECRET_KEY


@pytest.fixture
def temp_service(tmp_path):
    db_file = tmp_path / "test_users.db"
    service = UserService(str(db_file))
    # Seed initial test record
    with service.conn:
        service.conn.execute(
            "INSERT INTO users (username, role) VALUES (?, ?)",
            ("admin_user", "administrator")
        )
    return service


def test_get_user_valid(temp_service):
    user = temp_service.get_user_by_username("admin_user")
    assert user is not None
    assert user["username"] == "admin_user"
    assert user["role"] == "administrator"


def test_get_user_nonexistent(temp_service):
    user = temp_service.get_user_by_username("missing_user")
    assert user is None


def test_calculate_custom_quota(temp_service):
    result = temp_service.calculate_custom_quota("10 + 20")
    assert result == 30
