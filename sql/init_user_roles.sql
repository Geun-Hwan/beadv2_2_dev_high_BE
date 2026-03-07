-- Signup/login role bootstrap for user-service

CREATE SCHEMA IF NOT EXISTS "user";

CREATE TABLE IF NOT EXISTS "user".role (
    id   UUID PRIMARY KEY,
    name VARCHAR(10) NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_user_role_name'
          AND conrelid = '"user".role'::regclass
    ) THEN
        ALTER TABLE "user".role
            ADD CONSTRAINT uk_user_role_name UNIQUE (name);
    END IF;
END $$;

INSERT INTO "user".role (id, name) VALUES
    ('00000000-0000-0000-0000-000000000001', 'USER'),
    ('00000000-0000-0000-0000-000000000002', 'SELLER'),
    ('00000000-0000-0000-0000-000000000003', 'ADMIN')
ON CONFLICT (name) DO NOTHING;
