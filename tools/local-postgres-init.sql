\set ON_ERROR_STOP on

SET log_statement = 'none';
SET log_min_error_statement = 'panic';

CREATE ROLE authority_pro_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS;
CREATE ROLE authority_pro_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS CONNECTION LIMIT 2;
CREATE ROLE authority_pro_runtime LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS CONNECTION LIMIT 5;
GRANT authority_pro_owner TO authority_pro_migrator WITH INHERIT FALSE, SET TRUE;

\set migrator_verifier `cat /run/authority-pro-secrets/migrator-verifier`
\set runtime_verifier `cat /run/authority-pro-secrets/runtime-verifier`
ALTER ROLE authority_pro_migrator PASSWORD :'migrator_verifier';
ALTER ROLE authority_pro_runtime PASSWORD :'runtime_verifier';
\unset migrator_verifier
\unset runtime_verifier

CREATE DATABASE authority_pro OWNER authority_pro_owner CONNECTION LIMIT 12;
REVOKE ALL ON DATABASE authority_pro FROM PUBLIC;
GRANT CONNECT ON DATABASE authority_pro TO authority_pro_migrator, authority_pro_runtime;
REVOKE ALL ON DATABASE postgres FROM PUBLIC;
REVOKE ALL ON DATABASE template1 FROM PUBLIC;

\connect authority_pro
ALTER SCHEMA public OWNER TO authority_pro_owner;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO authority_pro_migrator, authority_pro_runtime;
ALTER DEFAULT PRIVILEGES FOR ROLE authority_pro_owner REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE authority_pro_owner REVOKE USAGE ON TYPES FROM PUBLIC;
