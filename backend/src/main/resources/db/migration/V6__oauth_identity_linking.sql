CREATE TABLE oauth_identities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_oauth_identity_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT uk_oauth_identity_provider_user UNIQUE (provider, user_id)
);

CREATE INDEX idx_oauth_identities_user ON oauth_identities(user_id);
