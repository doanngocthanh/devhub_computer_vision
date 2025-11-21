-- ========================================
-- Migration: Create notifications and notification_user tables
-- Date: 2025-11-19
-- Purpose: Store notifications and per-user delivery/read state
-- ========================================

BEGIN TRANSACTION;

CREATE TABLE IF NOT EXISTS notifications (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  message TEXT NOT NULL,
  data TEXT,
  created_at TEXT NOT NULL,
  actor_id INTEGER
);

CREATE TABLE IF NOT EXISTS notification_user (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  notification_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  is_read INTEGER DEFAULT 0,
  read_at TEXT,
  FOREIGN KEY(notification_id) REFERENCES notifications(id)
);

CREATE INDEX IF NOT EXISTS idx_notification_user_user ON notification_user(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);

COMMIT;

-- Verification queries:
-- SELECT n.id, n.title, nu.user_id, nu.is_read, nu.read_at, n.created_at FROM notifications n JOIN notification_user nu ON nu.notification_id = n.id ORDER BY n.created_at DESC;
