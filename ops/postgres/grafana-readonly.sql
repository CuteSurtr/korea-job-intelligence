-- The role Grafana connects as.
--
-- Grafana lets anyone with dashboard edit rights run arbitrary SQL against a datasource, so the
-- credentials it holds are the blast radius. This role can read and nothing else: no writes, no
-- DDL, no objects of its own. The DEFAULT PRIVILEGES line matters as much as the GRANT — without
-- it the next Flyway migration creates a table this role cannot see, and a panel starts failing
-- for a reason that has nothing to do with the panel.
--
-- Compose mounts this into the Postgres image's init directory, so a stack brought up on an
-- empty volume gets it automatically. Init scripts run only on first initialisation, so a stack
-- whose volume already exists has to be given it by hand, as the database owner:
--
--   GRAFANA_DB_PASSWORD=a-strong-password \
--     psql "postgresql://kji@localhost:5432/kji" -f ops/postgres/grafana-readonly.sql
--
-- The password comes from the environment rather than a literal so this file can stay in git.

\getenv grafana_password GRAFANA_DB_PASSWORD
\if :{?grafana_password}
\else
\set grafana_password kji
\endif

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'kji_readonly') THEN
    CREATE ROLE kji_readonly LOGIN;
  END IF;
END
$$;

ALTER ROLE kji_readonly WITH PASSWORD :'grafana_password';

GRANT CONNECT ON DATABASE :"DBNAME" TO kji_readonly;
GRANT USAGE ON SCHEMA public TO kji_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO kji_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO kji_readonly;

-- Nothing beyond reading. Stated rather than assumed: a role that can create objects in a schema
-- it can read is not read-only, and PUBLIC holds CREATE on public in some configurations.
REVOKE CREATE ON SCHEMA public FROM kji_readonly;
