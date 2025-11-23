import sqlite3
import os
import shutil
import subprocess
from datetime import datetime

# Đường dẫn
DB_PATH = r'C:\Workspace2\devhub_computer_vision\backends\devhubocr\db_local\database.db'
RECOVERED_PATH = r'C:\Workspace2\devhub_computer_vision\backends\devhubocr\db_local\database_recovered.db'
SQLITE_CLI = r'sqlite3.exe'  # Hoặc đường dẫn đầy đủ nếu đã tải

print("=== PHỤC HỒI DATABASE NẶNG ===\n")

# Biến theo dõi
method_success = False

# Backup file gốc
if os.path.exists(DB_PATH):
    backup_name = f"database_backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}.db"
    backup_full_path = os.path.join(os.path.dirname(DB_PATH), backup_name)
    shutil.copy2(DB_PATH, backup_full_path)
    print(f"✓ Đã backup: {backup_full_path}\n")

# Xóa file recovered cũ
if os.path.exists(RECOVERED_PATH):
    os.remove(RECOVERED_PATH)

# PHƯƠNG PHÁP 1: Dùng SQLite CLI .recover (TỐT NHẤT)
print("--- Phương pháp 1: SQLite CLI .recover ---")
try:
    # Kiểm tra xem có sqlite3 không
    result = subprocess.run([SQLITE_CLI, '--version'], 
                          capture_output=True, text=True, timeout=5)
    
    if result.returncode == 0:
        print(f"✓ Tìm thấy SQLite CLI: {result.stdout.strip()}")
        
        # Chạy lệnh .recover
        print("Đang chạy .recover...")
        cmd = f'"{SQLITE_CLI}" "{RECOVERED_PATH}" ".recover {DB_PATH}"'
        
        result = subprocess.run(cmd, shell=True, capture_output=True, 
                              text=True, timeout=300)
        
        if result.returncode == 0 and os.path.exists(RECOVERED_PATH):
            print(f"✓✓✓ THÀNH CÔNG với .recover!")
            print(f"File mới: {RECOVERED_PATH}")
            method_success = True
        else:
            print(f"✗ .recover thất bại: {result.stderr}")
            
    else:
        print("✗ Không tìm thấy sqlite3.exe trong PATH")
        print("Cần cài đặt SQLite CLI\n")
        
except FileNotFoundError:
    print("✗ Không tìm thấy sqlite3.exe")
    print("→ Tải tại: https://www.sqlite.org/download.html")
    print("→ Tìm: sqlite-tools-win-x64-*.zip\n")
except Exception as e:
    print(f"✗ Lỗi: {e}\n")

# PHƯƠNG PHÁP 2: Đọc raw binary và trích xuất
if not method_success:
    print("--- Phương pháp 2: Trích xuất raw data ---")
    try:
        with open(DB_PATH, 'rb') as f:
            data = f.read()
        
        # Kiểm tra header
        if data[:16] == b'SQLite format 3\x00':
            print(f"✓ File có SQLite header hợp lệ ({len(data):,} bytes)")
            
            # Tìm các SQL statements trong file
            sql_keywords = [b'CREATE TABLE', b'INSERT INTO', b'CREATE INDEX']
            found_sql = []
            
            for keyword in sql_keywords:
                pos = 0
                while True:
                    pos = data.find(keyword, pos)
                    if pos == -1:
                        break
                    # Trích xuất đoạn xung quanh
                    snippet = data[max(0, pos-50):pos+200]
                    try:
                        text = snippet.decode('utf-8', errors='ignore')
                        if text.strip():
                            found_sql.append(text)
                    except:
                        pass
                    pos += len(keyword)
            
            if found_sql:
                print(f"✓ Tìm thấy {len(found_sql)} SQL statements")
                
                # Lưu vào file text để xem
                sql_file = DB_PATH.replace('.db', '_extracted.sql')
                with open(sql_file, 'w', encoding='utf-8', errors='ignore') as f:
                    for sql in found_sql[:100]:  # Chỉ lưu 100 cái đầu
                        f.write(sql + '\n' + '='*50 + '\n')
                
                print(f"✓ Đã lưu SQL tìm được vào: {sql_file}")
                print("  (Có thể chỉnh sửa tay file này để tạo lại DB)")
            else:
                print("✗ Không tìm thấy SQL statements")
        else:
            print("✗ File không có SQLite header hợp lệ")
            print(f"  Header: {data[:16]}")
            
    except Exception as e:
        print(f"✗ Lỗi đọc file: {e}")

# PHƯƠNG PHÁP 3: Sử dụng connection với pragma đặc biệt
if not method_success:
    print("\n--- Phương pháp 3: Force recovery mode ---")
    try:
        # Thử với các pragma recovery
        conn = sqlite3.connect(DB_PATH)
        conn.execute("PRAGMA writable_schema = ON;")
        conn.execute("PRAGMA integrity_check(1);")
        cursor = conn.cursor()
        
        # Thử lấy danh sách bảng bằng cách khác
        cursor.execute("SELECT * FROM sqlite_master WHERE type='table';")
        tables = cursor.fetchall()
        
        if tables:
            print(f"✓ Tìm thấy {len(tables)} bảng:")
            for table in tables:
                print(f"  - {table[1]}")
        
        conn.close()
        
    except Exception as e:
        print(f"✗ Không thể truy cập: {e}")

# KẾT QUẢ CUỐI CÙNG
print("\n" + "="*60)
if method_success and os.path.exists(RECOVERED_PATH):
    print("✓✓✓ ĐÃ PHỤC HỒI THÀNH CÔNG ✓✓✓")
    
    # Kiểm tra DB mới
    try:
        test_conn = sqlite3.connect(RECOVERED_PATH)
        test_cursor = test_conn.cursor()
        
        test_cursor.execute("PRAGMA integrity_check;")
        result = test_cursor.fetchone()
        print(f"Integrity: {result[0]}")
        
        test_cursor.execute("SELECT COUNT(*) FROM sqlite_master WHERE type='table';")
        table_count = test_cursor.fetchone()[0]
        print(f"Số bảng: {table_count}")
        
        test_conn.close()
    except Exception as e:
        print(f"⚠ Cảnh báo: {e}")
        
else:
    print("❌ CHƯA PHỤC HỒI ĐƯỢC")
    print("\n📋 HƯỚNG DẪN TIẾP THEO:")
    print("\n1️⃣ CÀI SQLITE CLI (Khuyên dùng):")
    print("   - Truy cập: https://www.sqlite.org/download.html")
    print("   - Tải: sqlite-tools-win-x64-*.zip")
    print("   - Giải nén vào thư mục, ví dụ: C:\\sqlite\\")
    print("   - Chạy lệnh:")
    print(f'   C:\\sqlite\\sqlite3.exe "{RECOVERED_PATH}" ".recover {DB_PATH}"')
    
    print("\n2️⃣ DÙNG DB BROWSER (GUI - Dễ dùng):")
    print("   - Tải: https://sqlitebrowser.org/dl/")
    print("   - Mở DB Browser → File → Import → Database from SQL file")
    print("   - Hoặc Tools → Database → Compact Database")
    
    print("\n3️⃣ KIỂM TRA BACKUP:")
    print("   - File Explorer → Chuột phải vào thư mục")
    print("   - Properties → Previous Versions")
    print("   - Restore về version cũ hơn")
    
    print("\n4️⃣ PHẦN MỀM CHUYÊN DỤNG:")
    print("   - Stellar SQLite Recovery")
    print("   - Kernel for SQLite")
    print("   - (Các tool này trả phí nhưng hiệu quả cao)")

print("\n" + "="*60)