import sqlite3
import re
import os

# Đường dẫn
EXTRACTED_SQL = r'C:\Workspace2\devhub_computer_vision\backends\devhubocr\db_local\database_extracted.sql'
NEW_DB = r'C:\Workspace2\devhub_computer_vision\backends\devhubocr\db_local\database_rebuilt.db'

print("=== TÁI TẠO DATABASE TỪ SQL TRÍCH XUẤT ===\n")

# Đọc file SQL đã trích xuất
if not os.path.exists(EXTRACTED_SQL):
    print(f"✗ Không tìm thấy file: {EXTRACTED_SQL}")
    exit(1)

with open(EXTRACTED_SQL, 'r', encoding='utf-8', errors='ignore') as f:
    sql_content = f.read()

# Tách các CREATE TABLE statements
create_statements = []
lines = sql_content.split('='*50)

for section in lines:
    # Tìm CREATE TABLE
    matches = re.findall(r'CREATE TABLE[^;]+\)', section, re.IGNORECASE | re.DOTALL)
    create_statements.extend(matches)
    
    # Tìm CREATE INDEX
    index_matches = re.findall(r'CREATE INDEX[^;]+\)', section, re.IGNORECASE | re.DOTALL)
    create_statements.extend(index_matches)

print(f"✓ Tìm thấy {len(create_statements)} statements SQL\n")

# Tạo database mới
if os.path.exists(NEW_DB):
    os.remove(NEW_DB)

conn = sqlite3.connect(NEW_DB)
cursor = conn.cursor()

# Schema chuẩn từ những gì trích xuất được
schemas = {
    'pipelines': """CREATE TABLE pipelines (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        workflow_json TEXT NOT NULL,
        created_at TEXT,
        updated_at TEXT
    )""",
    
    'qaa0_bot_send_history': """CREATE TABLE qaa0_bot_send_history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        bot_id TEXT NOT NULL,
        chat_id TEXT,
        file_url TEXT,
        caption TEXT,
        response TEXT,
        status_code INTEGER,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP
    )""",
    
    'qaa0_bot_configs': """CREATE TABLE qaa0_bot_configs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        bot_id TEXT NOT NULL UNIQUE,
        token TEXT NOT NULL,
        base_url TEXT NOT NULL DEFAULT 'https://api.telegram.org',
        callback_url TEXT,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP
    )""",
    
    'notifications': """CREATE TABLE notifications (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT NOT NULL,
        message TEXT NOT NULL,
        data TEXT,
        created_at TEXT NOT NULL,
        actor_id INTEGER
    )""",
    
    'notification_user': """CREATE TABLE notification_user (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        notification_id INTEGER NOT NULL,
        user_id INTEGER NOT NULL,
        is_read INTEGER DEFAULT 0,
        read_at TEXT
    )""",
    
    'menus': """CREATE TABLE menus (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT NOT NULL,
        icon TEXT,
        path TEXT,
        parent_id INTEGER DEFAULT NULL,
        roles TEXT,
        order_num INTEGER DEFAULT 0,
        is_active INTEGER DEFAULT 1,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP
    )""",
    
    'users': """CREATE TABLE users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        email TEXT NOT NULL UNIQUE,
        password_hash TEXT NOT NULL,
        first_name TEXT,
        last_name TEXT,
        created_at TEXT,
        avatar TEXT,
        business_name TEXT,
        business_address TEXT
    )""",
    
    'roles': """CREATE TABLE roles (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        role TEXT NOT NULL UNIQUE,
        description TEXT
    )""",
    
    'users_roles': """CREATE TABLE users_roles (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL,
        role TEXT NOT NULL
    )""",
    
    'path_roles': """CREATE TABLE path_roles (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        path TEXT NOT NULL,
        role TEXT NOT NULL
    )""",
    
    'schema_migrations': """CREATE TABLE schema_migrations (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        filename TEXT NOT NULL UNIQUE,
        applied_at TEXT NOT NULL
    )""",
    
    'sqlite_sequence': """CREATE TABLE sqlite_sequence(name, seq)"""
}

# Tạo các bảng
print("Đang tạo các bảng...")
created_count = 0
for table_name, schema in schemas.items():
    try:
        cursor.execute(schema)
        print(f"  ✓ Tạo bảng: {table_name}")
        created_count += 1
    except sqlite3.Error as e:
        print(f"  ✗ Lỗi bảng {table_name}: {e}")

# Tạo các indexes
print("\nĐang tạo indexes...")
indexes = [
    "CREATE INDEX idx_notifications_created_at ON notifications(created_at)",
    "CREATE INDEX idx_users_roles_user ON users_roles(user_id)",
    "CREATE INDEX idx_path_roles_path ON path_roles(path)",
    "CREATE INDEX idx_notification_user_user ON notification_user(user_id)"
]

for idx in indexes:
    try:
        cursor.execute(idx)
        print(f"  ✓ {idx.split('CREATE INDEX ')[1].split(' ON')[0]}")
    except sqlite3.Error as e:
        print(f"  ⚠ {e}")

conn.commit()

# Kiểm tra kết quả
print("\n" + "="*60)
print("KẾT QUẢ:")
cursor.execute("SELECT COUNT(*) FROM sqlite_master WHERE type='table';")
table_count = cursor.fetchone()[0]
print(f"✓ Tổng số bảng: {table_count}")

cursor.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;")
tables = cursor.fetchall()
print("\nDanh sách bảng:")
for table in tables:
    cursor.execute(f"PRAGMA table_info({table[0]});")
    columns = cursor.fetchall()
    print(f"  📋 {table[0]} ({len(columns)} cột)")

# Test integrity
cursor.execute("PRAGMA integrity_check;")
result = cursor.fetchone()
if result[0] == 'ok':
    print(f"\n✓✓✓ DATABASE HOÀN TOÀN LÀNH MẠNH! ✓✓✓")
else:
    print(f"\n⚠ Integrity: {result[0]}")

conn.close()

print(f"\n📁 File database mới: {NEW_DB}")
print("\n" + "="*60)
print("\n🎯 BƯỚC TIẾP THEO:")
print("1. Kiểm tra database_rebuilt.db")
print("2. Nếu OK, đổi tên:")
print("   - database.db → database_old.db")
print("   - database_rebuilt.db → database.db")
print("3. Chạy lại ứng dụng!")
print("\n⚠️ Lưu ý: Database mới chỉ có cấu trúc, KHÔNG có dữ liệu cũ")
print("   Nếu cần dữ liệu cũ, phải dùng SQLite CLI .recover")