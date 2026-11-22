CREATE TABLE refresh_tokens (
                                id         BIGINT      NOT NULL AUTO_INCREMENT,
                                user_id    BIGINT      NOT NULL,
                                token_hash VARCHAR(64) NOT NULL,          -- SHA-256 hex. Never the raw token.
                                expires_at DATETIME(6) NOT NULL,
                                revoked_at DATETIME(6) NULL,
                                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                PRIMARY KEY (id),
                                UNIQUE KEY uk_refresh_hash (token_hash),  -- refresh: WHERE token_hash = ?
                                KEY idx_refresh_user (user_id),           -- logout-all: WHERE user_id = ?
                                CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE email_verification_tokens (
                                           id         BIGINT      NOT NULL AUTO_INCREMENT,
                                           user_id    BIGINT      NOT NULL,
                                           token_hash VARCHAR(64) NOT NULL,
                                           expires_at DATETIME(6) NOT NULL,
                                           used_at    DATETIME(6) NULL,
                                           created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                           PRIMARY KEY (id),
                                           UNIQUE KEY uk_evt_hash (token_hash),
                                           KEY idx_evt_user (user_id),
                                           CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;