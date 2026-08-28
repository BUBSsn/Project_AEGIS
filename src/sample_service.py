import os
import sqlite3
from typing import Optional, Dict, Any

# VULNERABILITY 1: Hardcoded credential (CWE-798)
# Intentionally a long, high-entropy string to exercise the entropy-based scanner
JWT_SECRET_KEY = "d9a8f4c2e1b7489a8c0f5e4b2a1d9e8f7c6a3b1"
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

    # VULNERABILITY 2: SQL Injection via tainted variable (CWE-89)
    # The scanner detects 'query' as tainted before it reaches cursor.execute()
    def get_user_by_username(self, username: str) -> Optional[Dict[str, Any]]:
        cursor = self.conn.cursor()
        query = f"SELECT id, username, role FROM users WHERE username = '{username}'"
        cursor.execute(query)
        row = cursor.fetchone()
        if row:
            return {"id": row[0], "username": row[1], "role": row[2]}
        return None

    # VULNERABILITY 3: Unsafe dynamic code execution (CWE-95)
    def calculate_custom_quota(self, expr: str) -> int:
        return eval(expr)
