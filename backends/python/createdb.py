from __future__ import annotations
import os
import sqlite3
import threading
from contextlib import contextmanager
from typing import Any, Dict, Iterable, List, Optional, Tuple

#!/usr/bin/env python3
"""
createdb.py

SQLite helper for the project.
Tạo DB mặc định tại /workspaces/devhub_computer_vision/db_local/database.db
Cung cấp các hàm CRUD và truy vấn dùng chung trong dự án.
"""



DEFAULT_DIR = "/workspaces/devhub_computer_vision/db_local"
DEFAULT_DB = os.path.join(DEFAULT_DIR, "database.db")


def _ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)


def _row_to_dict(row: sqlite3.Row) -> Dict[str, Any]:
    return dict(row) if row is not None else None


class Database:
    """
    Database wrapper để sử dụng chung trong dự án.

    Ví dụ:
        db = Database()  # sử dụng DB mặc định
        db.init_schema("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, name TEXT);")
        user_id = db.insert("users", {"name": "Alice"})
        rows = db.all("users")
    """

    def __init__(self, db_path: str = DEFAULT_DB) -> None:
        _ensure_dir(os.path.dirname(db_path))
        self.db_path = db_path
        self._conn: Optional[sqlite3.Connection] = None
        self._lock = threading.RLock()
        self.connect()

    def connect(self) -> None:
        """Mở kết nối nếu chưa mở."""
        with self._lock:
            if self._conn is None:
                self._conn = sqlite3.connect(
                    self.db_path,
                    check_same_thread=False,
                    detect_types=sqlite3.PARSE_DECLTYPES | sqlite3.PARSE_COLNAMES,
                )
                self._conn.row_factory = sqlite3.Row
                # enable foreign keys
                self._conn.execute("PRAGMA foreign_keys = ON;")

    def close(self) -> None:
        with self._lock:
            if self._conn is not None:
                try:
                    self._conn.close()
                finally:
                    self._conn = None

    def init_schema(self, sql_script: str) -> None:
        """
        Tạo hoặc cập nhật schema bằng SQL script (có thể chứa nhiều câu lệnh).
        Ví dụ: db.init_schema(open('schema.sql').read())
        """
        with self._lock:
            assert self._conn is not None
            self._conn.executescript(sql_script)
            self._conn.commit()

    def execute(self, sql: str, params: Iterable = (), commit: bool = True) -> sqlite3.Cursor:
        """Chạy câu lệnh SQL (INSERT/UPDATE/DELETE hoặc bất kỳ lệnh nào)."""
        with self._lock:
            assert self._conn is not None
            cur = self._conn.execute(sql, tuple(params))
            if commit:
                self._conn.commit()
            return cur

    def query(self, sql: str, params: Iterable = ()) -> List[Dict[str, Any]]:
        """Chạy SELECT SQL trả về danh sách dict."""
        with self._lock:
            assert self._conn is not None
            cur = self._conn.execute(sql, tuple(params))
            rows = cur.fetchall()
            return [dict(r) for r in rows]

    def fetchone(self, sql: str, params: Iterable = ()) -> Optional[Dict[str, Any]]:
        """Chạy SELECT SQL trả về một hàng (hoặc None)."""
        with self._lock:
            assert self._conn is not None
            cur = self._conn.execute(sql, tuple(params))
            row = cur.fetchone()
            return _row_to_dict(row)

    def insert(self, table: str, data: Dict[str, Any]) -> int:
        """
        Insert một record vào bảng.
        Trả về lastrowid.
        """
        if not data:
            raise ValueError("data không được rỗng")
        cols = ", ".join(data.keys())
        placeholders = ", ".join("?" for _ in data)
        sql = f"INSERT INTO {table} ({cols}) VALUES ({placeholders})"
        with self._lock:
            assert self._conn is not None
            cur = self._conn.execute(sql, tuple(data.values()))
            self._conn.commit()
            return cur.lastrowid

    def update(self, table: str, data: Dict[str, Any], where: str, where_params: Iterable = ()) -> int:
        """
        Update bản ghi.
        Trả về số hàng bị ảnh hưởng.
        where là chuỗi điều kiện (vd "id = ? AND status = ?"), where_params là các giá trị tương ứng.
        """
        if not data:
            raise ValueError("data không được rỗng")
        set_expr = ", ".join(f"{k}=?" for k in data.keys())
        sql = f"UPDATE {table} SET {set_expr} WHERE {where}"
        params = tuple(data.values()) + tuple(where_params)
        with self._lock:
            assert self._conn is not None
            cur = self._conn.execute(sql, params)
            self._conn.commit()
            return cur.rowcount

    def delete(self, table: str, where: str, where_params: Iterable = ()) -> int:
        """
        Xóa bản ghi theo điều kiện.
        Trả về số hàng bị xóa.
        """
        sql = f"DELETE FROM {table} WHERE {where}"
        with self._lock:
            assert self._conn is not None
            cur = self._conn.execute(sql, tuple(where_params))
            self._conn.commit()
            return cur.rowcount

    def get(self, table: str, where: str, where_params: Iterable = ()) -> Optional[Dict[str, Any]]:
        """
        Lấy một bản ghi từ bảng theo điều kiện.
        """
        sql = f"SELECT * FROM {table} WHERE {where} LIMIT 1"
        return self.fetchone(sql, where_params)

    def all(self, table: str, where: Optional[str] = None, where_params: Iterable = ()) -> List[Dict[str, Any]]:
        """
        Lấy tất cả bản ghi từ bảng (có thể có điều kiện).
        """
        if where:
            sql = f"SELECT * FROM {table} WHERE {where}"
            return self.query(sql, where_params)
        return self.query(f"SELECT * FROM {table}")

    @contextmanager
    def transaction(self):
        """
        Context manager cho transaction.
        Ví dụ:
            with db.transaction():
                db.insert(...)
                db.update(...)
        """
        with self._lock:
            assert self._conn is not None
            cur = self._conn.cursor()
            try:
                cur.execute("BEGIN")
                yield
                self._conn.commit()
            except Exception:
                self._conn.rollback()
                raise

    def __enter__(self) -> "Database":
        self.connect()
        return self

    def __exit__(self, exc_type, exc, tb) -> None:
        self.close()


# Singleton mặc định cho toàn dự án
_default_db: Optional[Database] = None


def get_db(path: str = DEFAULT_DB) -> Database:
    global _default_db
    if _default_db is None or _default_db.db_path != path:
        _default_db = Database(path)
    return _default_db


# Nếu file này được chạy trực tiếp thì tạo thư mục và DB trống
if __name__ == "__main__":
    db = get_db()
    # ví dụ khởi tạo bảng đơn giản nếu cần
    sample_schema = """
    CREATE TABLE IF NOT EXISTS example (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
    """
    db.init_schema(sample_schema)
    print("Database initialized at", db.db_path)