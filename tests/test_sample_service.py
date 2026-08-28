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


# ---------------------------------------------------------------------------
# CWE-798: JWT_SECRET_KEY must not be a hardcoded literal
# ---------------------------------------------------------------------------

def test_jwt_secret_key_not_hardcoded():
    """JWT_SECRET_KEY must come from the environment, not be a literal."""
    expected = os.getenv("JWT_SECRET_KEY", "fallback_dev_key")
    assert JWT_SECRET_KEY == expected, (
        f"JWT_SECRET_KEY appears to be hardcoded. Expected os.getenv value "
        f"'{expected}', got '{JWT_SECRET_KEY}'"
    )


# ---------------------------------------------------------------------------
# CWE-89: SQL injection — parameterized queries must neutralize all payloads
# ---------------------------------------------------------------------------

@pytest.mark.parametrize("payload", [
    "1' OR '1'='1",
    "admin'--",
    "'; DROP TABLE users;--",
])
def test_sql_injection_payload_returns_none(temp_service, payload):
    """Injection payloads must not return rows or raise errors."""
    result = temp_service.get_user_by_username(payload)
    assert result is None, (
        f"SQL injection payload '{payload}' unexpectedly returned a row: {result}"
    )


def test_sql_injection_does_not_destroy_table(temp_service):
    """A DROP TABLE payload must not actually drop the users table."""
    temp_service.get_user_by_username("'; DROP TABLE users;--")
    # If the table were dropped this would raise an OperationalError
    user = temp_service.get_user_by_username("admin_user")
    assert user is not None
    assert user["username"] == "admin_user"


# ---------------------------------------------------------------------------
# Valid query — existing happy-path tests
# ---------------------------------------------------------------------------

def test_get_user_valid(temp_service):
    user = temp_service.get_user_by_username("admin_user")
    assert user is not None
    assert user["username"] == "admin_user"
    assert user["role"] == "administrator"


def test_get_user_nonexistent(temp_service):
    user = temp_service.get_user_by_username("missing_user")
    assert user is None


# ---------------------------------------------------------------------------
# CWE-95: eval() must be replaced with a safe arithmetic evaluator
# ---------------------------------------------------------------------------

def test_calculate_custom_quota(temp_service):
    """Basic arithmetic must still work after replacing eval."""
    result = temp_service.calculate_custom_quota("10 + 20")
    assert result == 30


def test_calculate_custom_quota_multiplication(temp_service):
    result = temp_service.calculate_custom_quota("6 * 7")
    assert result == 42


def test_calculate_custom_quota_complex_expr(temp_service):
    result = temp_service.calculate_custom_quota("(100 - 50) * 2")
    assert result == 100


@pytest.mark.parametrize("dangerous_expr", [
    "__import__('os').system('id')",
    "open('/etc/passwd').read()",
    "__builtins__",
    "exit(0)",
])
def test_calculate_custom_quota_dangerous_expr_raises(temp_service, dangerous_expr):
    """Dangerous expressions must raise ValueError, not execute."""
    with pytest.raises((ValueError, TypeError)):
        temp_service.calculate_custom_quota(dangerous_expr)
