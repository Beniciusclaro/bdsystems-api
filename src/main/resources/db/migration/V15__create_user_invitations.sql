CREATE TABLE user_invitations
(
  id          UUID PRIMARY KEY,
  user_id     UUID                     NOT NULL,
  token_hash  VARCHAR(64)              NOT NULL UNIQUE,
  expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
  created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
  consumed_at TIMESTAMP WITH TIME ZONE,

  CONSTRAINT fk_user_invitations_user
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_user_invitations_user_created_at
  ON user_invitations (user_id, created_at DESC);
