DO $$
DECLARE
    constraint_name text;
BEGIN
    IF to_regclass('public.action_requests') IS NOT NULL THEN
        FOR constraint_name IN
            SELECT con.conname
            FROM pg_constraint con
            WHERE con.conrelid = 'public.action_requests'::regclass
              AND con.contype = 'u'
              AND (
                  SELECT array_agg(att.attname ORDER BY att.attname)
                  FROM unnest(con.conkey) AS key(attnum)
                  JOIN pg_attribute att
                    ON att.attrelid = con.conrelid
                   AND att.attnum = key.attnum
              ) = ARRAY['idempotency_key']::name[]
        LOOP
            EXECUTE format('ALTER TABLE public.action_requests DROP CONSTRAINT %I', constraint_name);
        END LOOP;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conrelid = 'public.action_requests'::regclass
              AND conname = 'uk_action_request_agent_idempotency'
        ) THEN
            ALTER TABLE public.action_requests
                ADD CONSTRAINT uk_action_request_agent_idempotency UNIQUE (agent_id, idempotency_key);
        END IF;
    END IF;
END
$$;
