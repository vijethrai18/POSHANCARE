-- ==============================================================================
-- POSHANCARE Safe Database Migration: Update users_role_check Constraint
-- Target Database: poshancare_db
-- Target Table: users
-- Description: Updates the users_role_check constraint to allow the 'ADMIN' role
-- along with existing 'WORKER' and 'PARENT' roles.
-- Preserves all existing records in users, children, growth_records, nutrition_records, and followups.
-- ==============================================================================

-- Step 1: Safely drop the existing outdated check constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

-- Step 2: Add the updated check constraint explicitly permitting WORKER, PARENT, and ADMIN
ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('WORKER', 'PARENT', 'ADMIN'));

-- Step 3: Verification queries
-- Check the constraint definition:
-- SELECT conname, pg_get_constraintdef(oid)
-- FROM pg_constraint
-- WHERE conrelid = 'users'::regclass AND conname = 'users_role_check';

-- Verify existing users remain unchanged:
-- SELECT id, full_name, mobile, role, status FROM users;
