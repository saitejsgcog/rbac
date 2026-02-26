-- file: src/main/resources/db/migration/VXX__iam_login_security.sql
-- Replace XX with your next version number

-- 1) Extend users for lockout (adjust table/column names if needed)
ALTER TABLE users
  ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
  ADD COLUMN locked_until DATETIME NULL;

-- 2) Sessions table
CREATE TABLE user_sessions (
  id            BINARY(16) NOT NULL,
  user_id       BINARY(16) NOT NULL,
  created_at    DATETIME NOT NULL,
  last_activity DATETIME NOT NULL,
  revoked       TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_sessions_user (user_id),
  CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 3) Refresh tokens table
CREATE TABLE refresh_tokens (
  id          BINARY(16) NOT NULL,
  session_id  BINARY(16) NOT NULL,
  user_id     BINARY(16) NOT NULL,
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

-- 4) Audit log table
CREATE TABLE auth_audit_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BINARY(16) NULL,
  event_type  VARCHAR(32) NOT NULL,
  details     VARCHAR(255) NULL,
  ip          VARCHAR(64) NULL,
  user_agent  VARCHAR(255) NULL,
  created_at  DATETIME NOT NULL
);