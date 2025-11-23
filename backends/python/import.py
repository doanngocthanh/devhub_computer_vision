#!/usr/bin/env python3
"""Import a SQLite .sql dump (plain or gz) into a new DB using Python's sqlite3.

Usage:
    python import_dump.py --dump db_local/dump.sql --out /tmp/test_import.db --show-tables
"""
from __future__ import annotations
import argparse
import gzip
import os
import sqlite3
from pathlib import Path


def open_dump(path: Path):
    if path.suffix == ".gz":
        return gzip.open(path, "rt", encoding="utf-8", errors="replace")
    return path.open("r", encoding="utf-8", errors="replace")


def import_dump(dump_path: Path, out_db: Path, force: bool = False):
    if out_db.exists() and not force:
        raise SystemExit(f"Output DB already exists: {out_db} -- use --force to overwrite")
    if out_db.exists():
        out_db.unlink()

    print(f"Importing dump '{dump_path}' -> '{out_db}'")
    with open_dump(dump_path) as f:
        sql = f.read()

    conn = sqlite3.connect(str(out_db))
    try:
        conn.executescript(sql)
        conn.commit()
    finally:
        conn.close()


def list_tables(db_path: Path):
    conn = sqlite3.connect(str(db_path))
    try:
        cur = conn.cursor()
        cur.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;")
        rows = cur.fetchall()
        return [r[0] for r in rows]
    finally:
        conn.close()


def main():
    p = argparse.ArgumentParser(description="Import a SQLite SQL dump into a new DB")
    p.add_argument("--dump", type=Path, required=True, help="Path to dump.sql or dump.sql.gz")
    p.add_argument("--out", type=Path, default=Path("/tmp/test_import.db"), help="Output DB path")
    p.add_argument("--force", action="store_true", help="Overwrite output DB if it exists")
    p.add_argument("--show-tables", action="store_true", help="List tables after import")
    args = p.parse_args()

    dump_path: Path = args.dump
    if not dump_path.is_file():
        raise SystemExit(f"Dump file not found: {dump_path}")

    out_db: Path = args.out
    out_db.parent.mkdir(parents=True, exist_ok=True)

    import_dump(dump_path, out_db, force=args.force)

    print("Import completed.")
    if args.show_tables:
        tables = list_tables(out_db)
        print("Tables in imported DB:")
        if not tables:
            print("  (none)")
        else:
            for t in tables:
                print("  -", t)


if __name__ == "__main__":
    main()
