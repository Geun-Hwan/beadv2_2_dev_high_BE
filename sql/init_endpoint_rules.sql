-- Gateway endpoint policy bootstrap
-- Tables:
--   public.endpoint
--   public.endpoint_role
--   public.endpoint_rule_version
-- Trigger:
--   endpoint / endpoint_role 변경 시 version 증가 + NOTIFY endpoint_rule_changed

CREATE TABLE IF NOT EXISTS public.endpoint (
    id            UUID PRIMARY KEY,
    path          VARCHAR(255) NOT NULL,
    method        VARCHAR(10)  NOT NULL,
    auth_required BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_endpoint_method_path'
          AND conrelid = 'public.endpoint'::regclass
    ) THEN
        ALTER TABLE public.endpoint
            ADD CONSTRAINT uk_endpoint_method_path UNIQUE (method, path);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.endpoint_role (
    endpoint_id UUID NOT NULL,
    role_id     UUID NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (endpoint_id, role_id),
    CONSTRAINT fk_endpoint_role_endpoint
        FOREIGN KEY (endpoint_id) REFERENCES public.endpoint(id) ON DELETE CASCADE,
    CONSTRAINT fk_endpoint_role_role
        FOREIGN KEY (role_id) REFERENCES "user".role(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_endpoint_role_role_id
    ON public.endpoint_role(role_id);

CREATE TABLE IF NOT EXISTS public.endpoint_rule_version (
    id         INT PRIMARY KEY,
    version    BIGINT      NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO public.endpoint_rule_version(id, version, updated_at)
VALUES (1, 0, NOW())
ON CONFLICT (id) DO NOTHING;

CREATE OR REPLACE FUNCTION public.fn_endpoint_rule_touch()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE public.endpoint_rule_version
       SET version = version + 1,
           updated_at = NOW()
     WHERE id = 1;

    PERFORM pg_notify('endpoint_rule_changed', 'changed');
    RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS trg_endpoint_rule_touch_endpoint ON public.endpoint;
CREATE TRIGGER trg_endpoint_rule_touch_endpoint
AFTER INSERT OR UPDATE OR DELETE ON public.endpoint
FOR EACH STATEMENT
EXECUTE FUNCTION public.fn_endpoint_rule_touch();

DROP TRIGGER IF EXISTS trg_endpoint_rule_touch_endpoint_role ON public.endpoint_role;
CREATE TRIGGER trg_endpoint_rule_touch_endpoint_role
AFTER INSERT OR UPDATE OR DELETE ON public.endpoint_role
FOR EACH STATEMENT
EXECUTE FUNCTION public.fn_endpoint_rule_touch();

INSERT INTO public.endpoint(id, method, path, auth_required)
VALUES
    ('11111111-1111-1111-1111-111111111001', '*', '*', FALSE);