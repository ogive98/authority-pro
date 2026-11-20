-- Opt-in qualification only, never in production Flyway locations.
CREATE TABLE tenancy.failed_v2_marker (id uuid PRIMARY KEY);
SELECT 1 / 0;
