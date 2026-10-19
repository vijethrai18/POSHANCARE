-- ==============================================================================
-- POSHANCARE Safe Database Migration: Phase 3 Worker Approval & Admin Support
-- Target Database: poshancare_db
-- Description: Adds status, centre_name, and password_hash columns to users table.
-- Ensures existing legitimate WORKER records are marked APPROVED so they are NOT locked out.
-- Leaves child, growth, nutrition, and follow-up records completely untouched.
-- ==============================================================================

-- 1. Safely add the status column if it does not already exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20);

-- 2. Safely add centre_name and password_hash columns if they do not exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS centre_name VARCHAR(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- 3. Migrate existing legitimate WORKER accounts to APPROVED
-- Why: Existing workers created prior to self-registration must continue working immediately.
-- Only rows with role = 'WORKER' and a NULL status are updated.
-- No account is automatically granted ADMIN privileges.
UPDATE users
SET status = 'APPROVED'
WHERE role = 'WORKER' AND (status IS NULL OR status = '');

-- 4. Apply NOT NULL constraint and default for status
-- All future records without an explicit status will default to 'PENDING'.
ALTER TABLE users ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE users ALTER COLUMN status SET NOT NULL;

-- 5. Verification query: run this to check existing users after migration
-- SELECT id, full_name, mobile, role, status, centre_name, created_at FROM users;
