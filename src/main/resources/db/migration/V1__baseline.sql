-- Baseline migration. Phase 1 has no tables yet; this proves Flyway runs against the database.
-- Real tables arrive with their phases (V2__create_users.sql, ...).
-- Never edit a migration after it has been applied anywhere: add a new V<n> file instead.
SELECT 1;
