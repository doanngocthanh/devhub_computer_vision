PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;
CREATE TABLE schema_migrations (id INTEGER PRIMARY KEY AUTOINCREMENT, filename TEXT NOT NULL UNIQUE, applied_at TEXT NOT NULL);
INSERT INTO schema_migrations VALUES(1,'20251118__create_menus_table.sql','Tue Nov 18 09:40:56 UTC 2025');
INSERT INTO schema_migrations VALUES(2,'20251118__create_roles_tables.sql','Tue Nov 18 09:42:18 UTC 2025');
INSERT INTO schema_migrations VALUES(3,'20251119__unique_path_roles.sql','Wed Nov 19 03:47:35 UTC 2025');
INSERT INTO schema_migrations VALUES(4,'20251119__create_menus_table.sql','Wed Nov 19 08:06:51 UTC 2025');
INSERT INTO schema_migrations VALUES(5,'20251119__create_notifications_table.sql','Fri Nov 21 02:59:38 UTC 2025');
INSERT INTO schema_migrations VALUES(6,'20251119__fix_parent_menu_paths.sql','Fri Nov 21 02:59:38 UTC 2025');
INSERT INTO schema_migrations VALUES(7,'20251120__add_file_id_to_send_history.sql','Fri Nov 21 03:00:58 UTC 2025');
INSERT INTO schema_migrations VALUES(8,'20251120__create_bot_configs_table.sql','Fri Nov 21 03:00:59 UTC 2025');
INSERT INTO schema_migrations VALUES(9,'20251120__create_bot_send_history_table.sql','Fri Nov 21 03:00:59 UTC 2025');
INSERT INTO schema_migrations VALUES(10,'20251121_create_pipeline_tables.sql','Fri Nov 21 03:47:44 UTC 2025');
CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, first_name TEXT, last_name TEXT, created_at TEXT, avatar TEXT, business_name TEXT, business_id TEXT, location TEXT, public_profile INTEGER DEFAULT 0);
INSERT INTO users VALUES(1,'admin@gmail.com','$2a$10$ue3pGIx4PiQ/M8vdvVZHUejWFRHmVxC1LtaKo6kYBS7VbMMvxLpsS','Đoàn','Ngọc Thành','2025-11-18T09:43:12.582084973Z','user-1-avatar.jpg','Đoàn Ngọc Thành','560afc32','Hồ Chí Minh, Việt Nam',1);
INSERT INTO users VALUES(2,'dnt.doanngocthanh@gmail.com','$2a$10$.4B4w8IoBe.jcgV7bqHF6O1Dt3nIcF1i9aktoQXej37hrivJIMk/C','Đoàn','Ngọc Thành','2025-11-19T04:12:05.170097531Z',NULL,NULL,NULL,NULL,0);
INSERT INTO users VALUES(3,'dnt1.doanngocthanh@gmail.com','$2a$10$2AXnv6V4Dmgr4TikFPSDkOGzo0/8LkrPpGtQTaZN6ZvsGxA6tG8KW','Thành','Đoàn Ngọc','2025-11-20T08:12:57.448675271Z',NULL,NULL,NULL,NULL,0);
CREATE TABLE path_roles (id INTEGER PRIMARY KEY AUTOINCREMENT, path TEXT NOT NULL, role TEXT NOT NULL);
INSERT INTO path_roles VALUES(7,'AA.A0.AAA0_0100','IT');
INSERT INTO path_roles VALUES(12,'AA.A0.AAA0_0102','IT');
INSERT INTO path_roles VALUES(16,'AA.A0.AAA0_0102.','IT');
CREATE TABLE users_roles (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, role TEXT NOT NULL);
INSERT INTO users_roles VALUES(13,2,'IT');
INSERT INTO users_roles VALUES(14,1,'IT');
INSERT INTO users_roles VALUES(15,1,'ADMIN');
CREATE TABLE roles (id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL UNIQUE, description TEXT);
INSERT INTO roles VALUES(1,'IT','Highest-privilege administrator');
INSERT INTO roles VALUES(3,'ADMIN','Là administration ');
CREATE TABLE menus (
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
INSERT INTO menus VALUES(1,'Hệ Thống','fa-home','/AA/A0/AAA0_0102',NULL,'IT',0,1,'2025-11-19 08:09:22','2025-11-19 08:09:22');
INSERT INTO menus VALUES(2,'Chỉnh sửa hồ sơ','user','/FM/A0/FMA0_0100/',NULL,'',0,1,'2025-11-19 08:09:22','2025-11-19 08:09:22');
INSERT INTO menus VALUES(3,'Quản Lý Menu','fas fa-bars or fa-solid fa-bars','/AA/A0/AAA0_0102/',1,'',0,1,'2025-11-19 08:34:40','2025-11-19 08:34:40');
INSERT INTO menus VALUES(4,'Computer Vision [JAVA]','fa-eye','/QR',NULL,'',0,1,'2025-11-19 09:36:02','2025-11-19 09:36:02');
INSERT INTO menus VALUES(5,'Quyền Truy Cập','a-solid fa-user-shield','/AA/A0/AAA0_0100',1,'',0,1,'2025-11-19 09:37:05','2025-11-19 09:37:05');
INSERT INTO menus VALUES(7,'Schedule Job','fa-clock','',1,'',0,1,'2025-11-19 10:11:44','2025-11-19 10:11:44');
INSERT INTO menus VALUES(8,'Image To Text (Tess4J)','fas fa-exchange-alt','/CP/A0/CPA0_0100',4,'',0,1,'2025-11-20 03:11:28','2025-11-20 03:11:28');
INSERT INTO menus VALUES(9,'Đánh Nhãn Đối Tượng','fa-solid fa-tags','',4,'',0,1,'2025-11-20 03:16:09','2025-11-20 03:16:09');
INSERT INTO menus VALUES(10,'Tạo Dự Án Đánh Nhãn','fa-solid fa-folder-plus','',4,'',0,1,'2025-11-20 03:17:21','2025-11-20 03:17:21');
INSERT INTO menus VALUES(11,'[1] Model Bot','fa-solid fa-gear','/QA/A0/QAA0_0100/',13,'',0,1,'2025-11-20 04:44:19','2025-11-20 04:44:19');
INSERT INTO menus VALUES(12,'Workflows','fa-tasks','',NULL,'',0,1,'2025-11-20 09:19:41','2025-11-20 09:19:41');
INSERT INTO menus VALUES(13,'Telegram Bot','fa-brands fa-telegram','',12,'',0,1,'2025-11-20 09:51:50','2025-11-20 09:51:50');
INSERT INTO menus VALUES(14,'[2] Test Send File','fa-solid fa-flask','/QA/A0/QAA0_0101/',13,'',0,1,'2025-11-20 09:55:19','2025-11-20 09:55:19');
INSERT INTO menus VALUES(15,'OCR Pipeline','fa-solid fa-timeline','/A0/WLA0_0100',NULL,'',0,1,'2025-11-20 10:16:54','2025-11-20 10:16:54');
INSERT INTO menus VALUES(16,'Model AI','fas fa-microchip','',1,'',0,1,'2025-11-21 03:26:03','2025-11-21 03:26:03');
INSERT INTO menus VALUES(17,'Upload Model','fas fa-upload','',16,'',0,1,'2025-11-21 03:34:47','2025-11-21 03:34:47');
INSERT INTO menus VALUES(18,'Pull Model','fas fa-download','',16,'',0,1,'2025-11-21 03:36:02','2025-11-21 03:36:02');
INSERT INTO menus VALUES(19,'Computer Vision [PyThon]','fa-eye','',NULL,'',0,1,'2025-11-21 07:35:03','2025-11-21 07:35:03');
CREATE TABLE notifications (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, message TEXT NOT NULL, data TEXT, created_at TEXT NOT NULL, actor_id INTEGER);
INSERT INTO notifications VALUES(1,'Thông báo thử','Đây là thông báo test.','','2025-11-19T10:39:08.043392058Z',NULL);
INSERT INTO notifications VALUES(2,'Thông báo thử','Đây là thông báo test.','','2025-11-19T10:40:06.672303860Z',NULL);
INSERT INTO notifications VALUES(3,'Thông báo thử','Đây là thông báo test.','','2025-11-19T10:41:31.914895446Z',NULL);
INSERT INTO notifications VALUES(4,'Thông báo thử','Đây là thông báo test.','','2025-11-19T10:46:24.449706130Z',NULL);
INSERT INTO notifications VALUES(5,'Thông báo thử','Đây là thông báo test.','','2025-11-20T02:15:54.600178682Z',NULL);
INSERT INTO notifications VALUES(6,'Thông báo thử','Đây là thông báo test.','','2025-11-20T02:32:51.450378743Z',NULL);
INSERT INTO notifications VALUES(7,'[Login] Thông báo đăng nhập','Đăng nhập thành công từ ip: 0:0:0:0:0:0:0:1','','2025-11-20T07:56:00.395859531Z',NULL);
INSERT INTO notifications VALUES(8,'Thông báo thử','Đây là thông báo test.','','2025-11-20T07:56:17.053895364Z',NULL);
INSERT INTO notifications VALUES(9,'[Login] Thông báo đăng nhập','Đăng nhập thành công từ ip: 0:0:0:0:0:0:0:1','','2025-11-20T07:56:37.786881934Z',NULL);
INSERT INTO notifications VALUES(10,'[Login] Thông báo đăng nhập','Đăng nhập thành công từ ip: 0:0:0:0:0:0:0:1','','2025-11-20T07:57:55.958570316Z',NULL);
INSERT INTO notifications VALUES(11,'[Login] Thông báo đăng nhập','Đăng nhập thành công từ ip: 0:0:0:0:0:0:0:1','','2025-11-20T08:13:02.935775387Z',NULL);
INSERT INTO notifications VALUES(12,'[Login] Thông báo đăng nhập','Đăng nhập thành công từ ip: 0:0:0:0:0:0:0:1','','2025-11-20T08:14:23.075214466Z',NULL);
INSERT INTO notifications VALUES(13,'[Login] Thông báo đăng nhập','Đăng nhập thành công từ ip: 0:0:0:0:0:0:0:1','','2025-11-20T08:36:53.789780965Z',NULL);
INSERT INTO notifications VALUES(14,'[Login] Thông báo đăng nhập','Đăng nhập thành công từ ip: 0:0:0:0:0:0:0:1','','2025-11-20T09:37:44.772581838Z',NULL);
INSERT INTO notifications VALUES(15,'Thông báo thử','Đây là thông báo test.','','2025-11-21T03:31:33.695155616Z',NULL);
INSERT INTO notifications VALUES(16,'Thông báo thử','Đây là thông báo test.','','2025-11-21T09:24:45.701167667Z',NULL);
INSERT INTO notifications VALUES(17,'[Login] Thông báo đăng nhập','Đăng nhập thành công từ ip: 27.78.34.172','','2025-11-22T07:20:45.615296483Z',NULL);
CREATE TABLE notification_user (id INTEGER PRIMARY KEY AUTOINCREMENT, notification_id INTEGER NOT NULL, user_id INTEGER NOT NULL, is_read INTEGER DEFAULT 0, read_at TEXT);
INSERT INTO notification_user VALUES(1,1,1,1,'2025-11-20T02:32:39.902402352Z');
INSERT INTO notification_user VALUES(2,2,1,1,'2025-11-20T02:32:46.688794353Z');
INSERT INTO notification_user VALUES(3,3,1,1,'2025-11-20T06:49:49.471416214Z');
INSERT INTO notification_user VALUES(4,4,1,1,'2025-11-20T02:30:53.279525458Z');
INSERT INTO notification_user VALUES(5,5,1,1,'2025-11-20T06:49:46.025188055Z');
INSERT INTO notification_user VALUES(6,6,1,1,'2025-11-20T06:49:42.644002823Z');
INSERT INTO notification_user VALUES(7,7,1,1,'2025-11-20T07:56:24.165157073Z');
INSERT INTO notification_user VALUES(8,8,1,1,'2025-11-20T07:56:21.623358559Z');
INSERT INTO notification_user VALUES(9,9,1,1,'2025-11-20T07:57:38.616591994Z');
INSERT INTO notification_user VALUES(10,10,2,1,'2025-11-20T07:58:15.609390004Z');
INSERT INTO notification_user VALUES(11,11,3,0,NULL);
INSERT INTO notification_user VALUES(12,12,1,1,'2025-11-20T08:36:34.439500547Z');
INSERT INTO notification_user VALUES(13,13,1,1,'2025-11-20T09:29:16.211846420Z');
INSERT INTO notification_user VALUES(14,14,1,1,'2025-11-21T03:31:44.468839044Z');
INSERT INTO notification_user VALUES(15,15,1,1,'2025-11-21T03:31:41.031146635Z');
INSERT INTO notification_user VALUES(16,16,1,1,'2025-11-21T09:24:57.194855979Z');
INSERT INTO notification_user VALUES(17,17,1,0,NULL);
CREATE TABLE qaa0_bot_configs (
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
INSERT INTO qaa0_bot_configs VALUES(1,'consenluutru_bot','7208604161:AAExB0QL6eg1Hkaw3-iMxfEMnvEtVO6N3sI','https://api.telegram.org',NULL,'Done! Congratulations on your new bot. You will find it at t.me/consenluutru_bot. You can now add a description, about section and profile picture for your bot, see /help for a list of commands. By the way, when you''ve finished creating your cool bot, ping our Bot Support if you want a better username for it. Just make sure the bot is fully operational before you do this.',1,'2025-11-20 09:39:47',NULL);
CREATE TABLE qaa0_bot_send_history (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  bot_id TEXT NOT NULL,
  chat_id TEXT,
  file_url TEXT,
  caption TEXT,
  response TEXT,
  status_code INTEGER,
  created_at TEXT DEFAULT CURRENT_TIMESTAMP
, file_id TEXT);
INSERT INTO qaa0_bot_send_history VALUES(1,'consenluutru_bot','5717458324','blob:https://github.com/eb019e96-fea9-49a6-a1ed-e3951e2c7398','Test','{"ok":false,"error_code":400,"description":"Bad Request: invalid file HTTP URL specified: Wrong port number specified in the URL"}',400,'2025-11-20 09:43:02',NULL);
INSERT INTO qaa0_bot_send_history VALUES(2,'consenluutru_bot','5717458324','https://getsamplefiles.com/download/pdf/sample-1.pdf','Test','{"ok":true,"result":{"message_id":36,"from":{"id":7208604161,"is_bot":true,"first_name":"Con Sen L\u01b0u Tr\u1eef","username":"consenluutru_bot"},"chat":{"id":5717458324,"first_name":"Ng\u1ecdc Th\u00e0nh \u0110o\u00e0n","username":"ngocthanhdoanfree","type":"private"},"date":1763631839,"document":{"file_name":"sample-1.pdf","mime_type":"application/pdf","file_id":"BQACAgQAAxkDAAMkaR7i3xmuXH4-RbuoywoR9htJsbQAApIJAAIrSPxQgBrxoMxVuok2BA","file_unique_id":"AgADkgkAAitI_FA","file_size":69988},"caption":"Test"}}',200,'2025-11-20 09:44:00',NULL);
INSERT INTO qaa0_bot_send_history VALUES(3,'consenluutru_bot','5717458324','fa-regular-400.woff2',NULL,'{"ok":true,"result":{"message_id":37,"from":{"id":7208604161,"is_bot":true,"first_name":"Con Sen L\u01b0u Tr\u1eef","username":"consenluutru_bot"},"chat":{"id":5717458324,"first_name":"Ng\u1ecdc Th\u00e0nh \u0110o\u00e0n","username":"ngocthanhdoanfree","type":"private"},"date":1763632843,"document":{"file_name":"fa-regular-400.woff2","mime_type":"font/woff2","file_id":"BQACAgUAAxkDAAMlaR7my55z7yyCakqddju5X3HeFScAAskaAAK8FvhUfmgI6lfeHkc2BA","file_unique_id":"AgADyRoAArwW-FQ","file_size":25452}}}',200,'2025-11-20 10:00:43',NULL);
INSERT INTO qaa0_bot_send_history VALUES(4,'consenluutru_bot','5717458324','CV_VN_DoanNgocThanh-2.pdf',NULL,'{"ok":true,"result":{"message_id":38,"from":{"id":7208604161,"is_bot":true,"first_name":"Con Sen L\u01b0u Tr\u1eef","username":"consenluutru_bot"},"chat":{"id":5717458324,"first_name":"Ng\u1ecdc Th\u00e0nh \u0110o\u00e0n","username":"ngocthanhdoanfree","type":"private"},"date":1763632871,"document":{"file_name":"CV_VN_DoanNgocThanh-2.pdf","mime_type":"application/pdf","file_id":"BQACAgUAAxkDAAMmaR7m5u9sM-hAVoB7758acz7tPzIAAsoaAAK8FvhUxzahBb1YOFw2BA","file_unique_id":"AgADyhoAArwW-FQ","file_size":3917254}}}',200,'2025-11-20 10:01:11',NULL);
INSERT INTO qaa0_bot_send_history VALUES(5,'consenluutru_bot','5717458324','fa-solid-900.woff2',NULL,'{"ok":true,"result":{"message_id":39,"from":{"id":7208604161,"is_bot":true,"first_name":"Con Sen L\u01b0u Tr\u1eef","username":"consenluutru_bot"},"chat":{"id":5717458324,"first_name":"Ng\u1ecdc Th\u00e0nh \u0110o\u00e0n","username":"ngocthanhdoanfree","type":"private"},"date":1763633381,"document":{"file_name":"fa-solid-900.woff2","mime_type":"font/woff2","file_id":"BQACAgUAAxkDAAMnaR7o5Qzu6GJxXXNTmPSwCWFOYSQAAs4aAAK8FvhUA6Cuzjo0nb02BA","file_unique_id":"AgADzhoAArwW-FQ","file_size":156496}}}',200,'2025-11-20 10:09:41','BQACAgUAAxkDAAMnaR7o5Qzu6GJxXXNTmPSwCWFOYSQAAs4aAAK8FvhUA6Cuzjo0nb02BA');
CREATE TABLE pipelines (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    workflow_json TEXT NOT NULL,
    created_at TEXT,
    updated_at TEXT
);
DELETE FROM sqlite_sequence;
INSERT INTO sqlite_sequence VALUES('roles',3);
INSERT INTO sqlite_sequence VALUES('schema_migrations',10);
INSERT INTO sqlite_sequence VALUES('users',3);
INSERT INTO sqlite_sequence VALUES('users_roles',15);
INSERT INTO sqlite_sequence VALUES('path_roles',16);
INSERT INTO sqlite_sequence VALUES('menus',19);
INSERT INTO sqlite_sequence VALUES('notifications',17);
INSERT INTO sqlite_sequence VALUES('notification_user',17);
INSERT INTO sqlite_sequence VALUES('qaa0_bot_configs',1);
INSERT INTO sqlite_sequence VALUES('qaa0_bot_send_history',5);
CREATE INDEX idx_path_roles_path ON path_roles(path);
CREATE INDEX idx_users_roles_user ON users_roles(user_id);
CREATE INDEX idx_notification_user_user ON notification_user(user_id);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
COMMIT;
