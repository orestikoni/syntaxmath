-- Google-only accounts have no password
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Sign-up collects first name and last name
ALTER TABLE users
    ADD COLUMN first_name VARCHAR(100),
    ADD COLUMN last_name VARCHAR(100);

-- Server-side sessions: only the hash of the session token is stored
CREATE TABLE user_sessions (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               token_hash VARCHAR(128) NOT NULL UNIQUE,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_user_sessions_user ON user_sessions(user_id);