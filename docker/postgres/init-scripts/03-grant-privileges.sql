\c wise_task_profile;

DO $$
DECLARE r record;
BEGIN
FOR r IN
SELECT tablename FROM pg_tables
WHERE schemaname = 'public' AND tableowner = 'wise_task'
    LOOP
        EXECUTE format('ALTER TABLE public.%I OWNER TO wise_task_profile', r.tablename);
END LOOP;
END $$;

DO $$
DECLARE r record;
BEGIN
FOR r IN
SELECT sequencename FROM pg_sequences
WHERE schemaname = 'public' AND sequenceowner = 'wise_task'
    LOOP
        EXECUTE format('ALTER SEQUENCE public.%I OWNER TO wise_task_profile', r.sequencename);
END LOOP;
END $$;

GRANT USAGE, CREATE ON SCHEMA public TO wise_task_profile;

\c wise_task_plugin;

DO $$
DECLARE r record;
BEGIN
FOR r IN
SELECT tablename FROM pg_tables
WHERE schemaname = 'public' AND tableowner = 'wise_task'
    LOOP
        EXECUTE format('ALTER TABLE public.%I OWNER TO wise_task_plugin', r.tablename);
END LOOP;
END $$;

DO $$
DECLARE r record;
BEGIN
FOR r IN
SELECT sequencename FROM pg_sequences
WHERE schemaname = 'public' AND sequenceowner = 'wise_task'
    LOOP
        EXECUTE format('ALTER SEQUENCE public.%I OWNER TO wise_task_plugin', r.sequencename);
END LOOP;
END $$;

GRANT USAGE, CREATE ON SCHEMA public TO wise_task_plugin;

\c wise_task_task;

DO $$
DECLARE r record;
BEGIN
FOR r IN
SELECT tablename FROM pg_tables
WHERE schemaname = 'public' AND tableowner = 'wise_task'
    LOOP
        EXECUTE format('ALTER TABLE public.%I OWNER TO wise_task_task', r.tablename);
END LOOP;
END $$;

DO $$
DECLARE r record;
BEGIN
FOR r IN
SELECT sequencename FROM pg_sequences
WHERE schemaname = 'public' AND sequenceowner = 'wise_task'
    LOOP
        EXECUTE format('ALTER SEQUENCE public.%I OWNER TO wise_task_task', r.sequencename);
END LOOP;
END $$;

GRANT USAGE, CREATE ON SCHEMA public TO wise_task_task;