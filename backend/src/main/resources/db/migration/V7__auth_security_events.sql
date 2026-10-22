CREATE TABLE auth_security_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(64) NOT NULL,
    account_identifier_hash VARCHAR(64) NOT NULL,
    event_details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_security_events_created_at ON auth_security_events(created_at);
CREATE INDEX idx_auth_security_events_account_created
    ON auth_security_events(account_identifier_hash, created_at);
