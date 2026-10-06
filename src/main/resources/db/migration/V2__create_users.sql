-- Users. One role per user (see README: role is an enum column, not a join table).
-- phone is nullable at the database level because social-login users (Phase 3) arrive without one;
-- normal registration requires it at the API level.
CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(254) NOT NULL,
    phone         VARCHAR(20),
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_users_role   CHECK (role   IN ('USER', 'ADMIN', 'SUPER_ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

-- Case-insensitive uniqueness even if some code path forgets to lower-case the email.
CREATE UNIQUE INDEX uk_users_email ON users (lower(email));
-- Unique when present; many users may have no phone yet.
CREATE UNIQUE INDEX uk_users_phone ON users (phone) WHERE phone IS NOT NULL;
