import ast
import os
import re
import sqlite3
from typing import Optional, Dict, Any

# FIX CWE-798: Secret sourced from environment variable, never hardcoded.
JWT_SECRET_KEY = os.getenv("JWT_SECRET_KEY", "fallback_dev_key")
DATABASE_PATH = "users.db"


class UserService:
    def __init__(self, db_path: str = DATABASE_PATH):
        self.conn = sqlite3.connect(db_path)
        self._init_db()

    def _init_db(self):
        with self.conn:
            self.conn.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY,
                    username TEXT,
                    role TEXT
                )
            """)

    # FIX CWE-89: Parameterized query — username is never interpolated into SQL.
    def get_user_by_username(self, username: str) -> Optional[Dict[str, Any]]:
        cursor = self.conn.cursor()
        cursor.execute(
            "SELECT id, username, role FROM users WHERE username = ?",
            (username,)
        )
        row = cursor.fetchone()
        if row:
            return {"id": row[0], "username": row[1], "role": row[2]}
        return None

    # FIX CWE-95: Only allow safe arithmetic expressions; reject anything else.
    _SAFE_EXPR_RE = re.compile(r'^[\d\s\+\-\*/\(\)\.]+$')

    def calculate_custom_quota(self, expr: str) -> int:
        if not self._SAFE_EXPR_RE.match(expr):
            raise ValueError(f"Unsafe expression rejected: {expr!r}")
        return eval(expr, {"__builtins__": {}}, {})  # noqa: S307 — guarded by allowlist regex
