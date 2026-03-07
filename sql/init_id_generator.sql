-- Required by common.util.CustomIdGenerator:
--   SELECT public.fn_nextval('<table_name>')
--
-- Rule:
-- 1) table_nm row exists -> use that prefix/pad.
-- 2) row does not exist -> auto insert with generated unique prefix.
-- 3) prefix uniqueness is guaranteed by DB constraint + generation logic.

CREATE TABLE IF NOT EXISTS public.idgenerator_meta (
    table_nm   VARCHAR(100) PRIMARY KEY,
    seq        BIGINT NOT NULL DEFAULT 0,
    prefix     VARCHAR(10) NOT NULL,
    pad        INT NOT NULL DEFAULT 8,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed fixed prefixes (unique by design)
INSERT INTO public.idgenerator_meta(table_nm, seq, prefix, pad)
VALUES
    ('auction', 0, 'AUC', 8),
    ('user', 0, 'USR', 8),
    ('address', 0, 'ADR', 8),
    ('wishlist', 0, 'WSH', 8),
    ('notification', 0, 'NTF', 8),
    ('product', 0, 'PRD', 8),
    ('file', 0, 'FIL', 8),
    ('file_group', 0, 'FGP', 8),
    ('deposit_order', 0, 'DOR', 8),
    ('deposit_payment', 0, 'DPM', 8),
    ('winning_order', 0, 'WOR', 8),
    ('settlement', 0, 'STL', 8),
    ('settlement_group', 0, 'STG', 8)
ON CONFLICT (table_nm) DO UPDATE
SET prefix = EXCLUDED.prefix,
    pad = EXCLUDED.pad;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.idgenerator_meta
        GROUP BY prefix
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate prefix found in public.idgenerator_meta. Please resolve prefix conflicts first.';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_idgenerator_meta_prefix'
          AND conrelid = 'public.idgenerator_meta'::regclass
    ) THEN
        ALTER TABLE public.idgenerator_meta
            ADD CONSTRAINT uk_idgenerator_meta_prefix UNIQUE (prefix);
    END IF;
END $$;

CREATE OR REPLACE FUNCTION public.fn_nextval(p_table_nm TEXT)
RETURNS VARCHAR
LANGUAGE plpgsql
AS $$
DECLARE
    v_seq BIGINT;
    v_prefix TEXT;
    v_pad INT;
    v_base TEXT;
    v_try INT := 1;
BEGIN
    IF p_table_nm IS NULL OR BTRIM(p_table_nm) = '' THEN
        RAISE EXCEPTION 'p_table_nm must not be blank';
    END IF;

    -- Ensure concurrent callers don't create duplicate prefixes for new table keys.
    PERFORM pg_advisory_xact_lock(hashtext('idgenerator_meta_prefix_lock'));

    IF NOT EXISTS (SELECT 1 FROM public.idgenerator_meta WHERE table_nm = p_table_nm) THEN
        v_base := UPPER(LEFT(REGEXP_REPLACE(p_table_nm, '[^a-zA-Z0-9]', '', 'g'), 3));
        IF v_base IS NULL OR v_base = '' THEN
            v_base := 'IDG';
        END IF;

        v_prefix := v_base;
        WHILE EXISTS (SELECT 1 FROM public.idgenerator_meta WHERE prefix = v_prefix) LOOP
            v_prefix := v_base || v_try::TEXT;
            v_try := v_try + 1;
        END LOOP;

        INSERT INTO public.idgenerator_meta(table_nm, seq, prefix, pad)
        VALUES (p_table_nm, 0, v_prefix, 8);
    END IF;

    UPDATE public.idgenerator_meta
       SET seq = seq + 1,
           updated_at = NOW()
     WHERE table_nm = p_table_nm
     RETURNING seq, prefix, pad
          INTO v_seq, v_prefix, v_pad;

    RETURN v_prefix || LPAD(v_seq::TEXT, v_pad, '0');
END;
$$;

COMMENT ON FUNCTION public.fn_nextval(TEXT)
IS 'Returns ID like AUC00000001 using public.idgenerator_meta with unique prefix policy';
