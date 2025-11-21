-- Add a UNIQUE index to prevent duplicate (path, role) pairs
CREATE UNIQUE INDEX IF NOT EXISTS ux_path_roles_path_role ON path_roles(path, role);
-- Clean up any exact duplicates (keeps lowest id) — safe to run again
DELETE FROM path_roles WHERE id NOT IN (SELECT MIN(id) FROM path_roles GROUP BY path, role);
