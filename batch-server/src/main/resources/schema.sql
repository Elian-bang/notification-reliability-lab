-- ─────────────────────────── 도메인 5테이블 ───────────────────────────

CREATE TABLE IF NOT EXISTS tenant (
    id   BIGINT       NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

-- 자원 B — 수신자만 X락으로 잡는다
CREATE TABLE IF NOT EXISTS member_account (
    id                 BIGINT      NOT NULL PRIMARY KEY,
    tenant_id        BIGINT      NOT NULL,
    access_token       VARCHAR(64) NULL,
    token_refreshed_at DATETIME(6) NULL,
    last_login_at      DATETIME(6) NULL,
    CONSTRAINT fk_account_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE=InnoDB;

-- 요청 서버가 INSERT. account_id FK -> 계정 행에 S락
CREATE TABLE IF NOT EXISTS notification_request (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id  BIGINT      NOT NULL,
    account_id   BIGINT      NOT NULL,
    payload      VARCHAR(200) NOT NULL,
    status       VARCHAR(16) NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    finished_at  DATETIME(6) NULL,
    CONSTRAINT fk_req_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_req_account  FOREIGN KEY (account_id)  REFERENCES member_account (id),
    KEY idx_req_status (status)
) ENGINE=InnoDB;

-- 자원 A — 배치가 INSERT (여기서 FK S락이 걸린다) / 수신자가 read_at UPDATE
CREATE TABLE IF NOT EXISTS notification (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT      NOT NULL,
    request_id BIGINT      NULL,
    status     VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    sent_at    DATETIME(6) NULL,
    read_at    DATETIME(6) NULL,
    CONSTRAINT fk_noti_account FOREIGN KEY (account_id) REFERENCES member_account (id),
    KEY idx_noti_account (account_id),
    KEY idx_noti_request (request_id)
) ENGINE=InnoDB;

-- ─────────────────────── 실험 조율 (서버 3개 동기화) ───────────────────────

CREATE TABLE IF NOT EXISTS experiment_control (
    id                TINYINT     NOT NULL PRIMARY KEY,
    run_id            VARCHAR(64) NOT NULL,
    variant_id        VARCHAR(32) NOT NULL,
    receiver_unify    TINYINT     NOT NULL DEFAULT 0,  -- 1 = 수신자도 A->B (V4)
    receiver_tx_split TINYINT     NOT NULL DEFAULT 0,  -- 1 = 토큰갱신/알림확인 분리 (V9)
    isolation_level   VARCHAR(24) NOT NULL DEFAULT 'REPEATABLE READ',
    active            TINYINT     NOT NULL DEFAULT 0,
    mode              VARCHAR(16) NOT NULL DEFAULT 'SEND',
    hot_content_seq   BIGINT      NULL,
    view_in_same_tx   TINYINT     NOT NULL DEFAULT 1,
    updated_at        DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS experiment_metric (
    run_id       VARCHAR(64) NOT NULL,
    role         VARCHAR(16) NOT NULL,   -- batch | receiver | request
    attempted    BIGINT NOT NULL DEFAULT 0,
    committed    BIGINT NOT NULL DEFAULT 0,
    deadlocks    BIGINT NOT NULL DEFAULT 0,   -- MySQL 1213
    lock_timeouts BIGINT NOT NULL DEFAULT 0,  -- MySQL 1205
    other_errors BIGINT NOT NULL DEFAULT 0,
    p95_millis   BIGINT NOT NULL DEFAULT 0,
    step_token_fail BIGINT NOT NULL DEFAULT 0,
    step_read_fail  BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (run_id, role)
) ENGINE=InnoDB;

-- ───────────────────── 사건 재현: 공지 푸시 배치 ─────────────────────
-- T+0s 배치가 콘텐츠 3건을 한 트랜잭션으로 처리했다.
-- A는 T+72s 에 완료 표시(UPDATE)를 했지만 커밋은 T+171s였다.
-- 그 99초 동안 A 행이 잠겨 있었고, 푸시를 받고 들어온 회원의 조회수 UPDATE가 대기했다.
CREATE TABLE IF NOT EXISTS content_item (
    seq        BIGINT       NOT NULL PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    view_count INT          NOT NULL DEFAULT 0,
    status     VARCHAR(16)  NOT NULL DEFAULT 'READY',
    sent_at    DATETIME(6)  NULL,
    audience   INT          NOT NULL DEFAULT 0
) ENGINE=InnoDB;
