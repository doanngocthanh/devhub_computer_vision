
-- Create menus table for dynamic sidebar
CREATE TABLE IF NOT EXISTS menus (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  icon TEXT,
  path TEXT,
  parent_id INTEGER,
  roles TEXT
);
