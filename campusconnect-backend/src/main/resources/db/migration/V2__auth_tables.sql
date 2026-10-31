CREATE TABLE users (
                       id                BIGINT       NOT NULL AUTO_INCREMENT,
                       email             VARCHAR(255) NOT NULL,
                       password_hash     VARCHAR(100) NOT NULL,
                       role              VARCHAR(10)  NOT NULL DEFAULT 'STUDENT',
                       status            VARCHAR(25)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
                       email_verified_at DATETIME(6)  NULL,
                       deleted_at        DATETIME(6)  NULL,
                       created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                       PRIMARY KEY (id),
                       UNIQUE KEY uk_users_email (email),
                       CONSTRAINT chk_users_role   CHECK (role   IN ('STUDENT','FACULTY','ADMIN')),
                       CONSTRAINT chk_users_status CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE profiles (
                          user_id         BIGINT       NOT NULL,
                          display_name    VARCHAR(80)  NOT NULL,
                          bio             VARCHAR(500) NULL,
                          department      VARCHAR(100) NULL,
                          year_of_study   TINYINT      NULL,
                          graduation_year SMALLINT     NULL,
                          designation     VARCHAR(100) NULL,
                          avatar_key      VARCHAR(255) NULL,
                          created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                          updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                          PRIMARY KEY (user_id),
                          KEY idx_profiles_department (department, user_id),
                          FULLTEXT KEY ft_profiles (display_name, bio),
                          CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES users (id),
                          CONSTRAINT chk_profiles_year CHECK (year_of_study BETWEEN 1 AND 8)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;