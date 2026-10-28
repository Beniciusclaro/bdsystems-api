CREATE TABLE password_reset_tokens
(
  id          UUID PRIMARY KEY,
  user_id     UUID                     NOT NULL,
  token_hash  VARCHAR(64)              NOT NULL UNIQUE,
  expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
  created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
  consumed_at TIMESTAMP WITH TIME ZONE,

  CONSTRAINT fk_password_reset_tokens_user
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_password_reset_tokens_user_created_at
  ON password_reset_tokens (user_id, created_at DESC);
