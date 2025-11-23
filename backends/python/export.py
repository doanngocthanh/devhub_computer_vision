from __future__ import annotations
import argparse
import os
import shutil
import subprocess
import sys
import tempfile
from collections import deque
from pathlib import Path
import gzip

#!/usr/bin/env python3
"""
epo.py - small helper to locate a SQLite DB in the repo, export its schema+data,
optionally compress the dump, verify head/tail, and test import into a temp DB.

Usage examples:
    python epo.py                # auto-find DBs, pick first
    python epo.py --db path/to/db --out backup.sql --compress --test-import
    python epo.py --list         # list found DBs and exit
"""

DB_PATTERNS = ("*.db", "database.db")


def find_dbs(root: Path) -> list[Path]:
        matches = []
        for p in root.rglob("*"):
                if p.is_file() and (p.suffix == ".db" or p.name == "database.db"):
                        matches.append(p)
        return sorted(matches)


def require_bin(name: str):
        if shutil.which(name) is None:
                print(f"Error: required binary '{name}' not found on PATH. Install it and retry.", file=sys.stderr)
                sys.exit(2)


def dump_db(sqlite_bin: str, db_path: Path, out_path: Path):
        out_path.parent.mkdir(parents=True, exist_ok=True)
        print(f"Dumping {db_path} -> {out_path}")
        with out_path.open("wb") as f:
                p = subprocess.run([sqlite_bin, str(db_path), ".dump"], stdout=f)
                if p.returncode != 0:
                        raise SystemExit(f"sqlite3 returned {p.returncode} during dump")


def compress_file(path: Path):
        gz_path = path.with_suffix(path.suffix + ".gz")
        print(f"Compressing {path} -> {gz_path}")
        # use gzip via Python to avoid external dependency
        with path.open("rb") as src, gzip.open(gz_path, "wb") as dst:
                shutil.copyfileobj(src, dst)
        return gz_path


def print_head_tail(path: Path, head_lines=40, tail_lines=40):
        print(f"\n--- head ({head_lines} lines) ---")
        with path.open("r", encoding="utf-8", errors="replace") as f:
                for i in range(head_lines):
                        line = f.readline()
                        if not line:
                                break
                        print(line.rstrip())
        print(f"\n--- tail ({tail_lines} lines) ---")
        dq = deque(maxlen=tail_lines)
        with path.open("r", encoding="utf-8", errors="replace") as f:
                for line in f:
                        dq.append(line.rstrip())
        for line in dq:
                print(line)
        print("\n")


def test_import(sqlite_bin: str, dump_path: Path):
        with tempfile.NamedTemporaryFile(prefix="epo_test_import_", suffix=".db", delete=False) as tmp:
                tmp_db = Path(tmp.name)
        print(f"Testing import into temporary DB: {tmp_db}")
        # feed dump into sqlite3
        with dump_path.open("rb") as f:
                p = subprocess.run([sqlite_bin, str(tmp_db)], stdin=f)
                if p.returncode != 0:
                        print(f"Import failed with exit code {p.returncode}", file=sys.stderr)
                        return False
        # list tables
        p = subprocess.run([sqlite_bin, str(tmp_db), "SELECT name FROM sqlite_master WHERE type='table';"], capture_output=True, text=True)
        print("Tables found in imported DB:")
        print(p.stdout.strip() or "(none)")
        return True


def default_outpath(db_path: Path) -> Path:
        # prefer existing db_local folder sibling if present
        candidate = db_path.parent / "dump.sql"
        return candidate


def main():
        parser = argparse.ArgumentParser(description="Find and export SQLite DBs in repo.")
        parser.add_argument("--root", default=".", help="Repository root to search (default: .)")
        parser.add_argument("--db", type=Path, help="Path to the SQLite DB to export")
        parser.add_argument("--out", type=Path, help="Output SQL file (default: <dbdir>/dump.sql)")
        parser.add_argument("--compress", action="store_true", help="Compress the dump to .gz after exporting")
        parser.add_argument("--test-import", action="store_true", help="Attempt to import dump into a temp SQLite DB and list tables")
        parser.add_argument("--list", action="store_true", help="Only list found DBs and exit")
        args = parser.parse_args()

        root = Path(args.root).resolve()
        require_bin("sqlite3")

        dbs = find_dbs(root)
        if args.list:
                if not dbs:
                        print("No DB files found.")
                        return
                print("Found DB files:")
                for p in dbs:
                        print(f" - {p}")
                return

        db_path = args.db
        if db_path:
                db_path = db_path.resolve()
                if not db_path.is_file():
                        raise SystemExit(f"Provided DB path does not exist: {db_path}")
        else:
                if not dbs:
                        raise SystemExit("No SQLite DB files found in repository root.")
                if len(dbs) > 1:
                        print("Multiple DB files found; selecting the first one:")
                        for i, p in enumerate(dbs):
                                print(f"{i+1}. {p}")
                        db_path = dbs[0]
                        print(f"Auto-selected: {db_path}")
                else:
                        db_path = dbs[0]
                        print(f"Found DB: {db_path}")

        out_path = args.out or default_outpath(db_path)
        dump_db("sqlite3", db_path, out_path)

        if args.compress:
                gz = compress_file(out_path)
                print(f"Compressed dump written to: {gz}")

        print_head_tail(out_path)

        if args.test_import:
                ok = test_import("sqlite3", out_path)
                if not ok:
                        raise SystemExit("Test import failed.")
                print("Test import completed successfully.")


if __name__ == "__main__":
        main()