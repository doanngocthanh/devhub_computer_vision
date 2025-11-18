-- Create tables to map paths to roles and users to roles
CREATE TABLE IF NOT EXISTS path_roles (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  path TEXT NOT NULL,
  role TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_path_roles_path ON path_roles(path);

CREATE TABLE IF NOT EXISTS users_roles (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  role TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_roles_user ON users_roles(user_id);

-- Example: make home public (uncomment to enable)
-- INSERT INTO path_roles(path, role) VALUES('AA.A0.AAA0_0100', 'RLZZANY');
