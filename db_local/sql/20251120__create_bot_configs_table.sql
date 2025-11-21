-- Create table to store bot configurations (tokens stored but not exposed via APIs)
CREATE TABLE IF NOT EXISTS qaa0_bot_configs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  bot_id TEXT NOT NULL UNIQUE,
  token TEXT NOT NULL,
  base_url TEXT NOT NULL DEFAULT 'https://api.telegram.org',
  callback_url TEXT,
  description TEXT,
  enabled INTEGER DEFAULT 1,
  created_at TEXT DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT
);
