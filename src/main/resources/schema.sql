CREATE TABLE IF NOT EXISTS app_user (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    lastname TEXT NOT NULL,
    email TEXT NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('USER', 'ADMIN'))
);

CREATE UNIQUE INDEX IF NOT EXISTS app_user_email_unique ON app_user (email);
