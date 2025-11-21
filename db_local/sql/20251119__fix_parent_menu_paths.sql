-- ========================================
-- Migration: Fix parent menus must not have path
-- Date: 2025-11-19
-- Purpose: Set `path` to NULL for any menu that is a parent (has children).
-- Notes: If your SQLite variant or application layer treats empty string as "no path",
--        uncomment the alternative UPDATE (sets empty string) below.
-- ========================================

BEGIN TRANSACTION;

-- Set path to NULL for menus that have at least one child.
UPDATE menus
SET path = NULL
WHERE id IN (
    SELECT DISTINCT parent_id FROM menus WHERE parent_id IS NOT NULL
);

-- Alternative (if NULL is not accepted by your schema/app):
-- UPDATE menus
-- SET path = ''
-- WHERE id IN (
--     SELECT DISTINCT parent_id FROM menus WHERE parent_id IS NOT NULL
-- );

COMMIT;

-- Verification query (run after migration):
SELECT
    m1.id,
    m1.title,
    m1.icon,
    CASE
        WHEN m1.path IS NULL OR m1.path = '' THEN '[NO PATH - PARENT MENU]'
        ELSE m1.path
    END as path,
    m1.parent_id,
    m2.title as parent_title,
    (SELECT COUNT(*) FROM menus WHERE parent_id = m1.id) as children_count
FROM menus m1
LEFT JOIN menus m2 ON m1.parent_id = m2.id
ORDER BY m1.order_num, m1.id;
