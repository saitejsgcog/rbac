-- user_sessions: user_id is BIGINT (matches users.id)
CREATE TABLE user_sessions (
  id            BINARY(16) NOT NULL,
  user_id       BIGINT NOT NULL,
  created_at    DATETIME NOT NULL,
  last_activity DATETIME NOT NULL,
  revoked       TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_sessions_user (user_id),
  CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- refresh_tokens: user_id BIGINT, session_id UUID
CREATE TABLE refresh_tokens (
  id          BINARY(16) NOT NULL,
  session_id  BINARY(16) NOT NULL,
  user_id     BIGINT NOT NULL,
  token_hash  VARBINARY(64) NOT NULL,
  issued_at   DATETIME NOT NULL,
  expires_at  DATETIME NOT NULL,
  revoked     TINYINT(1) NOT NULL DEFAULT 0,
  rotated_at  DATETIME NULL,
  parent_id   BINARY(16) NULL,
  PRIMARY KEY (id),
  KEY idx_refresh_session (session_id),
  KEY idx_refresh_user (user_id),
  CONSTRAINT fk_refresh_session FOREIGN KEY (session_id) REFERENCES user_sessions(id),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_refresh_parent FOREIGN KEY (parent_id) REFERENCES refresh_tokens(id)
);

-- auth_audit_log: user_id BIGINT (nullable to log failures for unknown users)
CREATE TABLE auth_audit_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NULL,
  event_type  VARCHAR(32) NOT NULL,
  details     VARCHAR(255) NULL,
  ip          VARCHAR(64) NULL,
  user_agent  VARCHAR(255) NULL,
  created_at  DATETIME NOT NULL
);