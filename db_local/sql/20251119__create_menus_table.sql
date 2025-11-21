-- Migration: create menus table for dynamic sidebar
CREATE TABLE IF NOT EXISTS menus (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  icon TEXT,
  path TEXT,
  parent_id INTEGER DEFAULT NULL,
  roles TEXT,
  order_num INTEGER DEFAULT 0,
  is_active INTEGER DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Optional index for faster lookups by path
CREATE INDEX IF NOT EXISTS idx_menus_path ON menus(path);
