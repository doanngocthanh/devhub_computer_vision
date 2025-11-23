-- qaa0_bot_send_history definition

CREATE TABLE qaa0_bot_send_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bot_id TEXT NOT NULL,
    chat_id TEXT,
    file_url TEXT,
    file_id TEXT,
    caption TEXT,
    response TEXT,
    status_code INTEGER,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

