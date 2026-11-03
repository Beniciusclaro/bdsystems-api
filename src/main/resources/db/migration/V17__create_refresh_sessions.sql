ALTER TABLE company
  ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE refresh_sessions
(
  id           UUID PRIMARY KEY,
  user_id      UUID                     NOT NULL,
  token_hash   VARCHAR(64)              NOT NULL UNIQUE,
  created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
  expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
  revoked_at   TIMESTAMP WITH TIME ZONE,
  last_used_at TIMESTAMP WITH TIME ZONE,

  CONSTRAINT fk_refresh_sessions_user
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_refresh_sessions_user_id ON refresh_sessions (user_id);
CREATE INDEX idx_refresh_sessions_expires_at ON refresh_sessions (expires_at);
CREATE INDEX idx_refresh_sessions_active_lookup
  ON refresh_sessions (token_hash, expires_at, revoked_at);
