-- Rehash the demo users using plaintext passwords that remain visible in this migration.
-- This keeps the seed easy to read while storing bcrypt hashes in the database.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

WITH demo_passwords(email, plain_password) AS (
    VALUES
        ('admin@balanced.local', 'password'),
        ('user@balanced.local', 'password')
)
UPDATE users u
SET password_hash = crypt(demo_passwords.plain_password, gen_salt('bf', 10))
FROM demo_passwords
WHERE u.email = demo_passwords.email;
